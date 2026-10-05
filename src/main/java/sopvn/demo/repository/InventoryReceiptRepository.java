package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.InventoryReceipt;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReceiptRepository extends JpaRepository<InventoryReceipt, Long> {
    Optional<InventoryReceipt> findByReceiptCode(String receiptCode);
    List<InventoryReceipt> findAllByOrderByCreatedAtDesc();
}
