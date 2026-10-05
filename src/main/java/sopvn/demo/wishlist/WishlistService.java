package sopvn.demo.wishlist;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.Product;
import sopvn.demo.entity.User;
import sopvn.demo.entity.Wishlist;
import sopvn.demo.entity.WishlistItem;
import sopvn.demo.repository.ProductRepository;
import sopvn.demo.repository.WishlistItemRepository;
import sopvn.demo.repository.WishlistRepository;

import java.util.Collections;
import java.util.List;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           WishlistItemRepository wishlistItemRepository,
                           ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Wishlist getOrCreateWishlist(User user) {
        if (user == null) {
            throw new CustomException("Người dùng không hợp lệ!");
        }
        return wishlistRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wishlist w = new Wishlist();
                    w.setUser(user);
                    return wishlistRepository.save(w);
                });
    }

    @Transactional(readOnly = true)
    public List<WishlistItem> getUserWishlistItems(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        return wishlistRepository.findByUserId(user.getId())
                .map(w -> wishlistItemRepository.findByWishlistIdOrderByCreatedAtDesc(w.getId()))
                .orElse(Collections.emptyList());
    }

    @Transactional
    public boolean toggleWishlist(User user, Long productId) {
        if (user == null) {
            throw new CustomException("Vui lòng đăng nhập để thực hiện thao tác này!");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException("Không tìm thấy sản phẩm ID: " + productId));

        Wishlist wishlist = getOrCreateWishlist(user);

        if (wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getId(), productId);
            return false;
        } else {
            WishlistItem item = new WishlistItem(wishlist, product);
            wishlistItemRepository.save(item);
            return true;
        }
    }

    @Transactional
    public void removeFromWishlist(User user, Long productId) {
        if (user == null) return;
        wishlistRepository.findByUserId(user.getId()).ifPresent(w -> {
            wishlistItemRepository.deleteByWishlistIdAndProductId(w.getId(), productId);
        });
    }

    @Transactional
    public void clearWishlist(User user) {
        if (user == null) return;
        wishlistRepository.findByUserId(user.getId()).ifPresent(w -> {
            wishlistItemRepository.deleteByWishlistId(w.getId());
        });
    }

    @Transactional(readOnly = true)
    public int getWishlistCount(User user) {
        if (user == null) return 0;
        return getUserWishlistItems(user).size();
    }

    @Transactional
    public void mergeGuestWishlist(User user, List<Long> productIds) {
        if (user == null || productIds == null || productIds.isEmpty()) return;
        Wishlist wishlist = getOrCreateWishlist(user);
        for (Long pid : productIds) {
            if (pid != null && !wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), pid)) {
                productRepository.findById(pid).ifPresent(p -> {
                    wishlistItemRepository.save(new WishlistItem(wishlist, p));
                });
            }
        }
    }
}
