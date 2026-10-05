package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.User;
import sopvn.demo.order.OrderService;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/don-hang")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public AdminOrderController(OrderRepository orderRepository,
                                OrderService orderService,
                                UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listOrders(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "paymentStatus", required = false) String paymentStatus,
                             @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                             Model model) {
        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();

        long totalCount = allOrders.size();
        long pendingCount = allOrders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getOrderStatus())).count();
        long shippingCount = allOrders.stream().filter(o -> "SHIPPING".equalsIgnoreCase(o.getOrderStatus())).count();
        long completedCount = allOrders.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getOrderStatus())).count();

        List<Order> filtered = allOrders;

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(o -> (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(kw))
                            || (o.getRecipientName() != null && o.getRecipientName().toLowerCase().contains(kw))
                            || (o.getRecipientPhone() != null && o.getRecipientPhone().contains(kw))
                            || (o.getRecipientEmail() != null && o.getRecipientEmail().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            filtered = filtered.stream()
                    .filter(o -> status.equalsIgnoreCase(o.getOrderStatus()))
                    .collect(Collectors.toList());
        }

        if (paymentStatus != null && !paymentStatus.isBlank()) {
            filtered = filtered.stream()
                    .filter(o -> paymentStatus.equalsIgnoreCase(o.getPaymentStatus()))
                    .collect(Collectors.toList());
        }

        if (paymentMethod != null && !paymentMethod.isBlank()) {
            filtered = filtered.stream()
                    .filter(o -> paymentMethod.equalsIgnoreCase(o.getPaymentMethod()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("orders", filtered);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("shippingCount", shippingCount);
        model.addAttribute("completedCount", completedCount);

        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPaymentStatus", paymentStatus);
        model.addAttribute("selectedPaymentMethod", paymentMethod);

        return "admin/order-list";
    }

    @PostMapping("/{orderId}/status")
    public String updateOrderStatus(@PathVariable("orderId") Long orderId,
                                    @RequestParam("status") String status,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            User currentUser = null;
            if (principal != null) {
                currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
            }

            orderService.transitionStatus(orderId, status, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng #" + orderId + " thành công!");
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái đơn hàng: " + ex.getMessage());
        }
        return "redirect:/admin/don-hang";
    }

    @GetMapping("/{orderCode}/in-van-don")
    public String printOrder(@PathVariable("orderCode") String orderCode, Model model) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        model.addAttribute("order", order);
        return "admin/order-print";
    }
}
