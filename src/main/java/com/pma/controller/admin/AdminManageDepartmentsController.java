package com.pma.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

// Giả sử bạn sẽ có một class DepartmentViewModel hoặc tương tự để hiển thị dữ liệu
// import com.pma.model.ui.DepartmentViewModel; // Ví dụ
public class AdminManageDepartmentsController {

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
    private TableView<?> departmentsTable; // Thay thế ? bằng kiểu dữ liệu phù hợp, ví dụ: TableView<DepartmentViewModel>

    @FXML
    private TableColumn<?, ?> departmentNameColumn; // Thay thế ? bằng kiểu dữ liệu phù hợp, ví dụ: TableColumn<DepartmentViewModel, String>

    @FXML
    private TableColumn<?, ?> createdAtColumn; // Thay thế ? bằng kiểu dữ liệu phù hợp, ví dụ: TableColumn<DepartmentViewModel, String> hoặc LocalDateTime

    @FXML
    private TableColumn<?, ?> updatedAtColumn; // Thay thế ? bằng kiểu dữ liệu phù hợp, ví dụ: TableColumn<DepartmentViewModel, String> hoặc LocalDateTime

    @FXML
    void initialize() {
        // Khởi tạo controller, ví dụ: thiết lập cell value factories cho TableView
        // departmentNameColumn.setCellValueFactory(new PropertyValueFactory<>("name")); // Ví dụ
        // createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAtFormatted")); // Ví dụ
        // updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAtFormatted")); // Ví dụ

        // Load data vào table
        // loadDepartments();
    }

    @FXML
    void loadAdminViewRevenue(ActionEvent event) {
        System.out.println("Admin: Load View Revenue action triggered.");
        // TODO: Implement navigation logic
    }

    @FXML
    void loadAdminManageDoctors(ActionEvent event) {
        System.out.println("Admin: Load Manage Doctors action triggered.");
        // TODO: Implement navigation logic
    }

    @FXML
    void loadAdminManagePatients(ActionEvent event) {
        System.out.println("Admin: Load Manage Patients action triggered.");
        // TODO: Implement navigation logic
    }

    @FXML
    void loadAdminManageDepartments(ActionEvent event) {
        System.out.println("Admin: Load Manage Departments action triggered (current view).");
        // TODO: Implement navigation logic (hoặc làm mới view nếu cần)
    }

    @FXML
    void loadAdminManageMedicines(ActionEvent event) {
        System.out.println("Admin: Load Manage Medicines action triggered.");
        // TODO: Implement navigation logic
    }

    @FXML
    void loadAdminManageUserAccounts(ActionEvent event) {
        System.out.println("Admin: Load Manage User Accounts action triggered.");
        // TODO: Implement navigation logic
    }

    @FXML
    void loadAdminManageDiseases(ActionEvent event) {
        System.out.println("Admin: Load Manage Diseases action triggered.");
        // TODO: Implement navigation logic
    }

    @FXML
    void addDepartment(ActionEvent event) {
        String departmentName = departmentNameField.getText();
        System.out.println("Add Department: " + departmentName);
        // TODO: Implement logic to add department
        // clearForm(null);
        // loadDepartments();
    }

    @FXML
    void updateDepartment(ActionEvent event) {
        // Object selectedDepartment = departmentsTable.getSelectionModel().getSelectedItem();
        String departmentName = departmentNameField.getText();
        System.out.println("Update Department: " + departmentName);
        // TODO: Implement logic to update department
        // clearForm(null);
        // loadDepartments();
    }

    @FXML
    void deleteDepartment(ActionEvent event) {
        // Object selectedDepartment = departmentsTable.getSelectionModel().getSelectedItem();
        System.out.println("Delete Department action triggered.");
        // TODO: Implement logic to delete department
        // clearForm(null);
        // loadDepartments();
    }

    @FXML
    void clearForm(ActionEvent event) {
        departmentNameField.clear();
        departmentsTable.getSelectionModel().clearSelection();
        System.out.println("Form cleared.");
    }

    // private void loadDepartments() {
    //     // TODO: Implement logic to load departments into the table
    //     // ObservableList<DepartmentViewModel> departmentList = ...
    //     // departmentsTable.setItems(departmentList);
    // }
}
