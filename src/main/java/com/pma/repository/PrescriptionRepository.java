package com.pma.repository; // Đảm bảo đúng package

import java.time.LocalDate; // Import Doctor để tìm theo bác sĩ
import java.util.List;// Import MedicalRecord để tìm theo bản ghi y tế
import java.util.Optional;
import java.util.UUID; // Import Patient để tìm theo bệnh nhân

import org.springframework.data.domain.Page; // Import Entity Prescription
import org.springframework.data.domain.Pageable; // Import Enum PrescriptionStatus
import org.springframework.data.jpa.repository.EntityGraph; // Import cho phân trang
import org.springframework.data.jpa.repository.JpaRepository; // Import cho phân trang
import org.springframework.stereotype.Repository;

import com.pma.model.entity.Doctor;
import com.pma.model.entity.MedicalRecord; // Import nếu dùng @Query
import com.pma.model.entity.Patient; // Import nếu dùng @Query với tham số
import com.pma.model.entity.Prescription;
import com.pma.model.enums.PrescriptionStatus; // Import nếu tìm theo ngày

/**
 * Spring Data JPA repository cho thực thể Prescription.
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    // Kế thừa JpaRepository<Prescription, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.
    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---
    /**
     * Finds a Prescription by its ID, eagerly fetching its associated Doctor,
     * PrescriptionDetails, and the Medicine for each detail.
     *
     * @param prescriptionId The ID of the Prescription to find.
     * @return An Optional containing the Prescription if found, otherwise empty.
     */
    @EntityGraph(attributePaths = {"doctor", "prescriptionDetails.medicine"})
    Optional<Prescription> findById(UUID prescriptionId);

    /**
     * Tìm danh sách các đơn thuốc của một bệnh nhân cụ thể.
     *
     * @param patient Đối tượng Patient.
     * @return Danh sách các Prescription của bệnh nhân đó.
     */
    @EntityGraph(attributePaths = {"doctor", "prescriptionDetails.medicine"})
    List<Prescription> findByPatient(Patient patient);

    /**
     * Tìm danh sách các đơn thuốc của một bệnh nhân theo ID của bệnh nhân.
     *
     * @param patientId ID của Patient.
     * @return Danh sách các Prescription của bệnh nhân đó.
     */
    @EntityGraph(attributePaths = {"doctor", "prescriptionDetails.medicine"})
    List<Prescription> findByPatient_PatientId(UUID patientId);

    /**
     * Tìm danh sách các đơn thuốc được kê bởi một bác sĩ cụ thể.
     *
     * @param doctor Đối tượng Doctor.
     * @return Danh sách các Prescription được kê bởi bác sĩ đó.
     */
    @EntityGraph(attributePaths = {"patient"})
    List<Prescription> findByDoctor(Doctor doctor);

    /**
     * Tìm danh sách các đơn thuốc được kê bởi một bác sĩ theo ID của bác sĩ.
     *
     * @param doctorId ID của Doctor.
     * @return Danh sách các Prescription được kê bởi bác sĩ đó.
     */
    @EntityGraph(attributePaths = {"patient"})
    List<Prescription> findByDoctor_DoctorId(UUID doctorId);

    /**
     * Tìm danh sách các đơn thuốc liên quan đến một bản ghi y tế cụ thể.
     *
     * @param medicalRecord Đối tượng MedicalRecord.
     * @return Danh sách các Prescription liên quan.
     */
    List<Prescription> findByMedicalRecord(MedicalRecord medicalRecord);

    /**
     * Tìm danh sách các đơn thuốc liên quan đến một bản ghi y tế theo ID.
     *
     * @param recordId ID của MedicalRecord.
     * @return Danh sách các Prescription liên quan.
     */
    List<Prescription> findByMedicalRecord_RecordId(UUID recordId);

    /**
     * Tìm danh sách các đơn thuốc theo trạng thái.
     *
     * @param status Trạng thái cần lọc (sử dụng kiểu Enum).
     * @return Danh sách các Prescription có trạng thái tương ứng.
     */
    List<Prescription> findByStatus(PrescriptionStatus status);

    /**
     * Tìm danh sách các đơn thuốc được kê trong một khoảng ngày.
     *
     * @param startDate Ngày bắt đầu (bao gồm).
     * @param endDate Ngày kết thúc (bao gồm).
     * @return Danh sách các Prescription phù hợp.
     */
    List<Prescription> findByPrescriptionDateBetween(LocalDate startDate, LocalDate endDate);

    // --- Ví dụ sử dụng Phân trang và Sắp xếp ---
    /**
     * Tìm các đơn thuốc của một bệnh nhân, sắp xếp theo ngày kê đơn giảm dần,
     * có phân trang.
     *
     * @param patientId ID của Patient.
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách Prescription.
     */
    @EntityGraph(attributePaths = {"doctor", "prescriptionDetails.medicine"}) // Eagerly fetch related entities
    Page<Prescription> findByPatient_PatientIdOrderByPrescriptionDateDesc(UUID patientId, Pageable pageable);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Tìm các đơn thuốc đang hoạt động (Active) cho một bệnh nhân cụ thể.
     *
     * @param patientId ID của Patient.
     * @return Danh sách các Prescription đang hoạt động.
     */
    /*
     * @Query("SELECT p FROM Prescription p WHERE p.patient.patientId = :patientId AND p.status = :status"
     * )
     * List<Prescription> findActivePrescriptionsForPatient(
     * 
     * @Param("patientId") UUID patientId,
     * 
     * @Param("status") PrescriptionStatus status); // Truyền
     * PrescriptionStatus.Active
     */
}
