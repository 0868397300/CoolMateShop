package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.PromotionUsage;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    boolean existsByPromotionIdAndUserId(Long promotionId, Long userId);

    boolean existsByPromotionIdAndUserIdAndStatusNot(Long promotionId, Long userId, String status);

    long countByPromotionId(Long promotionId);

    long countByPromotionIdAndStatusNot(Long promotionId, String status);

    List<PromotionUsage> findByUserIdOrderByUsedAtDesc(Long userId);

    List<PromotionUsage> findByOrderId(Long orderId);

    Optional<PromotionUsage> findByOrderIdAndPromotionId(Long orderId, Long promotionId);

    boolean existsByOrderIdAndPromotionId(Long orderId, Long promotionId);
}
