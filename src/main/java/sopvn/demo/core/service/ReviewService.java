package sopvn.demo.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.*;
import sopvn.demo.repository.OrderItemRepository;
import sopvn.demo.repository.ProductRepository;
import sopvn.demo.repository.ReviewRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderItemRepository orderItemRepository,
                         ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Review createVerifiedReview(User user, Long orderItemId, int rating, String comment,
                                       String fitFeedback, Integer heightCm, Integer weightKg, String imageUrls) {
        if (user == null) {
            throw new CustomException("Vui lòng đăng nhập để gửi đánh giá.");
        }

        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new CustomException("Không tìm thấy sản phẩm trong đơn hàng."));

        Order order = item.getOrder();
        if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new CustomException("Bạn chỉ được đánh giá sản phẩm từ đơn hàng của chính mình.");
        }

        if (!"COMPLETED".equalsIgnoreCase(order.getOrderStatus()) && !"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new CustomException("Bạn chỉ có thể đánh giá sau khi đã nhận hàng thành công.");
        }

        if (reviewRepository.existsByUserIdAndOrderItemId(user.getId(), orderItemId)) {
            throw new CustomException("Bạn đã gửi đánh giá cho sản phẩm này rồi.");
        }

        if (rating < 1 || rating > 5) {
            throw new CustomException("Số sao đánh giá phải từ 1 đến 5 sao.");
        }

        if (comment == null || comment.trim().length() < 5) {
            throw new CustomException("Nội dung đánh giá phải có ít nhất 5 ký tự.");
        }

        Product product = item.getVariant() != null ? item.getVariant().getProduct() : null;
        if (product == null) {
            throw new CustomException("Sản phẩm không còn tồn tại trên hệ thống.");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setOrderItem(item);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setPurchasedColor(item.getColorNameSnapshot());
        review.setPurchasedSize(item.getSizeNameSnapshot());
        review.setFitFeedback(fitFeedback != null ? fitFeedback : "TRUE_TO_SIZE");
        review.setCustomerHeightCm(heightCm != null ? heightCm : user.getHeightCm());
        review.setCustomerWeightKg(weightKg != null ? weightKg : user.getWeightKg());
        review.setImageUrls(imageUrls);
        review.setStatus("PENDING");
        review.setIsApproved(false);

        item.setIsReviewed(true);
        orderItemRepository.save(item);

        return reviewRepository.save(review);
    }

    @Transactional
    public void approveReview(Long reviewId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("Đánh giá không tồn tại: " + reviewId));
        r.setStatus("APPROVED");
        r.setIsApproved(true);
        reviewRepository.save(r);

        updateProductRatingStats(r.getProduct());
    }

    @Transactional
    public void hideReview(Long reviewId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("Đánh giá không tồn tại: " + reviewId));
        r.setStatus("HIDDEN");
        r.setIsApproved(false);
        reviewRepository.save(r);

        updateProductRatingStats(r.getProduct());
    }

    @Transactional
    public void replyReview(Long reviewId, String adminReply) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("Đánh giá không tồn tại: " + reviewId));
        r.setAdminReply(adminReply != null ? adminReply.trim() : "");
        r.setAdminRepliedAt(LocalDateTime.now());
        reviewRepository.save(r);
    }

    @Transactional(readOnly = true)
    public List<Review> getApprovedReviews(Long productId) {
        return reviewRepository.findByProductIdAndIsApprovedTrue(productId);
    }

    public void updateProductRatingStats(Product product) {
        if (product == null) return;
        List<Review> approved = reviewRepository.findByProductIdAndIsApprovedTrue(product.getId());
        if (approved.isEmpty()) {
            product.setReviewCount(0);
            product.setRatingAvg(BigDecimal.ZERO);
        } else {
            product.setReviewCount(approved.size());
            double avg = approved.stream().mapToInt(Review::getRating).average().orElse(0.0);
            product.setRatingAvg(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        }
        productRepository.save(product);
    }
}
