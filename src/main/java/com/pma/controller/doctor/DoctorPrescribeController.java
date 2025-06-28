package com.pma.controller.doctor;

import com.pma.model.entity.Diagnosis;
import com.pma.model.entity.Disease;
import com.pma.model.entity.Doctor;
import com.pma.model.entity.Medicine;
import com.pma.model.entity.Patient;
import com.pma.model.entity.Prescription;
import com.pma.model.entity.PrescriptionDetail;
import com.pma.model.enums.PrescriptionStatus;
import com.pma.service.DoctorService;
import com.pma.service.DiagnosisService;
import com.pma.service.MedicineService;
import com.pma.service.PatientService;
import com.pma.service.PrescriptionService;
import com.pma.service.PrescriptionService.PrescriptionDetailDTO;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import com.pma.util.UIManager;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

@Controller
public class DoctorPrescribeController {

    private static final Logger log = LoggerFactory.getLogger(DoctorPrescribeController.class);

    @FXML
    private Button doctorViewPatientsButton;

    @FXML
    private Button doctorMedicalRecordsButton;

    @FXML
    private Button doctorPrescribeButton;

    @FXML
    private Button doctorBookAppointmentButton;

    @FXML
    private ComboBox<Patient> patientCombo;
    @FXML
    private DatePicker prescriptionDatePicker;
    @FXML
    private TextArea notesField;
    @FXML
    private ComboBox<PrescriptionStatus> statusCombo;
    @FXML
    private ComboBox<Medicine> medicineCombo;
    @FXML
    private TextField quantityField;
    @FXML
    private TextField unitPriceField;
    @FXML
    private TextField dosageField;
    @FXML
    private TextArea instructionsField;
    @FXML
    private ComboBox<Disease> diseaseCombo; // New FXML component for disease selection
    @FXML
    private Button prescribeButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button clearButton;
    @FXML
    private TableView<Prescription> prescriptionsTable;
    @FXML
    private TableColumn<Prescription, String> patientColumn;
    @FXML
    private TableColumn<Prescription, LocalDate> prescriptionDateColumn;
    @FXML
    private TableColumn<Prescription, String> notesColumn;
    @FXML
    private TableColumn<Prescription, PrescriptionStatus> statusColumn;
    @FXML
    private TableColumn<Prescription, LocalDateTime> createdAtColumn;
    @FXML
    private TableColumn<Prescription, LocalDateTime> updatedAtColumn;

    private final PrescriptionService prescriptionService;
    private final PatientService patientService;
    private final MedicineService medicineService;
    private final DiagnosisService diagnosisService; // Inject DiagnosisService
    private final DoctorService doctorService;

    private Doctor currentDoctor;
    private Prescription selectedPrescription;
    @Autowired
    private UIManager uiManager;

    @Autowired
    public DoctorPrescribeController(
            PrescriptionService prescriptionService,
            PatientService patientService,
            MedicineService medicineService,
            DoctorService doctorService,
            DiagnosisService diagnosisService) { // Add DiagnosisService to constructor
        this.prescriptionService = prescriptionService;
        this.patientService = patientService;
        this.medicineService = medicineService;
        this.diagnosisService = diagnosisService; // Assign it
        this.doctorService = doctorService;
    }

    @FXML
    public void initialize() {
        log.info("Initializing DoctorPrescribeController");

        // Load current doctor
        loadCurrentDoctor();

        // Initialize ComboBoxes
        loadPatients();
        loadMedicines();
        loadDiseases(); // Load diseases for the new combo box
        loadStatusOptions();

        // Initialize Prescriptions TableView
        patientColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getPatient().getFullName()));
        prescriptionDateColumn
                .setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPrescriptionDate()));
        notesColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNotes()));
        statusColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStatus()));
        createdAtColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCreatedAt()));
        updatedAtColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getUpdatedAt()));

        // Load prescriptions table
        loadPrescriptions();

        // Add listener for table selection
        prescriptionsTable.getSelectionModel().selectedItemProperty().addListener((_, _, newSelection) -> {
            if (newSelection != null) {
                selectedPrescription = newSelection;
                populateForm(newSelection);
                updateButton.setDisable(false);
            } else {
                selectedPrescription = null;
                updateButton.setDisable(true);
                clearForm(null);
            }
        });

        // Add listener for medicineCombo to auto-fill unitPriceField
        medicineCombo.getSelectionModel().selectedItemProperty().addListener((_, _, newMedicine) -> {
            if (newMedicine != null) {
                unitPriceField.setText(newMedicine.getPrice().toString());
                log.debug("Auto-filled unit price for medicine {}: {}", newMedicine.getMedicineName(),
                        newMedicine.getPrice());
            } else {
                unitPriceField.clear();
            }
        });

        // Disable update button initially
        updateButton.setDisable(true);
    }

    private void loadCurrentDoctor() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (currentUsername != null && !currentUsername.equals("anonymousUser")) {
            doctorService.findDoctorByEmail(currentUsername).ifPresent(doctor -> {
                this.currentDoctor = doctor;
                log.info("Current doctor loaded: {}", currentDoctor.getFullName());
            });
        }
        if (currentDoctor == null) {
            log.error("No doctor found for current user. Cannot proceed with prescribing.");
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy bác sĩ liên kết với người dùng hiện tại.");
            throw new IllegalStateException("No doctor associated with current user.");
        }
    }

    private void loadPatients() {
        List<Patient> patients = patientService.findAll();
        ObservableList<Patient> patientList = FXCollections.observableArrayList(patients);
        patientCombo.setItems(patientList);
        patientCombo.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                setText(empty ? null : patient.getFullName());
            }
        });
        patientCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                setText(empty ? null : patient.getFullName());
            }
        });
    }

    private void loadMedicines() {
        List<Medicine> medicines = medicineService.searchMedicinesByName("");
        ObservableList<Medicine> medicineList = FXCollections.observableArrayList(medicines);
        medicineCombo.setItems(medicineList);
        medicineCombo.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Medicine medicine, boolean empty) {
                super.updateItem(medicine, empty);
                setText(empty ? null : medicine.getMedicineName());
            }
        });
        medicineCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Medicine medicine, boolean empty) {
                super.updateItem(medicine, empty);
                setText(empty ? null : medicine.getMedicineName());
            }
        });
    }

    private void loadStatusOptions() {
        ObservableList<PrescriptionStatus> statusList = FXCollections.observableArrayList(PrescriptionStatus.values());
        statusCombo.setItems(statusList);
    }

    /**
     * Loads diseases into the diseaseCombo.
     */
    private void loadDiseases() {
        List<Disease> diseases = diagnosisService.getAllDiseases(); // Assuming DiagnosisService has getAllDiseases
        ObservableList<Disease> diseaseList = FXCollections.observableArrayList(diseases);
        diseaseCombo.setItems(diseaseList);
        diseaseCombo.setPromptText("Chọn Bệnh");
        diseaseCombo.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Disease disease, boolean empty) {
                super.updateItem(disease, empty);
                setText(empty ? null : disease.getDiseaseName() + " (" + disease.getDiseaseCode() + ")");
            }
        });
        diseaseCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Disease disease, boolean empty) {
                super.updateItem(disease, empty);
                setText(empty ? null : disease.getDiseaseName() + " (" + disease.getDiseaseCode() + ")");
            }
        });
    }

    private void loadPrescriptions() {
        if (currentDoctor != null) {
            List<Prescription> prescriptions = prescriptionService.getPrescriptionsByDoctor(currentDoctor.getDoctorId());
            prescriptionsTable.setItems(FXCollections.observableArrayList(prescriptions));
        } else {
            prescriptionsTable.getItems().clear();
            log.warn("Cannot load prescriptions because currentDoctor is null.");
        }
    }

    @FXML
    private void prescribe(ActionEvent event) {
        try {
            // 1. Validate and gather data from the form
            if (patientCombo.getValue() == null) {
                throw new IllegalArgumentException("Phải chọn bệnh nhân.");
            }
            if (prescriptionDatePicker.getValue() == null) {
                throw new IllegalArgumentException("Phải chọn ngày kê đơn.");
            }
            if (statusCombo.getValue() == null) {
                throw new IllegalArgumentException("Phải chọn trạng thái.");
            }

            // Create a temporary Prescription object to hold form data
            Prescription prescriptionData = new Prescription();
            prescriptionData.setPrescriptionDate(prescriptionDatePicker.getValue());
            prescriptionData.setNotes(notesField.getText());
            prescriptionData.setStatus(statusCombo.getValue());

            // Create DTO for prescription details
            PrescriptionDetailDTO detail = new PrescriptionDetailDTO();
            validateAndPopulateDetail(detail);

            // 2. Call the service with IDs and DTOs
            Prescription savedPrescription = prescriptionService.createPrescription(
                    prescriptionData,
                    patientCombo.getValue().getPatientId(),
                    currentDoctor.getDoctorId(),
                    null, // medicalRecordId
                    Collections.singletonList(detail));

            // 4. Create Diagnosis if a disease is selected
            Disease selectedDisease = diseaseCombo.getValue();
            if (selectedDisease != null) {
                Diagnosis newDiagnosis = new Diagnosis();
                newDiagnosis.setDiagnosisDate(LocalDate.now()); // Or from a new DatePicker
                newDiagnosis.setDiagnosisDescription(notesField.getText()); // Reuse notes or add a new field
                newDiagnosis.setStatus(com.pma.model.enums.DiagnosisStatus.Active); // Default status

                diagnosisService.createDiagnosis(
                        newDiagnosis,
                        savedPrescription.getMedicalRecord().getRecordId(), // Get the ID of the newly created MedicalRecord
                        selectedDisease.getDiseaseCode());
                log.info("Diagnosis created for medical record: {} with disease: {}", savedPrescription.getMedicalRecord().getRecordId(), selectedDisease.getDiseaseName());
            }
            // 3. Update UI
            prescriptionsTable.getItems().add(savedPrescription);
            clearForm(null);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo đơn thuốc thành công.");
            log.info("Prescription created for patient: {}", savedPrescription.getPatient().getFullName());
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            log.error("Failed to create prescription: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void update(ActionEvent event) {
        if (selectedPrescription == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Chưa chọn đơn thuốc nào.");
            return;
        }
        try {
            // Create a temporary Prescription object to hold form data
            Prescription prescriptionUpdateData = new Prescription();
            populatePrescriptionFromForm(prescriptionUpdateData); // Populate with date, notes, status

            // Create DTO for prescription details from the form
            PrescriptionDetailDTO detail = new PrescriptionDetailDTO();
            validateAndPopulateDetail(detail);

            // Call the new update service method
            Prescription updatedPrescription = prescriptionService.updatePrescription(
                    selectedPrescription.getPrescriptionId(),
                    prescriptionUpdateData,
                    Collections.singletonList(detail));

            // Update UI
            int index = prescriptionsTable.getItems().indexOf(selectedPrescription);
            if (index != -1) {
                prescriptionsTable.getItems().set(index, updatedPrescription);
            } else {
                loadPrescriptions(); // Fallback to reload all if item not found
            }
            clearForm(null);
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật trạng thái đơn thuốc thành công.");
            log.info("Prescription updated for patient: {}", updatedPrescription.getPatient().getFullName());
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            log.error("Failed to update prescription: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void clearForm(ActionEvent event) {
        patientCombo.getSelectionModel().clearSelection();
        prescriptionDatePicker.setValue(null);
        notesField.clear();
        statusCombo.getSelectionModel().clearSelection();
        medicineCombo.getSelectionModel().clearSelection();
        diseaseCombo.getSelectionModel().clearSelection(); // Clear disease selection
        quantityField.clear();
        unitPriceField.clear();
        dosageField.clear();
        instructionsField.clear();
        selectedPrescription = null;
        updateButton.setDisable(true);
    }

    /**
     * Populates a Prescription object from the form fields. Does not handle
     * entity associations. This is used for both creating a new prescription
     * data object and updating an existing one.
     *
     * @param prescription The prescription object to populate.
     */
    private void populatePrescriptionFromForm(Prescription prescription) {
        if (patientCombo.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn bệnh nhân.");
        }
        if (prescriptionDatePicker.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn ngày kê đơn.");
        }
        if (statusCombo.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn trạng thái.");
        }
        prescription.setPrescriptionDate(prescriptionDatePicker.getValue());
        prescription.setNotes(notesField.getText());
        prescription.setStatus(statusCombo.getValue());
    }

    private void validateAndPopulateDetail(PrescriptionDetailDTO detail) {
        if (medicineCombo.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn thuốc.");
        }
        if (quantityField.getText().isBlank() || !quantityField.getText().matches("\\d+")) {
            throw new IllegalArgumentException("Số lượng phải là số hợp lệ.");
        }
        if (dosageField.getText().isBlank()) {
            throw new IllegalArgumentException("Liều lượng không được để trống.");
        }
        if (unitPriceField.getText().isBlank() || !unitPriceField.getText().matches("\\d+(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Giá đơn vị phải là số hợp lệ với tối đa 2 chữ số thập phân.");
        }

        detail.setMedicineId(medicineCombo.getValue().getMedicineId());
        detail.setQuantity(Integer.parseInt(quantityField.getText()));
        detail.setDosage(dosageField.getText());
        detail.setInstructions(instructionsField.getText());
    }

    private void populateForm(Prescription prescription) {
        patientCombo.setValue(prescription.getPatient());
        prescriptionDatePicker.setValue(prescription.getPrescriptionDate());
        notesField.setText(prescription.getNotes());
        statusCombo.setValue(prescription.getStatus());
        diseaseCombo.setValue(null); // Clear disease selection when populating form from existing prescription
        // Populate the first PrescriptionDetail if available
        try {
            Set<PrescriptionDetail> details = prescription.getPrescriptionDetailsView();
            if (details != null && !details.isEmpty()) {
                PrescriptionDetail detail = details.iterator().next(); // Lấy chi tiết đầu tiên
                Medicine medicine = detail.getMedicine();
                medicineCombo.setValue(medicine);
                quantityField.setText(String.valueOf(detail.getQuantity()));
                dosageField.setText(detail.getDosage());
                instructionsField.setText(detail.getInstructions());
                unitPriceField.setText(detail.getUnitPrice().toString());
            } else {
                medicineCombo.getSelectionModel().clearSelection();
                quantityField.clear();
                dosageField.clear();
                instructionsField.clear();
                unitPriceField.clear();
            }
        } catch (org.hibernate.LazyInitializationException e) {
            log.error("Failed to load prescription details for prescription id: {}. Lazy initialization error: {}",
                    prescription.getPrescriptionId(), e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải chi tiết đơn thuốc do lỗi dữ liệu.");
            medicineCombo.getSelectionModel().clearSelection();
            quantityField.clear();
            dosageField.clear();
            instructionsField.clear();
            unitPriceField.clear();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void loadDoctorViewPatients(ActionEvent event) {
        uiManager.switchToDoctorViewPatients();
    }

    @FXML
    private void loadDoctorMedicalRecords(ActionEvent event) {
        uiManager.switchToDoctorMedicalRecords();
    }

    @FXML
    private void loadDoctorPrescribe(ActionEvent event) {
        uiManager.switchToDoctorPrescribe();
    }

    @FXML
    private void loadDoctorBookAppointment(ActionEvent event) {
        uiManager.switchToDoctorBookAppointment();
    }
}
