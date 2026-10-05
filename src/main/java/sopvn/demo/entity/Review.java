package sopvn.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private OrderItem orderItem;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 1000, nullable = false)
    private String comment;

    @Column(name = "customer_height_cm")
    private Integer customerHeightCm;

    @Column(name = "customer_weight_kg")
    private Integer customerWeightKg;

    @Column(name = "purchased_color", length = 50, nullable = false)
    private String purchasedColor;

    @Column(name = "purchased_size", length = 20, nullable = false)
    private String purchasedSize;

    @Column(name = "fit_feedback", length = 30, nullable = false)
    private String fitFeedback = "TRUE_TO_SIZE";

    @Column(name = "image_urls", length = 1000)
    private String imageUrls;

    @Column(name = "admin_reply", length = 1000)
    private String adminReply;

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;
    @Column(name = "status", length = 30)
    private String status = "PENDING"; // PENDING, APPROVED, HIDDEN

    @Column(name = "admin_replied_at")
    private LocalDateTime adminRepliedAt;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Review() {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getCustomerHeightCm() {
        return customerHeightCm;
    }

    public void setCustomerHeightCm(Integer customerHeightCm) {
        this.customerHeightCm = customerHeightCm;
    }

    public Integer getCustomerWeightKg() {
        return customerWeightKg;
    }

    public void setCustomerWeightKg(Integer customerWeightKg) {
        this.customerWeightKg = customerWeightKg;
    }

    public String getPurchasedColor() {
        return purchasedColor;
    }

    public void setPurchasedColor(String purchasedColor) {
        this.purchasedColor = purchasedColor;
    }

    public String getPurchasedSize() {
        return purchasedSize;
    }

    public void setPurchasedSize(String purchasedSize) {
        this.purchasedSize = purchasedSize;
    }

    public String getFitFeedback() {
        return fitFeedback;
    }

    public void setFitFeedback(String fitFeedback) {
        this.fitFeedback = fitFeedback;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public Boolean getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(Boolean isApproved) {
        this.isApproved = isApproved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAdminRepliedAt() { return adminRepliedAt; }
    public void setAdminRepliedAt(LocalDateTime adminRepliedAt) { this.adminRepliedAt = adminRepliedAt; }

}