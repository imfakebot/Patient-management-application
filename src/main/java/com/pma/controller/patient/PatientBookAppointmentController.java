package com.pma.controller.patient;

import com.pma.model.entity.Appointment;
import com.pma.model.entity.Doctor;
import com.pma.model.enums.AppointmentStatus;
import com.pma.service.AppointmentService;
import com.pma.repository.DoctorRepository;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Bộ điều khiển cho giao diện patient_book_appointment.fxml. Xử lý logic để
 * bệnh nhân đặt lịch hẹn, bao gồm chọn bác sĩ, ngày giờ, lý do và loại cuộc
 * hẹn.
 */
@Component
public class PatientBookAppointmentController {

    private static final Logger log = LoggerFactory.getLogger(PatientBookAppointmentController.class);

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
    @FXML 
    private Button logoutBtn;

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
        doctorCombo.setCellFactory(_ -> createDoctorListCell());
        doctorCombo.setButtonCell(createDoctorListCell());
    }

    /**
     * Tạo một ListCell để hiển thị thông tin bác sĩ trong ComboBox.
     *
     * @return một ListCell đã được cấu hình.
     */
    private ListCell<Doctor> createDoctorListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Doctor doctor, boolean empty) {
                super.updateItem(doctor, empty);
                setText(empty || doctor == null ? null : doctor.getFullName() + " (" + doctor.getSpecialty() + ")");
            }
        };
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
        appointmentDatePicker.setDayCellFactory(_ -> new DateCell() {
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
    @SuppressWarnings("unchecked")
    private void khoiTaoAppointmentsTable() {
        // Định nghĩa các cột cho bảng
        TableColumn<Appointment, String> doctorColumn = new TableColumn<>("Bác sĩ");
        doctorColumn.setCellValueFactory(cellData -> {
            Doctor doctor = cellData.getValue().getDoctor();
            return new javafx.beans.property.SimpleStringProperty(
                    doctor != null ? doctor.getFullName() : "Chưa phân công");
        });

        TableColumn<Appointment, LocalDateTime> dateTimeColumn = new TableColumn<>("Ngày & Giờ");
        dateTimeColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAppointmentDatetime()));

        TableColumn<Appointment, String> typeColumn = new TableColumn<>("Loại");
        typeColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAppointmentType()));

        TableColumn<Appointment, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString()));

        appointmentsTable.getColumns().addAll(doctorColumn, dateTimeColumn, typeColumn, statusColumn);

        // Tải các cuộc hẹn sắp tới của bệnh nhân
        List<Appointment> upcomingAppointments = appointmentService.getAppointmentsByPatient(patientId,
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent()
                .stream()
                .filter(app -> app.getStatus() == AppointmentStatus.Scheduled
                && app.getAppointmentDatetime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        appointmentsTable.setItems(FXCollections.observableArrayList(upcomingAppointments));
    }

    /**
     * Xử lý sự kiện khi nhấn nút đặt lịch hẹn.
     *
     */
    @FXML
    private void bookAppointment() {
        try {
            Optional<String> validationError = getValidationError();
            if (validationError.isPresent()) {
                DialogUtil.showErrorAlert("Lỗi Nhập Liệu", validationError.get());
                return;
            }

            Doctor selectedDoctor = doctorCombo.getValue();
            LocalDateTime appointmentDateTime = getAppointmentDateTimeFromForm();

            // Tạo đối tượng Appointment
            Appointment appointment = new Appointment();
            appointment.setAppointmentDatetime(appointmentDateTime);
            appointment.setReason(reasonField.getText().trim());
            appointment.setAppointmentType(appointmentTypeCombo.getValue());
            appointment.setStatus(AppointmentStatus.Scheduled);

            // Đặt lịch hẹn
            Appointment savedAppointment = appointmentService.scheduleAppointment(appointment, patientId, selectedDoctor.getDoctorId());

            DialogUtil.showSuccessAlert("Thành Công", "Đặt lịch hẹn thành công vào "
                    + savedAppointment.getAppointmentDatetime() + " với Bác sĩ " + selectedDoctor.getFullName());

            // Làm mới bảng lịch hẹn
            khoiTaoAppointmentsTable();

            // Xóa form
            clearForm();

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
     * Xác thực dữ liệu nhập vào từ form đặt lịch hẹn.
     *
     * @return một Optional chứa thông báo lỗi nếu có, ngược lại là Optional
     * rỗng.
     */
    private Optional<String> getValidationError() {
        if (doctorCombo.getValue() == null) {
            return Optional.of("Vui lòng chọn bác sĩ.");
        }
        if (appointmentDatePicker.getValue() == null) {
            return Optional.of("Vui lòng chọn ngày hẹn.");
        }
        if (comboHour.getValue() == null || comboMinute.getValue() == null) {
            return Optional.of("Vui lòng chọn giờ và phút hợp lệ.");
        }
        if (reasonField.getText() == null || reasonField.getText().trim().isEmpty()) {
            return Optional.of("Vui lòng cung cấp lý do cho cuộc hẹn.");
        }
        if (appointmentTypeCombo.getValue() == null) {
            return Optional.of("Vui lòng chọn loại cuộc hẹn.");
        }

        if (getAppointmentDateTimeFromForm().isBefore(LocalDateTime.now())) {
            return Optional.of("Ngày và giờ hẹn phải ở tương lai.");
        }
        return Optional.empty();
    }

    /**
     * Lấy LocalDateTime từ các trường chọn ngày và giờ trên form.
     *
     * @return LocalDateTime được tạo từ form.
     */
    private LocalDateTime getAppointmentDateTimeFromForm() {
        LocalDate appointmentDate = appointmentDatePicker.getValue();
        LocalTime appointmentTime = LocalTime.parse(comboHour.getValue() + ":" + comboMinute.getValue());
        return LocalDateTime.of(appointmentDate, appointmentTime);
    }

    /**
     * Xử lý sự kiện khi nhấn nút xóa form để đặt lại các trường nhập liệu.
     *
     */
    @FXML
    private void clearForm() {
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
        if (patientId == null) {
            log.warn("Cannot switch to Book Appointment screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientBookAppointment(patientId);
    }

    @FXML
    private void loadPatientViewPrescriptions() {
        if (patientId == null) {
            log.warn("Cannot switch to View Prescriptions screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientViewPrescriptions(patientId);
    }

    @FXML
    private void loadPatientMedicalHistory() {
        if (patientId == null) {
            log.warn("Cannot switch to Medical History screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientMedicalHistory(patientId);
    }

    @FXML
    private void loadPatientUpdateProfile() {
        if (patientId == null) {
            log.warn("Cannot switch to Update Profile screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientUpdateProfile(patientId);
    }

    @FXML
    private void loadPatientViewBills() {
        if (patientId == null) {
            log.warn("Cannot switch to View Bills screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientViewBills(patientId);
    }

    @FXML
    private void logout() {
        uiManager.switchToLoginScreen();  
    }

}
