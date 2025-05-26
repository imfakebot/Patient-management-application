package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Bill; // Import Entity Bill
import com.pma.model.entity.Patient; // Import Patient để tìm theo bệnh nhân
import com.pma.model.enums.BillPaymentStatus; // Import Enum BillPaymentStatus
import org.springframework.data.domain.Page; // Import cho phân trang
import org.springframework.data.domain.Pageable; // Import cho phân trang
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.data.repository.query.Param; // Import nếu dùng @Query với tham số
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // Import nếu tìm theo ngày
import java.time.LocalDateTime; // Import nếu tìm theo ngày giờ
import java.util.List;
import java.util.UUID; // Kiểu dữ liệu của khóa chính (billId)

/**
 * Spring Data JPA repository cho thực thể Bill.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {
    // Kế thừa JpaRepository<Bill, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.
    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---
    /**
     * Tìm danh sách các hóa đơn của một bệnh nhân cụ thể.
     *
     * @param patient Đối tượng Patient.
     * @return Danh sách các Bill của bệnh nhân đó.
     */
    List<Bill> findByPatient(Patient patient);

    /**
     * Tìm danh sách các hóa đơn của một bệnh nhân theo ID của bệnh nhân.
     *
     * @param patientId ID của Patient.
     * @return Danh sách các Bill của bệnh nhân đó.
     */
    List<Bill> findByPatient_PatientId(UUID patientId);

    /**
     * Tìm danh sách các hóa đơn theo trạng thái thanh toán.
     *
     * @param paymentStatus Trạng thái cần lọc (sử dụng kiểu Enum).
     * @return Danh sách các Bill có trạng thái tương ứng.
     */
    List<Bill> findByPaymentStatus(BillPaymentStatus paymentStatus);

    /**
     * Tìm danh sách các hóa đơn được tạo trong một khoảng thời gian.
     *
     * @param startDateTime Thời điểm bắt đầu (bao gồm).
     * @param endDateTime Thời điểm kết thúc (bao gồm).
     * @return Danh sách các Bill phù hợp.
     */
    List<Bill> findByBillDatetimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * Tìm danh sách các hóa đơn có hạn thanh toán trước một ngày nhất định.
     *
     * @param date Ngày hạn chót (không bao gồm).
     * @return Danh sách các Bill phù hợp.
     */
    List<Bill> findByDueDateBefore(LocalDate date);

    /**
     * Tìm danh sách các hóa đơn của một bệnh nhân có trạng thái cụ thể.
     *
     * @param patientId ID của Patient.
     * @param paymentStatus Trạng thái thanh toán.
     * @return Danh sách các Bill phù hợp.
     */
    List<Bill> findByPatient_PatientIdAndPaymentStatus(UUID patientId, BillPaymentStatus paymentStatus);

    // --- Ví dụ sử dụng Phân trang và Sắp xếp ---
    /**
     * Tìm các hóa đơn của một bệnh nhân, sắp xếp theo ngày hóa đơn giảm dần, có
     * phân trang.
     *
     * @param patientId ID của Patient.
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách Bill.
     */
    Page<Bill> findByPatient_PatientIdOrderByBillDatetimeDesc(UUID patientId, Pageable pageable);

    /**
     * Tìm các hóa đơn theo trạng thái thanh toán, có phân trang.
     *
     * @param paymentStatus Trạng thái thanh toán.
     * @param pageable Đối tượng phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách Bill.
     */
    Page<Bill> findByPaymentStatus(BillPaymentStatus paymentStatus, Pageable pageable);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Tìm các hóa đơn chưa thanh toán (Pending) và đã quá hạn.
     *
     * @param now Thời điểm hiện tại để so sánh với due_date.
     * @return Danh sách các Bill quá hạn và chưa thanh toán.
     */
    /*
     * @Query("SELECT b FROM Bill b WHERE b.paymentStatus = :status AND b.dueDate IS NOT NULL AND b.dueDate < :now"
     * )
     * List<Bill> findOverduePendingBills(
     * 
     * @Param("status") BillPaymentStatus status, // Truyền
     * BillPaymentStatus.Pending
     * 
     * @Param("now") LocalDate now);
     */
}
