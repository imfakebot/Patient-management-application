package com.pma.controller.patient;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pma.model.entity.Patient;
import com.pma.model.enums.Gender;
import com.pma.service.PatientService;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

@Component
public class PatientUpdateProfileController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(PatientUpdateProfileController.class);
    private final PatientService patientService;
    private Patient currentPatient;

    @FXML
    private Button patientBookAppointmentButton;
    @FXML
    private Button patientViewPrescriptionsButton;
    @FXML
    private Button patientMedicalHistoryButton;
    @FXML
    private Button patientUpdateProfileButton;
    @FXML
    private Button patientReviewButton;
    @FXML
    private Button patientViewBillsButton;

    @FXML
    private TextField fullNameField;
    @FXML
    private DatePicker dateOfBirthPicker;
    @FXML
    private ComboBox<String> genderCombo;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextArea addressLine1Field;
    @FXML
    private TextArea addressLine2Field;
    @FXML
    private TextField cityField;
    @FXML
    private TextField postalCodeField;
    @FXML
    private TextField countryField;
    @FXML
    private ComboBox<String> bloodTypeCombo;
    @FXML
    private TextArea allergiesField;
    @FXML
    private TextArea medicalHistoryField;
    @FXML
    private TextField insuranceNumberField;
    @FXML
    private TextField emergencyContactNameField;
    @FXML
    private TextField emergencyContactPhoneField;
    @FXML
    private Button updateButton;
    @FXML
    private Button clearButton;

    public PatientUpdateProfileController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Autowired
    private UIManager uiManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComboBoxes();
    }

    /**
     * Khởi tạo dữ liệu cho controller với ID của bệnh nhân.
     *
     * @param patientId ID của bệnh nhân.
     */
    public void initData(UUID patientId) {
        try {
            this.currentPatient = patientService.getPatientById(patientId);
            populateForm(currentPatient);
        } catch (Exception e) {
            log.error("Lỗi khi tải dữ liệu bệnh nhân với ID: {}", patientId, e);
            DialogUtil.showErrorAlert("Lỗi", "Không thể tải thông tin bệnh nhân");
        }
    }

    private void setupComboBoxes() {
        genderCombo.getItems().addAll("Male", "Female", "Other");
        bloodTypeCombo.getItems().addAll("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-");
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

    @FXML
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
        if (currentPatient != null) {
            uiManager.switchToPatientBookAppointment(currentPatient.getPatientId());
        } else {
            log.warn("Cannot switch to Book Appointment screen because currentPatient is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
        }
    }

    @FXML
    private void loadPatientViewPrescriptions() {
        if (currentPatient != null) {
            uiManager.switchToPatientViewPrescriptions(currentPatient.getPatientId());
        } else {
            log.warn("Cannot switch to View Prescriptions screen because currentPatient is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
        }
    }

    @FXML
    private void loadPatientMedicalHistory() {
        if (currentPatient != null) {
            uiManager.switchToPatientMedicalHistory(currentPatient.getPatientId());
        } else {
            log.warn("Cannot switch to Medical History screen because currentPatient is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
        }
    }

    @FXML
    private void loadPatientUpdateProfile() {
        if (currentPatient != null) {
            uiManager.switchToPatientUpdateProfile(currentPatient.getPatientId());
        } else {
            log.warn("Cannot switch to Update Profile screen because currentPatient is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
        }
    }

    @FXML
    private void loadPatientViewBills() {
        if (currentPatient != null) {
            uiManager.switchToPatientViewBills(currentPatient.getPatientId());
        } else {
            log.warn("Cannot switch to View Bills screen because currentPatient is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
        }
    }
}
