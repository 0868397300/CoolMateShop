package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.OrderReturn;
import sopvn.demo.entity.User;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.OrderReturnRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/doi-tra")
public class AdminReturnController {

    private final OrderReturnRepository orderReturnRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminReturnController(OrderReturnRepository orderReturnRepository,
                                 OrderRepository orderRepository,
                                 UserRepository userRepository) {
        this.orderReturnRepository = orderReturnRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listReturns(Model model) {
        List<OrderReturn> returns = orderReturnRepository.findAll();
        model.addAttribute("returns", returns);
        return "admin/return-list";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        OrderReturn ret = orderReturnRepository.findById(id).orElse(null);
        if (ret != null) {
            ret.setStatus(status);

            // Nếu chấp thuận hoàn tiền vào ví CoolCash
            if ("COMPLETED".equalsIgnoreCase(status) && "REFUND_COOLCASH".equalsIgnoreCase(ret.getReturnType())) {
                User u = ret.getUser();
                if (u != null && ret.getRefundAmount() != null) {
                    BigDecimal currentBalance = u.getCoolcashBalance() != null ? u.getCoolcashBalance() : BigDecimal.ZERO;
                    u.setCoolcashBalance(currentBalance.add(ret.getRefundAmount()));
                    userRepository.save(u);
                }
            }

            Order o = ret.getOrder();
            if (o != null) {
                if ("COMPLETED".equalsIgnoreCase(status)) {
                    o.setOrderStatus("RETURN_COMPLETED");
                } else if ("REJECTED".equalsIgnoreCase(status)) {
                    o.setOrderStatus("COMPLETED");
                }
                orderRepository.save(o);
            }

            orderReturnRepository.save(ret);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái yêu cầu đổi trả thành công!");
        }
        return "redirect:/admin/doi-tra";
    }
}
