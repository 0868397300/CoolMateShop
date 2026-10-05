package sopvn.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "Products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @Column(name = "name", length = 250, nullable = false)
    private String name;

    @Column(name = "slug", length = 250, nullable = false, unique = true)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "material", length = 200)
    private String material;

    @Column(name = "fit_type", length = 50)
    private String fitType;

    @Column(name = "features", length = 500)
    private String features;

    @Column(name = "base_price", precision = 18, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @Column(name = "rating_avg", precision = 3, scale = 2, nullable = false)
    private BigDecimal ratingAvg = BigDecimal.valueOf(5.0);

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "sold_count", nullable = false)
    private Integer soldCount = 0;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductImage> images = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductVariant> variants = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new LinkedHashSet<>();

    public Product() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getFitType() {
        return fitType;
    }

    public void setFitType(String fitType) {
        this.fitType = fitType;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public void setRatingAvg(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(Integer soldCount) {
        this.soldCount = soldCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<ProductImage> getImages() {
        return images;
    }

    public void setImages(Set<ProductImage> images) {
        this.images = images;
    }

    public Set<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(Set<ProductVariant> variants) {
        this.variants = variants;
    }

    public Set<Review> getReviews() {
        return reviews;
    }

    public void setReviews(Set<Review> reviews) {
        this.reviews = reviews;
    }

    public String getThumbnailUrl() {
        if (images != null) {
            for (ProductImage img : images) {
                if (Boolean.TRUE.equals(img.getIsThumbnail())) {
                    return img.getImageUrl();
                }
            }
            if (!images.isEmpty()) {
                return images.iterator().next().getImageUrl();
            }
        }
        return "/images/products/ao-thun-compact-den-1.jpg";
    }

    public BigDecimal getMinSalePrice() {
        if (variants != null && !variants.isEmpty()) {
            return variants.stream()
                    .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                    .map(ProductVariant::getSalePrice)
                    .min(BigDecimal::compareTo)
                    .orElse(basePrice);
        }
        return basePrice;
    }

    public BigDecimal getMaxOriginalPrice() {
        if (variants != null && !variants.isEmpty()) {
            return variants.stream()
                    .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                    .map(ProductVariant::getOriginalPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(basePrice);
        }
        return basePrice;
    }

    public Integer getDiscountPercent() {
        BigDecimal orig = getMaxOriginalPrice();
        BigDecimal sale = getMinSalePrice();
        if (orig != null && sale != null && orig.compareTo(sale) > 0 && orig.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = orig.subtract(sale);
            return diff.multiply(BigDecimal.valueOf(100)).divide(orig, 0, RoundingMode.HALF_UP).intValue();
        }
        return 0;
    }

    public Set<Color> getDistinctColors() {
        Set<Color> set = new LinkedHashSet<>();
        if (variants != null) {
            for (ProductVariant v : variants) {
                if (v.getColor() != null) {
                    set.add(v.getColor());
                }
            }
        }
        return set;
    }

    public Set<Size> getDistinctSizes() {
        Set<Size> set = new LinkedHashSet<>();
        if (variants != null) {
            for (ProductVariant v : variants) {
                if (v.getSize() != null) {
                    set.add(v.getSize());
                }
            }
        }
        return set;
    }

    public String getImageUrlForColor(Integer colorId) {
        if (images != null && colorId != null) {
            for (ProductImage img : images) {
                if (img.getColor() != null && colorId.equals(img.getColor().getId())) {
                    return img.getImageUrl();
                }
            }
        }
        return getThumbnailUrl();
    }
}
