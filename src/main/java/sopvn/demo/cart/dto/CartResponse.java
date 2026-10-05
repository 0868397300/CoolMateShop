package sopvn.demo.cart.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartResponse {
    private List<CartItemDto> items = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal comboDiscount = BigDecimal.ZERO;
    private BigDecimal voucherDiscount = BigDecimal.ZERO;
    private BigDecimal coolcashUsed = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal finalTotal = BigDecimal.ZERO;
    private Integer totalQuantity = 0;
    private Boolean isFreeshipQualified = false;
    private BigDecimal freeshipThresholdRemaining = BigDecimal.ZERO;

    public CartResponse() {
    }

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getComboDiscount() { return comboDiscount; }
    public void setComboDiscount(BigDecimal comboDiscount) { this.comboDiscount = comboDiscount; }

    public BigDecimal getVoucherDiscount() { return voucherDiscount; }
    public void setVoucherDiscount(BigDecimal voucherDiscount) { this.voucherDiscount = voucherDiscount; }

    public BigDecimal getCoolcashUsed() { return coolcashUsed; }
    public void setCoolcashUsed(BigDecimal coolcashUsed) { this.coolcashUsed = coolcashUsed; }

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }

    public BigDecimal getFinalTotal() { return finalTotal; }
    public void setFinalTotal(BigDecimal finalTotal) { this.finalTotal = finalTotal; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Boolean getIsFreeshipQualified() { return isFreeshipQualified; }
    public void setIsFreeshipQualified(Boolean isFreeshipQualified) { this.isFreeshipQualified = isFreeshipQualified; }
    public Boolean isFreeshipQualified() { return isFreeshipQualified; }
    public void setFreeshipQualified(Boolean freeshipQualified) { this.isFreeshipQualified = freeshipQualified; }

    public BigDecimal getFreeshipThresholdRemaining() { return freeshipThresholdRemaining; }
    public void setFreeshipThresholdRemaining(BigDecimal freeshipThresholdRemaining) { this.freeshipThresholdRemaining = freeshipThresholdRemaining; }

    // Aliases to support both naming conventions seamlessly
    public BigDecimal getDiscountAmount() { return comboDiscount != null ? comboDiscount : BigDecimal.ZERO; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.comboDiscount = discountAmount; }

    public BigDecimal getTotalAmount() { return finalTotal != null ? finalTotal : BigDecimal.ZERO; }
    public void setTotalAmount(BigDecimal totalAmount) { this.finalTotal = totalAmount; }
}
