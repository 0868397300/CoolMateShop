package sopvn.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sopvn.demo.entity.User;
import sopvn.demo.entity.UserAddress;
import sopvn.demo.order.ShippingFeeService;
import sopvn.demo.repository.UserAddressRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AddressController {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final ShippingFeeService shippingFeeService;

    public AddressController(UserAddressRepository userAddressRepository,
                             UserRepository userRepository,
                             ShippingFeeService shippingFeeService) {
        this.userAddressRepository = userAddressRepository;
        this.userRepository = userRepository;
        this.shippingFeeService = shippingFeeService;
    }

    @GetMapping("/shipping/calculate")
    public ResponseEntity<Map<String, Object>> calculateShipping(
            @RequestParam(value = "subtotal", defaultValue = "0") BigDecimal subtotal,
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "district", required = false) String district) {

        Map<String, Object> resp = new HashMap<>();

        BigDecimal fee = shippingFeeService.calculateShippingFee(subtotal, province, district);
        boolean isFreeship = fee.compareTo(BigDecimal.ZERO) == 0;

        resp.put("shippingFee", fee);
        resp.put("isFreeship", isFreeship);
        if (isFreeship) {
            resp.put("message", "Đã đạt chuẩn Miễn Phí Vận Chuyển toàn quốc!");
        } else {
            resp.put("message", "Phí giao hàng: " + fee + "đ (Mua thêm để được Freeship từ 200.000đ)");
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/address/save")
    public ResponseEntity<Map<String, Object>> saveAddress(
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        Map<String, Object> resp = new HashMap<>();
        if (principal == null) {
            resp.put("success", false);
            resp.put("message", "Vui lòng đăng nhập để lưu địa chỉ.");
            return ResponseEntity.status(401).body(resp);
        }

        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            resp.put("success", false);
            resp.put("message", "Tài khoản không hợp lệ.");
            return ResponseEntity.status(401).body(resp);
        }

        String recipientName = (String) payload.get("recipientName");
        String recipientPhone = (String) payload.get("recipientPhone");
        String province = (String) payload.get("provinceName");
        String district = (String) payload.get("districtName");
        String ward = (String) payload.get("wardName");
        String street = (String) payload.get("streetAddress");
        Boolean isDefault = payload.get("isDefault") != null ? (Boolean) payload.get("isDefault") : false;

        if (Boolean.TRUE.equals(isDefault)) {
            List<UserAddress> oldAddrs = userAddressRepository.findByUserIdOrderByIsDefaultDesc(user.getId());
            for (UserAddress a : oldAddrs) {
                a.setIsDefault(false);
                userAddressRepository.save(a);
            }
        }

        UserAddress address = new UserAddress();
        address.setUser(user);
        address.setRecipientName(recipientName);
        address.setRecipientPhone(recipientPhone);
        address.setProvinceName(province);
        address.setDistrictName(district);
        address.setWardName(ward);
        address.setStreetAddress(street);
        address.setIsDefault(isDefault);

        UserAddress saved = userAddressRepository.save(address);

        resp.put("success", true);
        resp.put("addressId", saved.getId());
        resp.put("message", "Đã lưu địa chỉ giao hàng thành công.");
        return ResponseEntity.ok(resp);
    }
}
