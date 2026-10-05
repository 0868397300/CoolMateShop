package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.OrderItem;
import sopvn.demo.entity.ProductVariant;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.ProductRepository;
import sopvn.demo.repository.ProductVariantRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminDashboardController(ProductRepository productRepository,
                                    ProductVariantRepository productVariantRepository,
                                    OrderRepository orderRepository,
                                    UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<ProductVariant> allVariants = productVariantRepository.findByIsActiveTrue();

        // 1. Tính Doanh thu thuần (Net Revenue): Các đơn giao thành công (COMPLETED, DELIVERED)
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCOGS = BigDecimal.ZERO; // Giá vốn hàng bán (Cost of Goods Sold)
        long successfulOrdersCount = 0;

        for (Order o : allOrders) {
            String st = o.getOrderStatus();
            if ("COMPLETED".equalsIgnoreCase(st) || "DELIVERED".equalsIgnoreCase(st)) {
                successfulOrdersCount++;
                if (o.getFinalAmount() != null) {
                    totalRevenue = totalRevenue.add(o.getFinalAmount());
                }
                if (o.getItems() != null) {
                    for (OrderItem item : o.getItems()) {
                        ProductVariant v = item.getVariant();
                        BigDecimal importPrice = (v != null && v.getImportPrice() != null) ? v.getImportPrice() : BigDecimal.ZERO;
                        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                        totalCOGS = totalCOGS.add(importPrice.multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }
        }

        // 2. Lợi nhuận gộp (Gross Profit) = Doanh thu thuần - Giá vốn hàng bán
        BigDecimal grossProfit = totalRevenue.subtract(totalCOGS);

        // 3. Tỷ suất lợi nhuận biên (Profit Margin %) = (Lợi nhuận gộp / Doanh thu) * 100
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = grossProfit.multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 1, RoundingMode.HALF_UP);
        }

        // 4. Giá trị hàng tồn kho hiện tại = Σ (Tồn kho * Giá vốn)
        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        for (ProductVariant v : allVariants) {
            BigDecimal importPrice = v.getImportPrice() != null ? v.getImportPrice() : BigDecimal.ZERO;
            int stock = v.getStockQuantity() != null ? v.getStockQuantity() : 0;
            totalInventoryValue = totalInventoryValue.add(importPrice.multiply(BigDecimal.valueOf(stock)));
        }

        // 5. Cảnh báo sản phẩm sắp hết hàng (Tồn kho <= 40)
        List<ProductVariant> lowStockVariants = productVariantRepository
                .findByStockQuantityLessThanEqualAndIsActiveTrueOrderByStockQuantityAsc(40);

        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("successfulOrdersCount", successfulOrdersCount);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalReceipts", 0);

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCOGS", totalCOGS);
        model.addAttribute("grossProfit", grossProfit);
        model.addAttribute("profitMargin", profitMargin);
        model.addAttribute("totalInventoryValue", totalInventoryValue);

        model.addAttribute("lowStockVariants", lowStockVariants);
        model.addAttribute("recentOrders", allOrders.stream().limit(5).toList());
        model.addAttribute("recentReceipts", Collections.emptyList());

        return "admin/dashboard";
    }
}
