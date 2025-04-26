package com.pma.model.enums;

public enum BillItemType {
    Consultation, Medicine, Lab_Test("Lab Test"), Procedure, Other; // Cần value nếu có khoảng trắng/ký tự đặc biệt

    private final String value;

    BillItemType(String value) {
        this.value = value;
    }

    BillItemType() {
        this.value = this.name();
    }

    public String getValue() {
        return value;
    }
}
