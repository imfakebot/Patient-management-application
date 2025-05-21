// package com.pma.controller;

// import com.pma.model.entity.Patient;
// import com.pma.service.PatientService;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.fxml.FXML;
// import javafx.scene.control.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import java.time.LocalDate;
// import java.util.UUID;

// @Controller
// public class PatientController {

//     @FXML private TableView<Patient> patientTable;
//     @FXML private TableColumn<Patient, UUID> patientIdColumn;
//     @FXML private TableColumn<Patient, String> fullNameColumn;
//     @FXML private TableColumn<Patient, String> phoneNumberColumn;
//     @FXML private TextField fullNameField;
//     @FXML private TextField phoneNumberField;
//     @FXML private TextField addressField;
//     @FXML private DatePicker dateOfBirthPicker;
//     @FXML private ComboBox<String> genderComboBox;
//     @FXML private Button addButton;
//     @FXML private Button updateButton;
//     @FXML private Button deleteButton;

//     @Autowired
//     private PatientService patientService;

//     private ObservableList<Patient> patientList = FXCollections.observableArrayList();

//     @FXML
//     private void initialize() {
//         patientIdColumn.setCellValueFactory(cellData -> cellData.getValue().patientIdProperty());
//         fullNameColumn.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
//         phoneNumberColumn.setCellValueFactory(cellData -> cellData.getValue().phoneNumberProperty());
//         patientTable.setItems(patientList);
//         genderComboBox.getItems().addAll("Nam", "Nữ", "Khác");
//         loadPatients();

//         addButton.setOnAction(event -> addPatient());
//         updateButton.setOnAction(event -> updatePatient());
//         deleteButton.setOnAction(event -> deletePatient());
//     }

//     private void loadPatients() {
//         patientList.setAll(patientService.getAllPatients(null).getContent());
//     }

//     private void addPatient() {
//         try {
//             Patient patient = new Patient();
//             patient.setFullName(fullNameField.getText());
//             patient.setPhoneNumber(phoneNumberField.getText());
//             patient.setAddress(addressField.getText());
//             patient.setDateOfBirth(dateOfBirthPicker.getValue());
//             patient.setGender(genderComboBox.getValue());
//             patientService.createPatient(patient);
//             loadPatients();
//             clearFields();
//             showAlert("Thành công", "Bệnh nhân đã được thêm!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể thêm bệnh nhân: " + e.getMessage());
//         }
//     }

//     private void updatePatient() {
//         Patient selected = patientTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Lỗi", "Vui lòng chọn bệnh nhân!");
//             return;
//         }
//         try {
//             selected.setFullName(fullNameField.getText());
//             selected.setPhoneNumber(phoneNumberField.getText());
//             selected.setAddress(addressField.getText());
//             selected.setDateOfBirth(dateOfBirthPicker.getValue());
//             selected.setGender(genderComboBox.getValue());
//             patientService.updatePatient(selected.getPatientId(), selected);
//             loadPatients();
//             clearFields();
//             showAlert("Thành công", "Bệnh nhân đã được cập nhật!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể cập nhật bệnh nhân: " + e.getMessage());
//         }
//     }

//     private void deletePatient() {
//         Patient selected = patientTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Lỗi", "Vui lòng chọn bệnh nhân!");
//             return;
//         }
//         try {
//             patientService.deletePatient(selected.getPatientId());
//             loadPatients();
//             clearFields();
//             showAlert("Thành công", "Bệnh nhân đã được xóa!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể xóa bệnh nhân: " + e.getMessage());
//         }
//     }

//     private void clearFields() {
//         fullNameField.clear();
//         phoneNumberField.clear();
//         addressField.clear();
//         dateOfBirthPicker.setValue(null);
//         genderComboBox.setValue(null);
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }
// }