package com.pma.controller.patient;

import com.pma.model.entity.Diagnosis;
import com.pma.model.entity.MedicalRecord;
import com.pma.repository.DiagnosisRepository;
import com.pma.repository.MedicalRecordRepository;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.util.converter.LocalDateStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class PatientMedicalHistoryController {

    private static final Logger log = LoggerFactory.getLogger(PatientMedicalHistoryController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Button patientBookAppointmentButton;
    @FXML private Button patientViewPrescriptionsButton;
    @FXML private Button patientMedicalHistoryButton;
    @FXML private Button patientUpdateProfileButton;
    @FXML private Button patientReviewButton;
    @FXML private Button patientViewBillsButton;

    @FXML
    private TableView<MedicalRecord> medicalRecordsTable;

    @FXML
    private TableColumn<MedicalRecord, LocalDate> recordDateColumn;

    @FXML
    private TableColumn<MedicalRecord, String> doctorColumn;

    @FXML
    private TableColumn<MedicalRecord, String> appointmentColumn;

    @FXML
    private TableColumn<MedicalRecord, String> notesColumn;

    @FXML
    private TableView<Diagnosis> diagnosesTable;

    @FXML
    private TableColumn<Diagnosis, String> diseaseCodeColumn;

    @FXML
    private TableColumn<Diagnosis, String> diseaseNameColumn;

    @FXML
    private TableColumn<Diagnosis, String> diagnosisDescriptionColumn;

    @FXML
    private TableColumn<Diagnosis, LocalDate> diagnosisDateColumn;

    @FXML
    private TableColumn<Diagnosis, String> statusColumn;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    private UUID patientId;

    private final ObservableList<MedicalRecord> medicalRecords = FXCollections.observableArrayList();
    private final ObservableList<Diagnosis> diagnoses = FXCollections.observableArrayList();
    @Autowired
    private UIManager uiManager;

    @FXML
public void initialize() {
    // Thiết lập các cột của medicalRecordsTable
    recordDateColumn.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
    recordDateColumn.setCellFactory(column -> new TableCell<MedicalRecord, LocalDate>() {
        @Override
        protected void updateItem(LocalDate item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText("N/A");
            } else {
                setText(DATE_FORMATTER.format(item));
            }
        }
    });
    doctorColumn.setCellValueFactory(cellData -> {
        MedicalRecord record = cellData.getValue();
        return new SimpleStringProperty(
                record.getDoctor() != null ? record.getDoctor().getFullName() : "N/A");
    });
    appointmentColumn.setCellValueFactory(cellData -> {
        MedicalRecord record = cellData.getValue();
        return new SimpleStringProperty(
                record.getAppointment() != null ? record.getAppointment().getAppointmentId().toString() : "N/A");
    });
    notesColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getNotes() != null ? cellData.getValue().getNotes() : ""));

    // Thiết lập các cột của diagnosesTable
    diseaseCodeColumn.setCellValueFactory(cellData -> {
        Diagnosis diagnosis = cellData.getValue();
        return new SimpleStringProperty(
                diagnosis.getDisease() != null ? diagnosis.getDisease().getDiseaseCode() : "N/A");
    });
    diseaseNameColumn.setCellValueFactory(cellData -> {
        Diagnosis diagnosis = cellData.getValue();
        return new SimpleStringProperty(
                diagnosis.getDisease() != null ? diagnosis.getDisease().getDiseaseName() : "N/A");
    });
    diagnosisDescriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getDiagnosisDescription() != null ? cellData.getValue().getDiagnosisDescription() : ""));
    diagnosisDateColumn.setCellValueFactory(new PropertyValueFactory<>("diagnosisDate"));
    diagnosisDateColumn.setCellFactory(column -> new TableCell<Diagnosis, LocalDate>() {
        @Override
        protected void updateItem(LocalDate item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText("N/A");
            } else {
                setText(DATE_FORMATTER.format(item));
            }
        }
    });
    statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().toString() : "N/A"));

    // Gán danh sách dữ liệu cho bảng
    medicalRecordsTable.setItems(medicalRecords);
    diagnosesTable.setItems(diagnoses);

    // Lắng nghe sự kiện chọn bản ghi y tế để cập nhật bảng chẩn đoán
    medicalRecordsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
        if (newSelection != null) {
            loadDiagnosesForRecord(newSelection.getRecordId());
        } else {
            diagnoses.clear();
        }
    });
}

    public void initData(UUID patientId) {
        this.patientId = patientId;
        loadMedicalRecords();
    }

    private void loadMedicalRecords() {
        try {
            List<MedicalRecord> records = medicalRecordRepository.findByPatient_PatientId(patientId);
            medicalRecords.setAll(records);
            if (!records.isEmpty()) {
                medicalRecordsTable.getSelectionModel().selectFirst();
            } else {
                diagnoses.clear();
            }
        } catch (Exception e) {
            log.error("Failed to load medical records for patient ID: {}", patientId, e);
            DialogUtil.showExceptionDialog("Load Error", "Could not load medical history.",
                    "Failed to retrieve medical records for patient.", e);
        }
    }

    private void loadDiagnosesForRecord(UUID recordId) {
        try {
            List<Diagnosis> recordDiagnoses = diagnosisRepository.findByMedicalRecord_RecordId(recordId);
            diagnoses.setAll(recordDiagnoses);
        } catch (Exception e) {
            log.error("Failed to load diagnoses for medical record ID: {}", recordId, e);
            DialogUtil.showExceptionDialog("Load Error", "Could not load diagnoses.",
                    "Failed to retrieve diagnoses for selected medical record.", e);
        }
    }

    @FXML
    private void loadPatientBookAppointment() {
        uiManager.switchToPatientBookAppointment(patientId);
    }
    
    @FXML
    private void loadPatientViewPrescriptions() {
        uiManager.switchToPatientViewPrescriptions();
    }

    @FXML 
    private void loadPatientMedicalHistory() {
        uiManager.switchToPatientMedicalHistory();
    }

    @FXML
    private void loadPatientUpdateProfile() {
        uiManager.switchToPatientUpdateProfile();
    }
    @FXML 
    private void loadPatientReview() {
        uiManager.switchToPatientReview();
    }

    @FXML 
    private void loadPatientViewBills() {
        uiManager.switchToPatientViewBills();
    }
}