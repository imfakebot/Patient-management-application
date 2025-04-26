package com.pma.model.entity; // <-- THAY ĐỔI PACKAGE NẾU CẦN

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula; // Cần cho cột tính toán
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// ---> IMPORT ENUM TỪ PACKAGE RIÊNG (NẾU CÓ) <---
import com.pma.model.enums.BillItemType; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN

/**
 * Entity đại diện cho bảng BillItems trong cơ sở dữ liệu.
 * Lưu trữ chi tiết từng mục trong một hóa đơn.
 */
@Getter
@Setter
// Exclude các quan hệ để tránh lỗi LAZY / vòng lặp
@ToString(exclude = { "bill", "prescriptionDetail" /* , "labTest", "procedure" */ })
@NoArgsConstructor // Constructor mặc định cho JPA
@Entity
@Table(name = "BillItems", indexes = {
        // Index từ schema SQL
        @Index(name = "IX_BillItems_bill_id", columnList = "bill_id"),
        @Index(name = "IX_BillItems_prescription_detail_id", columnList = "prescription_detail_id")
// Add indexes for lab_test_id, procedure_id if they exist
})
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bill_item_id", nullable = false, updatable = false)
    private UUID billItemId;

    /**
     * Mô tả của mục hóa đơn (ví dụ: Phí khám, Tên thuốc, Tên xét nghiệm).
     */
    @Column(name = "item_description", nullable = false, length = 255)
    private String itemDescription;

    /**
     * Loại mục hóa đơn. Sử dụng Enum để chuẩn hóa.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private BillItemType itemType; // Sử dụng enum BillItemType đã import

    /**
     * Số lượng của mục này. Mặc định là 1.
     */
    @Column(name = "quantity", nullable = false)
    private int quantity = 1; // Gán giá trị mặc định trong Java

    /**
     * Đơn giá của một đơn vị mục này.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) // DECIMAL(10, 2)
    private BigDecimal unitPrice = BigDecimal.ZERO; // Khởi tạo mặc định

    /**
     * Tổng tiền cho mục này (Số lượng * Đơn giá).
     * Đây là cột tính toán trong DB (AS ...).
     * Sử dụng @Formula của Hibernate để đọc giá trị này từ DB.
     * Không nên set giá trị này từ Java.
     * Hibernate sẽ không bao gồm cột này trong câu lệnh INSERT/UPDATE.
     */
    @Formula("CAST(quantity * unit_price AS DECIMAL(12, 2))") // Khớp với công thức trong SQL
    // Hoặc đơn giản hơn nếu DB hỗ trợ tốt: @Formula("quantity * unit_price")
    @Setter(AccessLevel.NONE) // Không cho phép set giá trị này từ Java
    private BigDecimal lineTotal; // Hibernate sẽ đọc giá trị được tính bởi DB

    /**
     * Thời điểm bản ghi mục hóa đơn được tạo. Quản lý tự động.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi mục hóa đơn được cập nhật lần cuối. Quản lý tự động.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ ManyToOne (Phía sở hữu khóa ngoại) ---

    /**
     * Hóa đơn mà mục này thuộc về. Bắt buộc.
     * Quan hệ này có ON DELETE CASCADE ở DB, nên khi Bill bị xóa, BillItem này cũng
     * bị xóa.
     * Hibernate sẽ tự động xử lý nếu Bill có CascadeType.ALL hoặc REMOVE cho
     * collection billItems.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc phải có Bill
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    /**
     * Liên kết tùy chọn tới chi tiết đơn thuốc (nếu mục này là thuốc).
     * DB có ON DELETE SET NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true) // PrescriptionDetail có thể null
    @JoinColumn(name = "prescription_detail_id", nullable = true)
    private PrescriptionDetail prescriptionDetail;

    /**
     * Liên kết tùy chọn tới xét nghiệm (nếu có bảng LabTests).
     */
    // @ManyToOne(fetch = FetchType.LAZY, optional = true)
    // @JoinColumn(name = "lab_test_id", nullable = true)
    // private LabTest labTest; // Giả sử có Entity LabTest

    /**
     * Liên kết tùy chọn tới thủ thuật (nếu có bảng Procedures).
     */
    // @ManyToOne(fetch = FetchType.LAZY, optional = true)
    // @JoinColumn(name = "procedure_id", nullable = true)
    // private Procedure procedure; // Giả sử có Entity Procedure

    // --- Enum cho ItemType (nếu chưa tạo file riêng) ---
    /*
     * public enum BillItemType {
     * Consultation, Medicine, Lab_Test, Procedure, Other // Đổi Lab Test thành tên
     * hợp lệ
     * }
     */

    // --- Helper methods quản lý quan hệ hai chiều ---

    // --- Cho Bill (QUAN TRỌNG vì Cascade ALL ở phía Bill) ---
    /**
     * Thiết lập Bill và đảm bảo tính nhất quán hai chiều.
     * Phương thức này nên là cách chính để liên kết BillItem với Bill.
     * 
     * @param bill Hóa đơn liên quan.
     */
    public void setBill(Bill bill) {
        // Tránh gọi lại không cần thiết
        if (Objects.equals(this.bill, bill)) {
            return;
        }
        // Ngắt kết nối khỏi bill cũ (nếu có)
        Bill oldBill = this.bill;
        this.bill = null; // Ngắt trước
        if (oldBill != null) {
            oldBill.removeBillItemInternal(this); // Gọi lại internal ở Bill
        }
        // Kết nối với bill mới (nếu không null)
        this.bill = bill;
        if (bill != null) {
            bill.addBillItemInternal(this); // Gọi lại internal ở Bill
        }
    }

    /**
     * Phương thức nội bộ được gọi bởi Bill.addBillItem/removeBillItem.
     * Chỉ cập nhật tham chiếu phía này, không gọi lại Bill.
     * 
     * @param bill Hóa đơn hoặc null.
     */
    void setBillInternal(Bill bill) {
        this.bill = bill;
    }

    // --- Cho PrescriptionDetail (Optional, ON DELETE SET NULL) ---
    // Quản lý hai chiều ít quan trọng hơn, setter đơn giản có thể đủ.
    public void setPrescriptionDetail(PrescriptionDetail prescriptionDetail) {
        // Có thể thêm logic gọi lại PrescriptionDetail.remove/addBillItemInternal nếu
        // cần
        this.prescriptionDetail = prescriptionDetail;
    }

    // --- equals() và hashCode() chuẩn ---
    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        BillItem billItem = (BillItem) o;
        return getBillItemId() != null && Objects.equals(getBillItemId(), billItem.getBillItemId());
    }

    @Override
    public final int hashCode() {
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu lớp Bill (và PrescriptionDetail nếu cần) phải có các phương thức
    // internal helper tương ứng (add/removeBillItemInternal) để hoàn thiện
    // việc đồng bộ hai chiều, đặc biệt quan trọng với Bill do CascadeType.ALL.
}