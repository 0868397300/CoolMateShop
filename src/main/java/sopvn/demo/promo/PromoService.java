package sopvn.demo.promo;

import org.springframework.stereotype.Service;
import sopvn.demo.entity.Promotion;
import sopvn.demo.repository.PromotionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PromoService {

    private final PromotionRepository promotionRepository;

    public PromoService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public Optional<Promotion> findValidPromotion(String code, BigDecimal orderValue) {
        Optional<Promotion> promoOpt = promotionRepository.findByCodeAndIsActiveTrue(code);
        if (promoOpt.isPresent()) {
            Promotion promo = promoOpt.get();
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(promo.getStartDate()) && now.isBefore(promo.getEndDate())) {
                if (orderValue.compareTo(promo.getMinOrderValue()) >= 0 && promo.getUsedCount() < promo.getUsageLimit()) {
                    return Optional.of(promo);
                }
            }
        }
        return Optional.empty();
    }
}
