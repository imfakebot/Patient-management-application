package com.pma.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

import java.io.InputStream;
import java.io.IOException;

@Component
public class ResetPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ResetPasswordController.class);

    @FXML
    private VBox resetPasswordFormContainer;
    @FXML
    private TextField usernameResetField; // Thêm trường username
    @FXML
    private TextField tokenField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private TextField textNewPasswordField;
    @FXML
    private ImageView toggleNewPasswordVisibility;
    @FXML
    private PasswordField confirmNewPasswordField;
    @FXML
    private TextField textConfirmNewPasswordField;
    @FXML
    private ImageView toggleConfirmNewPasswordVisibility;
    @FXML
    private Button resetPasswordButton;
    @FXML
    private Button backToLoginButtonReset;
    @FXML
    private Label errorLabelResetPass;
    @FXML
    private ProgressIndicator progressIndicatorResetPass;

    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private UIManager uiManager;

    private String usernameForReset; // Sẽ được set bởi UIManager
    private boolean newPasswordVisible = false;
    private boolean confirmNewPasswordVisible = false;
    private Image eyeOpenImage;
    private Image eyeClosedImage;

    private Image loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                log.error("Cannot load image: {}", path);
                return null;
            }
            return new Image(is);
        } catch (IOException e) {
            log.error("IOException while loading image: {}", path, e);
            return null;
        }
    }

    @FXML
    public void initialize() {
        eyeOpenImage = loadImage("/com/pma/img/open.png");
        eyeClosedImage = loadImage("/com/pma/img/closed.png");

        clearError();
        hideProgress();

        setupPasswordToggle(newPasswordField, textNewPasswordField, toggleNewPasswordVisibility, newPasswordVisible, this::toggleNewPasswordVisibilityAction);
        setupPasswordToggle(confirmNewPasswordField, textConfirmNewPasswordField, toggleConfirmNewPasswordVisibility, confirmNewPasswordVisible, this::toggleConfirmNewPasswordVisibilityAction);

        if (usernameForReset != null && !usernameForReset.isEmpty() && usernameResetField != null) {
            usernameResetField.setText(usernameForReset);
            usernameResetField.setDisable(true); // Nếu username được truyền vào, không cho sửa
        } else if (usernameResetField != null) {
            usernameResetField.setDisable(false); // Cho phép nhập nếu không được truyền
        }

        confirmNewPasswordField.setOnAction(this::handleResetPasswordAction);
        textConfirmNewPasswordField.setOnAction(this::handleResetPasswordAction);
    }

    public void initData(String username) {
        this.usernameForReset = username;
        if (username != null && usernameResetField != null) {
            usernameResetField.setText(username);
            usernameResetField.setDisable(true);
        } else if (usernameResetField != null) {
            usernameResetField.setDisable(false);
            usernameResetField.clear();
        }
        log.info("ResetPasswordController initialized for user: {}", username);
    }

    private void setupPasswordToggle(PasswordField pf, TextField tf, ImageView iv, boolean isInitiallyVisible, javafx.event.EventHandler<MouseEvent> handler) {
        if (pf == null || tf == null || iv == null) {
            log.warn("One or more FXML elements for a password toggle are null. Skipping setup.");
            return;
        }
        tf.setManaged(isInitiallyVisible);
        tf.setVisible(isInitiallyVisible);
        pf.setManaged(!isInitiallyVisible);
        pf.setVisible(!isInitiallyVisible);
        if (iv.getImage() == null && eyeClosedImage != null) { // Set default if not set by FXML
             iv.setImage(isInitiallyVisible ? eyeOpenImage : eyeClosedImage);
        }
        iv.setCursor(Cursor.HAND);
        iv.setOnMouseClicked(handler);
    }

    @FXML
    private void handleResetPasswordAction(ActionEvent event) {
        String token = tokenField.getText().trim();
        String newPassword = newPasswordVisible ? textNewPasswordField.getText() : newPasswordField.getText();
        String confirmPassword = confirmNewPasswordVisible ? textConfirmNewPasswordField.getText() : confirmNewPasswordField.getText();

        clearError();
        String usernameToUse;
        if (usernameResetField != null && !usernameResetField.getText().trim().isEmpty()) {
            usernameToUse = usernameResetField.getText().trim();
        } else if (usernameForReset != null && !usernameForReset.isEmpty()) {
            usernameToUse = usernameForReset;
        } else {
            showError("Username is required to reset password.");
            return;
        }

        if (usernameToUse.isEmpty()) {
            showError("Thông tin người dùng không hợp lệ. Vui lòng thử lại từ đầu hoặc nhập username.");
            return;
        }
        if (token.isEmpty()) {
            showError("Mã đặt lại không được để trống.");
            return;
        }
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Mật khẩu mới và xác nhận mật khẩu không được để trống.");
            return;
        }
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!newPassword.matches(passwordRegex)) {
            showError("Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số, và ký tự đặc biệt (@$!%*?&).");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showError("Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return;
        }

        showProgress();
        setFormDisabled(true);

        Thread resetThread = new Thread(() -> {
            boolean success = userAccountService.resetPasswordWithToken(usernameToUse, token, newPassword);
            Platform.runLater(() -> {
                hideProgress();
                if (success) {
                    DialogUtil.showSuccessAlert("Thành công", "Mật khẩu của bạn đã được đặt lại thành công. Vui lòng đăng nhập bằng mật khẩu mới.");
                    uiManager.switchToLoginScreen();
                } else {
                    setFormDisabled(false);
                    showError("Mã đặt lại không hợp lệ, đã hết hạn hoặc có lỗi xảy ra. Vui lòng thử lại hoặc yêu cầu mã mới.");
                }
            });
        });
        resetThread.setDaemon(true);
        resetThread.start();
    }

    @FXML
    private void handleBackToLoginResetAction(ActionEvent event) {
        uiManager.switchToLoginScreen();
    }

    @FXML
    private void toggleNewPasswordVisibilityAction(MouseEvent event) {
        newPasswordVisible = !newPasswordVisible;
        updateFieldVisibility(newPasswordField, textNewPasswordField, toggleNewPasswordVisibility, newPasswordVisible, eyeOpenImage, eyeClosedImage);
    }

    @FXML
    private void toggleConfirmNewPasswordVisibilityAction(MouseEvent event) {
        confirmNewPasswordVisible = !confirmNewPasswordVisible;
        updateFieldVisibility(confirmNewPasswordField, textConfirmNewPasswordField, toggleConfirmNewPasswordVisibility, confirmNewPasswordVisible, eyeOpenImage, eyeClosedImage);
    }

    private void updateFieldVisibility(PasswordField pf, TextField tf, ImageView iv, boolean makeTextVisible, Image openImg, Image closedImg) {
        if (pf == null || tf == null || iv == null) return;
        Image newImage = makeTextVisible ? openImg : closedImg;
        if (newImage != null) iv.setImage(newImage);

        if (makeTextVisible) {
            tf.setText(pf.getText());
            tf.setVisible(true); tf.setManaged(true);
            pf.setVisible(false); pf.setManaged(false);
            Platform.runLater(() -> {
                tf.requestFocus();
                tf.positionCaret(tf.getText().length());
            });
        } else {
            pf.setText(tf.getText());
            pf.setVisible(true); pf.setManaged(true);
            tf.setVisible(false); tf.setManaged(false);
            Platform.runLater(() -> {
                pf.requestFocus();
                pf.positionCaret(pf.getText().length());
            });
        }
    }

    private void showError(String message) {
        if (errorLabelResetPass != null) {
            errorLabelResetPass.setText(message);
            errorLabelResetPass.setVisible(true);
        } else {
            DialogUtil.showErrorAlert("Lỗi", message);
        }
    }

    private void clearError() {
        if (errorLabelResetPass != null) {
            errorLabelResetPass.setText("");
            errorLabelResetPass.setVisible(false);
        }
    }

    private void showProgress() {
        if (progressIndicatorResetPass != null) {
            progressIndicatorResetPass.setVisible(true);
        }
    }

    private void hideProgress() {
        if (progressIndicatorResetPass != null) {
            progressIndicatorResetPass.setVisible(false);
        }
    }

    private void setFormDisabled(boolean disabled) {
        if (resetPasswordFormContainer != null) {
            resetPasswordFormContainer.setDisable(disabled);
        }
        // Consider if backToLoginButtonReset should always be enabled
        // if (backToLoginButtonReset != null) {
        //     backToLoginButtonReset.setDisable(disabled && (progressIndicatorResetPass != null && progressIndicatorResetPass.isVisible()));
        // }
    }
}
