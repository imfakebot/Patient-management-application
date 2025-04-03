module com.pma {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires java.sql;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;

    requires jbcrypt; // Tên module tự động

    opens com.pma to javafx.fxml;
    opens com.pma.controller to javafx.fxml;
    // opens com.pma.model.entity to org.hibernate.orm.core, javafx.base;

    exports com.pma;
}