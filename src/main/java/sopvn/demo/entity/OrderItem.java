package sopvn.demo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Order_Items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "product_name_snapshot", length = 250, nullable = false)
    private String productNameSnapshot;

    @Column(name = "sku_snapshot", length = 100, nullable = false)
    private String skuSnapshot;

    @Column(name = "color_name_snapshot", length = 50, nullable = false)
    private String colorNameSnapshot;

    @Column(name = "size_name_snapshot", length = 20, nullable = false)
    private String sizeNameSnapshot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "is_reviewed", nullable = false)
    private Boolean isReviewed = false;
    @Column(name = "cost_price_snapshot", precision = 18, scale = 2)
    private BigDecimal costPriceSnapshot;


    public OrderItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    public String getSkuSnapshot() {
        return skuSnapshot;
    }

    public void setSkuSnapshot(String skuSnapshot) {
        this.skuSnapshot = skuSnapshot;
    }

    public String getColorNameSnapshot() {
        return colorNameSnapshot;
    }

    public void setColorNameSnapshot(String colorNameSnapshot) {
        this.colorNameSnapshot = colorNameSnapshot;
    }

    public String getSizeNameSnapshot() {
        return sizeNameSnapshot;
    }

    public void setSizeNameSnapshot(String sizeNameSnapshot) {
        this.sizeNameSnapshot = sizeNameSnapshot;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Boolean getIsReviewed() {
        return isReviewed;
    }

    public void setIsReviewed(Boolean isReviewed) {
        this.isReviewed = isReviewed;
    }

    public BigDecimal getCostPriceSnapshot() {
        return costPriceSnapshot;
    }

    public void setCostPriceSnapshot(BigDecimal costPriceSnapshot) {
        this.costPriceSnapshot = costPriceSnapshot;
    }

}