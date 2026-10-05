package sopvn.demo.payment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.cart.CartService;
import sopvn.demo.cart.PricingService;
import sopvn.demo.cart.dto.CartResponse;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.core.service.NotificationService;
import sopvn.demo.entity.*;
import sopvn.demo.inventory.StockService;
import sopvn.demo.order.OrderService;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.UserAddressRepository;
import sopvn.demo.repository.UserRepository;
import sopvn.demo.wallet.CoolCashService;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/thanh-toan")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final VnpayService vnpayService;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final StockService stockService;
    private final CoolCashService coolCashService;
    private final NotificationService notificationService;

    public PaymentController(VnpayService vnpayService,
                             CartService cartService,
                             OrderService orderService,
                             OrderRepository orderRepository,
                             UserRepository userRepository,
                             UserAddressRepository userAddressRepository,
                             StockService stockService,
                             CoolCashService coolCashService,
                             NotificationService notificationService) {
        this.vnpayService = vnpayService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
        this.stockService = stockService;
        this.coolCashService = coolCashService;
        this.notificationService = notificationService;
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
                               @RequestParam(value = "coolcash", required = false) BigDecimal coolcashRequested,
                               @RequestParam(value = "province", required = false) String province,
                               @RequestParam(value = "district", required = false) String district,
                               Principal principal,
                               HttpSession session,
                               Model model) {
        User user = getAuthenticatedUser(principal, session);
        String sessionId = (session != null) ? session.getId() : null;

        CartResponse cart = cartService.getCartSummary(user, sessionId, voucherCode, coolcashRequested, province, district);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/gio-hang";
        }

        List<UserAddress> addresses = (user != null) 
                ? userAddressRepository.findByUserIdOrderByIsDefaultDesc(user.getId()) 
                : Collections.emptyList();

        model.addAttribute("cart", cart);
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("addresses", addresses);
        model.addAttribute("appliedVoucher", voucherCode);
        model.addAttribute("appliedCoolCash", coolcashRequested);

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
                             @RequestParam(value = "saveAddress", defaultValue = "false") boolean saveAddress,
                             @RequestParam(value = "isDefaultAddress", defaultValue = "false") boolean isDefaultAddress,
                             @RequestParam(value = "note", required = false) String note,
                             @RequestParam("paymentMethod") String paymentMethod,
                             @RequestParam(value = "voucherCode", required = false) String voucherCode,
                             @RequestParam(value = "coolcashUsed", required = false) BigDecimal requestedCoolCash,
                             Principal principal,
                             HttpSession session,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {

        User user = getAuthenticatedUser(principal, session);
        String sessionId = (session != null) ? session.getId() : null;

        Cart cart = cartService.getOrCreateCart(user, sessionId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống! Vui lòng chọn sản phẩm trước khi đặt hàng.");
            return "redirect:/gio-hang";
        }

        String finalShippingAddress = "";
        String finalName = recipientName;
        String finalPhone = recipientPhone;

        // 1. Nếu chọn địa chỉ đã lưu
        if (selectedAddressId != null && selectedAddressId > 0 && user != null) {
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

        // 2. Nếu nhập địa chỉ mới
        if (finalShippingAddress.isEmpty()) {
            if (finalName == null || finalName.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập họ và tên người nhận hàng.");
                return "redirect:/thanh-toan/checkout";
            }
            if (finalPhone == null || !finalPhone.trim().matches("^(0[3|5|7|8|9])+([0-9]{8})$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại nhận hàng không hợp lệ (10 chữ số).");
                return "redirect:/thanh-toan/checkout";
            }
            if (provinceName == null || provinceName.trim().isEmpty() ||
                districtName == null || districtName.trim().isEmpty() ||
                wardName == null || wardName.trim().isEmpty() ||
                specificAddress == null || specificAddress.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ địa chỉ nhận hàng.");
                return "redirect:/thanh-toan/checkout";
            }

            finalShippingAddress = specificAddress.trim() + ", " + wardName.trim() + ", " + districtName.trim() + ", " + provinceName.trim();

            if (saveAddress && user != null) {
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

        try {
            Order order = orderService.createOrder(
                    user, finalName.trim(), finalPhone.trim(), recipientEmail, 
                    finalShippingAddress, provinceName, districtName, note,
                    cart.getItems(), paymentMethod, voucherCode, requestedCoolCash
            );

            // B1: Chỉ xóa giỏ hàng ngay khi phương thức là COD. Với VNPAY, chỉ xóa sau khi thanh toán thành công!
            if ("COD".equalsIgnoreCase(paymentMethod)) {
                cartService.clearCart(user, sessionId);
                return "redirect:/thanh-toan/thanh-cong?orderCode=" + order.getOrderCode();
            } else if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                long amount = order.getFinalAmount().longValue();
                String paymentUrl = vnpayService.createPaymentUrl(
                        amount, "Thanh toan don hang " + order.getOrderCode(), 
                        order.getVnpayTxnRef(), null, request
                );
                return "redirect:" + paymentUrl;
            }

            return "redirect:/thanh-toan/thanh-cong?orderCode=" + order.getOrderCode();
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/thanh-toan/checkout";
        } catch (Exception ex) {
            log.error("Lỗi khi tạo đơn hàng", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hoàn tất đặt hàng. Vui lòng kiểm tra lại tồn kho giỏ hàng.");
            return "redirect:/thanh-toan/checkout";
        }
    }

    @GetMapping("/thanh-cong")
    public String orderSuccess(@RequestParam(value = "orderCode", required = false) String orderCode, Model model) {
        model.addAttribute("orderCode", orderCode);
        return "client/order-success";
    }

    /**
     * B1: Xử lý VNPAY Return URL (Khách hàng từ VNPAY quay lại trình duyệt)
     * Xác thực chữ ký, số tiền, trạng thái đơn hàng và xử lý thanh toán Idempotent
     */
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request,
                              Principal principal,
                              HttpSession session,
                              Model model) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // 1. Xác thực chữ ký bí mật
        boolean signVerified = vnpayService.validateSignature(fields, vnp_SecureHash);
        if (!signVerified) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Chữ ký bảo mật VNPAY không hợp lệ! Giao dịch có thể đã bị can thiệp.");
            return "client/payment-result";
        }

        String txnRef = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        String transactionNo = request.getParameter("vnp_TransactionNo");
        String bankCode = request.getParameter("vnp_BankCode");
        String amountStr = request.getParameter("vnp_Amount");

        Order order = orderRepository.findByVnpayTxnRef(txnRef).orElse(null);
        if (order == null) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Không tìm thấy đơn hàng tương ứng với giao dịch VNPAY #" + txnRef);
            return "client/payment-result";
        }

        // 2. Xác thực số tiền thanh toán
        long receivedAmount = amountStr != null ? Long.parseLong(amountStr) / 100 : 0;
        if (order.getFinalAmount() == null || receivedAmount != order.getFinalAmount().longValue()) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Số tiền thanh toán không khớp với giá trị đơn hàng!");
            return "client/payment-result";
        }

        boolean isSuccess = "00".equals(responseCode);

        // 3. Idempotent Update
        if (isSuccess) {
            if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                order.setPaymentStatus("PAID");
                order.setPaymentPaidAt(LocalDateTime.now());
                order.setVnpayTransactionNo(transactionNo);
                order.setPaymentBankCode(bankCode);
                order.setPaymentResponseCode(responseCode);
                order.setOrderStatus("CONFIRMED"); // Đơn đã thanh toán, sẵn sàng đóng gói
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                // Xác nhận xuất kho vĩnh viễn
                stockService.consumeStock(order);

                // Xóa giỏ hàng sau khi thanh toán thành công
                User user = getAuthenticatedUser(principal, session);
                String sessionId = (session != null) ? session.getId() : null;
                cartService.clearCart(user, sessionId);

                notificationService.notifyPaymentSuccess(order);
            }
            model.addAttribute("success", true);
            model.addAttribute("orderCode", order.getOrderCode());
            model.addAttribute("transactionNo", transactionNo);
            model.addAttribute("amount", receivedAmount);
            model.addAttribute("message", "Thanh toán thành công! Đơn hàng đang được chuẩn bị để giao tới bạn.");
        } else {
            // Thanh toán thất bại hoặc hủy bỏ
            if (!"CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
                order.setPaymentStatus("PAYMENT_FAILED");
                order.setPaymentResponseCode(responseCode);
                order.setPaymentFailureReason("Khách hàng hủy hoặc lỗi VNPAY mã: " + responseCode);
                order.setOrderStatus("CANCELLED");
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelledReason("Thanh toán VNPAY không thành công");
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                // Release tồn kho đã giữ
                stockService.releaseStock(order);

                // Hoàn lại CoolCash nếu đã trừ
                if (order.getUser() != null && order.getCoolcashUsed() != null && order.getCoolcashUsed().compareTo(BigDecimal.ZERO) > 0) {
                    coolCashService.refundCoolCash(order.getUser(), order, order.getCoolcashUsed(), 
                            "Hoàn lại CoolCash do giao dịch VNPAY thất bại cho đơn #" + order.getOrderCode());
                }

                notificationService.notifyPaymentFailed(order, "Mã phản hồi: " + responseCode);
            }
            model.addAttribute("success", false);
            model.addAttribute("orderCode", order.getOrderCode());
            model.addAttribute("message", "Giao dịch không thành công hoặc bạn đã hủy thanh toán. Sản phẩm trong giỏ hàng vẫn được giữ nguyên để bạn có thể thử lại.");
        }

        return "client/payment-result";
    }

    /**
     * B1: VNPAY IPN Callback (Server-to-Server)
     */
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

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        Map<String, String> response = new HashMap<>();

        if (!vnpayService.validateSignature(fields, vnp_SecureHash)) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
            return ResponseEntity.ok(response);
        }

        String txnRef = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        String transactionNo = request.getParameter("vnp_TransactionNo");
        String amountStr = request.getParameter("vnp_Amount");

        Order order = orderRepository.findByVnpayTxnRef(txnRef).orElse(null);
        if (order == null) {
            response.put("RspCode", "01");
            response.put("Message", "Order Not Found");
            return ResponseEntity.ok(response);
        }

        long receivedAmount = amountStr != null ? Long.parseLong(amountStr) / 100 : 0;
        if (order.getFinalAmount() == null || receivedAmount != order.getFinalAmount().longValue()) {
            response.put("RspCode", "04");
            response.put("Message", "Invalid Amount");
            return ResponseEntity.ok(response);
        }

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return ResponseEntity.ok(response);
        }

        if ("00".equals(responseCode)) {
            order.setPaymentStatus("PAID");
            order.setPaymentPaidAt(LocalDateTime.now());
            order.setVnpayTransactionNo(transactionNo);
            order.setPaymentResponseCode(responseCode);
            order.setOrderStatus("CONFIRMED");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            stockService.consumeStock(order);
        } else {
            order.setPaymentStatus("PAYMENT_FAILED");
            order.setOrderStatus("CANCELLED");
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelledReason("VNPAY IPN failed code: " + responseCode);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            stockService.releaseStock(order);
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return ResponseEntity.ok(response);
    }
}
