package com.pma.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration; // No longer needed
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pma.service.UserAccountService;
import java.util.Collections;

/**
 * Cấu hình Spring Security cho ứng dụng Desktop. Tập trung vào Authentication
 * Provider và Method Security. KHÔNG cấu hình HttpSecurity hay
 * SecurityFilterChain nếu không cần thiết cho ứng dụng desktop.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Bật bảo mật dựa trên annotation trên phương thức
// @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity // Removed: Not needed for a desktop application and pulls in web dependencies
public class SecurityConfig {

    @Autowired
    @Lazy
    private UserAccountService userAccountService; // UserAccountService của bạn phải implement UserDetailsService

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Trả về instance của UserAccountService đã được inject.
        // UserAccountService cần implement interface UserDetailsService của Spring Security.
        return userAccountService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService()); // Sử dụng UserDetailsService đã định nghĩa ở trên
        authProvider.setPasswordEncoder(passwordEncoder());     // Sử dụng PasswordEncoder đã định nghĩa ở trên
        return authProvider;
    }

    /**
     * Cung cấp AuthenticationManager bean. Bean này được sử dụng trong
     * LoginController để xác thực người dùng. For non-web applications, we
     * construct it directly from the DaoAuthenticationProvider.
     */
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoAuthenticationProvider) {
        return new ProviderManager(Collections.singletonList(daoAuthenticationProvider));
    }

    // !!! LƯU Ý !!!
    // Với @EnableWebSecurity, Spring Security sẽ cố gắng tạo một SecurityFilterChain mặc định.
    // Đối với ứng dụng desktop không có web, điều này thường không gây vấn đề,
    // nhưng nếu bạn gặp lỗi liên quan đến việc thiếu Servlet API hoặc các cấu hình web,
    // bạn có thể cần định nghĩa một SecurityFilterChain rỗng hoặc tùy chỉnh nó
    // để vô hiệu hóa các tính năng web không cần thiết.
    // Ví dụ, để vô hiệu hóa CSRF và các cấu hình HTTP cơ bản nếu không cần:
    /*
    @Bean
    public org.springframework.security.web.SecurityFilterChain securityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Vô hiệu hóa CSRF nếu không dùng web form
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll()); // Cho phép tất cả nếu không có endpoint HTTP
        return http.build();
    }
     */
    // Tuy nhiên, đối với lỗi ban đầu của bạn (không tìm thấy AuthenticationConfiguration),
    // việc thêm @EnableWebSecurity là bước đầu tiên và quan trọng nhất.
    // Nếu không có yêu cầu HTTP nào, SecurityFilterChain mặc định có thể không ảnh hưởng.
}
