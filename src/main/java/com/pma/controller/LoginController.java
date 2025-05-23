package com.pma.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.pma.model.entity.UserAccount;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

@Component
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UIManager uiManager;

    @Autowired
    private UserAccountService userAccountService;

    @FXML
    private VBox loginFormContainer;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField textPasswordField;
    @FXML
    private ImageView toggleImage;
    @FXML
    private Button loginButton;
    @FXML
    private Label errorLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    private boolean passwordVisible = false;
    private final Image eyeOpenImage = loadImage("/com/pma/img/open.png"); // Nên có đường dẫn đầy đủ từ resources
    private final Image eyeClosedImage = loadImage("/com/pma/img/closed.png");

    private Image loadImage(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            log.error("Failed to load image: {}", path, e);
            return null; // Hoặc một ảnh placeholder
        }
    }

    @FXML
    public void initialize() {
        clearError();
        hideProgress();

        textPasswordField.setManaged(false);
        textPasswordField.setVisible(false);
        passwordField.setManaged(true);
        passwordField.setVisible(true);

        if (toggleImage != null && eyeClosedImage != null) {
            toggleImage.setImage(eyeClosedImage);
            toggleImage.setCursor(Cursor.HAND);
        } else if (toggleImage != null) {
            log.warn("Toggle image or eyeClosedImage is null, cannot set initial image or cursor.");
        }

        // Đồng bộ textProperty an toàn
        if (textPasswordField != null && passwordField != null) {
            textPasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        }

        // Listener cho phím Enter
        if (passwordField != null) {
            passwordField.setOnAction(this::handleLoginButtonAction);
        }
        if (textPasswordField != null) {
            textPasswordField.setOnAction(this::handleLoginButtonAction);
        }
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordVisible ? textPasswordField.getText() : passwordField.getText();

        clearError();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password cannot be empty.");
            return;
        }

        showProgress();
        setFormDisabled(true);

        Thread authenticationThread = new Thread(() -> {
            try {
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
                Authentication authentication = authenticationManager.authenticate(token);

                UserAccount userAccount = userAccountService.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User account details not found post-authentication: " + username));

                // --- XỬ LÝ 2FA CƠ BẢN ---
                if (userAccount.isTwoFactorEnabled()) {
                    log.info("User '{}' authenticated (step 1), 2FA is enabled. Proceeding to 2FA screen.", username);
                    // Logic gửi OTP (ví dụ: email) nếu là phương thức chính hoặc không có TOTP secret
                    if (userAccount.getTwoFactorSecret() == null || userAccount.getTwoFactorSecret().isBlank()) {
                        try {
                            // Giả sử người dùng này sẽ dùng Email OTP nếu chưa setup TOTP
                            userAccountService.generateAndSendEmailOtp(userAccount.getUserId());
                            log.info("Email OTP sent for 2FA for user: {}", username);
                        } catch (Exception e) {
                            log.error("Failed to send Email OTP for user {}: {}", username, e.getMessage());
                            Platform.runLater(() -> {
                                hideProgress();
                                setFormDisabled(false);
                                showError("Could not send 2FA code. Please try again or contact support.");
                            });
                            return; // Dừng lại nếu lỗi gửi OTP
                        }
                    }
                    // Chuyển sang màn hình 2FA, truyền thông tin xác thực bước 1
                    final Authentication preAuthFor2FA = authentication; // Để dùng trong Platform.runLater
                    Platform.runLater(() -> {
                        hideProgress();
                        // setFormDisabled(false); // Để người dùng có thể thử lại nếu màn hình 2FA có vấn đề
                        uiManager.switchToTwoFactorAuthScreen(userAccount.getUsername(), preAuthFor2FA);
                    });
                } else {
                    // --- ĐĂNG NHẬP THÀNH CÔNG (KHÔNG CÓ 2FA HOẶC 2FA ĐÃ QUA) ---
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("User '{}' logged in successfully. Authorities: {}", username, authentication.getAuthorities());
                    userAccountService.updateUserLoginInfo(username, "DesktopLogin"); // Ghi lại IP nếu có

                    Platform.runLater(() -> {
                        hideProgress();
                        uiManager.switchToMainDashboard(); // Chuyển sang màn hình chính
                    });
                }
            } catch (UsernameNotFoundException e) { // Lỗi này thường được ném bởi UserDetailsService
                handleAuthenticationFailure("Invalid username or password.", username, false); // Không count failed attempt vì user không tồn tại
            } catch (BadCredentialsException e) { // Sai mật khẩu
                handleAuthenticationFailure("Invalid username or password.", username, true);
            } catch (LockedException e) { // Tài khoản bị khóa
                handleAuthenticationFailure("Account is locked. Please contact administrator.", username, false);
            } catch (DisabledException e) { // Tài khoản bị vô hiệu hóa
                handleAuthenticationFailure("Account is disabled. Please contact administrator.", username, false);
            } catch (AuthenticationException e) { // Các lỗi xác thực khác
                log.warn("Authentication failed for username '{}': {}", username, e.getMessage());
                handleAuthenticationFailure("Authentication failed: " + e.getMessage(), username, true);
            } catch (Exception e) { // Lỗi không mong muốn khác
                log.error("An unexpected error occurred during login for user '{}'", username, e);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("An unexpected error occurred. Please try again.");
                });
            }
        });
        authenticationThread.setDaemon(true);
        authenticationThread.start();
    }

    private void handleAuthenticationFailure(String errorMessage, String username, boolean countFailedAttempt) {
        log.warn("Authentication failure for '{}': {}", username, errorMessage);
        Platform.runLater(() -> {
            hideProgress();
            setFormDisabled(false);
            showError(errorMessage);
        });
        if (countFailedAttempt) {
            try {
                userAccountService.handleFailedLoginAttempt(username);
            } catch (Exception ex) {
                log.error("Failed to handle failed login attempt for {}: {}", username, ex.getMessage());
            }
        }
    }

    @FXML
    private void togglePasswordVisibility(MouseEvent event) {
        passwordVisible = !passwordVisible;
        Image currentImage = passwordVisible ? eyeOpenImage : eyeClosedImage;

        if (passwordVisible) {
            textPasswordField.setText(passwordField.getText()); // Copy giá trị
            textPasswordField.setManaged(true);
            textPasswordField.setVisible(true);
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            if (toggleImage != null && currentImage != null) {
                toggleImage.setImage(currentImage);
            }
            textPasswordField.requestFocus();
            textPasswordField.positionCaret(textPasswordField.getText().length());
        } else {
            passwordField.setText(textPasswordField.getText()); // Copy giá trị
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            textPasswordField.setManaged(false);
            textPasswordField.setVisible(false);
            if (toggleImage != null && currentImage != null) {
                toggleImage.setImage(currentImage);
            }
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            DialogUtil.showErrorAlert("Login Error", message);
        }
    }

    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }

    private void showProgress() {
        if (progressIndicator != null) {
            progressIndicator.setVisible(true);
            if (loginButton != null) {
                loginButton.setDisable(true); // Vô hiệu hóa nút khi đang xử lý

            }
        }
    }

    private void hideProgress() {
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
            if (loginButton != null) {
                loginButton.setDisable(false); // Bật lại nút

            }
        }
    }

    private void setFormDisabled(boolean disabled) {
        if (loginFormContainer != null) {
            loginFormContainer.setDisable(disabled);
        } else {
            if (usernameField != null) {
                usernameField.setDisable(disabled);
            }
            if (passwordField != null) {
                passwordField.setDisable(disabled);
            }
            if (textPasswordField != null) {
                textPasswordField.setDisable(disabled);
            }
            if (loginButton != null) {
                loginButton.setDisable(disabled); // Đã xử lý ở show/hideProgress

            }
            if (toggleImage != null) {
                toggleImage.setDisable(disabled);
            }
        }
        // Đảm bảo nút login cũng được disable/enable đúng cách cùng với progress
        if (loginButton != null && progressIndicator != null && progressIndicator.isVisible()) {
            loginButton.setDisable(true);
        } else if (loginButton != null) {
            loginButton.setDisable(disabled);
        }
    }

    // Giả sử bạn có Hyperlink hoặc Button với fx:id="registerLink" và onAction="#handleRegisterLinkAction"
    @FXML
    private void handleRegisterLinkAction(ActionEvent event) {
        log.info("Register link/button clicked. Switching to registration screen.");
        if (uiManager != null) {
            uiManager.switchToRegisterScreen(); // Đảm bảo UIManager có phương thức này
        } else {
            log.error("UIManager is null. Cannot switch to registration screen.");
            showError("UI navigation error. Please contact support.");
        }
    }
}
