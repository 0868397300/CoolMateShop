package sopvn.demo.config;

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
import org.springframework.security.core.context.SecurityContextHolder;
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
import sopvn.demo.entity.User;
import sopvn.demo.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CartService cartService;

    public SecurityConfig(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * B7: Bắt buộc dùng BCryptPasswordEncoder chuẩn, loại bỏ hoàn toàn so sánh plaintext
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Tài khoản hoặc mật khẩu không chính xác: " + email));

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new DisabledException("Tài khoản chưa được kích hoạt hoặc đã bị khóa.");
            }

            List<SimpleGrantedAuthority> authorities = user.getRoles() != null
                    ? user.getRoles().stream()
                            .map(role -> {
                                String r = role.getRoleName();
                                return new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r);
                            })
                            .collect(Collectors.toList())
                    : Collections.emptyList();

            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPasswordHash(),
                    authorities
            );
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
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
            String requestedWith = request.getHeader("X-Requested-With");
            boolean isAjax = "XMLHttpRequest".equals(requestedWith) 
                    || (accept != null && accept.contains("application/json"))
                    || uri.startsWith("/api/");

            if (isAjax) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"authenticated\":false,\"success\":false,\"message\":\"Bạn cần đăng nhập tài khoản CoolClub.\",\"redirectUrl\":\"/auth/login\"}");
                return;
            }

            if (uri.startsWith("/gio-hang")) {
                response.sendRedirect("/auth/login?required=cart");
            } else if (uri.startsWith("/thanh-toan/checkout")) {
                response.sendRedirect("/auth/login?required=checkout");
            } else if (uri.startsWith("/don-hang")) {
                response.sendRedirect("/auth/login?required=order");
            } else if (uri.startsWith("/vi-coolcash") || uri.startsWith("/hoi-vien")) {
                response.sendRedirect("/auth/login?required=wallet");
            } else if (uri.startsWith("/thong-tin-tai-khoan") || uri.startsWith("/tai-khoan") || uri.startsWith("/profile")) {
                response.sendRedirect("/auth/login?required=profile");
            } else if (uri.startsWith("/admin")) {
                response.sendRedirect("/auth/login?required=admin");
            } else {
                response.sendRedirect("/auth/login");
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler(UserRepository userRepository, SecurityContextRepository securityContextRepository) {
        return (request, response, authentication) -> {
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

            HttpSession session = request.getSession(true);
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                session.setAttribute("currentUser", user);
                session.setAttribute("user", user);
                session.setAttribute("userId", user.getId());
                session.setAttribute("USER_ID", user.getId());
                session.setAttribute("USER_EMAIL", user.getEmail());
                session.setAttribute("USER_NAME", user.getFullName());
                session.setAttribute("membershipTier", user.getMembershipTier());

                // D2: Tự động merge giỏ hàng vãng lai (Guest) vào tài khoản khi đăng nhập
                cartService.mergeGuestCartToUser(session.getId(), user);
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

    /**
     * B6: Phân quyền bảo mật chặt chẽ giữa ADMIN và STAFF
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, 
                                           DaoAuthenticationProvider authenticationProvider,
                                           SecurityContextRepository securityContextRepository,
                                           AuthenticationSuccessHandler customSuccessHandler) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API & Sandbox compatibility
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

                // Phân quyền ADMIN ONLY: Báo cáo tài chính, quản lý khách hàng & ví, khuyến mãi, duyệt nhập kho
                .requestMatchers(
                    "/admin/dashboard/**",
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
                    "/admin/san-pham/**",
                    "/admin/nhap-kho/**",
                    "/admin/doi-tra/**",
                    "/admin/danh-gia/**"
                ).hasAnyRole("ADMIN", "STAFF")

                // Các trang nghiệp vụ thành viên
                .requestMatchers(
                    "/thong-tin-tai-khoan/**", "/profile/**", "/tai-khoan/**",
                    "/vi-coolcash/**",
                    "/don-hang/**",
                    "/doi-tra/yeu-cau/**",
                    "/danh-gia/gui/**",
                    "/wishlist/**"
                ).authenticated()

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login-process")
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
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}