package com.pma.controller.patient;

import com.pma.model.entity.Patient;
import com.pma.model.enums.Gender;
import com.pma.service.PatientService;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.UUID;

@Component
public class PatientUpdateProfileController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(PatientUpdateProfileController.class);

    private final PatientService patientService;
    private final UserAccountService userAccountService;
    private Patient currentPatient;

    @FXML private Button patientBookAppointmentButton;
    @FXML private Button patientViewPrescriptionsButton;
    @FXML private Button patientMedicalHistoryButton;
    @FXML private Button patientUpdateProfileButton;
    @FXML private Button patientReviewButton;
    @FXML private Button patientViewBillsButton;

    @FXML private TextField fullNameField;
    @FXML private DatePicker dateOfBirthPicker;
    @FXML private ComboBox<String> genderCombo;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextArea addressLine1Field;
    @FXML private TextArea addressLine2Field;
    @FXML private TextField cityField;
    @FXML private TextField postalCodeField;
    @FXML private TextField countryField;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private TextArea allergiesField;
    @FXML private TextArea medicalHistoryField;
    @FXML private TextField insuranceNumberField;
    @FXML private TextField emergencyContactNameField;
    @FXML private TextField emergencyContactPhoneField;
    @FXML private Button updateButton;
    @FXML private Button clearButton;

    public PatientUpdateProfileController(PatientService patientService, 
                                       UserAccountService userAccountService) {
        this.patientService = patientService;
        this.userAccountService = userAccountService;
    }

    @Autowired
    private UIManager uiManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComboBoxes();
        loadPatientData();
        setupEventHandlers();
    }

    private void setupComboBoxes() {
        genderCombo.getItems().addAll("Male", "Female", "Other");
        bloodTypeCombo.getItems().addAll("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-");
    }

    private void loadPatientData() {
        try {
            UUID patientId = getCurrentPatientId();
            if (patientId != null) {
                currentPatient = patientService.getPatientById(patientId);
                populateForm(currentPatient);
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải dữ liệu bệnh nhân", e);
            DialogUtil.showErrorAlert("Lỗi", "Không thể tải thông tin bệnh nhân");
        }
    }

    private UUID getCurrentPatientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String username = authentication.getName();
            return userAccountService.findByUsername(username)
                    .map(userAccount -> {
                        if (userAccount.getPatient() != null) {
                            return userAccount.getPatient().getPatientId();
                        }
                        return null;
                    })
                    .orElse(null);
        }
        return null;
    }

    private void populateForm(Patient patient) {
        fullNameField.setText(patient.getFullName());
        dateOfBirthPicker.setValue(patient.getDateOfBirth());
        genderCombo.setValue(patient.getGender().toString());
        phoneField.setText(patient.getPhone());
        emailField.setText(patient.getEmail());
        addressLine1Field.setText(patient.getAddressLine1());
        addressLine2Field.setText(patient.getAddressLine2());
        cityField.setText(patient.getCity());
        postalCodeField.setText(patient.getPostalCode());
        countryField.setText(patient.getCountry());
        bloodTypeCombo.setValue(patient.getBloodType());
        allergiesField.setText(patient.getAllergies());
        medicalHistoryField.setText(patient.getMedicalHistory());
        insuranceNumberField.setText(patient.getInsuranceNumber());
        emergencyContactNameField.setText(patient.getEmergencyContactName());
        emergencyContactPhoneField.setText(patient.getEmergencyContactPhone());
    }

    private void setupEventHandlers() {
        updateButton.setOnAction(event -> updateProfile());
        clearButton.setOnAction(event -> clearForm());
    }

    private void updateProfile() {
        if (!validateInput()) {
            return;
        }

        try {
            updatePatientFromForm(currentPatient);
            patientService.updatePatientDetails(currentPatient.getPatientId(), currentPatient);
            DialogUtil.showSuccessAlert("Thành công", "Cập nhật hồ sơ thành công");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật hồ sơ", e);
            DialogUtil.showErrorAlert("Lỗi", "Cập nhật thất bại: " + e.getMessage());
        }
    }


    private void updatePatientFromForm(Patient patient) {
        patient.setFullName(fullNameField.getText().trim());
        patient.setDateOfBirth(dateOfBirthPicker.getValue());
        
        String genderString = genderCombo.getValue();
        if (genderString != null) {
            patient.setGender(Gender.valueOf(genderString.toUpperCase()));
        }
        
        patient.setPhone(phoneField.getText().trim());
        patient.setEmail(emailField.getText().trim());
        patient.setAddressLine1(addressLine1Field.getText().trim());
        patient.setAddressLine2(addressLine2Field.getText().trim());
        patient.setCity(cityField.getText().trim());
        patient.setPostalCode(postalCodeField.getText().trim());
        patient.setCountry(countryField.getText().trim());
        patient.setBloodType(bloodTypeCombo.getValue());
        patient.setAllergies(allergiesField.getText().trim());
        patient.setMedicalHistory(medicalHistoryField.getText().trim());
        patient.setInsuranceNumber(insuranceNumberField.getText().trim());
        patient.setEmergencyContactName(emergencyContactNameField.getText().trim());
        patient.setEmergencyContactPhone(emergencyContactPhoneField.getText().trim());
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
            errors.append("- Họ và tên không được để trống\n");
        }
        
        if (dateOfBirthPicker.getValue() == null) {
            errors.append("- Ngày sinh không được để trống\n");
        } else if (dateOfBirthPicker.getValue().isAfter(LocalDate.now())) {
            errors.append("- Ngày sinh không thể là ngày trong tương lai\n");
        }
        
        if (genderCombo.getValue() == null) {
            errors.append("- Giới tính không được để trống\n");
        }
        
        if (phoneField.getText() == null || phoneField.getText().trim().isEmpty()) {
            errors.append("- Số điện thoại không được để trống\n");
        } else if (!phoneField.getText().trim().matches("^0\\d{9,10}$")) {
            errors.append("- Số điện thoại không hợp lệ (phải bắt đầu bằng 0 và có 10-11 số)\n");
        }
        
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            errors.append("- Email không được để trống\n");
        } else if (!emailField.getText().trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            errors.append("- Email không hợp lệ\n");
        }

        if (errors.length() > 0) {
            DialogUtil.showWarningAlert("Lỗi dữ liệu", errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void clearForm() {
        populateForm(currentPatient);
        fullNameField.requestFocus();
    }

    @FXML
    private void loadPatientBookAppointment() {
        uiManager.switchToPatientBookAppointment(null);
    }
    
    @FXML
    private void loadPatientViewPrescriptions() {
        uiManager.switchToPatientViewPrescriptions();
    }
    
    @FXML 
    private void loadPatientMedicalHistory() {
        uiManager.switchToPatientMedicalHistory();
    }

    @FXML
    private void loadPatientUpdateProfile() {
        uiManager.switchToPatientUpdateProfile();
    }
    @FXML 
    private void loadPatientReview() {
        uiManager.switchToPatientReview();
    }

    @FXML 
    private void loadPatientViewBills() {
        uiManager.switchToPatientViewBills();
    }
}