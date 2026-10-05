package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.InventoryMovement;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(Long variantId);
    List<InventoryMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    List<InventoryMovement> findAllByOrderByCreatedAtDesc();
}
