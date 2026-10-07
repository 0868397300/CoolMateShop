package sopvn.demo.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import sopvn.demo.cart.CartService;
import sopvn.demo.entity.Role;
import sopvn.demo.entity.User;
import sopvn.demo.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CartService cartService;

    public SecurityConfig(CartService cartService) {
        this.cartService = cartService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại trên hệ thống: " + email));

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new DisabledException("Tài khoản của bạn đã bị tạm khóa. Vui lòng liên hệ bộ phận hỗ trợ khách hàng Coolmate.");
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                for (Role r : user.getRoles()) {
                    if (r != null && r.getRoleName() != null && !r.getRoleName().isBlank()) {
                        String rName = r.getRoleName().trim();
                        if (!rName.startsWith("ROLE_")) {
                            rName = "ROLE_" + rName.toUpperCase();
                        }
                        authorities.add(new SimpleGrantedAuthority(rName));
                    }
                }
            }
            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
            }

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPasswordHash(),
                    authorities
            );
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            String uri = request.getRequestURI();
            String accept = request.getHeader("Accept");

            if (uri.startsWith("/api/") || (accept != null && accept.contains("application/json"))) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\":false,\"authenticated\":false,\"message\":\"Phiên đăng nhập đã hết hạn hoặc bạn chưa đăng nhập. Vui lòng đăng nhập để tiếp tục.\"}");
            } else if (uri.startsWith("/admin")) {
                response.sendRedirect("/auth/login?required=admin");
            } else if (uri.startsWith("/thanh-toan/checkout")) {
                response.sendRedirect("/auth/login?required=checkout");
            } else if (uri.startsWith("/gio-hang")) {
                response.sendRedirect("/auth/login?required=cart");
            } else if (uri.startsWith("/vi-coolcash") || uri.startsWith("/hoi-vien")) {
                response.sendRedirect("/auth/login?required=wallet");
            } else if (uri.startsWith("/thong-tin-tai-khoan") || uri.startsWith("/tai-khoan") || uri.startsWith("/profile")) {
                response.sendRedirect("/auth/login?required=profile");
            } else {
                response.sendRedirect("/auth/login");
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler(UserRepository userRepository) {
        return (request, response, authentication) -> {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                HttpSession session = request.getSession();
                session.setAttribute("currentUser", user);
                session.setAttribute("userId", user.getId());
                session.setAttribute("USER_ID", user.getId());
                session.setAttribute("USER_EMAIL", user.getEmail());
                session.setAttribute("USER_NAME", user.getFullName());
                session.setAttribute("membershipTier", user.getMembershipTier());

                // Merge Guest Cart: Kiểm tra cookie COOLMATE_GUEST_CART hoặc session id
                String guestToken = null;
                if (request.getCookies() != null) {
                    for (Cookie c : request.getCookies()) {
                        if ("COOLMATE_GUEST_CART".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                            guestToken = c.getValue();
                            break;
                        }
                    }
                }
                if (guestToken != null) {
                    cartService.mergeGuestCartToUser(guestToken, user);
                } else {
                    cartService.mergeGuestCartToUser(session.getId(), user);
                }
            }

            boolean isAdminOrStaff = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
            if (isAdminOrStaff) {
                response.sendRedirect("/admin/dashboard");
            } else {
                response.sendRedirect("/");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            DaoAuthenticationProvider authenticationProvider,
                                            SecurityContextRepository securityContextRepository,
                                            AuthenticationSuccessHandler customSuccessHandler) throws Exception {
        http
            // Bật CSRF cho browser forms, chỉ exempt webhook external callbacks và REST api
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                "/thanh-toan/vnpay-ipn/**",
                "/thanh-toan/vnpay-return/**",
                "/api/**"
            ))
            .authenticationProvider(authenticationProvider)
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint()))
            .authorizeHttpRequests(auth -> auth
                // Công khai hoàn toàn (Guest Commerce)
                .requestMatchers(
                    "/", "/index", "/san-pham/**", "/danh-muc/**", "/bo-suu-tap/**", "/tim-kiem/**",
                    "/coolclub/**", "/hoi-vien/**",
                    "/chinh-sach-doi-tra/**", "/doi-tra/chinh-sach/**",
                    "/tra-cuu-don-hang/**",
                    "/api/size-advisor/**",
                    "/thanh-toan/vnpay-return/**", "/thanh-toan/vnpay-ipn/**",
                    "/gio-hang/**", "/cart/**", "/api/cart/**",
                    "/auth/**", "/login", "/register",
                    "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"
                ).permitAll()

                // Phân quyền ADMIN ONLY: Báo cáo tài chính, quản lý sản phẩm, danh mục, khách hàng & ví, khuyến mãi, duyệt nhập kho
                .requestMatchers(
                    "/admin/dashboard/**",
                    "/admin/san-pham/**",
                    "/admin/khach-hang/**",
                    "/admin/khuyen-mai/**",
                    "/admin/danh-muc/**",
                    "/admin/nhap-kho/*/duyet",
                    "/admin/nhap-kho/*/tu-choi"
                ).hasRole("ADMIN")

                // Phân quyền STAFF & ADMIN: Đơn hàng, vận chuyển, tạo phiếu nhập kho, đổi trả, duyệt đánh giá
                .requestMatchers(
                    "/admin",
                    "/admin/don-hang/**",
                    "/admin/nhap-kho/**",
                    "/admin/doi-tra/**",
                    "/admin/danh-gia/**"
                ).hasAnyRole("ADMIN", "STAFF")

                // Các trang nghiệp vụ thành viên
                .requestMatchers(
                    "/thong-tin-tai-khoan/**", "/profile/**", "/tai-khoan/**",
                    "/vi-coolcash/**",
                    "/thanh-toan/checkout/**", "/thanh-toan/dat-hang/**",
                    "/don-hang/**",
                    "/doi-tra/yeu-cau/**",
                    "/danh-gia/gui/**",
                    "/wishlist/**",
                    "/api/wishlist/**"
                ).authenticated()

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(customSuccessHandler)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "COOLMATE_GUEST_CART")
                .permitAll()
            );

        return http.build();
    }
}
