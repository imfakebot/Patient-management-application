package Util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for handling password hashing and verification.
 * 
 * This class provides methods to securely hash passwords and verify them
 * against hashed values using the BCrypt algorithm.
 */
public class PasswordUtil {

    /**
     * Hashes a plain text password using the BCrypt algorithm.
     * 
     * @param plainPassword The plain text password to hash.
     * @return A hashed password as a {@code String}.
     */
    public static String hashPassword(String plainPassword) {
        // Use BCrypt to hash the password with a randomly generated salt
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /**
     * Verifies a plain text password against a hashed password.
     * 
     * @param plainPassword  The plain text password to verify.
     * @param hashedPassword The hashed password to compare against.
     * @return {@code true} if the plain text password matches the hashed password,
     *         {@code false} otherwise.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        // Compare the plain text password with the hashed password
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
