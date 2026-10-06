package sopvn.demo.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.cart.CartService;
import sopvn.demo.core.service.NotificationService;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.Promotion;
import sopvn.demo.entity.PromotionUsage;
import sopvn.demo.inventory.StockService;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.PromotionRepository;
import sopvn.demo.repository.PromotionUsageRepository;
import sopvn.demo.wallet.CoolCashService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VnpayPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentProcessor.class);

    private final VnpayService vnpayService;
    private final OrderRepository orderRepository;
    private final StockService stockService;
    private final CoolCashService coolCashService;
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final CartService cartService;
    private final NotificationService notificationService;

    public VnpayPaymentProcessor(VnpayService vnpayService,
                                 OrderRepository orderRepository,
                                 StockService stockService,
                                 CoolCashService coolCashService,
                                 PromotionRepository promotionRepository,
                                 PromotionUsageRepository promotionUsageRepository,
                                 CartService cartService,
                                 NotificationService notificationService) {
        this.vnpayService = vnpayService;
        this.orderRepository = orderRepository;
        this.stockService = stockService;
        this.coolCashService = coolCashService;
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.cartService = cartService;
        this.notificationService = notificationService;
    }

    public static class PaymentResult {
        private final boolean success;
        private final String code;
        private final String message;
        private final Order order;
        private final long amount;
        private final String transactionNo;

        public PaymentResult(boolean success, String code, String message, Order order, long amount, String transactionNo) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.order = order;
            this.amount = amount;
            this.transactionNo = transactionNo;
        }

        public boolean isSuccess() { return success; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Order getOrder() { return order; }
        public long getAmount() { return amount; }
        public String getTransactionNo() { return transactionNo; }
    }

    @Transactional
    public synchronized PaymentResult processVnpayResult(Map<String, String> params, String guestToken) {
        Map<String, String> fields = new HashMap<>(params);
        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // 1. Kiểm tra chữ ký số
        if (!vnpayService.validateSignature(fields, vnp_SecureHash)) {
            log.warn("VNPAY signature validation failed for params: {}", params);
            return new PaymentResult(false, "97", "Chữ ký giao dịch không hợp lệ (Invalid Checksum)", null, 0, null);
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String amountStr = params.get("vnp_Amount");

        Order order = orderRepository.findByVnpayTxnRef(txnRef).orElse(null);
        if (order == null) {
            log.warn("VNPAY order not found for txnRef: {}", txnRef);
            return new PaymentResult(false, "01", "Không tìm thấy đơn hàng tương ứng (Order Not Found)", null, 0, transactionNo);
        }

        // 2. Chống malformed request khi parse amount (LỖI 8)
        long receivedAmount = 0;
        try {
            if (amountStr != null && !amountStr.isBlank()) {
                receivedAmount = Long.parseLong(amountStr.trim()) / 100;
            }
        } catch (NumberFormatException e) {
            log.error("VNPAY amount parse error: {}", amountStr);
            return new PaymentResult(false, "04", "Dữ liệu số tiền không hợp lệ (Invalid Amount Format)", order, 0, transactionNo);
        }

        if (order.getFinalAmount() == null || receivedAmount != order.getFinalAmount().longValue()) {
            log.warn("VNPAY amount mismatch: expected={}, received={}", order.getFinalAmount(), receivedAmount);
            return new PaymentResult(false, "04", "Số tiền thanh toán không khớp với đơn hàng (Invalid Amount)", order, receivedAmount, transactionNo);
        }

        // 3. Idempotent check: Nếu đơn đã PAID trước đó
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return new PaymentResult(true, "02", "Giao dịch đã được ghi nhận thành công trước đó (Order already confirmed)", order, receivedAmount, transactionNo);
        }

        boolean isSuccess = "00".equals(responseCode);

        if (isSuccess) {
            // Thanh toán thành công: Cập nhật PAID, CONFIRMED
            order.setPaymentStatus("PAID");
            order.setPaymentPaidAt(LocalDateTime.now());
            order.setVnpayTransactionNo(transactionNo);
            order.setPaymentBankCode(bankCode);
            order.setPaymentResponseCode(responseCode);
            order.setOrderStatus("CONFIRMED");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Xác nhận tiêu thụ tồn kho (Consume)
            stockService.consumeStock(order);

            // Finalize CoolCash spend (LỖI 5)
            if (order.getUser() != null && order.getCoolcashUsed() != null && order.getCoolcashUsed().compareTo(BigDecimal.ZERO) > 0) {
                coolCashService.spendCoolCash(order.getUser(), order, order.getCoolcashUsed(), "ORDER_COOLCASH_SPEND:" + order.getId());
            }

            // Finalize Voucher usage (LỖI 6)
            List<PromotionUsage> usages = promotionUsageRepository.findByOrderId(order.getId());
            for (PromotionUsage u : usages) {
                if ("RESERVED".equalsIgnoreCase(u.getStatus())) {
                    u.setStatus("FINALIZED");
                    promotionUsageRepository.save(u);
                    Promotion p = u.getPromotion();
                    if (p != null) {
                        p.setUsedCount(p.getUsedCount() + 1);
                        promotionRepository.save(p);
                    }
                }
            }

            // Xóa giỏ hàng
            cartService.clearCart(order.getUser(), guestToken);

            notificationService.notifyPaymentSuccess(order);
            return new PaymentResult(true, "00", "Thanh toán thành công qua VNPAY", order, receivedAmount, transactionNo);
        } else {
            // Thanh toán thất bại hoặc hủy bỏ
            if (!"CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
                order.setPaymentStatus("PAYMENT_FAILED");
                order.setPaymentResponseCode(responseCode);
                order.setPaymentFailureReason("Giao dịch VNPAY thất bại/hủy với mã: " + responseCode);
                order.setOrderStatus("CANCELLED");
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelledReason("Thanh toán VNPAY không thành công");
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                // Release tồn kho đã giữ
                stockService.releaseStock(order);

                // Release Voucher usage (LỖI 6)
                List<PromotionUsage> usages = promotionUsageRepository.findByOrderId(order.getId());
                for (PromotionUsage u : usages) {
                    u.setStatus("RELEASED");
                    promotionUsageRepository.save(u);
                }

                // Nếu CoolCash đã bị trừ, hoàn lại
                if (order.getUser() != null && order.getCoolcashUsed() != null && order.getCoolcashUsed().compareTo(BigDecimal.ZERO) > 0) {
                    coolCashService.refundCoolCash(order.getUser(), order, order.getCoolcashUsed(),
                            "Hoàn lại CoolCash do giao dịch VNPAY thất bại cho đơn #" + order.getOrderCode(),
                            "ORDER_COOLCASH_REFUND:" + order.getId());
                }

                notificationService.notifyPaymentFailed(order, "Mã phản hồi: " + responseCode);
            }
            return new PaymentResult(false, responseCode != null ? responseCode : "99", 
                    "Giao dịch không thành công hoặc bạn đã hủy thanh toán.", order, receivedAmount, transactionNo);
        }
    }
}
