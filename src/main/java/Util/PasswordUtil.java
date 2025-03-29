package Util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Phương thức băm mật khẩu
    public static String hashPassword(String plainPassword) {
        // Sử dụng BCrypt để băm mật khẩu với salt ngẫu nhiên
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    // Phương thức kiểm tra mật khẩu
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        // So sánh mật khẩu người dùng nhập vào với mật khẩu đã băm
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
