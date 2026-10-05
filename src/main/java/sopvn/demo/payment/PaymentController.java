package sopvn.demo.payment;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.cart.CartService;
import sopvn.demo.cart.dto.CartResponse;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.Cart;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.User;
import sopvn.demo.entity.UserAddress;
import sopvn.demo.order.OrderService;
import sopvn.demo.repository.UserAddressRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/**
 * Controller triển khai tính năng CL-06: Checkout, VNPAY & Đặt hàng với hỗ trợ địa chỉ & tính phí ship
 */
@Controller
@RequestMapping("/thanh-toan")
public class PaymentController {

    private final VnpayService vnpayService;
    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;

    public PaymentController(VnpayService vnpayService,
                             CartService cartService,
                             OrderService orderService,
                             UserRepository userRepository,
                             UserAddressRepository userAddressRepository) {
        this.vnpayService = vnpayService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    @GetMapping("/checkout")
    public String checkoutPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login?required=checkout";
        }
        User user = getCurrentUser(principal);
        if (user == null) {
            return "redirect:/auth/login?required=checkout";
        }

        CartResponse cart = cartService.getCartSummary(user, null);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/gio-hang";
        }

        List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIsDefaultDesc(user.getId());

        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        return "client/checkout";
    }

    @PostMapping("/dat-hang")
    public String placeOrder(@RequestParam(value = "selectedAddressId", required = false) Long selectedAddressId,
                             @RequestParam(value = "recipientName", required = false) String recipientName,
                             @RequestParam(value = "recipientPhone", required = false) String recipientPhone,
                             @RequestParam(value = "recipientEmail", required = false) String recipientEmail,
                             @RequestParam(value = "provinceName", required = false) String provinceName,
                             @RequestParam(value = "districtName", required = false) String districtName,
                             @RequestParam(value = "wardName", required = false) String wardName,
                             @RequestParam(value = "specificAddress", required = false) String specificAddress,
                             @RequestParam(value = "shippingAddress", required = false) String directAddress,
                             @RequestParam(value = "saveAddress", defaultValue = "false") boolean saveAddress,
                             @RequestParam(value = "isDefaultAddress", defaultValue = "false") boolean isDefaultAddress,
                             @RequestParam(value = "note", required = false) String note,
                             @RequestParam("paymentMethod") String paymentMethod,
                             Principal principal,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập tài khoản CoolClub để tiến hành đặt hàng.");
            return "redirect:/auth/login?required=checkout";
        }

        User user = getCurrentUser(principal);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên đăng nhập của bạn đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.");
            return "redirect:/auth/login?required=checkout";
        }

        Cart cart = cartService.getOrCreateCart(user, null);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống! Vui lòng chọn ít nhất 1 sản phẩm trước khi đặt hàng.");
            return "redirect:/gio-hang";
        }

        String finalShippingAddress = "";
        String finalName = recipientName;
        String finalPhone = recipientPhone;

        // 1. Trường hợp chọn địa chỉ có sẵn
        if (selectedAddressId != null && selectedAddressId > 0) {
            UserAddress savedAddr = userAddressRepository.findById(selectedAddressId).orElse(null);
            if (savedAddr != null && savedAddr.getUser().getId().equals(user.getId())) {
                finalName = savedAddr.getRecipientName();
                finalPhone = savedAddr.getRecipientPhone();
                provinceName = savedAddr.getProvinceName();
                districtName = savedAddr.getDistrictName();
                wardName = savedAddr.getWardName();
                specificAddress = savedAddr.getStreetAddress();
                finalShippingAddress = specificAddress + ", " + wardName + ", " + districtName + ", " + provinceName;
            }
        }

        // 2. Trường hợp nhập địa chỉ mới theo các cấp hành chính
        if (finalShippingAddress.isEmpty()) {
            if (finalName == null || finalName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ họ và tên người nhận hàng.");
                return "redirect:/thanh-toan/checkout";
            }
            if (finalPhone == null || !finalPhone.trim().matches("^(0[3|5|7|8|9])+([0-9]{8})$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại di động gồm 10 chữ số.");
                return "redirect:/thanh-toan/checkout";
            }
            if (provinceName == null || provinceName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn Tỉnh / Thành phố nhận hàng.");
                return "redirect:/thanh-toan/checkout";
            }
            if (districtName == null || districtName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn Quận / Huyện nhận hàng.");
                return "redirect:/thanh-toan/checkout";
            }
            if (wardName == null || wardName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn Phường / Xã nhận hàng.");
                return "redirect:/thanh-toan/checkout";
            }
            if (specificAddress == null || specificAddress.trim().length() < 3) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường, tòa nhà...).");
                return "redirect:/thanh-toan/checkout";
            }

            finalShippingAddress = specificAddress.trim() + ", " + wardName.trim() + ", " + districtName.trim() + ", " + provinceName.trim();

            // Nếu người dùng chọn lưu địa chỉ
            if (saveAddress) {
                if (isDefaultAddress) {
                    List<UserAddress> oldAddrs = userAddressRepository.findByUserIdOrderByIsDefaultDesc(user.getId());
                    for (UserAddress a : oldAddrs) {
                        a.setIsDefault(false);
                        userAddressRepository.save(a);
                    }
                }
                UserAddress newAddr = new UserAddress();
                newAddr.setUser(user);
                newAddr.setRecipientName(finalName.trim());
                newAddr.setRecipientPhone(finalPhone.trim());
                newAddr.setProvinceName(provinceName.trim());
                newAddr.setDistrictName(districtName.trim());
                newAddr.setWardName(wardName.trim());
                newAddr.setStreetAddress(specificAddress.trim());
                newAddr.setIsDefault(isDefaultAddress);
                userAddressRepository.save(newAddr);
            }
        }

        // Tính phí vận chuyển theo thuật toán
        CartResponse cartSummary = cartService.getCartSummary(user, null);
        BigDecimal subtotal = cartSummary.getSubtotal();
        BigDecimal shippingFee = BigDecimal.ZERO;

        if (subtotal.compareTo(BigDecimal.valueOf(200000)) < 0) {
            String pLower = provinceName != null ? provinceName.toLowerCase() : "";
            String dLower = districtName != null ? districtName.toLowerCase() : "";
            boolean isHanoiOrHcm = pLower.contains("hà nội") || pLower.contains("hồ chí minh") || pLower.contains("hcm");
            if (isHanoiOrHcm) {
                boolean isInner = dLower.contains("quận 1") || dLower.contains("quận 3") || dLower.contains("quận 4")
                        || dLower.contains("quận 5") || dLower.contains("quận 10") || dLower.contains("tân bình")
                        || dLower.contains("phú nhuận") || dLower.contains("hoàn kiếm") || dLower.contains("ba đình")
                        || dLower.contains("đống đa") || dLower.contains("cầu giấy");
                shippingFee = isInner ? BigDecimal.valueOf(20000) : BigDecimal.valueOf(25000);
            } else {
                shippingFee = BigDecimal.valueOf(30000);
            }
        }

        try {
            Order order = orderService.createOrder(user, finalName.trim(), finalPhone.trim(),
                    recipientEmail != null ? recipientEmail.trim() : user.getEmail(),
                    finalShippingAddress, note, cart.getItems(), paymentMethod, shippingFee);

            // Xóa các item trong giỏ sau khi đặt thành công
            cartService.clearCart(user, null);

            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                long amount = order.getFinalAmount().longValue();
                String paymentUrl = vnpayService.createPaymentUrl(amount, "Thanh toan don hang " + order.getOrderCode(), null, request);
                return "redirect:" + paymentUrl;
            }

            return "redirect:/thanh-toan/thanh-cong?orderCode=" + order.getOrderCode();
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/thanh-toan/checkout";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng chưa thể hoàn tất do có sản phẩm vừa hết hàng hoặc thay đổi tồn kho. Vui lòng kiểm tra lại giỏ hàng.");
            return "redirect:/thanh-toan/checkout";
        }
    }

    @GetMapping("/thanh-cong")
    public String orderSuccess(@RequestParam(value = "orderCode", required = false) String orderCode, Model model) {
        model.addAttribute("orderCode", orderCode);
        return "client/order-success";
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam(value = "vnp_ResponseCode", required = false) String responseCode,
                              @RequestParam(value = "vnp_TransactionNo", required = false) String transactionNo,
                              @RequestParam(value = "vnp_OrderInfo", required = false) String orderInfo,
                              @RequestParam(value = "vnp_Amount", required = false) String amount,
                              Model model) {
        boolean success = "00".equals(responseCode);
        model.addAttribute("success", success);
        model.addAttribute("transactionNo", transactionNo);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("amount", amount != null ? Long.parseLong(amount) / 100 : 0);
        return "client/payment-result";
    }
}
