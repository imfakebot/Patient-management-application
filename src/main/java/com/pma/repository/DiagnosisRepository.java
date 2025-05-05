package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Diagnosis; // Import Entity Diagnosis
import com.pma.model.entity.Disease; // Import Disease để tìm theo loại bệnh
import com.pma.model.entity.MedicalRecord;// Import MedicalRecord để tìm theo bản ghi y tế
import com.pma.model.enums.DiagnosisStatus; // Import Enum DiagnosisStatus
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.data.repository.query.Param; // Import nếu dùng @Query với tham số
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // Import nếu tìm theo ngày
import java.util.List;
import java.util.UUID; // Kiểu dữ liệu của khóa chính (diagnosisId)

/**
 * Spring Data JPA repository cho thực thể Diagnosis.
 */
@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
        // Kế thừa JpaRepository<Diagnosis, UUID>

        // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
        // save, findById, findAll, deleteById, count, existsById, etc.

        // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

        /**
         * Tìm tất cả các chẩn đoán thuộc về một bản ghi y tế cụ thể.
         * 
         * @param medicalRecord Đối tượng MedicalRecord.
         * @return Danh sách các Diagnosis thuộc bản ghi đó.
         */
        List<Diagnosis> findByMedicalRecord(MedicalRecord medicalRecord);

        /**
         * Tìm tất cả các chẩn đoán thuộc về một bản ghi y tế theo ID của bản ghi.
         * 
         * @param recordId ID của MedicalRecord.
         * @return Danh sách các Diagnosis thuộc bản ghi đó.
         */
        List<Diagnosis> findByMedicalRecord_RecordId(UUID recordId);

        /**
         * Tìm tất cả các chẩn đoán liên quan đến một loại bệnh cụ thể.
         * 
         * @param disease Đối tượng Disease.
         * @return Danh sách các Diagnosis liên quan đến bệnh đó.
         */
        List<Diagnosis> findByDisease(Disease disease);

        /**
         * Tìm tất cả các chẩn đoán liên quan đến một loại bệnh theo mã bệnh.
         * 
         * @param diseaseCode Mã bệnh (ví dụ: ICD-10 code).
         * @return Danh sách các Diagnosis liên quan đến bệnh đó.
         */
        List<Diagnosis> findByDisease_DiseaseCode(String diseaseCode);

        /**
         * Tìm các chẩn đoán theo trạng thái.
         * 
         * @param status Trạng thái cần lọc (sử dụng kiểu Enum).
         * @return Danh sách các Diagnosis có trạng thái tương ứng.
         */
        List<Diagnosis> findByStatus(DiagnosisStatus status);

        /**
         * Tìm các chẩn đoán được thực hiện trong một khoảng ngày.
         * 
         * @param startDate Ngày bắt đầu (bao gồm).
         * @param endDate   Ngày kết thúc (bao gồm).
         * @return Danh sách các Diagnosis phù hợp.
         */
        List<Diagnosis> findByDiagnosisDateBetween(LocalDate startDate, LocalDate endDate);

        /**
         * Tìm các chẩn đoán của một bệnh nhân (thông qua MedicalRecord).
         * Cần dùng @Query vì liên quan đến join qua nhiều bảng.
         */
        @Query("SELECT diag FROM Diagnosis diag JOIN diag.medicalRecord mr WHERE mr.patient.patientId = :patientId")
        List<Diagnosis> findDiagnosesByPatientId(@Param("patientId") UUID patientId);

        /**
         * Tìm các chẩn đoán của một bệnh nhân cho một loại bệnh cụ thể.
         */
        @Query("SELECT diag FROM Diagnosis diag JOIN diag.medicalRecord mr WHERE mr.patient.patientId = :patientId AND diag.disease.diseaseCode = :diseaseCode")
        List<Diagnosis> findDiagnosesByPatientIdAndDiseaseCode(
                        @Param("patientId") UUID patientId,
                        @Param("diseaseCode") String diseaseCode);

        /**
         * Tìm các chẩn đoán của một bệnh nhân theo trạng thái.
         */
        @Query("SELECT diag FROM Diagnosis diag JOIN diag.medicalRecord mr WHERE mr.patient.patientId = :patientId AND diag.status = :status")
        List<Diagnosis> findDiagnosesByPatientIdAndStatus(
                        @Param("patientId") UUID patientId,
                        @Param("status") DiagnosisStatus status);

        /**
         * Đếm số lượng chẩn đoán liên quan đến một loại bệnh theo mã bệnh.
         * 
         * @param diseaseCode Mã bệnh (ví dụ: ICD-10 code).
         * @return Số lượng Diagnosis liên quan đến bệnh đó.
         */
        long countByDisease_DiseaseCode(String diseaseCode);
}