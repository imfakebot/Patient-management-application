package com.pma.service; // Đảm bảo đúng package

import java.time.LocalDateTime; // Import Entity Appointment
import java.util.List; // Import List for Appointment collections
import java.util.UUID; // Import Doctor

import org.slf4j.Logger; // Import Patient
import org.slf4j.LoggerFactory; // Import Enum AppointmentStatus
import org.springframework.beans.factory.annotation.Autowired; // Import Repository Appointment
import org.springframework.dao.DataIntegrityViolationException; // Import Repository Doctor để kiểm tra
import org.springframework.data.domain.Page; // Import Repository Patient để kiểm tra
import org.springframework.data.domain.Pageable; // Exception chuẩn
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pma.model.entity.Appointment;
import com.pma.model.entity.Doctor;
import com.pma.model.entity.Patient;
import com.pma.model.enums.AppointmentStatus;
import com.pma.repository.AppointmentRepository;
import com.pma.repository.DoctorRepository;
import com.pma.repository.PatientRepository;

import jakarta.persistence.EntityNotFoundException; // Cần import nếu có phương thức trả về List

/**
 * Lớp Service cho việc quản lý các nghiệp vụ liên quan đến Appointment. Bao gồm
 * đặt lịch, hủy lịch, cập nhật trạng thái, truy vấn và gửi email thông báo.
 */
@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final EmailService emailService; // Dùng để gửi email thông báo

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            EmailService emailService) { // Inject EmailService
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.emailService = emailService; // Gán vào biến thành viên
    }

    /**
     * Kiểm tra xem một cuộc hẹn có tồn tại trong cơ sở dữ liệu dựa vào ID hay
     * không.
     *
     * @param id UUID của Appointment cần kiểm tra.
     * @return true nếu Appointment tồn tại, false nếu không.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean appointmentExists(UUID id) {
        log.debug("Checking existence of appointment with id: {}", id);
        boolean exists = appointmentRepository.existsById(id);
        log.debug("Appointment with id: {} exists? {}", id, exists);
        return exists;
    }

    /**
     * Tạo một cuộc hẹn mới (đặt lịch). Gửi email xác nhận sau khi đặt lịch
     * thành công.
     *
     * @param appointment Đối tượng Appointment chứa thông tin cơ bản (reason,
     * type, dateTime). ID nên là null.
     * @param patientId UUID của Patient đặt lịch.
     * @param doctorId (Optional) UUID của Doctor được yêu cầu. Null nếu chưa
     * xác định hoặc không yêu cầu cụ thể.
     * @return Appointment đã được tạo và lưu.
     * @throws EntityNotFoundException nếu patientId hoặc doctorId (nếu khác
     * null) không tồn tại.
     * @throws IllegalArgumentException nếu thời gian hẹn không hợp lệ (ví dụ:
     * trong quá khứ).
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Appointment scheduleAppointment(Appointment appointment, UUID patientId, UUID doctorId) {
        log.info("Attempting to schedule appointment for patientId: {}, doctorId: {}", patientId, doctorId);

        // --- Validation ---
        if (appointment.getAppointmentDatetime() == null
                || appointment.getAppointmentDatetime().isBefore(LocalDateTime.now())) {
            log.warn("Scheduling failed. Appointment datetime is invalid: {}", appointment.getAppointmentDatetime());
            throw new IllegalArgumentException("Appointment date and time must be in the future.");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + patientId));

        Doctor doctor = null;
        if (doctorId != null) {
            doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + doctorId));

            // Kiểm tra chồng chéo lịch hẹn
            checkAppointmentOverlap(appointment.getAppointmentDatetime(), doctorId, null);
        }

        // --- Thiết lập và Lưu ---
        appointment.setAppointmentId(null);
        appointment.setPatient(patient); // Gọi helper method (nếu có) để đồng bộ 2 chiều
        appointment.setDoctor(doctor); // Gọi helper method (nếu có) để đồng bộ 2 chiều
        appointment.setStatus(AppointmentStatus.Scheduled);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Successfully scheduled appointment with id: {}", savedAppointment.getAppointmentId());

        // --- GỬI EMAIL XÁC NHẬN ---
        try {
            emailService.sendSchedulingConfirmation(savedAppointment);
            log.info("Triggered confirmation email for appointment id: {}", savedAppointment.getAppointmentId());
        } catch (Exception e) {
            log.error("Failed to trigger confirmation email sending for appointment id: {}",
                    savedAppointment.getAppointmentId(), e);
        }

        return savedAppointment;
    }

    /**
     * Cập nhật thông tin của một cuộc hẹn đã tồn tại.
     *
     * @param appointmentId ID của cuộc hẹn cần cập nhật.
     * @param appointmentUpdateData Đối tượng Appointment chứa dữ liệu cập nhật từ form.
     * @param patientId ID của bệnh nhân.
     * @param doctorId ID của bác sĩ.
     * @return Appointment đã được cập nhật.
     * @throws EntityNotFoundException nếu không tìm thấy Appointment, Patient, hoặc Doctor.
     * @throws IllegalArgumentException nếu thời gian hẹn không hợp lệ hoặc bị chồng chéo.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Appointment updateAppointment(UUID appointmentId, Appointment appointmentUpdateData, UUID patientId, UUID doctorId) {
        log.info("Attempting to update appointment with id: {}", appointmentId);

        // 1. Tải thực thể cuộc hẹn hiện tại (managed entity)
        Appointment existingAppointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + appointmentId));

        // 2. Tải các thực thể liên quan (managed entities)
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + patientId));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + doctorId));

        // 3. Kiểm tra thời gian hẹn có hợp lệ không
        if (appointmentUpdateData.getAppointmentDatetime() == null
                || appointmentUpdateData.getAppointmentDatetime().isBefore(LocalDateTime.now())) {
            log.warn("Update failed. Appointment datetime is invalid: {}", appointmentUpdateData.getAppointmentDatetime());
            throw new IllegalArgumentException("Appointment date and time must be in the future.");
        }

        // 4. Kiểm tra chồng chéo lịch hẹn (loại trừ chính cuộc hẹn đang cập nhật)
        checkAppointmentOverlap(appointmentUpdateData.getAppointmentDatetime(), doctorId, appointmentId);

        // 5. Cập nhật các thuộc tính của existingAppointment
        existingAppointment.setAppointmentDatetime(appointmentUpdateData.getAppointmentDatetime());
        existingAppointment.setReason(appointmentUpdateData.getReason());
        existingAppointment.setAppointmentType(appointmentUpdateData.getAppointmentType());
        existingAppointment.setStatus(appointmentUpdateData.getStatus());
        
        // Cập nhật các mối quan hệ nếu có thay đổi (sẽ không thay đổi Patient/Doctor ID trong trường hợp này)
        // Nhưng vẫn set lại để đảm bảo đối tượng managed được liên kết đúng
        existingAppointment.setPatient(patient);
        existingAppointment.setDoctor(doctor);

        // Transaction commit sẽ tự động lưu các thay đổi
        log.info("Successfully updated appointment with id: {}", appointmentId);
        return existingAppointment;
    }

    /**
     * Lấy thông tin chi tiết của một cuộc hẹn bằng ID.
     *
     * @param id UUID của Appointment.
     * @return Đối tượng Appointment.
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Appointment getAppointmentById(UUID id) {
        log.info("Fetching appointment with id: {}", id);
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + id));
    }
    
    /**
     * Phương thức helper để kiểm tra chồng chéo lịch hẹn cho một bác sĩ.
     *
     * @param newDateTime Thời gian bắt đầu của cuộc hẹn mới.
     * @param doctorId ID của bác sĩ.
     * @param appointmentIdToExclude ID của cuộc hẹn cần loại trừ khỏi kiểm tra (dùng khi cập nhật). Null nếu là cuộc hẹn mới.
     * @throws IllegalArgumentException nếu có lịch hẹn chồng chéo.
     */
    private void checkAppointmentOverlap(LocalDateTime newDateTime, UUID doctorId, UUID appointmentIdToExclude) {
        final long DEFAULT_APPOINTMENT_DURATION_MINUTES = 60; // Giả định thời lượng cuộc hẹn
        LocalDateTime newAppStartTime = newDateTime;
        LocalDateTime newAppEndTime = newAppStartTime.plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES);

        // Lấy tất cả các cuộc hẹn "Scheduled" (chưa hoàn thành) của bác sĩ
        List<Appointment> doctorScheduledAppointments = appointmentRepository
                .findByDoctor_DoctorIdAndStatus(doctorId, AppointmentStatus.Scheduled);

        for (Appointment existingApp : doctorScheduledAppointments) {
            // Bỏ qua cuộc hẹn đang được cập nhật (nếu có)
            if (appointmentIdToExclude != null && existingApp.getAppointmentId().equals(appointmentIdToExclude)) {
                continue;
            }

            LocalDateTime existingAppStartTime = existingApp.getAppointmentDatetime();
            LocalDateTime existingAppEndTime = existingAppStartTime.plusMinutes(DEFAULT_APPOINTMENT_DURATION_MINUTES);

            // Kiểm tra chồng chéo: (StartA < EndB) AND (EndA > StartB)
            if (newAppStartTime.isBefore(existingAppEndTime) && newAppEndTime.isAfter(existingAppStartTime)) {
                log.warn("Lịch hẹn chồng chéo phát hiện cho bác sĩ {} (ID: {}). Lịch hẹn hiện có (ID: {}) từ {} đến {}. Lịch hẹn mới/cập nhật từ {} đến {}.",
                        doctorId, doctorId, existingApp.getAppointmentId(), existingAppStartTime, existingAppEndTime, newAppStartTime, newAppEndTime);
                throw new IllegalArgumentException("Bác sĩ không rảnh vào thời gian đã chọn do có lịch hẹn chồng chéo. Vui lòng chọn khung giờ khác.");
            }
        }
    }

    /**
     * Cập nhật trạng thái của một cuộc hẹn. Gửi email thông báo nếu trạng thái
     * được cập nhật thành Cancelled.
     *
     * @param id UUID của Appointment cần cập nhật.
     * @param newStatus Trạng thái mới (Completed, Cancelled, No_Show).
     * @param updateNote (Optional) Ghi chú lý do cập nhật.
     * @return Appointment đã được cập nhật.
     * @throws EntityNotFoundException nếu không tìm thấy Appointment.
     * @throws IllegalArgumentException nếu trạng thái mới không hợp lệ hoặc
     * không được phép thay đổi.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Appointment updateAppointmentStatus(UUID id, AppointmentStatus newStatus, String updateNote) {
        log.info("Attempting to update status for appointment id: {} to {}", id, newStatus);
        Appointment appointment = getAppointmentById(id);

        // --- Validation ---
        if ((appointment.getStatus() == AppointmentStatus.Completed
                || appointment.getStatus() == AppointmentStatus.Cancelled)
                && appointment.getStatus() != newStatus) {
            log.warn("Update failed. Cannot change status from {} for appointment id: {}", appointment.getStatus(), id);
            throw new IllegalStateException("Cannot change status from " + appointment.getStatus());
        }
        if (newStatus == AppointmentStatus.Scheduled && appointment.getStatus() != AppointmentStatus.Scheduled) {
            log.warn("Update failed. Cannot set status back to Scheduled for appointment id: {}", id);
            throw new IllegalArgumentException("Cannot set status back to Scheduled.");
        }

        // --- Cập nhật ---
        appointment.setStatus(newStatus);
        if (updateNote != null && !updateNote.trim().isEmpty()) {
            String currentReason = appointment.getReason() == null ? "" : appointment.getReason() + "\n---\n";
            appointment.setReason(currentReason + "Status updated to " + newStatus + " on "
                    + LocalDateTime.now().withNano(0) + ": " + updateNote);
        }

        log.info("Appointment status updated successfully for id: {}", id);

        // --- GỬI EMAIL THÔNG BÁO HỦY (NẾU CÓ) ---
        if (newStatus == AppointmentStatus.Cancelled) {
            try {
                emailService.sendCancellationNotification(appointment,
                        updateNote != null ? updateNote : "No reason provided.");
                log.info("Triggered cancellation email for appointment id: {}", id);
            } catch (Exception e) {
                log.error("Failed to trigger cancellation email sending for appointment id: {}", id, e);
            }
        }

        return appointment; // Transaction commit sẽ lưu thay đổi
    }

    /**
     * Hủy một cuộc hẹn. Wrapper cho updateAppointmentStatus.
     *
     * @param id UUID của Appointment cần hủy.
     * @param cancellationReason Lý do hủy.
     * @return Appointment đã được hủy.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Appointment cancelAppointment(UUID id, String cancellationReason) {
        log.info("Attempting to cancel appointment id: {} with reason: {}", id, cancellationReason);
        return updateAppointmentStatus(id, AppointmentStatus.Cancelled, cancellationReason);
    }

    /**
     * Lấy danh sách các cuộc hẹn của một bệnh nhân (có phân trang).
     *
     * @param patientId ID của Patient.
     * @param pageable Thông tin phân trang.
     * @return Page chứa danh sách Appointment.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Appointment> getAppointmentsByPatient(UUID patientId, Pageable pageable) {
        log.info("Fetching appointments for patient id: {} with pagination: {}", patientId, pageable);
        return appointmentRepository.findByPatient_PatientIdOrderByAppointmentDatetimeDesc(patientId, pageable);
    }

    /**
     * Lấy danh sách các cuộc hẹn của một bác sĩ (có phân trang).
     *
     * @param doctorId ID của Doctor.
     * @param pageable Thông tin phân trang.
     * @return Page chứa danh sách Appointment.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Appointment> getAppointmentsByDoctor(UUID doctorId, Pageable pageable) {
        log.info("Fetching appointments for doctor id: {} with pagination: {}", doctorId, pageable);
        // !! Đảm bảo phương thức này tồn tại trong AppointmentRepository !!
        return appointmentRepository.findByDoctor_DoctorIdOrderByAppointmentDatetimeDesc(doctorId, pageable);
    }

    /**
     * Lấy danh sách các cuộc hẹn trong một khoảng thời gian (có phân trang).
     *
     * @param startDateTime Thời điểm bắt đầu.
     * @param endDateTime Thời điểm kết thúc.
     * @param pageable Thông tin phân trang.
     * @return Page chứa danh sách Appointment.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Appointment> getAppointmentsBetweenDates(LocalDateTime startDateTime, LocalDateTime endDateTime,
            Pageable pageable) {
        log.info("Fetching appointments between {} and {} with pagination: {}", startDateTime, endDateTime, pageable);
        // !! Đảm bảo phương thức này tồn tại trong AppointmentRepository !!
        return appointmentRepository.findByAppointmentDatetimeBetweenOrderByAppointmentDatetimeAsc(startDateTime,
                endDateTime, pageable);
    }

    /**
     * Xóa một cuộc hẹn theo ID (Xóa cứng - Thường không khuyến khích).
     *
     * @param id UUID của Appointment cần xóa.
     * @throws EntityNotFoundException nếu không tìm thấy Appointment.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteAppointment(UUID id) {
        log.warn("Attempting to HARD DELETE appointment with id: {}. Consider cancelling instead.", id);
        // Sử dụng phương thức kiểm tra tồn tại trước
        if (!appointmentExists(id)) {
            log.error("Deletion failed. Appointment not found with id: {}", id);
            throw new EntityNotFoundException("Appointment not found with id: " + id);
        }
        try {
            appointmentRepository.deleteById(id);
            log.info("Successfully deleted appointment with id: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error(
                    "Data integrity violation during deletion of appointment id: {}. Check foreign key constraints (e.g., MedicalRecords linking). Error: {}",
                    id, e.getMessage());
            throw new IllegalStateException(
                    "Could not delete appointment with id " + id + " due to data integrity issues.", e);
        }
    }
}
