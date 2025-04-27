package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Department; // Import lớp Entity Department
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // Dùng cho phương thức tìm kiếm có thể không trả về kết quả
import java.util.UUID; // Kiểu dữ liệu của khóa chính (departmentId)

/**
 * Spring Data JPA repository cho thực thể Department.
 * Cung cấp các phương thức CRUD cơ bản và khả năng định nghĩa truy vấn tùy
 * chỉnh.
 */
@Repository // Đánh dấu đây là một Spring Bean Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    // Kế thừa JpaRepository<TênEntity, KiểuID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn bởi JpaRepository ---
    // save(Department entity), saveAll(Iterable<Department> entities)
    // findById(UUID id), existsById(UUID id)
    // findAll(), findAllById(Iterable<UUID> ids)
    // count()
    // deleteById(UUID id), delete(Department entity), deleteAll(...)
    // flush()
    // ... và nhiều hơn nữa

    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

    /**
     * Tìm kiếm một Department dựa trên tên chính xác của nó.
     * Do department_name là UNIQUE, phương thức này trả về Optional.
     *
     * @param departmentName Tên khoa cần tìm.
     * @return Optional chứa Department nếu tìm thấy, hoặc Optional rỗng nếu không.
     */
    Optional<Department> findByDepartmentName(String departmentName);

    /**
     * Tìm kiếm một Department dựa trên tên (không phân biệt chữ hoa chữ thường).
     *
     * @param departmentName Tên khoa cần tìm (không phân biệt hoa thường).
     * @return Optional chứa Department nếu tìm thấy, hoặc Optional rỗng.
     */
    Optional<Department> findByDepartmentNameIgnoreCase(String departmentName);

    // Bạn có thể thêm các phương thức truy vấn khác ở đây nếu cần.
    // Ví dụ:
    // boolean existsByDepartmentName(String departmentName); // Kiểm tra sự tồn tại
    // theo tên

    // --- Hoặc sử dụng @Query cho các truy vấn phức tạp hơn ---
    /*
     * @Query("SELECT d FROM Department d WHERE LOWER(d.departmentName) LIKE LOWER(concat('%', :keyword, '%'))"
     * )
     * List<Department> searchByNameContaining(@Param("keyword") String keyword);
     */

}