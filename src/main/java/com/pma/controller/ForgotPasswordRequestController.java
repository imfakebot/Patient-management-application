package com.pma.controller;

import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ForgotPasswordRequestController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordRequestController.class);

    @FXML
    private VBox forgotPasswordFormContainer;
    @FXML
    private TextField emailOrUsernameField;
    @FXML
    private Button submitRequestButton;
    @FXML
    private Button backToLoginButton;
    @FXML
    private Label errorLabelForgotPass;
    @FXML
    private ProgressIndicator progressIndicatorForgotPass;

    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private UIManager uiManager;

    @FXML
    public void initialize() {
        clearError();
        hideProgress();
        emailOrUsernameField.setOnAction(this::handleSubmitRequestAction); // Allow Enter key
    }

    @FXML
    private void handleSubmitRequestAction(ActionEvent event) {
        String input = emailOrUsernameField.getText().trim();
        clearError();

        if (input.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập hoặc email của bạn.");
            return;
        }

        showProgress();
        setFormDisabled(true);

        // Run service call in a background thread
        new Thread(() -> {
            try {
                // This service method should internally handle email sending
                // and always return true for security reasons (not revealing if user exists)
                userAccountService.initiatePasswordReset(input);

                Platform.runLater(() -> {
                    hideProgress();
                    DialogUtil.showInfoAlert("Yêu cầu đã được xử lý",
                            "Nếu tài khoản của bạn tồn tại trong hệ thống, một email chứa hướng dẫn đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn (bao gồm cả thư mục spam).");
                    // Optionally disable the form further or navigate away
                    submitRequestButton.setDisable(true);
                    emailOrUsernameField.setDisable(true);
                });
            } catch (Exception e) {
                log.error("Lỗi khi bắt đầu quá trình đặt lại mật khẩu cho '{}': {}", input, e.getMessage(), e);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.");
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLoginAction(ActionEvent event) {
        uiManager.switchToLoginScreen();
    }

    private void showError(String message) {
        errorLabelForgotPass.setText(message);
        errorLabelForgotPass.setVisible(true);
    }

    private void clearError() {
        errorLabelForgotPass.setText("");
        errorLabelForgotPass.setVisible(false);
    }

    private void showProgress() {
        progressIndicatorForgotPass.setVisible(true);
    }

    private void hideProgress() {
        progressIndicatorForgotPass.setVisible(false);
    }

    private void setFormDisabled(boolean disabled) {
        forgotPasswordFormContainer.setDisable(disabled);
    }
}
