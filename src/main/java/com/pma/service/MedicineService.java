package com.pma.service;

import com.pma.model.entity.Medicine; // Import Entity Medicine
import com.pma.model.enums.MedicineStatus; // Import Enum MedicineStatus
import com.pma.repository.MedicineRepository; // Import Repository Medicine
import com.pma.repository.PrescriptionDetailRepository; // Import nếu cần kiểm tra ràng buộc khi xóa
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lớp Service cho việc quản lý các nghiệp vụ liên quan đến Medicine.
 */
@Service
public class MedicineService {

    private static final Logger log = LoggerFactory.getLogger(MedicineService.class);

    private final MedicineRepository medicineRepository;
    private final PrescriptionDetailRepository prescriptionDetailRepository; // Cần để kiểm tra trước khi xóa

    @Autowired
    public MedicineService(MedicineRepository medicineRepository,
            PrescriptionDetailRepository prescriptionDetailRepository) {
        this.medicineRepository = medicineRepository;
        this.prescriptionDetailRepository = prescriptionDetailRepository;
    }

    /**
     * Tạo một loại thuốc mới.
     *
     * @param medicine Đối tượng Medicine chứa thông tin cần tạo.
     * @return Medicine đã được lưu.
     * @throws IllegalArgumentException nếu tên thuốc đã tồn tại.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Medicine createMedicine(Medicine medicine) {
        log.info("Attempting to create medicine with name: {}", medicine.getMedicineName());

        // --- Validation ---
        if (medicine.getMedicineName() == null || medicine.getMedicineName().trim().isEmpty()) {
            throw new IllegalArgumentException("Medicine name cannot be empty.");
        }
        if (medicine.getUnit() == null || medicine.getUnit().trim().isEmpty()) {
            throw new IllegalArgumentException("Medicine unit cannot be empty.");
        }
        if (medicine.getPrice() == null || medicine.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Medicine price must be non-negative.");
        }
        if (medicine.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }

        // Kiểm tra trùng tên (UNIQUE constraint)
        medicineRepository.findByMedicineNameIgnoreCase(medicine.getMedicineName()).ifPresent(existing -> {
            log.warn("Medicine creation failed. Name already exists: {}", medicine.getMedicineName());
            throw new IllegalArgumentException("Medicine name '" + medicine.getMedicineName() + "' already exists.");
        });

        // --- Thiết lập và Lưu ---
        medicine.setMedicineId(null); // Đảm bảo tạo mới
        // Set trạng thái mặc định nếu chưa có (mặc dù entity đã có)
        if (medicine.getStatus() == null) {
            medicine.setStatus(MedicineStatus.AVAILABLE);
        }

        Medicine savedMedicine = medicineRepository.save(medicine);
        log.info("Successfully created medicine with id: {}", savedMedicine.getMedicineId());
        return savedMedicine;
    }

    /**
     * Lấy thông tin Medicine theo ID.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Medicine getMedicineById(UUID id) {
        log.info("Fetching medicine with id: {}", id);
        return medicineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found with id: " + id));
    }

    /**
     * Lấy danh sách tất cả các loại thuốc (có phân trang).
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Medicine> getAllMedicines(Pageable pageable) {
        log.info("Fetching medicines with pagination: {}", pageable);
        Page<Medicine> medicinePage = medicineRepository.findAll(pageable);
        log.info("Found {} medicines on page {}/{}", medicinePage.getNumberOfElements(), pageable.getPageNumber(),
                medicinePage.getTotalPages());
        return medicinePage;
    }

    /**
     * Tìm kiếm thuốc theo tên (không phân biệt hoa thường).
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Medicine> searchMedicinesByName(String nameFragment) {
        log.debug("Searching for medicines with name containing: {}", nameFragment);
        return medicineRepository.findByMedicineNameContainingIgnoreCase(nameFragment);
    }

    /**
     * Cập nhật thông tin của một loại thuốc.
     *
     * @param id              UUID của Medicine cần cập nhật.
     * @param medicineDetails Đối tượng chứa thông tin mới.
     * @return Medicine đã được cập nhật.
     * @throws EntityNotFoundException  nếu không tìm thấy Medicine.
     * @throws IllegalArgumentException nếu tên mới bị trùng hoặc giá trị không hợp
     *                                  lệ.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Medicine updateMedicine(UUID id, Medicine medicineDetails) {
        log.info("Attempting to update medicine with id: {}", id);
        Medicine existingMedicine = getMedicineById(id); // Lấy entity đang quản lý

        // --- Validation và Cập nhật ---
        // Cập nhật tên nếu thay đổi và không trùng
        String newName = medicineDetails.getMedicineName();
        if (newName != null && !newName.trim().isEmpty()
                && !existingMedicine.getMedicineName().equalsIgnoreCase(newName.trim())) {
            medicineRepository.findByMedicineNameIgnoreCase(newName.trim()).ifPresent(conflicting -> {
                if (!conflicting.getMedicineId().equals(id)) { // Đảm bảo không phải chính nó
                    log.warn("Medicine update failed. New name '{}' conflicts with existing medicine id: {}",
                            newName.trim(), conflicting.getMedicineId());
                    throw new IllegalArgumentException("Medicine name '" + newName.trim() + "' is already in use.");
                }
            });
            existingMedicine.setMedicineName(newName.trim());
            log.info("Medicine name updated for id: {}", id);
        }

        // Cập nhật các trường khác nếu được cung cấp và hợp lệ
        if (medicineDetails.getManufacturer() != null)
            existingMedicine.setManufacturer(medicineDetails.getManufacturer());
        if (medicineDetails.getUnit() != null && !medicineDetails.getUnit().trim().isEmpty())
            existingMedicine.setUnit(medicineDetails.getUnit());
        if (medicineDetails.getDescription() != null)
            existingMedicine.setDescription(medicineDetails.getDescription());
        if (medicineDetails.getPrice() != null && medicineDetails.getPrice().compareTo(BigDecimal.ZERO) >= 0)
            existingMedicine.setPrice(medicineDetails.getPrice());
        if (medicineDetails.getStatus() != null)
            existingMedicine.setStatus(medicineDetails.getStatus());
        // Không nên cho phép cập nhật tồn kho trực tiếp qua đây, nên có phương thức
        // riêng

        // Transaction commit sẽ lưu thay đổi
        log.info("Medicine details update process completed for id: {}", id);
        return existingMedicine;
    }

    /**
     * Cập nhật số lượng tồn kho cho một loại thuốc.
     * Có thể dùng số âm để giảm tồn kho.
     *
     * @param medicineId     ID của thuốc.
     * @param quantityChange Số lượng thay đổi (dương để tăng, âm để giảm).
     * @return Medicine với số lượng tồn kho đã cập nhật.
     * @throws EntityNotFoundException  nếu không tìm thấy Medicine.
     * @throws IllegalArgumentException nếu số lượng tồn kho mới sẽ thành âm.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Medicine updateStockQuantity(UUID medicineId, int quantityChange) {
        log.info("Attempting to update stock quantity for medicine id: {} by {}", medicineId, quantityChange);
        Medicine medicine = getMedicineById(medicineId);

        int newStock = medicine.getStockQuantity() + quantityChange;
        if (newStock < 0) {
            log.warn("Stock update failed for medicine id: {}. New stock quantity ({}) cannot be negative.", medicineId,
                    newStock);
            throw new IllegalArgumentException("Cannot update stock. Resulting quantity would be negative.");
        }

        medicine.setStockQuantity(newStock);

        // Cập nhật trạng thái nếu cần (ví dụ: hết hàng)
        if (newStock == 0 && medicine.getStatus() == MedicineStatus.AVAILABLE) {
            medicine.setStatus(MedicineStatus.OUT_OF_STOCK);
            log.info("Medicine status set to OUT_OF_STOCK for id: {} due to zero stock.", medicineId);
        } else if (newStock > 0 && medicine.getStatus() == MedicineStatus.DISCONTINUED) {
            // Tự động chuyển về Available nếu có hàng lại? Tùy logic nghiệp vụ
            // medicine.setStatus(MedicineStatus.Available);
        }

        // Dùng save() rõ ràng để đảm bảo cập nhật được thực hiện ngay lập tức nếu cần
        // đọc lại giá trị mới
        Medicine updatedMedicine = medicineRepository.save(medicine);
        log.info("Successfully updated stock quantity for medicine id: {} to {}", medicineId, newStock);
        return updatedMedicine;
    }

    /**
     * Xóa một loại thuốc theo ID.
     * Chỉ xóa được nếu không có PrescriptionDetail nào đang tham chiếu đến nó.
     * Thường thì không nên xóa thuốc mà nên đổi trạng thái thành Discontinued.
     *
     * @param id UUID của Medicine cần xóa.
     * @throws EntityNotFoundException nếu không tìm thấy Medicine.
     * @throws IllegalStateException   nếu Medicine đang được sử dụng trong
     *                                 PrescriptionDetail.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteMedicine(UUID id) {
        log.warn(
                "Attempting to DELETE medicine with id: {}. Consider setting status to Discontinued instead. This will fail if any prescription detail references it.",
                id);

        // 1. Kiểm tra tồn tại
        if (!medicineRepository.existsById(id)) {
            log.error("Deletion failed. Medicine not found with id: {}", id);
            throw new EntityNotFoundException("Medicine not found with id: " + id);
        }

        // 2. Kiểm tra ràng buộc khóa ngoại với PrescriptionDetail
        // **Yêu cầu: Phải thêm `long countByMedicine_MedicineId(UUID medicineId);` vào
        // PrescriptionDetailRepository**
        long detailCount = prescriptionDetailRepository.countByMedicine_MedicineId(id);
        if (detailCount > 0) {
            log.warn("Deletion failed for medicine id: {}. Found {} associated prescription details.", id, detailCount);
            throw new IllegalStateException("Cannot delete medicine with id " + id + " because " +
                    detailCount + " prescription detail(s) are still associated with it.");
        }

        // 3. Nếu không có ràng buộc, tiến hành xóa
        try {
            medicineRepository.deleteById(id);
            log.info("Successfully deleted medicine with id: {}", id);
        } catch (DataIntegrityViolationException e) {
            // Dự phòng nếu có lỗi ràng buộc khác
            log.error("Data integrity violation during deletion of medicine id: {}. Error: {}", id, e.getMessage());
            throw new IllegalStateException(
                    "Could not delete medicine with id " + id + " due to data integrity issues.", e);
        }
    }

    // --- Cần thêm vào PrescriptionDetailRepository ---
    // long countByMedicine_MedicineId(UUID medicineId);
}