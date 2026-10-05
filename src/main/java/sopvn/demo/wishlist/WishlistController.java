package sopvn.demo.wishlist;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sopvn.demo.entity.User;
import sopvn.demo.entity.WishlistItem;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    public WishlistController(WishlistService wishlistService, UserRepository userRepository) {
        this.wishlistService = wishlistService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Principal principal, HttpSession session) {
        if (principal != null) {
            return userRepository.findByEmail(principal.getName()).orElse(null);
        }
        if (session != null) {
            Object sUser = session.getAttribute("currentUser");
            if (sUser instanceof User) return (User) sUser;
            Object uid = session.getAttribute("userId");
            if (uid != null) {
                try {
                    return userRepository.findById(Long.valueOf(uid.toString())).orElse(null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @GetMapping("/wishlist")
    public String viewWishlist(Principal principal, HttpSession session, Model model) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=wishlist";
        }

        List<WishlistItem> items = wishlistService.getUserWishlistItems(user);
        model.addAttribute("items", items);
        model.addAttribute("user", user);
        return "client/wishlist";
    }

    @PostMapping("/api/wishlist/toggle/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(@PathVariable("productId") Long productId,
                                                              Principal principal,
                                                              HttpSession session) {
        User user = getAuthenticatedUser(principal, session);
        Map<String, Object> resp = new HashMap<>();

        if (user == null) {
            resp.put("authenticated", false);
            resp.put("message", "Vui lòng đăng nhập để lưu sản phẩm yêu thích.");
            return ResponseEntity.status(401).body(resp);
        }

        boolean inWishlist = wishlistService.toggleWishlist(user, productId);
        resp.put("authenticated", true);
        resp.put("inWishlist", inWishlist);
        resp.put("message", inWishlist ? "Đã thêm vào danh sách yêu thích!" : "Đã bỏ khỏi danh sách yêu thích.");
        return ResponseEntity.ok(resp);
    }
}