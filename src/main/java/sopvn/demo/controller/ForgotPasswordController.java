package sopvn.demo.controller;

import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;
import java.util.UUID;

@Controller
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ForgotPasswordController(UserRepository userRepository,
                                    PasswordResetTokenRepository tokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    NotificationService notificationService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @GetMapping("/auth/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            tokenRepository.deleteByUserId(user.getId());

            String token = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
            PasswordResetToken resetToken = new PasswordResetToken(token, user, expiry);
            tokenRepository.save(resetToken);

            // LỖI 10 FIX: Sử dụng baseUrl cấu hình
            String resetUrl = baseUrl + "/auth/reset-password?token=" + token;
            notificationService.notifyPasswordReset(user, resetUrl);
        }

        redirectAttributes.addFlashAttribute("successMessage", 
                "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến bạn.");
        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/auth/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired() || Boolean.TRUE.equals(tokenOpt.get().getIsUsed())) {
            model.addAttribute("errorMessage", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "auth/forgot-password";
        }

        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/auth/reset-password")
    public String handleResetPassword(@RequestParam("token") String token,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired() || Boolean.TRUE.equals(tokenOpt.get().getIsUsed())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "redirect:/auth/forgot-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "redirect:/auth/reset-password?token=" + token;
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu phải có ít nhất 6 ký tự!");
            return "redirect:/auth/reset-password?token=" + token;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);

        redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.");
        return "redirect:/auth/login";
    }
}
