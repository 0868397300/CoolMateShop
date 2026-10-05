package sopvn.demo.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.OrderReturn;
import sopvn.demo.entity.User;
import sopvn.demo.order.ReturnService;
import sopvn.demo.repository.OrderReturnRepository;
import sopvn.demo.repository.UserRepository;

import java.util.List;

@Controller
@RequestMapping("/admin/doi-tra")
public class AdminReturnController {

    private final OrderReturnRepository orderReturnRepository;
    private final ReturnService returnService;
    private final UserRepository userRepository;

    public AdminReturnController(OrderReturnRepository orderReturnRepository,
                                 ReturnService returnService,
                                 UserRepository userRepository) {
        this.orderReturnRepository = orderReturnRepository;
        this.returnService = returnService;
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
                               @RequestParam(value = "refundMethod", defaultValue = "COOLCASH") String refundMethod,
                               @RequestParam(value = "rejectReason", required = false) String rejectReason,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            User staffUser = null;
            if (userDetails != null) {
                staffUser = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            }

            String action = ("COMPLETED".equalsIgnoreCase(status) || "APPROVE".equalsIgnoreCase(status)) ? "APPROVE" : "REJECT";
            returnService.processReturnApproval(id, action, refundMethod, rejectReason, staffUser);

            if ("APPROVE".equalsIgnoreCase(action)) {
                redirectAttributes.addFlashAttribute("successMessage", "Chấp thuận và hoàn tất đổi/trả hàng #" + id + " thành công!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu đổi/trả hàng #" + id);
            }
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý yêu cầu đổi trả: " + ex.getMessage());
        }

        return "redirect:/admin/doi-tra";
    }
}
