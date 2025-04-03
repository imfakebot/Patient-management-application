package com.pma.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for validating passwords based on specific security criteria.
 * 
 * The password must meet the following requirements:
 * - At least one digit (0-9)
 * - At least one lowercase letter (a-z)
 * - At least one uppercase letter (A-Z)
 * - At least one special character (@#$%^&+=!)
 * - No whitespace characters
 * - Minimum length of 8 characters
 */
public class PasswordValidator {

    // Regular expression pattern for password validation
    // (?=.*[0-9]) : At least one digit
    // (?=.*[a-z]) : At least one lowercase letter
    // (?=.*[A-Z]) : At least one uppercase letter
    // (?=.*[@#$%^&+=!]) : At least one special character
    // (?=\\S+$) : No whitespace allowed
    // .{8,} : Minimum length of 8 characters
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    // Compiled pattern for efficient reuse
    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    /**
     * Validates whether the given password meets the defined security criteria.
     * 
     * @param password The password to validate.
     * @return {@code true} if the password is valid, {@code false} otherwise.
     */
    public static boolean isValid(String password) {
        // Return false if the password is null
        if (password == null) {
            return false;
        }

        // Match the password against the defined pattern
        Matcher matcher = PATTERN.matcher(password);
        return matcher.matches();
    }
}
