package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findTop8ByStatusOrderBySoldCountDesc(String status);

    Optional<Product> findBySlug(String slug);

    List<Product> findByCategoryIdAndStatus(Integer categoryId, String status);

    List<Product> findByCollectionIdAndStatus(Integer collectionId, String status);

    List<Product> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN p.variants v " +
           "WHERE p.status = 'ACTIVE' " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId OR p.category.parent.id = :categoryId) " +
           "AND (:collectionId IS NULL OR p.collection.id = :collectionId) " +
           "AND (:colorId IS NULL OR v.color.id = :colorId) " +
           "AND (:sizeId IS NULL OR v.size.id = :sizeId) " +
           "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice) " +
           "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.material) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> filterProducts(@Param("categoryId") Integer categoryId,
                                 @Param("collectionId") Integer collectionId,
                                 @Param("colorId") Integer colorId,
                                 @Param("sizeId") Integer sizeId,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("keyword") String keyword);
}
