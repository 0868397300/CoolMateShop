package sopvn.demo.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.cart.dto.PricingSummaryDTO;
import sopvn.demo.entity.*;
import sopvn.demo.order.ShippingFeeService;
import sopvn.demo.repository.ComboRuleRepository;
import sopvn.demo.repository.PromotionRepository;
import sopvn.demo.repository.PromotionUsageRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PricingService {

    private final ComboRuleRepository comboRuleRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final ShippingFeeService shippingFeeService;

    public PricingService(ComboRuleRepository comboRuleRepository,
                          PromotionRepository promotionRepository,
                          PromotionUsageRepository promotionUsageRepository,
                          ShippingFeeService shippingFeeService) {
        this.comboRuleRepository = comboRuleRepository;
        this.promotionRepository = promotionRepository;
        this.promotionUsageRepository = promotionUsageRepository;
        this.shippingFeeService = shippingFeeService;
    }

    @Transactional(readOnly = true)
    public PricingSummaryDTO calculatePricing(User user,
                                             List<CartItem> items,
                                             String voucherCode,
                                             BigDecimal requestedCoolCash,
                                             String provinceName,
                                             String districtName,
                                             String paymentMethod) {
        PricingSummaryDTO summary = new PricingSummaryDTO();
        if (items == null || items.isEmpty()) {
            return summary;
        }

        // 1. Tính Subtotal và Group theo Category cho Combo Rules (Category.id là Integer)
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Integer, Integer> categoryQtyMap = new HashMap<>();
        Map<Integer, BigDecimal> categoryAmountMap = new HashMap<>();

        for (CartItem item : items) {
            ProductVariant v = item.getVariant();
            if (v == null) continue;
            BigDecimal price = v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            if (v.getProduct() != null && v.getProduct().getCategory() != null) {
                Integer catId = v.getProduct().getCategory().getId();
                if (catId != null) {
                    categoryQtyMap.put(catId, categoryQtyMap.getOrDefault(catId, 0) + item.getQuantity());
                    categoryAmountMap.put(catId, categoryAmountMap.getOrDefault(catId, BigDecimal.ZERO).add(lineTotal));
                }
            }
        }
        summary.setSubtotal(subtotal);

        // 2. Tính Combo Discount theo từng Category (Chọn tier cao nhất phù hợp)
        BigDecimal comboDiscount = BigDecimal.ZERO;
        List<ComboRule> activeRules = comboRuleRepository.findByIsActiveTrueOrderByMinQuantityDesc();
        List<ComboRule> appliedCombos = new ArrayList<>();

        Set<Integer> processedCategories = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : categoryQtyMap.entrySet()) {
            Integer catId = entry.getKey();
            int qty = entry.getValue();
            BigDecimal catAmount = categoryAmountMap.getOrDefault(catId, BigDecimal.ZERO);

            for (ComboRule rule : activeRules) {
                if (rule.getCategory() != null && rule.getCategory().getId() != null && rule.getCategory().getId().equals(catId)) {
                    if (qty >= rule.getMinQuantity() && !processedCategories.contains(catId)) {
                        // rule.getDiscountPercentage() trả về BigDecimal
                        BigDecimal discPercent = rule.getDiscountPercentage() != null 
                                ? rule.getDiscountPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        BigDecimal ruleDiscount = catAmount.multiply(discPercent).setScale(0, RoundingMode.HALF_UP);
                        comboDiscount = comboDiscount.add(ruleDiscount);
                        appliedCombos.add(rule);
                        processedCategories.add(catId);
                        break;
                    }
                }
            }
        }
        summary.setComboDiscount(comboDiscount);
        summary.setAppliedComboRules(appliedCombos);

        BigDecimal eligibleAfterCombo = subtotal.subtract(comboDiscount);
        if (eligibleAfterCombo.compareTo(BigDecimal.ZERO) < 0) eligibleAfterCombo = BigDecimal.ZERO;

        // 3. Tính Voucher Discount
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank()) {
            String normCode = voucherCode.trim().toUpperCase();
            Optional<Promotion> promoOpt = promotionRepository.findByCodeAndIsActiveTrue(normCode);
            if (promoOpt.isPresent()) {
                Promotion promo = promoOpt.get();
                LocalDateTime now = LocalDateTime.now();

                boolean valid = true;
                if (promo.getStartDate() != null && now.isBefore(promo.getStartDate())) {
                    summary.getMessages().add("Mã giảm giá " + normCode + " chưa đến thời gian áp dụng.");
                    valid = false;
                }
                if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
                    summary.getMessages().add("Mã giảm giá " + normCode + " đã hết hạn sử dụng.");
                    valid = false;
                }
                if (promo.getMinOrderValue() != null && eligibleAfterCombo.compareTo(promo.getMinOrderValue()) < 0) {
                    summary.getMessages().add("Đơn hàng chưa đạt giá trị tối thiểu " + promo.getMinOrderValue() + "đ để áp dụng voucher.");
                    valid = false;
                }
                if (promo.getUsageLimit() != null) {
                    long used = promotionUsageRepository.countByPromotionId(promo.getId());
                    if (used >= promo.getUsageLimit()) {
                        summary.getMessages().add("Mã giảm giá " + normCode + " đã hết lượt sử dụng.");
                        valid = false;
                    }
                }
                if (user != null && promo.getId() != null) {
                    boolean alreadyUsed = promotionUsageRepository.existsByPromotionIdAndUserId(promo.getId(), user.getId());
                    if (alreadyUsed) {
                        summary.getMessages().add("Bạn đã từng sử dụng mã giảm giá này cho một đơn hàng trước đó.");
                        valid = false;
                    }
                }

                if (valid) {
                    if ("PERCENTAGE".equalsIgnoreCase(promo.getDiscountType())) {
                        BigDecimal pct = promo.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        voucherDiscount = eligibleAfterCombo.multiply(pct).setScale(0, RoundingMode.HALF_UP);
                        if (promo.getMaxDiscountAmount() != null && voucherDiscount.compareTo(promo.getMaxDiscountAmount()) > 0) {
                            voucherDiscount = promo.getMaxDiscountAmount();
                        }
                    } else {
                        voucherDiscount = promo.getDiscountValue();
                        if (voucherDiscount.compareTo(eligibleAfterCombo) > 0) {
                            voucherDiscount = eligibleAfterCombo;
                        }
                    }
                    summary.setAppliedPromotion(promo);
                }
            } else {
                summary.getMessages().add("Mã giảm giá không hợp lệ hoặc đã bị vô hiệu hóa.");
            }
        }
        summary.setVoucherDiscount(voucherDiscount);

        BigDecimal eligibleAfterVoucher = eligibleAfterCombo.subtract(voucherDiscount);
        if (eligibleAfterVoucher.compareTo(BigDecimal.ZERO) < 0) eligibleAfterVoucher = BigDecimal.ZERO;

        // 4. Giới hạn CoolCash: Tối đa 50% giá trị hợp lệ sau khuyến mãi
        BigDecimal coolCashUsed = BigDecimal.ZERO;
        if (requestedCoolCash != null && requestedCoolCash.compareTo(BigDecimal.ZERO) > 0 && user != null) {
            BigDecimal maxAllowedCoolCash = eligibleAfterVoucher.multiply(BigDecimal.valueOf(0.5)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal userBalance = user.getCoolcashBalance() != null ? user.getCoolcashBalance() : BigDecimal.ZERO;

            BigDecimal usable = requestedCoolCash.min(userBalance).min(maxAllowedCoolCash);
            if (usable.compareTo(BigDecimal.ZERO) > 0) {
                coolCashUsed = usable;
            }
        }
        summary.setCoolcashUsed(coolCashUsed);

        // 5. Phí vận chuyển & Freeship
        BigDecimal shippingFee = shippingFeeService.calculateShippingFee(eligibleAfterCombo, provinceName, districtName);
        summary.setShippingFee(shippingFee);

        boolean isFree = shippingFee.compareTo(BigDecimal.ZERO) == 0;
        summary.setFreeshipQualified(isFree);

        BigDecimal threshold = ShippingFeeService.FREESHIP_THRESHOLD;
        if (eligibleAfterCombo.compareTo(threshold) < 0) {
            summary.setRemainingToFreeShip(threshold.subtract(eligibleAfterCombo));
        } else {
            summary.setRemainingToFreeShip(BigDecimal.ZERO);
        }

        // 6. Tổng thanh toán cuối cùng
        BigDecimal finalAmount = eligibleAfterVoucher.subtract(coolCashUsed).add(shippingFee);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;
        summary.setFinalAmount(finalAmount);

        return summary;
    }
}
