package com.pma.model.enums;

public enum AppointmentStatus {
    Scheduled,
    Completed,
    Cancelled,
    No_Show // Dùng gạch dưới nếu tên chứa khoảng trắng trong DB không hợp lệ trong Java
            // Identifier
    // Hoặc sử dụng @JsonProperty("No Show") nếu dùng Jackson và muốn tên JSON khác
}
