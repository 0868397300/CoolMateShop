package sopvn.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.core.service.ReviewService;
import sopvn.demo.entity.User;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Principal principal, HttpSession session) {
        if (principal != null) {
            return userRepository.findByEmail(principal.getName()).orElse(null);
        }
        if (session != null) {
            Object sUser = session.getAttribute("currentUser");
            if (sUser instanceof User) return (User) sUser;
            Object uid = session.getAttribute("userId");
            if (uid != null) {
                try {
                    return userRepository.findById(Long.valueOf(uid.toString())).orElse(null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @PostMapping("/danh-gia/gui")
    public String submitReview(@RequestParam("orderItemId") Long orderItemId,
                               @RequestParam("rating") int rating,
                               @RequestParam("comment") String comment,
                               @RequestParam(value = "fitFeedback", defaultValue = "TRUE_TO_SIZE") String fitFeedback,
                               @RequestParam(value = "heightCm", required = false) Integer heightCm,
                               @RequestParam(value = "weightKg", required = false) Integer weightKg,
                               @RequestParam(value = "imageUrls", required = false) String imageUrls,
                               @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                               Principal principal,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để gửi đánh giá sản phẩm.");
            return "redirect:/auth/login?required=review";
        }

        try {
            reviewService.createVerifiedReview(user, orderItemId, rating, comment, fitFeedback, heightCm, weightKg, imageUrls);
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã gửi đánh giá! Đánh giá sẽ được hiển thị sau khi quản trị viên phê duyệt.");
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi gửi đánh giá: " + ex.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/tai-khoan/don-hang";
    }
}
