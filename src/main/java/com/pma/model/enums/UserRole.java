package com.pma.model.enums;

public enum UserRole {
    // Đổi tên hằng số thành chữ hoa theo quy ước và để dễ dàng tạo chuỗi authority
    PATIENT("ROLE_PATIENT"),
    DOCTOR("ROLE_DOCTOR"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    UserRole(String authority) {
        this.authority = authority;
    }

    /**
     * Trả về chuỗi đại diện cho quyền (authority) được sử dụng trong Spring
     * Security. Ví dụ: "ROLE_ADMIN".
     *
     * @return Chuỗi authority.
     */
    public String getAuthority() {
        return authority;
    }
}
