package com.pma.controller; // Package của bạn

import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil; // Giả sử có lớp này
import com.pma.util.UIManager;   // Giả sử có lớp này

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator; // Thêm import nếu dùng progress
import javafx.scene.control.TextField;
import javafx.scene.image.Image; // Import cho hình ảnh
import javafx.scene.image.ImageView; // Import cho hình ảnh
import javafx.scene.input.MouseEvent; // Import cho sự kiện click chuột
import javafx.scene.layout.VBox; // Import layout VBox
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    // Inject các Spring Beans
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UIManager uiManager;

    @Autowired
    private UserAccountService userAccountService;

    // Các thành phần FXML khớp với FXML bạn cung cấp
    @FXML
    private VBox loginFormContainer; // VBox chứa các control để disable/enable (thay vì loginForm)
    @FXML
    private TextField usernameField; // Cần thêm fx:id="usernameField" vào FXML
    @FXML
    private PasswordField passwordField; // fx:id="passwordField" đã có
    @FXML
    private TextField textPasswordField; // fx:id="textPasswordField" đã có (để hiện pass)
    @FXML
    private ImageView toggleImage;      // fx:id="toggleImage" đã có (icon mắt)
    @FXML
    private Button loginButton;         // Cần thêm fx:id="loginButton" và onAction="#handleLoginButtonAction" vào FXML
    @FXML
    private Label errorLabel;           // Cần thêm fx:id="errorLabel" vào FXML (ví dụ đặt dưới nút Login)
    // @FXML private ProgressIndicator progressIndicator; // Thêm fx:id nếu bạn muốn dùng progress

    // Biến trạng thái cho việc ẩn/hiện mật khẩu
    private boolean passwordVisible = false;
    // Hình ảnh cho nút ẩn/hiện mật khẩu (cần đặt đường dẫn đúng)
    private final Image eyeOpenImage = new Image(getClass().getResourceAsStream("/com/pma/img/eye_open.png")); // Thay đường dẫn
    private final Image eyeClosedImage = new Image(getClass().getResourceAsStream("/com/pma/img/eye_closed.png")); // Thay đường dẫn

    @FXML
    public void initialize() {
        clearError();
        // hideProgress(); // Bỏ comment nếu dùng progress
        // Thiết lập ban đầu cho ẩn/hiện mật khẩu
        textPasswordField.setManaged(false); // Không quản lý layout
        textPasswordField.setVisible(false); // Ẩn đi
        passwordField.setManaged(true);
        passwordField.setVisible(true);
        toggleImage.setImage(eyeClosedImage); // Mặc định là mắt đóng

        // Đồng bộ text giữa hai trường khi thay đổi
        textPasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        // Cho phép nhấn Enter trên password field để login
        passwordField.setOnAction(this::handleLoginButtonAction);
        textPasswordField.setOnAction(this::handleLoginButtonAction);

        // Thay đổi con trỏ khi di chuột qua icon mắt
        if (toggleImage != null) {
            toggleImage.setCursor(Cursor.HAND);
        }
    }

    /**
     * Xử lý sự kiện khi nút Login được nhấn. (Cần thêm
     * onAction="#handleLoginButtonAction" vào Button trong FXML)
     */
    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        // Lấy mật khẩu từ trường đang hiển thị (PasswordField hoặc TextField)
        String password = passwordVisible ? textPasswordField.getText() : passwordField.getText();

        clearError();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password cannot be empty.");
            return;
        }

        // showProgress(); // Bỏ comment nếu dùng progress
        setFormDisabled(true);

        // Thực hiện xác thực trên luồng nền
        Thread authenticationThread = new Thread(() -> {
            try {
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
                log.debug("Attempting authentication for user: {}", username);
                Authentication authentication = authenticationManager.authenticate(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("User '{}' logged in successfully. Authorities: {}", username, authentication.getAuthorities());

                // Cập nhật thông tin login cuối
                try {
                    userAccountService.updateUserLoginInfo(username, "local_login");
                } catch (Exception e) {
                    log.error("Failed to update user login info for {}", username, e);
                }

                // Chuyển màn hình (chạy trên luồng UI)
                Platform.runLater(() -> {
                    // hideProgress();
                    uiManager.switchToMainScreen();
                });

            } catch (UsernameNotFoundException | BadCredentialsException e) {
                log.warn("Login failed: Invalid credentials for username '{}'.", username);
                Platform.runLater(() -> {
                    // hideProgress();
                    setFormDisabled(false);
                    showError("Invalid username or password.");
                });
                try {
                    userAccountService.handleFailedLoginAttempt(username);
                } catch (Exception ex) {
                    log.error("Failed to handle failed login attempt for {}", username, ex);
                }
            } catch (LockedException e) {
                log.warn("Login failed: Account locked for username '{}'.", username);
                Platform.runLater(() -> {
                    // hideProgress();
                    setFormDisabled(false);
                    showError("Account is locked. Please contact administrator.");
                });
            } catch (AuthenticationException e) {
                log.warn("Login failed for username '{}': {}", username, e.getMessage());
                Platform.runLater(() -> {
                    // hideProgress();
                    setFormDisabled(false);
                    showError("Login failed: " + e.getMessage());
                });
                try {
                    userAccountService.handleFailedLoginAttempt(username);
                } catch (Exception ex) {
                    log.error("Failed to handle failed login attempt for {}", username, ex);
                }
            } catch (Exception e) {
                log.error("An unexpected error occurred during login for user '{}'", username, e);
                Platform.runLater(() -> {
                    // hideProgress();
                    setFormDisabled(false);
                    showError("An unexpected error occurred during login.");
                });
            }
        });
        authenticationThread.setDaemon(true);
        authenticationThread.start();
    }

    /**
     * Xử lý sự kiện khi nhấn vào icon mắt để ẩn/hiện mật khẩu. (Cần thêm
     * onMouseClicked="#togglePasswordVisibility" vào ImageView toggleImage
     * trong FXML)
     */
    @FXML
    private void togglePasswordVisibility(MouseEvent event) {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            // Hiện TextField, ẩn PasswordField
            textPasswordField.setManaged(true);
            textPasswordField.setVisible(true);
            passwordField.setManaged(false);
            passwordField.setVisible(false);
            toggleImage.setImage(eyeOpenImage); // Đổi thành mắt mở
            textPasswordField.requestFocus(); // Focus vào text field
            textPasswordField.positionCaret(textPasswordField.getText().length()); // Di chuyển con trỏ về cuối
        } else {
            // Hiện PasswordField, ẩn TextField
            passwordField.setManaged(true);
            passwordField.setVisible(true);
            textPasswordField.setManaged(false);
            textPasswordField.setVisible(false);
            toggleImage.setImage(eyeClosedImage); // Đổi thành mắt đóng
            passwordField.requestFocus(); // Focus vào password field
            passwordField.positionCaret(passwordField.getText().length()); // Di chuyển con trỏ về cuối
        }
    }

    // --- Các phương thức tiện ích cho UI ---
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setStyle("-fx-text-fill: red;"); // Thêm style cho lỗi
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

    /* // Bỏ comment nếu dùng ProgressIndicator
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
     */
    private void setFormDisabled(boolean disabled) {
        // Vô hiệu hóa VBox chứa các control hoặc từng control riêng lẻ
        if (loginFormContainer != null) { // Giả sử VBox cha có fx:id="loginFormContainer"
            loginFormContainer.setDisable(disabled);
        } else {
            usernameField.setDisable(disabled);
            passwordField.setDisable(disabled);
            textPasswordField.setDisable(disabled); // Cả trường text cũng disable
            loginButton.setDisable(disabled);
            toggleImage.setDisable(disabled); // Có thể disable cả icon mắt
        }
    }
}
