package com.pma.controller.patient;

import com.pma.model.entity.Appointment;
import com.pma.model.entity.Doctor;
import com.pma.model.entity.Patient;
import com.pma.model.enums.AppointmentStatus;
import com.pma.service.AppointmentService;
import com.pma.repository.DoctorRepository;
import com.pma.repository.PatientRepository;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bộ điều khiển cho giao diện patient_book_appointment.fxml.
 * Xử lý logic để bệnh nhân đặt lịch hẹn, bao gồm chọn bác sĩ, ngày giờ, lý do và loại cuộc hẹn.
 */
@Component
public class PatientBookAppointmentController {

    private static final Logger log = LoggerFactory.getLogger(PatientBookAppointmentController.class);

    @FXML private Button patientBookAppointmentButton;
    @FXML private Button patientViewPrescriptionsButton;
    @FXML private Button patientMedicalHistoryButton;
    @FXML private Button patientUpdateProfileButton;
    @FXML private Button patientReviewButton;
    @FXML private Button patientViewBillsButton;

    @FXML
    private ComboBox<Doctor> doctorCombo; // Hộp chọn bác sĩ

    @FXML
    private ComboBox<String> comboHour; // Hộp chọn giờ

    @FXML
    private ComboBox<String> comboMinute; // Hộp chọn phút

    @FXML
    private DatePicker appointmentDatePicker; // Bộ chọn ngày hẹn

    @FXML
    private TextArea reasonField; // Trường nhập lý do cuộc hẹn

    @FXML
    private ComboBox<String> appointmentTypeCombo; // Hộp chọn loại cuộc hẹn

    @FXML
    private Button bookButton; // Nút đặt lịch

    @FXML
    private Button clearButton; // Nút xóa form

    @FXML
    private TableView<Appointment> appointmentsTable; // Bảng hiển thị các cuộc hẹn sắp tới

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UIManager uiManager;

    private UUID patientId;

    /**
     * Khởi tạo controller với ID của bệnh nhân.
     *
     * @param patientId ID của bệnh nhân đặt lịch hẹn.
     */
    public void initData(UUID patientId) {
        this.patientId = patientId;
        khoiTaoDoctorCombo();
        khoiTaoComboHour();
        khoiTaoComboMinute();
        khoiTaoAppointmentTypeCombo();
        khoiTaoDatePicker();
        khoiTaoAppointmentsTable();
    }

    /**
     * Khởi tạo hộp chọn bác sĩ với danh sách bác sĩ đang hoạt động.
     */
    private void khoiTaoDoctorCombo() {
        List<Doctor> activeDoctors = doctorRepository.findByStatus(com.pma.model.enums.DoctorStatus.ACTIVE);
        ObservableList<Doctor> doctorList = FXCollections.observableArrayList(activeDoctors);
        doctorCombo.setItems(doctorList);
        doctorCombo.setPromptText("Chọn Bác sĩ");
        // Tùy chỉnh hiển thị tên và chuyên khoa của bác sĩ
        doctorCombo.setCellFactory(param -> new ListCell<Doctor>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);
                if (empty || doctor == null) {
                    setText(null);
                } else {
                    setText(doctor.getFullName() + " (" + doctor.getSpecialty() + ")");
                }
            }
        });
        doctorCombo.setButtonCell(new ListCell<Doctor>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);
                if (empty || doctor == null) {
                    setText(null);
                } else {
                    setText(doctor.getFullName() + " (" + doctor.getSpecialty() + ")");
                }
            }
        });
    }

    /**
     * Khởi tạo hộp chọn giờ với giá trị từ 00 đến 23.
     */
    private void khoiTaoComboHour() {
        List<String> hours = IntStream.rangeClosed(0, 23)
                .mapToObj(i -> String.format("%02d", i))
                .collect(Collectors.toList());
        comboHour.setItems(FXCollections.observableArrayList(hours));
        comboHour.setPromptText("Giờ");
    }

    /**
     * Khởi tạo hộp chọn phút với các giá trị 00, 15, 30, 45.
     */
    private void khoiTaoComboMinute() {
        List<String> minutes = List.of("00", "15", "30", "45");
        comboMinute.setItems(FXCollections.observableArrayList(minutes));
        comboMinute.setPromptText("Phút");
    }

    /**
     * Khởi tạo hộp chọn loại cuộc hẹn với các giá trị cố định.
     */
    private void khoiTaoAppointmentTypeCombo() {
        List<String> types = List.of("Khám lần đầu", "Tái khám", "Tư vấn", "Kiểm tra định kỳ");
        appointmentTypeCombo.setItems(FXCollections.observableArrayList(types));
        appointmentTypeCombo.setPromptText("Chọn Loại Cuộc Hẹn");
    }

    /**
     * Khởi tạo bộ chọn ngày, vô hiệu hóa các ngày trong quá khứ.
     */
    private void khoiTaoDatePicker() {
        appointmentDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }

    /**
     * Khởi tạo bảng hiển thị các cuộc hẹn sắp tới của bệnh nhân.
     */
    private void khoiTaoAppointmentsTable() {
        // Định nghĩa các cột cho bảng
        TableColumn<Appointment, String> doctorColumn = new TableColumn<>("Bác sĩ");
        doctorColumn.setCellValueFactory(cellData -> {
            Doctor doctor = cellData.getValue().getDoctor();
            return new javafx.beans.property.SimpleStringProperty(
                doctor != null ? doctor.getFullName() : "Chưa phân công");
        });

        TableColumn<Appointment, LocalDateTime> dateTimeColumn = new TableColumn<>("Ngày & Giờ");
        dateTimeColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAppointmentDatetime()));

        TableColumn<Appointment, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAppointmentType()));

        TableColumn<Appointment, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString()));

        appointmentsTable.getColumns().addAll(doctorColumn, dateTimeColumn, typeColumn, statusColumn);

        // Tải các cuộc hẹn sắp tới của bệnh nhân
        List<Appointment> upcomingAppointments = appointmentService.getAppointmentsByPatient(patientId,
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent()
                .stream()
                .filter(app -> app.getStatus() == AppointmentStatus.Scheduled &&
                        app.getAppointmentDatetime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        appointmentsTable.setItems(FXCollections.observableArrayList(upcomingAppointments));
    }

    /**
     * Xử lý sự kiện khi nhấn nút đặt lịch hẹn.
     *
     * @param event Sự kiện ActionEvent từ nút đặt lịch.
     */
    @FXML
    private void bookAppointment(ActionEvent event) {
        try {
            // Kiểm tra đầu vào
            Doctor selectedDoctor = doctorCombo.getValue();
            LocalDate appointmentDate = appointmentDatePicker.getValue();
            String hour = comboHour.getValue();
            String minute = comboMinute.getValue();
            String reason = reasonField.getText();
            String appointmentType = appointmentTypeCombo.getValue();

            if (selectedDoctor == null) {
                DialogUtil.showErrorAlert("Lỗi Nhập Liệu", "Vui lòng chọn bác sĩ.");
                return;
            }
            if (appointmentDate == null) {
                DialogUtil.showErrorAlert("Lỗi Nhập Liệu", "Vui lòng chọn ngày hẹn.");
                return;
            }
            if (hour == null || minute == null) {
                DialogUtil.showErrorAlert("Lỗi Nhập Liệu", "Vui lòng chọn giờ và phút hợp lệ.");
                return;
            }
            if (reason == null || reason.trim().isEmpty()) {
                DialogUtil.showErrorAlert("Lỗi Nhập Liệu", "Vui lòng cung cấp lý do cho cuộc hẹn.");
                return;
            }
            if (appointmentType == null) {
                DialogUtil.showErrorAlert("Lỗi Nhập Liệu", "Vui lòng chọn loại cuộc hẹn.");
                return;
            }

            // Phân tích thời gian
            LocalTime appointmentTime = LocalTime.parse(hour + ":" + minute);
            LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);

            if (appointmentDateTime.isBefore(LocalDateTime.now())) {
                DialogUtil.showErrorAlert("Lỗi Nhập Liệu", "Ngày và giờ hẹn phải ở tương lai.");
                return;
            }

            // Tạo đối tượng Appointment
            Appointment appointment = new Appointment();
            appointment.setAppointmentDatetime(appointmentDateTime);
            appointment.setReason(reason);
            appointment.setAppointmentType(appointmentType);
            appointment.setStatus(AppointmentStatus.Scheduled);

            // Đặt lịch hẹn
            Appointment savedAppointment = appointmentService.scheduleAppointment(appointment, patientId, selectedDoctor.getDoctorId());

            DialogUtil.showSuccessAlert("Thành Công", "Đặt lịch hẹn thành công vào " +
                    savedAppointment.getAppointmentDatetime() + " với Bác sĩ " + selectedDoctor.getFullName());

            // Làm mới bảng lịch hẹn
            khoiTaoAppointmentsTable();

            // Xóa form
            clearForm(null);

        } catch (IllegalArgumentException e) {
            log.warn("Đặt lịch hẹn thất bại: {}", e.getMessage());
            DialogUtil.showErrorAlert("Lỗi Đặt Lịch", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi đặt lịch hẹn", e);
            DialogUtil.showExceptionDialog("Lỗi Không Mong Muốn", "Đã xảy ra lỗi khi đặt lịch hẹn.",
                    e.getMessage(), e);
        }
    }

    /**
     * Xử lý sự kiện khi nhấn nút xóa form để đặt lại các trường nhập liệu.
     *
     * @param event Sự kiện ActionEvent từ nút xóa.
     */
    @FXML
    private void clearForm(ActionEvent event) {
        doctorCombo.getSelectionModel().clearSelection();
        appointmentDatePicker.setValue(null);
        comboHour.getSelectionModel().clearSelection();
        comboMinute.getSelectionModel().clearSelection();
        reasonField.clear();
        appointmentTypeCombo.getSelectionModel().clearSelection();
        log.info("Form đặt lịch hẹn đã được xóa.");
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
    private void loadPatientViewBills() {
        uiManager.switchToPatientViewBills();
    }

}