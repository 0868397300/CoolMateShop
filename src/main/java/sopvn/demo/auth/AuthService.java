package sopvn.demo.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sopvn.demo.auth.dto.RegisterRequest;
import sopvn.demo.core.exception.CustomException;
import sopvn.demo.entity.Role;
import sopvn.demo.entity.User;
import sopvn.demo.repository.RoleRepository;
import sopvn.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new CustomException("Địa chỉ email '" + req.getEmail() + "' đã được sử dụng. Nếu bạn đã có tài khoản, vui lòng bấm Đăng nhập.");
        }

        if (req.getPhone() != null && !req.getPhone().trim().isEmpty() && userRepository.existsByPhone(req.getPhone().trim())) {
            throw new CustomException("Số điện thoại '" + req.getPhone() + "' đã được liên kết với một tài khoản khác. Vui lòng kiểm tra lại.");
        }

        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CUSTOMER", "Khách hàng thành viên hội viên CoolClub")));

        User user = new User();
        user.setFullName(req.getFullName().trim());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPhone(req.getPhone() != null && !req.getPhone().trim().isEmpty() ? req.getPhone().trim() : null);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setGender(req.getGender() != null ? req.getGender() : "Nam");
        user.setHeightCm(req.getHeightCm());
        user.setWeightKg(req.getWeightKg());
        user.setMembershipTier("NEW");
        user.setCoolcashBalance(BigDecimal.ZERO);
        user.setTotalSpent(BigDecimal.ZERO);
        user.setIsActive(true);

        user.getRoles().add(customerRole);
        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(rawPassword, user.getPasswordHash()) && Boolean.TRUE.equals(user.getIsActive())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}
