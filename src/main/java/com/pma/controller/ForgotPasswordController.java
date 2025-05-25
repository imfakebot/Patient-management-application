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
                boolean requestProcessed = userAccountService.initiatePasswordReset(input);

                Platform.runLater(() -> {
                    hideProgress();
                    if (requestProcessed) { // Generic message for security
                        DialogUtil.showInfoAlert("Password Reset",
                                "If an account with that username or email exists, a password reset token has been sent. Please check your email (including spam folder) for the token and instructions.");
                        sendResetLinkButton.setDisable(true);
                        usernameOrEmailField.setDisable(true);
                        // User will then go to ResetPasswordScreen and input the token from email
                    } else {
                        // This case might occur if email sending failed critically or other specific internal error
                        setFormDisabled(false); 
                        showError("Could not process your request. Please try again later or contact support.");
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
        if (usernameOrEmailField != null) usernameOrEmailField.setDisable(disabled);
        if (sendResetLinkButton != null) sendResetLinkButton.setDisable(disabled);
    }
}