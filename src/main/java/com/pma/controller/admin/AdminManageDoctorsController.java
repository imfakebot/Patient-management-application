package com.pma.controller.admin;

import com.pma.model.entity.Department;
import com.pma.model.entity.Doctor;
import com.pma.model.enums.DoctorStatus;
import com.pma.model.enums.Gender;
import com.pma.service.DepartmentService;
import com.pma.service.DoctorService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class AdminManageDoctorsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(AdminManageDoctorsController.class);

    private final UIManager uiManager;
    private final DoctorService doctorService;
    private final DepartmentService departmentService;

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
    private TextField specialtyField;
    @FXML
    private ComboBox<Department> departmentCombo;
    @FXML
    private TextField medicalLicenseField;
    @FXML
    private TextField yearsOfExperienceField;
    @FXML
    private TextField salaryField;
    @FXML
    private ComboBox<String> statusCombo;

    // Action Buttons
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;

    // Table
    @FXML
    private TableView<Doctor> doctorsTable;
    @FXML
    private TableColumn<Doctor, String> fullNameColumn;
    @FXML
    private TableColumn<Doctor, LocalDate> dateOfBirthColumn;
    @FXML
    private TableColumn<Doctor, String> genderColumn;
    @FXML
    private TableColumn<Doctor, String> phoneColumn;
    @FXML
    private TableColumn<Doctor, String> emailColumn;
    @FXML
    private TableColumn<Doctor, String> specialtyColumn;
    @FXML
    private TableColumn<Doctor, String> departmentColumn;
    @FXML
    private TableColumn<Doctor, String> medicalLicenseColumn;
    @FXML
    private TableColumn<Doctor, Integer> yearsOfExperienceColumn;
    @FXML
    private TableColumn<Doctor, BigDecimal> salaryColumn;
    @FXML
    private TableColumn<Doctor, String> statusColumn;
    @FXML
    private TableColumn<Doctor, LocalDateTime> createdAtColumn;
    @FXML
    private TableColumn<Doctor, LocalDateTime> updatedAtColumn;

    // Search Fields
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> searchCriteriaCombo;
    @FXML
    private Button searchButton;
    @FXML
    private Button showAllButton;

    private final ObservableList<Doctor> doctorObservableList = FXCollections.observableArrayList();
    private final ObservableList<Department> departmentObservableList = FXCollections.observableArrayList();
    // List to hold all doctors fetched from the database, used as a master list for searching
    private final ObservableList<Doctor> allDoctorsMasterList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing AdminManageDoctorsController");
        setupSidebar();
        setupFormControls();
        setupDoctorsTable();
        loadDepartmentsData();
        loadInitialDoctorsData(); // Load all doctors into master list first
        doctorObservableList.setAll(allDoctorsMasterList); // Initially display all doctors

        doctorsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        populateForm(newSelection);
                    } else {
                        clearForm(null);
                    }
                });

        // Setup search criteria
        searchCriteriaCombo.setItems(FXCollections.observableArrayList(
                "Theo Tên", "Theo Chuyên khoa", "Theo Khoa", "Theo Email", "Theo Số điện thoại"
        ));
        searchCriteriaCombo.setPromptText("Tiêu chí tìm kiếm");

        log.info("AdminManageDoctorsController initialized successfully");
    }

    private void setupSidebar() {
        // Optional: Highlight active button
        // adminManageDoctorsButton.getStyleClass().add("active-sidebar-button");
    }

    private void setupFormControls() {
        // Gender ComboBox items are set in FXML
        // Status ComboBox items are set in FXML

        // Department ComboBox
        departmentCombo.setItems(departmentObservableList);
        departmentCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Department department) {
                return department == null ? null : department.getName();
            }

            @Override
            public Department fromString(String string) {
                return departmentObservableList.stream()
                        .filter(d -> d.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void setupDoctorsTable() {
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        dateOfBirthColumn.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));
        genderColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGender() != null ? cellData.getValue().getGender().name() : ""));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        specialtyColumn.setCellValueFactory(new PropertyValueFactory<>("specialty"));
        departmentColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDepartment() != null ? cellData.getValue().getDepartment().getName() : ""));
        medicalLicenseColumn.setCellValueFactory(new PropertyValueFactory<>("medicalLicense"));
        yearsOfExperienceColumn.setCellValueFactory(new PropertyValueFactory<>("yearsOfExperience"));
        salaryColumn.setCellValueFactory(new PropertyValueFactory<>("salary"));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().name() : ""));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        doctorsTable.setItems(doctorObservableList);
    }

    private void loadDepartmentsData() {
        try {
            log.debug("Loading departments data...");
            // Giả sử DepartmentService có phương thức findAllActiveDepartments()
            // hoặc một phương thức tương tự để lấy danh sách khoa đang hoạt động.
            List<Department> departments = departmentService.getAllDepartments(); // Hoặc findAllActiveDepartments()
            if (departments != null) {
                departmentObservableList.setAll(departments);
                log.info("Loaded {} departments.", departments.size());
            } else {
                departmentObservableList.clear();
                log.warn("DepartmentService returned null for loading departments.");
            }
        } catch (Exception e) {
            log.error("Error loading departments data: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi tải Khoa", "Không thể tải danh sách khoa.");
            departmentObservableList.clear();
        }
    }

    private void loadInitialDoctorsData() {
        try {
            log.debug("Loading doctors data...");
            List<Doctor> doctors = doctorService.getAllDoctors(); // Giả sử DoctorService có phương thức này
            if (doctors != null) {
                allDoctorsMasterList.setAll(doctors);
                log.info("Loaded {} doctors.", doctors.size());
            } else {
                allDoctorsMasterList.clear();
                log.warn("DoctorService returned null for loading doctors.");
            }
        } catch (Exception e) {
            log.error("Error loading doctors data: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi tải Bác sĩ", "Không thể tải danh sách bác sĩ.");
            allDoctorsMasterList.clear();
        }
    }

    private void populateForm(Doctor doctor) {
        if (doctor == null) {
            clearForm(null);
            return;
        }
        fullNameField.setText(doctor.getFullName());
        dateOfBirthPicker.setValue(doctor.getDateOfBirth());
        genderCombo.setValue(doctor.getGender() != null ? doctor.getGender().name() : null);
        phoneField.setText(doctor.getPhone());
        emailField.setText(doctor.getEmail());
        specialtyField.setText(doctor.getSpecialty());
        departmentCombo.setValue(doctor.getDepartment());
        medicalLicenseField.setText(doctor.getMedicalLicense());
        yearsOfExperienceField.setText(doctor.getYearsOfExperience() != null ? String.valueOf(doctor.getYearsOfExperience()) : "");
        salaryField.setText(doctor.getSalary() != null ? doctor.getSalary().toPlainString() : "");
        statusCombo.setValue(doctor.getStatus() != null ? doctor.getStatus().name() : null);
    }

    @FXML
    private void clearForm(ActionEvent event) {
        fullNameField.clear();
        dateOfBirthPicker.setValue(null);
        genderCombo.getSelectionModel().clearSelection();
        phoneField.clear();
        emailField.clear();
        specialtyField.clear();
        departmentCombo.getSelectionModel().clearSelection();
        medicalLicenseField.clear();
        yearsOfExperienceField.clear();
        salaryField.clear();
        searchField.clear();
        searchCriteriaCombo.getSelectionModel().clearSelection();
        statusCombo.getSelectionModel().clearSelection();
        doctorsTable.getSelectionModel().clearSelection();
        log.debug("Form cleared.");
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
            errors.append("- Họ và tên không được để trống.\n");
        }
        if (dateOfBirthPicker.getValue() == null) {
            errors.append("- Ngày sinh không được để trống.\n");
        } else if (dateOfBirthPicker.getValue().isAfter(LocalDate.now())) {
            errors.append("- Ngày sinh không thể là một ngày trong tương lai.\n");
        }
        if (genderCombo.getValue() == null) {
            errors.append("- Giới tính không được để trống.\n");
        }

        // Phone validation
        String phone = phoneField.getText();
        if (phone == null || phone.trim().isEmpty()) {
            errors.append("- Số điện thoại không được để trống.\n");
        } else if (!phone.trim().matches("^0\\d{9,10}$")) { // Vietnamese phone number format: 0 followed by 9 or 10 digits
            errors.append("- Số điện thoại không hợp lệ (ví dụ: 0912345678).\n");
        }

        // Email validation
        String email = emailField.getText();
        if (email == null || email.trim().isEmpty()) {
            errors.append("- Email không được để trống.\n");
        } else if (!email.trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            errors.append("- Email không hợp lệ.\n");
        }

        if (specialtyField.getText() == null || specialtyField.getText().trim().isEmpty()) {
            errors.append("- Chuyên khoa không được để trống.\n");
        }
        if (departmentCombo.getValue() == null) {
            errors.append("- Khoa không được để trống.\n");
        }
        if (medicalLicenseField.getText() == null || medicalLicenseField.getText().trim().isEmpty()) {
            errors.append("- Giấy phép y tế không được để trống.\n");
        }

        // Years of Experience validation
        String yearsExpText = yearsOfExperienceField.getText();
        if (yearsExpText != null && !yearsExpText.trim().isEmpty()) {
            try {
                int years = Integer.parseInt(yearsExpText.trim());
                if (years < 0) {
                    errors.append("- Năm kinh nghiệm không thể là số âm.\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- Năm kinh nghiệm phải là một số nguyên.\n");
            }
        } // Optional: else { errors.append("- Năm kinh nghiệm không được để trống.\n"); } if it's mandatory

        // Salary validation
        String salaryText = salaryField.getText();
        if (salaryText != null && !salaryText.trim().isEmpty()) {
            try {
                BigDecimal salaryValue = new BigDecimal(salaryText.trim());
                if (salaryValue.compareTo(BigDecimal.ZERO) < 0) {
                    errors.append("- Lương không thể là số âm.\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- Lương phải là một số.\n");
            }
        } // Optional: else { errors.append("- Lương không được để trống.\n"); } if it's mandatory

        if (statusCombo.getValue() == null) {
            errors.append("- Trạng thái không được để trống.\n");
        }

        if (!errors.isEmpty()) {
            DialogUtil.showWarningAlert("Dữ liệu không hợp lệ", errors.toString());
            return false;
        }
        return true;
    }

    private Doctor setDoctorFromForm(Doctor doctor) {
        doctor.setFullName(fullNameField.getText().trim());
        doctor.setDateOfBirth(dateOfBirthPicker.getValue());
        doctor.setGender(Gender.valueOf(genderCombo.getValue().toUpperCase())); // Chuyển sang chữ hoa để khớp Enum
        doctor.setPhone(phoneField.getText().trim());
        doctor.setEmail(emailField.getText().trim());
        doctor.setSpecialty(specialtyField.getText().trim());
        doctor.setDepartment(departmentCombo.getValue());
        doctor.setMedicalLicense(medicalLicenseField.getText().trim());
        if (yearsOfExperienceField.getText() != null && !yearsOfExperienceField.getText().trim().isEmpty()) {
            doctor.setYearsOfExperience(Integer.parseInt(yearsOfExperienceField.getText().trim()));
        } else {
            doctor.setYearsOfExperience(null); // Hoặc 0 nếu đó là giá trị mặc định
        }
        if (salaryField.getText() != null && !salaryField.getText().trim().isEmpty()) {
            doctor.setSalary(new BigDecimal(salaryField.getText().trim()));
        } else {
            doctor.setSalary(null); // Hoặc BigDecimal.ZERO nếu đó là giá trị mặc định
        }
        doctor.setStatus(DoctorStatus.valueOf(statusCombo.getValue().toUpperCase())); // Chuyển sang chữ hoa
        return doctor;
    }

    @FXML
    private void addDoctor(ActionEvent event) {
        log.info("Add Doctor button clicked.");
        if (!validateInput()) {
            return;
        }
        Doctor newDoctor = new Doctor();
        setDoctorFromForm(newDoctor);

        try {
            // Giả sử DoctorService.createDoctor giờ nhận departmentId riêng
            // Hoặc DoctorService.addDoctor nhận Doctor đã có Department được set
            // Nếu DoctorService.addDoctor(Doctor doctor) thì department đã được set trong setDoctorFromForm
            Doctor savedDoctor = doctorService.createDoctor(newDoctor, newDoctor.getDepartment().getDepartmentId());
            doctorObservableList.add(savedDoctor);
            doctorsTable.getSelectionModel().select(savedDoctor);
            DialogUtil.showSuccessAlert("Thành công", "Đã thêm bác sĩ mới thành công.");
            clearForm(null); // Clear form after successful addition
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while adding doctor: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi Trùng lặp", "Không thể thêm bác sĩ. Email, số điện thoại hoặc giấy phép y tế có thể đã tồn tại.");
        } catch (Exception e) {
            log.error("Error adding doctor: {}", e.getMessage(), e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể thêm bác sĩ.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    private void updateDoctor(ActionEvent event) {
        log.info("Update Doctor button clicked.");
        Doctor selectedDoctor = doctorsTable.getSelectionModel().getSelectedItem();
        if (selectedDoctor == null) {
            DialogUtil.showWarningAlert("Chưa chọn Bác sĩ", "Vui lòng chọn một bác sĩ để cập nhật.");
            return;
        }
        if (!validateInput()) {
            return;
        }

        Doctor doctorToUpdate = new Doctor();
        // Giữ lại ID và CreatedAt từ selectedDoctor
        doctorToUpdate.setDoctorId(selectedDoctor.getDoctorId()); // Sửa từ setId thành setDoctorId
        doctorToUpdate.setCreatedAt(selectedDoctor.getCreatedAt());

        setDoctorFromForm(doctorToUpdate); // Điền thông tin từ form

        try {
            // Giả sử DoctorService.updateDoctor nhận ID, Doctor details, và newDepartmentId (nếu có)
            // Nếu department không thay đổi, có thể truyền null hoặc ID của department hiện tại.
            Doctor updatedDoctor = doctorService.updateDoctor(selectedDoctor.getDoctorId(), doctorToUpdate, doctorToUpdate.getDepartment().getDepartmentId());
            int index = doctorObservableList.indexOf(selectedDoctor);
            if (index != -1) {
                doctorObservableList.set(index, updatedDoctor);
                doctorsTable.getSelectionModel().select(updatedDoctor);
            } else {
                refreshDoctorsTable(); // Fallback nếu không tìm thấy (hiếm khi xảy ra)
            }
            DialogUtil.showSuccessAlert("Thành công", "Đã cập nhật thông tin bác sĩ thành công.");
            clearForm(null);
        } catch (EntityNotFoundException e) {
            log.error("Doctor not found for update: {}", selectedDoctor.getDoctorId(), e);
            DialogUtil.showErrorAlert("Không tìm thấy", "Không tìm thấy bác sĩ để cập nhật. Có thể đã bị xóa.");
            loadDoctorsData();
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating doctor: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi Trùng lặp", "Không thể cập nhật bác sĩ. Email, số điện thoại hoặc giấy phép y tế có thể đã tồn tại cho bác sĩ khác.");
        } catch (Exception e) {
            log.error("Error updating doctor: {}", e.getMessage(), e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể cập nhật bác sĩ.", "Vui lòng thử lại sau.", e);
        }
    }

    private void loadDoctorsData() {
        // This method is called as a fallback, for example, when an entity is not found during an update.
        // Refreshing the entire table and master list is a safe approach here.
        log.debug("loadDoctorsData() called, refreshing doctors table.");
        refreshDoctorsTable();
    }

    @FXML
    private void deleteDoctor(ActionEvent event) {
        log.info("Delete Doctor button clicked.");
        Doctor selectedDoctor = doctorsTable.getSelectionModel().getSelectedItem();
        if (selectedDoctor == null) {
            DialogUtil.showWarningAlert("Chưa chọn Bác sĩ", "Vui lòng chọn một bác sĩ để xóa.");
            return;
        }

        boolean confirmed = DialogUtil.showConfirmation("Xác nhận Xóa",
                "Bạn có chắc chắn muốn xóa bác sĩ '" + selectedDoctor.getFullName() + "' không? "
                + "Hành động này không thể hoàn tác.");
        if (confirmed) {
            try {
                doctorService.deleteDoctor(selectedDoctor.getDoctorId()); // Sửa từ getId thành getDoctorId
                doctorObservableList.remove(selectedDoctor);
                DialogUtil.showSuccessAlert("Thành công", "Đã xóa bác sĩ thành công.");
                clearForm(null);
            } catch (EntityNotFoundException e) {
                log.error("Doctor not found for deletion: {}", selectedDoctor.getDoctorId(), e);
                DialogUtil.showErrorAlert("Không tìm thấy", "Không tìm thấy bác sĩ để xóa. Có thể đã bị xóa.");
                refreshDoctorsTable();
            } catch (DataIntegrityViolationException e) {
                log.error("Cannot delete doctor due to existing references: {}", selectedDoctor.getDoctorId(), e);
                DialogUtil.showErrorAlert("Không thể Xóa", "Không thể xóa bác sĩ này do có các dữ liệu liên quan (ví dụ: lịch hẹn).");
            } catch (Exception e) {
                log.error("Error deleting doctor: {}", e.getMessage(), e);
                DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể xóa bác sĩ.", "Vui lòng thử lại sau.", e);
            }
        }
    }

    @FXML
    private void searchDoctors(ActionEvent event) {
        String keyword = searchField.getText().toLowerCase().trim();
        String criteria = searchCriteriaCombo.getValue();

        if (keyword.isEmpty() || criteria == null) {
            DialogUtil.showWarningAlert("Thiếu thông tin", "Vui lòng nhập từ khóa và chọn tiêu chí tìm kiếm.");
            doctorObservableList.setAll(allDoctorsMasterList); // Hiển thị lại tất cả nếu tìm kiếm rỗng
            return;
        }

        ObservableList<Doctor> filteredList = FXCollections.observableArrayList();
        for (Doctor doctor : allDoctorsMasterList) {
            boolean match = false;
            switch (criteria) {
                case "Theo Tên":
                    match = doctor.getFullName() != null && doctor.getFullName().toLowerCase().contains(keyword);
                    break;
                case "Theo Chuyên khoa":
                    match = doctor.getSpecialty() != null && doctor.getSpecialty().toLowerCase().contains(keyword);
                    break;
                case "Theo Khoa":
                    match = doctor.getDepartment() != null && doctor.getDepartment().getName() != null
                            && doctor.getDepartment().getName().toLowerCase().contains(keyword);
                    break;
                case "Theo Email":
                    match = doctor.getEmail() != null && doctor.getEmail().toLowerCase().contains(keyword);
                    break;
                case "Theo Số điện thoại":
                    match = doctor.getPhone() != null && doctor.getPhone().toLowerCase().contains(keyword);
                    break;
            }
            if (match) {
                filteredList.add(doctor);
            }
        }
        doctorObservableList.setAll(filteredList);
        if (filteredList.isEmpty()) {
            DialogUtil.showInfoAlert("Không tìm thấy", "Không tìm thấy bác sĩ nào khớp với tiêu chí tìm kiếm.");
        }
    }

    @FXML
    private void loadAllDoctors(ActionEvent event) {
        doctorObservableList.setAll(allDoctorsMasterList);
        searchField.clear();
        searchCriteriaCombo.getSelectionModel().clearSelection();
        searchCriteriaCombo.setPromptText("Tiêu chí tìm kiếm");
        doctorsTable.getSelectionModel().clearSelection();
        clearForm(null); // Clear form fields as well
    }

    // --- Sidebar Navigation Methods ---
    @FXML
    private void loadAdminViewRevenue(ActionEvent event) {
        uiManager.switchToAdminViewRevenue();
    }

    @FXML
    private void loadAdminManageDoctors(ActionEvent event) {
        // Already on this screen, maybe refresh data or do nothing
        log.info("Admin Manage Doctors button clicked (already on this screen).");
        refreshDoctorsTable(); // Refresh data
        loadDepartmentsData(); // Also refresh department list in case it changed
    }

    @FXML
    private void loadAdminManagePatients(ActionEvent event) {
        uiManager.switchToAdminManagePatients();
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
        log.info("Navigating to Admin Manage User Accounts screen.");
        uiManager.switchToAdminManageUserAccounts();
    }

    @FXML
    private void loadAdminManageDiseases(ActionEvent event) {
        uiManager.switchToAdminManageDiseases();
    }

    private void refreshDoctorsTable() {
        loadInitialDoctorsData(); // Tải lại dữ liệu gốc
        doctorObservableList.setAll(allDoctorsMasterList); // Cập nhật bảng hiển thị
        clearForm(null); // Xóa form
    }
}
