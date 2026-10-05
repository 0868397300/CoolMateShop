package sopvn.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Sizes")
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 20, nullable = false, unique = true)
    private String name;

    @Column(name = "min_height_cm", nullable = false)
    private Integer minHeightCm;

    @Column(name = "max_height_cm", nullable = false)
    private Integer maxHeightCm;

    @Column(name = "min_weight_kg", nullable = false)
    private Integer minWeightKg;

    @Column(name = "max_weight_kg", nullable = false)
    private Integer maxWeightKg;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public Size() {
    }

    public Size(String name, Integer minHeightCm, Integer maxHeightCm, Integer minWeightKg, Integer maxWeightKg, Integer displayOrder) {
        this.name = name;
        this.minHeightCm = minHeightCm;
        this.maxHeightCm = maxHeightCm;
        this.minWeightKg = minWeightKg;
        this.maxWeightKg = maxWeightKg;
        this.displayOrder = displayOrder;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMinHeightCm() {
        return minHeightCm;
    }

    public void setMinHeightCm(Integer minHeightCm) {
        this.minHeightCm = minHeightCm;
    }

    public Integer getMaxHeightCm() {
        return maxHeightCm;
    }

    public void setMaxHeightCm(Integer maxHeightCm) {
        this.maxHeightCm = maxHeightCm;
    }

    public Integer getMinWeightKg() {
        return minWeightKg;
    }

    public void setMinWeightKg(Integer minWeightKg) {
        this.minWeightKg = minWeightKg;
    }

    public Integer getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(Integer maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
