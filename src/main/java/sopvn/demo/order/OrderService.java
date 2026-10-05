package sopvn.demo.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.cart.PricingService;
import sopvn.demo.cart.dto.PricingSummaryDTO;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.core.service.NotificationService;
import sopvn.demo.entity.*;
import sopvn.demo.inventory.StockService;
import sopvn.demo.repository.*;
import sopvn.demo.wallet.CoolCashService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final PricingService pricingService;
    private final StockService stockService;
    private final CoolCashService coolCashService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductVariantRepository productVariantRepository,
                        PromotionRepository promotionRepository,
                        PromotionUsageRepository promotionUsageRepository,
                        PricingService pricingService,
                        StockService stockService,
                        CoolCashService coolCashService,
                        NotificationService notificationService,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.pricingService = pricingService;
        this.stockService = stockService;
        this.coolCashService = coolCashService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Tạo đơn hàng chuẩn nghiệp vụ D2C: Tính tiền server-side, giữ kho atomic, ghi snapshot cost
     */
    @Transactional
    public Order createOrder(User user, String recipientName, String phone, String email, String address, 
                            String provinceName, String districtName, String note,
                            List<CartItem> cartItems, String paymentMethod, String voucherCode, BigDecimal requestedCoolCash) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new CustomException("Giỏ hàng của bạn đang trống! Vui lòng chọn ít nhất 1 sản phẩm trước khi thanh toán.");
        }

        if (recipientName == null || recipientName.trim().isEmpty()) {
            throw new CustomException("Vui lòng nhập họ và tên người nhận hàng.");
        }

        if (phone == null || !phone.trim().matches("^(0[3|5|7|8|9])+([0-9]{8})$")) {
            throw new CustomException("Số điện thoại nhận hàng không hợp lệ. Vui lòng nhập số điện thoại gồm 10 chữ số.");
        }

        if (address == null || address.trim().length() < 6) {
            throw new CustomException("Vui lòng nhập địa chỉ nhận hàng chi tiết.");
        }

        // 1. Tính toán giá tiền Server-Side qua PricingService (Nguồn sự thật duy nhất)
        PricingSummaryDTO pricing = pricingService.calculatePricing(
                user, cartItems, voucherCode, requestedCoolCash, provinceName, districtName, paymentMethod
        );

        String orderCode = "CM" + System.currentTimeMillis();
        String txnRef = orderCode + "_" + System.currentTimeMillis();

        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setVnpayTxnRef(txnRef);
        order.setUser(user);
        order.setRecipientName(recipientName.trim());
        order.setRecipientPhone(phone.trim());
        order.setRecipientEmail(email != null ? email.trim() : (user != null ? user.getEmail() : null));
        order.setShippingAddress(address.trim());
        order.setNote(note != null ? note.trim() : null);

        order.setSubtotalAmount(pricing.getSubtotal());
        order.setComboDiscountAmount(pricing.getComboDiscount());
        order.setVoucherDiscountAmount(pricing.getVoucherDiscount());
        order.setCoolcashUsed(pricing.getCoolcashUsed());
        order.setShippingFee(pricing.getShippingFee());
        order.setFinalAmount(pricing.getFinalAmount());

        order.setPaymentMethod(paymentMethod);
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            order.setPaymentStatus("PAYMENT_PENDING"); // B1: Chưa set PAID trước khi thanh toán thành công
        } else {
            order.setPaymentStatus("UNPAID"); // COD
        }

        order.setOrderStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (pricing.getAppliedPromotion() != null) {
            order.setPromotion(pricing.getAppliedPromotion());
        }

        order = orderRepository.save(order);

        // 2. Tạo Order Items & Snapshot giá vốn (Historical COGS)
        List<OrderItem> savedItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            ProductVariant variant = productVariantRepository.findById(ci.getVariant().getId())
                    .orElseThrow(() -> new CustomException("Phiên bản sản phẩm không tồn tại"));

            String pName = variant.getProduct() != null ? variant.getProduct().getName() : "Sản phẩm Coolmate";
            String cName = variant.getColor() != null ? variant.getColor().getName() : "";
            String sName = variant.getSize() != null ? variant.getSize().getName() : "";

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setVariant(variant);
            oi.setProductNameSnapshot(pName);
            oi.setSkuSnapshot(variant.getSku());
            oi.setColorNameSnapshot(cName);
            oi.setSizeNameSnapshot(sName);
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(variant.getSalePrice());
            oi.setTotalPrice(variant.getSalePrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            // B3: Snapshot giá vốn lịch sử tại thời điểm đặt hàng
            oi.setCostPriceSnapshot(variant.getImportPrice() != null ? variant.getImportPrice() : BigDecimal.ZERO);
            oi.setIsReviewed(false);

            savedItems.add(orderItemRepository.save(oi));
        }
        order.setItems(savedItems);

        // 3. B2: Giữ tồn kho nguyên tử (Stock Reservation)
        stockService.reserveStock(savedItems, order);

        // 4. Trừ CoolCash nếu khách dùng
        if (user != null && pricing.getCoolcashUsed().compareTo(BigDecimal.ZERO) > 0) {
            coolCashService.spendCoolCash(user, order, pricing.getCoolcashUsed());
        }

        // 5. Ghi nhận lượt sử dụng Voucher nếu có
        if (pricing.getAppliedPromotion() != null) {
            Promotion promo = pricing.getAppliedPromotion();
            promo.setUsedCount(promo.getUsedCount() + 1);
            promotionRepository.save(promo);

            PromotionUsage usage = new PromotionUsage(promo, user, order);
            promotionUsageRepository.save(usage);
        }

        notificationService.notifyOrderCreated(order);
        return order;
    }

    /**
     * Hủy đơn hàng: Hoàn trả tồn kho (Stock Release), hoàn CoolCash, hoàn voucher
     */
    @Transactional
    public void cancelOrder(Long orderId, User requestUser, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng #" + orderId));

        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return; // Idempotent
        }

        if ("SHIPPING".equalsIgnoreCase(order.getOrderStatus()) || 
            "DELIVERED".equalsIgnoreCase(order.getOrderStatus()) || 
            "COMPLETED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new CustomException("Đơn hàng đang giao hoặc đã hoàn tất, không thể hủy.");
        }

        order.setOrderStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledReason(reason != null ? reason : "Hủy đơn hàng");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 1. Release tồn kho
        stockService.releaseStock(order);

        // 2. Hoàn lại CoolCash đã chi tiêu
        if (order.getUser() != null && order.getCoolcashUsed() != null && order.getCoolcashUsed().compareTo(BigDecimal.ZERO) > 0) {
            coolCashService.refundCoolCash(order.getUser(), order, order.getCoolcashUsed(), 
                    "Hoàn lại số dư ví do hủy đơn hàng #" + order.getOrderCode());
        }

        notificationService.notifyOrderCancelled(order, reason);
    }

    /**
     * Hoàn tất đơn hàng (COMPLETED): Trao thưởng hoàn tiền CoolClub 1 lần duy nhất (Idempotent)
     */
    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng #" + orderId));

        if ("COMPLETED".equalsIgnoreCase(order.getOrderStatus())) {
            return; // Đã hoàn tất rồi, không trao thưởng lần 2
        }

        order.setOrderStatus("COMPLETED");
        order.setPaymentStatus("PAID");
        if (order.getDeliveredAt() == null) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        order.setUpdatedAt(LocalDateTime.now());

        // Trao thưởng CoolCash theo hạng
        User user = order.getUser();
        if (user != null) {
            String tier = user.getMembershipTier() != null ? user.getMembershipTier().toUpperCase() : "NEW";
            int cashBackRate = 3;
            if ("PLATINUM".equals(tier)) cashBackRate = 10;
            else if ("GOLD".equals(tier)) cashBackRate = 7;
            else if ("SILVER".equals(tier)) cashBackRate = 5;

            BigDecimal finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
            BigDecimal coolcashEarned = finalAmount.multiply(BigDecimal.valueOf(cashBackRate))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            order.setCoolcashEarned(coolcashEarned);

            coolCashService.rewardOrderCashback(user, order, coolcashEarned, cashBackRate);

            // Cập nhật tổng chi tiêu và thăng hạng
            BigDecimal currentSpent = user.getTotalSpent() != null ? user.getTotalSpent() : BigDecimal.ZERO;
            BigDecimal newSpent = currentSpent.add(finalAmount);
            user.setTotalSpent(newSpent);

            if (newSpent.compareTo(BigDecimal.valueOf(6000000)) >= 0) {
                user.setMembershipTier("PLATINUM");
            } else if (newSpent.compareTo(BigDecimal.valueOf(3000000)) >= 0) {
                user.setMembershipTier("GOLD");
            } else if (newSpent.compareTo(BigDecimal.valueOf(1000000)) >= 0) {
                user.setMembershipTier("SILVER");
            }
            userRepository.save(user);
        }

        orderRepository.save(order);
    }
}