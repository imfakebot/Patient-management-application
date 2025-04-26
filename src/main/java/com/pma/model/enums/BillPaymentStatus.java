package com.pma.model.enums;

/**
 * Enum đại diện cho trạng thái thanh toán của hóa đơn.
 */
public enum BillPaymentStatus {
    /**
     * Hóa đơn đang chờ thanh toán
     */
    Pending,

    /**
     * Hóa đơn đã thanh toán đầy đủ
     */
    Paid,

    /**
     * Hóa đơn đã thanh toán một phần
     */
    Partially_Paid,

    /**
     * Hóa đơn đã bị hủy
     */
    Cancelled
}
