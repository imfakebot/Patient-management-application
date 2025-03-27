package com.pma;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PasswordToggleController {
    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField textPasswordField;

    @FXML
    private ImageView toggleImage;

    private boolean isPasswordVisible = false;

    private final String SHOW_ICON = "/img/eye_open.png"; // Thay bằng đường dẫn ảnh "hiện mật khẩu"
    private final String HIDE_ICON = "/img/eye_closed.png"; // Thay bằng đường dẫn ảnh "ẩn mật khẩu"

    @FXML
    private void initialize() {
        toggleImage.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));
    
        textPasswordField.setManaged(false);
        textPasswordField.setVisible(false);
        textPasswordField.textProperty().bindBidirectional(passwordField.textProperty());
    }
    
    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
    
        if (isPasswordVisible) {
            textPasswordField.setVisible(true);
            textPasswordField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            toggleImage.setImage(new Image(getClass().getResourceAsStream(SHOW_ICON)));
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            textPasswordField.setVisible(false);
            textPasswordField.setManaged(false);
            toggleImage.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));
        }
    }
}