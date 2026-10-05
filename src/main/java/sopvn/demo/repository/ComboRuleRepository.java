package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.ComboRule;

import java.util.List;

@Repository
public interface ComboRuleRepository extends JpaRepository<ComboRule, Integer> {

    List<ComboRule> findByCategoryIdAndIsActiveTrue(Integer categoryId);

    List<ComboRule> findByIsActiveTrue();

    List<ComboRule> findByIsActiveTrueOrderByDiscountPercentageDesc();
}
