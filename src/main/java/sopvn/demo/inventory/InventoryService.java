package sopvn.demo.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.*;
import sopvn.demo.inventory.dto.InventoryReceiptItemRequest;
import sopvn.demo.inventory.dto.InventoryReceiptRequest;
import sopvn.demo.inventory.dto.InventoryStatsDTO;
import sopvn.demo.repository.InventoryReceiptRepository;
import sopvn.demo.repository.ProductVariantRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private final InventoryReceiptRepository receiptRepository;
    private final ProductVariantRepository variantRepository;

    public InventoryService(InventoryReceiptRepository receiptRepository,
                            ProductVariantRepository variantRepository) {
        this.receiptRepository = receiptRepository;
        this.variantRepository = variantRepository;
    }

    public List<InventoryReceipt> getAllReceipts() {
        return receiptRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<InventoryReceipt> getReceiptById(Long id) {
        return receiptRepository.findById(id);
    }

    @Transactional
    public InventoryReceipt createReceipt(InventoryReceiptRequest request, User currentUser) {
        if (request.getSupplierName() == null || request.getSupplierName().trim().isEmpty()) {
            throw new CustomException("Vui lòng nhập tên nhà cung cấp hoặc xưởng may!");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new CustomException("Phiếu nhập kho phải có ít nhất 1 sản phẩm!");
        }

        String receiptCode = request.getReceiptCode();
        if (receiptCode == null || receiptCode.trim().isEmpty()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            receiptCode = "PNK" + LocalDateTime.now().format(dtf);
        } else {
            receiptCode = receiptCode.trim().toUpperCase();
            if (receiptRepository.findByReceiptCode(receiptCode).isPresent()) {
                throw new CustomException("Mã phiếu nhập " + receiptCode + " đã tồn tại trong hệ thống!");
            }
        }

        InventoryReceipt receipt = new InventoryReceipt();
        receipt.setReceiptCode(receiptCode);
        receipt.setSupplierName(request.getSupplierName().trim());
        receipt.setCreatedBy(currentUser);
        receipt.setNote(request.getNote());
        receipt.setStatus("COMPLETED");
        receipt.setCreatedAt(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (InventoryReceiptItemRequest itemReq : request.getItems()) {
            if (itemReq.getVariantId() == null) continue;
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new CustomException("Số lượng nhập phải lớn hơn 0!");
            }
            if (itemReq.getImportPrice() == null || itemReq.getImportPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new CustomException("Đơn giá nhập không được âm!");
            }

            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new CustomException("Không tìm thấy biến thể sản phẩm ID: " + itemReq.getVariantId()));

            BigDecimal itemTotal = itemReq.getImportPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            InventoryReceiptItem receiptItem = new InventoryReceiptItem();
            receiptItem.setReceipt(receipt);
            receiptItem.setVariant(variant);
            receiptItem.setQuantity(itemReq.getQuantity());
            receiptItem.setImportPrice(itemReq.getImportPrice());
            receiptItem.setTotalPrice(itemTotal);

            receipt.getItems().add(receiptItem);

            // Cập nhật tăng số lượng tồn kho và cập nhật giá vốn mới nhất vào SKU
            variant.setStockQuantity(variant.getStockQuantity() + itemReq.getQuantity());
            variant.setImportPrice(itemReq.getImportPrice());
            variantRepository.save(variant);
        }

        receipt.setTotalAmount(totalAmount);
        return receiptRepository.save(receipt);
    }

    public InventoryStatsDTO getInventoryStats() {
        InventoryStatsDTO stats = new InventoryStatsDTO();
        List<InventoryReceipt> allReceipts = receiptRepository.findAll();
        stats.setTotalReceipts(allReceipts.size());

        BigDecimal spentMonth = BigDecimal.ZERO;
        long totalUnits = 0;
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        for (InventoryReceipt r : allReceipts) {
            if (r.getCreatedAt() != null && r.getCreatedAt().isAfter(startOfMonth)) {
                spentMonth = spentMonth.add(r.getTotalAmount());
            }
            if (r.getItems() != null) {
                for (InventoryReceiptItem it : r.getItems()) {
                    totalUnits += it.getQuantity();
                }
            }
        }
        stats.setTotalSpentThisMonth(spentMonth);
        stats.setTotalUnitsImported(totalUnits);

        // Tính tổng giá trị hàng tồn kho hiện tại: Σ (tồn kho * giá vốn)
        BigDecimal inventoryValue = BigDecimal.ZERO;
        List<ProductVariant> allVariants = variantRepository.findByIsActiveTrue();
        for (ProductVariant v : allVariants) {
            BigDecimal cost = v.getImportPrice() != null ? v.getImportPrice() : BigDecimal.ZERO;
            int stock = v.getStockQuantity() != null ? v.getStockQuantity() : 0;
            inventoryValue = inventoryValue.add(cost.multiply(BigDecimal.valueOf(stock)));
        }
        stats.setTotalInventoryValue(inventoryValue);

        // Cảnh báo tồn kho thấp (<= 40 sản phẩm)
        List<ProductVariant> lowStock = variantRepository.findByStockQuantityLessThanEqualAndIsActiveTrueOrderByStockQuantityAsc(40);
        stats.setLowStockVariants(lowStock);

        return stats;
    }
}
