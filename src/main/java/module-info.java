module com.pma {
    // === Core Java & Jakarta EE APIs ===
    requires java.sql;
    requires jakarta.persistence;
    requires static lombok; // Chỉ cần lúc biên dịch

    // === Logging Facade ===
    requires org.slf4j;
    requires spring.jcl; // Bridge cho commons-logging

    // === JavaFX UI Framework ===
    requires transitive javafx.graphics; // transitive nếu module khác cần truy cập
    requires javafx.controls;
    requires javafx.fxml;

    // === Spring Framework Modules (Chủ yếu là các Starters và API cần thiết) ===
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context; // Cần cho các annotation cơ bản của Spring (@Component, etc.)
    requires spring.beans; // Cần cho cơ chế Bean của Spring
    requires spring.tx; // Cần cho @Transactional
    requires spring.data.commons; // <<< QUAN TRỌNG: Cho Page, Pageable, Sort, etc.
    requires spring.data.jpa; // Cho phép module com.pma đọc spring-data-jpa (chứa JpaRepository)
    // --- 'requires spring.core;' có thể cần nếu dùng @NonNullApi hoặc lớp cụ thể
    // từ core ---
    // requires spring.core; // Giữ lại nếu thực sự cần, nếu không có thể bỏ

    // === Spring Security Modules ===
    requires spring.security.core;
    requires spring.security.config;
    requires spring.security.crypto;

    // === Third-Party Libraries (Automatic Modules) ===
    requires com.google.zxing;
    requires googleauth;
    requires org.hibernate.orm.core; // Cần cho các annotation của Hibernate và truy cập nội bộ

    // === OPEN PACKAGES (Chỉ mở cho các framework cần reflection sâu) ===

    // Mở controller cho JavaFX FXML
    opens com.pma.controller to javafx.fxml;

    // Mở entity cho Hibernate
    // !!! THAY ĐÚNG TÊN PACKAGE ENTITY CỦA BẠN !!!
    opens com.pma.model.entity to org.hibernate.orm.core;

    // --- KHÔNG cần opens cho các module Spring ---
    // Spring component scanning sẽ hoạt động nếu các package nằm trong modulepath
    // opens com.pma.config to ... ;
    // opens com.pma.service to ... ;
    // opens com.pma.repository to ... ;
    // opens com.pma.security to ... ;

    // === EXPORTS (Chỉ export các package cần thiết cho module khác) ===
    exports com.pma; // Ví dụ: Lớp Application chính
    // exports com.pma.dto;
    // exports com.pma.service; // Ví dụ: export interface service
}