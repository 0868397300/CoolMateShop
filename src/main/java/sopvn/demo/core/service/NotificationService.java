package sopvn.demo.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.OrderReturn;
import sopvn.demo.entity.User;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    private void sendEmailQuietly(String to, String subject, String content) {
        if (to == null || to.isBlank()) return;
        try {
            if (mailSender != null && mailFrom != null && !mailFrom.isBlank()) {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(mailFrom);
                msg.setTo(to.trim());
                msg.setSubject(subject);
                msg.setText(content);
                mailSender.send(msg);
                log.info("[EMAIL SENT] Đến: {}, Tiêu đề: {}", to, subject);
            } else {
                log.info("[EMAIL SIMULATION - SMTP not configured] Đến: {}, Tiêu đề: {}", to, subject);
            }
        } catch (Exception e) {
            log.warn("[EMAIL FAILED] Không thể gửi email tới {}: {}", to, e.getMessage());
        }
    }

    public void notifyOrderCreated(Order order) {
        if (order == null) return;
        log.info("[NOTIFICATION] Đơn hàng mới: Mã={}, Khách hàng={}, Tổng tiền={}đ",
                order.getOrderCode(), order.getRecipientName(), order.getFinalAmount());
        if (order.getRecipientEmail() != null) {
            sendEmailQuietly(order.getRecipientEmail(), "Coolmate - Đặt hàng thành công #" + order.getOrderCode(),
                    "Chào " + order.getRecipientName() + ",\nĐơn hàng #" + order.getOrderCode() + " trị giá " + order.getFinalAmount() + "đ của bạn đã được tiếp nhận thành công!");
        }
    }

    public void notifyPaymentSuccess(Order order) {
        if (order == null) return;
        log.info("[NOTIFICATION] Thanh toán VNPAY thành công: Mã đơn={}, Mã giao dịch={}, Số tiền={}đ",
                order.getOrderCode(), order.getVnpayTxnRef(), order.getFinalAmount());
        if (order.getRecipientEmail() != null) {
            sendEmailQuietly(order.getRecipientEmail(), "Coolmate - Xác nhận thanh toán đơn hàng #" + order.getOrderCode(),
                    "Thanh toán thành công qua VNPAY cho đơn hàng #" + order.getOrderCode() + ". Coolmate đang chuẩn bị hàng gửi bạn.");
        }
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
        if (order.getRecipientEmail() != null) {
            sendEmailQuietly(order.getRecipientEmail(), "Coolmate - Thông báo hủy đơn hàng #" + order.getOrderCode(),
                    "Đơn hàng #" + order.getOrderCode() + " đã được hủy thành công. Lý do: " + reason);
        }
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
        // P0-16: Never log password reset tokens or complete reset URLs
        log.info("[NOTIFICATION] Gửi email đặt lại mật khẩu cho tài khoản: Email={}", user.getEmail());
        sendEmailQuietly(user.getEmail(), "Coolmate - Yêu cầu đặt lại mật khẩu",
                "Chào " + user.getFullName() + ",\nVui lòng nhấn vào liên kết sau để đặt lại mật khẩu (hiệu lực 15 phút):\n" + resetUrl);
    }

    public void notifyOrderRefunded(Order order) {
        if (order == null) return;
        log.info("[NOTIFICATION] Hoàn tiền đơn hàng thành công: Mã đơn={}, Mã giao dịch hoàn={}, Số tiền={}đ",
                order.getOrderCode(), order.getRefundReference(), order.getRefundAmount());
        if (order.getRecipientEmail() != null) {
            sendEmailQuietly(order.getRecipientEmail(), "Coolmate - Thông báo hoàn tiền đơn hàng #" + order.getOrderCode(),
                    "Khoản tiền " + order.getRefundAmount() + "đ cho đơn hàng #" + order.getOrderCode() + " đã được hoàn tất xử lý qua " + order.getPaymentMethod() + ".");
        }
    }
}
