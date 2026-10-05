package sopvn.demo.inventory.dto;

import java.math.BigDecimal;

public class InventoryReceiptItemRequest {
    private Long variantId;
    private Integer quantity;
    private BigDecimal importPrice;

    public InventoryReceiptItemRequest() {
    }

    public InventoryReceiptItemRequest(Long variantId, Integer quantity, BigDecimal importPrice) {
        this.variantId = variantId;
        this.quantity = quantity;
        this.importPrice = importPrice;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }
}
