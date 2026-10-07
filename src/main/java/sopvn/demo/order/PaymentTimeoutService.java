package sopvn.demo.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class PaymentTimeoutService {

    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutService.class);
    public static final int VNPAY_TIMEOUT_MINUTES = 15;

    private final OrderService orderService;

    public PaymentTimeoutService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Định kỳ quét dọn các đơn hàng VNPAY bị bỏ dở hoặc quá hạn thanh toán 15 phút.
     * Chạy mỗi 60 giây một lần (fixedDelay = 60000ms).
     */
    @Scheduled(fixedDelay = 60000)
    public void scheduleVnpayExpirationCleanup() {
        try {
            int cleaned = orderService.cleanupExpiredPaymentPendingOrders(VNPAY_TIMEOUT_MINUTES);
            if (cleaned > 0) {
                log.info("[VNPAY TIMEOUT CLEANUP] Đã tự động hủy và giải phóng tồn kho cho {} đơn hàng quá hạn.", cleaned);
            }
        } catch (Exception e) {
            log.error("[VNPAY TIMEOUT CLEANUP ERROR] Lỗi khi quét dọn đơn hàng quá hạn: {}", e.getMessage());
        }
    }
}
