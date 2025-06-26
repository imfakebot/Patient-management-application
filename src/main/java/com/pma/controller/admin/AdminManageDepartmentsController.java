package com.pma.controller.admin;

import com.pma.model.entity.Department;
import com.pma.service.DepartmentService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityNotFoundException;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import javafx.fxml.Initializable;

@Component
@RequiredArgsConstructor
public class AdminManageDepartmentsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(AdminManageDepartmentsController.class);

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
    private Button logoutButton;

    @FXML
    private TextField departmentNameField;

    @FXML
    private Button addButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    @FXML
    private TableView<Department> departmentsTable;

    @FXML
    private TableColumn<Department, String> departmentNameColumn;

    @FXML
    private TableColumn<Department, String> createdAtColumn;

    @FXML
    private TableColumn<Department, String> updatedAtColumn;

    private final DepartmentService departmentService;
    private final UIManager uiManager;

    private final ObservableList<Department> departmentObservableList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing AdminManageDepartmentsController");
        setupTable();
        setupButtons();
        loadDepartments();
        log.info("AdminManageDepartmentsController initialized successfully");
    }

    private void setupTable() {
        departmentNameColumn.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        createdAtColumn.setCellValueFactory(cellData
                -> new SimpleStringProperty(cellData.getValue().getCreatedAt() != null
                        ? cellData.getValue().getCreatedAt().format(DATE_FORMATTER) : ""));
        updatedAtColumn.setCellValueFactory(cellData
                -> new SimpleStringProperty(cellData.getValue().getUpdatedAt() != null
                        ? cellData.getValue().getUpdatedAt().format(DATE_FORMATTER) : ""));

        departmentsTable.setItems(departmentObservableList);

        // Add selection listener
        departmentsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        departmentNameField.setText(newSelection.getDepartmentName());
                        updateButton.setDisable(false);
                        deleteButton.setDisable(false);
                    } else {
                        clearForm(null); // Clear form and disable buttons if no selection
                    }
                });
    }

    private void setupButtons() {
        // Initial state handled by selection listener
        clearForm(null);
    }

    @FXML
    void loadAdminViewRevenue(ActionEvent event) {
        uiManager.switchToAdminViewRevenue();
    }

    @FXML
    void loadAdminManageDoctors(ActionEvent event) {
        uiManager.switchToAdminManageDoctors();
    }

    @FXML
    void loadAdminManagePatients(ActionEvent event) {
        uiManager.switchToAdminManagePatients();
    }

    @FXML
    void loadAdminManageDepartments(ActionEvent event) {
        log.info("Admin: Load Manage Departments action triggered (current view). Refreshing data.");
        loadDepartments();
    }

    @FXML
    void loadAdminManageMedicines(ActionEvent event) {
        uiManager.switchToAdminManageMedicines();
    }

    @FXML
    void loadAdminManageUserAccounts(ActionEvent event) {
        uiManager.switchToAdminManageUserAccounts();
    }

    @FXML
    void loadAdminManageDiseases(ActionEvent event) {
        uiManager.switchToAdminManageDiseases();
    }

    @FXML
    void logout(ActionEvent event) {
        uiManager.switchToLoginScreen();
    }

    @FXML
    void addDepartment(ActionEvent event) {
        log.info("Add Department button clicked.");
        String departmentName = departmentNameField.getText().trim();

        if (departmentName.isEmpty()) {
            DialogUtil.showWarningAlert("Lỗi", "Tên khoa không được để trống!");
            return;
        }

        try {
            Department department = new Department();
            department.setDepartmentName(departmentName); // Match entity field
            Department savedDepartment = departmentService.createDepartment(department); // Assuming createDepartment method

            departmentObservableList.add(savedDepartment);
            departmentsTable.getSelectionModel().select(savedDepartment);
            DialogUtil.showSuccessAlert("Thành công", "Thêm khoa mới thành công!");
            clearForm(null);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while adding department: {}", departmentName, e);
            DialogUtil.showErrorAlert("Lỗi Trùng lặp", "Không thể thêm khoa. Tên khoa '" + departmentName + "' có thể đã tồn tại.");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for adding department: {}", e.getMessage());
            DialogUtil.showWarningAlert("Dữ liệu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            log.error("Error adding department: {}", departmentName, e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể thêm khoa.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void updateDepartment(ActionEvent event) {
        log.info("Update Department button clicked.");
        Department selectedDepartment = departmentsTable.getSelectionModel().getSelectedItem();
        if (selectedDepartment == null) {
            DialogUtil.showWarningAlert("Lỗi", "Vui lòng chọn khoa cần cập nhật!");
            return;
        }

        String departmentName = departmentNameField.getText().trim();
        if (departmentName.isEmpty()) {
            DialogUtil.showWarningAlert("Lỗi", "Tên khoa không được để trống!");
            return;
        }

        try {
            // It's safer to create a new object or DTO for updates
            // or ensure the service handles detached entities correctly.
            // For simplicity, assuming service can handle updating based on ID and new details.
            Department departmentDetailsToUpdate = new Department();
            departmentDetailsToUpdate.setDepartmentName(departmentName);
            // The service should fetch the entity by ID, apply changes, then save.
            Department updatedDepartment = departmentService.updateDepartment(selectedDepartment.getDepartmentId(), departmentDetailsToUpdate);

            int index = departmentObservableList.indexOf(selectedDepartment);
            if (index != -1) {
                departmentObservableList.set(index, updatedDepartment);
                departmentsTable.getSelectionModel().select(updatedDepartment);
            } else {
                loadDepartments(); // Fallback if not found
            }
            DialogUtil.showSuccessAlert("Thành công", "Cập nhật khoa thành công!");
            clearForm(null);
        } catch (EntityNotFoundException e) {
            log.error("Department not found for update: ID {}", selectedDepartment.getDepartmentId(), e);
            DialogUtil.showErrorAlert("Không tìm thấy", "Không tìm thấy khoa để cập nhật. Có thể đã bị xóa.");
            loadDepartments(); // Refresh list
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating department: {}", departmentName, e);
            DialogUtil.showErrorAlert("Lỗi Trùng lặp", "Không thể cập nhật. Tên khoa '" + departmentName + "' có thể đã tồn tại.");
        } catch (Exception e) {
            log.error("Error updating department: {}", departmentName, e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể cập nhật khoa.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void deleteDepartment(ActionEvent event) {
        log.info("Delete Department button clicked.");
        Department selectedDepartment = departmentsTable.getSelectionModel().getSelectedItem();
        if (selectedDepartment == null) {
            DialogUtil.showWarningAlert("Lỗi", "Vui lòng chọn khoa cần xóa!");
            return;
        }

        if (!DialogUtil.showConfirmationAlert("Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa khoa '" + selectedDepartment.getDepartmentName() + "' không?")
                .filter(response -> response == ButtonType.OK).isPresent()) {
            return;
        }

        try {
            departmentService.deleteDepartment(selectedDepartment.getDepartmentId());
            departmentObservableList.remove(selectedDepartment);
            DialogUtil.showSuccessAlert("Thành công", "Xóa khoa thành công!");
            clearForm(null);
        } catch (EntityNotFoundException e) {
            log.error("Department not found for deletion: ID {}", selectedDepartment.getDepartmentId(), e);
            DialogUtil.showErrorAlert("Không tìm thấy", "Không tìm thấy khoa để xóa. Có thể đã bị xóa.");
            loadDepartments(); // Refresh list
        } catch (DataIntegrityViolationException e) {
            log.error("Cannot delete department due to existing references: ID {}", selectedDepartment.getDepartmentId(), e);
            DialogUtil.showErrorAlert("Không thể Xóa", "Không thể xóa khoa này do có các bác sĩ hoặc dữ liệu khác đang liên kết.");
        } catch (Exception e) {
            log.error("Error deleting department: ID {}", selectedDepartment.getDepartmentId(), e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể xóa khoa.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void clearForm(ActionEvent event) {
        departmentNameField.clear();
        departmentsTable.getSelectionModel().clearSelection();
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void loadDepartments() {
        try {
            log.debug("Loading departments data...");
            List<Department> departments = departmentService.getAllDepartments(); // Assuming getAllDepartments method
            departmentObservableList.setAll(departments);
            log.info("Loaded {} departments.", departments.size());
        } catch (Exception e) {
            log.error("Error loading departments data: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi tải dữ liệu", "Không thể tải danh sách khoa.");
            departmentObservableList.clear();
        }
        // Ensure buttons are in correct state after loading
        clearForm(null);
    }
}
