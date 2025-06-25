package com.pma.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.pma.model.entity.UserAccount;
import com.pma.model.enums.UserRole;
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
import javafx.scene.input.MouseEvent;

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
    private Label infoLabel2FA; // Để hiển thị thông tin cho người dùng
    @FXML
    private Label sendEmailOtpLabel; // Đổi tên để khớp với FXML

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
        infoLabel2FA.setText(infoMessage != null ? infoMessage : "Vui lòng nhập mã OTP.");
        log.info("Màn hình 2FA/OTP được khởi tạo cho người dùng: {}. Thông tin: {}", username, infoMessage);

        // Hiển thị tùy chọn "Gửi mã qua Email" cho người dùng không phải là Admin
        UserAccount user = userAccountService.findByUsername(usernameFor2FA).orElse(null);
        if (user != null && user.getRole() != UserRole.ADMIN) {
            sendEmailOtpLabel.setVisible(true);
            sendEmailOtpLabel.setManaged(true);
        } else {
            sendEmailOtpLabel.setVisible(false);
            sendEmailOtpLabel.setManaged(false);
        }
    }

    @FXML
    private void handleVerifyOtpAction(ActionEvent event) {
        String otpCode = otpCodeField.getText().trim();
        clearError();

        if (otpCode.isEmpty()) {
            showError("Mã OTP không được để trống.");
            return;
        }
        if (usernameFor2FA == null) { // preAuthenticatedToken có thể null nếu là OTP do đăng nhập sai
            log.error("Xác thực 2FA/OTP được thực hiện mà không có username.");
            showError("Đã xảy ra lỗi. Vui lòng đăng nhập lại.");
            return;
        }

        showProgress();
        setFormDisabled(true);

        Thread verificationThread = new Thread(() -> {
            try {
                // Lấy userId từ username (cần chắc chắn username là duy nhất)
                UserAccount user = userAccountService.findByUsername(usernameFor2FA)
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng " + usernameFor2FA + " trong quá trình xác thực 2FA/OTP."));
                UUID userId = user.getUserId();

                boolean isValidOtp = false;
                // Logic xác thực kết hợp:
                // 1. Thử xác thực bằng mã OTP gửi qua email trước.
                // 2. Nếu thất bại, và người dùng đã bật 2FA, thử xác thực bằng mã TOTP từ ứng dụng.
                if (userAccountService.verifyEmailOtp(userId, otpCode)) {
                    isValidOtp = true;
                    log.debug("Đã xác thực người dùng {} bằng mã OTP qua Email.", usernameFor2FA);
                } else if (user.isTwoFactorEnabled() && userAccountService.verifyTwoFactorCode(userId, otpCode)) {
                    isValidOtp = true;
                    log.debug("Đã xác thực người dùng {} bằng mã TOTP.", usernameFor2FA);
                }

                if (isValidOtp) {
                    log.info("Xác thực 2FA/OTP thành công cho người dùng: {}", usernameFor2FA);
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
                        log.info("Mã OTP do đăng nhập sai đã được xác thực cho người dùng: {}. Người dùng cần đăng nhập lại.", usernameFor2FA);
                        Platform.runLater(() -> {
                            hideProgress();
                            DialogUtil.showInfoAlert("Xác thực OTP thành công", "Xác thực OTP thành công. Vui lòng đăng nhập bằng thông tin của bạn.");
                            uiManager.switchToLoginScreen();
                        });
                    }
                } else {
                    log.warn("Mã 2FA/OTP không hợp lệ cho người dùng: {}", usernameFor2FA);
                    Platform.runLater(() -> {
                        hideProgress();
                        setFormDisabled(false);
                        showError("Mã OTP không hợp lệ hoặc đã hết hạn.");
                    });
                }
            } catch (Exception e) {
                log.error("Lỗi trong quá trình xác thực 2FA/OTP cho người dùng '{}'", usernameFor2FA, e);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("Đã xảy ra lỗi trong quá trình xác thực.");
                });
            }
        });
        verificationThread.setDaemon(true);
        verificationThread.start();
    }

    @FXML
    private void handleCancelButtonAction(ActionEvent event) {
        log.info("Người dùng {} đã hủy xác thực 2FA/OTP.", usernameFor2FA);
        // Chuyển về màn hình đăng nhập
        uiManager.switchToLoginScreen();
    }

    /**
     * Xử lý sự kiện khi người dùng nhấp vào "Gửi mã qua Email".
     */
    @FXML
    private void handleSendEmailOtpAction(MouseEvent event) {
        if (usernameFor2FA == null) {
            showError("Không thể gửi OTP. Thiếu thông tin người dùng.");
            return;
        }
        UserAccount user = userAccountService.findByUsername(usernameFor2FA).orElse(null);
        if (user == null) {
            showError("Không thể gửi OTP. Không tìm thấy người dùng.");
            return;
        }
        if (user.getRole() == UserRole.ADMIN) {
            log.warn("Đã chặn hành động gửi OTP qua email cho tài khoản ADMIN '{}'.", usernameFor2FA);
            showError("Tính năng này không khả dụng cho tài khoản quản trị viên.");
            return;
        }

        log.info("Người dùng {} yêu cầu gửi OTP qua email.", usernameFor2FA);
        showProgress();
        setFormDisabled(true);
        sendEmailOtpLabel.setDisable(true);

        Thread resendThread = new Thread(() -> {
            try {
                userAccountService.generateAndSendEmailOtp(user.getUserId());
                Platform.runLater(() -> {
                    infoLabel2FA.setText("Một mã OTP mới đã được gửi đến email của bạn.");
                    DialogUtil.showInfoAlert("Đã gửi OTP", "Một mã OTP mới đã được gửi đến email của bạn.");
                });
            } catch (Exception e) {
                log.error("Không thể gửi OTP cho người dùng {}: {}", usernameFor2FA, e.getMessage());
                Platform.runLater(() -> showError("Không thể gửi OTP. Vui lòng thử lại sau."));
            } finally {
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    sendEmailOtpLabel.setDisable(false);
                });
            }
        });
        resendThread.setDaemon(true);
        resendThread.start();
    }

    // --- UI Helper Methods ---
    private void showError(String message) {
        if (errorLabel2FA != null) {
            errorLabel2FA.setText(message);
            errorLabel2FA.setVisible(true);
            errorLabel2FA.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            DialogUtil.showErrorAlert("Lỗi 2FA", message);
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
