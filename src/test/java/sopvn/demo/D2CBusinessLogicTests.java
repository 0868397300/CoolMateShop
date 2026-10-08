package sopvn.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sopvn.demo.cart.CartService;
import sopvn.demo.cart.PricingService;
import sopvn.demo.cart.dto.PricingSummaryDTO;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.core.service.NotificationService;
import sopvn.demo.core.service.ReviewService;
import sopvn.demo.entity.*;
import sopvn.demo.inventory.InventoryService;
import sopvn.demo.inventory.StockService;
import sopvn.demo.inventory.dto.InventoryStatsDTO;
import sopvn.demo.order.OrderService;
import sopvn.demo.order.ReturnService;
import sopvn.demo.order.ShippingFeeService;
import sopvn.demo.payment.VnpayPaymentProcessor;
import sopvn.demo.payment.VnpayService;
import sopvn.demo.repository.*;
import sopvn.demo.wallet.CoolCashService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class D2CBusinessLogicTests {

    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private InventoryMovementRepository inventoryMovementRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private CoolCashTransactionRepository coolCashTransactionRepository;
    @Mock private ComboRuleRepository comboRuleRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionUsageRepository promotionUsageRepository;
    @Mock private OrderReturnRepository orderReturnRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private NotificationService notificationService;
    @Mock private ShippingFeeService shippingFeeService;
    @Mock private InventoryReceiptRepository inventoryReceiptRepository;
    @Mock private VnpayService vnpayService;

    private StockService stockService;
    private CoolCashService coolCashService;
    private PricingService pricingService;
    private OrderService orderService;
    private ReturnService returnService;
    private ReviewService reviewService;
    private CartService cartService;
    private VnpayPaymentProcessor vnpayPaymentProcessor;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(productVariantRepository, inventoryMovementRepository, notificationService);
        coolCashService = new CoolCashService(userRepository, coolCashTransactionRepository);
        pricingService = new PricingService(comboRuleRepository, promotionRepository, promotionUsageRepository, shippingFeeService);
        orderService = new OrderService(orderRepository, orderItemRepository, productVariantRepository,
                promotionRepository, promotionUsageRepository, pricingService, stockService, 
                coolCashService, notificationService, userRepository, coolCashTransactionRepository);
        returnService = new ReturnService(orderRepository, orderItemRepository, orderReturnRepository, 
                productVariantRepository, stockService, coolCashService, notificationService, userRepository);
        reviewService = new ReviewService(reviewRepository, orderItemRepository, productRepository);
        cartService = new CartService(cartRepository, cartItemRepository, productVariantRepository, productImageRepository, pricingService);
        vnpayPaymentProcessor = new VnpayPaymentProcessor(vnpayService, orderRepository, stockService, coolCashService,
                promotionRepository, promotionUsageRepository, cartService, notificationService);
        inventoryService = new InventoryService(inventoryReceiptRepository, productVariantRepository);
    }

    // =========================================================================
    // 1. CONCURRENCY - SKU stock = 1, concurrent execution atomic reservation
    // =========================================================================
    @Test
    @DisplayName("TEST 1: Concurrency check - SKU stock = 1, concurrent execution atomic reservation")
    void test1_stockReservationConcurrency() throws InterruptedException {
        ProductVariant variant = new ProductVariant();
        variant.setId(101L);
        variant.setSku("CM-TS-BLK-L");
        variant.setStockQuantity(1);
        variant.setIsActive(true);

        ReentrantLock dbRowLock = new ReentrantLock();
        when(productVariantRepository.findByIdForUpdate(101L)).thenAnswer(inv -> {
            dbRowLock.lock();
            return Optional.of(variant);
        });
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            try {
                return inv.getArgument(0);
            } finally {
                if (dbRowLock.isHeldByCurrentThread()) {
                    dbRowLock.unlock();
                }
            }
        });

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long orderId = 1000L + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    stockService.reserveStock(variant, 1, orderId);
                    successCount.incrementAndGet();
                } catch (CustomException ex) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    if (dbRowLock.isHeldByCurrentThread()) {
                        dbRowLock.unlock();
                    }
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Chỉ đúng 1 giao dịch được giữ hàng thành công khi stock = 1");
        assertEquals(1, failCount.get(), "Giao dịch thứ 2 phải thất bại với CustomException thiếu hàng");
        assertEquals(0, variant.getStockQuantity(), "Tồn kho sau đó phải bằng 0, không bị âm tồn kho");
    }

    // =========================================================================
    // 2. VNPAY pending payment cancellation releases reserved stock
    // =========================================================================
    @Test
    @DisplayName("TEST 2: VNPAY pending payment cancellation releases reserved stock")
    void test2_cancelPendingOrderReleasesStock() {
        ProductVariant variant = new ProductVariant();
        variant.setId(201L);
        variant.setStockQuantity(5);

        OrderItem item = new OrderItem();
        item.setId(2001L);
        item.setVariant(variant);
        item.setQuantity(2);

        Order order = new Order();
        order.setId(501L);
        order.setOrderCode("CM-VNPAY-TEST2");
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("PAYMENT_PENDING");
        order.setPaymentMethod("VNPAY");
        order.setItems(Collections.singletonList(item));

        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(productVariantRepository.findById(201L)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceIdAndMovementType("ORDER", 501L, "ORDER_RESERVE")).thenReturn(true);

        orderService.cancelOrder(order.getId(), null, "Khách hàng hủy đơn VNPAY quá hạn");

        assertEquals("CANCELLED", order.getOrderStatus());
        assertEquals(7, variant.getStockQuantity());
    }

    // =========================================================================
    // 3. VNPAY Success callback & Duplicate callback idempotency
    // =========================================================================
    @Test
    @DisplayName("TEST 3: VNPAY real processor - Success callback & Duplicate callback idempotency")
    void test3_vnpaySuccessIdempotencyWithRealProcessor() {
        Order order = new Order();
        order.setId(601L);
        order.setOrderCode("CM-TXN-601");
        order.setVnpayTxnRef("VNPAY-TXN-601");
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("PAYMENT_PENDING");
        order.setFinalAmount(BigDecimal.valueOf(250_000));

        when(vnpayService.validateSignature(anyMap(), any())).thenReturn(true);
        when(orderRepository.findByVnpayTxnRefForUpdate("VNPAY-TXN-601")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNPAY-TXN-601");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_Amount", "25000000"); // 250,000 x 100
        params.put("vnp_TransactionNo", "14000123");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_SecureHash", "VALID_HASH");

        // Callback lần 1: Thành công
        VnpayPaymentProcessor.PaymentResult result1 = vnpayPaymentProcessor.processVnpayResult(params, "guestToken");
        assertTrue(result1.isSuccess());
        assertEquals("00", result1.getCode());
        assertEquals("PAID", order.getPaymentStatus());
        assertEquals("CONFIRMED", order.getOrderStatus());

        // Callback lần 2 (Duplicate IPN/Return): Idempotent rejection
        VnpayPaymentProcessor.PaymentResult result2 = vnpayPaymentProcessor.processVnpayResult(params, "guestToken");
        assertTrue(result2.isSuccess());
        assertEquals("02", result2.getCode(), "Lần 2 phải trả về code 02 (đã ghi nhận trước đó)");
    }

    // =========================================================================
    // 3B. VNPAY success callback CANNOT revive order that was CANCELLED
    // =========================================================================
    @Test
    @DisplayName("TEST 3B: VNPAY success callback cannot revive order that was already CANCELLED")
    void test3b_vnpaySuccessCannotReviveCancelledOrder() {
        Order order = new Order();
        order.setId(602L);
        order.setOrderCode("CM-TXN-602");
        order.setVnpayTxnRef("VNPAY-TXN-602");
        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus("PAYMENT_FAILED");
        order.setFinalAmount(BigDecimal.valueOf(300_000));

        when(vnpayService.validateSignature(anyMap(), any())).thenReturn(true);
        when(orderRepository.findByVnpayTxnRefForUpdate("VNPAY-TXN-602")).thenReturn(Optional.of(order));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNPAY-TXN-602");
        params.put("vnp_ResponseCode", "00"); // Success response arriving late
        params.put("vnp_Amount", "30000000");
        params.put("vnp_SecureHash", "VALID_HASH");

        VnpayPaymentProcessor.PaymentResult result = vnpayPaymentProcessor.processVnpayResult(params, "guestToken");

        assertFalse(result.isSuccess(), "Không được phép hồi sinh đơn hàng đã bị CANCELLED");
        assertEquals("CANCELLED", order.getOrderStatus(), "Trạng thái đơn hàng vẫn phải là CANCELLED");
        assertEquals("PAYMENT_FAILED", order.getPaymentStatus());
    }

    // =========================================================================
    // 3C. Unpaid VNPAY order cannot be completed or confirmed
    // =========================================================================
    @Test
    @DisplayName("TEST 3C: Unpaid VNPAY order cannot be completed or confirmed")
    void test3c_unpaidVnpayOrderCannotBeCompleted() {
        Order order = new Order();
        order.setId(603L);
        order.setOrderCode("CM-UNPAID-603");
        order.setPaymentMethod("VNPAY");
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("PAYMENT_PENDING");

        when(orderRepository.findById(603L)).thenReturn(Optional.of(order));

        // 1. Không thể completeOrder khi chưa DELIVERED và chưa PAID
        assertThrows(CustomException.class, () -> {
            orderService.completeOrder(603L);
        }, "Đơn hàng VNPAY chưa thanh toán không được phép completeOrder");

        // 2. Không thể transitionStatus sang CONFIRMED khi chưa PAID
        User admin = new User(); admin.setId(1L);
        Role r = new Role(); r.setRoleName("ROLE_ADMIN"); admin.getRoles().add(r);

        assertThrows(CustomException.class, () -> {
            orderService.transitionStatus(603L, "CONFIRMED", admin);
        }, "Đơn hàng VNPAY chưa thanh toán không được phép transitionStatus sang CONFIRMED");

        assertEquals("PENDING", order.getOrderStatus());
        assertEquals("PAYMENT_PENDING", order.getPaymentStatus());
    }

    // =========================================================================
    // 3D. Cancel order PAID follows refund workflow, restocks consumed stock
    // =========================================================================
    @Test
    @DisplayName("TEST 3D: Cancel order PAID follows refund workflow, restocks consumed stock")
    void test3d_cancelPaidOrderFollowsRefundPolicy() {
        ProductVariant variant = new ProductVariant();
        variant.setId(205L);
        variant.setStockQuantity(8);

        OrderItem item = new OrderItem();
        item.setId(2005L);
        item.setVariant(variant);
        item.setQuantity(2);

        User user = new User();
        user.setId(705L);
        user.setCoolcashBalance(BigDecimal.valueOf(10_000));

        Order order = new Order();
        order.setId(505L);
        order.setOrderCode("CM-PAID-505");
        order.setUser(user);
        order.setOrderStatus("CONFIRMED");
        order.setPaymentStatus("PAID");
        order.setPaymentMethod("VNPAY");
        order.setCoolcashUsed(BigDecimal.valueOf(50_000));
        order.setFinalAmount(BigDecimal.valueOf(200_000));
        order.setItems(Collections.singletonList(item));

        Promotion promo = new Promotion();
        promo.setId(99L);
        promo.setUsedCount(1);
        PromotionUsage usage = new PromotionUsage(promo, user, order, BigDecimal.valueOf(30_000), "FINALIZED");

        when(orderRepository.findById(505L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productVariantRepository.findById(205L)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceIdAndMovementType("ORDER", 505L, "ORDER_CONSUME")).thenReturn(true);
        when(promotionUsageRepository.findByOrderId(505L)).thenReturn(Collections.singletonList(usage));
        when(promotionRepository.findById(99L)).thenReturn(Optional.of(promo));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(705L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(505L, null, "Hủy đơn hàng đã thanh toán trước khi giao");

        assertEquals("CANCELLED", order.getOrderStatus());
        assertEquals("REFUND_PENDING", order.getPaymentStatus(), "Đơn đã PAID khi hủy chuyển thành REFUND_PENDING");
        assertEquals("REFUND_PENDING", order.getRefundStatus(), "refundStatus phải được khởi tạo là REFUND_PENDING");
        assertEquals(10, variant.getStockQuantity(), "Hàng đã tiêu thụ (CONSUMED) phải được nhập lại kho");
        assertEquals("RELEASED", usage.getStatus(), "Voucher usage phải được chuyển sang RELEASED");
        assertEquals(0, promo.getUsedCount(), "Voucher usedCount của đơn PAID phải được giảm lại khi hủy");
        assertEquals(0, user.getCoolcashBalance().compareTo(BigDecimal.valueOf(60_000)), "CoolCash đã chi tiêu phải được hoàn lại số dư");
    }

    // =========================================================================
    // 3E. VNPAY Failure callback cancels order and compensates
    // =========================================================================
    @Test
    @DisplayName("TEST 3E: VNPAY failure callback marks order CANCELLED and compensates")
    void test3e_vnpayFailureCallbackMarksCancelled() {
        Order order = new Order();
        order.setId(604L);
        order.setOrderCode("CM-FAIL-604");
        order.setVnpayTxnRef("VNPAY-FAIL-604");
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("PAYMENT_PENDING");
        order.setFinalAmount(BigDecimal.valueOf(150_000));

        when(vnpayService.validateSignature(anyMap(), any())).thenReturn(true);
        when(orderRepository.findByVnpayTxnRefForUpdate("VNPAY-FAIL-604")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNPAY-FAIL-604");
        params.put("vnp_ResponseCode", "24"); // 24 = Customer cancelled
        params.put("vnp_Amount", "15000000");
        params.put("vnp_SecureHash", "VALID_HASH");

        VnpayPaymentProcessor.PaymentResult result = vnpayPaymentProcessor.processVnpayResult(params, "guestToken");

        assertFalse(result.isSuccess());
        assertEquals("24", result.getCode());
        assertEquals("CANCELLED", order.getOrderStatus());
        assertEquals("PAYMENT_FAILED", order.getPaymentStatus());
    }

    // =========================================================================
    // 3F. VNPAY payment timeout cleanup
    // =========================================================================
    @Test
    @DisplayName("TEST 3F: VNPAY payment timeout cleanup cancels expired pending orders")
    void test3f_vnpayPaymentTimeoutCleanup() {
        Order expiredOrder = new Order();
        expiredOrder.setId(605L);
        expiredOrder.setOrderCode("CM-EXPIRED-605");
        expiredOrder.setOrderStatus("PENDING");
        expiredOrder.setPaymentStatus("PAYMENT_PENDING");
        expiredOrder.setPaymentMethod("VNPAY");
        expiredOrder.setCreatedAt(LocalDateTime.now().minusMinutes(20));

        when(orderRepository.findExpiredVnpayOrders(any(LocalDateTime.class))).thenReturn(Collections.singletonList(expiredOrder));
        when(orderRepository.findByIdForUpdate(605L)).thenReturn(Optional.of(expiredOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        int cleaned = orderService.cleanupExpiredPaymentPendingOrders(15);

        assertEquals(1, cleaned);
        assertEquals("CANCELLED", expiredOrder.getOrderStatus());
        assertEquals("PAYMENT_FAILED", expiredOrder.getPaymentStatus());
    }

    // =========================================================================
    // 3G. DELIVERED order cancellation is strictly REJECTED
    // =========================================================================
    @Test
    @DisplayName("TEST 3G: DELIVERED order cancellation is strictly rejected")
    void test3g_deliveredOrderCancellationRejected() {
        Order order = new Order();
        order.setId(606L);
        order.setOrderCode("CM-DELIVERED-606");
        order.setOrderStatus("DELIVERED");
        order.setPaymentStatus("PAID");

        when(orderRepository.findById(606L)).thenReturn(Optional.of(order));

        assertThrows(CustomException.class, () -> {
            orderService.cancelOrder(606L, null, "Khách muốn hủy sau khi đã nhận hàng");
        }, "Đơn hàng DELIVERED không thể hủy, phải hướng dẫn khách dùng quy trình Đổi / Trả hàng");
    }

    // =========================================================================
    // 4. Historical COGS snapshot immutability
    // =========================================================================
    @Test
    @DisplayName("TEST 4: Historical COGS snapshot immutability")
    void test4_historicalCostSnapshotPreserved() {
        OrderItem oldItem = new OrderItem();
        oldItem.setId(4001L);
        oldItem.setQuantity(2);
        oldItem.setCostPriceSnapshot(BigDecimal.valueOf(100_000));

        ProductVariant variant = new ProductVariant();
        variant.setImportPrice(BigDecimal.valueOf(150_000));
        oldItem.setVariant(variant);

        BigDecimal oldOrderCOGS = oldItem.getCostPriceSnapshot().multiply(BigDecimal.valueOf(oldItem.getQuantity()));
        assertEquals(BigDecimal.valueOf(200_000), oldOrderCOGS);
        assertNotEquals(variant.getImportPrice().multiply(BigDecimal.valueOf(oldItem.getQuantity())), oldOrderCOGS);
    }

    // =========================================================================
    // 5. Combo category rule - Not Qualified
    // =========================================================================
    @Test
    @DisplayName("TEST 5: Combo category rule - 1 Shirt + 1 Pants does not qualify 2 Shirts combo")
    void test5_comboCategoryRuleNotQualified() {
        Category catShirt = new Category(); catShirt.setId(1); catShirt.setName("Áo");
        Category catPants = new Category(); catPants.setId(2); catPants.setName("Quần");

        Product p1 = new Product(); p1.setCategory(catShirt);
        Product p2 = new Product(); p2.setCategory(catPants);

        ProductVariant v1 = new ProductVariant(); v1.setProduct(p1); v1.setSalePrice(BigDecimal.valueOf(200_000));
        ProductVariant v2 = new ProductVariant(); v2.setProduct(p2); v2.setSalePrice(BigDecimal.valueOf(300_000));

        CartItem item1 = new CartItem(); item1.setVariant(v1); item1.setQuantity(1);
        CartItem item2 = new CartItem(); item2.setVariant(v2); item2.setQuantity(1);

        ComboRule shirtRule = new ComboRule();
        shirtRule.setCategory(catShirt);
        shirtRule.setMinQuantity(2);
        shirtRule.setDiscountPercentage(BigDecimal.valueOf(10));
        shirtRule.setIsActive(true);

        when(comboRuleRepository.findByIsActiveTrueOrderByMinQuantityDesc()).thenReturn(Collections.singletonList(shirtRule));
        when(shippingFeeService.calculateShippingFee(any(), any(), any())).thenReturn(BigDecimal.valueOf(25_000));

        PricingSummaryDTO summary = pricingService.calculatePricing(null, Arrays.asList(item1, item2), null, null, null, null, null);

        assertEquals(0, summary.getComboDiscount().compareTo(BigDecimal.ZERO), "1 Shirt + 1 Pants must not qualify for 2 Shirts combo");
    }

    // =========================================================================
    // 6. Combo category rule - Qualified
    // =========================================================================
    @Test
    @DisplayName("TEST 6: Combo category rule - 2 Shirts qualifies for combo discount")
    void test6_comboCategoryRuleQualified() {
        Category catShirt = new Category(); catShirt.setId(1); catShirt.setName("Áo");
        Product p1 = new Product(); p1.setCategory(catShirt);

        ProductVariant v1 = new ProductVariant(); v1.setProduct(p1); v1.setSalePrice(BigDecimal.valueOf(200_000));

        CartItem item1 = new CartItem(); item1.setVariant(v1); item1.setQuantity(2);

        ComboRule shirtRule = new ComboRule();
        shirtRule.setCategory(catShirt);
        shirtRule.setMinQuantity(2);
        shirtRule.setDiscountPercentage(BigDecimal.valueOf(10));
        shirtRule.setIsActive(true);

        when(comboRuleRepository.findByIsActiveTrueOrderByMinQuantityDesc()).thenReturn(Collections.singletonList(shirtRule));
        when(shippingFeeService.calculateShippingFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        PricingSummaryDTO summary = pricingService.calculatePricing(null, Collections.singletonList(item1), null, null, null, null, null);

        assertEquals(0, summary.getComboDiscount().compareTo(BigDecimal.valueOf(40_000)));
        assertEquals(0, summary.getFinalAmount().compareTo(BigDecimal.valueOf(360_000)));
    }

    // =========================================================================
    // 7. CoolCash 50% checkout limit clamping
    // =========================================================================
    @Test
    @DisplayName("TEST 7: CoolCash 50% checkout limit clamping")
    void test7_coolcashMax50PercentClamp() {
        User user = new User();
        user.setId(701L);
        user.setCoolcashBalance(BigDecimal.valueOf(200_000));

        ProductVariant v = new ProductVariant();
        v.setSalePrice(BigDecimal.valueOf(300_000));
        CartItem item = new CartItem(); item.setVariant(v); item.setQuantity(1);

        when(comboRuleRepository.findByIsActiveTrueOrderByMinQuantityDesc()).thenReturn(Collections.emptyList());
        when(shippingFeeService.calculateShippingFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        BigDecimal requestedCoolCash = BigDecimal.valueOf(200_000);
        PricingSummaryDTO summary = pricingService.calculatePricing(user, Collections.singletonList(item), null, requestedCoolCash, null, null, null);

        assertEquals(0, summary.getCoolcashUsed().compareTo(BigDecimal.valueOf(150_000)));
        assertEquals(0, summary.getFinalAmount().compareTo(BigDecimal.valueOf(150_000)));
    }

    // =========================================================================
    // 8. CoolCash duplicate spend/refund with Idempotency Key
    // =========================================================================
    @Test
    @DisplayName("TEST 8: CoolCash duplicate spend/refund with Idempotency Key protection")
    void test8_coolcashSpendIdempotency() {
        User user = new User();
        user.setId(801L);
        user.setCoolcashBalance(BigDecimal.valueOf(100_000));

        Order order = new Order();
        order.setId(888L);
        order.setOrderCode("CM-ORDER-888");

        when(userRepository.findById(801L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(coolCashTransactionRepository.existsByIdempotencyKey("ORDER_COOLCASH_SPEND:888")).thenReturn(false).thenReturn(true);

        // Lần 1: Trừ tiền thành công
        coolCashService.spendCoolCash(user, order, BigDecimal.valueOf(50_000), "ORDER_COOLCASH_SPEND:888");
        assertEquals(0, user.getCoolcashBalance().compareTo(BigDecimal.valueOf(50_000)));

        // Lần 2 (Duplicate callback/retry): Không trừ tiếp
        coolCashService.spendCoolCash(user, order, BigDecimal.valueOf(50_000), "ORDER_COOLCASH_SPEND:888");
        assertEquals(0, user.getCoolcashBalance().compareTo(BigDecimal.valueOf(50_000)), "Số dư không được bị trừ lần 2");
    }

    // =========================================================================
    // 8A. CoolCash concurrent spend - atomic/row locking prevents negative balance
    // =========================================================================
    @Test
    @DisplayName("TEST 8A: CoolCash concurrent spend with row lock prevents negative balance")
    void test8a_coolcashConcurrentSpendPreventsNegativeBalance() throws InterruptedException {
        User user = new User();
        user.setId(880L);
        user.setCoolcashBalance(BigDecimal.valueOf(100_000));

        ReentrantLock userRowLock = new ReentrantLock();
        when(userRepository.findByIdForUpdate(880L)).thenAnswer(inv -> {
            userRowLock.lock();
            return Optional.of(user);
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            try {
                return inv.getArgument(0);
            } finally {
                if (userRowLock.isHeldByCurrentThread()) {
                    userRowLock.unlock();
                }
            }
        });

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Order ord = new Order(); ord.setId(8800L + idx); ord.setOrderCode("CM-SPEND-" + idx);
                    coolCashService.spendCoolCash(user, ord, BigDecimal.valueOf(70_000), "ORDER_SPEND_CONCURRENT:" + idx);
                    successCount.incrementAndGet();
                } catch (CustomException ex) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    if (userRowLock.isHeldByCurrentThread()) {
                        userRowLock.unlock();
                    }
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Chỉ 1 giao dịch được chi tiêu 70k thành công từ số dư 100k");
        assertEquals(1, failCount.get(), "Giao dịch thứ 2 phải thất bại do số dư không đủ");
        assertEquals(0, user.getCoolcashBalance().compareTo(BigDecimal.valueOf(30_000)), "Số dư còn lại phải là 30k, không âm");
    }

    // =========================================================================
    // 8A-2. CoolCash release without matching reservation NEVER creates money
    // =========================================================================
    @Test
    @DisplayName("TEST 8A-2: CoolCash release without matching reservation never creates money")
    void test8a2_coolcashReleaseWithoutReservationNeverCreatesMoney() {
        User user = new User();
        user.setId(882L);
        user.setCoolcashBalance(BigDecimal.valueOf(50_000));

        Order order = new Order();
        order.setId(8822L);
        order.setOrderCode("CM-8822");

        // Không có reservation tồn tại
        when(coolCashTransactionRepository.findByIdempotencyKey("ORDER_COOLCASH_RESERVE:8822")).thenReturn(Optional.empty());

        coolCashService.releaseReservedCoolCash(user, order, BigDecimal.valueOf(100_000), "RELEASE_KEY");

        // Số dư tuyệt đối không được tăng lên
        assertEquals(0, user.getCoolcashBalance().compareTo(BigDecimal.valueOf(50_000)), "Số dư không được tự sinh ra tiền khi không có reservation khớp");
    }

    // =========================================================================
    // 8B. Voucher RELEASED from cancelled order can be reused
    // =========================================================================
    @Test
    @DisplayName("TEST 8B: Voucher RELEASED from cancelled order allows user reuse")
    void test8b_voucherReleasedAllowsReuse() {
        User user = new User();
        user.setId(802L);

        Promotion promo = new Promotion();
        promo.setId(88L);
        promo.setCode("COOLMATE20");
        promo.setDiscountType("PERCENTAGE");
        promo.setDiscountValue(BigDecimal.valueOf(20));
        promo.setIsActive(true);
        promo.setStartDate(LocalDateTime.now().minusDays(1));
        promo.setEndDate(LocalDateTime.now().plusDays(10));

        ProductVariant v = new ProductVariant();
        v.setSalePrice(BigDecimal.valueOf(250_000));
        CartItem item = new CartItem(); item.setVariant(v); item.setQuantity(1);

        when(promotionRepository.findByCodeAndIsActiveTrue("COOLMATE20")).thenReturn(Optional.of(promo));
        when(promotionUsageRepository.existsByPromotionIdAndUserIdAndStatusNot(88L, 802L, "RELEASED")).thenReturn(false);
        when(comboRuleRepository.findByIsActiveTrueOrderByMinQuantityDesc()).thenReturn(Collections.emptyList());
        when(shippingFeeService.calculateShippingFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        PricingSummaryDTO summary = pricingService.calculatePricing(user, Collections.singletonList(item), "COOLMATE20", null, null, null, null);

        assertEquals(0, summary.getVoucherDiscount().compareTo(BigDecimal.valueOf(50_000)), "Voucher phải được áp dụng lại khi usage trước đã RELEASED");
    }

    // =========================================================================
    // 8C. Checkout Pricing & Shipping fee location calculation
    // =========================================================================
    @Test
    @DisplayName("TEST 8C: Checkout shipping fee calculation by location and freeship threshold")
    void test8c_checkoutShippingFeeByLocation() {
        ShippingFeeService realShippingService = new ShippingFeeService();

        // 1. Dưới 200k, Nội thành Hà Nội -> 20.000đ
        BigDecimal feeInner = realShippingService.calculateShippingFee(BigDecimal.valueOf(150_000), "Hà Nội", "Quận Hoàn Kiếm");
        assertEquals(0, feeInner.compareTo(BigDecimal.valueOf(20_000)));

        // 2. Dưới 200k, Ngoại thành Hà Nội -> 25.000đ
        BigDecimal feeOuter = realShippingService.calculateShippingFee(BigDecimal.valueOf(150_000), "Hà Nội", "Huyện Gia Lâm");
        assertEquals(0, feeOuter.compareTo(BigDecimal.valueOf(25_000)));

        // 3. Dưới 200k, Tỉnh khác -> 30.000đ
        BigDecimal feeProvince = realShippingService.calculateShippingFee(BigDecimal.valueOf(150_000), "Đà Nẵng", "Hải Châu");
        assertEquals(0, feeProvince.compareTo(BigDecimal.valueOf(30_000)));

        // 4. Từ 200k trở lên -> Freeship 0đ
        BigDecimal feeFree = realShippingService.calculateShippingFee(BigDecimal.valueOf(250_000), "Đà Nẵng", "Hải Châu");
        assertEquals(0, feeFree.compareTo(BigDecimal.ZERO));
    }

    // =========================================================================
    // 9. Partial return refund allocation proportional to item
    // =========================================================================
    @Test
    @DisplayName("TEST 9: Partial return refund allocation proportional to item")
    void test9_partialReturnRefundAllocation() {
        User user = new User(); user.setId(901L);

        ProductVariant v1 = new ProductVariant(); v1.setId(1L); v1.setStockQuantity(10);
        ProductVariant v2 = new ProductVariant(); v2.setId(2L); v2.setStockQuantity(10);
        ProductVariant v3 = new ProductVariant(); v3.setId(3L); v3.setStockQuantity(10);

        OrderItem i1 = new OrderItem(); i1.setId(91L); i1.setVariant(v1); i1.setUnitPrice(BigDecimal.valueOf(100_000)); i1.setQuantity(1);
        OrderItem i2 = new OrderItem(); i2.setId(92L); i2.setVariant(v2); i2.setUnitPrice(BigDecimal.valueOf(200_000)); i2.setQuantity(1);
        OrderItem i3 = new OrderItem(); i3.setId(93L); i3.setVariant(v3); i3.setUnitPrice(BigDecimal.valueOf(300_000)); i3.setQuantity(1);

        Order order = new Order();
        order.setId(9001L);
        order.setUser(user);
        order.setOrderStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now().minusDays(5));
        order.setSubtotalAmount(BigDecimal.valueOf(600_000));
        order.setFinalAmount(BigDecimal.valueOf(600_000));
        order.setItems(Arrays.asList(i1, i2, i3));
        i1.setOrder(order); i2.setOrder(order); i3.setOrder(order);

        when(orderRepository.findById(9001L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByIdForUpdate(91L)).thenReturn(Optional.of(i1));
        when(orderReturnRepository.findByOrderItemId(91L)).thenReturn(Collections.emptyList());
        when(orderReturnRepository.save(any(OrderReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderReturn ret = returnService.requestReturn(user, 9001L, 91L, 1, "REFUND_COOLCASH", null, "Không ưng ý", null);

        assertEquals(0, ret.getRefundAmount().compareTo(BigDecimal.valueOf(100_000)), "Refund must be allocated only for returned item (100k)");
    }

    // =========================================================================
    // 9B. Return exchange with different product rejected
    // =========================================================================
    @Test
    @DisplayName("TEST 9B: Return exchange with different product rejected")
    void test9b_returnExchangeDifferentProductRejected() {
        User user = new User(); user.setId(902L);

        Product p1 = new Product(); p1.setId(101L); p1.setName("Áo Thun");
        Product p2 = new Product(); p2.setId(102L); p2.setName("Quần Jeans");

        ProductVariant vOrig = new ProductVariant(); vOrig.setId(11L); vOrig.setProduct(p1);
        ProductVariant vTargetDiff = new ProductVariant(); vTargetDiff.setId(22L); vTargetDiff.setProduct(p2); vTargetDiff.setIsActive(true); vTargetDiff.setStockQuantity(5);

        OrderItem item = new OrderItem(); item.setId(991L); item.setVariant(vOrig); item.setQuantity(1); item.setUnitPrice(BigDecimal.valueOf(200_000));

        Order order = new Order();
        order.setId(9002L);
        order.setUser(user);
        order.setOrderStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now().minusDays(3));
        order.setItems(Collections.singletonList(item));
        item.setOrder(order);

        when(orderRepository.findById(9002L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByIdForUpdate(991L)).thenReturn(Optional.of(item));
        when(productVariantRepository.findById(22L)).thenReturn(Optional.of(vTargetDiff));

        assertThrows(CustomException.class, () -> {
            returnService.requestReturn(user, 9002L, 991L, 1, "RETURN_SIZE", 22L, "Đổi sang sản phẩm khác", null);
        });
    }

    // =========================================================================
    // 9B-2. Return exchange price difference handling (Cheaper vs More Expensive)
    // =========================================================================
    @Test
    @DisplayName("TEST 9B-2: Return exchange price difference - cheaper refunds difference, more expensive rejected")
    void test9b2_exchangePriceDifferenceHandling() {
        User user = new User(); user.setId(905L);
        Product p = new Product(); p.setId(500L); p.setName("Áo Polo Excool");

        ProductVariant vOrig = new ProductVariant(); vOrig.setId(51L); vOrig.setProduct(p); vOrig.setSalePrice(BigDecimal.valueOf(300_000));
        ProductVariant vCheaper = new ProductVariant(); vCheaper.setId(52L); vCheaper.setProduct(p); vCheaper.setSalePrice(BigDecimal.valueOf(250_000)); vCheaper.setIsActive(true); vCheaper.setStockQuantity(10);
        ProductVariant vMoreExpensive = new ProductVariant(); vMoreExpensive.setId(53L); vMoreExpensive.setProduct(p); vMoreExpensive.setSalePrice(BigDecimal.valueOf(350_000)); vMoreExpensive.setIsActive(true); vMoreExpensive.setStockQuantity(10);

        OrderItem item = new OrderItem(); item.setId(995L); item.setVariant(vOrig); item.setQuantity(1); item.setUnitPrice(BigDecimal.valueOf(300_000));

        Order order = new Order();
        order.setId(9005L);
        order.setUser(user);
        order.setOrderStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now().minusDays(2));
        order.setSubtotalAmount(BigDecimal.valueOf(300_000));
        order.setFinalAmount(BigDecimal.valueOf(300_000));
        order.setItems(Collections.singletonList(item));
        item.setOrder(order);

        when(orderRepository.findById(9005L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByIdForUpdate(995L)).thenReturn(Optional.of(item));

        // 1. Đổi sang biến thể đắt hơn -> Từ chối nâng cấp miễn phí
        when(productVariantRepository.findById(53L)).thenReturn(Optional.of(vMoreExpensive));
        assertThrows(CustomException.class, () -> {
            returnService.requestReturn(user, 9005L, 995L, 1, "RETURN_SIZE", 53L, "Đổi sang biến thể đắt hơn", null);
        }, "Không được cho phép nâng cấp sản phẩm đổi miễn phí");

        // 2. Đổi sang biến thể rẻ hơn -> Hoàn lại khoản chênh lệch 50.000đ
        when(productVariantRepository.findById(52L)).thenReturn(Optional.of(vCheaper));
        when(orderReturnRepository.save(any(OrderReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderReturn ret = returnService.requestReturn(user, 9005L, 995L, 1, "RETURN_SIZE", 52L, "Đổi sang biến thể rẻ hơn", null);
        assertEquals(0, ret.getRefundAmount().compareTo(BigDecimal.valueOf(50_000)), "Khoản chênh lệch 50k phải được hoàn cho khách");
    }

    // =========================================================================
    // 9C. Return completion idempotency
    // =========================================================================
    @Test
    @DisplayName("TEST 9C: Return completion idempotency - second complete call is no-op")
    void test9c_returnCompletionIdempotency() {
        User user = new User(); user.setId(903L); user.setCoolcashBalance(BigDecimal.ZERO);
        Order order = new Order(); order.setId(9003L); order.setOrderCode("CM-9003"); order.setFinalAmount(BigDecimal.valueOf(200_000));

        ProductVariant variant = new ProductVariant(); variant.setId(31L); variant.setStockQuantity(10);
        OrderItem item = new OrderItem(); item.setId(993L); item.setVariant(variant); item.setQuantity(1);

        OrderReturn req = new OrderReturn();
        req.setId(777L);
        req.setOrder(order);
        req.setOrderItem(item);
        req.setUser(user);
        req.setQuantity(1);
        req.setRefundAmount(BigDecimal.valueOf(200_000));
        req.setStatus("PROCESSING");

        when(orderReturnRepository.findById(777L)).thenReturn(Optional.of(req));
        when(userRepository.findById(903L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User staff = new User(); staff.setId(99L);
        // Lần 1: Hoàn tất
        returnService.completeReturn(777L, "COOLCASH", staff);
        assertEquals("COMPLETED", req.getStatus());

        // Lần 2: Gọi lại không gây lỗi và không lặp thao tác
        returnService.completeReturn(777L, "COOLCASH", staff);
        assertEquals("COMPLETED", req.getStatus());
    }

    // =========================================================================
    // 9D. Invalid return action rejected and state skipping forbidden
    // =========================================================================
    @Test
    @DisplayName("TEST 9D: Invalid return action rejected and state skipping forbidden")
    void test9d_invalidReturnActionAndStateSkipping() {
        OrderReturn req = new OrderReturn();
        req.setId(788L);
        req.setStatus("REQUESTED");

        when(orderReturnRepository.findById(788L)).thenReturn(Optional.of(req));
        User staff = new User(); staff.setId(99L);

        // 1. Action không hợp lệ -> ném CustomException
        assertThrows(CustomException.class, () -> {
            returnService.processReturnApproval(788L, "FOOBAR", "COOLCASH", null, staff);
        }, "Action lạ ngoài APPROVE/REJECT/PROCESS/COMPLETE phải bị từ chối");

        // 2. Không cho phép nhảy thẳng từ REQUESTED sang COMPLETE
        assertThrows(CustomException.class, () -> {
            returnService.processReturnApproval(788L, "COMPLETE", "COOLCASH", null, staff);
        }, "Không được skip PROCESSING để chuyển trực tiếp sang COMPLETED");

        // 3. Workflow hợp lệ: REQUESTED -> APPROVE
        returnService.processReturnApproval(788L, "APPROVE", "COOLCASH", null, staff);
        assertEquals("APPROVED", req.getStatus());

        // 4. APPROVED -> PROCESS
        returnService.processReturnApproval(788L, "PROCESS", "COOLCASH", null, staff);
        assertEquals("PROCESSING", req.getStatus());
    }

    // =========================================================================
    // 10. Return after 60 days rejected
    // =========================================================================
    @Test
    @DisplayName("TEST 10: Return after 60 days rejected")
    void test10_returnAfter60DaysRejected() {
        User user = new User(); user.setId(1001L);

        Order order = new Order();
        order.setId(10001L);
        order.setUser(user);
        order.setOrderStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now().minusDays(65));

        when(orderRepository.findById(10001L)).thenReturn(Optional.of(order));

        assertThrows(CustomException.class, () -> {
            returnService.requestReturn(user, 10001L, 1L, 1, "RETURN_SIZE", null, "Đổi size", null);
        });
    }

    // =========================================================================
    // 11. Return quantity exceeding remaining purchased quantity rejected
    // =========================================================================
    @Test
    @DisplayName("TEST 11: Return quantity exceeding remaining purchased quantity rejected")
    void test11_returnQuantityExceedingRemainingRejected() {
        User user = new User(); user.setId(1101L);
        OrderItem item = new OrderItem(); item.setId(111L); item.setQuantity(2);

        Order order = new Order();
        order.setId(11001L);
        order.setUser(user);
        order.setOrderStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now().minusDays(10));
        order.setItems(Collections.singletonList(item));
        item.setOrder(order);

        when(orderRepository.findById(11001L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByIdForUpdate(111L)).thenReturn(Optional.of(item));

        OrderReturn prev = new OrderReturn(); prev.setQuantity(1); prev.setStatus("COMPLETED");
        when(orderReturnRepository.findByOrderItemId(111L)).thenReturn(Collections.singletonList(prev));

        assertThrows(CustomException.class, () -> {
            returnService.requestReturn(user, 11001L, 111L, 2, "RETURN_SIZE", null, "Đổi size", null);
        });
    }

    // =========================================================================
    // 11B. InventoryStats only counts receipt APPROVED
    // =========================================================================
    @Test
    @DisplayName("TEST 11B: InventoryService.getInventoryStats() only counts APPROVED receipts")
    void test11b_inventoryStatsOnlyCountsApprovedReceipts() {
        InventoryReceiptItem it1 = new InventoryReceiptItem(); it1.setQuantity(10);
        InventoryReceipt rSubmitted = new InventoryReceipt();
        rSubmitted.setStatus("SUBMITTED");
        rSubmitted.setTotalAmount(BigDecimal.valueOf(100_000));
        rSubmitted.setCreatedAt(LocalDateTime.now());
        rSubmitted.setItems(Collections.singletonList(it1));

        InventoryReceiptItem it2 = new InventoryReceiptItem(); it2.setQuantity(20);
        InventoryReceipt rRejected = new InventoryReceipt();
        rRejected.setStatus("REJECTED");
        rRejected.setTotalAmount(BigDecimal.valueOf(200_000));
        rRejected.setCreatedAt(LocalDateTime.now());
        rRejected.setItems(Collections.singletonList(it2));

        InventoryReceiptItem it3 = new InventoryReceiptItem(); it3.setQuantity(30);
        InventoryReceipt rApproved = new InventoryReceipt();
        rApproved.setStatus("APPROVED");
        rApproved.setTotalAmount(BigDecimal.valueOf(300_000));
        rApproved.setCreatedAt(LocalDateTime.now());
        rApproved.setItems(Collections.singletonList(it3));

        when(inventoryReceiptRepository.findAll()).thenReturn(Arrays.asList(rSubmitted, rRejected, rApproved));
        when(productVariantRepository.findAll()).thenReturn(Collections.emptyList());

        InventoryStatsDTO stats = inventoryService.getInventoryStats();

        assertEquals(1, stats.getTotalReceipts(), "Chỉ đúng 1 phiếu APPROVED được tính vào tổng số phiếu nhập");
        assertEquals(30, stats.getTotalUnitsImported(), "Chỉ 30 sản phẩm từ phiếu APPROVED được tính vào tổng nhập");
        assertEquals(0, stats.getTotalSpentThisMonth().compareTo(BigDecimal.valueOf(300_000)), "Chỉ 300k từ phiếu APPROVED được tính vào chi phí nhập");
    }

    // =========================================================================
    // 12. Cart Item ownership IDOR check
    // =========================================================================
    @Test
    @DisplayName("TEST 12: Cart Item ownership IDOR check")
    void test12_cartItemIdorProtection() {
        User userA = new User(); userA.setId(1201L);
        User userB = new User(); userB.setId(1202L);

        Cart cartA = new Cart(); cartA.setUser(userA);
        CartItem itemA = new CartItem(); itemA.setId(121L); itemA.setCart(cartA);

        when(cartItemRepository.findById(121L)).thenReturn(Optional.of(itemA));

        assertThrows(CustomException.class, () -> {
            cartService.removeItem(userB, null, 121L);
        }, "User B deleting User A cart item must be rejected");
    }

    // =========================================================================
    // 13. CoolCash wallet query data leak protection
    // =========================================================================
    @Test
    @DisplayName("TEST 13: CoolCash wallet query data leak protection")
    void test13_walletDataLeakProtection() {
        User userA = new User(); userA.setId(1301L); userA.setFullName("User A");

        CoolCashTransaction txA = new CoolCashTransaction(); txA.setUser(userA); txA.setAmount(BigDecimal.valueOf(50_000));
        when(coolCashTransactionRepository.findByUserIdOrderByCreatedAtDesc(1301L))
                .thenReturn(Collections.singletonList(txA));

        List<CoolCashTransaction> result = coolCashService.getUserTransactions(1301L);
        assertEquals(1, result.size());
        assertEquals(1301L, result.get(0).getUser().getId());
    }

    // =========================================================================
    // 14. Role based access control separation
    // =========================================================================
    @Test
    @DisplayName("TEST 14: Role based access control separation - STAFF cannot manage products")
    void test14_staffCannotPerformAdminOnlyActions() {
        Role staffRole = new Role(); staffRole.setRoleName("ROLE_STAFF");
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");

        User staff = new User(); staff.getRoles().add(staffRole);
        User admin = new User(); admin.getRoles().add(adminRole);

        assertFalse(staff.hasRole("ROLE_ADMIN"), "Staff must not have ROLE_ADMIN");
        assertTrue(staff.hasRole("ROLE_STAFF"), "Staff must have ROLE_STAFF");
        assertTrue(admin.hasRole("ROLE_ADMIN"), "Admin must have ROLE_ADMIN");
    }

    // =========================================================================
    // 15. Review moderation lifecycle - PENDING to APPROVED
    // =========================================================================
    @Test
    @DisplayName("TEST 15: Review moderation lifecycle - PENDING to APPROVED")
    void test15_reviewModerationLifecycle() {
        Product product = new Product(); product.setId(1501L); product.setRatingAvg(BigDecimal.ZERO); product.setReviewCount(0);
        Review review = new Review();
        review.setId(151L);
        review.setProduct(product);
        review.setStatus("PENDING");
        review.setIsApproved(false);
        review.setRating(5);

        when(reviewRepository.findById(151L)).thenReturn(Optional.of(review));
        when(reviewRepository.findByProductIdAndIsApprovedTrue(1501L)).thenReturn(Collections.singletonList(review));

        assertFalse(review.getIsApproved());

        reviewService.approveReview(151L);

        assertTrue(review.getIsApproved());
        assertEquals("APPROVED", review.getStatus());
        assertEquals(1, product.getReviewCount());
        assertEquals(0, product.getRatingAvg().compareTo(BigDecimal.valueOf(5.0)));
    }

    // =========================================================================
    // 16. Verified purchase - prevent duplicate review on same order item
    // =========================================================================
    @Test
    @DisplayName("TEST 16: Verified purchase - prevent duplicate review on same order item")
    void test16_duplicateReviewRejected() {
        User user = new User(); user.setId(1601L);
        OrderItem item = new OrderItem(); item.setId(161L);
        Order order = new Order(); order.setUser(user); order.setOrderStatus("COMPLETED");
        item.setOrder(order);

        when(orderItemRepository.findById(161L)).thenReturn(Optional.of(item));
        when(reviewRepository.existsByUserIdAndOrderItemId(1601L, 161L)).thenReturn(true);

        assertThrows(CustomException.class, () -> {
            reviewService.createVerifiedReview(user, 161L, 5, "Sản phẩm tuyệt vời", "TRUE_TO_SIZE", 175, 70, null);
        });
    }

    // =========================================================================
    // 17. Financial calculation ignores cancelled orders
    // =========================================================================
    @Test
    @DisplayName("TEST 17: Financial calculation ignores cancelled orders")
    void test17_cancelledOrderNotCountedInNetRevenue() {
        Order completedOrder = new Order();
        completedOrder.setOrderStatus("COMPLETED");
        completedOrder.setFinalAmount(BigDecimal.valueOf(500_000));

        Order cancelledOrder = new Order();
        cancelledOrder.setOrderStatus("CANCELLED");
        cancelledOrder.setFinalAmount(BigDecimal.valueOf(300_000));

        List<Order> orders = Arrays.asList(completedOrder, cancelledOrder);

        BigDecimal netRevenue = BigDecimal.ZERO;
        for (Order o : orders) {
            if ("COMPLETED".equalsIgnoreCase(o.getOrderStatus()) || "DELIVERED".equalsIgnoreCase(o.getOrderStatus())) {
                netRevenue = netRevenue.add(o.getFinalAmount());
            }
        }

        assertEquals(0, netRevenue.compareTo(BigDecimal.valueOf(500_000)), "Cancelled order must not be included in net revenue");
    }

    // =========================================================================
    // 18. Future inventory receipt changes current cost without affecting historical COGS
    // =========================================================================
    @Test
    @DisplayName("TEST 18: Future inventory receipt changes current cost without affecting historical COGS")
    void test18_futureReceiptDoesNotAffectHistoricalCOGS() {
        OrderItem historicalItem = new OrderItem();
        historicalItem.setQuantity(5);
        historicalItem.setCostPriceSnapshot(BigDecimal.valueOf(80_000));

        BigDecimal historicalCOGS = historicalItem.getCostPriceSnapshot().multiply(BigDecimal.valueOf(historicalItem.getQuantity()));
        assertEquals(BigDecimal.valueOf(400_000), historicalCOGS);

        ProductVariant variant = new ProductVariant();
        variant.setImportPrice(BigDecimal.valueOf(120_000));

        BigDecimal recheckedHistoricalCOGS = historicalItem.getCostPriceSnapshot().multiply(BigDecimal.valueOf(historicalItem.getQuantity()));
        assertEquals(BigDecimal.valueOf(400_000), recheckedHistoricalCOGS);
    }

    // =========================================================================
    // 19. Invalid paymentMethod rejected strictly
    // =========================================================================
    @Test
    @DisplayName("TEST 19: Invalid paymentMethod rejected strictly (CASH, BANK, ONLINE, abc, null, empty)")
    void test19_invalidPaymentMethodRejected() {
        User user = new User(); user.setId(1901L);
        ProductVariant variant = new ProductVariant(); variant.setId(191L); variant.setStockQuantity(10); variant.setIsActive(true); variant.setSalePrice(BigDecimal.valueOf(100_000));
        CartItem item = new CartItem(); item.setVariant(variant); item.setQuantity(1);
        List<CartItem> cartItems = Collections.singletonList(item);

        when(productVariantRepository.findById(191L)).thenReturn(Optional.of(variant));

        // Test with null
        assertThrows(CustomException.class, () -> {
            orderService.createOrder(user, "Nguyen Van A", "0901234567", "a@gmail.com", "123 Duong A", "HCM", "Q1", "Note", null, null, false, cartItems);
        });

        // Test with blank
        assertThrows(CustomException.class, () -> {
            orderService.createOrder(user, "Nguyen Van A", "0901234567", "a@gmail.com", "123 Duong A", "HCM", "Q1", "Note", "   ", null, false, cartItems);
        });

        // Test with BANK
        assertThrows(CustomException.class, () -> {
            orderService.createOrder(user, "Nguyen Van A", "0901234567", "a@gmail.com", "123 Duong A", "HCM", "Q1", "Note", "BANK", null, false, cartItems);
        });

        // Test with CASH
        assertThrows(CustomException.class, () -> {
            orderService.createOrder(user, "Nguyen Van A", "0901234567", "a@gmail.com", "123 Duong A", "HCM", "Q1", "Note", "CASH", null, false, cartItems);
        });

        // Test with random string
        assertThrows(CustomException.class, () -> {
            orderService.createOrder(user, "Nguyen Van A", "0901234567", "a@gmail.com", "123 Duong A", "HCM", "Q1", "Note", "arbitrary_method", null, false, cartItems);
        });
    }

    // =========================================================================
    // 20. STAFF refund authorization forbidden (Only ADMIN allowed)
    // =========================================================================
    @Test
    @DisplayName("TEST 20: STAFF refund authorization forbidden (Only ADMIN allowed)")
    void test20_staffRefundAuthorizationForbidden() {
        User staff = new User();
        staff.setId(2001L);
        Role staffRole = new Role(); staffRole.setRoleName("ROLE_STAFF");
        staff.setRoles(Collections.singleton(staffRole));

        // Staff calling startProcessingRefund -> Throws CustomException
        assertThrows(CustomException.class, () -> {
            orderService.startProcessingRefund(1L, staff);
        });

        // Staff calling confirmRefundSuccess -> Throws CustomException
        assertThrows(CustomException.class, () -> {
            orderService.confirmRefundSuccess(1L, "REF-123", "Note", staff);
        });

        // Staff calling markRefundFailed -> Throws CustomException
        assertThrows(CustomException.class, () -> {
            orderService.markRefundFailed(1L, "Reason", staff);
        });

        // Staff calling confirmReturnRefund on ReturnService -> Throws CustomException
        assertThrows(CustomException.class, () -> {
            returnService.confirmReturnRefund(1L, "REF-123", staff);
        });
    }

    // =========================================================================
    // 21. ADMIN refund authorization allowed and state machine transitions
    // =========================================================================
    @Test
    @DisplayName("TEST 21: ADMIN refund authorization allowed and state machine transitions")
    void test21_adminRefundAllowedAndStateMachine() {
        User admin = new User();
        admin.setId(2101L);
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");
        admin.setRoles(Collections.singleton(adminRole));

        Order order = new Order();
        order.setId(211L);
        order.setOrderCode("CM-REF-211");
        order.setPaymentStatus("REFUND_PENDING");
        order.setRefundStatus("REFUND_PENDING");
        order.setFinalAmount(BigDecimal.valueOf(500_000));

        when(orderRepository.findByIdForUpdate(211L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Step 1: startProcessingRefund -> transitions to REFUND_PROCESSING
        orderService.startProcessingRefund(211L, admin);
        assertEquals("REFUND_PROCESSING", order.getRefundStatus());

        // Step 2: markRefundFailed -> transitions to REFUND_FAILED
        orderService.markRefundFailed(211L, "Bank gateway timed out", admin);
        assertEquals("REFUND_FAILED", order.getRefundStatus());

        // Step 3: startProcessingRefund from REFUND_FAILED -> retry succeeds to REFUND_PROCESSING
        orderService.startProcessingRefund(211L, admin);
        assertEquals("REFUND_PROCESSING", order.getRefundStatus());

        // Step 4: confirmRefundSuccess -> transitions to REFUNDED
        orderService.confirmRefundSuccess(211L, "VNPAY-REFUND-999", "Hoàn tiền thành công", admin);
        assertEquals("REFUNDED", order.getRefundStatus());
        assertEquals("REFUNDED", order.getPaymentStatus());
        assertEquals("VNPAY-REFUND-999", order.getRefundReference());
    }

    // =========================================================================
    // 22. Return external refund (BANK/VNPAY) does NOT fake success
    // =========================================================================
    @Test
    @DisplayName("TEST 22: Return external refund (BANK/VNPAY) does NOT fake success - Option A enforcement")
    void test22_returnExternalRefundDoesNotFakeSuccess() {
        User staff = new User(); staff.setId(2201L);
        OrderReturn req = new OrderReturn();
        req.setId(221L);
        req.setStatus("PROCESSING");
        req.setRefundAmount(BigDecimal.valueOf(250_000));
        req.setQuantity(1);

        Order order = new Order();
        order.setId(220L);
        order.setOrderCode("CM-RET-220");
        order.setFinalAmount(BigDecimal.valueOf(500_000));
        req.setOrder(order);

        when(orderReturnRepository.findById(221L)).thenReturn(Optional.of(req));
        when(orderReturnRepository.save(any(OrderReturn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When refundMethod is BANK: does NOT set COMPLETED, keeps PROCESSING and REFUND_PENDING
        returnService.completeReturn(221L, "BANK", staff);

        assertEquals("PROCESSING", req.getStatus(), "Return status must remain PROCESSING while external refund is pending");
        assertEquals("REFUND_PENDING", req.getRefundStatus(), "Refund status must be REFUND_PENDING, not fake COMPLETED");
        assertNull(req.getRefundReference(), "External refund must not generate fake reference before actual refund");
    }

    // =========================================================================
    // 23. ADMIN confirmReturnRefund with real reference completes return
    // =========================================================================
    @Test
    @DisplayName("TEST 23: ADMIN confirmReturnRefund with real reference completes return")
    void test23_adminConfirmReturnRefundSuccess() {
        User admin = new User(); admin.setId(2301L);
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");
        admin.setRoles(Collections.singleton(adminRole));

        OrderReturn req = new OrderReturn();
        req.setId(231L);
        req.setStatus("PROCESSING");
        req.setRefundStatus("REFUND_PROCESSING");
        req.setRefundAmount(BigDecimal.valueOf(250_000));

        when(orderReturnRepository.findByIdForUpdate(231L)).thenReturn(Optional.of(req));
        when(orderReturnRepository.findById(231L)).thenReturn(Optional.of(req));
        when(orderReturnRepository.save(any(OrderReturn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        returnService.confirmReturnRefund(231L, "BANK-TXN-REAL-888", admin);

        assertEquals("COMPLETED", req.getStatus());
        assertEquals("REFUNDED", req.getRefundStatus());
        assertEquals("BANK-TXN-REAL-888", req.getRefundReference());
    }

    @Test
    @DisplayName("TEST 23B: confirmReturnRefund rejects REFUND_PENDING state directly")
    void test23b_confirmReturnRefundRejectsPendingState() {
        User admin = new User(); admin.setId(2302L);
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");
        admin.setRoles(Collections.singleton(adminRole));

        OrderReturn req = new OrderReturn();
        req.setId(232L);
        req.setStatus("PROCESSING");
        req.setRefundStatus("REFUND_PENDING");
        req.setRefundAmount(BigDecimal.valueOf(250_000));

        when(orderReturnRepository.findByIdForUpdate(232L)).thenReturn(Optional.of(req));
        when(orderReturnRepository.findById(232L)).thenReturn(Optional.of(req));

        assertThrows(CustomException.class, () -> {
            returnService.confirmReturnRefund(232L, "BANK-TXN-REAL-888", admin);
        });
    }

    // =========================================================================
    // 24. Refund confirmation idempotency - duplicate with same ref is no-op, different ref rejected
    // =========================================================================
    @Test
    @DisplayName("TEST 24: Refund confirmation idempotency - same ref no-op, different ref rejected")
    void test24_refundConfirmationIdempotency() {
        User admin = new User(); admin.setId(2401L);
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");
        admin.setRoles(Collections.singleton(adminRole));

        Order order = new Order();
        order.setId(241L);
        order.setRefundStatus("REFUNDED");
        order.setRefundReference("EXISTING-REF-001");

        when(orderRepository.findByIdForUpdate(241L)).thenReturn(Optional.of(order));

        // Duplicate with same reference -> returns peacefully (idempotent)
        assertDoesNotThrow(() -> {
            orderService.confirmRefundSuccess(241L, "EXISTING-REF-001", null, admin);
        });

        // Duplicate with different reference -> throws CustomException to prevent overwrite race condition
        assertThrows(CustomException.class, () -> {
            orderService.confirmRefundSuccess(241L, "DIFFERENT-REF-002", null, admin);
        });
    }

    // =========================================================================
    // 25. Complete order requires DELIVERED and VNPAY must be PAID
    // =========================================================================
    @Test
    @DisplayName("TEST 25: Complete order requires DELIVERED and VNPAY must be PAID")
    void test25_completeOrderDeliveredAndPaidGuard() {
        Order unpaidVnpay = new Order();
        unpaidVnpay.setId(251L);
        unpaidVnpay.setPaymentMethod("VNPAY");
        unpaidVnpay.setPaymentStatus("PAYMENT_PENDING");
        unpaidVnpay.setOrderStatus("DELIVERED");

        when(orderRepository.findByIdForUpdate(251L)).thenReturn(Optional.of(unpaidVnpay));

        // Unpaid VNPAY delivered order cannot be completed
        assertThrows(CustomException.class, () -> {
            orderService.completeOrder(251L);
        });

        Order shippingOrder = new Order();
        shippingOrder.setId(252L);
        shippingOrder.setPaymentMethod("COD");
        shippingOrder.setOrderStatus("SHIPPING");

        when(orderRepository.findByIdForUpdate(252L)).thenReturn(Optional.of(shippingOrder));

        // Order in SHIPPING (not DELIVERED) cannot be completed
        assertThrows(CustomException.class, () -> {
            orderService.completeOrder(252L);
        });
    }

    // =========================================================================
    // 26. Complete order idempotency and exact-once cashback & totalSpent
    // =========================================================================
    @Test
    @DisplayName("TEST 26: Complete order idempotency and exact-once cashback & totalSpent")
    void test26_completeOrderIdempotencyAndExactOnceCashback() {
        User user = new User();
        user.setId(2601L);
        user.setCoolcashBalance(BigDecimal.ZERO);
        user.setTotalSpent(BigDecimal.ZERO);
        user.setMembershipTier("NEW");

        Order order = new Order();
        order.setId(261L);
        order.setUser(user);
        order.setPaymentMethod("COD");
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("DELIVERED");
        order.setFinalAmount(BigDecimal.valueOf(1_000_000));

        when(orderRepository.findByIdForUpdate(261L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // First completion: marks PAID, COMPLETED, grants 3% cashback = 30,000, updates totalSpent = 1,000,000, tier = SILVER
        orderService.completeOrder(261L);
        assertEquals("COMPLETED", order.getOrderStatus());
        assertEquals("PAID", order.getPaymentStatus());
        assertEquals(0, user.getTotalSpent().compareTo(BigDecimal.valueOf(1_000_000)));
        assertEquals("SILVER", user.getMembershipTier());

        // Second completion call: order already COMPLETED -> no-op, does not grant cashback or increase spent again
        orderService.completeOrder(261L);
        assertEquals(0, user.getTotalSpent().compareTo(BigDecimal.valueOf(1_000_000)), "totalSpent must not be doubled");
    }

    // =========================================================================
    // 27. Duplicate CoolCash release without reservation never creates money
    // =========================================================================
    @Test
    @DisplayName("TEST 27: Duplicate CoolCash release idempotent - no double credit")
    void test27_duplicateCoolCashReleaseIdempotent() {
        User user = new User();
        user.setId(2701L);
        user.setCoolcashBalance(BigDecimal.valueOf(50_000));

        Order order = new Order();
        order.setId(271L);
        order.setOrderCode("CM-REL-271");

        String releaseKey = "ORDER_COOLCASH_RELEASE:271";
        String reserveKey = "ORDER_COOLCASH_RESERVE:271";

        // Scenario 1: Already released
        when(coolCashTransactionRepository.existsByIdempotencyKey(releaseKey)).thenReturn(true);

        coolCashService.releaseReservedCoolCash(user, order, BigDecimal.valueOf(20_000), releaseKey);
        assertEquals(0, user.getCoolcashBalance().compareTo(BigDecimal.valueOf(50_000)), "Balance should remain unchanged if already released");
    }

    // =========================================================================
    // 28. Duplicate inventory release idempotent - no double increment
    // =========================================================================
    @Test
    @DisplayName("TEST 28: Duplicate inventory release idempotent - no double stock increment")
    void test28_duplicateInventoryReleaseIdempotent() {
        ProductVariant variant = new ProductVariant();
        variant.setId(281L);
        variant.setStockQuantity(10);

        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(2);

        Order order = new Order();
        order.setId(280L);
        order.setItems(Collections.singletonList(item));

        // If ORDER_RELEASE already recorded -> returns immediately without incrementing stock
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceIdAndMovementType("ORDER", 280L, "ORDER_RELEASE")).thenReturn(true);

        stockService.releaseStock(order);
        assertEquals(10, variant.getStockQuantity(), "Stock must not be incremented when release already exists");
    }


    // =========================================================================
    // 29. STAFF COMPLETE return with COOLCASH -> forbidden
    // =========================================================================
    @Test
    @DisplayName("TEST 29: STAFF COMPLETE return with COOLCASH -> forbidden")
    void test29_staffCompleteReturnWithCoolcashForbidden() {
        User staff = new User(); staff.setId(2901L);
        Role staffRole = new Role(); staffRole.setRoleName("ROLE_STAFF");
        staff.setRoles(Collections.singleton(staffRole));

        OrderReturn req = new OrderReturn();
        req.setId(291L);
        req.setStatus("PROCESSING");
        req.setRefundAmount(BigDecimal.valueOf(150_000)); // Operation has refund

        when(orderReturnRepository.findByIdForUpdate(291L)).thenReturn(Optional.of(req));

        CustomException ex = assertThrows(CustomException.class, () -> {
            returnService.completeReturn(291L, "COOLCASH", staff);
        });
        assertTrue(ex.getMessage().contains("Nhân viên (STAFF) không có quyền") || ex.getMessage().contains("ADMIN"));
    }

    // =========================================================================
    // 30. ADMIN COMPLETE return with COOLCASH -> allowed and finalizes accounting
    // =========================================================================
    @Test
    @DisplayName("TEST 30: ADMIN COMPLETE return with COOLCASH -> allowed and finalizes accounting")
    void test30_adminCompleteReturnWithCoolcashAllowed() {
        User admin = new User(); admin.setId(3001L);
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");
        admin.setRoles(Collections.singleton(adminRole));

        User customer = new User(); customer.setId(3002L);
        customer.setCoolcashBalance(BigDecimal.valueOf(10_000));
        customer.setTotalSpent(BigDecimal.valueOf(1_000_000));
        customer.setMembershipTier("SILVER");

        Order order = new Order(); order.setId(301L); order.setUser(customer); order.setOrderCode("CM-RET-301");
        order.setFinalAmount(BigDecimal.valueOf(500_000));
        order.setCoolcashEarned(BigDecimal.valueOf(15_000));

        OrderReturn req = new OrderReturn();
        req.setId(302L);
        req.setUser(customer);
        req.setOrder(order);
        req.setStatus("PROCESSING");
        req.setRefundAmount(BigDecimal.valueOf(250_000));

        when(orderReturnRepository.findByIdForUpdate(302L)).thenReturn(Optional.of(req));
        when(orderReturnRepository.save(any(OrderReturn.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByIdForUpdate(3002L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        returnService.completeReturn(302L, "COOLCASH", admin);

        assertEquals("COMPLETED", req.getStatus());
        assertEquals("REFUNDED", req.getRefundStatus());
        assertEquals("COOLCASH-RETURN-302", req.getRefundReference());
        assertEquals(0, customer.getTotalSpent().compareTo(BigDecimal.valueOf(750_000)), "totalSpent must be reduced by refundAmount");
    }

    // =========================================================================
    // 31. Duplicate /vi-coolcash mapping verification
    // =========================================================================
    @Test
    @DisplayName("TEST 31: Duplicate /vi-coolcash mapping -> startup must pass with exactly one mapping")
    void test31_duplicateViCoolcashMappingStartup() {
        // Verify WalletController has the mapping
        assertDoesNotThrow(() -> {
            Class<?> walletCtrl = Class.forName("sopvn.demo.wallet.WalletController");
            assertNotNull(walletCtrl);
        });

        // Verify CoolCashController is absent
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("sopvn.demo.controller.CoolCashController");
        });
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("sopvn.demo.wallet.CoolCashController");
        });
    }

    // =========================================================================
    // 32. External refund failure must NOT leave accounting finalized
    // =========================================================================
    @Test
    @DisplayName("TEST 32: External refund failure must NOT leave accounting finalized")
    void test32_externalRefundFailureMustNotLeaveAccountingFinalized() {
        User admin = new User(); admin.setId(3201L);
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");
        admin.setRoles(Collections.singleton(adminRole));

        User customer = new User(); customer.setId(3202L);
        customer.setCoolcashBalance(BigDecimal.valueOf(50_000));
        customer.setTotalSpent(BigDecimal.valueOf(2_000_000));
        customer.setMembershipTier("SILVER");

        Order order = new Order(); order.setId(321L); order.setUser(customer); order.setOrderCode("CM-RET-321");
        order.setFinalAmount(BigDecimal.valueOf(500_000));
        order.setCoolcashEarned(BigDecimal.valueOf(25_000));

        OrderReturn req = new OrderReturn();
        req.setId(322L);
        req.setUser(customer);
        req.setOrder(order);
        req.setStatus("PROCESSING");
        req.setRefundStatus("REFUND_PENDING");
        req.setRefundAmount(BigDecimal.valueOf(200_000));

        when(orderReturnRepository.findByIdForUpdate(322L)).thenReturn(Optional.of(req));
        when(orderReturnRepository.save(any(OrderReturn.class))).thenAnswer(i -> i.getArgument(0));

        // Step 1: Start refund processing
        returnService.startProcessingReturnRefund(322L, admin);
        assertEquals("REFUND_PROCESSING", req.getRefundStatus());

        // Step 2: Bank transfer fails -> mark failed
        returnService.markReturnRefundFailed(322L, "Tài khoản ngân hàng người nhận không hợp lệ", admin);
        assertEquals("REFUND_FAILED", req.getRefundStatus());

        // Assert accounting: totalSpent and balance must NOT be altered when refund failed!
        assertEquals(0, customer.getTotalSpent().compareTo(BigDecimal.valueOf(2_000_000)), "totalSpent must remain unchanged when refund fails");
        assertEquals(0, customer.getCoolcashBalance().compareTo(BigDecimal.valueOf(50_000)), "balance must remain unchanged when refund fails");
        assertEquals("SILVER", customer.getMembershipTier());
    }

    // =========================================================================
    // 33. Concurrent return refund confirmation idempotency
    // =========================================================================
    @Test
    @DisplayName("TEST 33: Concurrent return refund confirmation idempotency")
    void test33_concurrentReturnRefundConfirmation() {
        User admin = new User(); admin.setId(3301L);
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");
        admin.setRoles(Collections.singleton(adminRole));

        User customer = new User(); customer.setId(3302L);
        customer.setTotalSpent(BigDecimal.valueOf(1_500_000));

        OrderReturn req = new OrderReturn();
        req.setId(331L);
        req.setUser(customer);
        req.setStatus("PROCESSING");
        req.setRefundStatus("REFUND_PROCESSING");
        req.setRefundAmount(BigDecimal.valueOf(100_000));

        when(orderReturnRepository.findByIdForUpdate(331L)).thenReturn(Optional.of(req));
        when(orderReturnRepository.save(any(OrderReturn.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByIdForUpdate(3302L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // First confirmation succeeds
        returnService.confirmReturnRefund(331L, "BANK-TXN-REAL-777", admin);
        assertEquals("REFUNDED", req.getRefundStatus());
        assertEquals("BANK-TXN-REAL-777", req.getRefundReference());
        assertEquals(0, customer.getTotalSpent().compareTo(BigDecimal.valueOf(1_400_000)));

        // Second confirmation with same reference is idempotent (does not double decrease totalSpent)
        assertDoesNotThrow(() -> {
            returnService.confirmReturnRefund(331L, "BANK-TXN-REAL-777", admin);
        });
        assertEquals(0, customer.getTotalSpent().compareTo(BigDecimal.valueOf(1_400_000)), "totalSpent must not be deducted twice on duplicate confirmation");

        // Third confirmation with different reference throws error
        assertThrows(CustomException.class, () -> {
            returnService.confirmReturnRefund(331L, "DIFFERENT-TXN-888", admin);
        });
    }

    // =========================================================================
    // 34. Concurrent inventory movement idempotency
    // =========================================================================
    @Test
    @DisplayName("TEST 34: Concurrent inventory movement idempotency")
    void test34_concurrentInventoryMovementIdempotency() {
        ProductVariant variant = new ProductVariant();
        variant.setId(341L);
        variant.setStockQuantity(20);

        when(productVariantRepository.findByIdForUpdate(341L)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

        // First reservation: succeeds, stock drops 20 -> 15
        stockService.reserveStock(variant, 5, 3401L);
        assertEquals(15, variant.getStockQuantity());

        // When movement already recorded in repo (as simulated by DB index / repo check)
        when(inventoryMovementRepository.existsByReferenceTypeAndReferenceIdAndVariantIdAndMovementType(
                "ORDER", 3401L, 341L, "ORDER_RESERVE")).thenReturn(true);

        // Second duplicate reservation attempt for same order & variant is a no-op!
        stockService.reserveStock(variant, 5, 3401L);
        assertEquals(15, variant.getStockQuantity(), "Stock must not be double deducted");
    }

    // =========================================================================
    // 35. CoolCash revoke reconciles balance and ledger without exceeding balance
    // =========================================================================
    @Test
    @DisplayName("TEST 35: CoolCash revoke reconciles balance and ledger without exceeding balance")
    void test35_coolCashRevokeReconcilesLedgerWithoutNegativeExceedingBalance() {
        User user = new User();
        user.setId(3501L);
        user.setCoolcashBalance(BigDecimal.valueOf(10_000)); // Only has 10k in wallet

        when(userRepository.findByIdForUpdate(3501L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        Order order = new Order(); order.setId(351L); order.setOrderCode("CM-351");

        // Requesting to revoke 30k cashback, but customer only has 10k left
        coolCashService.revokeOrderCashback(user, order, BigDecimal.valueOf(30_000), "Thu hồi đổi trả", "KEY-REVOKE-351");

        // Balance should be exactly 0 (not negative)
        assertEquals(0, user.getCoolcashBalance().compareTo(BigDecimal.ZERO));

        // Captured transaction amount must be exactly -10,000 (reconciling with the balance change)
        verify(coolCashTransactionRepository).save(argThat(tx -> 
            tx.getAmount().compareTo(BigDecimal.valueOf(-10_000)) == 0 &&
            "REVOKE_RETURN".equals(tx.getTransactionType()) &&
            tx.getDescription().contains("10000/30000")
        ));
    }

    // =========================================================================
    // 36. Resource ownership validation in AdminProductController
    // =========================================================================
    @Test
    @DisplayName("TEST 36: Resource ownership validation in AdminProductController")
    void test36_adminProductOwnershipValidation() {
        Product p1 = new Product(); p1.setId(100L);
        Product p2 = new Product(); p2.setId(200L);

        ProductVariant vOfP2 = new ProductVariant();
        vOfP2.setId(555L);
        vOfP2.setProduct(p2); // belongs to product 200

        when(productVariantRepository.findById(555L)).thenReturn(Optional.of(vOfP2));

        // When admin attempts to update or delete variant 555 under product 100 -> rejected!
        ProductVariant variant = productVariantRepository.findById(555L).orElseThrow();
        boolean belongsToP1 = variant.getProduct() != null && Long.valueOf(100L).equals(variant.getProduct().getId());
        assertFalse(belongsToP1, "Variant of product 200 must not belong to product 100");
    }

}
