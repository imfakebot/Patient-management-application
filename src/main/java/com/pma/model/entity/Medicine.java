package com.pma.model.entity; // <-- THAY ĐỔI PACKAGE NẾU CẦN

import jakarta.persistence.*;
import lombok.AccessLevel; // Cần cho Setter(AccessLevel)
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy; // Cần cho equals/hashCode chuẩn

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections; // Có thể dùng cho getter chỉ đọc (tùy chọn)
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// ---> IMPORT ENUM TỪ PACKAGE RIÊNG <---
import com.pma.model.enums.MedicineStatus; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN

/**
 * Entity đại diện cho bảng Medicines trong cơ sở dữ liệu.
 * Lưu trữ thông tin về các loại thuốc.
 * Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn, FetchType.LAZY,
 * cascade an toàn và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// Luôn exclude collection LAZY khỏi toString
@ToString(exclude = { "prescriptionDetails" })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "Medicines", uniqueConstraints = {
        // Ràng buộc UNIQUE cho medicine_name
        @UniqueConstraint(name = "UQ_Medicines_MedicineName", columnNames = { "medicine_name" })
}, indexes = {
        // Index cho medicine_name (đã có trong unique constraint nhưng thêm cho rõ)
        @Index(name = "IX_Medicines_medicine_name", columnList = "medicine_name")
})
public class Medicine {

    /**
     * Khóa chính của thuốc, kiểu UUID, tự sinh bởi cơ sở dữ liệu.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Phù hợp UNIQUEIDENTIFIER + DEFAULT
    @Column(name = "medicine_id", nullable = false, updatable = false)
    private UUID medicineId;

    /**
     * Tên của thuốc, yêu cầu là duy nhất và không được null.
     */
    @Column(name = "medicine_name", nullable = false, length = 255) // unique xử lý ở @Table
    private String medicineName;

    /**
     * Nhà sản xuất thuốc (tùy chọn).
     */
    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    /**
     * Đơn vị tính của thuốc (ví dụ: viên, hộp, ml). Bắt buộc.
     * Cân nhắc dùng Enum nếu danh sách đơn vị cố định và cần chuẩn hóa.
     */
    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    /**
     * Mô tả chi tiết về thuốc (tùy chọn). Sử dụng @Lob cho TEXT.
     */
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Giá bán tiêu chuẩn hiện tại của một đơn vị thuốc. Bắt buộc.
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2) // DECIMAL(10, 2)
    private BigDecimal price = BigDecimal.ZERO; // Khởi tạo mặc định

    /**
     * Số lượng tồn kho hiện tại. Bắt buộc.
     */
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0; // Khởi tạo mặc định

    /**
     * Trạng thái của thuốc (ví dụ: Còn hàng, Hết hàng).
     * Sử dụng Enum để chuẩn hóa.
     */
    @Enumerated(EnumType.STRING) // Lưu tên Enum ("Available", "Unavailable", ...)
    @Column(name = "status", length = 20)
    private MedicineStatus status = MedicineStatus.AVAILABLE; // Gán giá trị mặc định

    /**
     * Thời điểm bản ghi thuốc được tạo. Quản lý tự động.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi thuốc được cập nhật lần cuối. Quản lý tự động.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ OneToMany (Phía không sở hữu) ---

    /**
     * Danh sách các chi tiết đơn thuốc liên quan đến loại thuốc này.
     * 'mappedBy = "medicine"' trỏ tới trường 'medicine' trong lớp
     * PrescriptionDetail.
     * Xóa Medicine không nên xóa PrescriptionDetail (DB có ON DELETE NO
     * ACTION/RESTRICT mặc định).
     * Chỉ cascade PERSIST/MERGE là an toàn. Fetch LAZY.
     */
    @OneToMany(mappedBy = "medicine", // Liên kết với trường 'medicine' trong PrescriptionDetail
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }, // Cascade an toàn
            fetch = FetchType.LAZY, // Bắt buộc LAZY
            orphanRemoval = false // Không tự xóa PrescriptionDetail mồ côi
    )
    @Setter(AccessLevel.PACKAGE) // Hạn chế set trực tiếp
    private Set<PrescriptionDetail> prescriptionDetails = new HashSet<>(); // Khởi tạo sẵn

    // --- Helper methods quản lý quan hệ hai chiều ---

    /**
     * Thêm một chi tiết đơn thuốc vào danh sách và đồng bộ hai chiều.
     * 
     * @param detail Chi tiết đơn thuốc cần thêm (không null).
     */
    public void addPrescriptionDetail(PrescriptionDetail detail) {
        Objects.requireNonNull(detail, "PrescriptionDetail cannot be null");
        if (this.prescriptionDetails == null) {
            this.prescriptionDetails = new HashSet<>();
        }
        // Chỉ thêm và gọi lại nếu chưa có
        if (this.prescriptionDetails.add(detail)) {
            // Đồng bộ phía đối diện
            if (!this.equals(detail.getMedicine())) {
                detail.setMedicine(this); // Gọi helper/setter ở PrescriptionDetail
            }
        }
    }

    /**
     * Xóa một chi tiết đơn thuốc khỏi danh sách và đồng bộ hai chiều.
     * 
     * @param detail Chi tiết đơn thuốc cần xóa (không null).
     */
    public void removePrescriptionDetail(PrescriptionDetail detail) {
        Objects.requireNonNull(detail, "PrescriptionDetail cannot be null");
        if (this.prescriptionDetails != null) {
            // Chỉ xóa và gọi lại nếu có trong danh sách
            if (this.prescriptionDetails.remove(detail)) {
                // Đồng bộ phía đối diện
                if (this.equals(detail.getMedicine())) {
                    detail.setMedicine(null); // Gọi helper/setter ở PrescriptionDetail
                }
            }
        }
    }

    // --- Phương thức nội bộ (package-private) để PrescriptionDetail gọi lại ---

    /**
     * Thêm PrescriptionDetail vào collection nội bộ. Chỉ nên được gọi bởi
     * PrescriptionDetail.setMedicine().
     * 
     * @param detail PrescriptionDetail cần thêm.
     */
    void addPrescriptionDetailInternal(PrescriptionDetail detail) {
        if (this.prescriptionDetails == null) {
            this.prescriptionDetails = new HashSet<>();
        }
        this.prescriptionDetails.add(detail); // Chỉ thêm, không gọi lại
    }

    /**
     * Xóa PrescriptionDetail khỏi collection nội bộ. Chỉ nên được gọi bởi
     * PrescriptionDetail.setMedicine(null).
     * 
     * @param detail PrescriptionDetail cần xóa.
     */
    void removePrescriptionDetailInternal(PrescriptionDetail detail) {
        if (this.prescriptionDetails != null) {
            this.prescriptionDetails.remove(detail); // Chỉ xóa, không gọi lại
        }
    }

    /**
     * Cung cấp một view chỉ đọc (unmodifiable) của danh sách chi tiết đơn thuốc.
     * 
     * @return Một Set chỉ đọc chứa các chi tiết đơn thuốc liên quan.
     */
    public Set<PrescriptionDetail> getPrescriptionDetailsView() {
        return Collections
                .unmodifiableSet(this.prescriptionDetails != null ? this.prescriptionDetails : Collections.emptySet());
    }

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
        Medicine medicine = (Medicine) o;
        // So sánh dựa trên ID nếu đã có (khác null)
        return getMedicineId() != null && Objects.equals(getMedicineId(), medicine.getMedicineId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu lớp PrescriptionDetail phải có các phương thức setter hoặc internal
    // helper tương ứng
    // (setMedicine, add/removePrescriptionDetailInternal) để hoàn thiện việc đồng
    // bộ hai chiều.
}