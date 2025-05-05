package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Medicine; // Import Medicine để tìm theo thuốc
import com.pma.model.entity.Prescription; // Import Prescription để tìm theo đơn thuốc
import com.pma.model.entity.PrescriptionDetail; // Import Entity PrescriptionDetail
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.data.repository.query.Param; // Import nếu dùng @Query với tham số
import org.springframework.stereotype.Repository;

import java.math.BigDecimal; // Import nếu tìm theo giá
import java.util.List;
import java.util.UUID; // Kiểu dữ liệu của khóa chính (prescriptionDetailId)

/**
 * Spring Data JPA repository cho thực thể PrescriptionDetail.
 */
@Repository
public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, UUID> {
    // Kế thừa JpaRepository<PrescriptionDetail, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.

    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

    /**
     * Tìm tất cả các chi tiết thuộc về một đơn thuốc cụ thể.
     * 
     * @param prescription Đối tượng Prescription.
     * @return Danh sách các PrescriptionDetail thuộc đơn thuốc đó.
     */
    List<PrescriptionDetail> findByPrescription(Prescription prescription);

    /**
     * Tìm tất cả các chi tiết thuộc về một đơn thuốc theo ID của đơn thuốc.
     * 
     * @param prescriptionId ID của Prescription.
     * @return Danh sách các PrescriptionDetail thuộc đơn thuốc đó.
     */
    List<PrescriptionDetail> findByPrescription_PrescriptionId(UUID prescriptionId);

    /**
     * Tìm tất cả các chi tiết đơn thuốc liên quan đến một loại thuốc cụ thể.
     * 
     * @param medicine Đối tượng Medicine.
     * @return Danh sách các PrescriptionDetail liên quan đến thuốc đó.
     */
    List<PrescriptionDetail> findByMedicine(Medicine medicine);

    /**
     * Tìm tất cả các chi tiết đơn thuốc liên quan đến một loại thuốc theo ID của
     * thuốc.
     * 
     * @param medicineId ID của Medicine.
     * @return Danh sách các PrescriptionDetail liên quan đến thuốc đó.
     */
    List<PrescriptionDetail> findByMedicine_MedicineId(UUID medicineId);

    /**
     * Tìm các chi tiết đơn thuốc có số lượng lớn hơn một giá trị nhất định.
     * 
     * @param quantity Số lượng tối thiểu (không bao gồm).
     * @return Danh sách các PrescriptionDetail phù hợp.
     */
    List<PrescriptionDetail> findByQuantityGreaterThan(int quantity);

    /**
     * Tìm các chi tiết đơn thuốc có đơn giá trong một khoảng nhất định.
     * 
     * @param minPrice Giá tối thiểu (bao gồm).
     * @param maxPrice Giá tối đa (bao gồm).
     * @return Danh sách các PrescriptionDetail phù hợp.
     */
    List<PrescriptionDetail> findByUnitPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Tìm tất cả chi tiết của một đơn thuốc và sắp xếp theo tên thuốc.
     * Cần JOIN FETCH để tải thông tin Medicine cùng lúc (tránh N+1 nếu cần truy cập
     * tên thuốc).
     * 
     * @param prescriptionId ID của đơn thuốc.
     * @return Danh sách PrescriptionDetail đã sắp xếp.
     */
    /*
     * @Query("SELECT pd FROM PrescriptionDetail pd JOIN FETCH pd.medicine m WHERE pd.prescription.prescriptionId = :prescriptionId ORDER BY m.medicineName ASC"
     * )
     * List<PrescriptionDetail>
     * findByPrescriptionIdOrderByMedicineName(@Param("prescriptionId") UUID
     * prescriptionId);
     */

    /**
     * Tính tổng số lượng của một loại thuốc cụ thể đã được kê trong tất cả các đơn.
     * 
     * @param medicineId ID của thuốc.
     * @return Tổng số lượng (Integer) hoặc null nếu chưa từng được kê.
     */
    /*
     * @Query("SELECT SUM(pd.quantity) FROM PrescriptionDetail pd WHERE pd.medicine.medicineId = :medicineId"
     * )
     * Integer getTotalQuantityPrescribedForMedicine(@Param("medicineId") UUID
     * medicineId);
     */

    /**
     * Đếm số lượng chi tiết đơn thuốc có chứa một loại thuốc theo ID của thuốc.
     * 
     * @param medicineId ID của thuốc cần đếm
     * @return Số lượng chi tiết đơn thuốc chứa thuốc này
     */
    long countByMedicine_MedicineId(UUID medicineId);
}