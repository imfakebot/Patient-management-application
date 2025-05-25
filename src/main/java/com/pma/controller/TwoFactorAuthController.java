package com.pma.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.pma.model.entity.UserAccount;
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
import javafx.scene.input.MouseEvent; // Import MouseEvent

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
    private Button cancelButton; // Thêm nút Cancel
    @FXML
    private Label errorLabel2FA;
    @FXML
    private ProgressIndicator progressIndicator2FA;
    @FXML
    private Label resendOtpLabel; // Thêm Label để resend OTP
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

        // Hiển thị tùy chọn "Resend OTP" nếu đây là luồng OTP qua email
        // (tức là preAuth != null VÀ 2FA (TOTP) chưa được bật HOẶC preAuth == null (OTP do đăng nhập sai))
        UserAccount user = userAccountService.findByUsername(usernameFor2FA).orElse(null);
        if (user != null && ((preAuthenticatedToken != null && !user.isTwoFactorEnabled()) || preAuthenticatedToken == null)) {
            resendOtpLabel.setVisible(true);
            resendOtpLabel.setManaged(true);
        } else {
            resendOtpLabel.setVisible(false);
            resendOtpLabel.setManaged(false);
        }
    }

    @FXML
    private void handleVerifyOtpAction(ActionEvent event) {
        String otpCode = otpCodeField.getText().trim();
        clearError();

        if (otpCode.isEmpty()) {
            showError("OTP code cannot be empty.");
            return;
        }
        if (usernameFor2FA == null) { // preAuthenticatedToken có thể null nếu là OTP do đăng nhập sai
            log.error("2FA/OTP verification attempted without username.");
            showError("An error occurred. Please try logging in again.");
            return;
        }

        showProgress();
        setFormDisabled(true);

        Thread verificationThread = new Thread(() -> {
            try {
                // Lấy userId từ username (cần chắc chắn username là duy nhất)
                UserAccount user = userAccountService.findByUsername(usernameFor2FA)
                        .orElseThrow(() -> new IllegalStateException("User " + usernameFor2FA + " not found during 2FA/OTP verification."));
                UUID userId = user.getUserId();

                boolean isValidOtp;
                // Nếu preAuthenticatedToken tồn tại VÀ người dùng đã bật 2FA (TOTP)
                // thì đây là xác minh TOTP.
                // Ngược lại, đây là xác minh OTP qua email (do đăng nhập sai hoặc flow khác).
                if (preAuthenticatedToken != null && user.isTwoFactorEnabled()) {
                    log.debug("Verifying TOTP for user {} (2FA enabled)", usernameFor2FA);
                    isValidOtp = userAccountService.verifyTwoFactorCode(userId, otpCode); // Xác minh TOTP
                } else {
                    log.debug("Verifying Email OTP for user {} (2FA not enabled or OTP for recovery)", usernameFor2FA);
                    isValidOtp = userAccountService.verifyEmailOtp(userId, otpCode); // Xác minh OTP Email
                }

                if (isValidOtp) {
                    log.info("2FA/OTP verification successful for user: {}", usernameFor2FA);
                    // Quan trọng: Reset số lần đăng nhập sai và cờ yêu cầu OTP
                    userAccountService.resetFailedLoginAttempts(userId);

                    if (preAuthenticatedToken != null) {
                        // Đây là luồng 2FA (TOTP hoặc OTP email sau bước 1) thành công
                        SecurityContextHolder.getContext().setAuthentication(preAuthenticatedToken);
                        userAccountService.updateUserLoginInfo(usernameFor2FA, "DesktopLogin_2FA_Verified");
                        final Authentication finalAuth = preAuthenticatedToken;
                        Platform.runLater(() -> {
                            hideProgress();
                            uiManager.navigateAfterLogin(finalAuth); // Thay đổi ở đây
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
                    log.warn("Invalid 2FA/OTP code for user: {}", usernameFor2FA);
                    Platform.runLater(() -> {
                        hideProgress();
                        setFormDisabled(false);
                        showError("Invalid or expired OTP code.");
                    });
                }
            } catch (Exception e) {
                log.error("Error during 2FA/OTP verification for user '{}'", usernameFor2FA, e);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("An error occurred during verification.");
                });
            }
        });
        verificationThread.setDaemon(true);
        verificationThread.start();
    }

    @FXML
    private void handleCancelButtonAction(ActionEvent event) {
        log.info("2FA/OTP verification cancelled by user: {}", usernameFor2FA);
        // Chuyển về màn hình đăng nhập
        uiManager.switchToLoginScreen();
    }

    @FXML
    private void handleResendOtpAction(MouseEvent event) {
        if (usernameFor2FA == null) {
            showError("Cannot resend OTP. User context is missing.");
            return;
        }
        UserAccount user = userAccountService.findByUsername(usernameFor2FA).orElse(null);
        if (user == null) {
            showError("Cannot resend OTP. User not found.");
            return;
        }

        // Chỉ cho phép resend nếu đây là luồng OTP email
        if ((preAuthenticatedToken != null && !user.isTwoFactorEnabled()) || preAuthenticatedToken == null) {
            log.info("Resend OTP requested for user: {}", usernameFor2FA);
            showProgress(); // Hiển thị progress
            setFormDisabled(true); // Vô hiệu hóa form
            resendOtpLabel.setDisable(true); // Vô hiệu hóa link resend tạm thời

            Thread resendThread = new Thread(() -> {
                try {
                    userAccountService.generateAndSendEmailOtp(user.getUserId());
                    Platform.runLater(() -> {
                        DialogUtil.showInfoAlert("OTP Resent", "A new OTP has been sent to your email.");
                    });
                } catch (Exception e) {
                    log.error("Failed to resend OTP for user {}: {}", usernameFor2FA, e.getMessage());
                    Platform.runLater(() -> showError("Could not resend OTP. Please try again later."));
                } finally {
                    Platform.runLater(() -> {
                        hideProgress();
                        setFormDisabled(false);
                        resendOtpLabel.setDisable(false); // Kích hoạt lại link resend
                    });
                }
            });
            resendThread.setDaemon(true);
            resendThread.start();
        } else {
            log.warn("Resend OTP clicked for user {} but it's not an email OTP flow.", usernameFor2FA);
        }
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
