package sopvn.demo.admin;

import org.springframework.security.access.AccessDeniedException;
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
        List<OrderReturn> returns = orderReturnRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("returns", returns);
        return "admin/return-list";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String status,
                               @RequestParam(value = "refundMethod", defaultValue = "COOLCASH") String refundMethod,
                               @RequestParam(value = "refundReference", required = false) String refundReference,
                               @RequestParam(value = "rejectReason", required = false) String rejectReason,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            User staffUser = null;
            if (userDetails != null) {
                staffUser = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
            }

            String action = (status != null) ? status.trim().toUpperCase() : "APPROVE";

            // Role guard: các action tài chính chỉ dành cho ADMIN
            boolean isFinancialAction = "CONFIRM_REFUND".equalsIgnoreCase(action)
                    || "START_REFUND".equalsIgnoreCase(action)
                    || "MARK_REFUND_FAILED".equalsIgnoreCase(action);

            if (isFinancialAction && (staffUser == null || !staffUser.isAdmin())) {
                throw new AccessDeniedException("Chỉ quản trị viên (ADMIN) mới có quyền xử lý thao tác hoàn tiền đổi trả.");
            }

            returnService.processReturnApproval(id, action, refundMethod, refundReference, rejectReason, staffUser);

            if ("REJECT".equalsIgnoreCase(action)) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu đổi/trả hàng #" + id);
            } else if ("APPROVE".equalsIgnoreCase(action)) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt yêu cầu đổi/trả hàng #" + id + ", chờ khách gửi hàng.");
            } else if ("PROCESS".equalsIgnoreCase(action)) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã tiếp nhận hàng đổi trả #" + id + ", đang kiểm định.");
            } else if ("START_REFUND".equalsIgnoreCase(action)) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã tiếp nhận xử lý hoàn tiền cho yêu cầu #" + id + ".");
            } else if ("CONFIRM_REFUND".equalsIgnoreCase(action)) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận hoàn tiền thành công cho yêu cầu #" + id + " (Mã tham chiếu: " + refundReference + ").");
            } else if ("MARK_REFUND_FAILED".equalsIgnoreCase(action)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đã đánh dấu hoàn tiền thất bại cho yêu cầu #" + id + ".");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã hoàn tất xử lý yêu cầu đổi/trả hàng #" + id + " thành công!");
            }
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý yêu cầu đổi trả: " + ex.getMessage());
        }

        return "redirect:/admin/doi-tra";
    }
}
