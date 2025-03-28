package com.pma;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class RegisterPasswordToggleController {
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField textPasswordField;
    @FXML
    private ImageView toggleImagePassword;
    
    @FXML
    private PasswordField reEnterPasswordField;
    @FXML
    private TextField textReEnterPasswordField;
    @FXML
    private ImageView toggleImageReEnterPassword;

    private boolean isPasswordVisible = false;
    private boolean isReEnterPasswordVisible = false;

    private final String SHOW_ICON = "/img/eye_open.png"; // Icon hiện mật khẩu
    private final String HIDE_ICON = "/img/eye_closed.png"; // Icon ẩn mật khẩu

    @FXML
    private void initialize() {
        // Khởi tạo hình ảnh icon cho nút toggle
        toggleImagePassword.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));
        toggleImageReEnterPassword.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));

        // Liên kết nội dung giữa TextField và PasswordField
        textPasswordField.setManaged(false);
        textPasswordField.setVisible(false);
        textPasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        textReEnterPasswordField.setManaged(false);
        textReEnterPasswordField.setVisible(false);
        textReEnterPasswordField.textProperty().bindBidirectional(reEnterPasswordField.textProperty());
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            textPasswordField.setVisible(true);
            textPasswordField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            toggleImagePassword.setImage(new Image(getClass().getResourceAsStream(SHOW_ICON)));
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            textPasswordField.setVisible(false);
            textPasswordField.setManaged(false);
            toggleImagePassword.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));
        }
    }

    @FXML
    private void toggleReEnterPasswordVisibility() {
        isReEnterPasswordVisible = !isReEnterPasswordVisible;

        if (isReEnterPasswordVisible) {
            textReEnterPasswordField.setVisible(true);
            textReEnterPasswordField.setManaged(true);
            reEnterPasswordField.setVisible(false);
            reEnterPasswordField.setManaged(false);
            toggleImageReEnterPassword.setImage(new Image(getClass().getResourceAsStream(SHOW_ICON)));
        } else {
            reEnterPasswordField.setVisible(true);
            reEnterPasswordField.setManaged(true);
            textReEnterPasswordField.setVisible(false);
            textReEnterPasswordField.setManaged(false);
            toggleImageReEnterPassword.setImage(new Image(getClass().getResourceAsStream(HIDE_ICON)));
        }
    }
}