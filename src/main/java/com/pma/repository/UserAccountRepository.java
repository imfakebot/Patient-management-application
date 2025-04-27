package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Doctor; // Import Doctor để tìm theo liên kết
import com.pma.model.entity.Patient; // Import Patient để tìm theo liên kết
import com.pma.model.entity.UserAccount; // Import Entity UserAccount
import com.pma.model.enums.UserRole; // Import Enum UserRole
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.data.repository.query.Param; // Import nếu dùng @Query với tham số
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID; // Kiểu dữ liệu của khóa chính (userId)

/**
 * Spring Data JPA repository cho thực thể UserAccount.
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    // Kế thừa JpaRepository<UserAccount, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.

    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

    /**
     * Tìm tài khoản người dùng theo tên đăng nhập (UNIQUE).
     * Rất quan trọng cho việc xác thực (authentication).
     * 
     * @param username Tên đăng nhập cần tìm.
     * @return Optional chứa UserAccount nếu tìm thấy.
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * Tìm tài khoản người dùng liên kết với một Patient cụ thể.
     * Do có thể có ràng buộc UNIQUE ở DB (filtered index), nên trả về Optional.
     * 
     * @param patient Đối tượng Patient.
     * @return Optional chứa UserAccount nếu tìm thấy.
     */
    Optional<UserAccount> findByPatient(Patient patient);

    /**
     * Tìm tài khoản người dùng liên kết với một Patient theo ID.
     * 
     * @param patientId ID của Patient.
     * @return Optional chứa UserAccount nếu tìm thấy.
     */
    Optional<UserAccount> findByPatient_PatientId(UUID patientId);

    /**
     * Tìm tài khoản người dùng liên kết với một Doctor cụ thể.
     * 
     * @param doctor Đối tượng Doctor.
     * @return Optional chứa UserAccount nếu tìm thấy.
     */
    Optional<UserAccount> findByDoctor(Doctor doctor);

    /**
     * Tìm tài khoản người dùng liên kết với một Doctor theo ID.
     * 
     * @param doctorId ID của Doctor.
     * @return Optional chứa UserAccount nếu tìm thấy.
     */
    Optional<UserAccount> findByDoctor_DoctorId(UUID doctorId);

    /**
     * Tìm danh sách các tài khoản người dùng theo vai trò.
     * 
     * @param role Vai trò cần lọc (sử dụng kiểu Enum).
     * @return Danh sách các UserAccount có vai trò tương ứng.
     */
    List<UserAccount> findByRole(UserRole role);

    /**
     * Tìm danh sách các tài khoản đang hoạt động.
     * 
     * @param active Trạng thái hoạt động (true hoặc false).
     * @return Danh sách các UserAccount phù hợp.
     */
    List<UserAccount> findByActive(boolean active);

    /**
     * Tìm tài khoản theo token xác thực email (nếu dùng tính năng này).
     * 
     * @param token Token cần tìm.
     * @return Optional chứa UserAccount.
     */
    Optional<UserAccount> findByEmailVerificationToken(String token);

    /**
     * Tìm tài khoản theo token đặt lại mật khẩu (nếu dùng tính năng này).
     * 
     * @param token Token cần tìm.
     * @return Optional chứa UserAccount.
     */
    Optional<UserAccount> findByPasswordResetToken(String token);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Kiểm tra xem một username đã tồn tại hay chưa (cách khác ngoài
     * findByUsername().isPresent()).
     * 
     * @param username Tên đăng nhập cần kiểm tra.
     * @return true nếu tồn tại, false nếu không.
     */
    /*
     * @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserAccount u WHERE u.username = :username"
     * )
     * boolean existsByUsername(@Param("username") String username);
     */

}