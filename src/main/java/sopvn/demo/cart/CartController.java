package sopvn.demo.cart;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.cart.dto.CartResponse;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.*;
import sopvn.demo.repository.ProductRepository;
import sopvn.demo.repository.ProductVariantRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/gio-hang")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartController(CartService cartService, 
                          UserRepository userRepository,
                          ProductRepository productRepository,
                          ProductVariantRepository productVariantRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    private String getOrCreateGuestToken(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("COOLMATE_GUEST_CART".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        String newToken = "GUEST-" + UUID.randomUUID().toString();
        Cookie cookie = new Cookie("COOLMATE_GUEST_CART", newToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(30 * 24 * 3600);
        response.addCookie(cookie);
        return newToken;
    }

    private User getCurrentUser(Principal principal, HttpSession session) {
        if (principal != null) {
            User u = userRepository.findByEmail(principal.getName()).orElse(null);
            if (u != null) return u;
        }
        if (session != null) {
            Object sUser = session.getAttribute("currentUser");
            if (sUser instanceof User) return (User) sUser;
            Object uid = session.getAttribute("userId");
            if (uid == null) uid = session.getAttribute("USER_ID");
            if (uid != null) {
                try {
                    return userRepository.findById(Long.valueOf(uid.toString())).orElse(null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @GetMapping
    public String viewCart(Principal principal,
                           HttpSession session,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Model model) {
        User user = getCurrentUser(principal, session);
        String guestToken = (user == null) ? getOrCreateGuestToken(request, response) : null;

        CartResponse cart = cartService.getCartSummary(user, guestToken);
        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        return "cart";
    }

    @PostMapping("/them")
    public String addToCart(@RequestParam(value = "variantId", required = false) Long variantId,
                            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                            @RequestParam(value = "buyNow", defaultValue = "false") boolean buyNow,
                            Principal principal,
                            HttpSession session,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        String guestToken = (user == null) ? getOrCreateGuestToken(request, response) : null;

        if (variantId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn màu sắc và kích cỡ (Size) sản phẩm trước khi thêm vào giỏ hàng.");
            return "redirect:/gio-hang";
        }

        if (quantity == null || quantity <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số lượng sản phẩm đặt mua tối thiểu phải từ 1 trở lên.");
            return "redirect:/gio-hang";
        }

        try {
            cartService.addToCart(user, guestToken, variantId, quantity);
            if (buyNow) {
                return "redirect:/thanh-toan/checkout";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng thành công!");
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể thêm sản phẩm do lỗi hệ thống tồn kho. Vui lòng thử lại sau.");
        }

        return "redirect:/gio-hang";
    }

    @PostMapping("/them-nhanh")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickAddToCart(@RequestParam("productId") Long productId,
                                                              @RequestParam(value = "colorId", required = false) Integer colorId,
                                                              @RequestParam("sizeId") Integer sizeId,
                                                              @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                                              Principal principal,
                                                              HttpSession session,
                                                              HttpServletRequest request,
                                                              HttpServletResponse response) {
        Map<String, Object> resp = new HashMap<>();
        User user = getCurrentUser(principal, session);
        String guestToken = (user == null) ? getOrCreateGuestToken(request, response) : null;

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            resp.put("success", false);
            resp.put("message", "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
            return ResponseEntity.ok(resp);
        }

        ProductVariant variant = null;
        if (colorId != null && sizeId != null) {
            variant = productVariantRepository.findByProductIdAndColorIdAndSizeId(productId, colorId, sizeId).orElse(null);
        }

        if (variant == null) {
            List<ProductVariant> list = productVariantRepository.findByProductIdAndIsActiveTrue(productId);
            for (ProductVariant v : list) {
                if (v.getSize() != null && sizeId.equals(v.getSize().getId())) {
                    variant = v;
                    break;
                }
            }
        }

        if (variant == null) {
            resp.put("success", false);
            resp.put("message", "Phiên bản màu và kích cỡ này hiện chưa có sẵn.");
            return ResponseEntity.ok(resp);
        }

        if (variant.getStockQuantity() <= 0) {
            resp.put("success", false);
            resp.put("message", "Sản phẩm phiên bản này hiện đã tạm hết hàng trong kho.");
            return ResponseEntity.ok(resp);
        }

        try {
            cartService.addToCart(user, guestToken, variant.getId(), quantity);
            Cart cart = cartService.getOrCreateCart(user, guestToken);
            int count = (cart.getItems() != null) ? cart.getItems().size() : 0;

            String colorName = variant.getColor() != null ? variant.getColor().getName() : "";
            String sizeName = variant.getSize() != null ? variant.getSize().getName() : "";

            resp.put("success", true);
            resp.put("authenticated", user != null);
            resp.put("message", "Đã thêm '" + product.getName() + "' (Màu: " + colorName + ", Size: " + sizeName + ") vào giỏ hàng thành công!");
            resp.put("cartItemCount", count);
            resp.put("variantSku", variant.getSku());
            return ResponseEntity.ok(resp);
        } catch (CustomException ex) {
            resp.put("success", false);
            resp.put("message", ex.getMessage());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            resp.put("success", false);
            resp.put("message", "Lỗi khi thêm sản phẩm vào giỏ hàng. Vui lòng thử lại.");
            return ResponseEntity.ok(resp);
        }
    }

    @PostMapping("/cap-nhat")
    public String updateQuantity(@RequestParam(value = "cartItemId", required = false) Long cartItemId,
                                 @RequestParam(value = "itemId", required = false) Long itemId,
                                 @RequestParam("quantity") Integer quantity,
                                 Principal principal,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        String guestToken = (user == null) ? getOrCreateGuestToken(request, response) : null;

        Long targetId = cartItemId != null ? cartItemId : itemId;
        if (targetId == null) {
            return "redirect:/gio-hang";
        }

        try {
            // LỖI 1 FIX: Gọi đúng contract updateQuantity(User, String, Long, int)
            cartService.updateQuantity(user, guestToken, targetId, quantity != null ? quantity : 1);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật số lượng thành công.");
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật số lượng.");
        }

        return "redirect:/gio-hang";
    }

    @PostMapping("/xoa/{cartItemId}")
    public String removeItem(@PathVariable("cartItemId") Long cartItemId,
                             Principal principal,
                             HttpSession session,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        String guestToken = (user == null) ? getOrCreateGuestToken(request, response) : null;

        try {
            // LỖI 2 FIX: Gọi đúng contract removeItem(User, String, Long)
            cartService.removeItem(user, guestToken, cartItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm.");
        }

        return "redirect:/gio-hang";
    }
}
