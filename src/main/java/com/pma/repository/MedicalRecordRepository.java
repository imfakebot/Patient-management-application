package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Appointment; // Import Appointment để tìm theo cuộc hẹn
import com.pma.model.entity.Doctor; // Import Doctor để tìm theo bác sĩ
import com.pma.model.entity.MedicalRecord;// Import Entity MedicalRecord
import com.pma.model.entity.Patient; // Import Patient để tìm theo bệnh nhân
import org.springframework.data.domain.Page; // Import cho phân trang
import org.springframework.data.domain.Pageable; // Import cho phân trang
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.data.repository.query.Param; // Import nếu dùng @Query với tham số
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // Import nếu tìm theo ngày
import java.util.List;
import java.util.UUID; // Kiểu dữ liệu của khóa chính (recordId)

/**
 * Spring Data JPA repository cho thực thể MedicalRecord.
 */
@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    // Kế thừa JpaRepository<MedicalRecord, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.

    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

    /**
     * Tìm danh sách các bản ghi y tế của một bệnh nhân cụ thể.
     * 
     * @param patient Đối tượng Patient.
     * @return Danh sách các MedicalRecord của bệnh nhân đó.
     */
    List<MedicalRecord> findByPatient(Patient patient);

    /**
     * Tìm danh sách các bản ghi y tế của một bệnh nhân theo ID của bệnh nhân.
     * 
     * @param patientId ID của Patient.
     * @return Danh sách các MedicalRecord của bệnh nhân đó.
     */
    List<MedicalRecord> findByPatient_PatientId(UUID patientId);

    /**
     * Tìm danh sách các bản ghi y tế được tạo bởi một bác sĩ cụ thể.
     * 
     * @param doctor Đối tượng Doctor.
     * @return Danh sách các MedicalRecord được tạo bởi bác sĩ đó.
     */
    List<MedicalRecord> findByDoctor(Doctor doctor);

    /**
     * Tìm danh sách các bản ghi y tế được tạo bởi một bác sĩ theo ID của bác sĩ.
     * 
     * @param doctorId ID của Doctor.
     * @return Danh sách các MedicalRecord được tạo bởi bác sĩ đó.
     */
    List<MedicalRecord> findByDoctor_DoctorId(UUID doctorId);

    /**
     * Tìm danh sách các bản ghi y tế liên quan đến một cuộc hẹn cụ thể.
     * 
     * @param appointment Đối tượng Appointment.
     * @return Danh sách các MedicalRecord liên quan (có thể rỗng).
     */
    List<MedicalRecord> findByAppointment(Appointment appointment);

    /**
     * Tìm danh sách các bản ghi y tế liên quan đến một cuộc hẹn theo ID.
     * 
     * @param appointmentId ID của Appointment.
     * @return Danh sách các MedicalRecord liên quan.
     */
    List<MedicalRecord> findByAppointment_AppointmentId(UUID appointmentId);

    /**
     * Tìm danh sách các bản ghi y tế được ghi nhận trong một khoảng ngày.
     * 
     * @param startDate Ngày bắt đầu (bao gồm).
     * @param endDate   Ngày kết thúc (bao gồm).
     * @return Danh sách các MedicalRecord phù hợp.
     */
    List<MedicalRecord> findByRecordDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Tìm danh sách các bản ghi y tế của một bệnh nhân được ghi nhận trong một
     * khoảng ngày.
     * 
     * @param patientId ID của Patient.
     * @param startDate Ngày bắt đầu.
     * @param endDate   Ngày kết thúc.
     * @return Danh sách MedicalRecord phù hợp.
     */
    List<MedicalRecord> findByPatient_PatientIdAndRecordDateBetween(UUID patientId, LocalDate startDate,
            LocalDate endDate);

    // --- Ví dụ sử dụng Phân trang và Sắp xếp ---
    /**
     * Tìm các bản ghi y tế của một bệnh nhân, sắp xếp theo ngày ghi nhận giảm dần,
     * có phân trang.
     * 
     * @param patientId ID của Patient.
     * @param pageable  Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách MedicalRecord.
     */
    Page<MedicalRecord> findByPatient_PatientIdOrderByRecordDateDesc(UUID patientId, Pageable pageable);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Tìm các bản ghi y tế chứa một từ khóa trong phần ghi chú (notes).
     * 
     * @param keyword Từ khóa tìm kiếm.
     * @return Danh sách các MedicalRecord phù hợp.
     */
    /*
     * @Query("SELECT mr FROM MedicalRecord mr WHERE LOWER(mr.notes) LIKE LOWER(concat('%', :keyword, '%'))"
     * )
     * List<MedicalRecord> searchByNotesContaining(@Param("keyword") String
     * keyword);
     */

}