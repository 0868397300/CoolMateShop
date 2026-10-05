package sopvn.demo.order;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.User;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/don-hang")
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderController(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
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

    @GetMapping
    public String orderList(Principal principal, HttpSession session, Model model) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=order";
        }
        // CHỈ LẤY ĐƠN HÀNG CỦA TÀI KHOẢN ĐANG ĐĂNG NHẬP
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        model.addAttribute("orders", orders != null ? orders : Collections.emptyList());
        model.addAttribute("user", user);
        return "order-history";
    }

    @GetMapping("/{orderCode}")
    public String orderDetail(@PathVariable("orderCode") String orderCode,
                             Principal principal,
                             HttpSession session,
                             Model model) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=order";
        }

        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return "redirect:/don-hang";
        }

        // Kiểm tra quyền sở hữu đơn hàng: Khách hàng chỉ xem được đơn của chính mình
        boolean isOwner = (order.getUser() != null && order.getUser().getId().equals(user.getId()));
        boolean isStaffOrAdmin = user.hasRole("ROLE_ADMIN") || user.hasRole("ROLE_STAFF");

        if (!isOwner && !isStaffOrAdmin) {
            return "redirect:/don-hang";
        }

        model.addAttribute("order", order);
        model.addAttribute("user", user);
        return "order-detail";
    }
}
