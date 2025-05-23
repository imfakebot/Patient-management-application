// package com.pma.controller;

// import com.pma.model.entity.Diagnosis;
// import com.pma.service.DiagnosisService;
// import com.pma.service.DiseaseService;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.fxml.FXML;
// import javafx.scene.control.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import java.time.LocalDate;
// import java.util.UUID;

// @Controller
// public class MedicalReportController {

//     @FXML private TableView<Diagnosis> reportTable;
//     @FXML private TableColumn<Diagnosis, UUID> diagnosisIdColumn;
//     @FXML private TableColumn<Diagnosis, String> patientNameColumn;
//     @FXML private TableColumn<Diagnosis, String> diseaseColumn;
//     @FXML private TextField patientIdField;
//     @FXML private ComboBox<String> diseaseComboBox;
//     @FXML private TextArea descriptionArea;
//     @FXML private DatePicker diagnosisDatePicker;
//     @FXML private Button addButton;
//     @FXML private Button updateButton;

//     @Autowired
//     private DiagnosisService diagnosisService;
//     @Autowired
//     private DiseaseService diseaseService;

//     private ObservableList<Diagnosis> diagnosisList = FXCollections.observableArrayList();

//     @FXML
//     private void initialize() {
//         diagnosisIdColumn.setCellValueFactory(cellData -> cellData.getValue().diagnosisIdProperty());
//         patientNameColumn.setCellValueFactory(cellData -> cellData.getValue().getPatient().fullNameProperty());
//         diseaseColumn.setCellValueFactory(cellData -> cellData.getValue().getDisease().nameProperty());
//         reportTable.setItems(diagnosisList);
//         loadDiseases();
//         loadDiagnoses();

//         addButton.setOnAction(event -> addDiagnosis());
//         updateButton.setOnAction(event -> updateDiagnosis());
//     }

//     private void loadDiagnoses() {
//         diagnosisList.setAll(diagnosisService.getAllDiagnoses(null).getContent());
//     }

//     private void loadDiseases() {
//         diseaseComboBox.getItems().addAll(
//             diseaseService.getAllDiseases(null).getContent().stream()
//                 .map(disease -> disease.getName()).toList());
//     }

//     private void addDiagnosis() {
//         try {
//             Diagnosis diagnosis = new Diagnosis();
//             diagnosis.setPatient(diagnosisService.getPatientById(UUID.fromString(patientIdField.getText())));
//             diagnosis.setDisease(diseaseService.getDiseaseByName(diseaseComboBox.getValue()));
//             diagnosis.setDescription(descriptionArea.getText());
//             diagnosis.setDiagnosisDate(diagnosisDatePicker.getValue());
//             diagnosisService.createDiagnosis(diagnosis);
//             loadDiagnoses();
//             clearFields();
//             showAlert("Thành công", "Hồ sơ y tế đã được thêm!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể thêm hồ sơ y tế: " + e.getMessage());
//         }
//     }

//     private void updateDiagnosis() {
//         Diagnosis selected = reportTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Lỗi", "Vui lòng chọn hồ sơ y tế!");
//             return;
//         }
//         try {
//             selected.setPatient(diagnosisService.getPatientById(UUID.fromString(patientIdField.getText())));
//             selected.setDisease(diseaseService.getDiseaseByName(diseaseComboBox.getValue()));
//             selected.setDescription(descriptionArea.getText());
//             selected.setDiagnosisDate(diagnosisDatePicker.getValue());
//             diagnosisService.updateDiagnosis(selected.getDiagnosisId(), selected);
//             loadDiagnoses();
//             clearFields();
//             showAlert("Thành công", "Hồ sơ y tế đã được cập nhật!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể cập nhật hồ sơ y tế: " + e.getMessage());
//         }
//     }

//     private void clearFields() {
//         patientIdField.clear();
//         diseaseComboBox.setValue(null);
//         descriptionArea.clear();
//         diagnosisDatePicker.setValue(null);
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }
// }