package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.Review;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdAndIsApprovedTrue(Long productId);

    List<Review> findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(Long productId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Review> findAllByOrderByCreatedAtDesc();

    boolean existsByUserIdAndOrderItemId(Long userId, Long orderItemId);

    Optional<Review> findByOrderItemId(Long orderItemId);
}