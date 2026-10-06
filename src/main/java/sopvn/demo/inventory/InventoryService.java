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
import java.util.ArrayList;
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
        // Trạng thái ban đầu khi tạo phiếu là SUBMITTED, chỉ duyệt (APPROVED) mới tăng tồn kho
        receipt.setStatus("SUBMITTED");
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
            // Không tăng stock hay sửa import price tại đây - StockService.applyInventoryReceipt() chịu trách nhiệm khi Admin duyệt
        }

        receipt.setTotalAmount(totalAmount);
        return receiptRepository.save(receipt);
    }

    public InventoryStatsDTO getInventoryStats() {
        InventoryStatsDTO stats = new InventoryStatsDTO();

        List<InventoryReceipt> allReceipts = receiptRepository.findAll();
        long totalReceipts = allReceipts.size();

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        BigDecimal spentThisMonth = BigDecimal.ZERO;
        long totalUnitsImported = 0;

        for (InventoryReceipt r : allReceipts) {
            if (r.getCreatedAt() != null && !r.getCreatedAt().isBefore(startOfMonth)) {
                if (r.getTotalAmount() != null) {
                    spentThisMonth = spentThisMonth.add(r.getTotalAmount());
                }
            }
            if (r.getItems() != null) {
                for (InventoryReceiptItem item : r.getItems()) {
                    if (item.getQuantity() != null) {
                        totalUnitsImported += item.getQuantity();
                    }
                }
            }
        }

        List<ProductVariant> variants = variantRepository.findAll();
        BigDecimal totalValue = BigDecimal.ZERO;
        List<ProductVariant> lowStockList = new ArrayList<>();

        for (ProductVariant v : variants) {
            int qty = v.getStockQuantity() != null ? v.getStockQuantity() : 0;
            BigDecimal cost = v.getImportPrice() != null ? v.getImportPrice() : BigDecimal.ZERO;
            totalValue = totalValue.add(cost.multiply(BigDecimal.valueOf(qty)));
            if (qty <= 5) {
                lowStockList.add(v);
            }
        }

        // Cập nhật đúng các methods hiện có của InventoryStatsDTO
        stats.setTotalReceipts(totalReceipts);
        stats.setTotalSpentThisMonth(spentThisMonth);
        stats.setTotalUnitsImported(totalUnitsImported);
        stats.setTotalInventoryValue(totalValue);
        stats.setLowStockVariants(lowStockList);

        return stats;
    }
}
