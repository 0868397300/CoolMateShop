package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.ProductVariant;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Optional<ProductVariant> findByProductIdAndColorIdAndSizeId(Long productId, Integer colorId, Integer sizeId);

    List<ProductVariant> findByStockQuantityLessThanEqualAndIsActiveTrueOrderByStockQuantityAsc(Integer threshold);

    List<ProductVariant> findByIsActiveTrue();
}
