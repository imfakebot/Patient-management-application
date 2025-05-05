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
    // requires spring.mail; // Đã bỏ, thay bằng spring.context.support

    // === Spring Security Modules ===
    requires spring.security.core;
    requires spring.security.config;
    requires spring.security.crypto;

    // === Third-Party Libraries (Automatic Modules / Explicit Modules) ===
    requires com.google.zxing; // Automatic module
    requires googleauth; // Automatic module
    requires org.hibernate.orm.core; // Module của Hibernate 6+

    // --- Dependencies cho Email ---
    requires spring.context.support; // Cho JavaMailSenderImpl và các lớp hỗ trợ mail của Spring
    requires jakarta.mail; // API chuẩn Jakarta Mail
    requires jakarta.activation; // API chuẩn Jakarta Activation (dependency của jakarta.mail)

    // === OPEN PACKAGES (Chỉ mở cho các framework cần reflection sâu) ===

    // Mở controller cho JavaFX FXML
    opens com.pma.controller to javafx.fxml;

    // Mở entity cho Hibernate
    // !!! THAY ĐÚNG TÊN PACKAGE ENTITY CỦA BẠN !!!
    opens com.pma.model.entity to org.hibernate.orm.core;

    // Mở các package chứa Spring Beans (@Service, @Repository, @Configuration) cho
    // Spring Core
    // Mặc dù Spring Boot thường có thể quét mà không cần 'opens', việc khai báo rõ
    // ràng
    // đôi khi cần thiết, đặc biệt khi có vấn đề hoặc dùng AOP/Proxy phức tạp.
    // Nếu gặp lỗi liên quan đến Bean không tìm thấy hoặc Proxy, hãy thử bỏ comment
    // các dòng này.
    opens com.pma.config; // Mở cho chính module (để Spring quét)
    opens com.pma.service; // Mở cho chính module
    opens com.pma.repository; // Mở cho chính module
    // opens com.pma.security; // Mở nếu có cấu hình Security phức tạp cần
    // reflection

    // === EXPORTS (Chỉ export các package cần thiết cho module khác hoặc UI) ===
    exports com.pma; // Lớp Application chính

    // Export các package chứa Entity và Enum nếu UI (JavaFX) cần truy cập trực tiếp
    // Hoặc nếu có module khác cần dùng chúng. Nếu không, không cần export.
    exports com.pma.model.entity;
    exports com.pma.model.enums;

    // Export các interface Service nếu UI hoặc module khác cần gọi
    exports com.pma.service;

    // Thường không cần export Repository interface ra ngoài Service layer
    exports com.pma.repository;

    exports com.pma.util;

    // Export DTO nếu có và cần dùng ở tầng khác
    // exports com.pma.dto;

}