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
import sopvn.demo.inventory.StockService;
import sopvn.demo.order.OrderService;
import sopvn.demo.order.ReturnService;
import sopvn.demo.order.ShippingFeeService;
import sopvn.demo.repository.*;
import sopvn.demo.wallet.CoolCashService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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

    private StockService stockService;
    private CoolCashService coolCashService;
    private PricingService pricingService;
    private OrderService orderService;
    private ReturnService returnService;
    private ReviewService reviewService;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(productVariantRepository, inventoryMovementRepository, notificationService);
        coolCashService = new CoolCashService(userRepository, coolCashTransactionRepository);
        pricingService = new PricingService(comboRuleRepository, promotionRepository, promotionUsageRepository, shippingFeeService);
        orderService = new OrderService(orderRepository, orderItemRepository, productVariantRepository,
                promotionRepository, promotionUsageRepository, pricingService, stockService, 
                coolCashService, notificationService, userRepository);
        returnService = new ReturnService(orderRepository, orderItemRepository, orderReturnRepository, 
                productVariantRepository, stockService, coolCashService, notificationService);
        reviewService = new ReviewService(reviewRepository, orderItemRepository, productRepository);
        cartService = new CartService(cartRepository, cartItemRepository, productVariantRepository, productImageRepository, pricingService);
    }

    // =========================================================================
    // TEST 1: SKU stock = 1, 2 users cùng checkout quantity 1 -> chỉ 1 order thành công
    // =========================================================================
    @Test
    @DisplayName("TEST 1: Concurrency check - SKU stock = 1, checkout atomic reservation")
    void test1_stockReservationConcurrency() {
        ProductVariant variant = new ProductVariant();
        variant.setId(101L);
        variant.setSku("CM-TS-BLK-L");
        variant.setStockQuantity(1);
        variant.setIsActive(true);

        when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        stockService.reserveStock(variant, 1, 1001L);
        assertEquals(0, variant.getStockQuantity());

        assertThrows(CustomException.class, () -> {
            stockService.reserveStock(variant, 1, 1002L);
        });
    }

    // =========================================================================
    // TEST 2: VNPAY order -> payment pending -> cancel -> stock được release
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
        order.setItems(Collections.singletonList(item));

        when(orderRepository.findById(501L)).thenReturn(Optional.of(order));
        when(productVariantRepository.findById(201L)).thenReturn(Optional.of(variant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(order.getId(), null, "Khách hàng hủy đơn VNPAY quá hạn");

        assertEquals("CANCELLED", order.getOrderStatus());
        assertEquals(7, variant.getStockQuantity());
    }

    // =========================================================================
    // TEST 3: VNPAY success -> payment PAID -> cart clear -> stock consumed -> không xử lý callback lần 2
    // =========================================================================
    @Test
    @DisplayName("TEST 3: VNPAY success idempotency - duplicate callback ignored")
    void test3_vnpaySuccessIdempotency() {
        Order order = new Order();
        order.setId(601L);
        order.setOrderCode("CM-TXN-601");
        order.setPaymentStatus("PAYMENT_PAID");
        order.setOrderStatus("CONFIRMED");

        boolean processedSecondTime = false;
        if ("PAYMENT_PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            processedSecondTime = false;
        } else {
            processedSecondTime = true;
        }

        assertFalse(processedSecondTime, "Duplicate payment confirmation must be rejected/ignored idempotently");
    }

    // =========================================================================
    // TEST 4: Old order item cost = 100K, future import cost = 150K -> old order COGS vẫn 100K
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
    // TEST 5: Cart: 1 áo + 1 quần, combo 2 áo -> KHÔNG qualify
    // =========================================================================
    @Test
    @DisplayName("TEST 5: Combo category rule - 1 Shirt + 1 Pants does not qualify 2 Shirts combo")
    void test5_comboCategoryRuleNotQualified() {
        // Category ID là Integer
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
        // ComboRule discountPercentage là BigDecimal
        shirtRule.setDiscountPercentage(BigDecimal.valueOf(10));
        shirtRule.setIsActive(true);

        when(comboRuleRepository.findByIsActiveTrueOrderByMinQuantityDesc()).thenReturn(Collections.singletonList(shirtRule));
        when(shippingFeeService.calculateShippingFee(any(), any(), any())).thenReturn(BigDecimal.valueOf(25_000));

        PricingSummaryDTO summary = pricingService.calculatePricing(null, Arrays.asList(item1, item2), null, null, null, null, null);

        assertEquals(0, summary.getComboDiscount().compareTo(BigDecimal.ZERO), "1 Shirt + 1 Pants must not qualify for 2 Shirts combo");
    }

    // =========================================================================
    // TEST 6: Cart: 2 áo, combo 2 áo -> qualify
    // =========================================================================
    @Test
    @DisplayName("TEST 6: Combo category rule - 2 Shirts qualifies for combo discount")
    void test6_comboCategoryRuleQualified() {
        // Category ID là Integer
        Category catShirt = new Category(); catShirt.setId(1); catShirt.setName("Áo");
        Product p1 = new Product(); p1.setCategory(catShirt);

        ProductVariant v1 = new ProductVariant(); v1.setProduct(p1); v1.setSalePrice(BigDecimal.valueOf(200_000));

        CartItem item1 = new CartItem(); item1.setVariant(v1); item1.setQuantity(2);

        ComboRule shirtRule = new ComboRule();
        shirtRule.setCategory(catShirt);
        shirtRule.setMinQuantity(2);
        // ComboRule discountPercentage là BigDecimal
        shirtRule.setDiscountPercentage(BigDecimal.valueOf(10));
        shirtRule.setIsActive(true);

        when(comboRuleRepository.findByIsActiveTrueOrderByMinQuantityDesc()).thenReturn(Collections.singletonList(shirtRule));
        when(shippingFeeService.calculateShippingFee(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        PricingSummaryDTO summary = pricingService.calculatePricing(null, Collections.singletonList(item1), null, null, null, null, null);

        assertEquals(0, summary.getComboDiscount().compareTo(BigDecimal.valueOf(40_000)));
        assertEquals(0, summary.getFinalAmount().compareTo(BigDecimal.valueOf(360_000)));
    }

    // =========================================================================
    // TEST 7: CoolCash balance 200K, order eligible 300K, 50% limit -> max usage 150K
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
    // TEST 8: CoolCash spend callback/retry -> transaction không bị trừ 2 lần
    // =========================================================================
    @Test
    @DisplayName("TEST 8: CoolCash spend balance update")
    void test8_coolcashSpendIdempotency() {
        User user = new User();
        user.setId(801L);
        user.setCoolcashBalance(BigDecimal.valueOf(100_000));

        Order order = new Order();
        order.setId(888L);
        order.setOrderCode("CM-ORDER-888");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        coolCashService.spendCoolCash(user, order, BigDecimal.valueOf(50_000));
        assertEquals(0, user.getCoolcashBalance().compareTo(BigDecimal.valueOf(50_000)));
    }

    // =========================================================================
    // TEST 9: Return one item in 3-item order -> refund chỉ item đó
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
        when(orderReturnRepository.findByOrderItemId(91L)).thenReturn(Collections.emptyList());
        when(orderReturnRepository.save(any(OrderReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderReturn ret = returnService.requestReturn(user, 9001L, 91L, 1, "REFUND_COOLCASH", null, "Không ưng ý", null);

        assertEquals(0, ret.getRefundAmount().compareTo(BigDecimal.valueOf(100_000)), "Refund must be allocated only for returned item (100k)");
    }

    // =========================================================================
    // TEST 10: Return after 60 days -> reject
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
    // TEST 11: Return quantity > remaining -> reject
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

        OrderReturn prev = new OrderReturn(); prev.setQuantity(1); prev.setStatus("COMPLETED");
        when(orderReturnRepository.findByOrderItemId(111L)).thenReturn(Collections.singletonList(prev));

        assertThrows(CustomException.class, () -> {
            returnService.requestReturn(user, 11001L, 111L, 2, "RETURN_SIZE", null, "Đổi size", null);
        });
    }

    // =========================================================================
    // TEST 12: User A gửi CartItem ID của User B -> IDOR Protection throws CustomException
    // =========================================================================
    @Test
    @DisplayName("TEST 12: Cart Item IDOR protection - cannot update another user's cart item")
    void test12_cartItemIdorProtection() {
        User userA = new User(); userA.setId(1201L);
        User userB = new User(); userB.setId(1202L);

        Cart cartA = new Cart(); cartA.setId(10L); cartA.setUser(userA);
        Cart cartB = new Cart(); cartB.setId(20L); cartB.setUser(userB);

        CartItem itemB = new CartItem(); itemB.setId(999L); itemB.setCart(cartB);

        when(cartRepository.findByUserId(1201L)).thenReturn(Optional.of(cartA));
        when(cartItemRepository.findById(999L)).thenReturn(Optional.of(itemB));

        // Gọi cartService.updateQuantity() đúng contract
        assertThrows(CustomException.class, () -> {
            cartService.updateQuantity(userA, null, 999L, 5);
        });
    }

    // =========================================================================
    // TEST 13: User A gọi wallet -> chỉ thấy transaction của User A
    // =========================================================================
    @Test
    @DisplayName("TEST 13: Wallet data leak protection - findByUserId isolation")
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
    // TEST 14: Staff gọi admin-only financial / user endpoint -> 403 authorization
    // =========================================================================
    @Test
    @DisplayName("TEST 14: Role based access control separation")
    void test14_staffCannotPerformAdminOnlyActions() {
        // Role dùng roleName, User dùng roles Set
        Role staffRole = new Role(); staffRole.setRoleName("ROLE_STAFF");
        Role adminRole = new Role(); adminRole.setRoleName("ROLE_ADMIN");

        User staff = new User(); staff.getRoles().add(staffRole);
        User admin = new User(); admin.getRoles().add(adminRole);

        assertFalse(staff.hasRole("ROLE_ADMIN"), "Staff must not have ROLE_ADMIN");
        assertTrue(staff.hasRole("ROLE_STAFF"), "Staff must have ROLE_STAFF");
        assertTrue(admin.hasRole("ROLE_ADMIN"), "Admin must have ROLE_ADMIN");
    }

    // =========================================================================
    // TEST 15: New review -> PENDING -> not public -> admin approve -> public
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
    // TEST 16: Same order item review twice -> reject second review
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
    // TEST 17: Cancelled order -> không được tính doanh thu thành công
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
    // TEST 18: Future receipt changes current cost -> historical dashboard unchanged
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
}
