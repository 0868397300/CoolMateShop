package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sopvn.demo.entity.InventoryReceipt;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.OrderItem;
import sopvn.demo.entity.ProductVariant;
import sopvn.demo.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;
    private final OrderReturnRepository orderReturnRepository;

    public AdminDashboardController(ProductRepository productRepository,
                                    ProductVariantRepository productVariantRepository,
                                    OrderRepository orderRepository,
                                    UserRepository userRepository,
                                    InventoryReceiptRepository inventoryReceiptRepository,
                                    OrderReturnRepository orderReturnRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.inventoryReceiptRepository = inventoryReceiptRepository;
        this.orderReturnRepository = orderReturnRepository;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(@RequestParam(value = "range", required = false, defaultValue = "30days") String range,
                            Model model) {
        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<ProductVariant> allVariants = productVariantRepository.findByIsActiveTrue();
        List<InventoryReceipt> allReceipts = inventoryReceiptRepository.findAll();

        LocalDateTime filterDate = LocalDateTime.now().minusDays(30);
        if ("today".equalsIgnoreCase(range)) {
            filterDate = LocalDate.now().atStartOfDay();
        } else if ("7days".equalsIgnoreCase(range)) {
            filterDate = LocalDateTime.now().minusDays(7);
        } else if ("thisMonth".equalsIgnoreCase(range)) {
            filterDate = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        } else if ("all".equalsIgnoreCase(range)) {
            filterDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        }

        final LocalDateTime finalFilter = filterDate;
        List<Order> filteredOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(finalFilter))
                .collect(Collectors.toList());

        // 1. B3: Tính Doanh Thu Thuần & Giá Vốn Lịch Sử (Historical COGS via costPriceSnapshot)
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCOGS = BigDecimal.ZERO;
        long successfulOrdersCount = 0;
        long pendingOrdersCount = 0;
        long cancelledOrdersCount = 0;

        for (Order o : filteredOrders) {
            String st = o.getOrderStatus();
            if ("COMPLETED".equalsIgnoreCase(st) || "DELIVERED".equalsIgnoreCase(st)) {
                successfulOrdersCount++;
                if (o.getFinalAmount() != null) {
                    totalRevenue = totalRevenue.add(o.getFinalAmount());
                }
                if (o.getItems() != null) {
                    for (OrderItem item : o.getItems()) {
                        // B3: Sử dụng Snapshot giá vốn lịch sử tại thời điểm đặt hàng
                        BigDecimal costPrice = item.getCostPriceSnapshot();
                        if (costPrice == null) {
                            ProductVariant v = item.getVariant();
                            costPrice = (v != null && v.getImportPrice() != null) ? v.getImportPrice() : BigDecimal.ZERO;
                        }
                        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                        totalCOGS = totalCOGS.add(costPrice.multiply(BigDecimal.valueOf(qty)));
                    }
                }
            } else if ("PENDING".equalsIgnoreCase(st)) {
                pendingOrdersCount++;
            } else if ("CANCELLED".equalsIgnoreCase(st)) {
                cancelledOrdersCount++;
            }
        }

        // 2. Lợi nhuận gộp & Biên lợi nhuận
        BigDecimal grossProfit = totalRevenue.subtract(totalCOGS);
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = grossProfit.multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 1, RoundingMode.HALF_UP);
        }

        // 3. Giá trị hàng tồn kho = Σ (Tồn kho * Giá vốn hiện tại)
        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        for (ProductVariant v : allVariants) {
            BigDecimal importPrice = v.getImportPrice() != null ? v.getImportPrice() : BigDecimal.ZERO;
            int stock = v.getStockQuantity() != null ? v.getStockQuantity() : 0;
            totalInventoryValue = totalInventoryValue.add(importPrice.multiply(BigDecimal.valueOf(stock)));
        }

        // 4. Cảnh báo sản phẩm sắp hết hàng
        List<ProductVariant> lowStockVariants = productVariantRepository
                .findByStockQuantityLessThanEqualAndIsActiveTrueOrderByStockQuantityAsc(40);

        // 5. N: Tính dữ liệu biểu đồ doanh thu theo ngày từ dữ liệu thực tế
        Map<String, BigDecimal> dailyRevenueMap = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            dailyRevenueMap.put(d.format(dayFormatter), BigDecimal.ZERO);
        }

        for (Order o : allOrders) {
            if (("COMPLETED".equalsIgnoreCase(o.getOrderStatus()) || "DELIVERED".equalsIgnoreCase(o.getOrderStatus())) 
                && o.getCreatedAt() != null && o.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))) {
                String dStr = o.getCreatedAt().format(dayFormatter);
                if (dailyRevenueMap.containsKey(dStr) && o.getFinalAmount() != null) {
                    dailyRevenueMap.put(dStr, dailyRevenueMap.get(dStr).add(o.getFinalAmount()));
                }
            }
        }

        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("filteredOrdersCount", filteredOrders.size());
        model.addAttribute("successfulOrdersCount", successfulOrdersCount);
        model.addAttribute("pendingOrdersCount", pendingOrdersCount);
        model.addAttribute("cancelledOrdersCount", cancelledOrdersCount);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalReceipts", allReceipts.size());
        model.addAttribute("totalReturns", orderReturnRepository.count());

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCOGS", totalCOGS);
        model.addAttribute("grossProfit", grossProfit);
        model.addAttribute("profitMargin", profitMargin);
        model.addAttribute("totalInventoryValue", totalInventoryValue);

        model.addAttribute("selectedRange", range);
        model.addAttribute("lowStockVariants", lowStockVariants);
        model.addAttribute("recentOrders", allOrders.stream().limit(6).collect(Collectors.toList()));
        model.addAttribute("recentReceipts", allReceipts.stream().sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt())).limit(5).collect(Collectors.toList()));
        model.addAttribute("chartDays", new ArrayList<>(dailyRevenueMap.keySet()));
        model.addAttribute("chartRevenues", new ArrayList<>(dailyRevenueMap.values()));

        return "admin/dashboard";
    }
}