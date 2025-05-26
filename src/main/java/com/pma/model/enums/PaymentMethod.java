package com.pma.model.enums;

/**
 * Enum đại diện cho các phương thức thanh toán.
 */
public enum PaymentMethod {
    CASH, // Tiền mặt
    CREDIT_CARD, // Thẻ tín dụng
    DEBIT_CARD, // Thẻ ghi nợ
    BANK_TRANSFER, // Chuyển khoản ngân hàng
    INSURANCE, // Bảo hiểm
    E_WALLET, // Ví điện tử (ví dụ: Momo, ZaloPay)
    OTHER // Phương thức khác
    // Thêm các phương thức khác nếu cần
}
