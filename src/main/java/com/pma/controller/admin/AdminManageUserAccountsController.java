package com.pma.controller.admin;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.pma.model.entity.Doctor;
import com.pma.model.entity.Patient;
import com.pma.model.entity.UserAccount;
import com.pma.model.enums.UserRole;
import com.pma.service.DoctorService;
import com.pma.service.PatientService;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;

import jakarta.persistence.EntityNotFoundException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminManageUserAccountsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(AdminManageUserAccountsController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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

    @FXML
    private TableView<UserAccount> userAccountsTable;
    @FXML
    private TableColumn<UserAccount, String> usernameColumn;
    // Consider PasswordField for password input if it's for creation/reset
    @FXML
    private TableColumn<UserAccount, String> roleColumn;
    @FXML
    private TableColumn<UserAccount, String> patientColumn;
    @FXML
    private TableColumn<UserAccount, String> doctorColumn;
    @FXML
    private TableColumn<UserAccount, String> lastLoginColumn;
    @FXML
    private TableColumn<UserAccount, String> lastLoginIpColumn;
    @FXML
    private TableColumn<UserAccount, String> lastPasswordChangeColumn;
    @FXML
    private TableColumn<UserAccount, Boolean> isActiveColumn;
    @FXML
    private TableColumn<UserAccount, Integer> failedLoginAttemptsColumn;
    @FXML
    private TableColumn<UserAccount, String> lockoutUntilColumn;
    @FXML
    private TableColumn<UserAccount, Boolean> isEmailVerifiedColumn;
    @FXML
    private TableColumn<UserAccount, Boolean> twoFactorEnabledColumn;
    @FXML
    private TableColumn<UserAccount, String> createdAtColumn;
    @FXML
    private TableColumn<UserAccount, String> updatedAtColumn;

    @FXML
    private TextField usernameField;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private ComboBox<Patient> patientCombo;
    @FXML
    private ComboBox<Doctor> doctorCombo;
    @FXML
    private CheckBox isActiveCheckBox;
    @FXML
    private CheckBox isEmailVerifiedCheckBox;
    @FXML
    private CheckBox twoFactorEnabledCheckBox;
    @FXML
    private Button addButton; // Assuming you have an Add button in FXML
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;

    private final UserAccountService userAccountService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final UIManager uiManager;

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing AdminManageUserAccountsController");
        setupTable();
        setupComboBoxes();
        setupListeners();
        // Initial button states are set by clearForm
        clearForm(null); // Call clearForm to set initial state of buttons and fields
        loadData();
        log.info("AdminManageUserAccountsController initialized successfully");
    }

    // Constants for Dialogs
    private static final String SUCCESS_TITLE = "Thành công";
    private static final String ERROR_TITLE = "Lỗi";
    private static final String WARNING_TITLE = "Cảnh báo";

    private void setupTable() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        patientColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPatient() != null ? data.getValue().getPatient().getFullName() : ""));
        doctorColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDoctor() != null ? data.getValue().getDoctor().getFullName() : ""));
        lastLoginColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getLastLogin() != null ? data.getValue().getLastLogin().format(DATE_FORMATTER) : ""));
        lastLoginIpColumn.setCellValueFactory(new PropertyValueFactory<>("lastLoginIp"));
        lastPasswordChangeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getLastPasswordChange() != null
                ? data.getValue().getLastPasswordChange().format(DATE_FORMATTER) : ""));
        isActiveColumn.setCellValueFactory(new PropertyValueFactory<>("isActive"));
        failedLoginAttemptsColumn.setCellValueFactory(new PropertyValueFactory<>("failedLoginAttempts"));
        lockoutUntilColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getLockoutUntil() != null
                ? data.getValue().getLockoutUntil().format(DATE_FORMATTER) : ""));
        isEmailVerifiedColumn.setCellValueFactory(new PropertyValueFactory<>("isEmailVerified"));
        twoFactorEnabledColumn.setCellValueFactory(new PropertyValueFactory<>("twoFactorEnabled"));
        createdAtColumn.setCellValueFactory(data -> {
            if (data.getValue() != null && data.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(data.getValue().getCreatedAt().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("");
        });
        updatedAtColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUpdatedAt().format(DATE_FORMATTER)));
    }

    private void setupComboBoxes() {
        roleCombo.setItems(FXCollections.observableArrayList(
                UserRole.PATIENT.name(),
                UserRole.DOCTOR.name(),
                UserRole.ADMIN.name()
        ));

        try {
            List<Patient> patients = patientService.findAll();
            patientCombo.setItems(FXCollections.observableArrayList(patients));
        } catch (Exception e) {
            log.error("Failed to load patients for ComboBox", e);
            DialogUtil.showErrorAlert(ERROR_TITLE, "Không thể tải danh sách bệnh nhân.");
        }
        patientCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Patient patient) {
                return patient == null ? null : patient.getFullName() + " (ID: " + patient.getPatientId() + ")";
            }

            @Override
            public Patient fromString(String string) {
                return patientCombo.getItems().stream().filter(p -> toString(p).equals(string)).findFirst().orElse(null);
            }
        });

        try {
            List<Doctor> doctors = doctorService.findAll();
            doctorCombo.setItems(FXCollections.observableArrayList(doctors));
        } catch (Exception e) {
            log.error("Failed to load doctors for ComboBox", e);
            DialogUtil.showErrorAlert(ERROR_TITLE, "Không thể tải danh sách bác sĩ.");
        }
        doctorCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Doctor doctor) {
                return doctor == null ? null : doctor.getFullName() + " (ID: " + doctor.getDoctorId() + ")";
            }

            @Override
            public Doctor fromString(String string) {
                return doctorCombo.getItems().stream().filter(d -> toString(d).equals(string)).findFirst().orElse(null);
            }
        });
    }

    private void setupListeners() {
        userAccountsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        populateForm(newSelection);
                        updateButton.setDisable(false);
                        deleteButton.setDisable(false);
                    }
                });

        roleCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                boolean isPatient = UserRole.PATIENT.name().equals(newValue);
                boolean isDoctor = UserRole.DOCTOR.name().equals(newValue);
                patientCombo.setDisable(!isPatient);
                doctorCombo.setDisable(!isDoctor);
                if (!isPatient) {
                    patientCombo.getSelectionModel().clearSelection();
                }
                if (!isDoctor) {
                    doctorCombo.getSelectionModel().clearSelection();
                }
            }
        });
    }

    private void loadData() {
        try {
            log.debug("Loading user accounts data...");
            List<UserAccount> accounts = userAccountService.findAll();
            userAccountsTable.setItems(FXCollections.observableArrayList(accounts));
            log.info("Loaded {} user accounts.", accounts.size());
        } catch (Exception e) {
            log.error("Error loading user accounts data: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert(ERROR_TITLE, "Không thể tải danh sách tài khoản người dùng.");
            userAccountsTable.getItems().clear();
        }
        clearForm(null); // Reset form and button states after loading
    }

    private void populateForm(UserAccount userAccount) {
        usernameField.setDisable(true); // Username should not be editable after creation
        usernameField.setText(userAccount.getUsername());
        roleCombo.setValue(userAccount.getRole().name());
        patientCombo.setValue(userAccount.getPatient());
        doctorCombo.setValue(userAccount.getDoctor());
        isActiveCheckBox.setSelected(userAccount.isActive());
        isEmailVerifiedCheckBox.setSelected(userAccount.isEmailVerified());
        twoFactorEnabledCheckBox.setSelected(userAccount.isTwoFactorEnabled());

        // Enable/disable patient/doctor combo based on role
        boolean isPatient = UserRole.PATIENT.equals(userAccount.getRole());
        boolean isDoctor = UserRole.DOCTOR.equals(userAccount.getRole());
        patientCombo.setDisable(!isPatient);
        doctorCombo.setDisable(!isDoctor);
    }

    @FXML
    void addUserAccount(ActionEvent event) {
        log.info("Add User Account button clicked.");
        if (!validateInput()) { // Password validation removed
            return;
        }

        try {
            UserAccount userAccount = new UserAccount();
            updateUserAccountFromForm(userAccount);
            // Ensure patient/doctor is null if role is not PATIENT/DOCTOR
            if (userAccount.getRole() != UserRole.PATIENT) {
                userAccount.setPatient(null);
            }
            if (userAccount.getRole() != UserRole.DOCTOR) {
                userAccount.setDoctor(null);
            }

            userAccountService.save(userAccount);
            DialogUtil.showSuccessAlert(SUCCESS_TITLE, "Thêm tài khoản thành công!");
            clearForm(null);
            loadData();
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while adding user account: {}", usernameField.getText(), e);
            DialogUtil.showErrorAlert(ERROR_TITLE, "Không thể thêm tài khoản. Tên người dùng '" + usernameField.getText() + "' có thể đã tồn tại.");
        } catch (Exception e) {
            log.error("Error adding user account: {}", usernameField.getText(), e);
            DialogUtil.showExceptionDialog(ERROR_TITLE, "Không thể thêm tài khoản.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void updateUserAccount(ActionEvent event) {
        log.info("Update User Account button clicked.");
        UserAccount selectedAccount = userAccountsTable.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            DialogUtil.showWarningAlert(WARNING_TITLE, "Vui lòng chọn tài khoản cần cập nhật!");
            return;
        }

        if (!validateInput()) { // Password validation removed
            return;
        }

        try {
            updateUserAccountFromForm(selectedAccount);
            // Ensure patient/doctor is null if role is not PATIENT/DOCTOR
            if (selectedAccount.getRole() != UserRole.PATIENT) {
                selectedAccount.setPatient(null);
            }
            if (selectedAccount.getRole() != UserRole.DOCTOR) {
                selectedAccount.setDoctor(null);
            }

            userAccountService.save(selectedAccount);
            DialogUtil.showSuccessAlert(SUCCESS_TITLE, "Cập nhật tài khoản thành công!");
            clearForm(null);
            loadData();
        } catch (EntityNotFoundException e) {
            log.error("User account not found for update: ID {}", selectedAccount.getUserId(), e);
            DialogUtil.showErrorAlert(ERROR_TITLE, "Không tìm thấy tài khoản để cập nhật. Có thể đã bị xóa.");
            loadData(); // Refresh list
        } catch (Exception e) {
            log.error("Error updating user account: ID {}", selectedAccount.getUserId(), e);
            DialogUtil.showExceptionDialog(ERROR_TITLE, "Không thể cập nhật tài khoản.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void deleteUserAccount(ActionEvent event) {
        UserAccount selectedAccount = userAccountsTable.getSelectionModel().getSelectedItem();
        if (selectedAccount == null) {
            DialogUtil.showWarningAlert(WARNING_TITLE, "Vui lòng chọn tài khoản cần xóa!");
            return;
        }

        if (!DialogUtil.showConfirmationAlert("Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa tài khoản '" + selectedAccount.getUsername() + "' không?")
                .filter(buttonType -> buttonType == ButtonType.OK || buttonType == ButtonType.YES).isPresent()) {
            return;
        }

        try {
            userAccountService.delete(selectedAccount.getUserId());
            DialogUtil.showSuccessAlert(SUCCESS_TITLE, "Xóa tài khoản thành công!");
            clearForm(null);
            loadData();
        } catch (EntityNotFoundException e) {
            log.error("User account not found for deletion: ID {}", selectedAccount.getUserId(), e);
            DialogUtil.showErrorAlert(ERROR_TITLE, "Không tìm thấy tài khoản để xóa. Có thể đã bị xóa.");
            loadData(); // Refresh list
        } catch (DataIntegrityViolationException e) {
            log.error("Cannot delete user account due to existing references: ID {}", selectedAccount.getUserId(), e);
            DialogUtil.showErrorAlert(ERROR_TITLE, "Không thể xóa tài khoản này do có các dữ liệu liên quan.");
        } catch (Exception e) {
            log.error("Error deleting user account: ID {}", selectedAccount.getUserId(), e);
            DialogUtil.showExceptionDialog(ERROR_TITLE, "Không thể xóa tài khoản.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void clearForm(ActionEvent event) {
        usernameField.setDisable(false); // Enable for new entry
        usernameField.clear();
        roleCombo.setValue(null);
        patientCombo.setValue(null);
        doctorCombo.setValue(null);
        isActiveCheckBox.setSelected(false);
        isEmailVerifiedCheckBox.setSelected(false);
        twoFactorEnabledCheckBox.setSelected(false);
        userAccountsTable.getSelectionModel().clearSelection();
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        patientCombo.setDisable(true); // Disable by default
        doctorCombo.setDisable(true);  // Disable by default
        log.debug("Form cleared and buttons reset.");
    }

    private boolean validateInput() { // Parameter isNewUser removed
        StringBuilder errors = new StringBuilder();

        if (usernameField.getText().trim().isEmpty()) {
            errors.append("- Tên người dùng không được để trống\n");
        }

        if (roleCombo.getValue() == null) {
            errors.append("- Vai trò không được để trống\n");
        } else {
            if (UserRole.PATIENT.name().equals(roleCombo.getValue()) && patientCombo.getValue() == null) {
                errors.append("- Bệnh nhân không được để trống với vai trò Patient\n");
            }
            if (UserRole.DOCTOR.name().equals(roleCombo.getValue()) && doctorCombo.getValue() == null) {
                errors.append("- Bác sĩ không được để trống với vai trò Doctor\n");
            }
        }

        if (errors.length() > 0) {
            DialogUtil.showWarningAlert(WARNING_TITLE, errors.toString());
            return false;
        }
        return true;
    }

    private void updateUserAccountFromForm(UserAccount userAccount) {
        if (usernameField.isDisabled()) { // Only update username if it's a new account (field is enabled)
            // Do not update username if field is disabled (meaning it's an update operation)
        } else {
            userAccount.setUsername(usernameField.getText().trim());
        }

        userAccount.setRole(UserRole.valueOf(roleCombo.getValue()));
        userAccount.setPatient(UserRole.PATIENT.name().equals(roleCombo.getValue())
                ? patientCombo.getValue() : null);
        userAccount.setDoctor(UserRole.DOCTOR.name().equals(roleCombo.getValue())
                ? doctorCombo.getValue() : null);
        userAccount.setActive(isActiveCheckBox.isSelected());
        userAccount.setEmailVerified(isEmailVerifiedCheckBox.isSelected());
        userAccount.setTwoFactorEnabled(twoFactorEnabledCheckBox.isSelected());
    }

    // Navigation methods
    @FXML
    void loadAdminViewRevenue(ActionEvent event) {
        log.info("Navigating to Admin View Revenue screen.");
        uiManager.switchToAdminViewRevenue();
    }

    @FXML
    void loadAdminManageDoctors(ActionEvent event) {
        log.info("Navigating to Admin Manage Doctors screen.");
        uiManager.switchToAdminManageDoctors();
    }

    @FXML
    void loadAdminManagePatients(ActionEvent event) {
        log.info("Navigating to Admin Manage Patients screen.");
        uiManager.switchToAdminManagePatients();
    }

    @FXML
    void loadAdminManageDepartments(ActionEvent event) {
        log.info("Navigating to Admin Manage Departments screen.");
        uiManager.switchToAdminManageDepartments();
    }

    @FXML
    void loadAdminManageMedicines(ActionEvent event) {
        log.info("Navigating to Admin Manage Medicines screen.");
        uiManager.switchToAdminManageMedicines();
    }

    @FXML
    void loadAdminManageUserAccounts(ActionEvent event) {
        // Already on this screen, refresh data
        log.info("Admin Manage User Accounts button clicked (already on this screen). Refreshing data.");
        loadData();
    }

    @FXML
    void loadAdminManageDiseases(ActionEvent event) {
        log.info("Navigating to Admin Manage Diseases screen.");
        uiManager.switchToAdminManageDiseases();
    }
}
