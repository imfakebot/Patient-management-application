package com.pma.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
import javafx.scene.layout.VBox; // Import UUID

@Component
public class TwoFactorAuthController {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorAuthController.class);

    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private UIManager uiManager;

    @FXML
    private VBox twoFactorFormContainer;
    @FXML
    private TextField otpCodeField;
    @FXML
    private Button verifyOtpButton;
    @FXML
    private Label errorLabel2FA;
    @FXML
    private ProgressIndicator progressIndicator2FA;
    @FXML
    private Label infoLabel2FA; // Để hiển thị thông tin cho người dùng

    private String usernameFor2FA;
    private Authentication preAuthenticatedToken; // Lưu token từ bước 1

    @FXML
    public void initialize() {
        clearError();
        hideProgress();
        otpCodeField.setOnAction(this::handleVerifyOtpAction); // Cho phép Enter
    }

    /**
     * Được gọi từ LoginController để truyền username và token xác thực bước 1.
     *
     * @param username Tên đăng nhập của người dùng.
     * @param preAuth Token xác thực từ bước 1 (có thể null nếu OTP được yêu cầu
     * do đăng nhập sai).
     * @param infoMessage Thông báo hiển thị cho người dùng.
     */
    public void initData(String username, Authentication preAuth, String infoMessage) {
        this.usernameFor2FA = username;
        this.preAuthenticatedToken = preAuth;
        infoLabel2FA.setText(infoMessage != null ? infoMessage : "Enter the OTP code.");
        log.info("2FA/OTP screen initialized for user: {}. Info: {}", username, infoMessage);
    }

    @FXML
    private void handleVerifyOtpAction(ActionEvent event) {
        String otpCode = otpCodeField.getText().trim();
        clearError();

        if (otpCode.isEmpty()) {
            showError("OTP code cannot be empty.");
            return;
        }
        if (usernameFor2FA == null || preAuthenticatedToken == null) {
            log.error("2FA/OTP verification attempted without username.");
            showError("An error occurred. Please try logging in again.");
            return;
        }

        showProgress();
        setFormDisabled(true);

        Thread verificationThread = new Thread(() -> {
            try {
                // Lấy userId từ username (cần chắc chắn username là duy nhất)
                UUID userId = userAccountService.findByUsername(usernameFor2FA)
                        .orElseThrow(() -> new IllegalStateException("User " + usernameFor2FA + " not found during 2FA verification."))
                        .getUserId();

                boolean isValidOtp = userAccountService.verifyTwoFactorCode(userId, otpCode);

                if (isValidOtp) {
                    log.info("2FA/OTP verification successful for user: {}", usernameFor2FA);
                    // Quan trọng: Reset số lần đăng nhập sai và cờ yêu cầu OTP
                    userAccountService.resetFailedLoginAttempts(userId);

                    if (preAuthenticatedToken != null) {
                        // Đây là luồng 2FA chuẩn sau khi đăng nhập bước 1 thành công
                        SecurityContextHolder.getContext().setAuthentication(preAuthenticatedToken);
                        userAccountService.updateUserLoginInfo(usernameFor2FA, "DesktopLogin_2FA_Verified");
                        Platform.runLater(() -> {
                            hideProgress();
                            uiManager.switchToMainDashboard();
                        });
                    } else {
                        // Đây là luồng OTP được yêu cầu do đăng nhập sai nhiều lần
                        log.info("OTP for failed attempts verified for user: {}. User should re-attempt login.", usernameFor2FA);
                        Platform.runLater(() -> {
                            hideProgress();
                            DialogUtil.showInfoAlert("OTP Verified", "OTP verification successful. Please log in with your credentials.");
                            uiManager.switchToLoginScreen();
                        });
                    }
                } else {
                    log.warn("Invalid 2FA code for user: {}", usernameFor2FA);
                    Platform.runLater(() -> {
                        hideProgress();
                        setFormDisabled(false);
                        showError("Invalid or expired OTP code.");
                    });
                }
            } catch (Exception e) {
                log.error("Error during 2FA verification for user '{}'", usernameFor2FA, e);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("An error occurred during 2FA verification.");
                });
            }
        });
        verificationThread.setDaemon(true);
        verificationThread.start();
    }

    // --- UI Helper Methods ---
    private void showError(String message) {
        if (errorLabel2FA != null) {
            errorLabel2FA.setText(message);
            errorLabel2FA.setVisible(true);
            errorLabel2FA.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            DialogUtil.showErrorAlert("2FA Error", message);
        }
    }

    private void clearError() {
        if (errorLabel2FA != null) {
            errorLabel2FA.setText("");
            errorLabel2FA.setVisible(false);
        }
    }

    private void showProgress() {
        if (progressIndicator2FA != null) {
            progressIndicator2FA.setVisible(true);

        }
    }

    private void hideProgress() {
        if (progressIndicator2FA != null) {
            progressIndicator2FA.setVisible(false);

        }
    }

    private void setFormDisabled(boolean disabled) {
        if (twoFactorFormContainer != null) {
            twoFactorFormContainer.setDisable(disabled);

        }

    }
}
