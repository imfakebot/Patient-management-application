package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Department; // Import Department để tìm theo khoa
import com.pma.model.entity.Doctor; // Import Entity Doctor
import com.pma.model.enums.DoctorStatus; // Import Enum DoctorStatus
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // Kiểu dữ liệu của khóa chính (doctorId)

/**
 * Spring Data JPA repository cho thực thể Doctor.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    // Kế thừa JpaRepository<Doctor, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.
    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---
    /**
     * Tìm bác sĩ theo số điện thoại (UNIQUE).
     *
     * @param phone Số điện thoại cần tìm.
     * @return Optional chứa Doctor nếu tìm thấy.
     */
    Optional<Doctor> findByPhone(String phone);

    /**
     * Tìm bác sĩ theo địa chỉ email (UNIQUE).
     *
     * @param email Địa chỉ email cần tìm.
     * @return Optional chứa Doctor nếu tìm thấy.
     */
    Optional<Doctor> findByEmail(String email);

    /**
     * Tìm bác sĩ theo giấy phép hành nghề (UNIQUE).
     *
     * @param medicalLicense Mã giấy phép cần tìm.
     * @return Optional chứa Doctor nếu tìm thấy.
     */
    Optional<Doctor> findByMedicalLicense(String medicalLicense);

    /**
     * Tìm danh sách bác sĩ theo chuyên khoa (không phân biệt hoa thường).
     *
     * @param specialty Chuyên khoa cần tìm.
     * @return Danh sách các bác sĩ phù hợp.
     */
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);

    /**
     * Tìm danh sách các bác sĩ thuộc về một khoa cụ thể (truyền vào đối tượng
     * Department).
     *
     * @param department Khoa cần tìm bác sĩ.
     * @return Danh sách các bác sĩ thuộc khoa đó.
     */
    List<Doctor> findByDepartment(Department department);

    /**
     * Tìm danh sách các bác sĩ thuộc về một khoa cụ thể (truyền vào ID của
     * Department). Spring Data JPA hỗ trợ truy cập thuộc tính lồng nhau qua dấu
     * gạch dưới (_).
     *
     * @param departmentId ID của khoa cần tìm bác sĩ.
     * @return Danh sách các bác sĩ thuộc khoa đó.
     */
    List<Doctor> findByDepartment_DepartmentId(UUID departmentId);
    // Tương đương JPQL: SELECT d FROM Doctor d WHERE d.department.departmentId =
    // :departmentId

    /**
     * Tìm danh sách các bác sĩ theo trạng thái làm việc.
     *
     * @param status Trạng thái cần lọc (sử dụng kiểu Enum).
     * @return Danh sách các bác sĩ có trạng thái tương ứng.
     */
    List<Doctor> findByStatus(DoctorStatus status);

    /**
     * Tìm danh sách bác sĩ có số năm kinh nghiệm lớn hơn hoặc bằng một giá trị
     * nào đó.
     *
     * @param years Số năm kinh nghiệm tối thiểu.
     * @return Danh sách các bác sĩ phù hợp.
     */
    List<Doctor> findByYearsOfExperienceGreaterThanEqual(Integer years);

    // --- Ví dụ sử dụng @Query cho truy vấn phức tạp hơn ---
    /**
     * Tìm kiếm bác sĩ theo tên hoặc chuyên khoa chứa một từ khóa (không phân
     * biệt hoa thường).
     *
     * @param keyword Từ khóa tìm kiếm.
     * @return Danh sách các bác sĩ phù hợp.
     */
    /*
     * @Query("SELECT d FROM Doctor d WHERE LOWER(d.fullName) LIKE LOWER(concat('%', :keyword, '%')) OR LOWER(d.specialty) LIKE LOWER(concat('%', :keyword, '%'))"
     * )
     * List<Doctor> searchByNameOrSpecialty(@Param("keyword") String keyword);
     */
    /**
     * Counts the number of doctors associated with a specific department ID.
     *
     * @param departmentId the UUID of the department.
     * @return the count of doctors associated with the department.
     */
    long countByDepartment_DepartmentId(UUID departmentId);

    /**
     * Retrieves all doctors, eagerly fetching their associated Department. This
     * helps prevent LazyInitializationException when accessing department
     * details outside a session.
     *
     * @return A list of Doctor entities with their Department eagerly loaded.
     */
    @Query("SELECT d FROM Doctor d JOIN FETCH d.department")
    List<Doctor> findAllWithDepartments();
}
