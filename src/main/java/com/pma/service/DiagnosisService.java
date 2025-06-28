package com.pma.service;

import com.pma.model.entity.Diagnosis; // Import Entity Diagnosis
import com.pma.model.entity.Disease;
import com.pma.model.entity.MedicalRecord;
import com.pma.model.enums.DiagnosisStatus; // Import Enum DiagnosisStatus
import com.pma.repository.DiagnosisRepository; // Import Repository Diagnosis
import com.pma.repository.DiseaseRepository; // Import để lấy Disease
import com.pma.repository.MedicalRecordRepository; // Import để lấy MedicalRecord
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lớp Service cho việc quản lý các nghiệp vụ liên quan đến Diagnosis.
 */
@Service
public class DiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisService.class);

    private final DiagnosisRepository diagnosisRepository;
    private final MedicalRecordRepository medicalRecordRepository; // Cần để liên kết Diagnosis với MedicalRecord
    private final DiseaseRepository diseaseRepository; // Cần để liên kết Diagnosis với Disease

    @Autowired
    public DiagnosisService(DiagnosisRepository diagnosisRepository,
            MedicalRecordRepository medicalRecordRepository,
            DiseaseRepository diseaseRepository) {
        this.diagnosisRepository = diagnosisRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.diseaseRepository = diseaseRepository;
    }

    /**
     * Tạo một chẩn đoán mới và liên kết nó với MedicalRecord và Disease. Lưu ý:
     * Cách này tạo Diagnosis độc lập. Thường thì Diagnosis được tạo như một
     * phần của việc tạo/cập nhật MedicalRecord thông qua cascade. Tuy nhiên,
     * phương thức này hữu ích nếu cần tạo Diagnosis riêng lẻ.
     *
     * @param diagnosis Đối tượng Diagnosis (chưa có ID, MedicalRecord,
     * Disease).
     * @param recordId ID của MedicalRecord.
     * @param diseaseCode Mã của Disease.
     * @return Diagnosis đã được lưu.
     * @throws EntityNotFoundException nếu MedicalRecord hoặc Disease không tồn
     * tại.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Diagnosis createDiagnosis(Diagnosis diagnosis, UUID recordId, String diseaseCode) {
        log.info("Attempting to create diagnosis for recordId: {} and diseaseCode: {}", recordId, diseaseCode);

        MedicalRecord medicalRecord = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Medical Record not found with id: " + recordId));
        Disease disease = diseaseRepository.findById(diseaseCode)
                .orElseThrow(() -> new EntityNotFoundException("Disease not found with code: " + diseaseCode));

        diagnosis.setDiagnosisId(null); // Đảm bảo tạo mới
        diagnosis.setMedicalRecord(medicalRecord); // Dùng helper nếu có
        diagnosis.setDisease(disease); // Dùng helper nếu có
        if (diagnosis.getDiagnosisDate() == null) {
            diagnosis.setDiagnosisDate(LocalDate.now());
        }

        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        log.info("Successfully created diagnosis with id: {}", savedDiagnosis.getDiagnosisId());
        return savedDiagnosis;
    }

    /**
     * Lấy Diagnosis theo ID.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Diagnosis getDiagnosisById(UUID id) {
        log.info("Fetching diagnosis with id: {}", id);
        return diagnosisRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Diagnosis not found with id: " + id));
    }

    /**
     * Lấy danh sách tất cả các chẩn đoán cho một Medical Record.
     *
     * @param recordId ID của Medical Record.
     * @return List các Diagnosis.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Diagnosis> getDiagnosesByMedicalRecord(UUID recordId) {
        log.info("Fetching diagnoses for medical record id: {}", recordId);
        // Optional: Kiểm tra MedicalRecord tồn tại
        // if (!medicalRecordRepository.existsById(recordId)) { throw ... }
        return diagnosisRepository.findByMedicalRecord_RecordId(recordId);
    }

    /**
     * Lấy danh sách các chẩn đoán cho một Bệnh nhân.
     *
     * @param patientId ID của Patient.
     * @return List các Diagnosis.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Diagnosis> getDiagnosesByPatient(UUID patientId) {
        log.info("Fetching diagnoses for patient id: {}", patientId);
        // Sử dụng phương thức @Query đã định nghĩa trong DiagnosisRepository
        return diagnosisRepository.findDiagnosesByPatientId(patientId);
    }

    /**
     * Lấy danh sách tất cả các loại bệnh.
     *
     * @return List các Disease.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Disease> getAllDiseases() {
        log.info("Fetching all diseases for selection.");
        return diseaseRepository.findAll();
    }

    /**
     * Cập nhật trạng thái hoặc mô tả của một Diagnosis.
     *
     * @param diagnosisId ID của Diagnosis cần cập nhật.
     * @param newStatus (Optional) Trạng thái mới.
     * @param newDescription (Optional) Mô tả mới.
     * @return Diagnosis đã được cập nhật.
     * @throws EntityNotFoundException nếu không tìm thấy Diagnosis.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Diagnosis updateDiagnosisDetails(UUID diagnosisId, DiagnosisStatus newStatus, String newDescription) {
        log.info("Attempting to update diagnosis with id: {}", diagnosisId);
        Diagnosis existingDiagnosis = getDiagnosisById(diagnosisId);

        boolean updated = false;
        if (newStatus != null && existingDiagnosis.getStatus() != newStatus) {
            existingDiagnosis.setStatus(newStatus);
            log.info("Diagnosis status updated for id: {}", diagnosisId);
            updated = true;
        }
        if (newDescription != null && !Objects.equals(existingDiagnosis.getDiagnosisDescription(), newDescription)) {
            existingDiagnosis.setDiagnosisDescription(newDescription);
            log.info("Diagnosis description updated for id: {}", diagnosisId);
            updated = true;
        }

        if (updated) {
            log.info("Diagnosis update process completed for id: {}", diagnosisId);
            // Transaction commit sẽ lưu
        } else {
            log.info("No changes detected for diagnosis id: {}", diagnosisId);
        }
        return existingDiagnosis;
    }

    /**
     * Xóa một Diagnosis theo ID. Thường được quản lý bởi cascade từ
     * MedicalRecord, nhưng phương thức này cho phép xóa độc lập nếu cần (và nếu
     * không có ràng buộc nào khác).
     *
     * @param diagnosisId ID của Diagnosis cần xóa.
     * @throws EntityNotFoundException nếu không tìm thấy Diagnosis.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteDiagnosis(UUID diagnosisId) {
        log.warn("Attempting to DELETE diagnosis with id: {}", diagnosisId);
        if (!diagnosisRepository.existsById(diagnosisId)) {
            log.error("Deletion failed. Diagnosis not found with id: {}", diagnosisId);
            throw new EntityNotFoundException("Diagnosis not found with id: " + diagnosisId);
        }
        try {
            // Ngắt kết nối hai chiều trước khi xóa nếu cần (mặc dù cascade thường xử lý)
            // Diagnosis diagnosis = getDiagnosisById(diagnosisId);
            // if(diagnosis.getMedicalRecord() != null)
            // diagnosis.getMedicalRecord().removeDiagnosisInternal(diagnosis);
            // if(diagnosis.getDisease() != null)
            // diagnosis.getDisease().removeDiagnosisInternal(diagnosis);

            diagnosisRepository.deleteById(diagnosisId);
            log.info("Successfully deleted diagnosis with id: {}", diagnosisId);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation during deletion of diagnosis id: {}. Error: {}", diagnosisId,
                    e.getMessage());
            throw new IllegalStateException(
                    "Could not delete diagnosis with id " + diagnosisId + " due to data integrity issues.", e);
        }
    }
}
