package sopvn.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Promotion_Usages")
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "discount_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "RESERVED"; // RESERVED, FINALIZED, RELEASED

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    public PromotionUsage() {}

    public PromotionUsage(Promotion promotion, User user, Order order) {
        this.promotion = promotion;
        this.user = user;
        this.order = order;
        this.status = "RESERVED";
        this.discountAmount = order != null && order.getVoucherDiscountAmount() != null ? order.getVoucherDiscountAmount() : BigDecimal.ZERO;
    }

    public PromotionUsage(Promotion promotion, User user, Order order, BigDecimal discountAmount) {
        this.promotion = promotion;
        this.user = user;
        this.order = order;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.status = "RESERVED";
    }

    public PromotionUsage(Promotion promotion, User user, Order order, BigDecimal discountAmount, String status) {
        this.promotion = promotion;
        this.user = user;
        this.order = order;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.status = status != null ? status : "RESERVED";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
