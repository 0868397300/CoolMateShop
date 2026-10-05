package sopvn.demo.config;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
import sopvn.demo.entity.User;
import sopvn.demo.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword == null) return false;
                if (encodedPassword.equals(rawPassword.toString())) {
                    return true;
                }
                try {
                    if (bcrypt.matches(rawPassword, encodedPassword)) {
                        return true;
                    }
                } catch (Exception ignored) {}
                return false;
            }
        };
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
                            .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
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
            } else if (uri.startsWith("/thanh-toan")) {
                response.sendRedirect("/auth/login?required=checkout");
            } else if (uri.startsWith("/don-hang")) {
                response.sendRedirect("/auth/login?required=order");
            } else if (uri.startsWith("/vi-coolcash")) {
                response.sendRedirect("/auth/login?required=wallet");
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
            .csrf(csrf -> csrf.disable())
            .authenticationProvider(authenticationProvider)
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index", "/san-pham/**", "/danh-muc/**", "/tim-kiem/**",
                    "/chinh-sach-doi-tra/**", "/doi-tra/chinh-sach/**",
                    "/auth/**", "/login", "/register",
                    "/api/**", "/coolclub/**", "/hoi-vien/**",
                    "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"
                ).permitAll()
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
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
