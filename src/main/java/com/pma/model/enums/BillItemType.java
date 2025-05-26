package com.pma.model.enums;

public enum BillItemType {
    // Đổi tên các hằng số theo quy ước chuẩn (chữ hoa, gạch dưới)
    // Tên này sẽ được lưu vào cơ sở dữ liệu khi dùng @Enumerated(EnumType.STRING)
    CONSULTATION,
    MEDICINE,
    LAB_TEST,
    PROCEDURE,
    OTHER;

    /**
     * Trả về tên của hằng số enum.
     * Đây là giá trị sẽ được lưu vào DB khi dùng @Enumerated(EnumType.STRING).
     * @return Tên hằng số enum.
     */
    public String getValue() {
        return this.name(); // Trả về tên hằng số (ví dụ: "CONSULTATION", "LAB_TEST")
    }
}
