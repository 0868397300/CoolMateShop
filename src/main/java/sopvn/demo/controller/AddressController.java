package sopvn.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sopvn.demo.entity.User;
import sopvn.demo.entity.UserAddress;
import sopvn.demo.repository.UserAddressRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AddressController {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    public AddressController(UserAddressRepository userAddressRepository, UserRepository userRepository) {
        this.userAddressRepository = userAddressRepository;
        this.userRepository = userRepository;
    }

    /**
     * Thuật toán tính phí vận chuyển chuẩn Coolmate:
     * - Đơn từ 200.000đ: MIỄN PHÍ VẬN CHUYỂN (Freeship 0đ).
     * - Đơn dưới 200.000đ:
     *   + Nội thành Hà Nội / TP.HCM: 20.000đ
     *   + Ngoại thành Hà Nội / TP.HCM: 25.000đ
     *   + Các tỉnh thành khác: 30.000đ
     */
    @GetMapping("/shipping/calculate")
    public ResponseEntity<Map<String, Object>> calculateShipping(
            @RequestParam(value = "subtotal", defaultValue = "0") BigDecimal subtotal,
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "district", required = false) String district) {

        Map<String, Object> resp = new HashMap<>();

        if (subtotal.compareTo(BigDecimal.valueOf(200000)) >= 0) {
            resp.put("shippingFee", BigDecimal.ZERO);
            resp.put("isFreeship", true);
            resp.put("message", "Đã đạt chuẩn Miễn Phí Vận Chuyển toàn quốc!");
            return ResponseEntity.ok(resp);
        }

        BigDecimal fee = BigDecimal.valueOf(30000); // Mặc định các tỉnh khác
        String normProvince = province != null ? province.toLowerCase() : "";
        String normDistrict = district != null ? district.toLowerCase() : "";

        boolean isHanoiOrHcm = normProvince.contains("hà nội") || normProvince.contains("ha noi")
                || normProvince.contains("hồ chí minh") || normProvince.contains("ho chi minh")
                || normProvince.contains("hcm");

        if (isHanoiOrHcm) {
            // Các quận nội thành chính
            boolean isInnerCity = normDistrict.contains("quận 1") || normDistrict.contains("quận 3")
                    || normDistrict.contains("quận 4") || normDistrict.contains("quận 5")
                    || normDistrict.contains("quận 10") || normDistrict.contains("tân bình")
                    || normDistrict.contains("phú nhuận") || normDistrict.contains("bình thạnh")
                    || normDistrict.contains("hoàn kiếm") || normDistrict.contains("ba đình")
                    || normDistrict.contains("đống đa") || normDistrict.contains("hai bà trưng")
                    || normDistrict.contains("cầu giấy") || normDistrict.contains("thanh xuân");

            if (isInnerCity) {
                fee = BigDecimal.valueOf(20000); // Giao nhanh nội thành
            } else {
                fee = BigDecimal.valueOf(25000); // Ngoại thành
            }
        }

        resp.put("shippingFee", fee);
        resp.put("isFreeship", false);
        resp.put("message", "Phí giao hàng: " + fee + "đ (Mua thêm để được Freeship từ 200.000đ)");
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
