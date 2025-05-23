package com.pma.controller;

import java.awt.image.BufferedImage;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

@Component
public class TwoFactorSetupController {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorSetupController.class);

    private static final int QR_CODE_WIDTH = 200;
    private static final int QR_CODE_HEIGHT = 200;

    @FXML
    private VBox setupFormContainer;
    @FXML
    private ImageView qrCodeImageView;
    @FXML
    private Label secretKeyLabel;
    @FXML
    private TextField otpCodeField;
    @FXML
    private Button verifyAndEnableButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label errorLabelSetup;
    @FXML
    private ProgressIndicator progressIndicatorSetup;

    @Autowired
    private UserAccountService userAccountService;

    private UUID userId;
    private String generatedSecret;
    private boolean setupSuccessful = false;

    @FXML
    public void initialize() {
        hideProgress();
        clearError(); // Initial state: no error, form is interactive
        setFormInteractive(true);
        otpCodeField.setOnAction(this::handleVerifyAndEnableAction); // Allow submitting with Enter key
    }

    public void initData(UUID userId) {
        this.userId = userId;
        generateAndDisplayQrCode();
    }

    /**
     * Generates a new 2FA secret and QR code for the user and displays them.
     * This operation is performed on a background thread to avoid blocking the
     * UI.
     */
    private void generateAndDisplayQrCode() {
        showProgress(); // This will also call setFormInteractive(false)

        Thread qrGenerationThread = new Thread(() -> {
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
                    hideProgress(); // Hide indicator
                    setFormInteractive(true); // Re-enable form controls
                });
            } catch (WriterException e) { // Specific exception for QR code generation issues
                log.error("Error generating QR code for user {}: {}", userId, e.getMessage());
                Platform.runLater(() -> {
                    hideProgress(); // Hide indicator
                    showError("Could not generate QR Code. Please try again.");
                    // Disable OTP field and verify button as QR generation failed, but keep cancel active
                    if (otpCodeField != null) {
                        otpCodeField.setDisable(true);
                    }
                    if (verifyAndEnableButton != null) {
                        verifyAndEnableButton.setDisable(true);
                    }
                    if (cancelButton != null) {
                        cancelButton.setDisable(false); // Ensure cancel is usable

                                    }});
            } catch (Exception e) { // Catch other potential errors from service or logic
                log.error("Unexpected error generating 2FA data for user {}: {}", userId, e.getMessage(), e);
                Platform.runLater(() -> {
                    hideProgress(); // Hide indicator
                    showError("An unexpected error occurred while generating QR data. Please try again or contact support.");
                    // Disable OTP field and verify button, but keep cancel active
                    if (otpCodeField != null) {
                        otpCodeField.setDisable(true);
                    }
                    if (verifyAndEnableButton != null) {
                        verifyAndEnableButton.setDisable(true);
                    }
                    if (cancelButton != null) {
                        cancelButton.setDisable(false); // Ensure cancel is usable

                                    }});
            }
        });
        qrGenerationThread.setDaemon(true); // Ensure thread doesn't prevent app shutdown
        qrGenerationThread.start();
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
        showProgress(); // This will also call setFormInteractive(false)

        Thread verificationThread = new Thread(() -> {
            try {
                boolean isValid = userAccountService.verifyAndEnableTwoFactor(userId, generatedSecret, otp);
                Platform.runLater(() -> {
                    if (isValid) {
                        this.setupSuccessful = true;
                        DialogUtil.showInfoAlert("2FA Enabled", "Two-Factor Authentication has been successfully enabled for your account.");
                        closeDialog(); // No need to hide progress or re-enable form if dialog closes
                    } else {
                        hideProgress(); // Hide indicator
                        setFormInteractive(true); // Re-enable form for another try
                        showError("Invalid OTP code. Please try again.");
                    } // Consider adding a mechanism to limit OTP verification attempts
                });
            } catch (Exception e) {
                log.error("Error verifying and enabling 2FA for user {}: {}", userId, e.getMessage(), e);
                Platform.runLater(() -> {
                    hideProgress(); // Hide indicator
                    setFormInteractive(true); // Re-enable form
                    showError("An error occurred. " + e.getMessage());
                });
            }
        });
        verificationThread.setDaemon(true); // Ensure thread doesn't prevent app shutdown
        verificationThread.start();
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
        if (progressIndicatorSetup != null) {
            progressIndicatorSetup.setVisible(true);
        }
        setFormInteractive(false); // Disable form controls when progress is shown
    }

    private void hideProgress() {
        if (progressIndicatorSetup != null) {
            progressIndicatorSetup.setVisible(false);
        }
        // Note: Re-enabling form controls is now handled explicitly where needed
        // by calling setFormInteractive(true) after hideProgress().
    }

    /**
     * Enables or disables interactive elements of the form.
     *
     * @param interactive true to enable, false to disable.
     */
    private void setFormInteractive(boolean interactive) {
        if (setupFormContainer != null) {
            // Disabling the container might be too broad if it contains non-interactive elements
            // that should remain visible/styled normally.
            // setupFormContainer.setDisable(!interactive);
        }

        if (otpCodeField != null) {
            otpCodeField.setDisable(!interactive);
        }
        if (verifyAndEnableButton != null) {
            verifyAndEnableButton.setDisable(!interactive);
        }
        // Cancel button might have different logic, e.g., always enabled
        // or only disabled when progress is very specifically blocking all actions.
        // For now, group it with other interactive controls.
        if (cancelButton != null) {
            cancelButton.setDisable(!interactive);
        }
    }
}
