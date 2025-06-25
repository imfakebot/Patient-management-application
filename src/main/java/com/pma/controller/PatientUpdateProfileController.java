// package com.pma.controller; // Đảm bảo đúng package

// import java.util.Arrays;
// import java.util.Set;
// import java.util.UUID;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import com.pma.model.entity.Patient;
// import com.pma.model.enums.Gender;
// import com.pma.service.PatientService;

// import jakarta.persistence.EntityNotFoundException;
// import jakarta.validation.ConstraintViolation;
// import jakarta.validation.ConstraintViolationException;
// import jakarta.validation.Validator;
// import javafx.event.ActionEvent;
// import javafx.fxml.FXML;
// import javafx.scene.control.Alert;
// import javafx.scene.control.Button;
// import javafx.scene.control.ComboBox;
// import javafx.scene.control.DatePicker;
// import javafx.scene.control.TextArea;
// import javafx.scene.control.TextField;
// import javafx.stage.Stage;

// /**
//  * Controller cho giao diện cập nhật thông tin hồ sơ bệnh nhân
//  * (patient_update_profile.fxml).
//  */
// @Controller
// public class PatientUpdateProfileController {

//     private static final Logger log = LoggerFactory.getLogger(PatientUpdateProfileController.class);

//     @Autowired
//     private PatientService patientService;

//     @Autowired
//     private Validator validator; // Để xác thực dữ liệu đầu vào

//     // Các trường FXML khớp với fx:id trong patient_update_profile.fxml
//     @FXML
//     private TextField fullNameField;

//     @FXML
//     private DatePicker dateOfBirthPicker;

//     @FXML
//     private ComboBox<String> genderCombo;

//     @FXML
//     private TextField phoneField;

//     @FXML
//     private TextField emailField;

//     @FXML
//     private TextField addressLine1Field;

//     @FXML
//     private TextField addressLine2Field;

//     @FXML
//     private TextField cityField;

//     @FXML
//     private TextField stateProvinceField;

//     @FXML
//     private TextField postalCodeField;

//     @FXML
//     private TextField countryField;

//     @FXML
//     private ComboBox<String> bloodTypeCombo;

//     @FXML
//     private TextArea allergiesField;

//     @FXML
//     private TextArea medicalHistoryField;

//     @FXML
//     private TextField emergencyContactNameField;

//     @FXML
//     private TextField emergencyContactPhoneField;

//     @FXML
//     private Button saveButton;

//     @FXML
//     private Button cancelButton;

//     private Patient currentPatient; // Lưu trữ bệnh nhân đang được chỉnh sửa
//     private UUID patientId; // ID của bệnh nhân (null nếu tạo mới)

//     /**
//      * Khởi tạo controller, thiết lập các giá trị mặc định cho giao diện.
//      */
//     @FXML
//     public void initialize() {
//         log.info("Initializing PatientController");
//         // Thiết lập các giá trị cho ComboBox giới tính
//         genderCombo.getItems().addAll(
//                 Arrays.stream(Gender.values())
//                         .map(Enum::name)
//                         .toList()
//         );
//     }

//     /**
//      * Thiết lập dữ liệu bệnh nhân để chỉnh sửa.
//      *
//      * @param patientId UUID của bệnh nhân cần chỉnh sửa, null nếu tạo mới.
//      */
//     public void setPatient(UUID patientId) {
//         this.patientId = patientId;
//         if (patientId != null) {
//             try {
//                 currentPatient = patientService.getPatientById(patientId);
//                 populateFields(); // Hiển thị thông tin bệnh nhân lên giao diện
//             } catch (EntityNotFoundException e) {
//                 log.error("Patient not found with id: {}", patientId, e);
//                 showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy bệnh nhân với ID: " + patientId);
//                 closeWindow();
//             }
//         } else {
//             currentPatient = new Patient(); // Tạo mới bệnh nhân
//         }
//     }

//     /**
//      * Điền thông tin bệnh nhân vào các trường giao diện.
//      */
//     private void populateFields() {
//         fullNameField.setText(currentPatient.getFullName());
//         dateOfBirthPicker.setValue(currentPatient.getDateOfBirth());
//         genderCombo.setValue(currentPatient.getGender() != null ? currentPatient.getGender().name() : null);
//         phoneField.setText(currentPatient.getPhone());
//         emailField.setText(currentPatient.getEmail());
//         addressLine1Field.setText(currentPatient.getAddressLine1());
//         addressLine2Field.setText(currentPatient.getAddressLine2());
//         cityField.setText(currentPatient.getCity());
//         stateProvinceField.setText(currentPatient.getStateProvince());
//         postalCodeField.setText(currentPatient.getPostalCode());
//         countryField.setText(currentPatient.getCountry());
//         bloodTypeCombo.setValue(currentPatient.getBloodType());
//         allergiesField.setText(currentPatient.getAllergies());
//         medicalHistoryField.setText(currentPatient.getMedicalHistory());
//         emergencyContactNameField.setText(currentPatient.getEmergencyContactName());
//         emergencyContactPhoneField.setText(currentPatient.getEmergencyContactPhone());
//     }

//     /**
//      * Xử lý sự kiện khi nhấn nút Save.
//      */
//     @FXML
//     private void handleSaveButton(ActionEvent event) {
//         try {
//             // Tạo hoặc cập nhật đối tượng Patient từ dữ liệu giao diện
//             Patient patientDetails = buildPatientFromFields();

//             // Xác thực dữ liệu
//             Set<ConstraintViolation<Patient>> violations = validator.validate(patientDetails);
//             if (!violations.isEmpty()) {
//                 StringBuilder errorMessage = new StringBuilder("Dữ liệu không hợp lệ:\n");
//                 for (ConstraintViolation<Patient> violation : violations) {
//                     errorMessage.append("- ").append(violation.getMessage()).append("\n");
//                 }
//                 showAlert(Alert.AlertType.ERROR, "Lỗi xác thực", errorMessage.toString());
//                 return;
//             }

//             // Lưu hoặc cập nhật bệnh nhân
//             if (patientId == null) {
//                 // Đăng ký bệnh nhân mới
//                 patientService.registerPatient(patientDetails);
//                 log.info("Registered new patient: {}", patientDetails.getFullName());
//                 showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đăng ký bệnh nhân mới thành công!");
//             } else {
//                 // Cập nhật bệnh nhân hiện tại
//                 patientService.updatePatientDetails(patientId, patientDetails);
//                 log.info("Updated patient with id: {}", patientId);
//                 showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật thông tin bệnh nhân thành công!");
//             }

//             // Đóng cửa sổ sau khi lưu thành công
//             closeWindow();

//         } catch (IllegalArgumentException e) {
//             log.warn("Failed to save patient: {}", e.getMessage());
//             showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
//         } catch (ConstraintViolationException e) {
//             log.warn("Validation failed: {}", e.getMessage());
//             showAlert(Alert.AlertType.ERROR, "Lỗi xác thực", "Dữ liệu không hợp lệ: " + e.getMessage());
//         } catch (Exception e) {
//             log.error("Unexpected error while saving patient", e);
//             showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã xảy ra lỗi không mong muốn: " + e.getMessage());
//         }
//     }

//     /**
//      * Xử lý sự kiện khi nhấn nút Cancel.
//      */
//     @FXML
//     private void handleCancelButton(ActionEvent event) {
//         log.info("User cancelled patient update/registration");
//         closeWindow();
//     }

//     /**
//      * Tạo đối tượng Patient từ dữ liệu trong các trường giao diện.
//      *
//      * @return Patient với các giá trị từ giao diện.
//      */
//     private Patient buildPatientFromFields() {
//         Patient patient = new Patient();
//         patient.setFullName(fullNameField.getText());
//         patient.setDateOfBirth(dateOfBirthPicker.getValue());
//         patient.setGender(genderCombo.getValue() != null ? Gender.valueOf(genderCombo.getValue()) : null);
//         patient.setPhone(phoneField.getText());
//         patient.setEmail(emailField.getText().isEmpty() ? null : emailField.getText());
//         patient.setAddressLine1(addressLine1Field.getText());
//         patient.setAddressLine2(addressLine2Field.getText());
//         patient.setCity(cityField.getText());
//         patient.setStateProvince(stateProvinceField.getText());
//         patient.setPostalCode(postalCodeField.getText());
//         patient.setCountry(countryField.getText());
//         patient.setBloodType(bloodTypeCombo.getValue());
//         patient.setAllergies(allergiesField.getText());
//         patient.setMedicalHistory(medicalHistoryField.getText());
//         patient.setEmergencyContactName(emergencyContactNameField.getText());
//         patient.setEmergencyContactPhone(emergencyContactPhoneField.getText());
//         return patient;
//     }

//     /**
//      * Hiển thị thông báo cho người dùng.
//      *
//      * @param type Loại thông báo (INFORMATION, ERROR, v.v.).
//      * @param title Tiêu đề thông báo.
//      * @param message Nội dung thông báo.
//      */
//     private void showAlert(Alert.AlertType type, String title, String message) {
//         Alert alert = new Alert(type);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }

//     /**
//      * Đóng cửa sổ hiện tại.
//      */
//     private void closeWindow() {
//         Stage stage = (Stage) saveButton.getScene().getWindow();
//         stage.close();
//     }
// }
