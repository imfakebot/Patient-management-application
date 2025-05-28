module com.pma {
    // === Core Java & Jakarta EE APIs ===
    requires java.sql;
    requires jakarta.persistence;
    requires static lombok; // Chỉ cần lúc biên dịch
    requires jakarta.validation; // THÊM DÒNG NÀY ĐỂ SỬ DỤNG VALIDATION ANNOTATIONS

    // === Logging Facade ===
    requires org.slf4j;
    requires spring.jcl; // Bridge cho commons-logging

    // === JavaFX UI Framework ===
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires javafx.swing; // THÊM DÒNG NÀY để sử dụng SwingFXUtils

    // === Spring Framework Modules ===
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.tx;
    requires spring.data.commons;
    requires spring.data.jpa;
    // ---> THÊM DÒNG NÀY <---
    requires spring.core; // Rất quan trọng, cho các lớp core của Spring
    requires spring.aop;// Cần cho AOP/Proxying (như @Transactional, @Async)

    // === Spring Security Modules ===
    requires spring.security.core;
    requires spring.security.config;
    requires spring.security.crypto;

    // === Third-Party Libraries ===
    requires com.google.zxing; // Cho các lớp core của ZXing như QRCodeWriter
    requires com.google.zxing.javase; // THÊM DÒNG NÀY để sử dụng MatrixToImageWriter
    requires googleauth;
    requires org.apache.commons.lang3;
    requires org.hibernate.orm.core;

    // --- Dependencies cho Email ---
    requires spring.context.support;
    requires jakarta.mail;
    requires jakarta.activation;

    // === OPEN PACKAGES ===
    opens com.pma.controller to javafx.fxml, spring.beans, spring.context,spring.core; // Mở cho JavaFX và Spring
    opens com.pma.model.entity to org.hibernate.orm.core, spring.core, spring.beans; // Mở cho Hibernate và Spring
    opens com.pma.config to spring.beans, spring.context, spring.core; // Mở cho Spring để xử lý cấu hình
    opens com.pma.service to spring.beans, spring.context, spring.aop, spring.core, spring.security.core; // Mở cho Spring
    opens com.pma.repository to spring.data.commons, spring.data.jpa, spring.beans; // Mở cho Spring Data
    opens com.pma.admin to spring.core, spring.beans, spring.context, javafx.fxml;
    // opens com.pma.security;

    // === EXPORTS ===
    exports com.pma;
    exports com.pma.model.entity;
    exports com.pma.model.enums;
    exports com.pma.service;
    exports com.pma.repository;
    exports com.pma.util;
    exports com.pma.config;
    exports com.pma.controller.admin to spring.beans, spring.context, javafx.fxml;
    // exports com.pma.dto;
    opens com.pma.util to spring.core, spring.beans, spring.context;
    opens com.pma.controller.admin to spring.core, spring.beans, spring.context, javafx.fxml;
}
