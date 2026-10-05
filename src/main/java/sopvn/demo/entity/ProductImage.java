package sopvn.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Product_Images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "is_thumbnail", nullable = false)
    private Boolean isThumbnail = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public ProductImage() {
    }

    public ProductImage(Product product, Color color, String imageUrl, Boolean isThumbnail, Integer displayOrder) {
        this.product = product;
        this.color = color;
        this.imageUrl = imageUrl;
        this.isThumbnail = isThumbnail;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getImageUrl() {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return "/images/products/ao-thun-compact-den-1.jpg";
        }
        if (imageUrl.contains("coolmate.me")) {
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            return "/images/products/" + filename;
        }
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsThumbnail() {
        return isThumbnail;
    }

    public void setIsThumbnail(Boolean isThumbnail) {
        this.isThumbnail = isThumbnail;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
