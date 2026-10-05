package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.PromotionUsage;

import java.util.List;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {
    boolean existsByPromotionIdAndUserId(Long promotionId, Long userId);
    long countByPromotionId(Long promotionId);
    List<PromotionUsage> findByUserIdOrderByUsedAtDesc(Long userId);
    List<PromotionUsage> findByOrderId(Long orderId);
}
