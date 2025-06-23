package com.pma.controller.doctor;

import com.pma.model.entity.Patient;
import com.pma.service.PatientService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class DoctorViewPatientsController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(DoctorViewPatientsController.class);

    private final PatientService patientService;

    @FXML
    private Button doctorViewPatientsButton;

    @FXML
    private Button doctorMedicalRecordsButton;

    @FXML
    private Button doctorPrescribeButton;

    @FXML
    private Button doctorBookAppointmentButton;
    
    @FXML
    private TableView<Patient> patientsTable;
    @FXML
    private TableColumn<Patient, String> fullNameColumn;
    @FXML
    private TableColumn<Patient, LocalDate> dateOfBirthColumn;
    @FXML
    private TableColumn<Patient, String> genderColumn;
    @FXML
    private TableColumn<Patient, String> phoneColumn;
    @FXML
    private TableColumn<Patient, String> emailColumn;
    @FXML
    private TableColumn<Patient, String> addressLine1Column;
    @FXML
    private TableColumn<Patient, String> addressLine2Column;
    @FXML
    private TableColumn<Patient, String> cityColumn;
    @FXML
    private TableColumn<Patient, String> stateProvinceColumn;
    @FXML
    private TableColumn<Patient, String> postalCodeColumn;
    @FXML
    private TableColumn<Patient, String> countryColumn;
    @FXML
    private TableColumn<Patient, String> bloodTypeColumn;
    @FXML
    private TableColumn<Patient, String> allergiesColumn;
    @FXML
    private TableColumn<Patient, String> medicalHistoryColumn;
    @FXML
    private TableColumn<Patient, String> insuranceNumberColumn;
    @FXML
    private TableColumn<Patient, String> emergencyContactNameColumn;
    @FXML
    private TableColumn<Patient, String> emergencyContactPhoneColumn;
    @FXML
    private TableColumn<Patient, LocalDateTime> createdAtColumn;
    @FXML
    private TableColumn<Patient, LocalDateTime> updatedAtColumn;

    private ObservableList<Patient> patientList = FXCollections.observableArrayList();
    @Autowired
    private UIManager uiManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        loadPatientsData();
    }

    private void setupTableColumns() {
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        dateOfBirthColumn.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        addressLine1Column.setCellValueFactory(new PropertyValueFactory<>("addressLine1"));
        addressLine2Column.setCellValueFactory(new PropertyValueFactory<>("addressLine2"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        stateProvinceColumn.setCellValueFactory(new PropertyValueFactory<>("stateProvince"));
        postalCodeColumn.setCellValueFactory(new PropertyValueFactory<>("postalCode"));
        countryColumn.setCellValueFactory(new PropertyValueFactory<>("country"));
        bloodTypeColumn.setCellValueFactory(new PropertyValueFactory<>("bloodType"));
        allergiesColumn.setCellValueFactory(new PropertyValueFactory<>("allergies"));
        medicalHistoryColumn.setCellValueFactory(new PropertyValueFactory<>("medicalHistory"));
        insuranceNumberColumn.setCellValueFactory(new PropertyValueFactory<>("insuranceNumber"));
        emergencyContactNameColumn.setCellValueFactory(new PropertyValueFactory<>("emergencyContactName"));
        emergencyContactPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("emergencyContactPhone"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        patientsTable.setItems(patientList);
    }

    private void loadPatientsData() {
        log.info("Loading patient data...");

        javafx.concurrent.Task<List<Patient>> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected List<Patient> call() throws Exception {
                return patientService.getAllPatients();
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<Patient> patients = loadTask.getValue();
            patientList.setAll(patients);
            log.info("Loaded {} patients.", patients.size());
        });

        loadTask.setOnFailed(event -> {
            Throwable e = loadTask.getException();
            log.error("Error loading patient data: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Load Error", "Unable to load patient list. Please try again.");
            patientList.clear();
        });

        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
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