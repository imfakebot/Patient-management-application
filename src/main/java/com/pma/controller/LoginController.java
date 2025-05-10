package com.pma.controller;

import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil; // Hoặc cách hiển thị lỗi của bạn
import com.pma.util.UIManager;   // Lớp quản lý UI của bạn

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor; // Nếu bạn dùng cho các control
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator; // Nếu bạn muốn thêm
import javafx.scene.control.TextField;
import javafx.scene.image.Image;     // Nếu bạn dùng cho toggle password
import javafx.scene.image.ImageView; // Nếu bạn dùng cho toggle password
import javafx.scene.input.MouseEvent; // Nếu bạn dùng cho toggle password
import javafx.scene.layout.VBox;    // Nếu bạn dùng VBox làm container form
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException; // Tài khoản bị vô hiệu hóa
import org.springframework.security.authentication.LockedException;   // Tài khoản bị khóa
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component // Đảm bảo đây là Spring Bean
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UIManager uiManager;

    @Autowired
    private UserAccountService userAccountService;

    // Khai báo các @FXML khớp với file LoginView.fxml của bạn
    @FXML
    private VBox loginFormContainer; // VBox cha để disable form
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField textPasswordField; // Cho chức năng hiện mật khẩu
    @FXML
    private ImageView toggleImage;      // Icon mắt
    @FXML
    private Button loginButton;
    @FXML
    private Label errorLabel;
    @FXML
    private ProgressIndicator progressIndicator; // Chỉ báo đang xử lý

    private boolean passwordVisible = false;
    // Đường dẫn đến ảnh, đảm bảo chúng có trong resources/com/pma/img/
    private final Image eyeOpenImage = new Image(getClass().getResourceAsStream("/com/pma/img/open.png"));
    private final Image eyeClosedImage = new Image(getClass().getResourceAsStream("/com/pma/img/closed.png"));

    @FXML
    public void initialize() {
        clearError();
        hideProgress(); // Ẩn progress ban đầu

        // Thiết lập ban đầu cho ẩn/hiện mật khẩu
        textPasswordField.setManaged(false);
        textPasswordField.setVisible(false);
        passwordField.setManaged(true);
        passwordField.setVisible(true);
        if (toggleImage != null) { // Kiểm tra null trước khi set
            toggleImage.setImage(eyeClosedImage);
            toggleImage.setCursor(Cursor.HAND);
        }

        // Đồng bộ text giữa hai trường
        textPasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        // Listener cho phím Enter
        passwordField.setOnAction(this::handleLoginButtonAction);
        textPasswordField.setOnAction(this::handleLoginButtonAction);
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

        // Thực hiện xác thực trên luồng nền để không làm đơ UI
        Thread authenticationThread = new Thread(() -> {
            try {
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
                log.debug("Attempting authentication for user: {}", username);

                Authentication authentication = authenticationManager.authenticate(token); // Điểm xác thực chính

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("User '{}' logged in successfully. Authorities: {}", username, authentication.getAuthorities());

                // Cập nhật thông tin đăng nhập cuối (IP có thể lấy phức tạp hơn trong Desktop)
                userAccountService.updateUserLoginInfo(username, "DesktopLogin"); // Thay "DesktopLogin" bằng thông tin phù hợp

                Platform.runLater(() -> {
                    hideProgress();
                    uiManager.switchToMainDashboard(); // Chuyển sang màn hình chính
                });

            } catch (UsernameNotFoundException e) {
                handleAuthenticationFailure("Invalid username or password.", username, false);
            } catch (BadCredentialsException e) {
                handleAuthenticationFailure("Invalid username or password.", username, true);
            } catch (LockedException e) {
                handleAuthenticationFailure("Account is locked. Please contact administrator.", username, false);
            } catch (DisabledException e) {
                handleAuthenticationFailure("Account is disabled. Please contact administrator.", username, false);
            } catch (AuthenticationException e) { // Bắt các lỗi AuthenticationException khác
                log.warn("Login failed for username '{}': {}", username, e.getMessage());
                handleAuthenticationFailure("Login failed: " + e.getMessage(), username, true);
            } catch (Exception e) { // Bắt các lỗi không mong muốn khác
                log.error("An unexpected error occurred during login for user '{}'", username, e);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("An unexpected error occurred. Please try again.");
                });
            }
        });
        authenticationThread.setDaemon(true); // Luồng phụ sẽ tự kết thúc khi luồng chính kết thúc
        authenticationThread.start();
    }

    // Phương thức xử lý chung cho các lỗi xác thực
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
        if (passwordVisible) {
            textPasswordField.setManaged(true);
            textPasswordField.setVisible(true);
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            if (toggleImage != null) {
                toggleImage.setImage(eyeOpenImage);
            }
            textPasswordField.requestFocus();
            textPasswordField.positionCaret(textPasswordField.getText().length());
        } else {
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            textPasswordField.setManaged(false);
            textPasswordField.setVisible(false);
            if (toggleImage != null) {
                toggleImage.setImage(eyeClosedImage);
            }
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    // Các phương thức tiện ích cho UI
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
        }
    }

    private void hideProgress() {
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
    }

    private void setFormDisabled(boolean disabled) {
        if (loginFormContainer != null) {
            loginFormContainer.setDisable(disabled);
        } else {
            // Fallback nếu không có VBox cha
            usernameField.setDisable(disabled);
            passwordField.setDisable(disabled);
            textPasswordField.setDisable(disabled);
            loginButton.setDisable(disabled);
            if (toggleImage != null) {
                toggleImage.setDisable(disabled);
            }
        }
    }
}
