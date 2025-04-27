package com.pma.service;

import com.pma.model.entity.*; // Import các entity cần thiết
import com.pma.model.enums.PrescriptionStatus; // Import Enum
import com.pma.repository.*; // Import các repository cần thiết
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.Setter;

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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set; // Cần cho PrescriptionDetails
import java.util.UUID;
import java.util.stream.Collectors; // Cần cho xử lý details

@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordRepository medicalRecordRepository; // Optional
    private final MedicineRepository medicineRepository; // Cần để lấy giá thuốc
    private final PrescriptionDetailRepository prescriptionDetailRepository; // Cần để lưu chi tiết

    @Autowired
    public PrescriptionService(PrescriptionRepository prescriptionRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            MedicalRecordRepository medicalRecordRepository,
            MedicineRepository medicineRepository,
            PrescriptionDetailRepository prescriptionDetailRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.medicineRepository = medicineRepository;
        this.prescriptionDetailRepository = prescriptionDetailRepository;
    }

    /**
     * Tạo một đơn thuốc mới bao gồm cả các chi tiết thuốc.
     *
     * @param prescription           Đối tượng Prescription (chưa có ID, Patient,
     *                               Doctor, MedicalRecord, Details).
     * @param patientId              ID của Patient.
     * @param doctorId               ID của Doctor.
     * @param medicalRecordId        (Optional) ID của MedicalRecord liên quan.
     * @param prescriptionDetailDTOs Danh sách các đối tượng chứa thông tin chi tiết
     *                               (ví dụ: medicineId, quantity, dosage,
     *                               instructions).
     *                               Nên dùng DTO (Data Transfer Object) thay vì
     *                               Entity PrescriptionDetail ở đây.
     * @return Prescription đã được lưu cùng các chi tiết.
     * @throws EntityNotFoundException  nếu Patient, Doctor, MedicalRecord (nếu có),
     *                                  hoặc Medicine không tồn tại.
     * @throws IllegalArgumentException nếu thông tin chi tiết không hợp lệ (ví dụ:
     *                                  số lượng <= 0).
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Prescription createPrescription(Prescription prescription, UUID patientId, UUID doctorId,
            UUID medicalRecordId, List<PrescriptionDetailDTO> prescriptionDetailDTOs) {
        log.info("Attempting to create prescription for patientId: {}, doctorId: {}", patientId, doctorId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + patientId));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + doctorId));

        MedicalRecord medicalRecord = null;
        if (medicalRecordId != null) {
            medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                    .orElseThrow(
                            () -> new EntityNotFoundException("Medical Record not found with id: " + medicalRecordId));
            // Kiểm tra medicalRecord có đúng của patient/doctor không (tương tự
            // AppointmentService)
            if (!medicalRecord.getPatient().getPatientId().equals(patientId)
                    || !medicalRecord.getDoctor().getDoctorId().equals(doctorId)) {
                throw new IllegalArgumentException("Medical Record does not match the specified patient or doctor.");
            }
        }

        if (prescriptionDetailDTOs == null || prescriptionDetailDTOs.isEmpty()) {
            throw new IllegalArgumentException("Prescription must have at least one detail item.");
        }

        // --- Thiết lập Prescription chính ---
        prescription.setPrescriptionId(null);
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setMedicalRecord(medicalRecord);
        prescription.setStatus(PrescriptionStatus.Active); // Trạng thái ban đầu
        if (prescription.getPrescriptionDate() == null) {
            prescription.setPrescriptionDate(LocalDate.now());
        }

        // --- Xử lý và tạo PrescriptionDetails ---
        Set<PrescriptionDetail> details = new HashSet<>();
        for (PrescriptionDetailDTO dto : prescriptionDetailDTOs) {
            if (dto.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for medicineId: " + dto.getMedicineId());
            }

            Medicine medicine = medicineRepository.findById(dto.getMedicineId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Medicine not found with id: " + dto.getMedicineId()));

            // Kiểm tra tồn kho (stockQuantity) của medicine nếu cần
            // if (medicine.getStockQuantity() < dto.getQuantity()) { ... }

            PrescriptionDetail detail = new PrescriptionDetail();
            detail.setPrescriptionDetailId(null); // Tạo mới
            detail.setMedicine(medicine);
            detail.setQuantity(dto.getQuantity());
            detail.setDosage(dto.getDosage()); // Cần validation dosage không rỗng
            detail.setInstructions(dto.getInstructions());
            detail.setUnitPrice(medicine.getPrice()); // Lấy giá hiện tại của thuốc và lưu vào chi tiết

            // Quan trọng: Thiết lập quan hệ hai chiều
            // Dùng helper method của Prescription để thêm detail và tự động set ngược lại
            prescription.addPrescriptionDetail(detail); // Giả sử helper này tự gọi
                                                        // detail.setPrescriptionInternal(prescription)

            details.add(detail); // Vẫn thêm vào set tạm thời nếu cần xử lý gì thêm trước khi save
        }

        // Lưu Prescription (và các Details sẽ được cascade lưu theo do CascadeType.ALL)
        Prescription savedPrescription = prescriptionRepository.save(prescription);
        log.info("Successfully created prescription with id: {}", savedPrescription.getPrescriptionId());

        // Có thể cần cập nhật tồn kho thuốc ở đây (trong cùng transaction)
        // updateStockQuantities(details);

        return savedPrescription;
    }

    /**
     * Lấy Prescription theo ID.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Prescription getPrescriptionById(UUID id) {
        log.info("Fetching prescription with id: {}", id);
        // Có thể dùng JOIN FETCH để tải luôn details nếu thường xuyên cần
        // return prescriptionRepository.findByIdWithDetails(id).orElseThrow(...)
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prescription not found with id: " + id));
    }

    /**
     * Lấy danh sách đơn thuốc của bệnh nhân (có phân trang).
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Prescription> getPrescriptionsByPatient(UUID patientId, Pageable pageable) {
        log.info("Fetching prescriptions for patient id: {} with pagination: {}", patientId, pageable);
        return prescriptionRepository.findByPatient_PatientIdOrderByPrescriptionDateDesc(patientId, pageable);
    }

    /**
     * Cập nhật trạng thái đơn thuốc.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Prescription updatePrescriptionStatus(UUID id, PrescriptionStatus newStatus) {
        log.info("Attempting to update status for prescription id: {} to {}", id, newStatus);
        Prescription prescription = getPrescriptionById(id);
        // Thêm logic kiểm tra chuyển đổi trạng thái nếu cần
        prescription.setStatus(newStatus);
        log.info("Prescription status updated successfully for id: {}", id);
        return prescription;
    }

    /**
     * Xóa một Prescription.
     * CẢNH BÁO: Do CascadeType.ALL cho prescriptionDetails, việc này sẽ xóa tất cả
     * chi tiết đơn thuốc liên quan.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deletePrescription(UUID id) {
        log.warn("Attempting to DELETE prescription with id: {} AND ALL ASSOCIATED DETAILS!", id);
        if (!prescriptionRepository.existsById(id)) {
            log.error("Deletion failed. Prescription not found with id: {}", id);
            throw new EntityNotFoundException("Prescription not found with id: " + id);
        }
        try {
            prescriptionRepository.deleteById(id);
            log.info("Successfully deleted prescription with id: {}", id);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation during deletion of prescription id: {}. Error: {}", id, e.getMessage());
            throw new IllegalStateException(
                    "Could not delete prescription with id " + id + " due to data integrity issues.", e);
        }
    }

    // --- DTO đơn giản cho Prescription Detail ---
    // Nên tạo class này ở package riêng (ví dụ: com.pma.dto)
    @Getter
    @Setter
    public static class PrescriptionDetailDTO {
        private UUID medicineId;
        private int quantity;
        private String dosage;
        private String instructions;
    }

    // --- Phương thức helper có thể có ---
    /*
     * @Transactional // Phải cùng transaction với createPrescription
     * protected void updateStockQuantities(Set<PrescriptionDetail> details) {
     * for (PrescriptionDetail detail : details) {
     * // Giảm số lượng tồn kho
     * int updatedRows =
     * medicineRepository.updateStockQuantity(detail.getMedicine().getMedicineId(),
     * -detail.getQuantity());
     * if (updatedRows == 0) {
     * // Xử lý lỗi nếu không cập nhật được (ví dụ: medicine bị xóa?)
     * log.error("Failed to update stock for medicineId: {}",
     * detail.getMedicine().getMedicineId());
     * // Có thể throw exception để rollback transaction
     * }
     * }
     * }
     */

    // --- Phương thức để lấy Prescription kèm Details (ví dụ JOIN FETCH) ---
    /*
     * @Transactional(readOnly = true)
     * public Optional<Prescription> findByIdWithDetails(UUID id) {
     * // Cần định nghĩa phương thức findByIdWithDetails trong
     * PrescriptionRepository dùng @Query với JOIN FETCH
     * return prescriptionRepository.findByIdWithDetails(id);
     * }
     */
}