package sopvn.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.entity.Order;
import sopvn.demo.entity.User;
import sopvn.demo.entity.UserAddress;
import sopvn.demo.repository.OrderRepository;
import sopvn.demo.repository.UserAddressRepository;
import sopvn.demo.repository.UserRepository;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository,
                             UserAddressRepository userAddressRepository,
                             OrderRepository orderRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User getAuthenticatedUser(Principal principal, HttpSession session) {
        if (principal != null) {
            return userRepository.findByEmail(principal.getName()).orElse(null);
        }
        if (session != null) {
            Object sUser = session.getAttribute("currentUser");
            if (sUser instanceof User) {
                return (User) sUser;
            }
            Object uid = session.getAttribute("userId");
            if (uid != null) {
                try {
                    return userRepository.findById(Long.valueOf(uid.toString())).orElse(null);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @GetMapping({"/thong-tin-tai-khoan", "/profile", "/tai-khoan"})
    public String viewProfile(Principal principal, HttpSession session, Model model) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login?required=profile";
        }

        user = userRepository.findById(user.getId()).orElse(user);

        List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIdDesc(user.getId());
        List<Order> recentOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("addresses", addresses);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("activeTab", "profile");

        return "profile";
    }

    @PostMapping("/thong-tin-tai-khoan/cap-nhat")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "gender", required = false, defaultValue = "Nam") String gender,
                                @RequestParam(value = "birthDate", required = false) String birthDateStr,
                                @RequestParam(value = "heightCm", required = false) Integer heightCm,
                                @RequestParam(value = "weightKg", required = false) Integer weightKg,
                                Principal principal, HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        user = userRepository.findById(user.getId()).orElse(user);
        user.setFullName(fullName != null ? fullName.trim() : user.getFullName());
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone.trim());
        }
        user.setGender(gender);
        if (birthDateStr != null && !birthDateStr.isBlank()) {
            try {
                user.setBirthDate(LocalDate.parse(birthDateStr));
            } catch (Exception ignored) {}
        }
        user.setHeightCm(heightCm);
        user.setWeightKg(weightKg);

        userRepository.save(user);

        if (session != null) {
            session.setAttribute("currentUser", user);
            session.setAttribute("USER_NAME", user.getFullName());
        }

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tài khoản thành công!");
        return "redirect:/thong-tin-tai-khoan";
    }

    @PostMapping("/thong-tin-tai-khoan/dia-chi/them")
    public String addAddress(@RequestParam("recipientName") String recipientName,
                             @RequestParam("recipientPhone") String recipientPhone,
                             @RequestParam("provinceName") String provinceName,
                             @RequestParam("districtName") String districtName,
                             @RequestParam("wardName") String wardName,
                             @RequestParam("streetAddress") String streetAddress,
                             @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault,
                             Principal principal, HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (isDefault) {
            List<UserAddress> existing = userAddressRepository.findByUserId(user.getId());
            for (UserAddress a : existing) {
                a.setIsDefault(false);
                userAddressRepository.save(a);
            }
        }

        UserAddress address = new UserAddress();
        address.setUser(user);
        address.setRecipientName(recipientName != null ? recipientName.trim() : "");
        address.setRecipientPhone(recipientPhone != null ? recipientPhone.trim() : "");
        address.setProvinceName(provinceName != null ? provinceName.trim() : "");
        address.setDistrictName(districtName != null ? districtName.trim() : "");
        address.setWardName(wardName != null ? wardName.trim() : "");
        address.setStreetAddress(streetAddress != null ? streetAddress.trim() : "");
        address.setIsDefault(isDefault);

        userAddressRepository.save(address);

        redirectAttributes.addFlashAttribute("successMessage", "Thêm địa chỉ giao hàng thành công!");
        return "redirect:/thong-tin-tai-khoan";
    }

    @PostMapping("/thong-tin-tai-khoan/dia-chi/xoa/{id}")
    public String deleteAddress(@PathVariable("id") Long addressId,
                                Principal principal, HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        userAddressRepository.findById(addressId).ifPresent(addr -> {
            if (addr.getUser() != null && addr.getUser().getId().equals(user.getId())) {
                userAddressRepository.delete(addr);
            }
        });

        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa địa chỉ thành công!");
        return "redirect:/thong-tin-tai-khoan";
    }

    @PostMapping("/thong-tin-tai-khoan/dia-chi/mac-dinh/{id}")
    public String setDefaultAddress(@PathVariable("id") Long addressId,
                                   Principal principal, HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        List<UserAddress> existing = userAddressRepository.findByUserId(user.getId());
        for (UserAddress a : existing) {
            a.setIsDefault(a.getId().equals(addressId));
            userAddressRepository.save(a);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đã đặt làm địa chỉ mặc định!");
        return "redirect:/thong-tin-tai-khoan";
    }

    @PostMapping("/thong-tin-tai-khoan/doi-mat-khau")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Principal principal, HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal, session);
        if (user == null) {
            return "redirect:/auth/login";
        }

        user = userRepository.findById(user.getId()).orElse(user);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu hiện tại không đúng!");
            return "redirect:/thong-tin-tai-khoan";
        }

        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có ít nhất 6 ký tự!");
            return "redirect:/thong-tin-tai-khoan";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xác nhận mật khẩu mới không khớp!");
            return "redirect:/thong-tin-tai-khoan";
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        return "redirect:/thong-tin-tai-khoan";
    }
}