package sopvn.demo.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.cart.PricingService;
import sopvn.demo.cart.dto.PricingSummaryDTO;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.core.service.NotificationService;
import sopvn.demo.entity.*;
import sopvn.demo.inventory.StockService;
import sopvn.demo.repository.OrderItemRepository;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.ProductVariantRepository;
import sopvn.demo.repository.PromotionRepository;
import sopvn.demo.repository.PromotionUsageRepository;
import sopvn.demo.repository.UserRepository;
import sopvn.demo.wallet.CoolCashService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Transactional
    public Order createOrder(User user, String recipientName, String phone, String email, String address, 
                            String province, String district, String note, String paymentMethod, 
                            String voucherCode, boolean useCoolCash, List<CartItem> cartItems) {
        if (recipientName == null || recipientName.isBlank()) throw new CustomException("Tên người nhận không được để trống.");
        if (phone == null || phone.isBlank()) throw new CustomException("Số điện thoại nhận hàng không được để trống.");
        if (address == null || address.isBlank()) throw new CustomException("Địa chỉ giao hàng không được để trống.");
        if (cartItems == null || cartItems.isEmpty()) throw new CustomException("Giỏ hàng của bạn đang trống.");

        // 1. Re-validate tồn kho và giá cả server-side
        List<CartItem> validItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            ProductVariant v = productVariantRepository.findById(ci.getVariant().getId())
                    .orElseThrow(() -> new CustomException("Sản phẩm không còn tồn tại: " + ci.getVariant().getSku()));
            if (!Boolean.TRUE.equals(v.getIsActive())) {
                throw new CustomException("Sản phẩm '" + v.getSku() + "' đã tạm ngừng kinh doanh.");
            }
            if (v.getStockQuantity() < ci.getQuantity()) {
                throw new CustomException("Sản phẩm '" + v.getSku() + "' chỉ còn " + v.getStockQuantity() + " chiếc trong kho.");
            }
            ci.setVariant(v);
            validItems.add(ci);
        }

        BigDecimal requestedCoolCash = (useCoolCash && user != null) ? user.getCoolcashBalance() : BigDecimal.ZERO;
        PricingSummaryDTO pricing = pricingService.calculatePricing(user, validItems, voucherCode, requestedCoolCash, province, district, paymentMethod);

        Order order = new Order();
        String orderCode = "CM" + System.currentTimeMillis();
        order.setOrderCode(orderCode);
        order.setVnpayTxnRef(orderCode + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        order.setUser(user);
        order.setRecipientName(recipientName.trim());
        order.setRecipientPhone(phone.trim());
        order.setRecipientEmail(email != null ? email.trim() : (user != null ? user.getEmail() : null));
        order.setShippingAddress(address.trim());
        order.setNote(note);

        order.setPaymentMethod(paymentMethod != null ? paymentMethod.toUpperCase() : "COD");
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            order.setOrderStatus("PENDING");
            order.setPaymentStatus("PAYMENT_PENDING");
        } else {
            order.setOrderStatus("CONFIRMED"); // COD xác nhận đặt hàng thành công
            order.setPaymentStatus("UNPAID");
        }

        order.setSubtotalAmount(pricing.getSubtotal());
        order.setComboDiscountAmount(pricing.getComboDiscount());
        order.setVoucherDiscountAmount(pricing.getVoucherDiscount());
        order.setCoolcashUsed(pricing.getCoolcashUsed());
        order.setShippingFee(pricing.getShippingFee());
        order.setFinalAmount(pricing.getFinalAmount());
        order.setPromotion(pricing.getAppliedPromotion());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        // 2. Snapshot giá bán & giá vốn lịch sử (Historical COGS)
        List<OrderItem> savedItems = new ArrayList<>();
        for (CartItem ci : validItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setVariant(ci.getVariant());
            oi.setProductNameSnapshot(ci.getVariant().getProduct() != null ? ci.getVariant().getProduct().getName() : "");
            oi.setSkuSnapshot(ci.getVariant().getSku());
            oi.setColorNameSnapshot(ci.getVariant().getColor() != null ? ci.getVariant().getColor().getName() : "");
            oi.setSizeNameSnapshot(ci.getVariant().getSize() != null ? ci.getVariant().getSize().getName() : "");
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getVariant().getSalePrice());
            oi.setTotalPrice(ci.getVariant().getSalePrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            // Historical Cost Snapshot
            BigDecimal currentCost = ci.getVariant().getImportPrice() != null ? ci.getVariant().getImportPrice() : BigDecimal.ZERO;
            oi.setCostPriceSnapshot(currentCost);

            savedItems.add(orderItemRepository.save(oi));
        }
        order.setItems(savedItems);

        // 3. Giữ tồn kho nguyên tử (Stock Reservation)
        stockService.reserveStock(savedItems, order);

        // 4. CoolCash & Voucher Handling
        if ("COD".equalsIgnoreCase(paymentMethod)) {
            // COD: Trừ CoolCash ngay nếu có
            if (user != null && pricing.getCoolcashUsed().compareTo(BigDecimal.ZERO) > 0) {
                coolCashService.spendCoolCash(user, order, pricing.getCoolcashUsed(), "ORDER_COOLCASH_SPEND:" + order.getId());
            }
            // Ghi nhận Voucher FINALIZED
            if (pricing.getAppliedPromotion() != null) {
                Promotion promo = pricing.getAppliedPromotion();
                promo.setUsedCount(promo.getUsedCount() + 1);
                promotionRepository.save(promo);

                PromotionUsage usage = new PromotionUsage(promo, user, order, pricing.getVoucherDiscount(), "FINALIZED");
                promotionUsageRepository.save(usage);
            }
        } else {
            // VNPAY: Tạm giữ CoolCash và Voucher ở trạng thái RESERVED
            if (user != null && pricing.getCoolcashUsed().compareTo(BigDecimal.ZERO) > 0) {
                coolCashService.reserveCoolCash(user, order, pricing.getCoolcashUsed(), "ORDER_COOLCASH_RESERVE:" + order.getId());
            }
            if (pricing.getAppliedPromotion() != null) {
                Promotion promo = pricing.getAppliedPromotion();
                PromotionUsage usage = new PromotionUsage(promo, user, order, pricing.getVoucherDiscount(), "RESERVED");
                promotionUsageRepository.save(usage);
            }
        }

        notificationService.notifyOrderCreated(order);
        return order;
    }

    @Transactional
    public Order createOrder(User user, String recipientName, String phone, String email, String address, 
                            String note, String paymentMethod, String voucherCode, boolean useCoolCash, 
                            List<CartItem> cartItems) {
        return createOrder(user, recipientName, phone, email, address, null, null, note, paymentMethod, voucherCode, useCoolCash, cartItems);
    }

    @Transactional
    public void cancelOrder(Long orderId, User currentUser, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng #" + orderId));

        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return;
        }

        if ("COMPLETED".equalsIgnoreCase(order.getOrderStatus()) || "SHIPPING".equalsIgnoreCase(order.getOrderStatus())) {
            throw new CustomException("Không thể hủy đơn hàng đang giao hoặc đã hoàn tất.");
        }

        if (currentUser != null && order.getUser() != null && !currentUser.getId().equals(order.getUser().getId())) {
            boolean isStaffOrAdmin = currentUser.hasRole("ROLE_ADMIN") || currentUser.hasRole("ROLE_STAFF");
            if (!isStaffOrAdmin) {
                throw new CustomException("Bạn không có quyền hủy đơn hàng của người khác.");
            }
        }

        order.setOrderStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledReason(reason != null ? reason : "Người dùng hoặc quản trị viên hủy đơn");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // 1. Giải phóng tồn kho đã giữ
        stockService.releaseStock(order);

        // 2. Release Voucher usage để khách có thể tái sử dụng
        List<PromotionUsage> usages = promotionUsageRepository.findByOrderId(order.getId());
        for (PromotionUsage u : usages) {
            u.setStatus("RELEASED");
            promotionUsageRepository.save(u);
        }

        // 3. Hoàn lại CoolCash đã giữ hoặc đã chi tiêu
        if (order.getUser() != null && order.getCoolcashUsed() != null && order.getCoolcashUsed().compareTo(BigDecimal.ZERO) > 0) {
            coolCashService.releaseReservedCoolCash(order.getUser(), order, order.getCoolcashUsed(), 
                    "ORDER_COOLCASH_RELEASE:" + order.getId());
        }

        notificationService.notifyOrderCancelled(order, reason);
    }

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng #" + orderId));

        if ("COMPLETED".equalsIgnoreCase(order.getOrderStatus())) {
            return;
        }

        order.setOrderStatus("COMPLETED");
        // Chỉ đánh dấu PAID nếu là COD và xác nhận nhận hàng
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setPaymentStatus("PAID");
        }
        if (order.getDeliveredAt() == null) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        order.setUpdatedAt(LocalDateTime.now());

        // Trao thưởng CoolCash theo hạng thành viên
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

    @Transactional
    public void transitionStatus(Long orderId, String targetStatus, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("Không tìm thấy đơn hàng #" + orderId));

        String currentStatus = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PENDING";
        String next = targetStatus != null ? targetStatus.toUpperCase() : currentStatus;

        if (currentStatus.equals(next)) {
            return;
        }

        if ("CANCELLED".equals(currentStatus) || "COMPLETED".equals(currentStatus)) {
            throw new CustomException("Đơn hàng đã ở trạng thái kết thúc (" + currentStatus + "), không thể thay đổi.");
        }

        if ("CANCELLED".equals(next)) {
            cancelOrder(orderId, currentUser, "Hủy đơn hàng bởi người dùng/quản trị viên");
            return;
        }

        if ("PENDING".equals(currentStatus) && !"CONFIRMED".equals(next) && !"CANCELLED".equals(next)) {
            throw new CustomException("Đơn hàng PENDING chỉ có thể chuyển sang CONFIRMED hoặc CANCELLED.");
        }

        if ("CONFIRMED".equals(currentStatus) && !"SHIPPING".equals(next) && !"CANCELLED".equals(next)) {
            throw new CustomException("Đơn hàng CONFIRMED chỉ có thể chuyển sang SHIPPING hoặc CANCELLED.");
        }

        if ("SHIPPING".equals(currentStatus) && !"DELIVERED".equals(next)) {
            throw new CustomException("Đơn hàng SHIPPING chỉ có thể chuyển sang DELIVERED.");
        }

        if ("DELIVERED".equals(currentStatus) && !"COMPLETED".equals(next)) {
            throw new CustomException("Đơn hàng DELIVERED chỉ có thể chuyển sang COMPLETED.");
        }

        if ("COMPLETED".equals(next)) {
            completeOrder(orderId);
            return;
        }

        if ("CONFIRMED".equals(next)) {
            order.setOrderStatus("CONFIRMED");
            notificationService.notifyOrderConfirmed(order);
        } else if ("SHIPPING".equals(next)) {
            order.setOrderStatus("SHIPPING");
            notificationService.notifyOrderShipping(order);
        } else if ("DELIVERED".equals(next)) {
            order.setOrderStatus("DELIVERED");
            if (order.getDeliveredAt() == null) {
                order.setDeliveredAt(LocalDateTime.now());
            }
            notificationService.notifyOrderDelivered(order);
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
