package sopvn.demo.payment;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.cart.CartService;
import sopvn.demo.cart.PricingService;
import sopvn.demo.cart.dto.CartResponse;
import sopvn.demo.cart.dto.PricingSummaryDTO;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.*;
import sopvn.demo.order.OrderService;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.UserAddressRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/thanh-toan")
public class PaymentController {

    private final VnpayService vnpayService;
    private final VnpayPaymentProcessor paymentProcessor;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final PricingService pricingService;

    public PaymentController(VnpayService vnpayService,
                             VnpayPaymentProcessor paymentProcessor,
                             CartService cartService,
                             OrderService orderService,
                             OrderRepository orderRepository,
                             UserRepository userRepository,
                             UserAddressRepository userAddressRepository,
                             PricingService pricingService) {
        this.vnpayService = vnpayService;
        this.paymentProcessor = paymentProcessor;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
        this.pricingService = pricingService;
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

    @GetMapping("/checkout")
    public String checkoutPage(@RequestParam(value = "voucherCode", required = false) String voucherCode,
                               @RequestParam(value = "useCoolCash", defaultValue = "false") boolean useCoolCash,
                               Principal principal,
                               HttpSession session,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               Model model) {
        User user = getAuthenticatedUser(principal, session);
        String guestToken = (user == null) ? getOrCreateGuestToken(request, response) : null;

        CartResponse cart = cartService.getCartSummary(user, guestToken);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/gio-hang";
        }

        Cart actualCart = cartService.getOrCreateCart(user, guestToken);
        List<CartItem> cartItems = (actualCart != null && actualCart.getItems() != null) ? actualCart.getItems() : Collections.emptyList();

        BigDecimal requestedCoolCash = (useCoolCash && user != null) ? user.getCoolcashBalance() : BigDecimal.ZERO;
        PricingSummaryDTO pricing = pricingService.calculatePricing(user, cartItems, voucherCode, requestedCoolCash, null, null, "COD");

        List<UserAddress> addresses = (user != null) ? userAddressRepository.findByUserIdOrderByIsDefaultDesc(user.getId()) : Collections.emptyList();

        model.addAttribute("cart", cart);
        model.addAttribute("pricing", pricing);
        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        model.addAttribute("voucherCode", voucherCode);
        model.addAttribute("useCoolCash", useCoolCash);

        return "checkout";
    }

    @PostMapping("/dat-hang")
    public String placeOrder(@RequestParam("recipientName") String recipientName,
                             @RequestParam("recipientPhone") String recipientPhone,
                             @RequestParam(value = "recipientEmail", required = false) String recipientEmail,
                             @RequestParam("shippingAddress") String shippingAddress,
                             @RequestParam(value = "note", required = false) String note,
                             @RequestParam(value = "paymentMethod", defaultValue = "COD") String paymentMethod,
                             @RequestParam(value = "voucherCode", required = false) String voucherCode,
                             @RequestParam(value = "useCoolCash", defaultValue = "false") boolean useCoolCash,
                             Principal principal,
                             HttpSession session,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal, session);
        String guestToken = (user == null) ? getOrCreateGuestToken(request, response) : null;

        Cart cart = cartService.getOrCreateCart(user, guestToken);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
            return "redirect:/gio-hang";
        }

        try {
            Order order = orderService.createOrder(user, recipientName, recipientPhone, recipientEmail,
                    shippingAddress, note, paymentMethod, voucherCode, useCoolCash, cart.getItems());

            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                String clientIp = vnpayService.getClientIp(request);
                String paymentUrl = vnpayService.createPaymentUrl(order, clientIp);
                return "redirect:" + paymentUrl;
            } else {
                // COD: xóa giỏ hàng ngay khi đặt thành công
                cartService.clearCart(user, guestToken);
                redirectAttributes.addFlashAttribute("order", order);
                return "redirect:/thanh-toan/thanh-cong?orderCode=" + order.getOrderCode();
            }
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/thanh-toan/checkout";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo đơn hàng: " + ex.getMessage());
            return "redirect:/thanh-toan/checkout";
        }
    }

    @GetMapping("/thanh-cong")
    public String orderSuccessPage(@RequestParam("orderCode") String orderCode, Model model) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        model.addAttribute("order", order);
        return "order-success";
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request,
                              HttpServletResponse response,
                              Model model) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        String guestToken = getOrCreateGuestToken(request, response);
        VnpayPaymentProcessor.PaymentResult result = paymentProcessor.processVnpayResult(fields, guestToken);

        model.addAttribute("success", result.isSuccess());
        model.addAttribute("orderCode", result.getOrder() != null ? result.getOrder().getOrderCode() : paramsOrEmpty(fields, "vnp_TxnRef"));
        model.addAttribute("transactionNo", result.getTransactionNo());
        model.addAttribute("amount", result.getAmount());
        model.addAttribute("message", result.getMessage());

        return "client/payment-result";
    }

    @RequestMapping(value = "/vnpay-ipn", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<Map<String, String>> vnpayIpn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        VnpayPaymentProcessor.PaymentResult result = paymentProcessor.processVnpayResult(fields, null);

        Map<String, String> response = new HashMap<>();
        response.put("RspCode", result.getCode());
        response.put("Message", result.getMessage());
        return ResponseEntity.ok(response);
    }

    private String paramsOrEmpty(Map<String, String> map, String key) {
        return map.containsKey(key) ? map.get(key) : "";
    }
}
