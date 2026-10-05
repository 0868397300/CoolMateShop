package sopvn.demo.order;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.User;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderRepository orderRepository,
                           OrderService orderService,
                           UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
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

    @GetMapping("/don-hang")
    public String orderList(Principal principal, HttpSession session, Model model) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=order";
        }
        // CHỈ LẤY ĐƠN HÀNG CỦA TÀI KHOẢN ĐANG ĐĂNG NHẬP
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        model.addAttribute("orders", orders != null ? orders : Collections.emptyList());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        return "order-history";
    }

    @GetMapping("/don-hang/{identifier}")
    public String orderDetail(@PathVariable("identifier") String identifier,
                             Principal principal,
                             HttpSession session,
                             Model model) {
        User user = getCurrentUser(principal, session);

        Order order = null;
        try {
            Long orderId = Long.valueOf(identifier);
            order = orderRepository.findById(orderId).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (order == null) {
            order = orderRepository.findByOrderCode(identifier).orElse(null);
        }

        if (order == null) {
            return "redirect:/don-hang";
        }

        // Quyền xem đơn: Phải là chủ đơn hàng hoặc Admin/Staff
        boolean isOwner = (user != null && order.getUser() != null && order.getUser().getId().equals(user.getId()));
        boolean isStaffOrAdmin = (user != null && (user.hasRole("ROLE_ADMIN") || user.hasRole("ROLE_STAFF")));

        if (!isOwner && !isStaffOrAdmin) {
            return "redirect:/tra-cuu-don-hang?orderCode=" + order.getOrderCode();
        }

        model.addAttribute("order", order);
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        return "order-detail";
    }

    @PostMapping("/don-hang/{orderId}/huy")
    public String cancelOrder(@PathVariable("orderId") Long orderId,
                              @RequestParam(value = "reason", required = false) String reason,
                              Principal principal,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền hủy đơn hàng này.");
            return "redirect:/don-hang";
        }

        try {
            orderService.cancelOrder(orderId, user, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng #" + order.getOrderCode() + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn hàng: " + e.getMessage());
        }

        return "redirect:/don-hang/" + order.getOrderCode();
    }

    /**
     * D3: Tra cứu đơn hàng cho khách vãng lai (Guest Tracking) bằng Mã đơn & SĐT
     */
    @GetMapping("/tra-cuu-don-hang")
    public String guestTrackingPage(@RequestParam(value = "orderCode", required = false) String orderCode,
                                    @RequestParam(value = "phone", required = false) String phone,
                                    Model model) {
        if (orderCode != null && phone != null && !orderCode.isBlank() && !phone.isBlank()) {
            Order order = orderRepository.findByOrderCodeAndRecipientPhone(orderCode.trim(), phone.trim()).orElse(null);
            model.addAttribute("order", order);
            model.addAttribute("searched", true);
            if (order == null) {
                model.addAttribute("errorMessage", "Không tìm thấy đơn hàng nào khớp với mã đơn và số điện thoại đã nhập.");
            }
        }
        model.addAttribute("searchOrderCode", orderCode);
        model.addAttribute("searchPhone", phone);
        return "client/order-tracking";
    }

    @PostMapping("/tra-cuu-don-hang")
    public String postGuestTracking(@RequestParam("orderCode") String orderCode,
                                    @RequestParam("phone") String phone) {
        return "redirect:/tra-cuu-don-hang?orderCode=" + orderCode.trim() + "&phone=" + phone.trim();
    }
}
