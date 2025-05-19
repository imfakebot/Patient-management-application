package com.pma.controller;

import com.pma.model.entity.Prescription;
import com.pma.service.MedicineService;
import com.pma.service.PrescriptionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.UUID;

@Controller
public class PrescriptionController {

    @FXML private TableView<Prescription> prescriptionTable;
    @FXML private TableColumn<Prescription, UUID> prescriptionIdColumn;
    @FXML private TableColumn<Prescription, String> patientNameColumn;
    @FXML private TableColumn<Prescription, String> medicineColumn;
    @FXML private TextField patientIdField;
    @FXML private ComboBox<String> medicineComboBox;
    @FXML private TextField dosageField;
    @FXML private DatePicker issueDatePicker;
    @FXML private Button addButton;
    @FXML private Button updateButton;

    @Autowired
    private PrescriptionService prescriptionService;
    @Autowired
    private MedicineService medicineService;

    private ObservableList<Prescription> prescriptionList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        prescriptionIdColumn.setCellValueFactory(cellData -> cellData.getValue().prescriptionIdProperty());
        patientNameColumn.setCellValueFactory(cellData -> cellData.getValue().getPatient().fullNameProperty());
        medicineColumn.setCellValueFactory(cellData -> cellData.getValue().getMedicine().nameProperty());
        prescriptionTable.setItems(prescriptionList);
        loadMedicines();
        loadPrescriptions();

        addButton.setOnAction(event -> addPrescription());
        updateButton.setOnAction(event -> updatePrescription());
    }

    private void loadPrescriptions() {
        prescriptionList.setAll(prescriptionService.getAllPrescriptions(null).getContent());
    }

    private void loadMedicines() {
        medicineComboBox.getItems().addAll(
            medicineService.getAllMedicines(null).getContent().stream()
                .map(medicine -> medicine.getName()).toList());
    }

    private void addPrescription() {
        try {
            Prescription prescription = new Prescription();
            prescription.setPatient(prescriptionService.getPatientById(UUID.fromString(patientIdField.getText())));
            prescription.setMedicine(medicineService.getMedicineByName(medicineComboBox.getValue()));
            prescription.setDosage(dosageField.getText());
            prescription.setIssueDate(issueDatePicker.getValue());
            prescriptionService.createPrescription(prescription);
            loadPrescriptions();
            clearFields();
            showAlert("Thành công", "Đơn thuốc đã được thêm!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể thêm đơn thuốc: " + e.getMessage());
        }
    }

    private void updatePrescription() {
        Prescription selected = prescriptionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn đơn thuốc!");
            return;
        }
        try {
            selected.setPatient(prescriptionService.getPatientById(UUID.fromString(patientIdField.getText())));
            selected.setMedicine(medicineService.getMedicineByName(medicineComboBox.getValue()));
            selected.setDosage(dosageField.getText());
            selected.setIssueDate(issueDatePicker.getValue());
            prescriptionService.updatePrescription(selected.getPrescriptionId(), selected);
            loadPrescriptions();
            clearFields();
            showAlert("Thành công", "Đơn thuốc đã được cập nhật!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể cập nhật đơn thuốc: " + e.getMessage());
        }
    }

    private void clearFields() {
        patientIdField.clear();
        medicineComboBox.setValue(null);
        dosageField.clear();
        issueDatePicker.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}