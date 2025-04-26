package com.pma.util;

import org.springframework.security.crypto.password.PasswordEncoder; // Import từ Spring Security
import org.springframework.stereotype.Component;

/**
 * Utility class for handling password hashing and verification using Spring
 * Security.
 *
 * This class provides methods to securely hash passwords and verify them
 * against hashed values using the configured PasswordEncoder bean.
 */
@Component // Giữ lại là một Spring Bean
public class PasswordUtil {

    // Inject PasswordEncoder bean đã được định nghĩa trong cấu hình Spring
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor for dependency injection.
     * Spring sẽ tự động inject PasswordEncoder bean đã được cấu hình.
     *
     * @param passwordEncoder The PasswordEncoder bean provided by Spring context.
     */
    // Đảm bảo Spring inject bean vào constructor
    public PasswordUtil(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hashes a plain text password using the configured PasswordEncoder.
     *
     * @param plainPassword The plain text password to hash.
     * @return A hashed password as a {@code String}, or null if input is null.
     */
    // Chuyển thành phương thức instance (không còn static)
    public String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            // Hoặc ném Exception tùy theo logic của bạn
            return null;
        }
        // Sử dụng PasswordEncoder đã được inject
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Verifies a plain text password against a hashed password using the configured
     * PasswordEncoder.
     *
     * @param plainPassword  The plain text password to verify.
     * @param hashedPassword The hashed password to compare against.
     * @return {@code true} if the plain text password matches the hashed password,
     *         {@code false} otherwise or if inputs are null.
     */
    // Chuyển thành phương thức instance (không còn static)
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            // Hoặc ném Exception tùy theo logic của bạn
            return false;
        }
        // Sử dụng PasswordEncoder đã được inject
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
}