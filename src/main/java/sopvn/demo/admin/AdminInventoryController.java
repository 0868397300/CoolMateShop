package sopvn.demo.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.InventoryReceipt;
import sopvn.demo.entity.InventoryReceiptItem;
import sopvn.demo.entity.ProductVariant;
import sopvn.demo.entity.User;
import sopvn.demo.inventory.InventoryService;
import sopvn.demo.inventory.StockService;
import sopvn.demo.inventory.dto.InventoryReceiptItemRequest;
import sopvn.demo.inventory.dto.InventoryReceiptRequest;
import sopvn.demo.inventory.dto.InventoryStatsDTO;
import sopvn.demo.repository.InventoryMovementRepository;
import sopvn.demo.repository.InventoryReceiptRepository;
import sopvn.demo.repository.ProductVariantRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/nhap-kho")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final StockService stockService;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final UserRepository userRepository;

    public AdminInventoryController(InventoryService inventoryService,
                                    StockService stockService,
                                    ProductVariantRepository productVariantRepository,
                                    InventoryReceiptRepository inventoryReceiptRepository,
                                    InventoryMovementRepository inventoryMovementRepository,
                                    UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.stockService = stockService;
        this.productVariantRepository = productVariantRepository;
        this.inventoryReceiptRepository = inventoryReceiptRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listReceipts(Model model) {
        List<InventoryReceipt> receipts = inventoryReceiptRepository.findAll();
        InventoryStatsDTO stats = inventoryService.getInventoryStats();

        model.addAttribute("receipts", receipts);
        model.addAttribute("stats", stats);
        return "admin/inventory-list";
    }

    @GetMapping("/tao-moi")
    public String showCreateForm(Model model) {
        List<ProductVariant> variants = productVariantRepository.findByIsActiveTrue();
        model.addAttribute("variants", variants);
        return "admin/inventory-create";
    }

    @PostMapping("/tao-moi")
    public String createReceipt(@RequestParam("supplierName") String supplierName,
                                @RequestParam(value = "note", required = false) String note,
                                @RequestParam("variantId") List<Long> variantIds,
                                @RequestParam("quantity") List<Integer> quantities,
                                @RequestParam("importPrice") List<java.math.BigDecimal> importPrices,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(principal.getName()).orElseThrow();
            InventoryReceiptRequest req = new InventoryReceiptRequest();
            req.setSupplierName(supplierName);
            req.setNote(note);

            List<InventoryReceiptItemRequest> items = new ArrayList<>();
            for (int i = 0; i < variantIds.size(); i++) {
                if (variantIds.get(i) != null && quantities.get(i) != null && quantities.get(i) > 0) {
                    InventoryReceiptItemRequest it = new InventoryReceiptItemRequest();
                    it.setVariantId(variantIds.get(i));
                    it.setQuantity(quantities.get(i));
                    it.setImportPrice(importPrices.get(i));
                    items.add(it);
                }
            }
            req.setItems(items);

            InventoryReceipt created = inventoryService.createReceipt(req, user);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Phiếu nhập kho #" + created.getReceiptCode() + " đã được tạo ở trạng thái SUBMITTED, chờ Admin phê duyệt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo phiếu nhập kho: " + e.getMessage());
            return "redirect:/admin/nhap-kho/tao-moi";
        }
        return "redirect:/admin/nhap-kho";
    }

    @GetMapping("/{id}")
    public String viewDetail(@PathVariable("id") Long id, Model model) {
        InventoryReceipt receipt = inventoryReceiptRepository.findById(id).orElseThrow();
        model.addAttribute("receipt", receipt);
        return "admin/inventory-detail";
    }

    /**
     * Admin phê duyệt phiếu nhập hàng: Tăng tồn kho & Cập nhật Weighted Average Cost
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/duyet")
    public String approveReceipt(@PathVariable("id") Long id,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User admin = userRepository.findByEmail(principal.getName()).orElseThrow();
            InventoryReceipt receipt = inventoryReceiptRepository.findById(id).orElseThrow();
            stockService.applyInventoryReceipt(receipt, admin);
            inventoryReceiptRepository.save(receipt);

            redirectAttributes.addFlashAttribute("successMessage", 
                    "Phê duyệt phiếu nhập kho #" + receipt.getReceiptCode() + " thành công! Đã tự động cập nhật giá vốn bình quân và số lượng tồn kho.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi duyệt phiếu: " + e.getMessage());
        }
        return "redirect:/admin/nhap-kho/" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/tu-choi")
    public String rejectReceipt(@PathVariable("id") Long id,
                                @RequestParam(value = "reason", required = false) String reason,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            User admin = userRepository.findByEmail(principal.getName()).orElseThrow();
            InventoryReceipt receipt = inventoryReceiptRepository.findById(id).orElseThrow();
            if (!"SUBMITTED".equalsIgnoreCase(receipt.getStatus()) && !"PENDING".equalsIgnoreCase(receipt.getStatus())) {
                throw new CustomException("Phiếu nhập kho này đã được xử lý trước đó (trạng thái: " + receipt.getStatus() + ")!");
            }
            receipt.setStatus("REJECTED");
            receipt.setRejectedReason(reason != null ? reason : "Không đạt chất lượng kiểm định.");
            receipt.setApprovedBy(admin);
            receipt.setApprovedAt(java.time.LocalDateTime.now());
            inventoryReceiptRepository.save(receipt);

            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối phiếu nhập kho #" + receipt.getReceiptCode());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/nhap-kho/" + id;
    }
}