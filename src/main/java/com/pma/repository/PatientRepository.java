package com.pma.repository; // Đảm bảo đúng package

import java.time.LocalDate; // Import Entity Patient
import java.util.List; // Import Enum Gender nếu cần tìm theo giới tính
import java.util.Optional; // Import cho phân trang
import java.util.UUID; // Import cho phân trang

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Import nếu dùng @Query
import org.springframework.data.jpa.repository.JpaRepository; // Import nếu dùng @Query với tham số
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Import nếu tìm theo ngày sinh
import org.springframework.stereotype.Repository;

import com.pma.model.entity.Patient;
import com.pma.model.enums.Gender; // Kiểu dữ liệu của khóa chính (patientId)

/**
 * Spring Data JPA repository cho thực thể Patient.
 * Cung cấp các phương thức CRUD cơ bản và khả năng định nghĩa truy vấn tùy
 * chỉnh,
 * bao gồm cả phân trang và sắp xếp.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    // Kế thừa JpaRepository<Patient, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.

    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

    /**
     * Tìm bệnh nhân theo số điện thoại (UNIQUE).
     * 
     * @param phone Số điện thoại cần tìm.
     * @return Optional chứa Patient nếu tìm thấy.
     */
    Optional<Patient> findByPhone(String phone);

    /**
     * Tìm bệnh nhân theo địa chỉ email (UNIQUE, nhưng có thể null).
     * 
     * @param email Địa chỉ email cần tìm.
     * @return Optional chứa Patient nếu tìm thấy.
     */
    Optional<Patient> findByEmail(String email);

    /**
     * Tìm danh sách bệnh nhân có họ tên chứa một chuỗi ký tự (không phân biệt hoa
     * thường).
     * 
     * @param nameFragment Đoạn tên cần tìm kiếm.
     * @return Danh sách các bệnh nhân phù hợp.
     */
    List<Patient> findByFullNameContainingIgnoreCase(String nameFragment);

    /**
     * Tìm danh sách bệnh nhân theo giới tính.
     * 
     * @param gender Giới tính cần lọc (sử dụng kiểu Enum).
     * @return Danh sách các bệnh nhân có giới tính tương ứng.
     */
    List<Patient> findByGender(Gender gender);

    /**
     * Tìm danh sách bệnh nhân sinh sau một ngày nhất định.
     * 
     * @param date Ngày sinh tối thiểu (không bao gồm).
     * @return Danh sách các bệnh nhân phù hợp.
     */
    List<Patient> findByDateOfBirthAfter(LocalDate date);

    /**
     * Tìm danh sách bệnh nhân sinh trong một khoảng ngày.
     * 
     * @param startDate Ngày bắt đầu (bao gồm).
     * @param endDate   Ngày kết thúc (bao gồm).
     * @return Danh sách các bệnh nhân phù hợp.
     */
    List<Patient> findByDateOfBirthBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Tìm danh sách bệnh nhân theo thành phố (không phân biệt hoa thường).
     * 
     * @param city Tên thành phố.
     * @return Danh sách các bệnh nhân phù hợp.
     */
    List<Patient> findByCityIgnoreCase(String city);

    // --- Ví dụ sử dụng Phân trang và Sắp xếp ---
    /**
     * Tìm tất cả bệnh nhân và trả về kết quả dưới dạng trang (Page), sắp xếp theo
     * tên đầy đủ tăng dần.
     * Pageable chứa thông tin về số trang, kích thước trang, và sắp xếp.
     * 
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách bệnh nhân và thông tin phân trang.
     */
    Page<Patient> findByOrderByFullNameAsc(Pageable pageable);

    /**
     * Tìm bệnh nhân theo thành phố (không phân biệt hoa thường) và trả về kết quả
     * phân trang.
     * 
     * @param city     Tên thành phố.
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách bệnh nhân phù hợp.
     */
    Page<Patient> findByCityIgnoreCase(String city, Pageable pageable);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Đếm số lượng bệnh nhân theo nhóm máu (ví dụ).
     * 
     * @param bloodType Nhóm máu cần đếm.
     * @return Số lượng bệnh nhân có nhóm máu đó.
     */
    @Query("SELECT count(p) FROM Patient p WHERE p.bloodType = :bloodType")
    long countByBloodType(@Param("bloodType") String bloodType);

    /**
     * Tìm kiếm bệnh nhân dựa trên từ khóa trong tên, điện thoại hoặc email (ví dụ).
     * 
     * @param keyword Từ khóa tìm kiếm.
     * @return Danh sách bệnh nhân phù hợp.
     */
    @Query("SELECT p FROM Patient p WHERE LOWER(p.fullName) LIKE LOWER(concat('%', :keyword, '%')) OR p.phone LIKE concat('%', :keyword, '%') OR LOWER(p.email) LIKE LOWER(concat('%', :keyword, '%'))")
    List<Patient> searchByKeyword(@Param("keyword") String keyword);

}