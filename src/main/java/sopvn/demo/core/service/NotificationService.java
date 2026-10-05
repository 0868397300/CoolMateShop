package sopvn.demo.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.OrderReturn;
import sopvn.demo.entity.User;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyOrderCreated(Order order) {
        if (order == null) return;
        log.info("[NOTIFICATION] Đơn hàng mới: Mã={}, Khách hàng={}, Tổng tiền={}đ",
                order.getOrderCode(), order.getRecipientName(), order.getFinalAmount());
    }

    public void notifyPaymentSuccess(Order order) {
        if (order == null) return;
        log.info("[NOTIFICATION] Thanh toán VNPAY thành công: Mã đơn={}, Mã giao dịch={}, Số tiền={}đ",
                order.getOrderCode(), order.getVnpayTxnRef(), order.getFinalAmount());
    }

    public void notifyPaymentFailed(Order order, String reason) {
        if (order == null) return;
        log.warn("[NOTIFICATION] Thanh toán VNPAY thất bại: Mã đơn={}, Lý do={}",
                order.getOrderCode(), reason);
    }

    public void notifyOrderCancelled(Order order, String reason) {
        if (order == null) return;
        log.info("[NOTIFICATION] Đơn hàng đã bị hủy: Mã={}, Lý do={}",
                order.getOrderCode(), reason);
    }

    public void notifyOrderConfirmed(Order order) {
        if (order == null) return;
        log.info("[NOTIFICATION] Đơn hàng đã được xác nhận: Mã={}", order.getOrderCode());
    }

    public void notifyOrderShipping(Order order) {
        if (order == null) return;
        log.info("[NOTIFICATION] Đơn hàng đang được giao: Mã={}", order.getOrderCode());
    }

    public void notifyOrderDelivered(Order order) {
        if (order == null) return;
        log.info("[NOTIFICATION] Đơn hàng giao thành công: Mã={}", order.getOrderCode());
    }

    public void notifyReturnRequested(OrderReturn ret) {
        if (ret == null) return;
        log.info("[NOTIFICATION] Yêu cầu đổi trả mới: Mã yêu cầu={}, Mã đơn={}, Loại={}",
                ret.getId(), (ret.getOrder() != null ? ret.getOrder().getOrderCode() : ""), ret.getReturnType());
    }

    public void notifyReturnApproved(OrderReturn ret) {
        if (ret == null) return;
        log.info("[NOTIFICATION] Yêu cầu đổi trả đã được duyệt: Mã yêu cầu={}, Hoàn tiền={}đ",
                ret.getId(), ret.getRefundAmount());
    }

    public void notifyReturnRejected(OrderReturn ret) {
        if (ret == null) return;
        log.warn("[NOTIFICATION] Yêu cầu đổi trả bị từ chối: Mã yêu cầu={}, Lý do={}",
                ret.getId(), ret.getRejectionReason());
    }

    public void notifyReturnCompleted(OrderReturn ret) {
        if (ret == null) return;
        log.info("[NOTIFICATION] Yêu cầu đổi trả hoàn tất: Mã yêu cầu={}", ret.getId());
    }

    public void notifyPasswordReset(User user, String resetUrl) {
        if (user == null) return;
        log.info("[NOTIFICATION] Gửi email đặt lại mật khẩu: Email={}, Link={}",
                user.getEmail(), resetUrl);
    }
}
