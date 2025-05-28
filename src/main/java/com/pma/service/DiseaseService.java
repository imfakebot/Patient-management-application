package com.pma.service;

import java.util.List;
import java.util.Optional; // Import Entity Disease

import org.slf4j.Logger; // Import nếu cần kiểm tra ràng buộc khi xóa
import org.slf4j.LoggerFactory; // Import Repository Disease
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation; // Import để bắt lỗi xóa
import org.springframework.transaction.annotation.Transactional;

import com.pma.model.entity.Disease;
import com.pma.repository.DiagnosisRepository;
import com.pma.repository.DiseaseRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Lớp Service cho việc quản lý các nghiệp vụ liên quan đến Disease (Loại bệnh).
 * Chủ yếu là các thao tác CRUD và tìm kiếm cơ bản.
 */
@Service
public class DiseaseService {

    private static final Logger log = LoggerFactory.getLogger(DiseaseService.class);

    private final DiseaseRepository diseaseRepository;
    private final DiagnosisRepository diagnosisRepository; // Cần để kiểm tra trước khi xóa

    @Autowired
    public DiseaseService(DiseaseRepository diseaseRepository, DiagnosisRepository diagnosisRepository) {
        this.diseaseRepository = diseaseRepository;
        this.diagnosisRepository = diagnosisRepository;
    }

    /**
     * Lấy danh sách tất cả các loại bệnh.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Disease> getAllDiseases() {
        log.info("Fetching all diseases");
        List<Disease> diseases = diseaseRepository.findAll();
        log.info("Found {} diseases", diseases.size());
        return diseases;
    }

    /**
     * Tìm một loại bệnh theo mã bệnh (diseaseCode - Khóa chính).
     *
     * @param diseaseCode Mã bệnh cần tìm (kiểu String).
     * @return Đối tượng Disease.
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Disease getDiseaseByCode(String diseaseCode) {
        log.info("Fetching disease with code: {}", diseaseCode);
        return diseaseRepository.findById(diseaseCode) // findById nhận String
                .orElseThrow(() -> {
                    log.warn("Disease not found with code: {}", diseaseCode);
                    return new EntityNotFoundException("Disease not found with code: " + diseaseCode);
                });
    }

    /**
     * Tìm một loại bệnh theo tên (không phân biệt hoa thường).
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Disease> findDiseaseByNameIgnoreCase(String name) {
        log.info("Searching for disease with name (case-insensitive): {}", name);
        return diseaseRepository.findByDiseaseNameIgnoreCase(name);
    }

    /**
     * Tạo một loại bệnh mới. Lưu ý: diseaseCode là assigned identifier, phải
     * được cung cấp và là duy nhất.
     *
     * @param disease Đối tượng Disease chứa thông tin (bao gồm diseaseCode).
     * @return Disease đã được lưu.
     * @throws IllegalArgumentException nếu diseaseCode hoặc diseaseName đã tồn
     * tại.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Disease createDisease(Disease disease) {
        log.info("Attempting to create disease with code: {}", disease.getDiseaseCode());

        // --- Validation ---
        if (disease.getDiseaseCode() == null || disease.getDiseaseCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Disease code cannot be empty.");
        }
        if (disease.getDiseaseName() == null || disease.getDiseaseName().trim().isEmpty()) {
            throw new IllegalArgumentException("Disease name cannot be empty.");
        }

        // Kiểm tra trùng diseaseCode (khóa chính)
        if (diseaseRepository.existsById(disease.getDiseaseCode())) {
            log.warn("Disease creation failed. Code already exists: {}", disease.getDiseaseCode());
            throw new IllegalArgumentException("Disease code '" + disease.getDiseaseCode() + "' already exists.");
        }
        // Kiểm tra trùng diseaseName (UNIQUE constraint)
        findDiseaseByNameIgnoreCase(disease.getDiseaseName()).ifPresent(_ -> {
            log.warn("Disease creation failed. Name already exists: {}", disease.getDiseaseName());
            throw new IllegalArgumentException("Disease name '" + disease.getDiseaseName() + "' already exists.");
        });

        // --- Data Access ---
        Disease savedDisease = diseaseRepository.save(disease);
        log.info("Successfully created disease with code: {}", savedDisease.getDiseaseCode());
        return savedDisease;
    }

    /**
     * Cập nhật thông tin một loại bệnh (ví dụ: tên, mô tả). Không cho phép cập
     * nhật diseaseCode (khóa chính).
     *
     * @param diseaseCode Mã bệnh cần cập nhật.
     * @param diseaseDetails Đối tượng chứa thông tin mới (tên, mô tả).
     * @return Disease đã được cập nhật.
     * @throws EntityNotFoundException nếu không tìm thấy Disease.
     * @throws IllegalArgumentException nếu tên mới bị trùng.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Disease updateDisease(String diseaseCode, Disease diseaseDetails) {
        log.info("Attempting to update disease with code: {}", diseaseCode);
        Disease existingDisease = getDiseaseByCode(diseaseCode); // Lấy entity đang quản lý

        // --- Validation ---
        String newName = diseaseDetails.getDiseaseName();
        if (newName != null && !newName.trim().isEmpty()
                && !existingDisease.getDiseaseName().equalsIgnoreCase(newName.trim())) {
            findDiseaseByNameIgnoreCase(newName.trim()).ifPresent(conflicting -> {
                log.warn("Disease update failed. New name '{}' conflicts with existing disease code: {}",
                        newName.trim(), conflicting.getDiseaseCode());
                throw new IllegalArgumentException("Disease name '" + newName.trim() + "' is already in use.");
            });
            existingDisease.setDiseaseName(newName.trim());
            log.info("Disease name updated for code: {}", diseaseCode);
        }

        // Cập nhật mô tả nếu có
        if (diseaseDetails.getDescription() != null) {
            existingDisease.setDescription(diseaseDetails.getDescription());
            log.info("Disease description updated for code: {}", diseaseCode);
        }

        // Transaction commit sẽ lưu thay đổi
        log.info("Disease update process completed for code: {}", diseaseCode);
        return existingDisease;
    }

    /**
     * Xóa một loại bệnh theo mã bệnh. Chỉ xóa được nếu không có Diagnosis nào
     * đang tham chiếu đến nó.
     *
     * @param diseaseCode Mã bệnh cần xóa.
     * @throws EntityNotFoundException nếu không tìm thấy Disease.
     * @throws IllegalStateException nếu Disease đang được sử dụng trong
     * Diagnosis.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteDisease(String diseaseCode) {
        log.warn("Attempting to DELETE disease with code: {}. This will fail if any diagnosis references it.",
                diseaseCode);

        // 1. Kiểm tra tồn tại
        if (!diseaseRepository.existsById(diseaseCode)) {
            log.error("Deletion failed. Disease not found with code: {}", diseaseCode);
            throw new EntityNotFoundException("Disease not found with code: " + diseaseCode);
        }

        // 2. Kiểm tra ràng buộc khóa ngoại với Diagnosis
        // **Yêu cầu: Phải thêm `long countByDisease_DiseaseCode(String diseaseCode);`
        // vào DiagnosisRepository**
        long diagnosisCount = diagnosisRepository.countByDisease_DiseaseCode(diseaseCode);
        if (diagnosisCount > 0) {
            log.warn("Deletion failed for disease code: {}. Found {} associated diagnoses.", diseaseCode,
                    diagnosisCount);
            throw new IllegalStateException("Cannot delete disease with code " + diseaseCode + " because "
                    + diagnosisCount + " diagnosis/diagnoses are still associated with it.");
        }

        // 3. Nếu không có ràng buộc, tiến hành xóa
        try {
            diseaseRepository.deleteById(diseaseCode);
            log.info("Successfully deleted disease with code: {}", diseaseCode);
        } catch (DataIntegrityViolationException e) {
            // Dự phòng nếu có lỗi ràng buộc khác
            log.error("Data integrity violation during deletion of disease code: {}. Error: {}", diseaseCode,
                    e.getMessage());
            throw new IllegalStateException(
                    "Could not delete disease with code " + diseaseCode + " due to data integrity issues.", e);
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Disease> findByDiseaseCode(String code) {
        log.debug("Finding disease by code: {}", code);
        return diseaseRepository.findById(code);
    }

    // --- Cần thêm vào DiagnosisRepository ---
    // long countByDisease_DiseaseCode(String diseaseCode);
}
