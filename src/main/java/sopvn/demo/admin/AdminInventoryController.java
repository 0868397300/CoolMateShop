package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.InventoryReceipt;
import sopvn.demo.entity.ProductVariant;
import sopvn.demo.entity.User;
import sopvn.demo.inventory.InventoryService;
import sopvn.demo.inventory.dto.InventoryReceiptItemRequest;
import sopvn.demo.inventory.dto.InventoryReceiptRequest;
import sopvn.demo.inventory.dto.InventoryStatsDTO;
import sopvn.demo.repository.ProductVariantRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/nhap-kho")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    public AdminInventoryController(InventoryService inventoryService,
                                    ProductVariantRepository variantRepository,
                                    UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listReceipts(Model model) {
        List<InventoryReceipt> receipts = inventoryService.getAllReceipts();
        InventoryStatsDTO stats = inventoryService.getInventoryStats();

        model.addAttribute("receipts", receipts);
        model.addAttribute("stats", stats);
        return "admin/inventory-list";
    }

    @GetMapping("/tao-moi")
    public String createReceiptForm(@RequestParam(value = "sku", required = false) String prefillSku, Model model) {
        List<ProductVariant> variants = variantRepository.findByIsActiveTrue();
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        String defaultCode = "PNK" + LocalDateTime.now().format(dtf);

        List<java.util.Map<String, Object>> variantData = new java.util.ArrayList<>();
        for (ProductVariant v : variants) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", v.getId());
            map.put("sku", v.getSku());
            map.put("stockQuantity", v.getStockQuantity());
            map.put("importPrice", v.getImportPrice());
            java.util.Map<String, Object> p = new java.util.HashMap<>();
            p.put("name", v.getProduct() != null ? v.getProduct().getName() : "");
            map.put("product", p);
            java.util.Map<String, Object> c = new java.util.HashMap<>();
            c.put("name", v.getColor() != null ? v.getColor().getName() : "");
            map.put("color", c);
            java.util.Map<String, Object> s = new java.util.HashMap<>();
            s.put("name", v.getSize() != null ? v.getSize().getName() : "");
            map.put("size", s);
            variantData.add(map);
        }

        model.addAttribute("variants", variants);
        model.addAttribute("variantData", variantData);
        model.addAttribute("defaultCode", defaultCode);
        model.addAttribute("prefillSku", prefillSku);
        return "admin/inventory-create";
    }

    @PostMapping("/luu")
    public String saveReceipt(@RequestParam("supplierName") String supplierName,
                              @RequestParam(value = "receiptCode", required = false) String receiptCode,
                              @RequestParam(value = "note", required = false) String note,
                              @RequestParam("variantId[]") Long[] variantIds,
                              @RequestParam("quantity[]") Integer[] quantities,
                              @RequestParam("importPrice[]") BigDecimal[] importPrices,
                              RedirectAttributes redirectAttributes) {
        try {
            InventoryReceiptRequest request = new InventoryReceiptRequest();
            request.setSupplierName(supplierName);
            request.setReceiptCode(receiptCode);
            request.setNote(note);

            List<InventoryReceiptItemRequest> items = new ArrayList<>();
            if (variantIds != null) {
                for (int i = 0; i < variantIds.length; i++) {
                    if (variantIds[i] != null && quantities[i] != null && importPrices[i] != null) {
                        items.add(new InventoryReceiptItemRequest(variantIds[i], quantities[i], importPrices[i]));
                    }
                }
            }
            request.setItems(items);

            User adminUser = userRepository.findById(1L).orElseGet(() -> {
                List<User> all = userRepository.findAll();
                return all.isEmpty() ? null : all.get(0);
            });

            InventoryReceipt saved = inventoryService.createReceipt(request, adminUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Tạo phiếu nhập kho #" + saved.getReceiptCode() + " thành công! Số lượng tồn kho và giá vốn SKU đã được cập nhật.");
            return "redirect:/admin/nhap-kho/" + saved.getId();
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/nhap-kho/tao-moi";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý phiếu nhập: " + ex.getMessage());
            return "redirect:/admin/nhap-kho/tao-moi";
        }
    }

    @GetMapping("/{id}")
    public String viewReceiptDetail(@PathVariable("id") Long id, Model model) {
        InventoryReceipt receipt = inventoryService.getReceiptById(id)
                .orElseThrow(() -> new CustomException("Không tìm thấy phiếu nhập kho ID: " + id));

        model.addAttribute("receipt", receipt);
        return "admin/inventory-detail";
    }
}
