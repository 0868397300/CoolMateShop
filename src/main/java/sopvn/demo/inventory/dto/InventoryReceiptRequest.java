package sopvn.demo.inventory.dto;

import java.util.ArrayList;
import java.util.List;

public class InventoryReceiptRequest {
    private String receiptCode;
    private String supplierName;
    private String note;
    private List<InventoryReceiptItemRequest> items = new ArrayList<>();

    public InventoryReceiptRequest() {
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<InventoryReceiptItemRequest> getItems() {
        return items;
    }

    public void setItems(List<InventoryReceiptItemRequest> items) {
        this.items = items;
    }
}
