package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.Promotion;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCodeAndIsActiveTrue(String code);

    Optional<Promotion> findByCode(String code);

    List<Promotion> findByIsActiveTrue();

    List<Promotion> findByIsActiveTrueOrderByStartDateDesc();
}
