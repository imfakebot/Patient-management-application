module com.pma {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.pma.Controller to javafx.fxml;

    opens com.pma to javafx.fxml;

    exports com.pma;
}
