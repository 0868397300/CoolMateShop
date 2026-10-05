package sopvn.demo.inventory.dto;

import sopvn.demo.entity.ProductVariant;
import java.math.BigDecimal;
import java.util.List;

public class InventoryStatsDTO {
    private long totalReceipts;
    private BigDecimal totalSpentThisMonth;
    private long totalUnitsImported;
    private BigDecimal totalInventoryValue;
    private List<ProductVariant> lowStockVariants;

    public InventoryStatsDTO() {
    }

    public long getTotalReceipts() {
        return totalReceipts;
    }

    public void setTotalReceipts(long totalReceipts) {
        this.totalReceipts = totalReceipts;
    }

    public BigDecimal getTotalSpentThisMonth() {
        return totalSpentThisMonth;
    }

    public void setTotalSpentThisMonth(BigDecimal totalSpentThisMonth) {
        this.totalSpentThisMonth = totalSpentThisMonth;
    }

    public long getTotalUnitsImported() {
        return totalUnitsImported;
    }

    public void setTotalUnitsImported(long totalUnitsImported) {
        this.totalUnitsImported = totalUnitsImported;
    }

    public BigDecimal getTotalInventoryValue() {
        return totalInventoryValue;
    }

    public void setTotalInventoryValue(BigDecimal totalInventoryValue) {
        this.totalInventoryValue = totalInventoryValue;
    }

    public List<ProductVariant> getLowStockVariants() {
        return lowStockVariants;
    }

    public void setLowStockVariants(List<ProductVariant> lowStockVariants) {
        this.lowStockVariants = lowStockVariants;
    }
}
