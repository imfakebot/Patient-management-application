package com.pma.model.entity; // <-- THAY ĐỔI PACKAGE NẾU CẦN

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy; // Cần cho equals/hashCode chuẩn

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
// Import BillItem nếu cần quản lý quan hệ ngược từ PrescriptionDetail -> BillItem
// import java.util.Set;
// import java.util.HashSet;
// import lombok.AccessLevel;

/**
 * Entity đại diện cho bảng PrescriptionDetails trong cơ sở dữ liệu.
 * Lưu trữ chi tiết về một loại thuốc cụ thể trong một đơn thuốc.
 */
@Getter
@Setter
// Luôn exclude các quan hệ LAZY khỏi toString
@ToString(exclude = { "prescription", "medicine" /* , "billItems" */ })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "PrescriptionDetails", indexes = {
        // Index từ schema SQL
        @Index(name = "IX_PrescriptionDetails_prescription_id", columnList = "prescription_id"),
        @Index(name = "IX_PrescriptionDetails_medicine_id", columnList = "medicine_id")
})
public class PrescriptionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Phù hợp UNIQUEIDENTIFIER + DEFAULT
    @Column(name = "prescription_detail_id", nullable = false, updatable = false)
    private UUID prescriptionDetailId;

    /**
     * Đơn thuốc mà chi tiết này thuộc về. Bắt buộc.
     * DB có ON DELETE CASCADE, nên xóa Prescription sẽ xóa chi tiết này.
     * CascadeType.ALL ở phía Prescription đảm bảo tính nhất quán.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc phải có Prescription
    @JoinColumn(name = "prescription_id", nullable = false) // Liên kết cột FK
    private Prescription prescription;

    /**
     * Loại thuốc được kê trong chi tiết này. Bắt buộc.
     * DB có ON DELETE NO ACTION/RESTRICT, không nên cascade REMOVE từ đây.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc phải có Medicine
    @JoinColumn(name = "medicine_id", nullable = false) // Liên kết cột FK
    private Medicine medicine;

    /**
     * Số lượng thuốc được kê. Bắt buộc và phải lớn hơn 0.
     */
    @Column(name = "quantity", nullable = false)
    private int quantity; // CHECK constraint ở DB

    /**
     * Đơn giá của thuốc TẠI THỜI ĐIỂM kê đơn. Bắt buộc.
     * Giá này được lấy từ bảng Medicines nhưng lưu lại ở đây để đảm bảo tính lịch
     * sử.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) // DECIMAL(10, 2)
    private BigDecimal unitPrice;

    /**
     * Liều lượng sử dụng (ví dụ: "500mg", "1 viên"). Bắt buộc.
     */
    @Column(name = "dosage", nullable = false, length = 255)
    private String dosage;

    /**
     * Hướng dẫn sử dụng chi tiết cho loại thuốc này (tùy chọn). Sử dụng @Lob cho
     * TEXT.
     */
    @Lob
    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    /**
     * Thời điểm bản ghi chi tiết đơn thuốc được tạo. Quản lý tự động.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi chi tiết đơn thuốc được cập nhật lần cuối. Quản lý tự động.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ OneToMany (Phía không sở hữu - Tùy chọn) ---
    // Ít phổ biến, thường truy cập từ BillItem -> PrescriptionDetail
    /*
     * @OneToMany(mappedBy = "prescriptionDetail",
     * cascade = { CascadeType.PERSIST, CascadeType.MERGE }, // Không nên REMOVE
     * fetch = FetchType.LAZY)
     * 
     * @Setter(AccessLevel.PACKAGE)
     * private Set<BillItem> billItems = new HashSet<>();
     */

    // --- Helper methods quản lý quan hệ hai chiều ---

    // --- Cho Prescription (QUAN TRỌNG vì Cascade ALL ở phía Prescription) ---
    /**
     * Thiết lập Prescription và đồng bộ hai chiều.
     * Nên là cách chính để liên kết PrescriptionDetail với Prescription.
     * 
     * @param prescription Đơn thuốc liên quan.
     */
    public void setPrescription(Prescription prescription) {
        // Tránh gọi lại không cần thiết
        if (Objects.equals(this.prescription, prescription)) {
            return;
        }
        // Ngắt kết nối khỏi prescription cũ (nếu có)
        Prescription oldPrescription = this.prescription;
        this.prescription = null; // Ngắt trước
        if (oldPrescription != null) {
            oldPrescription.removePrescriptionDetailInternal(this); // Gọi lại internal ở Prescription
        }
        // Kết nối với prescription mới (nếu không null)
        this.prescription = prescription;
        if (prescription != null) {
            prescription.addPrescriptionDetailInternal(this); // Gọi lại internal ở Prescription
        }
    }

    /**
     * Phương thức nội bộ được gọi bởi Prescription.add/removePrescriptionDetail.
     * Chỉ cập nhật tham chiếu phía này, không gọi lại Prescription.
     * 
     * @param prescription Đơn thuốc hoặc null.
     */
    void setPrescriptionInternal(Prescription prescription) {
        this.prescription = prescription;
    }

    // --- Cho Medicine ---
    /**
     * Thiết lập Medicine và đồng bộ hai chiều.
     * 
     * @param medicine Loại thuốc liên quan.
     */
    public void setMedicine(Medicine medicine) {
        // Tránh gọi lại không cần thiết
        if (Objects.equals(this.medicine, medicine)) {
            return;
        }
        // Ngắt kết nối khỏi medicine cũ (nếu có)
        Medicine oldMedicine = this.medicine;
        this.medicine = null; // Ngắt trước
        if (oldMedicine != null) {
            oldMedicine.removePrescriptionDetailInternal(this); // Gọi lại internal ở Medicine
        }
        // Kết nối với medicine mới (nếu không null)
        this.medicine = medicine;
        if (medicine != null) {
            medicine.addPrescriptionDetailInternal(this); // Gọi lại internal ở Medicine
        }
    }

    /**
     * Phương thức nội bộ được gọi bởi Medicine.add/removePrescriptionDetail.
     * Chỉ cập nhật tham chiếu phía này, không gọi lại Medicine.
     * 
     * @param medicine Loại thuốc hoặc null.
     */
    void setMedicineInternal(Medicine medicine) {
        this.medicine = medicine;
    }

    // --- Helper methods cho OneToMany (BillItems - nếu kích hoạt) ---
    /*
     * public void addBillItem(BillItem billItem) { ... }
     * public void removeBillItem(BillItem billItem) { ... }
     * void addBillItemInternal(BillItem billItem) { ... }
     * void removeBillItemInternal(BillItem billItem) { ... }
     */

    // --- equals() và hashCode() chuẩn cho JPA/Hibernate ---
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
        PrescriptionDetail that = (PrescriptionDetail) o;
        // So sánh dựa trên ID nếu đã có (khác null)
        return getPrescriptionDetailId() != null
                && Objects.equals(getPrescriptionDetailId(), that.getPrescriptionDetailId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu các lớp Prescription và Medicine phải có các phương thức
    // internal helper tương ứng (add/removePrescriptionDetailInternal) để hoàn
    // thiện
    // việc đồng bộ hai chiều.
}