package sopvn.demo.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.service.NotificationService;
import sopvn.demo.entity.PasswordResetToken;
import sopvn.demo.entity.User;
import sopvn.demo.repository.PasswordResetTokenRepository;
import sopvn.demo.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public ForgotPasswordController(UserRepository userRepository,
                                    PasswordResetTokenRepository passwordResetTokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @GetMapping("/auth/quen-mat-khau")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/auth/quen-mat-khau")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user != null) {
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

            PasswordResetToken prt = new PasswordResetToken(user, token, expiresAt);
            passwordResetTokenRepository.save(prt);

            notificationService.notifyPasswordReset(user, token);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Đã tạo liên kết đặt lại mật khẩu! Vui lòng kiểm tra email của bạn hoặc sử dụng mã xác nhận: " + token);
            return "redirect:/auth/dat-lai-mat-khau?token=" + token;
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản với email này.");
            return "redirect:/auth/quen-mat-khau";
        }
    }

    @GetMapping("/auth/dat-lai-mat-khau")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/auth/dat-lai-mat-khau")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        PasswordResetToken prt = passwordResetTokenRepository.findByTokenAndUsedFalse(token.trim()).orElse(null);
        if (prt == null || prt.isExpired()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "redirect:/auth/quen-mat-khau";
        }

        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return "redirect:/auth/dat-lai-mat-khau?token=" + token;
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xác nhận mật khẩu mới không khớp.");
            return "redirect:/auth/dat-lai-mat-khau?token=" + token;
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);

        redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay.");
        return "redirect:/auth/login";
    }
}
