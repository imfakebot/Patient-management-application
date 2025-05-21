package com.pma.controller;

import com.pma.model.entity.Patient;
import com.pma.model.entity.UserAccount;
import com.pma.model.enums.Gender;
import com.pma.model.enums.UserRole;
import com.pma.service.PatientService;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
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
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);
    // Đã có


    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private UIManager uiManager;

    @FXML
    private VBox registerFormContainer; // VBox cha chứa các trường, fx:id="registerFormContainer"

    // UserAccount fields
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField; // fx:id="passwordField"
    @FXML
    private TextField textPasswordField; // fx:id="textPasswordField"
    @FXML
    private ImageView toggleImagePassword; // fx:id="toggleImagePassword"
    @FXML
    private PasswordField reEnterPasswordField; // fx:id="reEnterPasswordField"
    @FXML
    private TextField textReEnterPasswordField; // fx:id="textReEnterPasswordField"
    @FXML
    private ImageView toggleImageReEnterPassword; // fx:id="toggleImageReEnterPassword"
    @FXML
    private TextField emailField;

    // Patient fields
    @FXML
    private TextField fullNameField;
    @FXML
    private DatePicker dateOfBirthPicker;
    @FXML
    private ToggleGroup genderGroup; // fx:id="genderGroup" cho ToggleGroup của RadioButtons
    @FXML
    private TextField phoneNumberField;
    @FXML
    private TextField addressField1; // Cho Address Line 1
    // addressField2 is for address_line2 in the database
    @FXML
    private TextField addressField2; // Cho Address Line 2 (nếu có, FXML có 2 trường Address)
    @FXML
    private TextField cityField; // fx:id="cityField"
    @FXML
    private TextField stateProvinceField; // fx:id="stateProvinceField"
    @FXML
    private TextField postalCodeField; // fx:id="postalCodeField"
    @FXML
    private TextField countryField; // fx:id="countryField"
    @FXML
    private TextField insuranceNumberField;
    @FXML
    private TextField identityNumberField; // Giả sử FXML có trường này, nếu không thì xóa
    @FXML
    private TextField emergencyContactNameField;
    @FXML
    private TextField emergencyContactPhoneField;
    @FXML
    private TextField relationshipField; // Giả sử FXML có trường này, nếu không thì xóa
    @FXML
    private TextField bloodTypeField;
    @FXML
    private TextField allergiesField; // FXML đang là TextField, nếu là TextArea thì đổi kiểu
    @FXML
    private TextArea pastMedicalHistoryArea;
    @FXML
    private TextArea chronicDiseasesArea;

    @FXML
    private CheckBox enable2FACheckBox; // fx:id="enable2FACheckBox"
    @FXML
    private Button submitButton; // Đổi tên từ registerButton để khớp FXML
    @FXML
    private Label errorLabel; // fx:id="errorLabel" (cần thêm vào FXML nếu chưa có)
    @FXML
    private ProgressIndicator progressIndicator; // fx:id="progressIndicator" (cần thêm vào FXML nếu chưa có)

    private boolean isPasswordVisible = false;
    private boolean isReEnterPasswordVisible = false;

    private Image eyeOpenImage;
    private Image eyeClosedImage;

    private Image loadImage(String path) {
        try {
            return new Image(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            log.error("Failed to load image: {}", path, e);
            // Consider returning a placeholder image or null and handling it
            return null;
        }
    }

    @FXML
    public void initialize() {
        eyeOpenImage = loadImage("/com/pma/img/open.png");
        eyeClosedImage = loadImage("/com/pma/img/closed.png");

        clearError();
        hideProgress();

        // Password field 1
        setupPasswordToggle(passwordField, textPasswordField, toggleImagePassword, isPasswordVisible);
        if (toggleImagePassword != null) {
            toggleImagePassword.setCursor(Cursor.HAND);
        }

        // Password field 2 (Re-enter)
        setupPasswordToggle(reEnterPasswordField, textReEnterPasswordField, toggleImageReEnterPassword, isReEnterPasswordVisible);
        if (toggleImageReEnterPassword != null) {
            toggleImageReEnterPassword.setCursor(Cursor.HAND);
        }

        // Add listeners to clear error on input change (optional)
        // usernameField.textProperty().addListener((obs, oldVal, newVal) -> clearError());
        // ...
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

        // Bind text properties if not already bound in FXML or elsewhere
        // This ensures text is synced if user types in one field then toggles
        // Be cautious if you have other bindings or listeners
        // tf.textProperty().bindBidirectional(pf.textProperty()); // Can cause issues if not handled carefully
    }

    @FXML
    private void handleSubmitAndRegisterAction(ActionEvent event) { // Đổi tên phương thức
        clearError();

        // Lấy thông tin UserAccount
        String username = usernameField.getText().trim();
        String password = isPasswordVisible ? textPasswordField.getText() : passwordField.getText();
        String confirmPassword = isReEnterPasswordVisible ? textReEnterPasswordField.getText() : reEnterPasswordField.getText();
        String email = emailField.getText().trim();

        // Lấy thông tin Patient
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
        // String patientIdentityNumber = (identityNumberField != null) ? identityNumberField.getText().trim() : ""; // Nếu có
        // String patientRelationship = (relationshipField != null) ? relationshipField.getText().trim() : ""; // Nếu có

        // --- VALIDATION ---
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.isEmpty()) {
            showError("Username, passwords, and email cannot be empty.");
            return;
        }
        if (username.length() < 4) {
            showError("Username must be at least 4 characters long.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters long.");
            return;
        }
        // Thêm kiểm tra độ phức tạp mật khẩu bằng regex
        // Ví dụ: Ít nhất 1 chữ hoa, 1 chữ thường, 1 số, 1 ký tự đặc biệt, và dài ít nhất 8 ký tự
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!password.matches(passwordRegex)) {
            showError("Password must be at least 8 characters long, include an uppercase letter, a lowercase letter, a digit, and a special character (@$!%*?&).");
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
        // Validate Phone Number
        String phoneRegex = "^\\+?[0-9\\s\\-\\(\\)]{7,20}$"; // Regex cho số điện thoại, từ 7 đến 20 ký tự số, có thể có +, (), -, khoảng trắng
        if (!patientPhoneNumber.matches(phoneRegex)) {
            showError("Please enter a valid phone number (7-20 digits, can include +, (), -, spaces).");
            return;
        }
        // Validate Patient fields
        if (patientFullName.isEmpty() || patientDob == null || selectedGenderRadio == null || patientPhoneNumber.isEmpty() || patientAddress1.isEmpty()) {
            showError("Patient's full name, date of birth, gender, phone number, and address line 1 are required.");
            return;
        }
        // Thêm các validation khác nếu cần (ví dụ: định dạng số điện thoại, số CMND/CCCD)

        showProgress();
        setFormDisabled(true);

        Thread registrationThread = new Thread(() -> {
            try {
                // 1. Tạo UserAccount
                UserAccount newUserAccount = new UserAccount();
                newUserAccount.setUsername(username);
                newUserAccount.setPasswordHash(password); // Service sẽ mã hóa
                newUserAccount.setRole(UserRole.Patient); // Mặc định vai trò là PATIENT khi tự đăng ký

                UserAccount createdUserAccount = userAccountService.createUserAccount(newUserAccount);
                UUID userId = createdUserAccount.getUserId();
                log.info("UserAccount created for {} with ID: {}", username, userId);

                // 2. Tạo Patient
                Patient newPatient = new Patient();
                newPatient.setFullName(patientFullName);
                newPatient.setDateOfBirth(patientDob);
                String genderValue = selectedGenderRadio.getText(); // "Male", "Female"
                newPatient.setGender(genderValue.equalsIgnoreCase("Male") ? Gender.MALE : (genderValue.equalsIgnoreCase("FEMALE") ? Gender.FEMALE : Gender.OTHER));
                newPatient.setPhone(patientPhoneNumber);
                newPatient.setEmail(email); // Có thể dùng email của UserAccount
                newPatient.setAddressLine1(patientAddress1);
                if (patientAddress2 != null && !patientAddress2.isEmpty()) {
                    newPatient.setAddressLine2(patientAddress2);
                }
                // Set new address fields
                newPatient.setCity(patientCity.isEmpty() ? null : patientCity);
                newPatient.setStateProvince(patientStateProvince.isEmpty() ? null : patientStateProvince);
                newPatient.setPostalCode(patientPostalCode.isEmpty() ? null : patientPostalCode);
                newPatient.setCountry(patientCountry.isEmpty() ? null : patientCountry);

                // newPatient.setNationalId(patientIdentityNumber); // Nếu Patient entity có trường này
                newPatient.setInsuranceNumber(patientInsuranceNumber);
                newPatient.setEmergencyContactName(patientEmergencyContactName);
                newPatient.setEmergencyContactPhone(patientEmergencyContactPhone);
                newPatient.setBloodType(patientBloodType.isEmpty() ? null : patientBloodType);
                newPatient.setAllergies(patientAllergies);
                String combinedMedicalHistory = patientPastMedicalHistory;
                if (patientChronicDiseases != null && !patientChronicDiseases.isEmpty()) {
                    combinedMedicalHistory += (combinedMedicalHistory.isEmpty() ? "" : "\n") + "Chronic Diseases: " + patientChronicDiseases;
                }
                newPatient.setMedicalHistory(combinedMedicalHistory);

                Patient registeredPatient = patientService.registerPatient(newPatient);
                UUID patientId = registeredPatient.getPatientId();
                log.info("Patient profile created for {} with ID: {}", patientFullName, patientId);

                // 3. Liên kết UserAccount với Patient
                userAccountService.linkPatientToUserAccount(userId, patientId);
                log.info("UserAccount {} linked with Patient {}", userId, patientId);

                // Final step: Handle 2FA setup if requested, then show success and navigate
                Platform.runLater(() -> {
                    boolean is2FASelected = enable2FACheckBox.isSelected();

                    if (is2FASelected) {
                        log.info("2FA selected for user {}. Opening 2FA setup dialog.", username);
                        // This dialog should be modal and handle the 2FA setup process.
                        // It will interact with UserAccountService to generate/save secret and enable 2FA.
                        // Assumes uiManager.show2FASetupDialog(userId) exists, is modal,
                        // and returns true if 2FA was successfully set up.
                        boolean twoFaSuccessfullySetup = uiManager.show2FASetupDialog(userId);

                        if (twoFaSuccessfullySetup) {
                            DialogUtil.showInfoAlert("2FA Setup Successful", "Two-Factor Authentication has been enabled for your account.");
                        } else {
                            DialogUtil.showWarningAlert("2FA Setup Incomplete",
                                    "Two-Factor Authentication setup was cancelled or not completed. "
                                    + "Your account is created, but 2FA is not active. "
                                    + "You can enable it later from your profile settings.");
                        }
                    }
                    // This runs after 2FA dialog (if any) is closed, or if 2FA was not selected
                    hideProgress(); // Also re-enables the submit button
                    setFormDisabled(false); // Explicitly re-enable form fields
                    DialogUtil.showSuccessAlert("Registration Successful", "Account and profile created for " + username + ". You can now log in.");
                    if (uiManager != null) {
                        uiManager.switchToLoginScreen();
                    } else {
                        log.warn("UIManager is null, cannot switch to login screen automatically.");
                    }
                });
            } catch (IllegalArgumentException e) {
                log.warn("Registration failed for username '{}': {}", username, e.getMessage());
                Platform.runLater(() -> {
                    hideProgress();
                    setFormDisabled(false);
                    showError(e.getMessage());
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
        registrationThread.setDaemon(true);
        registrationThread.start();
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
        if (newImage == null) { // Fallback if images didn't load
            log.warn("Eye image is null for password toggle.");
        } else {
            iv.setImage(newImage);
        }

        if (makeTextVisible) {
            tf.setText(pf.getText()); // Sync text before switching
            tf.setVisible(true);
            tf.setManaged(true);
            pf.setVisible(false);
            pf.setManaged(false);
            Platform.runLater(tf::requestFocus); // Request focus after visibility change
            tf.positionCaret(tf.getText().length());
        } else {
            pf.setText(tf.getText()); // Sync text before switching
            pf.setVisible(true);
            pf.setManaged(true);
            tf.setVisible(false);
            tf.setManaged(false);
            Platform.runLater(pf::requestFocus); // Request focus after visibility change
            pf.positionCaret(pf.getText().length());
        }
    }

    // --- UI Helper Methods ---
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            // Fallback if errorLabel is not defined in FXML or null
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
        } else {
            // Fallback if container is not set, disable individual known fields
            if (usernameField != null) {
                usernameField.setDisable(disabled);
            }
            if (passwordField != null) {
                passwordField.setDisable(disabled);
            }
            // ... disable other fields ...
        }
        // Ensure submit button state is consistent with progress and form disable state
        if (submitButton != null) {
            boolean progressVisible = (progressIndicator != null && progressIndicator.isVisible());
            submitButton.setDisable(disabled || progressVisible);
        }
    }

    // Optional: Add a method to navigate back to login if you add a "Back to Login" button
    /*
    @FXML
    private void handleBackToLoginAction(ActionEvent event) {
        if (uiManager != null) {
            uiManager.switchToLoginScreen();
        }
    }
     */
}
