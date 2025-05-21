package com.pma.model.enums;

public enum UserRole {
    Patient, Doctor, Admin;

    private final String value;

    UserRole(final String value) {
        this.value = value;
    }

    UserRole() {
        this.value = this.name();
    }

    public String getValue() {
        return value;
    }
}
