package sopvn.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    // Rate limiting: 60-second cooldown per email
    private final Map<String, Long> requestCooldownMap = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MILLIS = 60_000L;

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

    @GetMapping({"/auth/forgot-password", "/auth/quen-mat-khau"})
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping({"/auth/forgot-password", "/auth/quen-mat-khau"})
    public String handleForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        String cleanEmail = (email != null) ? email.trim().toLowerCase() : "";

        // Generic response message to avoid account enumeration
        String genericMessage = "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến bạn.";

        if (!cleanEmail.isBlank()) {
            long now = System.currentTimeMillis();
            Long lastReq = requestCooldownMap.get(cleanEmail);

            // Rate limiting check
            if (lastReq == null || (now - lastReq) >= COOLDOWN_MILLIS) {
                requestCooldownMap.put(cleanEmail, now);

                Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    // Invalidate old tokens for this user
                    try {
                        tokenRepository.deleteByUserId(user.getId());
                    } catch (Exception e) {
                        log.warn("Lỗi khi xóa token cũ của user #{}: {}", user.getId(), e.getMessage());
                    }

                    // Create new secure token with 15-minute expiry
                    String token = UUID.randomUUID().toString();
                    LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
                    PasswordResetToken resetToken = new PasswordResetToken(token, user, expiry);
                    tokenRepository.save(resetToken);

                    // Build URL with token and send quietly via notification service (never log token!)
                    String resetUrl = baseUrl + "/auth/reset-password?token=" + token;
                    notificationService.notifyPasswordReset(user, resetUrl);
                }
            } else {
                log.info("Rate limit triggered for email: {}", cleanEmail);
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", genericMessage);
        return "redirect:/auth/forgot-password";
    }

    @GetMapping({"/auth/reset-password", "/auth/dat-lai-mat-khau"})
    public String showResetPasswordForm(@RequestParam(value = "token", required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("errorMessage", "Liên kết đặt lại mật khẩu không hợp lệ.");
            return "auth/forgot-password";
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token.trim());
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired() || Boolean.TRUE.equals(tokenOpt.get().getIsUsed())) {
            model.addAttribute("errorMessage", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn (hiệu lực 15 phút).");
            return "auth/forgot-password";
        }

        model.addAttribute("token", token.trim());
        return "auth/reset-password";
    }

    @PostMapping({"/auth/reset-password", "/auth/dat-lai-mat-khau"})
    public String handleResetPassword(@RequestParam("token") String token,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        if (token == null || token.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã xác thực không hợp lệ.");
            return "redirect:/auth/forgot-password";
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token.trim());
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired() || Boolean.TRUE.equals(tokenOpt.get().getIsUsed())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "redirect:/auth/forgot-password";
        }

        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu phải có ít nhất 6 ký tự!");
            return "redirect:/auth/reset-password?token=" + token.trim();
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "redirect:/auth/reset-password?token=" + token.trim();
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // One-time use: mark used immediately
        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);

        redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.");
        return "redirect:/auth/login";
    }
}
