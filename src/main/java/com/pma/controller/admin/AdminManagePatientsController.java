package com.pma.controller.admin;

import com.pma.model.entity.Patient; // Thay thế bằng Patient entity/DTO thực tế của bạn
import com.pma.model.enums.Gender;
import com.pma.service.PatientService; // Giả sử bạn có một PatientService
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;

import jakarta.persistence.EntityNotFoundException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminManagePatientsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(AdminManagePatientsController.class);

    private final UIManager uiManager;
    private final PatientService patientService; // Inject service của bạn
    // private final DialogUtil dialogUtil; // DialogUtil will be used statically

    @FXML
    private VBox sidebar;
    @FXML
    private Button adminViewRevenueButton;
    @FXML
    private Button adminManageDoctorsButton;
    @FXML
    private Button adminManagePatientsButton;
    @FXML
    private Button adminManageDepartmentsButton;
    @FXML
    private Button adminManageMedicinesButton;
    @FXML
    private Button adminManageUserAccountsButton;
    @FXML
    private Button adminManageDiseasesButton;

    // Form Fields
    @FXML
    private TextField fullNameField1;
    @FXML
    private DatePicker dateOfBirthPicker1;
    @FXML
    private ComboBox<String> genderCombo1;
    @FXML
    private TextField phoneField1;
    @FXML
    private TextField emailField1;
    @FXML
    private TextArea addressLine1Field1;
    @FXML
    private TextArea addressLine2Field1;
    @FXML
    private TextField cityField1;
    @FXML
    private TextField postalCodeField1;
    @FXML
    private TextField countryField1;
    @FXML
    private ComboBox<String> bloodTypeCombo1;
    @FXML
    private TextArea allergiesField1;
    @FXML
    private TextArea medicalHistoryField1;
    @FXML
    private TextField insuranceNumberField1;
    @FXML
    private TextField emergencyContactNameField1;
    @FXML
    private TextField emergencyContactPhoneField1;

    // Action Buttons
    @FXML
    private Button addButton1;
    @FXML
    private Button updateButton1;
    @FXML
    private Button deleteButton1;
    @FXML
    private Button clearButton1;

    // TableView and Columns
    @FXML
    private TableView<Patient> patientsTable;
    @FXML
    private TableColumn<Patient, String> fullNameColumn;
    @FXML
    private TableColumn<Patient, LocalDate> dateOfBirthColumn;
    @FXML
    private TableColumn<Patient, String> genderColumn;
    @FXML
    private TableColumn<Patient, String> phoneColumn;
    @FXML
    private TableColumn<Patient, String> emailColumn;
    @FXML
    private TableColumn<Patient, String> addressLine1Column;
    @FXML
    private TableColumn<Patient, String> addressLine2Column;
    @FXML
    private TableColumn<Patient, String> cityColumn;
    @FXML
    private TableColumn<Patient, String> stateProvinceColumn;
    @FXML
    private TableColumn<Patient, String> postalCodeColumn;
    @FXML
    private TableColumn<Patient, String> countryColumn;
    @FXML
    private TableColumn<Patient, String> bloodTypeColumn;
    @FXML
    private TableColumn<Patient, String> allergiesColumn;
    @FXML
    private TableColumn<Patient, String> medicalHistoryColumn;
    @FXML
    private TableColumn<Patient, String> insuranceNumberColumn;
    @FXML
    private TableColumn<Patient, String> emergencyContactNameColumn;
    @FXML
    private TableColumn<Patient, String> emergencyContactPhoneColumn;
    @FXML
    private TableColumn<Patient, LocalDateTime> createdAtColumn;
    @FXML
    private TableColumn<Patient, LocalDateTime> updatedAtColumn;

    private ObservableList<Patient> patientList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        loadPatientsData();
        // Thêm listener cho TableView để điền form khi chọn một hàng
        patientsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> populateForm(newValue)
        );

        // Các ComboBox đã được định nghĩa item trong FXML, nếu cần load từ DB thì làm ở đây
        // genderCombo1.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        // bloodTypeCombo1.setItems(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"));
    }

    private void setupTableColumns() {
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        dateOfBirthColumn.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        addressLine1Column.setCellValueFactory(new PropertyValueFactory<>("addressLine1"));
        addressLine2Column.setCellValueFactory(new PropertyValueFactory<>("addressLine2"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        stateProvinceColumn.setCellValueFactory(new PropertyValueFactory<>("stateProvince"));
        postalCodeColumn.setCellValueFactory(new PropertyValueFactory<>("postalCode"));
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("country"));
        bloodTypeColumn.setCellValueFactory(new PropertyValueFactory<>("bloodType"));
        allergiesColumn.setCellValueFactory(new PropertyValueFactory<>("allergies"));
        medicalHistoryColumn.setCellValueFactory(new PropertyValueFactory<>("medicalHistory"));
        insuranceNumberColumn.setCellValueFactory(new PropertyValueFactory<>("insuranceNumber"));
        emergencyContactNameColumn.setCellValueFactory(new PropertyValueFactory<>("emergencyContactName"));
        emergencyContactPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("emergencyContactPhone"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        patientsTable.setItems(patientList);
    }

    private void loadPatientsData() {
        log.info("Đang tải dữ liệu bệnh nhân...");

        javafx.concurrent.Task<List<Patient>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Patient> call() throws Exception {
                return patientService.getAllPatients(); // Chạy trên luồng nền
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<Patient> patients = loadTask.getValue();
            patientList.setAll(patients);
            log.info("Đã tải {} bệnh nhân.", patients.size());
        });

        loadTask.setOnFailed(event -> {
            Throwable e = loadTask.getException();
            log.error("Lỗi khi tải dữ liệu bệnh nhân: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi tải dữ liệu",
                    "Không thể tải danh sách bệnh nhân. Vui lòng thử lại.");
            patientList.clear();
        });

        // Start the background task
        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true); // Set as daemon thread
        loadThread.start();
    }

    private void populateForm(Patient patient) {
        if (patient != null) {
            fullNameField1.setText(patient.getFullName() != null ? patient.getFullName() : "");
            dateOfBirthPicker1.setValue(patient.getDateOfBirth());
            genderCombo1.setValue(patient.getGender() != null ? patient.getGender().name() : null);
            phoneField1.setText(patient.getPhone());
            emailField1.setText(patient.getEmail());
            addressLine1Field1.setText(patient.getAddressLine1());
            addressLine2Field1.setText(patient.getAddressLine2());
            cityField1.setText(patient.getCity());
            postalCodeField1.setText(patient.getPostalCode() != null ? patient.getPostalCode() : "");
            countryField1.setText(patient.getCountry() != null ? patient.getCountry() : "");
            bloodTypeCombo1.setValue(patient.getBloodType());
            allergiesField1.setText(patient.getAllergies() != null ? patient.getAllergies() : "");
            medicalHistoryField1.setText(patient.getMedicalHistory() != null ? patient.getMedicalHistory() : "");
            insuranceNumberField1.setText(patient.getInsuranceNumber() != null ? patient.getInsuranceNumber() : "");
            emergencyContactNameField1.setText(patient.getEmergencyContactName() != null ? patient.getEmergencyContactName() : "");
            emergencyContactPhoneField1.setText(patient.getEmergencyContactPhone() != null ? patient.getEmergencyContactPhone() : "");
        } else {
            clearForm(null); // Hoặc clearForm();
        }
    }

    @FXML
    private void addPatient(ActionEvent event) {
        log.info("Nút Thêm Bệnh nhân được nhấn");
        if (!validateInput()) {
            return;
        }

        Patient newPatient = new Patient();
        setPatientFromForm(newPatient);

        // Tạo username và mật khẩu ngẫu nhiên cho tài khoản mới
        String username = newPatient.getEmail(); // Sử dụng email làm username
        if (username == null || username.trim().isEmpty()) {
            DialogUtil.showErrorAlert("Lỗi dữ liệu", "Email bệnh nhân không được để trống để tạo tài khoản.");
            log.warn("Thêm bệnh nhân thất bại: Email trống, không thể tạo username.");
            return;
        }
        String rawPassword = generateRandomPassword(12); // Tạo mật khẩu ngẫu nhiên 12 ký tự

        try {
            // Gọi service để tạo bệnh nhân, tài khoản và gửi email
            Patient registeredPatient = patientService.createPatientWithAccountAndSendCredentials(newPatient, username, rawPassword);

            log.info("Đã thêm bệnh nhân mới: {}", registeredPatient.getFullName());
            // Thêm trực tiếp bệnh nhân vừa đăng ký vào ObservableList
            // Điều này hiệu quả hơn là tải lại toàn bộ danh sách
            if (registeredPatient != null) { // Đảm bảo service trả về đối tượng hợp lệ
                patientList.add(0, registeredPatient); // Thêm vào đầu danh sách để dễ thấy
                patientsTable.getSelectionModel().select(registeredPatient); // Tùy chọn: chọn hàng vừa thêm
            }
            DialogUtil.showSuccessAlert("Thành công", "Đã thêm bệnh nhân mới thành công.");
            clearForm(null);
        } catch (IllegalArgumentException e) {
            log.warn("Lỗi khi thêm bệnh nhân: {}", e.getMessage());
            DialogUtil.showErrorAlert("Lỗi dữ liệu", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi thêm bệnh nhân: {}", e.getMessage(), e);
            DialogUtil.showExceptionDialog("Lỗi hệ thống", "Không thể thêm bệnh nhân.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    private void updatePatient(ActionEvent event) {
        log.info("Nút Cập nhật Bệnh nhân được nhấn");
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            DialogUtil.showWarningAlert("Cảnh báo", "Vui lòng chọn một bệnh nhân để cập nhật.");
            return;
        }

        if (!validateInput()) {
            return;
        }

        // Tạo một đối tượng Patient mới từ form để truyền vào service
        // Hoặc cập nhật trực tiếp selectedPatient rồi truyền nó (tùy thiết kế service)
        Patient updatedDetails = new Patient(); // Tạo đối tượng mới để chứa thông tin cập nhật
        setPatientFromForm(updatedDetails);

        try {
            Patient updatedPatient = patientService.updatePatientDetails(selectedPatient.getPatientId(), updatedDetails);
            log.info("Đã cập nhật bệnh nhân: {}", updatedPatient.getFullName());
            // Cập nhật item trong ObservableList
            // Cách này yêu cầu Patient phải có equals() và hashCode() được implement đúng (thường dựa trên ID)
            // để indexOf hoạt động chính xác.
            int index = patientList.indexOf(selectedPatient);
            if (index != -1) {
                patientList.set(index, updatedPatient);
                patientsTable.getSelectionModel().select(updatedPatient); // Tùy chọn: chọn lại hàng vừa sửa
            } else {
                // Nếu không tìm thấy (hiếm khi xảy ra nếu selectedPatient thực sự từ list),
                // hoặc nếu equals/hashCode không được implement, tải lại toàn bộ là một giải pháp dự phòng.
                log.warn("Không tìm thấy bệnh nhân đã chọn trong danh sách để cập nhật. Tải lại toàn bộ danh sách.");
                loadPatientsData();
            }
            DialogUtil.showSuccessAlert("Thành công", "Đã cập nhật thông tin bệnh nhân thành công.");
            clearForm(null);
        } catch (EntityNotFoundException e) {
            log.error("Lỗi cập nhật: Không tìm thấy bệnh nhân với ID {}", selectedPatient.getPatientId());
            DialogUtil.showErrorAlert("Lỗi", "Không tìm thấy bệnh nhân để cập nhật. Có thể bệnh nhân đã bị xóa.");
            loadPatientsData(); // Tải lại để đồng bộ
        } catch (IllegalArgumentException e) {
            log.warn("Lỗi khi cập nhật bệnh nhân: {}", e.getMessage());
            DialogUtil.showErrorAlert("Lỗi dữ liệu", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi cập nhật bệnh nhân: {}", e.getMessage(), e);
            DialogUtil.showExceptionDialog("Lỗi hệ thống", "Không thể cập nhật bệnh nhân.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    private void deletePatient(ActionEvent event) {
        log.info("Nút Xóa Bệnh nhân được nhấn");
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            DialogUtil.showWarningAlert("Cảnh báo", "Vui lòng chọn một bệnh nhân để xóa.");
            return;
        }
        boolean confirmed = DialogUtil.showConfirmation("Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa bệnh nhân '" + selectedPatient.getFullName() + "' không? "
                + "Hành động này không thể hoàn tác và sẽ xóa tất cả dữ liệu liên quan (lịch hẹn, hồ sơ bệnh án,...).");
        if (confirmed) {
            try {
                patientService.deletePatient(selectedPatient.getPatientId());
                log.info("Đã xóa bệnh nhân: {}", selectedPatient.getFullName());
                patientList.remove(selectedPatient); // Xóa khỏi ObservableList
                DialogUtil.showSuccessAlert("Thành công", "Đã xóa bệnh nhân thành công.");
                clearForm(null);
            } catch (EntityNotFoundException e) {
                log.error("Lỗi xóa: Không tìm thấy bệnh nhân với ID {}", selectedPatient.getPatientId());
                DialogUtil.showErrorAlert("Lỗi", "Không tìm thấy bệnh nhân để xóa. Có thể bệnh nhân đã bị xóa.");
                loadPatientsData(); // Tải lại để đồng bộ
            } catch (IllegalStateException | DataIntegrityViolationException e) {
                log.warn("Lỗi khi xóa bệnh nhân {}: {}", selectedPatient.getPatientId(), e.getMessage());
                DialogUtil.showErrorAlert("Không thể xóa", "Không thể xóa bệnh nhân do có dữ liệu liên quan hoặc ràng buộc hệ thống. " + e.getMessage());
            } catch (Exception e) {
                log.error("Lỗi không mong muốn khi xóa bệnh nhân: {}", e.getMessage(), e);
                DialogUtil.showExceptionDialog("Lỗi hệ thống", "Không thể xóa bệnh nhân.", "Vui lòng thử lại sau.", e);
            }
        }
    }

    private void setPatientFromForm(Patient patient) {
        patient.setFullName(fullNameField1.getText() != null ? fullNameField1.getText().trim() : "");
        patient.setDateOfBirth(dateOfBirthPicker1.getValue());
        String genderString = genderCombo1.getValue();
        if (genderString != null) {
            try {
                patient.setGender(Gender.valueOf(genderString.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Giá trị gender không hợp lệ từ ComboBox: {}", genderString);
                patient.setGender(null); // Hoặc xử lý lỗi
            }
        } else {
            patient.setGender(null);
        }
        patient.setPhone(phoneField1.getText() != null ? phoneField1.getText().trim() : "");
        patient.setEmail(emailField1.getText() != null ? emailField1.getText().trim() : "");
        patient.setAddressLine1(addressLine1Field1.getText() != null ? addressLine1Field1.getText().trim() : "");
        patient.setAddressLine2(addressLine2Field1.getText() != null ? addressLine2Field1.getText().trim() : "");
        patient.setCity(cityField1.getText() != null ? cityField1.getText().trim() : "");
        patient.setPostalCode(postalCodeField1.getText() != null ? postalCodeField1.getText().trim() : "");
        patient.setCountry(countryField1.getText() != null ? countryField1.getText().trim() : "");
        patient.setBloodType(bloodTypeCombo1.getValue());
        patient.setAllergies(allergiesField1.getText() != null ? allergiesField1.getText().trim() : "");
        patient.setMedicalHistory(medicalHistoryField1.getText() != null ? medicalHistoryField1.getText().trim() : "");
        patient.setInsuranceNumber(insuranceNumberField1.getText() != null ? insuranceNumberField1.getText().trim() : "");
        patient.setEmergencyContactName(emergencyContactNameField1.getText() != null ? emergencyContactNameField1.getText().trim() : "");
        patient.setEmergencyContactPhone(emergencyContactPhoneField1.getText() != null ? emergencyContactPhoneField1.getText().trim() : "");
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if ((fullNameField1.getText() == null || fullNameField1.getText().trim().isEmpty())
                || dateOfBirthPicker1.getValue() == null
                || genderCombo1.getValue() == null
                || (phoneField1.getText() == null || phoneField1.getText().trim().isEmpty())
                || (emailField1.getText() == null || emailField1.getText().trim().isEmpty())) {
            errors.append("- Vui lòng điền đầy đủ các trường bắt buộc: Họ tên, Ngày sinh, Giới tính, Số điện thoại, Email.\n");
        }

        // Kiểm tra định dạng số điện thoại
        String phone = phoneField1.getText();
        if (phone != null && !phone.trim().isEmpty() && !phone.trim().matches("^0\\d{9,10}$")) {
            errors.append("- Số điện thoại không hợp lệ (ví dụ: 0912345678).\n");
        }

        // Kiểm tra định dạng email
        String email = emailField1.getText();
        if (email != null && !email.trim().isEmpty() && !email.trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            errors.append("- Email không hợp lệ.\n");
        }

        if (dateOfBirthPicker1.getValue() != null && dateOfBirthPicker1.getValue().isAfter(LocalDate.now())) {
            errors.append("- Ngày sinh không thể là một ngày trong tương lai.\n");
        }

        if (errors.length() > 0) {
            DialogUtil.showWarningAlert("Dữ liệu không hợp lệ", errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void clearForm(ActionEvent event) {
        log.info("Nút Xóa Form được nhấn");
        fullNameField1.clear();
        dateOfBirthPicker1.setValue(null);
        genderCombo1.getSelectionModel().clearSelection();
        phoneField1.clear();
        emailField1.clear();
        addressLine1Field1.clear();
        addressLine2Field1.clear();
        cityField1.clear();
        postalCodeField1.clear();
        countryField1.clear();
        bloodTypeCombo1.getSelectionModel().clearSelection();
        allergiesField1.clear();
        medicalHistoryField1.clear();
        insuranceNumberField1.clear();
        emergencyContactNameField1.clear();
        emergencyContactPhoneField1.clear();
        patientsTable.getSelectionModel().clearSelection();
    }

    // Sidebar Navigation Methods
    @FXML
    private void loadAdminViewRevenue(ActionEvent event) {
        uiManager.switchToAdminViewRevenue();
    }

    @FXML
    private void loadAdminManageDoctors(ActionEvent event) {
        uiManager.switchToAdminManageDoctors();
    }

    @FXML
    private void loadAdminManagePatients(ActionEvent event) {
        // Đã ở trang này rồi, có thể không cần làm gì hoặc refresh
        log.info("Đang ở trang Quản lý Bệnh nhân.");
        uiManager.switchToAdminManagePatients(); // Nếu muốn reload
    }

    @FXML
    private void loadAdminManageDepartments(ActionEvent event) {
        uiManager.switchToAdminManageDepartments();
    }

    @FXML
    private void loadAdminManageMedicines(ActionEvent event) {
        uiManager.switchToAdminManageMedicines();
    }

    @FXML
    private void loadAdminManageUserAccounts(ActionEvent event) {
        uiManager.switchToAdminManageUserAccounts();
    }

    @FXML
    private void loadAdminManageDiseases(ActionEvent event) {
        uiManager.switchToAdminManageDiseases();
    }

    /**
     * Tạo một mật khẩu ngẫu nhiên.
     * @param length Độ dài của mật khẩu.
     * @return Mật khẩu ngẫu nhiên.
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
