package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.service.ReviewService;
import sopvn.demo.entity.Product;
import sopvn.demo.entity.Review;
import sopvn.demo.repository.ProductRepository;
import sopvn.demo.repository.ReviewRepository;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/danh-gia")
public class AdminReviewController {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ReviewService reviewService;

    public AdminReviewController(ReviewRepository reviewRepository,
                                 ProductRepository productRepository,
                                 ReviewService reviewService) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.reviewService = reviewService;
    }

    @GetMapping
    public String listReviews(@RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "rating", required = false) Integer rating,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "fit", required = false) String fit,
                              Model model) {
        List<Review> reviews = reviewRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            reviews = reviews.stream()
                    .filter(r -> (r.getComment() != null && r.getComment().toLowerCase().contains(kw))
                            || (r.getProduct() != null && r.getProduct().getName() != null && r.getProduct().getName().toLowerCase().contains(kw))
                            || (r.getUser() != null && r.getUser().getFullName() != null && r.getUser().getFullName().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (rating != null) {
            reviews = reviews.stream()
                    .filter(r -> rating.equals(r.getRating()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            if ("APPROVED".equalsIgnoreCase(status)) {
                reviews = reviews.stream().filter(r -> Boolean.TRUE.equals(r.getIsApproved())).collect(Collectors.toList());
            } else if ("PENDING".equalsIgnoreCase(status)) {
                reviews = reviews.stream().filter(r -> !Boolean.TRUE.equals(r.getIsApproved())).collect(Collectors.toList());
            }
        }

        if (fit != null && !fit.isBlank()) {
            reviews = reviews.stream()
                    .filter(r -> fit.equalsIgnoreCase(r.getFitFeedback()))
                    .collect(Collectors.toList());
        }

        long totalReviews = reviewRepository.count();
        long pendingReviews = reviewRepository.findAll().stream().filter(r -> !Boolean.TRUE.equals(r.getIsApproved())).count();

        model.addAttribute("reviews", reviews);
        model.addAttribute("totalReviews", totalReviews);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRating", rating);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedFit", fit);

        return "admin/review-list";
    }

    @PostMapping("/{id}/duyet")
    public String approveReview(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.approveReview(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt đánh giá công khai lên website!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    @PostMapping("/{id}/an")
    public String hideReview(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.hideReview(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạm ẩn đánh giá của khách hàng!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    @PostMapping("/{id}/phan-hoi")
    public String replyReview(@PathVariable("id") Long id,
                              @RequestParam("adminReply") String adminReply,
                              RedirectAttributes redirectAttributes) {
        try {
            reviewService.replyReview(id, adminReply);
            redirectAttributes.addFlashAttribute("successMessage", "Phản hồi khách hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }

    @PostMapping("/{id}/xoa")
    public String deleteReview(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Review r = reviewRepository.findById(id).orElse(null);
            if (r != null) {
                Product p = r.getProduct();
                reviewRepository.delete(r);
                reviewService.updateProductRatingStats(p);
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đánh giá thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa đánh giá: " + e.getMessage());
        }
        return "redirect:/admin/danh-gia";
    }
}