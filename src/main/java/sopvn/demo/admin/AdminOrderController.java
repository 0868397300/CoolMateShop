package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.entity.CoolCashTransaction;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.User;
import sopvn.demo.repository.CoolCashTransactionRepository;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/don-hang")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CoolCashTransactionRepository coolCashTransactionRepository;

    public AdminOrderController(OrderRepository orderRepository,
                                UserRepository userRepository,
                                CoolCashTransactionRepository coolCashTransactionRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.coolCashTransactionRepository = coolCashTransactionRepository;
    }

    @GetMapping
    public String listOrders(Model model) {
        model.addAttribute("orders", orderRepository.findAllByOrderByCreatedAtDesc());
        return "admin/order-list";
    }

    @PostMapping("/{orderId}/status")
    public String updateOrderStatus(@PathVariable("orderId") Long orderId,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            String oldStatus = order.getOrderStatus();
            order.setOrderStatus(status);
            order.setUpdatedAt(LocalDateTime.now());

            // Nếu hoàn tất đơn hàng (COMPLETED), tính hoàn tiền CoolCash theo Hạng hội viên
            if ("COMPLETED".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(oldStatus)) {
                order.setDeliveredAt(LocalDateTime.now());
                order.setPaymentStatus("PAID");

                User user = order.getUser();
                if (user != null) {
                    // Tỷ lệ hoàn tiền theo hạng: NEW 3%, SILVER 5%, GOLD 7%, PLATINUM 10%
                    String tier = user.getMembershipTier() != null ? user.getMembershipTier().toUpperCase() : "NEW";
                    int cashBackRate = 3;
                    if ("PLATINUM".equals(tier)) cashBackRate = 10;
                    else if ("GOLD".equals(tier)) cashBackRate = 7;
                    else if ("SILVER".equals(tier)) cashBackRate = 5;

                    BigDecimal finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
                    BigDecimal coolcashEarned = finalAmount.multiply(BigDecimal.valueOf(cashBackRate))
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    order.setCoolcashEarned(coolcashEarned);

                    // Cộng vào ví khách hàng
                    BigDecimal currentBalance = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;
                    user.setCoolcashBalance(currentBalance.add(coolcashEarned));

                    // Cộng dồn tổng chi tiêu
                    BigDecimal currentSpent = user.getTotalSpent() != null ? user.getTotalSpent() : BigDecimal.ZERO;
                    BigDecimal newSpent = currentSpent.add(finalAmount);
                    user.setTotalSpent(newSpent);

                    // Tự động thăng hạng hội viên
                    if (newSpent.compareTo(BigDecimal.valueOf(6000000)) >= 0) {
                        user.setMembershipTier("PLATINUM");
                    } else if (newSpent.compareTo(BigDecimal.valueOf(3000000)) >= 0) {
                        user.setMembershipTier("GOLD");
                    } else if (newSpent.compareTo(BigDecimal.valueOf(1000000)) >= 0) {
                        user.setMembershipTier("SILVER");
                    }

                    userRepository.save(user);

                    // Ghi lịch sử giao dịch ví CoolCash
                    CoolCashTransaction tx = new CoolCashTransaction();
                    tx.setUser(user);
                    tx.setOrder(order);
                    tx.setAmount(coolcashEarned);
                    tx.setTransactionType("EARN_ORDER");
                    tx.setStatus("COMPLETED");
                    tx.setDescription("Hoàn tiền " + cashBackRate + "% hạng " + user.getMembershipTier() + " cho đơn hàng #" + order.getOrderCode());
                    tx.setCreatedAt(LocalDateTime.now());
                    coolCashTransactionRepository.save(tx);
                }
            }

            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng #" + order.getOrderCode() + " thành công!");
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
