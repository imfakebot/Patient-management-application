package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Medicine; // Import Entity Medicine
import com.pma.model.enums.MedicineStatus; // Import Enum MedicineStatus
import org.springframework.data.domain.Page; // Import cho phân trang
import org.springframework.data.domain.Pageable; // Import cho phân trang
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Import nếu dùng câu lệnh UPDATE/DELETE
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.data.repository.query.Param; // Import nếu dùng @Query với tham số
import org.springframework.stereotype.Repository;

import java.math.BigDecimal; // Import nếu tìm theo giá
import java.util.List;
import java.util.Optional;
import java.util.UUID; // Kiểu dữ liệu của khóa chính (medicineId)

/**
 * Spring Data JPA repository cho thực thể Medicine.
 */
@Repository
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {
    // Kế thừa JpaRepository<Medicine, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.

    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

    /**
     * Tìm một loại thuốc dựa trên tên chính xác của nó (UNIQUE).
     * 
     * @param medicineName Tên thuốc cần tìm.
     * @return Optional chứa Medicine nếu tìm thấy.
     */
    Optional<Medicine> findByMedicineName(String medicineName);

    /**
     * Tìm một loại thuốc dựa trên tên (không phân biệt hoa thường).
     * 
     * @param medicineName Tên thuốc cần tìm.
     * @return Optional chứa Medicine nếu tìm thấy.
     */
    Optional<Medicine> findByMedicineNameIgnoreCase(String medicineName);

    /**
     * Tìm danh sách các loại thuốc có tên chứa một chuỗi ký tự (không phân biệt hoa
     * thường).
     * 
     * @param nameFragment Đoạn tên cần tìm kiếm.
     * @return Danh sách các Medicine phù hợp.
     */
    List<Medicine> findByMedicineNameContainingIgnoreCase(String nameFragment);

    /**
     * Tìm danh sách các loại thuốc theo nhà sản xuất (không phân biệt hoa thường).
     * 
     * @param manufacturer Tên nhà sản xuất.
     * @return Danh sách các Medicine phù hợp.
     */
    List<Medicine> findByManufacturerIgnoreCase(String manufacturer);

    /**
     * Tìm danh sách các loại thuốc theo trạng thái.
     * 
     * @param status Trạng thái cần lọc (sử dụng kiểu Enum).
     * @return Danh sách các Medicine có trạng thái tương ứng.
     */
    List<Medicine> findByStatus(MedicineStatus status);

    /**
     * Tìm danh sách các loại thuốc có giá trong một khoảng nhất định.
     * 
     * @param minPrice Giá tối thiểu (bao gồm).
     * @param maxPrice Giá tối đa (bao gồm).
     * @return Danh sách các Medicine phù hợp.
     */
    List<Medicine> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Tìm danh sách các loại thuốc có số lượng tồn kho nhỏ hơn hoặc bằng một
     * ngưỡng.
     * 
     * @param quantity Ngưỡng số lượng tồn kho.
     * @return Danh sách các Medicine sắp hết hàng.
     */
    List<Medicine> findByStockQuantityLessThanEqual(int quantity);

    // --- Ví dụ sử dụng Phân trang và Sắp xếp ---
    /**
     * Tìm tất cả các loại thuốc, sắp xếp theo tên thuốc tăng dần, có phân trang.
     * 
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách Medicine.
     */
    Page<Medicine> findByOrderByMedicineNameAsc(Pageable pageable);

    // --- Ví dụ sử dụng @Query và @Modifying ---
    /**
     * Cập nhật số lượng tồn kho cho một loại thuốc cụ thể.
     * 
     * @Modifying: Đánh dấu đây là một câu lệnh thay đổi dữ liệu (UPDATE/DELETE),
     *             không phải SELECT.
     *             Cần được thực thi trong một transaction (@Transactional ở tầng
     *             Service).
     * @param medicineId     ID của thuốc cần cập nhật.
     * @param quantityChange Số lượng thay đổi (có thể âm để giảm).
     * @return Số lượng bản ghi bị ảnh hưởng (thường là 1 nếu thành công).
     */
    /*
     * @Modifying // Bắt buộc cho UPDATE/DELETE
     * 
     * @Query("UPDATE Medicine m SET m.stockQuantity = m.stockQuantity + :quantityChange WHERE m.medicineId = :medicineId"
     * )
     * int updateStockQuantity(@Param("medicineId") UUID
     * medicineId, @Param("quantityChange") int quantityChange);
     */

    /**
     * Tìm kiếm thuốc theo từ khóa trong tên hoặc nhà sản xuất.
     * 
     * @param keyword Từ khóa tìm kiếm.
     * @return Danh sách các Medicine phù hợp.
     */
    /*
     * @Query("SELECT m FROM Medicine m WHERE LOWER(m.medicineName) LIKE LOWER(concat('%', :keyword, '%')) OR LOWER(m.manufacturer) LIKE LOWER(concat('%', :keyword, '%'))"
     * )
     * List<Medicine> searchByNameOrManufacturer(@Param("keyword") String keyword);
     */

}