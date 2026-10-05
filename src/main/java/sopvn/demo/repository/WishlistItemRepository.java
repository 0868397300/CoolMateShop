package sopvn.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sopvn.demo.entity.WishlistItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByWishlistIdOrderByCreatedAtDesc(Long wishlistId);
    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);
    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);
    void deleteByWishlistIdAndProductId(Long wishlistId, Long productId);
    void deleteByWishlistId(Long wishlistId);
}
