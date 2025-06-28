package com.pma.controller.doctor;

import com.pma.model.entity.Diagnosis;
import com.pma.model.entity.Doctor;
import com.pma.model.entity.MedicalRecord;
import com.pma.model.enums.DiagnosisStatus;
import com.pma.repository.MedicalRecordRepository;
import com.pma.service.DiagnosisService;
import com.pma.service.UserAccountService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import com.pma.util.UIManager;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.springframework.data.domain.Page;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
@Component
public class DoctorMedicalRecordsController implements Initializable {

    // --- FXML Components ---
    @FXML
    private Button doctorViewPatientsButton;

    @FXML
    private Button doctorMedicalRecordsButton;

    @FXML
    private Button doctorPrescribeButton;

    @FXML
    private Button doctorBookAppointmentButton;

    @FXML
    private TableView<MedicalRecord> medicalRecordsTable;
    @FXML
    private TableColumn<MedicalRecord, String> patientColumn;
    @FXML
    private TableColumn<MedicalRecord, LocalDate> recordDateColumn;
    @FXML
    private TableColumn<MedicalRecord, String> appointmentColumn;
    @FXML
    private TableColumn<MedicalRecord, String> notesColumn;
    @FXML
    private TableColumn<MedicalRecord, LocalDateTime> createdAtColumn;
    @FXML
    private TableColumn<MedicalRecord, LocalDateTime> updatedAtColumn;

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
    private TableColumn<Diagnosis, DiagnosisStatus> statusColumn;
    @FXML
    private TableColumn<Diagnosis, LocalDateTime> createdAtColumn1;
    @FXML
    private TableColumn<Diagnosis, LocalDateTime> updatedAtColumn1;

    // --- Repositories and Services ---
    @Autowired
    private MedicalRecordRepository medicalRecordRepository;
    @Autowired
    private DiagnosisService diagnosisService;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private UIManager uiManager;

    // --- Observable Lists for Tables ---
    private final ObservableList<MedicalRecord> medicalRecordsList = FXCollections.observableArrayList();
    private final ObservableList<Diagnosis> diagnosesList = FXCollections.observableArrayList();

    // --- Doctor hiện tại ---
    private Doctor currentDoctor;

    // --- Pagination Controls ---
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Label pageInfoLabel;
    private int currentPage = 0;
    private final int pageSize = 10; // Số lượng bản ghi trên mỗi trang
    // --- DateTimeFormatter để định dạng ngày giờ ---
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Lấy Doctor hiện tại từ Spring Security
        setCurrentDoctorFromSecurityContext();

        // Thiết lập các cột cho medicalRecordsTable
        setupMedicalRecordsTable();

        // Thiết lập các cột cho diagnosesTable
        setupDiagnosesTable();

        // Load dữ liệu ban đầu
        setupPaginationControls();
        loadMedicalRecords();

        // Listener để load diagnoses khi chọn một MedicalRecord
        medicalRecordsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        loadDiagnosesForMedicalRecord(newSelection);
                    } else {
                        diagnosesList.clear(); // Xóa danh sách diagnoses nếu không có MedicalRecord được chọn
                    }
                });
    }

    private void setupPaginationControls() {
        prevPageButton.setOnAction(event -> {
            if (currentPage > 0) {
                currentPage--;
                loadMedicalRecords();
            }
        });

        nextPageButton.setOnAction(event -> {
            currentPage++;
            loadMedicalRecords();
        });
    }

    private void updatePaginationButtons(Page<MedicalRecord> page) {
        prevPageButton.setDisable(!page.hasPrevious());
        nextPageButton.setDisable(!page.hasNext());
        pageInfoLabel.setText(String.format("Trang %d/%d (%d bản ghi)",
                page.getNumber() + 1, page.getTotalPages(), page.getTotalElements()));
        medicalRecordsTable.setItems(FXCollections.observableArrayList(page.getContent()));
    }

    private void setCurrentDoctorFromSecurityContext() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userAccountService.findByUsername(username).ifPresent(userAccount -> {
            this.currentDoctor = userAccount.getDoctor();
            if (currentDoctor == null) {
                System.err.println("No Doctor linked to user: " + username);
            }
        });
    }

    private void setupMedicalRecordsTable() {
        // Cột Bệnh nhân: Hiển thị tên bệnh nhân
        patientColumn.setCellValueFactory(cellData -> {
            MedicalRecord record = cellData.getValue();
            return new SimpleStringProperty(record.getPatient() != null
                    ? record.getPatient().getFullName()
                    : "N/A");
        });

        // Cột Ngày ghi
        recordDateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getRecordDate()));

        // Cột Lịch hẹn: Hiển thị ngày giờ cuộc hẹn hoặc "N/A"
        appointmentColumn.setCellValueFactory(cellData -> {
            MedicalRecord record = cellData.getValue();
            return new SimpleStringProperty(record.getAppointment() != null
                    ? record.getAppointment().getAppointmentDatetime().format(DATE_TIME_FORMATTER)
                    : "N/A");
        });

        // Cột Ghi chú
        notesColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNotes()));

        // Cột Ngày tạo
        createdAtColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCreatedAt()));

        // Cột Ngày cập nhật
        updatedAtColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getUpdatedAt()));

        // Gán danh sách dữ liệu
        medicalRecordsTable.setItems(medicalRecordsList);
    }

    private void setupDiagnosesTable() {
        // Cột Mã bệnh
        diseaseCodeColumn.setCellValueFactory(cellData -> {
            Diagnosis diagnosis = cellData.getValue();
            return new SimpleStringProperty(diagnosis.getDisease() != null
                    ? diagnosis.getDisease().getDiseaseCode()
                    : "N/A");
        });

        // Cột Tên bệnh
        diseaseNameColumn.setCellValueFactory(cellData -> {
            Diagnosis diagnosis = cellData.getValue();
            return new SimpleStringProperty(diagnosis.getDisease() != null
                    ? diagnosis.getDisease().getDiseaseName()
                    : "N/A");
        });

        // Cột Mô tả
        diagnosisDescriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDiagnosisDescription()));

        // Cột Ngày chẩn đoán
        diagnosisDateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDiagnosisDate()));

        // Cột Trạng thái
        statusColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStatus()));

        // Cột Ngày tạo
        createdAtColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCreatedAt()));

        // Cột Ngày cập nhật
        updatedAtColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getUpdatedAt()));

        // Gán danh sách dữ liệu
        diagnosesTable.setItems(diagnosesList);
    }

    private void loadMedicalRecords() {
        if (currentDoctor != null) {
            Pageable pageable = PageRequest.of(currentPage, pageSize);
            Page<MedicalRecord> page = medicalRecordRepository.findByDoctorWithDetails(currentDoctor, pageable);
            medicalRecordsList.setAll(page.getContent());
            updatePaginationButtons(page);
        } else {
            updatePaginationButtons(Page.empty()); // Cập nhật nút khi không có bác sĩ
            medicalRecordsList.clear();
        }
    }

    private void loadDiagnosesForMedicalRecord(MedicalRecord medicalRecord) {
        if (medicalRecord != null) {
            List<Diagnosis> diagnoses = diagnosisService.getDiagnosesByMedicalRecord(medicalRecord.getRecordId());
            diagnosesList.setAll(diagnoses);
        } else {
            diagnosesList.clear();
        }
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
