package sopvn.demo.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.core.service.NotificationService;
import sopvn.demo.entity.*;
import sopvn.demo.repository.InventoryMovementRepository;
import sopvn.demo.repository.ProductVariantRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final NotificationService notificationService;

    public StockService(ProductVariantRepository productVariantRepository,
                        InventoryMovementRepository inventoryMovementRepository,
                        NotificationService notificationService) {
        this.productVariantRepository = productVariantRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void reserveStock(ProductVariant variant, int quantity, Long orderId) {
        if (quantity <= 0) return;
        ProductVariant v = productVariantRepository.findById(variant.getId())
                .orElseThrow(() -> new CustomException("Biến thể sản phẩm không tồn tại: " + variant.getId()));

        if (v.getStockQuantity() < quantity) {
            throw new CustomException("Sản phẩm '" + v.getSku() + "' chỉ còn " + v.getStockQuantity() + " chiếc trong kho, không đủ số lượng yêu cầu (" + quantity + ").");
        }

        int before = v.getStockQuantity();
        int after = before - quantity;
        v.setStockQuantity(after);
        productVariantRepository.save(v);

        InventoryMovement movement = new InventoryMovement();
        movement.setVariant(v);
        movement.setMovementType("ORDER_RESERVE");
        movement.setQuantity(quantity);
        movement.setBeforeQuantity(before);
        movement.setAfterQuantity(after);
        movement.setReferenceType("ORDER");
        movement.setReferenceId(orderId);
        movement.setNote("Giữ hàng khi tạo đơn hàng #" + orderId);
        inventoryMovementRepository.save(movement);
    }

    @Transactional
    public void reserveStock(List<OrderItem> items, Order order) {
        if (items == null || order == null) return;
        for (OrderItem item : items) {
            if (item.getVariant() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                reserveStock(item.getVariant(), item.getQuantity(), order.getId());
            }
        }
    }

    @Transactional
    public void consumeStock(Order order) {
        if (order == null || order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                InventoryMovement movement = new InventoryMovement();
                movement.setVariant(item.getVariant());
                movement.setMovementType("ORDER_CONSUME");
                movement.setQuantity(item.getQuantity());
                movement.setBeforeQuantity(item.getVariant().getStockQuantity());
                movement.setAfterQuantity(item.getVariant().getStockQuantity());
                movement.setReferenceType("ORDER");
                movement.setReferenceId(order.getId());
                movement.setNote("Xác nhận bán xuất kho đơn #" + order.getOrderCode());
                inventoryMovementRepository.save(movement);
            }
        }
    }

    @Transactional
    public void releaseStock(Order order) {
        if (order == null || order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                ProductVariant v = productVariantRepository.findById(item.getVariant().getId()).orElse(null);
                if (v != null) {
                    int before = v.getStockQuantity();
                    int after = before + item.getQuantity();
                    v.setStockQuantity(after);
                    productVariantRepository.save(v);

                    InventoryMovement movement = new InventoryMovement();
                    movement.setVariant(v);
                    movement.setMovementType("ORDER_RELEASE");
                    movement.setQuantity(item.getQuantity());
                    movement.setBeforeQuantity(before);
                    movement.setAfterQuantity(after);
                    movement.setReferenceType("ORDER");
                    movement.setReferenceId(order.getId());
                    movement.setNote("Hoàn kho khi hủy/thất bại đơn hàng #" + order.getOrderCode());
                    inventoryMovementRepository.save(movement);
                }
            }
        }
    }

    @Transactional
    public void restockFromReturn(ProductVariant variant, int quantity, Long returnId) {
        if (variant == null || quantity <= 0) return;
        ProductVariant v = productVariantRepository.findById(variant.getId()).orElse(null);
        if (v != null) {
            int before = v.getStockQuantity();
            int after = before + quantity;
            v.setStockQuantity(after);
            productVariantRepository.save(v);

            InventoryMovement movement = new InventoryMovement();
            movement.setVariant(v);
            movement.setMovementType("RETURN_RESTOCK");
            movement.setQuantity(quantity);
            movement.setBeforeQuantity(before);
            movement.setAfterQuantity(after);
            movement.setReferenceType("RETURN");
            movement.setReferenceId(returnId);
            movement.setNote("Nhập lại kho từ yêu cầu đổi/trả hàng #" + returnId);
            inventoryMovementRepository.save(movement);
        }
    }

    @Transactional
    public void applyInventoryReceipt(InventoryReceipt receipt, User admin) {
        if (receipt == null || receipt.getItems() == null || receipt.getItems().isEmpty()) {
            throw new CustomException("Phiếu nhập kho không có sản phẩm!");
        }

        if (!"SUBMITTED".equalsIgnoreCase(receipt.getStatus()) && !"PENDING".equalsIgnoreCase(receipt.getStatus())) {
            throw new CustomException("Phiếu nhập kho này đã được xử lý trước đó (trạng thái: " + receipt.getStatus() + ")!");
        }

        receipt.setStatus("APPROVED");
        receipt.setApprovedBy(admin);
        receipt.setApprovedAt(LocalDateTime.now());

        for (InventoryReceiptItem item : receipt.getItems()) {
            ProductVariant v = item.getVariant();
            if (v == null) continue;

            ProductVariant variant = productVariantRepository.findById(v.getId())
                    .orElseThrow(() -> new CustomException("Biến thể ID: " + v.getId() + " không tồn tại!"));

            int oldStock = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
            BigDecimal oldCost = variant.getImportPrice() != null ? variant.getImportPrice() : BigDecimal.ZERO;

            int receivedQty = item.getQuantity() != null ? item.getQuantity() : 0;
            BigDecimal receivedCost = item.getImportPrice() != null ? item.getImportPrice() : BigDecimal.ZERO;

            int newStock = oldStock + receivedQty;

            BigDecimal newAverageCost;
            if (newStock > 0) {
                BigDecimal oldValue = oldCost.multiply(BigDecimal.valueOf(oldStock));
                BigDecimal receivedValue = receivedCost.multiply(BigDecimal.valueOf(receivedQty));
                newAverageCost = oldValue.add(receivedValue).divide(BigDecimal.valueOf(newStock), 2, RoundingMode.HALF_UP);
            } else {
                newAverageCost = receivedCost;
            }

            variant.setStockQuantity(newStock);
            variant.setImportPrice(newAverageCost);
            productVariantRepository.save(variant);

            InventoryMovement movement = new InventoryMovement();
            movement.setVariant(variant);
            movement.setMovementType("IMPORT");
            movement.setQuantity(receivedQty);
            movement.setBeforeQuantity(oldStock);
            movement.setAfterQuantity(newStock);
            movement.setReferenceType("RECEIPT");
            movement.setReferenceId(receipt.getId());
            movement.setCreatedBy(admin);
            movement.setNote("Nhập hàng từ phiếu #" + receipt.getReceiptCode() + " (Giá nhập: " + receivedCost + "đ, Giá vốn mới: " + newAverageCost + "đ)");
            inventoryMovementRepository.save(movement);
        }
    }
}
