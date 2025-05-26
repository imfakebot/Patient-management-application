package com.pma.controller;

import com.pma.model.entity.Patient; // Thay thế bằng Patient entity/DTO thực tế của bạn
import com.pma.model.enums.Gender;
import com.pma.service.PatientService; // Giả sử bạn có một PatientService
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
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
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

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
        // Ví dụ: patientList.setAll(patientService.findAll());
        // Cần triển khai patientService để lấy dữ liệu
        log.info("Đang tải dữ liệu bệnh nhân...");
        // patientList.setAll(patientService.getAllPatients()); // Thay thế bằng lời gọi service thực tế
        // Để test, bạn có thể thêm dữ liệu mẫu:
        // patientList.add(new Patient("Nguyễn Văn A", LocalDate.of(1990, 1, 1), "Male", "0909123456", "vana@example.com"));
        // patientList.add(new Patient("Trần Thị B", LocalDate.of(1985, 5, 15), "Female", "0909654321", "thib@example.com"));
    }

    private void populateForm(Patient patient) {
        if (patient != null) {
            fullNameField1.setText(patient.getFullName());
            dateOfBirthPicker1.setValue(patient.getDateOfBirth());
            genderCombo1.setValue(patient.getGender() != null ? patient.getGender().name() : null);
            phoneField1.setText(patient.getPhone());
            emailField1.setText(patient.getEmail());
            addressLine1Field1.setText(patient.getAddressLine1());
            addressLine2Field1.setText(patient.getAddressLine2());
            cityField1.setText(patient.getCity());
            postalCodeField1.setText(patient.getPostalCode());
            countryField1.setText(patient.getCountry());
            bloodTypeCombo1.setValue(patient.getBloodType());
            allergiesField1.setText(patient.getAllergies());
            medicalHistoryField1.setText(patient.getMedicalHistory());
            insuranceNumberField1.setText(patient.getInsuranceNumber());
            emergencyContactNameField1.setText(patient.getEmergencyContactName());
            emergencyContactPhoneField1.setText(patient.getEmergencyContactPhone());
        } else {
            clearForm(null); // Hoặc clearForm();
        }
    }

    @FXML
    private void addPatient(ActionEvent event) {
        log.info("Nút Thêm Bệnh nhân được nhấn");
        // Lấy dữ liệu từ form
        // Tạo đối tượng Patient mới
        // Gọi patientService.save(newPatient);
        // loadPatientsData();
        // clearForm();
        DialogUtil.showInformation("Thông báo", "Chức năng Thêm Bệnh nhân chưa được triển khai.");
    }

    @FXML
    private void updatePatient(ActionEvent event) {
        log.info("Nút Cập nhật Bệnh nhân được nhấn");
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            DialogUtil.showWarningAlert("Cảnh báo", "Vui lòng chọn một bệnh nhân để cập nhật.");
            return;
        }
        // Cập nhật thông tin cho selectedPatient từ form
        // Gọi patientService.update(selectedPatient);
        // loadPatientsData();
        // clearForm();
        DialogUtil.showInformation("Thông báo", "Chức năng Cập nhật Bệnh nhân chưa được triển khai.");
    }

    @FXML
    private void deletePatient(ActionEvent event) {
        log.info("Nút Xóa Bệnh nhân được nhấn");
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            DialogUtil.showWarningAlert("Cảnh báo", "Vui lòng chọn một bệnh nhân để xóa.");
            return;
        }
        boolean confirmed = DialogUtil.showConfirmation("Xác nhận xóa", "Bạn có chắc chắn muốn xóa bệnh nhân này không?");
        if (confirmed) {
            // Gọi patientService.delete(selectedPatient.getId());
            // loadPatientsData();
            // clearForm();
            DialogUtil.showInformation("Thông báo", "Chức năng Xóa Bệnh nhân chưa được triển khai đầy đủ.");
        }
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
}
