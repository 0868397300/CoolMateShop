package sopvn.demo.cart.dto;

import sopvn.demo.entity.ComboRule;
import sopvn.demo.entity.Promotion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PricingSummaryDTO {

    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal comboDiscount = BigDecimal.ZERO;
    private BigDecimal voucherDiscount = BigDecimal.ZERO;
    private BigDecimal coolcashUsed = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;
    private BigDecimal remainingToFreeShip = BigDecimal.ZERO;
    private boolean freeshipQualified = false;

    private Promotion appliedPromotion;
    private List<ComboRule> appliedComboRules = new ArrayList<>();
    private List<String> messages = new ArrayList<>();

    public PricingSummaryDTO() {}

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO; }

    public BigDecimal getComboDiscount() { return comboDiscount; }
    public void setComboDiscount(BigDecimal comboDiscount) { this.comboDiscount = comboDiscount != null ? comboDiscount : BigDecimal.ZERO; }

    public BigDecimal getVoucherDiscount() { return voucherDiscount; }
    public void setVoucherDiscount(BigDecimal voucherDiscount) { this.voucherDiscount = voucherDiscount != null ? voucherDiscount : BigDecimal.ZERO; }

    public BigDecimal getCoolcashUsed() { return coolcashUsed; }
    public void setCoolcashUsed(BigDecimal coolcashUsed) { this.coolcashUsed = coolcashUsed != null ? coolcashUsed : BigDecimal.ZERO; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee != null ? shippingFee : BigDecimal.ZERO; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount != null ? finalAmount : BigDecimal.ZERO; }

    public BigDecimal getRemainingToFreeShip() { return remainingToFreeShip; }
    public void setRemainingToFreeShip(BigDecimal remainingToFreeShip) { this.remainingToFreeShip = remainingToFreeShip != null ? remainingToFreeShip : BigDecimal.ZERO; }

    public boolean isFreeshipQualified() { return freeshipQualified; }
    public void setFreeshipQualified(boolean freeshipQualified) { this.freeshipQualified = freeshipQualified; }

    public Promotion getAppliedPromotion() { return appliedPromotion; }
    public void setAppliedPromotion(Promotion appliedPromotion) { this.appliedPromotion = appliedPromotion; }

    public List<ComboRule> getAppliedComboRules() { return appliedComboRules; }
    public void setAppliedComboRules(List<ComboRule> appliedComboRules) { this.appliedComboRules = appliedComboRules != null ? appliedComboRules : new ArrayList<>(); }

    public List<String> getMessages() { return messages; }
    public void setMessages(List<String> messages) { this.messages = messages != null ? messages : new ArrayList<>(); }
}
