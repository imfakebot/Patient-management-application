module com.pma {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.pma to javafx.fxml;
    exports com.pma;
}
