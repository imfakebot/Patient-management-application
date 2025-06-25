// package com.pma.controller;

// import com.pma.model.entity.Appointment;
// import com.pma.model.entity.Doctor;
// import com.pma.model.entity.Patient;
// import com.pma.service.AppointmentService;
// import com.pma.service.PatientService;
// import javafx.beans.property.SimpleObjectProperty;
// import javafx.beans.property.SimpleStringProperty;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.event.ActionEvent;
// import javafx.fxml.FXML;
// import javafx.fxml.FXMLLoader;
// import javafx.scene.Node;
// import javafx.scene.Parent;
// import javafx.scene.Scene;
// import javafx.scene.control.Button;
// import javafx.scene.control.Label;
// import javafx.scene.control.TableColumn;
// import javafx.scene.control.TableView;
// import javafx.scene.layout.VBox;
// import javafx.stage.Stage;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.ApplicationContext;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import org.springframework.data.domain.Sort;
// import org.springframework.stereotype.Controller;

// import java.time.LocalDateTime;
// import java.util.UUID;

// /**
//  * Controller for patient_medical_history.fxml.
//  * Displays a patient's appointment history and basic details, with sidebar navigation.
//  */
// @Controller
// public class PatientMedicalHistoryController {

//     private static final Logger log = LoggerFactory.getLogger(PatientMedicalHistoryController.class);

//     @FXML
//     private Label patientNameLabel;

//     @FXML
//     private TableView<Appointment> appointmentTable;

//     @FXML
//     private TableColumn<Appointment, LocalDateTime> dateTimeColumn;

//     @FXML
//     private TableColumn<Appointment, String> doctorNameColumn;

//     @FXML
//     private TableColumn<Appointment, String> reasonColumn;

//     @FXML
//     private TableColumn<Appointment, String> statusColumn;

//     @FXML
//     private Button refreshButton;

//     @FXML
//     private Button backButton;

//     @FXML
//     private VBox sidebar;

//     @FXML
//     private Button homeButton;

//     @FXML
//     private Button appointmentsButton;

//     @FXML
//     private Button patientsButton;

//     private final PatientService patientService;
//     private final AppointmentService appointmentService;
//     private final ApplicationContext applicationContext;
//     private UUID patientId;
//     private final ObservableList<Appointment> appointmentList = FXCollections.observableArrayList();

//     @Autowired
//     public PatientMedicalHistoryController(PatientService patientService, AppointmentService appointmentService,
//                                           ApplicationContext applicationContext) {
//         this.patientService = patientService;
//         this.appointmentService = appointmentService;
//         this.applicationContext = applicationContext;
//     }

//     /**
//      * Sets the patient ID and initializes the view.
//      *
//      * @param patientId UUID of the patient whose history is to be displayed.
//      */
//     public void setPatientId(UUID patientId) {
//         this.patientId = patientId;
//         initializeView();
//     }

//     /**
//      * Initializes the table columns and loads patient data.
//      */
//     @FXML
//     public void initialize() {
//         setupTableColumns();
//         // Data loading is triggered after setPatientId is called
//     }

//     private void setupTableColumns() {
//         dateTimeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAppointmentDatetime()));
//         doctorNameColumn.setCellValueFactory(cellData -> {
//             Doctor doctor = cellData.getValue().getDoctor();
//             return new SimpleStringProperty(doctor != null ? doctor.getFullName() : "Unassigned");
//         });
//         reasonColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getReason()));
//         statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().toString()));
//     }

//     private void initializeView() {
//         if (patientId == null) {
//             log.error("Patient ID is not set. Cannot load medical history.");
//             patientNameLabel.setText("Error: Patient ID not provided");
//             return;
//         }

//         try {
//             // Load patient details
//             Patient patient = patientService.getPatientById(patientId);
//             patientNameLabel.setText(patient.getFullName());

//             // Load appointment history
//             loadAppointments();
//         } catch (Exception e) {
//             log.error("Failed to initialize patient medical history for patientId: {}", patientId, e);
//             patientNameLabel.setText("Error loading patient data");
//         }
//     }

//     private void loadAppointments() {
//         appointmentList.clear();
//         try {
//             // Fetch appointments with pagination (first page, 100 records, sorted by date descending)
//             Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "appointmentDatetime"));
//             Page<Appointment> appointmentPage = appointmentService.getAppointmentsByPatient(patientId, pageable);
//             appointmentList.addAll(appointmentPage.getContent());
//             appointmentTable.setItems(appointmentList);
//             log.info("Loaded {} appointments for patientId: {}", appointmentList.size(), patientId);
//         } catch (Exception e) {
//             log.error("Failed to load appointments for patientId: {}", patientId, e);
//             appointmentTable.setItems(FXCollections.observableArrayList());
//         }
//     }

//     @FXML
//     private void handleRefreshButton(ActionEvent event) {
//         log.info("Refresh button clicked for patientId: {}", patientId);
//         loadAppointments();
//     }

//     @FXML
//     private void handleBackButton(ActionEvent event) {
//         log.info("Back button clicked for patientId: {}", patientId);
//         Stage stage = (Stage) backButton.getScene().getWindow();
//         stage.close(); // Close the current window
//     }

//     @FXML
//     private void handleHomeButton(ActionEvent event) {
//         log.info("Home button clicked");
//         navigateTo("/fxml/home.fxml", event);
//     }

//     @FXML
//     private void handleAppointmentsButton(ActionEvent event) {
//         log.info("Appointments button clicked");
//         navigateTo("/fxml/appointments.fxml", event);
//     }

//     @FXML
//     private void handlePatientsButton(ActionEvent event) {
//         log.info("Patients button clicked");
//         navigateTo("/fxml/patients.fxml", event);
//     }

//     private void navigateTo(String fxmlPath, ActionEvent event) {
//         try {
//             FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
//             loader.setControllerFactory(applicationContext::getBean);
//             Parent root = loader.load();
//             Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
//             stage.setScene(new Scene(root));
//             stage.show();
//             log.info("Navigated to {}", fxmlPath);
//         } catch (Exception e) {
//             log.error("Failed to navigate to {}", fxmlPath, e);
//         }
//     }
// }
