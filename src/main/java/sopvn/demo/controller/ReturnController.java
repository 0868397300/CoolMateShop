package sopvn.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.OrderReturn;
import sopvn.demo.entity.User;
import sopvn.demo.order.ReturnService;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.OrderReturnRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;

@Controller
public class ReturnController {

    private final OrderRepository orderRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final UserRepository userRepository;
    private final ReturnService returnService;

    public ReturnController(OrderRepository orderRepository,
                            OrderReturnRepository orderReturnRepository,
                            UserRepository userRepository,
                            ReturnService returnService) {
        this.orderRepository = orderRepository;
        this.orderReturnRepository = orderReturnRepository;
        this.userRepository = userRepository;
        this.returnService = returnService;
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
    public String returnPolicyPage(Principal principal, HttpSession session, Model model) {
        User user = getCurrentUser(principal, session);
        model.addAttribute("currentUser", user);
        return "return-policy";
    }

    /**
     * F1-F5: Khách hàng gửi yêu cầu Đổi / Trả hàng chuẩn nghiệp vụ
     */
    @PostMapping("/doi-tra/yeu-cau")
    public String submitReturnRequest(@RequestParam("orderId") Long orderId,
                                      @RequestParam("orderItemId") Long orderItemId,
                                      @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                                      @RequestParam("returnType") String returnType,
                                      @RequestParam(value = "targetVariantId", required = false) Long targetVariantId,
                                      @RequestParam("reason") String reason,
                                      @RequestParam(value = "evidenceImages", required = false) String evidenceImages,
                                      Principal principal,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để gửi yêu cầu đổi trả.");
            return "redirect:/auth/login?required=order";
        }

        try {
            OrderReturn orderReturn = returnService.requestReturn(
                    user, orderId, orderItemId, quantity, returnType, targetVariantId, reason, evidenceImages
            );

            redirectAttributes.addFlashAttribute("successMessage", 
                    "Yêu cầu đổi/trả hàng #" + orderReturn.getId() + " đã được tiếp nhận thành công! Chuyên viên Coolmate sẽ liên hệ trong 24h để bưu tá đến tận nơi đổi hàng miễn phí cho bạn.");
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi yêu cầu đổi trả: " + ex.getMessage());
        }

        return "redirect:/don-hang/" + orderId;
    }
}
