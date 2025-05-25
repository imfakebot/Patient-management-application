package com.pma.model.enums;

/**
 * Enum để biểu thị kết quả của việc khởi tạo đặt lại mật khẩu.
 */
public enum PasswordResetInitiationResult {
    EMAIL_SENT, // Email đã được gửi thành công
    USER_NOT_FOUND_OR_NO_EMAIL, // Người dùng không tồn tại hoặc không có email liên kết
    EMAIL_SEND_FAILURE // Có lỗi khi gửi email
}
