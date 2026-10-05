package sopvn.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.OrderItem;
import sopvn.demo.entity.OrderReturn;
import sopvn.demo.entity.User;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.OrderReturnRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
public class ReturnController {

    private final OrderRepository orderRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final UserRepository userRepository;

    public ReturnController(OrderRepository orderRepository,
                            OrderReturnRepository orderReturnRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderReturnRepository = orderReturnRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Principal principal, HttpSession session) {
        if (principal != null) {
            User u = userRepository.findByEmail(principal.getName()).orElse(null);
            if (u != null) return u;
        }
        if (session != null) {
            Object sUser = session.getAttribute("currentUser");
            if (sUser instanceof User) return (User) sUser;
            Object uid = session.getAttribute("userId");
            if (uid == null) uid = session.getAttribute("USER_ID");
            if (uid != null) {
                try {
                    return userRepository.findById(Long.valueOf(uid.toString())).orElse(null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Trang thông tin chính sách đổi trả 60 ngày của Coolmate
     */
    @GetMapping({"/chinh-sach-doi-tra", "/doi-tra/chinh-sach"})
    public String returnPolicyPage() {
        return "return-policy";
    }

    /**
     * Khách hàng gửi yêu cầu Đổi / Trả hàng cho đơn hàng đã nhận
     */
    @PostMapping("/doi-tra/yeu-cau")
    public String submitReturnRequest(@RequestParam("orderId") Long orderId,
                                      @RequestParam(value = "orderItemId", required = false) Long orderItemId,
                                      @RequestParam("returnType") String returnType,
                                      @RequestParam("reason") String reason,
                                      @RequestParam(value = "note", required = false) String note,
                                      Principal principal,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để gửi yêu cầu đổi trả.");
            return "redirect:/auth/login?required=order";
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đơn hàng hợp lệ.");
            return "redirect:/don-hang";
        }

        OrderReturn req = new OrderReturn();
        req.setOrder(order);
        req.setUser(user);

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItem selectedItem = order.getItems().get(0);
            if (orderItemId != null) {
                for (OrderItem oi : order.getItems()) {
                    if (oi.getId().equals(orderItemId)) {
                        selectedItem = oi;
                        break;
                    }
                }
            }
            req.setOrderItem(selectedItem);
        }

        req.setReturnType(returnType);
        req.setReason(reason + (note != null && !note.isEmpty() ? " - Ghi chú: " + note : ""));
        req.setStatus("PENDING");
        req.setQuantity(1);
        req.setRefundAmount(order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO);
        orderReturnRepository.save(req);

        order.setOrderStatus("RETURN_REQUESTED");
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("successMessage", 
                "Yêu cầu đổi/trả hàng cho đơn " + order.getOrderCode() + " đã được ghi nhận thành công! Chuyên viên CSKH Coolmate sẽ liên hệ trong 24h để bưu tá đến tận nơi đổi hàng cho bạn.");

        return "redirect:/don-hang/" + order.getOrderCode();
    }
}
