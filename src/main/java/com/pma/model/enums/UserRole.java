package com.pma.model.enums;

public enum UserRole {
    Patient, Doctor, Admin, Receptionist, Nurse, Lab_Staff("Lab Staff"); // Cần value nếu có khoảng trắng/ký tự đặc biệt

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    UserRole() {
        this.value = this.name();
    }

    public String getValue() {
        return value;
    }
}
