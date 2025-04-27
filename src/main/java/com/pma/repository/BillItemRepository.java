package com.pma.repository; // Đảm bảo đúng package

import com.pma.model.entity.Bill; // Import Bill để tìm theo hóa đơn
import com.pma.model.entity.BillItem; // Import Entity BillItem
import com.pma.model.entity.PrescriptionDetail; // Import PrescriptionDetail nếu cần tìm theo chi tiết đơn thuốc
import com.pma.model.enums.BillItemType; // Import Enum BillItemType
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import nếu dùng @Query
import org.springframework.data.repository.query.Param; // Import nếu dùng @Query với tham số
import org.springframework.stereotype.Repository;

import java.math.BigDecimal; // Import nếu cần tìm theo giá
import java.util.List;
import java.util.UUID; // Kiểu dữ liệu của khóa chính (billItemId)

/**
 * Spring Data JPA repository cho thực thể BillItem.
 */
@Repository
public interface BillItemRepository extends JpaRepository<BillItem, UUID> {
    // Kế thừa JpaRepository<BillItem, UUID>

    // --- Các phương thức CRUD cơ bản được cung cấp sẵn ---
    // save, findById, findAll, deleteById, count, existsById, etc.

    // --- Định nghĩa các phương thức truy vấn tùy chỉnh theo quy ước đặt tên ---

    /**
     * Tìm tất cả các mục chi tiết thuộc về một hóa đơn cụ thể.
     * 
     * @param bill Đối tượng Bill.
     * @return Danh sách các BillItem thuộc hóa đơn đó.
     */
    List<BillItem> findByBill(Bill bill);

    /**
     * Tìm tất cả các mục chi tiết thuộc về một hóa đơn theo ID của hóa đơn.
     * 
     * @param billId ID của Bill.
     * @return Danh sách các BillItem thuộc hóa đơn đó.
     */
    List<BillItem> findByBill_BillId(UUID billId);

    /**
     * Tìm các mục hóa đơn theo loại mục (ví dụ: chỉ tìm các mục là thuốc).
     * 
     * @param itemType Loại mục cần lọc (sử dụng kiểu Enum).
     * @return Danh sách các BillItem có loại tương ứng.
     */
    List<BillItem> findByItemType(BillItemType itemType);

    /**
     * Tìm các mục hóa đơn có đơn giá lớn hơn một giá trị nhất định.
     * 
     * @param price Giá trị tối thiểu.
     * @return Danh sách các BillItem phù hợp.
     */
    List<BillItem> findByUnitPriceGreaterThan(BigDecimal price);

    /**
     * Tìm các mục hóa đơn có số lượng bằng một giá trị cụ thể.
     * 
     * @param quantity Số lượng cần tìm.
     * @return Danh sách các BillItem phù hợp.
     */
    List<BillItem> findByQuantity(int quantity);

    /**
     * Tìm mục hóa đơn liên kết với một chi tiết đơn thuốc cụ thể (nếu có).
     * 
     * @param prescriptionDetail Đối tượng PrescriptionDetail.
     * @return Danh sách các BillItem liên kết (thường chỉ có 1 hoặc 0).
     */
    List<BillItem> findByPrescriptionDetail(PrescriptionDetail prescriptionDetail);

    /**
     * Tìm mục hóa đơn liên kết với một chi tiết đơn thuốc theo ID.
     * 
     * @param prescriptionDetailId ID của PrescriptionDetail.
     * @return Danh sách các BillItem liên kết.
     */
    List<BillItem> findByPrescriptionDetail_PrescriptionDetailId(UUID prescriptionDetailId);

    // --- Ví dụ sử dụng @Query ---
    /**
     * Tính tổng tiền của tất cả các mục trong một hóa đơn cụ thể.
     * Lưu ý: Nên dùng phương thức tính toán @Transient trong Entity Bill sẽ hiệu
     * quả hơn.
     * 
     * @param billId ID của hóa đơn.
     * @return Tổng tiền (BigDecimal) hoặc null nếu không có mục nào.
     */
    /*
     * @Query("SELECT SUM(bi.quantity * bi.unitPrice) FROM BillItem bi WHERE bi.bill.billId = :billId"
     * )
     * BigDecimal calculateTotalAmountForBill(@Param("billId") UUID billId);
     */

    /**
     * Tìm các mục hóa đơn thuộc một hóa đơn và có loại cụ thể.
     * 
     * @param billId   ID của hóa đơn.
     * @param itemType Loại mục cần tìm.
     * @return Danh sách BillItem phù hợp.
     */
    /*
     * @Query("SELECT bi FROM BillItem bi WHERE bi.bill.billId = :billId AND bi.itemType = :itemType"
     * )
     * List<BillItem> findByBillIdAndItemType(@Param("billId") UUID
     * billId, @Param("itemType") BillItemType itemType);
     */

}