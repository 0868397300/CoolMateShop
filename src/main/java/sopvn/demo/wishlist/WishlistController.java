package sopvn.demo.wishlist;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sopvn.demo.entity.Product;
import sopvn.demo.repository.ProductRepository;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final ProductRepository productRepository;

    public WishlistController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<Map<String, Object>> toggleWishlist(@PathVariable("productId") Long productId,
                                                              Principal principal,
                                                              HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        boolean isLoggedIn = (principal != null) || (session != null && (session.getAttribute("currentUser") != null || session.getAttribute("userId") != null));
        if (!isLoggedIn) {
            response.put("success", false);
            response.put("authenticated", false);
            response.put("message", "Bạn cần đăng nhập tài khoản CoolClub để lưu sản phẩm vào danh sách yêu thích.");
            response.put("redirectUrl", "/auth/login?required=wishlist");
            return ResponseEntity.ok(response);
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            response.put("success", false);
            response.put("authenticated", true);
            response.put("message", "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
            return ResponseEntity.ok(response);
        }

        @SuppressWarnings("unchecked")
        Set<Long> wishlist = (Set<Long>) session.getAttribute("USER_WISHLIST");
        if (wishlist == null) {
            wishlist = new HashSet<>();
        }

        boolean isLiked;
        if (wishlist.contains(productId)) {
            wishlist.remove(productId);
            isLiked = false;
            response.put("message", "Đã xóa sản phẩm '" + product.getName() + "' khỏi danh sách yêu thích.");
        } else {
            wishlist.add(productId);
            isLiked = true;
            response.put("message", "Đã lưu sản phẩm '" + product.getName() + "' vào danh sách yêu thích của bạn!");
        }

        session.setAttribute("USER_WISHLIST", wishlist);
        response.put("success", true);
        response.put("authenticated", true);
        response.put("liked", isLiked);
        response.put("count", wishlist.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ids")
    public ResponseEntity<Set<Long>> getWishlistIds(HttpSession session) {
        @SuppressWarnings("unchecked")
        Set<Long> wishlist = (Set<Long>) session.getAttribute("USER_WISHLIST");
        return ResponseEntity.ok(wishlist != null ? wishlist : Collections.emptySet());
    }
}
