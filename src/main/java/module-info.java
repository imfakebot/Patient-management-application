module com.pma {
    // === Core Java & Jakarta EE APIs ===
    requires java.sql; // Cần cho JDBC types (ngay cả khi dùng JPA)
    requires jakarta.persistence; // API chuẩn của JPA
    requires static lombok; // Lombok Annotation Processor (chỉ cần lúc biên dịch)

    // === Logging Facade ===
    requires org.slf4j; // SLF4J API
    requires spring.jcl; // Thêm yêu cầu module spring.jcl để tránh lỗi split package

    // === JavaFX UI Framework ===
    // 'transitive' nếu bạn muốn các module khác phụ thuộc vào com.pma cũng thấy
    // javafx.graphics
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    // === Spring Framework Modules ===
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context; // Core IoC Container, @Component, @Service, @Configuration etc.
    requires spring.beans; // Định nghĩa và quản lý Bean
    requires spring.data.jpa; // Spring Data JPA Abstractions (Các interface @Repository)
    requires spring.tx; // Quản lý Transaction của Spring (@Transactional)
    requires spring.security.core; // Các lớp cốt lõi của Spring Security (Authentication, Authorization)
    requires spring.security.config; // Hỗ trợ cấu hình Security
    requires spring.security.crypto; // Mã hóa mật khẩu (BCryptPasswordEncoder etc.)
    // requires spring.web; // Thêm nếu dùng Spring MVC hoặc WebFlux
    // requires spring.mail; // Thêm nếu dùng trực tiếp các lớp từ Spring Mail
    // (thường không cần nếu dùng JavaMailSender)

    // === Third-Party Libraries (Automatic Modules) ===
    // Tên module tự động có thể không ổn định, nhưng thường là cách duy nhất
    requires com.google.zxing; // Tên module tự động cho ZXing Core
    requires googleauth; // Tên module tự động cho Google Authenticator
    // JDBC Driver thường không cần requires vì dùng ServiceLoader, trừ khi gọi lớp
    // cụ thể
    requires org.hibernate.orm.core; // Cho phép truy cập các lớp Hibernate, bao gồm annotations
    // === OPEN PACKAGES (Rất quan trọng cho Reflection) ===

    // Mở các gói chính và controller cho JavaFX FXML loading
    opens com.pma to javafx.fxml;
    opens com.pma.controller to javafx.fxml;

    // Mở gói chứa các lớp @Entity cho Hibernate và Spring Core
    // !!! QUAN TRỌNG: Thay 'com.pma.model.entity' bằng tên package thực tế của bạn
    // !!!
    // opens com.pma.model.entity to org.hibernate.orm.core, spring.core;

    // Mở các gói chứa các thành phần Spring (@Configuration, @Service, @Repository,
    // Security Config)
    // !!! QUAN TRỌNG: Đảm bảo các package này tồn tại và KHÔNG RỖNG !!!
    // Nếu package không tồn tại hoặc rỗng khi biên dịch, bạn sẽ gặp lỗi/cảnh báo.
    // opens com.pma.config to spring.core, spring.beans, spring.context;
    // opens com.pma.service to spring.core, spring.beans, spring.context;
    // Mở package Repository cho cả Spring Core và Spring Data JPA
    // opens com.pma.repository to spring.core, spring.beans, spring.context,
    // spring.data.jpa; // <<< Đảm bảo package này
    // // tồn tại!
    // Mở package Security cho cả Spring Core và Spring Security Config
    // opens com.pma.security to spring.core, spring.beans, spring.context,
    // spring.security.config; // <<< Đảm bảo package
    // // này tồn tại!

    // === EXPORTS (Chỉ export các package nếu module khác cần truy cập trực tiếp)
    // ===
    exports com.pma; // Export lớp App chính (nếu có)
    // exports com.pma.dto; // Ví dụ: export các lớp DTO nếu cần
    // exports com.pma.service; // Ví dụ: export các interface Service nếu cần
}