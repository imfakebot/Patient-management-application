package com.pma.controller;

import com.pma.model.entity.Doctor;
import com.pma.service.DepartmentService;
import com.pma.service.DoctorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class DoctorController {

    @FXML private TableView<Doctor> doctorTable;
    @FXML private TableColumn<Doctor, UUID> doctorIdColumn;
    @FXML private TableColumn<Doctor, String> fullNameColumn;
    @FXML private TableColumn<Doctor, String> departmentColumn;
    @FXML private TextField fullNameField;
    @FXML private TextField licenseNumberField;
    @FXML private ComboBox<String> departmentComboBox;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    @Autowired
    private DoctorService doctorService;
    @Autowired
    private DepartmentService departmentService;

    private ObservableList<Doctor> doctorList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        doctorIdColumn.setCellValueFactory(cellData -> cellData.getValue().doctorIdProperty());
        fullNameColumn.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        departmentColumn.setCellValueFactory(cellData -> cellData.getValue().getDepartment().nameProperty());
        doctorTable.setItems(doctorList);
        loadDepartments();
        loadDoctors();

        addButton.setOnAction(event -> addDoctor());
        updateButton.setOnAction(event -> updateDoctor());
        deleteButton.setOnAction(event -> deleteDoctor());
    }

    private void loadDoctors() {
        doctorList.setAll(doctorService.getAllDoctors(null).getContent());
    }

    private void loadDepartments() {
        departmentComboBox.getItems().addAll(
            departmentService.getAllDepartments(null).getContent().stream()
                .map(dept -> dept.getName()).toList());
    }

    private void addDoctor() {
        try {
            Doctor doctor = new Doctor();
            doctor.setFullName(fullNameField.getText());
            doctor.setLicenseNumber(licenseNumberField.getText());
            String deptName = departmentComboBox.getValue();
            doctor.setDepartment(departmentService.getDepartmentByName(deptName));
            doctorService.createDoctor(doctor);
            loadDoctors();
            clearFields();
            showAlert("Thành công", "Bác sĩ đã được thêm!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể thêm bác sĩ: " + e.getMessage());
        }
    }

    private void updateDoctor() {
        Doctor selected = doctorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn bác sĩ!");
            return;
        }
        try {
            selected.setFullName(fullNameField.getText());
            selected.setLicenseNumber(licenseNumberField.getText());
            String deptName = departmentComboBox.getValue();
            selected.setDepartment(departmentService.getDepartmentByName(deptName));
            doctorService.updateDoctor(selected.getDoctorId(), selected);
            loadDoctors();
            clearFields();
            showAlert("Thành công", "Bác sĩ đã được cập nhật!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể cập nhật bác sĩ: " + e.getMessage());
        }
    }

    private void deleteDoctor() {
        Doctor selected = doctorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn bác sĩ!");
            return;
        }
        try {
            doctorService.deleteDoctor(selected.getDoctorId());
            loadDoctors();
            clearFields();
            showAlert("Thành công", "Bác sĩ đã được xóa!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể xóa bác sĩ: " + e.getMessage());
        }
    }

    private void clearFields() {
        fullNameField.clear();
        licenseNumberField.clear();
        departmentComboBox.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}