package com.pma.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.pma.model.entity.Patient;
import com.pma.model.entity.UserAccount;
import com.pma.model.enums.Gender;
import com.pma.model.enums.UserRole;
import com.pma.service.EmailService;
import com.pma.service.PatientService;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil; // Import EmailService
import com.pma.util.UIManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

@Component
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private UIManager uiManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService; // Inject EmailService
    @FXML
    private VBox registerFormContainer; // VBox cha chứa các trường đăng ký chính

    // UserAccount fields
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField textPasswordField;
    @FXML
    private ImageView toggleImagePassword;
    @FXML
    private PasswordField reEnterPasswordField;
    @FXML
    private TextField textReEnterPasswordField;
    @FXML
    private ImageView toggleImageReEnterPassword;
    @FXML
    private TextField emailField;

    // Patient fields
    @FXML
    private TextField fullNameField;
    @FXML
    private DatePicker dateOfBirthPicker;
    @FXML
    private ToggleGroup genderGroup;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private TextField addressField1;
    @FXML
    private TextField addressField2;
    @FXML
    private TextField cityField;
    @FXML
    private TextField stateProvinceField;
    @FXML
    private TextField postalCodeField;
    @FXML
    private TextField countryField;
    @FXML
    private TextField insuranceNumberField;
    // @FXML private TextField identityNumberField; // Bỏ comment nếu có
    @FXML
    private TextField emergencyContactNameField;
    @FXML
    private TextField emergencyContactPhoneField;
    // @FXML private TextField relationshipField; // Bỏ comment nếu có
    @FXML
    private TextField bloodTypeField;
    @FXML
    private TextField allergiesField;
    @FXML
    private TextArea pastMedicalHistoryArea;
    @FXML
    private TextArea chronicDiseasesArea;

    @FXML
    private CheckBox enable2FACheckBox;
    @FXML
    private Button submitButton;
    @FXML
    private Label errorLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    // --- FXML Fields for OTP Verification ---
    @FXML
    private StackPane otpVerificationPane;
    @FXML
    private TextField otpField;
    @FXML
    private Button verifyOtpButton;
    @FXML
    private Button resendOtpButton;
    @FXML
    private Label errorLabelOtp;
    @FXML
    private ProgressIndicator progressIndicatorOtp;

    private boolean isPasswordVisible = false;
    private boolean isReEnterPasswordVisible = false;

    private Image eyeOpenImage;
    private Image eyeClosedImage;

    // Store pending data until OTP verification
    private UserAccount pendingUserAccountData;
    private Patient pendingPatientData;
    private String pendingOtpHash;
    private LocalDateTime pendingOtpExpiry;

    private Image loadImage(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            log.error("Failed to load image: {}", path, e);
            return null;
        }
    }

    @FXML
    public void initialize() {
        eyeOpenImage = loadImage("/com/pma/img/open.png");
        eyeClosedImage = loadImage("/com/pma/img/closed.png");

        clearError();
        hideProgress();

        // Ban đầu ẩn form OTP
        if (otpVerificationPane != null) {
            otpVerificationPane.setVisible(false);
            otpVerificationPane.setManaged(false);
        }
        hideOtpProgress();

        setupPasswordToggle(passwordField, textPasswordField, toggleImagePassword, isPasswordVisible);
        if (toggleImagePassword != null) {
            toggleImagePassword.setCursor(Cursor.HAND);
        }

        setupPasswordToggle(reEnterPasswordField, textReEnterPasswordField, toggleImageReEnterPassword, isReEnterPasswordVisible);
        if (toggleImageReEnterPassword != null) {
            toggleImageReEnterPassword.setCursor(Cursor.HAND);
        }
    }

    private void setupPasswordToggle(PasswordField pf, TextField tf, ImageView iv, boolean initiallyVisible) {
        if (pf == null || tf == null || iv == null) {
            log.warn("One or more FXML elements for password toggle are null. Check fx:id assignments.");
            return;
        }
        tf.setManaged(initiallyVisible);
        tf.setVisible(initiallyVisible);
        pf.setManaged(!initiallyVisible);
        pf.setVisible(!initiallyVisible);
        iv.setImage(initiallyVisible ? eyeOpenImage : eyeClosedImage);
        // tf.textProperty().bindBidirectional(pf.textProperty()); // Cẩn thận với binding này
    }

    @FXML
    private void handleSubmitAndRegisterAction(ActionEvent event) {
        clearError();

        String username = usernameField.getText().trim();
        String password = isPasswordVisible ? textPasswordField.getText() : passwordField.getText();
        String confirmPassword = isReEnterPasswordVisible ? textReEnterPasswordField.getText() : reEnterPasswordField.getText();
        String email = emailField.getText().trim();

        String patientFullName = fullNameField.getText().trim();
        LocalDate patientDob = dateOfBirthPicker.getValue();
        RadioButton selectedGenderRadio = genderGroup.getSelectedToggle() != null ? (RadioButton) genderGroup.getSelectedToggle() : null;
        String patientPhoneNumber = phoneNumberField.getText().trim();
        String patientAddress1 = addressField1.getText().trim();
        String patientAddress2 = (addressField2 != null) ? addressField2.getText().trim() : "";
        String patientCity = (cityField != null) ? cityField.getText().trim() : "";
        String patientStateProvince = (stateProvinceField != null) ? stateProvinceField.getText().trim() : "";
        String patientPostalCode = (postalCodeField != null) ? postalCodeField.getText().trim() : "";
        String patientCountry = (countryField != null) ? countryField.getText().trim() : "";
        String patientInsuranceNumber = insuranceNumberField.getText().trim();
        String patientEmergencyContactName = emergencyContactNameField.getText().trim();
        String patientEmergencyContactPhone = emergencyContactPhoneField.getText().trim();
        String patientBloodType = bloodTypeField.getText().trim();
        String patientAllergies = allergiesField.getText().trim();
        String patientPastMedicalHistory = pastMedicalHistoryArea.getText().trim();
        String patientChronicDiseases = chronicDiseasesArea.getText().trim();

        // --- VALIDATION ---
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.isEmpty()) {
            showError("Username, passwords, and email cannot be empty.");
            return;
        }
        if (username.length() < 4) {
            showError("Username must be at least 4 characters long.");
            return;
        }
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!password.matches(passwordRegex)) {
            showError("Password: 8+ chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char (@$!%*?&).");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showError("Please enter a valid email address.");
            return;
        }
        String phoneRegex = "^\\+?[0-9\\s\\-\\(\\)]{7,20}$";
        if (!patientPhoneNumber.matches(phoneRegex)) {
            showError("Please enter a valid phone number (7-20 digits).");
            return;
        }
        if (patientFullName.isEmpty() || patientDob == null || selectedGenderRadio == null || patientAddress1.isEmpty()) {
            showError("Patient's full name, DOB, gender, phone, and address line 1 are required.");
            return;
        }

        showProgress();
        setFormDisabled(true);

        // Create in-memory entities first
        UserAccount tempUserAccount = new UserAccount();
        tempUserAccount.setUsername(username);
        tempUserAccount.setPasswordHash(password); // Raw password, service will hash
        tempUserAccount.setRole(UserRole.Patient);
        tempUserAccount.setEmailVerified(false); // Will be true after OTP
        tempUserAccount.setTwoFactorEnabled(false); // Can be enabled later
        tempUserAccount.setActive(true); // Activate immediately, email verification is separate

        Patient tempPatient = new Patient();
        tempPatient.setFullName(patientFullName);
        tempPatient.setDateOfBirth(patientDob);
        String genderValue = selectedGenderRadio.getText();
        tempPatient.setGender(genderValue.equalsIgnoreCase("Male") ? Gender.MALE
                : (genderValue.equalsIgnoreCase("FEMALE") ? Gender.FEMALE : Gender.OTHER));
        tempPatient.setPhone(patientPhoneNumber);
        tempPatient.setEmail(email); // Use the validated email
        tempPatient.setAddressLine1(patientAddress1);
        if (patientAddress2 != null && !patientAddress2.isEmpty()) {
            tempPatient.setAddressLine2(patientAddress2);
        }
        tempPatient.setCity(patientCity.isEmpty() ? null : patientCity);
        tempPatient.setStateProvince(patientStateProvince.isEmpty() ? null : patientStateProvince);
        tempPatient.setPostalCode(patientPostalCode.isEmpty() ? null : patientPostalCode);
        tempPatient.setCountry(patientCountry.isEmpty() ? null : patientCountry);
        tempPatient.setInsuranceNumber(patientInsuranceNumber);
        tempPatient.setEmergencyContactName(patientEmergencyContactName);
        tempPatient.setEmergencyContactPhone(patientEmergencyContactPhone);
        tempPatient.setBloodType(patientBloodType.isEmpty() ? null : patientBloodType);
        tempPatient.setAllergies(patientAllergies);
        String combinedMedicalHistory = patientPastMedicalHistory;
        if (patientChronicDiseases != null && !patientChronicDiseases.isEmpty()) {
            combinedMedicalHistory += (combinedMedicalHistory.isEmpty() ? "" : "\n") + "Chronic Diseases: "
                    + patientChronicDiseases;
        }
        tempPatient.setMedicalHistory(combinedMedicalHistory);

        // Generate OTP
        String rawOtp = userAccountService.generateOtpString();
        String hashedOtp = passwordEncoder.encode(rawOtp);
        LocalDateTime otpExpiry = LocalDateTime.now().plus(UserAccountService.EMAIL_OTP_VALIDITY_DURATION);

        // Attempt to send OTP in a background thread, but wait for its result
        Thread otpSendingThread = new Thread(() -> {
            try {
                Future<Void> emailFuture = emailService.sendOtpEmail(email, username, rawOtp);
                emailFuture.get(20, TimeUnit.SECONDS); // Wait for email sending, with timeout

                // Email sent successfully (or at least no immediate error)
                // Store pending data
                this.pendingUserAccountData = tempUserAccount;
                this.pendingPatientData = tempPatient;
                this.pendingOtpHash = hashedOtp;
                this.pendingOtpExpiry = otpExpiry;

                log.info("OTP send request successful for email: {}", email);
                Platform.runLater(() -> {
                    hideProgress();
                    // Keep form disabled as we are moving to OTP screen
                    // setFormDisabled(false);
                    DialogUtil.showInfoAlert("Registration Pending",
                            "An OTP has been sent to " + email
                            + ". Please verify your email to complete registration.");
                    switchToOtpVerificationView();
                });

            } catch (InterruptedException | ExecutionException | TimeoutException emailEx) {
                Throwable cause = (emailEx instanceof ExecutionException) ? emailEx.getCause() : emailEx;
                log.error("Failed to send OTP email during registration for user '{}': {}", username,
                        cause.getMessage());
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("Could not send verification OTP: " + cause.getMessage()
                            + ". Please check your email or try again.");
                });
            } catch (Exception e) {
                log.error("An unexpected error occurred during registration for user '{}'", username, e);
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError("An unexpected error occurred. Please try again.");
                });
            }
        });
        otpSendingThread.setDaemon(true);
        otpSendingThread.start();
    }

    private void switchToOtpVerificationView() {
        if (registerFormContainer != null && otpVerificationPane != null) {
            registerFormContainer.setVisible(false);
            registerFormContainer.setManaged(false);
            otpVerificationPane.setVisible(true);
            otpVerificationPane.setManaged(true);
            clearOtpError();
            if (otpField != null) {
                otpField.clear();
                Platform.runLater(otpField::requestFocus);
            }
        } else {
            log.error("Cannot switch to OTP view: registerFormContainer or otpVerificationPane is null. Check FXML fx:id.");
            showError("UI Error: Cannot proceed to OTP verification step.");
        }
    }

    @FXML
    private void handleOtpVerificationAction(ActionEvent event) {
        clearOtpError();
        String otp = (otpField != null) ? otpField.getText().trim() : "";

        if (otp.isEmpty()) {
            showOtpError("Please enter the OTP code from your email.");
            return;
        }
        if (pendingUserAccountData == null || pendingPatientData == null || pendingOtpHash == null) {
            showOtpError("Registration session expired or data missing. Please start over.");
            // Optionally switch back to registration form
            // Platform.runLater(this::switchToRegistrationFormView); // You'd need to implement this
            return;
        }

        if (LocalDateTime.now().isAfter(pendingOtpExpiry)) {
            showOtpError("OTP has expired. Please resend OTP.");
            this.pendingOtpHash = null; // Invalidate old OTP
            return;
        }

        showOtpProgress();
        setOtpFormDisabled(true);

        Thread otpVerificationThread = new Thread(() -> {
            if (passwordEncoder.matches(otp, this.pendingOtpHash)) {
                // OTP is valid, proceed to save data
                try {
                    // Finalize user account details
                    this.pendingUserAccountData.setEmailVerified(true);
                    // OTP hash is not saved to DB for this flow, it was for verification only

                    UserAccount createdUserAccount = userAccountService.createUserAccount(this.pendingUserAccountData);
                    log.info("UserAccount created for {} with ID: {}", createdUserAccount.getUsername(), createdUserAccount.getUserId());

                    Patient registeredPatient = patientService.registerPatient(this.pendingPatientData);
                    log.info("Patient profile created for {} with ID: {}", registeredPatient.getFullName(), registeredPatient.getPatientId());

                    userAccountService.linkPatientToUserAccount(createdUserAccount.getUserId(), registeredPatient.getPatientId());
                    log.info("UserAccount {} linked with Patient {}", createdUserAccount.getUserId(), registeredPatient.getPatientId());

                    // Clear pending data as it's now saved
                    this.pendingUserAccountData = null;
                    this.pendingPatientData = null;
                    this.pendingOtpHash = null;
                    this.pendingOtpExpiry = null;

                    Platform.runLater(() -> {
                        hideOtpProgress();
                        // setOtpFormDisabled(false); // Form will be closed
                        DialogUtil.showSuccessAlert("Email Verified", "Your email has been successfully verified.");

                        boolean is2FASelected = enable2FACheckBox.isSelected();
                        if (is2FASelected) {
                            log.info("2FA was selected for user {}. Opening 2FA setup dialog.", createdUserAccount.getUsername());
                            boolean twoFaSuccessfullySetup = uiManager.show2FASetupDialog(createdUserAccount.getUserId());
                            if (twoFaSuccessfullySetup) {
                                DialogUtil.showInfoAlert("2FA Setup Successful", "Two-Factor Authentication has been enabled for your account.");
                            } else {
                                DialogUtil.showWarningAlert("2FA Setup Incomplete",
                                        "2FA setup was cancelled or not completed. "
                                        + "Your account is created, but 2FA is not active. "
                                        + "You can enable it later from your profile settings.");
                            }
                        }
                        if (uiManager != null) {
                            uiManager.switchToLoginScreen();
                        }
                    });

                } catch (IllegalArgumentException dbEx) { // Catch specific errors from service layers
                    log.error("Error saving registration data after OTP verification for {}: {}",
                            (this.pendingUserAccountData != null ? this.pendingUserAccountData.getUsername() : "N/A"), dbEx.getMessage());
                    Platform.runLater(() -> {
                        hideOtpProgress();
                        setOtpFormDisabled(false);
                        showOtpError("Error completing registration: " + dbEx.getMessage() + ". Please try again or contact support.");
                    });
                } catch (Exception e) { // Catch all other unexpected errors during DB operations
                    log.error("Unexpected error during saving data post-OTP verification for user '{}'",
                            (this.pendingUserAccountData != null ? this.pendingUserAccountData.getUsername() : "N/A"), e);
                    Platform.runLater(() -> {
                        hideOtpProgress();
                        setOtpFormDisabled(false);
                        showOtpError("An unexpected error occurred while saving your data. Please try again.");
                    });
                }
            } else {
                // Invalid OTP
                log.warn("Invalid OTP entered for user: {}",
                        (this.pendingUserAccountData != null ? this.pendingUserAccountData.getUsername() : "unknown"));
                Platform.runLater(() -> {
                    hideOtpProgress();
                    setOtpFormDisabled(false);
                    showOtpError("Invalid or expired OTP. Please try again or resend.");
                });
            }
        });
        otpVerificationThread.setDaemon(true);
        otpVerificationThread.start();
    }

    @FXML
    private void handleResendOtpAction(ActionEvent event) {
        if (pendingUserAccountData == null || pendingPatientData == null || pendingPatientData.getEmail() == null) {
            showOtpError("No pending registration to resend OTP for.");
            return;
        }
        clearOtpError();
        showOtpProgress();
        setOtpFormDisabled(true);

        // Generate new OTP
        String newRawOtp = userAccountService.generateOtpString();
        String newHashedOtp = passwordEncoder.encode(newRawOtp);
        LocalDateTime newOtpExpiry = LocalDateTime.now().plus(UserAccountService.EMAIL_OTP_VALIDITY_DURATION);

        Thread resendOtpThread = new Thread(() -> {
            try {
                Future<Void> emailFuture = emailService.sendOtpEmail(
                        pendingPatientData.getEmail(), pendingUserAccountData.getUsername(), newRawOtp);
                emailFuture.get(20, TimeUnit.SECONDS); // Wait for email sending

                // Update pending OTP info
                this.pendingOtpHash = newHashedOtp;
                this.pendingOtpExpiry = newOtpExpiry;

                Platform.runLater(() -> {
                    DialogUtil.showInfoAlert("OTP Resent", "A new OTP has been sent to your email.");
                });
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Throwable cause = (e instanceof ExecutionException) ? e.getCause() : e;
                log.error("Failed to resend OTP for user {}: {}", pendingUserAccountData.getUsername(), cause.getMessage(), cause);
                Platform.runLater(() -> showOtpError("Failed to resend OTP: " + cause.getMessage() + ". Please try again later."));
            } finally {
                Platform.runLater(() -> {
                    hideOtpProgress();
                    setOtpFormDisabled(false);
                });
            }
        });
        resendOtpThread.setDaemon(true);
        resendOtpThread.start();
    }

    @FXML
    private void togglePasswordVisibility(MouseEvent event) {
        isPasswordVisible = !isPasswordVisible;
        updateFieldVisibility(passwordField, textPasswordField, toggleImagePassword, isPasswordVisible);
    }

    @FXML
    private void toggleReEnterPasswordVisibility(MouseEvent event) {
        isReEnterPasswordVisible = !isReEnterPasswordVisible;
        updateFieldVisibility(reEnterPasswordField, textReEnterPasswordField, toggleImageReEnterPassword, isReEnterPasswordVisible);
    }

    private void updateFieldVisibility(PasswordField pf, TextField tf, ImageView iv, boolean makeTextVisible) {
        if (pf == null || tf == null || iv == null) {
            return;
        }
        Image newImage = makeTextVisible ? eyeOpenImage : eyeClosedImage;
        if (newImage != null) {
            iv.setImage(newImage);
        } else {
            log.warn("Eye image is null for password toggle.");
        }

        if (makeTextVisible) {
            tf.setText(pf.getText());
            log.debug("Password visibility toggled. TF set from PF. PF text: '{}', TF text: '{}'", pf.getText(), tf.getText());
            tf.setVisible(true);
            tf.setManaged(true);
            pf.setVisible(false);
            pf.setManaged(false);
            Platform.runLater(tf::requestFocus);
            tf.positionCaret(tf.getText().length());
        } else {
            pf.setText(tf.getText());
            pf.setVisible(true);
            pf.setManaged(true);
            tf.setVisible(false);
            tf.setManaged(false);
            Platform.runLater(pf::requestFocus);
            pf.positionCaret(pf.getText().length());
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            DialogUtil.showErrorAlert("Registration Error", message);
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
        if (submitButton != null) {
            submitButton.setDisable(true);
        }
    }

    private void hideProgress() {
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
        if (submitButton != null) {
            submitButton.setDisable(false);
        }
    }

    private void setFormDisabled(boolean disabled) {
        if (registerFormContainer != null) {
            registerFormContainer.setDisable(disabled);
        }
        boolean progressVisible = (progressIndicator != null && progressIndicator.isVisible());
        if (submitButton != null) {
            submitButton.setDisable(disabled || progressVisible);
        }
    }

    private void showOtpError(String message) {
        if (errorLabelOtp != null) {
            errorLabelOtp.setText(message);
            errorLabelOtp.setVisible(true);
        } else {
            DialogUtil.showErrorAlert("OTP Error", message);
        }
    }

    private void clearOtpError() {
        if (errorLabelOtp != null) {
            errorLabelOtp.setText("");
            errorLabelOtp.setVisible(false);
        }
    }

    private void showOtpProgress() {
        if (progressIndicatorOtp != null) {
            progressIndicatorOtp.setVisible(true);
        }
        if (verifyOtpButton != null) {
            verifyOtpButton.setDisable(true);
        }
        if (resendOtpButton != null) {
            resendOtpButton.setDisable(true);
        }
    }

    private void hideOtpProgress() {
        if (progressIndicatorOtp != null) {
            progressIndicatorOtp.setVisible(false);
        }
        if (verifyOtpButton != null) {
            verifyOtpButton.setDisable(false);
        }
        if (resendOtpButton != null) {
            resendOtpButton.setDisable(false);
        }
    }

    private void setOtpFormDisabled(boolean disabled) {
        if (otpVerificationPane != null) {
            otpVerificationPane.setDisable(disabled);
        }
        boolean progressVisible = (progressIndicatorOtp != null && progressIndicatorOtp.isVisible());
        if (verifyOtpButton != null) {
            verifyOtpButton.setDisable(disabled || progressVisible);
        }
        if (resendOtpButton != null) {
            resendOtpButton.setDisable(disabled || progressVisible);
        }
    }

    @FXML
    private void switchToLoginScreen() {
        if (uiManager != null) {
            uiManager.switchToLoginScreen();
        } else {
            log.warn("UIManager is null, cannot switch to login screen.");
            // Optionally show an error to the user
            DialogUtil.showErrorAlert("Navigation Error", "Cannot navigate to login screen.");
        }
    }
}
