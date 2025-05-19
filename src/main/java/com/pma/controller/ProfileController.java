package com.pma.controller;

import com.pma.model.entity.UserAccount;
import com.pma.service.UserAccountService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class ProfileController {

    @FXML private Label userIdLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button updateButton;

    @Autowired
    private UserAccountService userAccountService;

    private UserAccount currentUser;

    @FXML
    private void initialize() {
        roleComboBox.getItems().addAll("ADMIN", "DOCTOR", "STAFF");
        updateButton.setOnAction(event -> updateProfile());
        loadProfile();
    }

    private void loadProfile() {
        try {
            // Giả định lấy user hiện tại từ context
            currentUser = userAccountService.getAllUsers(null).getContent().get(0); // Thay bằng logic thực
            userIdLabel.setText(currentUser.getUserId().toString());
            usernameField.setText(currentUser.getUsername());
            passwordField.setText(currentUser.getPassword());
            emailField.setText(currentUser.getEmail());
            roleComboBox.setValue(currentUser.getRole());
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể tải hồ sơ: " + e.getMessage());
        }
    }

    private void updateProfile() {
        try {
            currentUser.setUsername(usernameField.getText());
            currentUser.setPassword(passwordField.getText());
            currentUser.setEmail(emailField.getText());
            currentUser.setRole(roleComboBox.getValue());
            userAccountService.updateUser(currentUser.getUserId(), currentUser);
            showAlert("Thành công", "Hồ sơ đã được cập nhật!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể cập nhật hồ sơ: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}