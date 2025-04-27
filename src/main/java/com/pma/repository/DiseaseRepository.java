package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Disease; // Import Entity Disease
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.data.repository.query.Param; // Import nếu dùng @Query với tham số
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
// Kiểu dữ liệu của khóa chính (diseaseCode) là String

/**
 * Spring Data JPA repository cho thực thể Disease.
 */
@Repository
public interface DiseaseRepository extends JpaRepository<Disease, String> { // Lưu ý: Kiểu ID là String
    // Kế thừa JpaRepository<Disease, String>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById(String id), findAll, deleteById(String id), count,
    // existsById(String id), etc.
    // Lưu ý findById, deleteById,... nhận tham số kiểu String

    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

    /**
     * Tìm một loại bệnh dựa trên tên chính xác của nó (UNIQUE).
     * 
     * @param diseaseName Tên bệnh cần tìm.
     * @return Optional chứa Disease nếu tìm thấy.
     */
    Optional<Disease> findByDiseaseName(String diseaseName);

    /**
     * Tìm một loại bệnh dựa trên tên (không phân biệt hoa thường).
     * 
     * @param diseaseName Tên bệnh cần tìm.
     * @return Optional chứa Disease nếu tìm thấy.
     */
    Optional<Disease> findByDiseaseNameIgnoreCase(String diseaseName);

    /**
     * Tìm danh sách các loại bệnh có tên chứa một chuỗi ký tự (không phân biệt hoa
     * thường).
     * 
     * @param nameFragment Đoạn tên cần tìm kiếm.
     * @return Danh sách các Disease phù hợp.
     */
    List<Disease> findByDiseaseNameContainingIgnoreCase(String nameFragment);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Tìm kiếm bệnh theo từ khóa trong tên hoặc mô tả.
     * 
     * @param keyword Từ khóa tìm kiếm.
     * @return Danh sách các Disease phù hợp.
     */
    /*
     * @Query("SELECT d FROM Disease d WHERE LOWER(d.diseaseName) LIKE LOWER(concat('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(concat('%', :keyword, '%'))"
     * )
     * List<Disease> searchByNameOrDescription(@Param("keyword") String keyword);
     */

}