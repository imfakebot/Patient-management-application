module com.pma {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires transitive javafx.graphics;
    requires jbcrypt;

    opens com.pma to javafx.fxml;

    opens Controller to javafx.fxml;

    exports com.pma;
    exports Util;   
}
