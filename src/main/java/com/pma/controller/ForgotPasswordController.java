package com.pma.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pma.model.enums.PasswordResetInitiationResult;
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

@Component
public class ForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);

    @FXML
    private VBox forgotPasswordFormContainer;
    @FXML
    private TextField usernameOrEmailField;
    @FXML
    private Button sendResetLinkButton;
    @FXML
    private Button backToLoginButtonForgot;
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
        usernameOrEmailField.setOnAction(this::handleSendResetLinkAction); // Allow Enter key
    }

    @FXML
    private void handleSendResetLinkAction(ActionEvent event) {
        String input = usernameOrEmailField.getText().trim();
        clearError();

        if (input.isEmpty()) {
            showError("Please enter your username or email.");
            return;
        }

        showProgress();
        setFormDisabled(true);

        Thread resetInitiationThread = new Thread(() -> {
            try {
                PasswordResetInitiationResult result = userAccountService.initiatePasswordReset(input);

                Platform.runLater(() -> {
                    hideProgress();
                    switch (result) {
                        case EMAIL_SENT:
                            DialogUtil.showInfoAlert("Yêu cầu đã được xử lý",
                                    "Một email chứa mã đặt lại mật khẩu đã được gửi đến địa chỉ liên kết với tài khoản của bạn. Vui lòng kiểm tra hộp thư (bao gồm cả thư mục spam) và nhập mã đó cùng với mật khẩu mới của bạn vào màn hình tiếp theo.");
                            // Chuyển đến màn hình đặt lại mật khẩu, truyền username/email đã nhập
                            // để ResetPasswordController có thể điền sẵn nếu muốn.
                            uiManager.switchToResetPasswordScreen(input);
                            break;
                        case USER_NOT_FOUND_OR_NO_EMAIL:
                            setFormDisabled(false); // Kích hoạt lại form
                            showError("Tài khoản không tồn tại hoặc không có email liên kết. Vui lòng thử lại. Nếu chưa có tài khoản, bạn có thể quay lại để đăng ký.");
                            // Nút "Quay lại đăng nhập" đã có sẵn và sẽ được kích hoạt lại.
                            // Bạn có thể thêm nút "Đăng ký" vào FXML nếu muốn có tùy chọn trực tiếp.
                            break;
                        case EMAIL_SEND_FAILURE:
                            setFormDisabled(false); // Kích hoạt lại form
                            showError("Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau hoặc liên hệ hỗ trợ.");
                            break;
                    }
                });
            } catch (Exception e) {
                log.error("Error initiating password reset for input '{}': {}", input, e.getMessage(), e);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("An unexpected error occurred. Please try again.");
                });
            }
        });
        resetInitiationThread.setDaemon(true);
        resetInitiationThread.start();
    }

    @FXML
    private void handleBackToLoginAction(ActionEvent event) {
        uiManager.switchToLoginScreen();
    }

    private void showError(String message) {
        if (errorLabelForgotPass != null) {
            errorLabelForgotPass.setText(message);
            errorLabelForgotPass.setVisible(true);
            errorLabelForgotPass.setManaged(true);
        } else {
            DialogUtil.showErrorAlert("Error", message);
        }
    }

    private void clearError() {
        if (errorLabelForgotPass != null) {
            errorLabelForgotPass.setText("");
            errorLabelForgotPass.setVisible(false);
            errorLabelForgotPass.setManaged(false);
        }
    }

    private void showProgress() {
        if (progressIndicatorForgotPass != null) {
            progressIndicatorForgotPass.setVisible(true);
            progressIndicatorForgotPass.setManaged(true);
        }
    }

    private void hideProgress() {
        if (progressIndicatorForgotPass != null) {
            progressIndicatorForgotPass.setVisible(false);
            progressIndicatorForgotPass.setManaged(false);
        }
    }

    private void setFormDisabled(boolean disabled) {
        if (forgotPasswordFormContainer != null) {
            forgotPasswordFormContainer.setDisable(disabled);
        }
        if (usernameOrEmailField != null) {
            usernameOrEmailField.setDisable(disabled);
        }
        if (sendResetLinkButton != null) {
            sendResetLinkButton.setDisable(disabled);
        }
    }
}
