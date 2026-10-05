package sopvn.demo.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import sopvn.demo.cart.CartService;
import sopvn.demo.entity.Cart;
import sopvn.demo.entity.User;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;

@ControllerAdvice
@Component
public class GlobalModelAttributes {

    private final CartService cartService;
    private final UserRepository userRepository;

    public GlobalModelAttributes(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    /**
     * Thông tin User đang đăng nhập để hiển thị trên Header, Navbar và CoolClub VIP banner.
     * Hỗ trợ tìm kiếm từ cả Spring Security Principal lẫn HttpSession để không bao giờ bị mất trạng thái.
     */
    @ModelAttribute("currentUser")
    public User getCurrentUser(Principal principal, HttpSession session) {
        User user = null;
        if (principal != null) {
            user = userRepository.findByEmail(principal.getName()).orElse(null);
        }
        if (user == null && session != null) {
            Object sUser = session.getAttribute("currentUser");
            if (sUser instanceof User) {
                user = (User) sUser;
            } else {
                Object uid = session.getAttribute("userId");
                if (uid == null) uid = session.getAttribute("USER_ID");
                if (uid != null) {
                    try {
                        Long id = Long.valueOf(uid.toString());
                        user = userRepository.findById(id).orElse(null);
                    } catch (Exception ignored) {}
                }
            }
        }
        if (user != null && session != null) {
            session.setAttribute("currentUser", user);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("USER_ID", user.getId());
        }
        return user;
    }

    /**
     * ID của người dùng đang đăng nhập.
     */
    @ModelAttribute("userId")
    public Long getUserId(Principal principal, HttpSession session) {
        User u = getCurrentUser(principal, session);
        return u != null ? u.getId() : null;
    }

    /**
     * Số lượng sản phẩm khác nhau (theo biến thể Color + Size) trong giỏ hàng.
     * Chưa đăng nhập: trả về null (giao diện không hiển thị số).
     * Đã đăng nhập: trả về số dòng sản phẩm (nếu rỗng trả về 0).
     */
    @ModelAttribute("cartItemCount")
    public Integer getCartItemCount(Principal principal, HttpSession session) {
        User user = getCurrentUser(principal, session);
        if (user == null) {
            return null;
        }
        Cart cart = cartService.getOrCreateCart(user, null);
        return (cart.getItems() != null) ? cart.getItems().size() : 0;
    }
}
