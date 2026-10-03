package sopvn.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, columnDefinition = "NVARCHAR(50)")
    private String name;

    @Column(name = "min_height_cm")
    private Integer minHeightCm;

    @Column(name = "max_height_cm")
    private Integer maxHeightCm;

    @Column(name = "min_weight_kg", precision = 5, scale = 2)
    private BigDecimal minWeightKg;

    @Column(name = "max_weight_kg", precision = 5, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "display_order")
    private Integer displayOrder;
}
