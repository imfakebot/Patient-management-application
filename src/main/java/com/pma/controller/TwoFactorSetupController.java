package com.pma.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.UUID;

@Component
public class TwoFactorSetupController {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorSetupController.class);

    private static final int QR_CODE_WIDTH = 200;
    private static final int QR_CODE_HEIGHT = 200;

    @FXML private VBox setupFormContainer;
    @FXML private ImageView qrCodeImageView;
    @FXML private Label secretKeyLabel;
    @FXML private TextField otpCodeField;
    @FXML private Button verifyAndEnableButton;
    @FXML private Button cancelButton;
    @FXML private Label errorLabelSetup;
    @FXML private ProgressIndicator progressIndicatorSetup;

    @Autowired
    private UserAccountService userAccountService;

    private UUID userId;
    private String generatedSecret;
    private boolean setupSuccessful = false;

    @FXML
    public void initialize() {
        hideProgress();
        clearError();
        otpCodeField.setOnAction(this::handleVerifyAndEnableAction); // Allow submitting with Enter key
    }

    public void initData(UUID userId) {
        this.userId = userId;
        generateAndDisplayQrCode();
    }

    /**
     * Generates a new 2FA secret and QR code for the user and displays them.
     * This operation is performed on a background thread to avoid blocking the UI.
     */
    private void generateAndDisplayQrCode() {
        showProgress();
        setFormDisabled(true);

        new Thread(() -> {
            try {
                UserAccountService.TwoFactorSecretAndQrData data = userAccountService.generateNewTwoFactorSecretAndQrData(userId);
                TwoFactorSetupController.this.generatedSecret = data.getSecret();
                String qrCodeUrl = data.getQrCodeData();
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
                BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
                Image qrImage = SwingFXUtils.toFXImage(bufferedImage, null);
                Platform.runLater(() -> {
                    qrCodeImageView.setImage(qrImage);
                    secretKeyLabel.setText(generatedSecret);
                    secretKeyLabel.setWrapText(true);
                    hideProgress();
                    setFormDisabled(false);
                });
            }catch (WriterException e) { // Specific exception for QR code generation issues
                log.error("Error generating QR code for user {}: {}", userId, e.getMessage());
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("Could not generate QR Code. Please try again.");
                    if (verifyAndEnableButton != null) verifyAndEnableButton.setDisable(true);
                });
            }
        }).start();
    }

    @FXML
    private void handleVerifyAndEnableAction(ActionEvent event) {
        if (userId == null || generatedSecret == null || generatedSecret.isEmpty()) {
            showError("Cannot verify OTP. Secret key not generated. Please try reloading the setup.");
            return;
        }
        String otp = otpCodeField.getText().trim();
        if (otp.isEmpty()) {
            showError("Please enter the OTP code from your authenticator app.");
            return;
        }
        clearError();
        showProgress();
        setFormDisabled(true);

        new Thread(() -> {
            try {
                boolean isValid = userAccountService.verifyAndEnableTwoFactor(userId, generatedSecret, otp);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    if (isValid) {
                        this.setupSuccessful = true;
                        DialogUtil.showInfoAlert("2FA Enabled", "Two-Factor Authentication has been successfully enabled for your account.");
                        closeDialog();
                    } else {
                        showError("Invalid OTP code. Please try again.");
                    } // Consider adding a mechanism to limit OTP verification attempts
                });
            } catch (Exception e) {
                log.error("Error verifying and enabling 2FA for user {}: {}", userId, e.getMessage());
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("An error occurred. " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleCancelAction(ActionEvent event) {
        this.setupSuccessful = false;
        closeDialog();
    }

    public boolean isSetupSuccessful() {
        return setupSuccessful;
    }

    private void closeDialog() {
        if (verifyAndEnableButton != null && verifyAndEnableButton.getScene() != null && verifyAndEnableButton.getScene().getWindow() instanceof Stage) {
            Stage stage = (Stage) verifyAndEnableButton.getScene().getWindow();
            stage.close();
        }
    }

    // --- UI Helper Methods ---
    private void showError(String message) {
        if (errorLabelSetup != null) {
            errorLabelSetup.setText(message);
            errorLabelSetup.setVisible(true);
        } else {
            DialogUtil.showErrorAlert("Setup Error", message);
        }
    }

    private void clearError() {
        if (errorLabelSetup != null) {
            errorLabelSetup.setText("");
            errorLabelSetup.setVisible(false);
        }
    }

    private void showProgress() {
        if (progressIndicatorSetup != null) progressIndicatorSetup.setVisible(true);
        // Disable buttons when progress is shown
        if (verifyAndEnableButton != null) verifyAndEnableButton.setDisable(true);
        if (cancelButton != null) cancelButton.setDisable(true);
    }

    private void hideProgress() {
        if (progressIndicatorSetup != null) progressIndicatorSetup.setVisible(false);
        // Re-enable buttons when progress is hidden, unless form is meant to be disabled
        boolean formShouldBeDisabled = (setupFormContainer != null && setupFormContainer.isDisabled());
        if (verifyAndEnableButton != null) verifyAndEnableButton.setDisable(formShouldBeDisabled);
        if (cancelButton != null) cancelButton.setDisable(formShouldBeDisabled);
    }

    private void setFormDisabled(boolean disabled) {
        if (setupFormContainer != null) {
            setupFormContainer.setDisable(disabled);
        } else { // Fallback if the main container is not available
            if (otpCodeField != null) otpCodeField.setDisable(disabled);
            // Ensure verifyAndEnableButton state considers progressIndicator visibility
            if (verifyAndEnableButton != null) verifyAndEnableButton.setDisable(disabled || (progressIndicatorSetup != null && progressIndicatorSetup.isVisible()));
            if (cancelButton != null) cancelButton.setDisable(disabled);
        }
    }
}
