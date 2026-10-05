package sopvn.demo.cart;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.cart.dto.CartResponse;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.Cart;
import sopvn.demo.entity.Product;
import sopvn.demo.entity.ProductVariant;
import sopvn.demo.entity.User;
import sopvn.demo.repository.ProductRepository;
import sopvn.demo.repository.ProductVariantRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public String viewCart(Principal principal, HttpSession session, Model model) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=cart";
        }
        CartResponse cart = cartService.getCartSummary(user, null);
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
                            RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập tài khoản CoolClub để " + (buyNow ? "tiến hành mua hàng." : "thêm sản phẩm vào giỏ hàng."));
            return "redirect:/auth/login?required=" + (buyNow ? "checkout" : "cart");
        }

        if (variantId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn màu sắc và kích cỡ (Size) sản phẩm trước khi thêm vào giỏ hàng.");
            return "redirect:/gio-hang";
        }

        if (quantity == null || quantity <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số lượng sản phẩm đặt mua tối thiểu phải từ 1 trở lên.");
            return "redirect:/gio-hang";
        }

        try {
            cartService.addToCart(user, null, variantId, quantity);
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

    /**
     * API Thêm nhanh vào giỏ hàng từ Trang chủ hoặc Danh mục sản phẩm (AJAX).
     * Khi khách chọn Màu và click vào một trong các Size dọc bên trái ảnh.
     */
    @PostMapping("/them-nhanh")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickAddToCart(@RequestParam("productId") Long productId,
                                                              @RequestParam(value = "colorId", required = false) Integer colorId,
                                                              @RequestParam("sizeId") Integer sizeId,
                                                              @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                                              Principal principal,
                                                              HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        User user = getCurrentUser(principal, session);
        if (user == null) {
            resp.put("success", false);
            resp.put("authenticated", false);
            resp.put("message", "Bạn cần đăng nhập tài khoản CoolClub để thêm sản phẩm vào giỏ hàng.");
            resp.put("redirectUrl", "/auth/login?required=cart");
            return ResponseEntity.ok(resp);
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            resp.put("success", false);
            resp.put("authenticated", true);
            resp.put("message", "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
            return ResponseEntity.ok(resp);
        }

        // Tìm variant phù hợp
        ProductVariant variant = null;
        if (colorId != null && sizeId != null) {
            variant = productVariantRepository.findByProductIdAndColorIdAndSizeId(productId, colorId, sizeId).orElse(null);
        }

        // Nếu không có màu cụ thể, lấy biến thể active đầu tiên theo size
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
            resp.put("authenticated", true);
            resp.put("message", "Phiên bản màu và kích cỡ này hiện chưa có sẵn.");
            return ResponseEntity.ok(resp);
        }

        if (variant.getStockQuantity() <= 0) {
            resp.put("success", false);
            resp.put("authenticated", true);
            resp.put("message", "Sản phẩm phiên bản này hiện đã tạm hết hàng trong kho.");
            return ResponseEntity.ok(resp);
        }

        try {
            cartService.addToCart(user, null, variant.getId(), quantity);
            Cart cart = cartService.getOrCreateCart(user, null);
            int count = (cart.getItems() != null) ? cart.getItems().size() : 0;

            String colorName = variant.getColor() != null ? variant.getColor().getName() : "";
            String sizeName = variant.getSize() != null ? variant.getSize().getName() : "";

            resp.put("success", true);
            resp.put("authenticated", true);
            resp.put("message", "Đã thêm '" + product.getName() + "' (Màu: " + colorName + ", Size: " + sizeName + ") vào giỏ hàng thành công!");
            resp.put("cartItemCount", count);
            resp.put("variantSku", variant.getSku());
            return ResponseEntity.ok(resp);
        } catch (CustomException ex) {
            resp.put("success", false);
            resp.put("authenticated", true);
            resp.put("message", ex.getMessage());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            resp.put("success", false);
            resp.put("authenticated", true);
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
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=cart";
        }

        Long targetId = cartItemId != null ? cartItemId : itemId;
        if (targetId == null) {
            return "redirect:/gio-hang";
        }

        try {
            cartService.updateQuantity(targetId, quantity != null ? quantity : 1);
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
                             RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=cart";
        }

        try {
            cartService.removeItem(cartItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm.");
        }

        return "redirect:/gio-hang";
    }
}
