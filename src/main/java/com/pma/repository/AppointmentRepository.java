package com.pma.repository; // Đảm bảo đúng package

import java.time.LocalDateTime; // Import Entity Appointment
import java.util.List; // Import Doctor để tìm theo bác sĩ
import java.util.UUID; // Import Patient để tìm theo bệnh nhân

import org.springframework.data.domain.Page; // Import Enum AppointmentStatus
import org.springframework.data.domain.Pageable; // Import cho phân trang
import org.springframework.data.jpa.repository.JpaRepository; // Import cho phân trang
import org.springframework.stereotype.Repository;

import com.pma.model.entity.Appointment; // Import nếu dùng @Query
import com.pma.model.entity.Doctor; // Import nếu dùng @Query với tham số
import com.pma.model.entity.Patient;
import com.pma.model.enums.AppointmentStatus; // Import nếu tìm theo thời gian

/**
 * Spring Data JPA repository cho thực thể Appointment.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    // Kế thừa JpaRepository<Appointment, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.
    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---
    /**
     * Tìm danh sách các cuộc hẹn của một bệnh nhân cụ thể.
     *
     * @param patient Đối tượng Patient.
     * @return Danh sách các Appointment của bệnh nhân đó.
     */
    List<Appointment> findByPatient(Patient patient);

    /**
     * Tìm danh sách các cuộc hẹn của một bệnh nhân theo ID của bệnh nhân.
     *
     * @param patientId ID của Patient.
     * @return Danh sách các Appointment của bệnh nhân đó.
     */
    List<Appointment> findByPatient_PatientId(UUID patientId);

    /**
     * Tìm danh sách các cuộc hẹn với một bác sĩ cụ thể.
     *
     * @param doctor Đối tượng Doctor.
     * @return Danh sách các Appointment với bác sĩ đó.
     */
    List<Appointment> findByDoctor(Doctor doctor);

    /**
     * Tìm danh sách các cuộc hẹn với một bác sĩ theo ID của bác sĩ.
     *
     * @param doctorId ID của Doctor.
     * @return Danh sách các Appointment với bác sĩ đó.
     */
    List<Appointment> findByDoctor_DoctorId(UUID doctorId);

    /**
     * Tìm danh sách các cuộc hẹn theo trạng thái.
     *
     * @param status Trạng thái cần lọc (sử dụng kiểu Enum).
     * @return Danh sách các Appointment có trạng thái tương ứng.
     */
    List<Appointment> findByStatus(AppointmentStatus status);

    /**
     * Tìm danh sách các cuộc hẹn diễn ra sau một thời điểm nhất định.
     *
     * @param dateTime Thời điểm bắt đầu (không bao gồm).
     * @return Danh sách các Appointment phù hợp.
     */
    List<Appointment> findByAppointmentDatetimeAfter(LocalDateTime dateTime);

    /**
     * Tìm danh sách các cuộc hẹn diễn ra trước một thời điểm nhất định.
     *
     * @param dateTime Thời điểm kết thúc (không bao gồm).
     * @return Danh sách các Appointment phù hợp.
     */
    List<Appointment> findByAppointmentDatetimeBefore(LocalDateTime dateTime);

    /**
     * Tìm danh sách các cuộc hẹn diễn ra trong một khoảng thời gian.
     *
     * @param startDateTime Thời điểm bắt đầu (bao gồm).
     * @param endDateTime Thời điểm kết thúc (bao gồm).
     * @return Danh sách các Appointment phù hợp.
     */
    List<Appointment> findByAppointmentDatetimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * Tìm danh sách các cuộc hẹn của một bệnh nhân trong một khoảng thời gian.
     *
     * @param patientId ID của Patient.
     * @param startDateTime Thời điểm bắt đầu.
     * @param endDateTime Thời điểm kết thúc.
     * @return Danh sách các Appointment phù hợp.
     */
    List<Appointment> findByPatient_PatientIdAndAppointmentDatetimeBetween(UUID patientId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime);

    /**
     * Tìm danh sách các cuộc hẹn của một bác sĩ trong một khoảng thời gian.
     *
     * @param doctorId ID của Doctor.
     * @param startDateTime Thời điểm bắt đầu.
     * @param endDateTime Thời điểm kết thúc.
     * @return Danh sách các Appointment phù hợp.
     */
    List<Appointment> findByDoctor_DoctorIdAndAppointmentDatetimeBetween(UUID doctorId, LocalDateTime startDateTime,
            LocalDateTime endDateTime);

    // --- Ví dụ sử dụng Phân trang và Sắp xếp ---
    /**
     * Tìm các cuộc hẹn của một bệnh nhân, sắp xếp theo thời gian hẹn giảm dần,
     * có phân trang.
     *
     * @param patientId ID của Patient.
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách Appointment.
     */
    Page<Appointment> findByPatient_PatientIdOrderByAppointmentDatetimeDesc(UUID patientId, Pageable pageable);

    /**
     * Tìm các cuộc hẹn của một bác sĩ, sắp xếp theo thời gian hẹn giảm dần, có
     * phân trang.
     *
     * @param doctorId ID của Doctor.
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách Appointment.
     */
    Page<Appointment> findByDoctor_DoctorIdOrderByAppointmentDatetimeDesc(UUID doctorId, Pageable pageable);

    /**
     * Tìm danh sách các cuộc hẹn diễn ra trong một khoảng thời gian, sắp xếp
     * theo thời gian hẹn tăng dần, có phân trang.
     *
     * @param startDateTime Thời điểm bắt đầu.
     * @param endDateTime Thời điểm kết thúc.
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách Appointment.
     */
    Page<Appointment> findByAppointmentDatetimeBetweenOrderByAppointmentDatetimeAsc(LocalDateTime startDateTime,
            LocalDateTime endDateTime, Pageable pageable);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Tìm các cuộc hẹn chưa hoàn thành (Scheduled) của một bác sĩ trước một
     * thời điểm cụ thể.
     *
     * @param doctorId ID của Doctor.
     * @param beforeTime Thời điểm giới hạn.
     * @return Danh sách các Appointment phù hợp.
     */
    /*
         * @Query("SELECT a FROM Appointment a WHERE a.doctor.doctorId = :doctorId AND a.status = :status AND a.appointmentDatetime < :beforeTime"
         * )
         * List<Appointment> findUpcomingScheduledAppointmentsForDoctor(
         * 
         * @Param("doctorId") UUID doctorId,
         * 
         * @Param("status") AppointmentStatus status, // Truyền
         * AppointmentStatus.Scheduled vào đây
         * 
         * @Param("beforeTime") LocalDateTime beforeTime);
     */
}
