module com.pma {
    requires transitive javafx.graphics; // Cho phép các module khác truy cập javafx.graphics thông qua com.pma
    requires javafx.controls;
    requires javafx.fxml;

    requires java.sql;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;

    requires jbcrypt; // Tên module tự động

    opens com.pma to javafx.fxml;
    opens com.pma.controller to javafx.fxml;
    // opens com.pma.model.entity to org.hibernate.orm.core, javafx.base;

    exports com.pma;
}