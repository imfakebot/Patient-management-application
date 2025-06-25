package com.pma.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pma.service.CustomUserDetailsService;

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
    private CustomUserDetailsService customUserDetailsService; // Sử dụng CustomUserDetailsService đã được tạo

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Trả về instance của UserAccountService đã được inject.
        // CustomUserDetailsService đã implement interface UserDetailsService của Spring Security.
        return customUserDetailsService;
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
}
