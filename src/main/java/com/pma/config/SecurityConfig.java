package com.pma.config;

import com.pma.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// Quan trọng: KHÔNG import EnableWebSecurity hay HttpSecurity, SecurityFilterChain
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cấu hình Spring Security cho ứng dụng Desktop. Tập trung vào Authentication
 * Provider và Method Security. KHÔNG cấu hình HttpSecurity hay
 * SecurityFilterChain.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Bật bảo mật phương thức
// KHÔNG cần @EnableWebSecurity
public class SecurityConfig {

    @Autowired
    @Lazy
    private UserAccountService userAccountService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userAccountService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Vẫn cần AuthenticationManager để xác thực thủ công
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // !!! KHÔNG CÓ BEAN SecurityFilterChain ở đây !!!
}
