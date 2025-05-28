package com.pma.controller;

import com.pma.model.entity.Appointment;
import com.pma.model.entity.Doctor;
import com.pma.model.entity.Patient;
import com.pma.model.enums.AppointmentStatus;
import com.pma.service.AppointmentService;
import com.pma.service.BillService;
import com.pma.service.DoctorService;
import com.pma.service.PatientService;
import jakarta.persistence.EntityNotFoundException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * Controller cho giao diện đặt lịch hẹn của bệnh nhân
 * (patient_book_appointment.fxml).
 * Quản lý đặt lịch hẹn và điều hướng qua sidebar.
 */
@Controller
public class PatientBookAppointmentController {

    private static final Logger log = LoggerFactory.getLogger(PatientBookAppointmentController.class);

    // Các FXML components cho đặt lịch
    @FXML
    private ComboBox<Doctor> doctorCombo;
    @FXML
    private DatePicker appointmentDatePicker;
    @FXML
    private TextField appointmentTimeField;
    @FXML
    private TextArea reasonField;
    @FXML
    private ComboBox<String> appointmentTypeCombo;
    @FXML
    private Button bookButton;
    @FXML
    private Button clearButton;

    // Các nút trong Sidebar
    @FXML
    private Button patientBookAppointmentButton;
    @FXML
    private Button patientViewPrescriptionsButton;
    @FXML
    private Button patientMedicalHistoryButton;
    @FXML
    private Button patientUpdateProfileButton;
    @FXML
    private Button patientReviewButton;
    @FXML
    private Button patientViewBillsButton;

    // Services được inject
    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final BillService billService;

    // Spring ApplicationContext để tạo controller
    private final ApplicationContext applicationContext;

    // ID của bệnh nhân
    private final UUID patientId;

    // Formatter cho thời gian
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public PatientBookAppointmentController(AppointmentService appointmentService,
            DoctorService doctorService,
            PatientService patientService,
            BillService billService,
            ApplicationContext applicationContext,
            UUID patientId) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.billService = billService;
        this.applicationContext = applicationContext;
        this.patientId = patientId;
    }

    /**
     * Khởi tạo controller, điền danh sách bác sĩ và loại cuộc hẹn.
     */
    @FXML
    public void initialize() {
        log.info("Initializing PatientBookAppointmentController for patientId: {}", patientId);

        // Điền danh sách bác sĩ vào ComboBox
        try {
            List<Doctor> doctors = doctorService.getAllDoctors();
            ObservableList<Doctor> doctorList = FXCollections.observableArrayList(doctors);
            doctorCombo.setItems(doctorList);
            doctorCombo.setCellFactory(param -> new ListCell<Doctor>() {
                @Override
                protected void updateItem(Doctor doctor, boolean empty) {
                    super.updateItem(doctor, empty);
                    setText(empty || doctor == null ? null : doctor.getFullName() + " (" + doctor.getSpecialty() + ")");
                }
            });
            doctorCombo.setButtonCell(new ListCell<Doctor>() {
                @Override
                protected void updateItem(Doctor doctor, boolean empty) {
                    super.updateItem(doctor, empty);
                    setText(empty || doctor == null ? null : doctor.getFullName() + " (" + doctor.getSpecialty() + ")");
                }
            });
        } catch (Exception e) {
            log.error("Failed to load doctors into ComboBox", e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách bác sĩ: " + e.getMessage());
        }

        // Điền danh sách loại cuộc hẹn
        ObservableList<String> appointmentTypes = FXCollections.observableArrayList(
                "Khám lần đầu", "Tái khám", "Tư vấn", "Kiểm tra định kỳ");
        appointmentTypeCombo.setItems(appointmentTypes);

        // Đánh dấu nút hiện tại là active (tùy chọn, có thể dùng CSS)
        patientBookAppointmentButton.setStyle("-fx-background-color: #4CAF50;"); // Ví dụ
    }

    /**
     * Xử lý nút "Đặt lịch".
     */
    @FXML
    private void bookAppointment(ActionEvent event) {
        log.info("Book button clicked for patientId: {}", patientId);

        try {
            LocalDate appointmentDate = appointmentDatePicker.getValue();
            String timeInput = appointmentTimeField.getText();
            String reason = reasonField.getText();
            String appointmentType = appointmentTypeCombo.getValue();
            Doctor selectedDoctor = doctorCombo.getValue();

            if (appointmentDate == null) {
                throw new IllegalArgumentException("Vui lòng chọn ngày hẹn.");
            }
            if (timeInput == null || timeInput.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập giờ hẹn.");
            }
            if (reason == null || reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do hẹn.");
            }
            if (appointmentType == null) {
                throw new IllegalArgumentException("Vui lòng chọn loại cuộc hẹn.");
            }

            LocalTime appointmentTime;
            try {
                appointmentTime = LocalTime.parse(timeInput, TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Định dạng giờ không hợp lệ, sử dụng HH:mm (ví dụ: 14:30).");
            }

            LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);
            if (appointmentDateTime.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Thời gian hẹn phải trong tương lai.");
            }

            Appointment appointment = new Appointment();
            appointment.setAppointmentDatetime(appointmentDateTime);
            appointment.setReason(reason);
            appointment.setAppointmentType(appointmentType);
            appointment.setStatus(AppointmentStatus.Scheduled);

            UUID doctorId = selectedDoctor != null ? selectedDoctor.getDoctorId() : null;
            Appointment savedAppointment = appointmentService.scheduleAppointment(appointment, patientId, doctorId);

            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đặt lịch hẹn thành công! Mã cuộc hẹn: " + savedAppointment.getAppointmentId());
            closeWindow();

        } catch (IllegalArgumentException e) {
            log.warn("Booking failed: {}", e.getMessage());
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", e.getMessage());
        } catch (EntityNotFoundException e) {
            log.error("Booking failed: {}", e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during booking", e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Đã xảy ra lỗi: " + e.getMessage());
        }
    }

    /**
     * Xử lý nút "Hủy".
     */
    @FXML
    private void clearForm(ActionEvent event) {
        log.info("Cancel button clicked, closing booking window");
        closeWindow();
    }

    /**
     * Xử lý nút "Xem đơn thuốc".
     */
    @FXML
    private void loadPatientViewPrescriptions(ActionEvent event) {
        log.info("View Prescriptions button clicked for patientId: {}", patientId);
        loadPage("@../resources/com/pma/fxml/patient_view_prescriptions.fxml", "Danh sách đơn thuốc");
    }

    /**
     * Xử lý nút "Xem hồ sơ y tế".
     */
    @FXML
    private void loadPatientMedicalHistory(ActionEvent event) {
        log.info("View Medical Records button clicked for patientId: {}", patientId);
        loadPage("@../resources/com/pma/fxml/patient_medical_history.fxml", "Hồ sơ y tế");
    }

    /**
     * Xử lý nút "Cập nhật hồ sơ".
     */
    @FXML
    private void loadPatientUpdateProfile(ActionEvent event) {
        log.info("View Appointments button clicked for patientId: {}", patientId);
        loadPage("@../resources/com/pma/fxml/patient_update_profile.fxml", "Danh sách lịch hẹn");
    }

    /**
     * Xử lý nút "Gửi đánh giá".
     */
    @FXML
    private void loadPatientReview(ActionEvent event) {
        log.info("View Bills button clicked for patientId: {}", patientId);
        loadPage("@../resources/com/pma/fxml/patient_review.fxml", "Danh sách hóa đơn");
    }

    /**
     * Xử lý nút "Xem hóa đơn".
     */
    @FXML
    private void loadPatientViewBills(ActionEvent event) {
        log.info("View Bills button clicked for patientId: {}", patientId);
        loadPage("@../resources/com/pma/fxml/patient_view_bills.fxml", "Danh sách hóa đơn");
    }

    /**
     * Xử lý nút "Đăng xuất".
     */
    @FXML
    private void handleLogoutButton(ActionEvent event) {
        log.info("Logout button clicked for patientId: {}", patientId);
        try {
            // Xóa thông tin phiên (giả định có SessionManager)
            // SessionManager.clearSession();

            // Tải trang đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("@../resources/com/pma/fxml/login.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) clearButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập");
            stage.show();
        } catch (IOException e) {
            log.error("Failed to load login page", e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải trang đăng nhập: " + e.getMessage());
        }
    }

    /**
     * Tải một trang FXML mới.
     */
    private void loadPage(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Stage stage = (Stage) bookButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            log.error("Failed to load page: {}", fxmlPath, e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải trang: " + e.getMessage());
        }
    }

    /**
     * Hiển thị thông báo.
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Đóng cửa sổ.
     */
    private void closeWindow() {
        Stage stage = (Stage) clearButton.getScene().getWindow();
        stage.close();
    }
}