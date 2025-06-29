package com.pma.controller.doctor;

import com.pma.model.entity.Appointment;
import com.pma.model.entity.Doctor;
import com.pma.model.entity.Patient;
import com.pma.model.enums.AppointmentStatus;
import com.pma.repository.PatientRepository;
import com.pma.service.AppointmentService;
import com.pma.service.DoctorService;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.pma.service.PatientService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.util.StringConverter;

/**
 * Controller cho doctor_book_appointment.fxml. Xử lý việc đặt lịch, cập nhật,
 * xóa và xóa form cho các cuộc hẹn của bác sĩ.
 */
@Controller
public class DoctorBookAppointmentController {

    private static final Logger log = LoggerFactory.getLogger(DoctorBookAppointmentController.class);

    @FXML
    private Button doctorViewPatientsButton;

    @FXML
    private Button doctorMedicalRecordsButton;

    @FXML
    private Button doctorPrescribeButton;

    @FXML
    private Button doctorBookAppointmentButton;

    @FXML
    private ComboBox<Patient> patientComboBox;

    @FXML
    private ComboBox<String> comboHourBox;

    @FXML
    private ComboBox<String> comboMinute;

    @FXML
    private DatePicker appointmentDatePicker;

    @FXML
    private TextArea reasonField;

    @FXML
    private ComboBox<String> appointmentTypeComboBox;

    @FXML
    private ComboBox<AppointmentStatus> statusComboBox;

    @FXML
    private Button bookButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button clearButton;

    @FXML
    private TableView<Appointment> appointmentsTable;

    @FXML
    private TableColumn<Appointment, Patient> patientColumn;

    @FXML
    private TableColumn<Appointment, LocalDateTime> appointmentDatetimeColumn;

    @FXML
    private TableColumn<Appointment, String> reasonColumn;

    @FXML
    private TableColumn<Appointment, String> appointmentTypeColumn;

    @FXML
    private TableColumn<Appointment, AppointmentStatus> statusColumn;

    @FXML
    private TableColumn<Appointment, LocalDateTime> createdAtColumn;

    @FXML
    private TableColumn<Appointment, LocalDateTime> updatedAtColumn;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PatientService patientService; // Thêm dòng này để tiêm PatientService

    private Doctor currentDoctor;

    @Autowired
    private UIManager uiManager;

    /**
     * Khởi tạo controller sau khi FXML được tải.
     */
    @FXML
    public void initialize() {
        log.info("Đang khởi tạo DoctorBookAppointmentController");
        initializeCombos();
        initializeTable();
        loadCurrentDoctor();
        loadPatients();
        loadAppointments();
        setupTableSelectionListener();
    }

    /**
     * Khởi tạo các ComboBox với giá trị mặc định.
     */
    private void initializeCombos() {
        // Khởi tạo combo giờ (00 đến 23)
        comboHourBox.getItems().setAll(
                IntStream.rangeClosed(0, 23)
                        .mapToObj(i -> String.format("%02d", i))
                        .collect(Collectors.toList())
        );
        comboHourBox.setValue("09"); // Mặc định là 9 giờ sáng

        // Khởi tạo combo phút (00, 15, 30, 45)
        comboMinute.getItems().setAll("00", "15", "30", "45");
        comboMinute.setValue("00");

        // Khởi tạo combo loại cuộc hẹn
        appointmentTypeComboBox.getItems().setAll("Tư vấn", "Kiểm tra", "Tái khám");
        appointmentTypeComboBox.setValue("Tư vấn");

        // Khởi tạo combo trạng thái
        statusComboBox.getItems().setAll(AppointmentStatus.values());
        statusComboBox.setValue(AppointmentStatus.Scheduled);
    }

    /**
     * Khởi tạo các cột của TableView.
     */
    private void initializeTable() {
        patientColumn.setCellValueFactory(cellData
                -> new SimpleObjectProperty<>(cellData.getValue().getPatient()));
        patientColumn.setCellFactory(column -> new TableCell<Appointment, Patient>() {
            @Override
            protected void updateItem(Patient patient, boolean empty) {
                super.updateItem(patient, empty);
                setText(empty || patient == null ? "" : patient.getFullName());
            }
        });

        appointmentDatetimeColumn.setCellValueFactory(
                new PropertyValueFactory<>("appointmentDatetime"));
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        appointmentTypeColumn.setCellValueFactory(
                new PropertyValueFactory<>("appointmentType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
    }

    /**
     * Tải thông tin bác sĩ đang đăng nhập.
     */
    private void loadCurrentDoctor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            userAccountService.findByUsername(username).ifPresent(userAccount -> {
                if (userAccount.getDoctor() != null) {
                    // userAccount.getDoctor() trả về một proxy được tải lười (lazy-loaded).
                    // Việc truy cập các thuộc tính của nó (như getFullName) bên ngoài một giao dịch (transaction)
                    // sẽ gây ra LazyInitializationException.
                    // Do đó, chúng ta cần lấy ID và tải lại đối tượng Doctor đầy đủ.
                    UUID doctorId = userAccount.getDoctor().getDoctorId();
                    this.currentDoctor = doctorService.findById(doctorId).orElse(null);

                    if (this.currentDoctor != null) {
                        log.info("Đã tải thông tin bác sĩ: {}", this.currentDoctor.getFullName());
                    } else {
                        log.error("Không tìm thấy bác sĩ với ID: {}", doctorId);
                        DialogUtil.showErrorAlert("Lỗi", "Không thể tải thông tin chi tiết của bác sĩ.");
                    }
                } else {
                    log.error("Không có bác sĩ nào liên kết với người dùng: {}", username);
                    DialogUtil.showErrorAlert("Lỗi", "Không có bác sĩ nào liên kết với người dùng hiện tại.");
                }
            });
        }
    }

    /**
     * Tải danh sách bệnh nhân vào patientComboBox.
     */
    private void loadPatients() {
        try {
            List<Patient> patients = patientService.getAllPatients(); // Sử dụng PatientService
            patientComboBox.getItems().setAll(patients);
            patientComboBox.setConverter(new StringConverter<Patient>() {
                @Override
                public String toString(Patient patient) {
                    return patient != null ? patient.getFullName() : "";
                }

                @Override
                public Patient fromString(String string) {
                    return null; // Không cần cho mục đích hiển thị
                }
            });
            log.info("Đã tải {} bệnh nhân vào patientComboBox", patients.size());
        } catch (Exception e) {
            log.error("Không thể tải danh sách bệnh nhân", e);
            DialogUtil.showErrorAlert("Lỗi", "Không thể tải danh sách bệnh nhân: " + e.getMessage());
        }
    }

    /**
     * Tải danh sách cuộc hẹn của bác sĩ hiện tại vào bảng.
     */
    private void loadAppointments() {
        if (currentDoctor != null) {
            try {
                List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(
                        currentDoctor.getDoctorId(), null).getContent();
                appointmentsTable.setItems(FXCollections.observableArrayList(appointments));
                log.info("Đã tải {} cuộc hẹn cho bác sĩ {}", appointments.size(), currentDoctor.getFullName());
            } catch (Exception e) {
                log.error("Không thể tải danh sách cuộc hẹn", e);
                DialogUtil.showErrorAlert("Lỗi", "Không thể tải danh sách cuộc hẹn: " + e.getMessage());
            }
        }
    }

    /**
     * Thiết lập listener cho việc chọn hàng trong bảng để điền dữ liệu vào
     * form.
     */
    private void setupTableSelectionListener() {
        appointmentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });
    }

    /**
     * Điền dữ liệu từ cuộc hẹn được chọn vào các trường của form.
     */
    private void populateForm(Appointment appointment) {
        patientComboBox.setValue(appointment.getPatient());
        LocalDateTime dateTime = appointment.getAppointmentDatetime();
        appointmentDatePicker.setValue(dateTime.toLocalDate());
        comboHourBox.setValue(String.format("%02d", dateTime.getHour()));
        comboMinute.setValue(String.format("%02d", dateTime.getMinute()));
        reasonField.setText(appointment.getReason());
        appointmentTypeComboBox.setValue(appointment.getAppointmentType());
        statusComboBox.setValue(appointment.getStatus());
    }

    /**
     * Xử lý hành động đặt lịch hẹn.
     */
    @FXML
    private void bookAppointment(ActionEvent event) {
        log.info("Đang cố gắng đặt lịch hẹn mới");
        try {
            // Lấy dữ liệu từ form
            Appointment appointmentData = createAppointmentFromForm();
            Patient selectedPatient = patientComboBox.getValue();

            // Gọi service để tạo và lưu lịch hẹn trong một transaction
            appointmentService.scheduleAppointment(appointmentData, selectedPatient.getPatientId(), currentDoctor.getDoctorId());

            loadAppointments();
            clearForm(null);
            DialogUtil.showSuccessAlert("Thành công", "Đã đặt lịch hẹn thành công.");
        } catch (Exception e) {
            log.error("Không thể đặt lịch hẹn", e);
            DialogUtil.showErrorAlert("Lỗi", "Không thể đặt lịch hẹn: " + e.getMessage());
        }
    }

    /**
     * Xử lý hành động xóa lịch hẹn.
     */
    @FXML
    private void deleteAppointment(ActionEvent event) {
        log.info("Đang cố gắng xóa lịch hẹn");
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarningAlert("Cảnh báo", "Vui lòng chọn một lịch hẹn để xóa.");
            return;
        }
        boolean confirmed = DialogUtil.showConfirmation("Xác nhận xóa", "Bạn có chắc chắn muốn xóa lịch hẹn này không?");
        if (confirmed) {
            try {
                appointmentService.deleteAppointment(selected.getAppointmentId());
                loadAppointments();
                clearForm(null);
                DialogUtil.showSuccessAlert("Thành công", "Đã xóa lịch hẹn thành công.");
            } catch (Exception e) {
                log.error("Không thể xóa lịch hẹn", e);
                DialogUtil.showErrorAlert("Lỗi", "Không thể xóa lịch hẹn: " + e.getMessage());
            }
        }
    }

    /**
     * Xử lý hành động cập nhật lịch hẹn.
     */
    @FXML
    private void updateAppointment(ActionEvent event) {
        log.info("Đang cố gắng cập nhật lịch hẹn");
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarningAlert("Cảnh báo", "Vui lòng chọn một lịch hẹn để cập nhật.");
            return;
        }
        try {
            // Lấy dữ liệu đã cập nhật từ form
            Appointment appointmentUpdateData = createAppointmentFromForm();
            Patient selectedPatient = patientComboBox.getValue();

            // Gọi service để cập nhật lịch hẹn (Giả định có phương thức updateAppointment trong service)
            appointmentService.updateAppointment(selected.getAppointmentId(), appointmentUpdateData, selectedPatient.getPatientId(), currentDoctor.getDoctorId());

            loadAppointments();
            clearForm(null);
            DialogUtil.showSuccessAlert("Thành công", "Đã cập nhật lịch hẹn thành công.");
        } catch (Exception e) {
            log.error("Không thể cập nhật lịch hẹn", e);
            DialogUtil.showErrorAlert("Lỗi", "Không thể cập nhật lịch hẹn: " + e.getMessage());
        }
    }

    /**
     * Xử lý hành động xóa dữ liệu trên form.
     */
    @FXML
    private void clearForm(ActionEvent event) {
        log.info("Đang xóa dữ liệu trên form lịch hẹn");
        patientComboBox.setValue(null);
        appointmentDatePicker.setValue(null);
        comboHourBox.setValue("09");
        comboMinute.setValue("00");
        reasonField.clear();
        appointmentTypeComboBox.setValue("Tư vấn");
        statusComboBox.setValue(AppointmentStatus.Scheduled);
        appointmentsTable.getSelectionModel().clearSelection();
    }

    /**
     * Tạo một đối tượng Appointment từ dữ liệu trên form.
     */
    private Appointment createAppointmentFromForm() {
        if (currentDoctor == null) {
            throw new IllegalStateException("Không có bác sĩ nào đăng nhập.");
        }
        if (patientComboBox.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn một bệnh nhân.");
        }
        if (appointmentDatePicker.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn ngày hẹn.");
        }
        if (comboHourBox.getValue() == null || comboMinute.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn thời gian hẹn.");
        }
        if (reasonField.getText() == null || reasonField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do không được để trống.");
        }
        if (appointmentTypeComboBox.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn loại cuộc hẹn.");
        }
        if (statusComboBox.getValue() == null) {
            throw new IllegalArgumentException("Phải chọn trạng thái.");
        }

        // Chỉ tạo một đối tượng tạm thời với dữ liệu từ form.
        // Việc liên kết với Patient và Doctor sẽ được thực hiện trong tầng Service.
        Appointment appointment = new Appointment();
        LocalDate date = appointmentDatePicker.getValue();
        LocalTime time = LocalTime.of(
                Integer.parseInt(comboHourBox.getValue()),
                Integer.parseInt(comboMinute.getValue())
        );
        appointment.setAppointmentDatetime(LocalDateTime.of(date, time));
        appointment.setReason(reasonField.getText().trim());
        appointment.setAppointmentType(appointmentTypeComboBox.getValue());
        appointment.setStatus(statusComboBox.getValue());
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointment;
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
