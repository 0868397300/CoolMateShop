package sopvn.demo.promo;

import org.springframework.stereotype.Service;
import sopvn.demo.entity.ComboRule;
import sopvn.demo.repository.ComboRuleRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ComboRuleService {

    private final ComboRuleRepository comboRuleRepository;

    public ComboRuleService(ComboRuleRepository comboRuleRepository) {
        this.comboRuleRepository = comboRuleRepository;
    }

    public List<ComboRule> getActiveComboRules() {
        return comboRuleRepository.findByIsActiveTrue();
    }

    public BigDecimal calculateComboDiscount(Integer categoryId, int quantity, BigDecimal lineTotal) {
        List<ComboRule> rules = comboRuleRepository.findByCategoryIdAndIsActiveTrue(categoryId);
        for (ComboRule rule : rules) {
            if (quantity >= rule.getMinQuantity()) {
                return lineTotal.multiply(rule.getDiscountPercentage()).divide(BigDecimal.valueOf(100));
            }
        }
        return BigDecimal.ZERO;
    }
}
