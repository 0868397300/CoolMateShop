package sopvn.demo.cart.dto;

import java.math.BigDecimal;

public class CartItemDto {
    private Long id;
    private Long variantId;
    private Long productId;
    private String productName;
    private String productSlug;
    private String sku;
    private String colorName;
    private String colorHex;
    private String sizeName;
    private String imageUrl;
    private BigDecimal price;
    private Integer quantity;
    private Integer stockQuantity;
    private BigDecimal itemSubtotal;

    public CartItemDto() {
    }

    public CartItemDto(Long id, Long variantId, Long productId, String productName, String productSlug, String sku, String colorName, String colorHex, String sizeName, String imageUrl, BigDecimal price, Integer quantity, Integer stockQuantity, BigDecimal itemSubtotal) {
        this.id = id;
        this.variantId = variantId;
        this.productId = productId;
        this.productName = productName;
        this.productSlug = productSlug;
        this.sku = sku;
        this.colorName = colorName;
        this.colorHex = colorHex;
        this.sizeName = sizeName;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
        this.stockQuantity = stockQuantity;
        this.itemSubtotal = itemSubtotal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductSlug() { return productSlug; }
    public void setProductSlug(String productSlug) { this.productSlug = productSlug; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public String getSizeName() { return sizeName; }
    public void setSizeName(String sizeName) { this.sizeName = sizeName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSalePrice() { return price; }
    public void setSalePrice(BigDecimal salePrice) { this.price = salePrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public BigDecimal getItemSubtotal() { return itemSubtotal; }
    public void setItemSubtotal(BigDecimal itemSubtotal) { this.itemSubtotal = itemSubtotal; }
    public BigDecimal getLineTotal() { return itemSubtotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.itemSubtotal = lineTotal; }
}
