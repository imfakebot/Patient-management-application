package com.pma.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal; // Import BigDecimal cho tính toán tổng tiền
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import com.pma.model.enums.PaymentMethod; // Import enum PaymentMethod
import com.pma.model.enums.BillPaymentStatus;

/**
 * Entity đại diện cho bảng Bills trong cơ sở dữ liệu. Lưu trữ thông tin hóa đơn
 * thanh toán cho bệnh nhân.
 */
@Getter
@Setter
// Exclude các quan hệ và collection để tránh lỗi LAZY / vòng lặp
@ToString(exclude = {"patient", "appointment", "billItems"})
@NoArgsConstructor // Constructor mặc định cho JPA
@Entity
@Table(name = "Bills", indexes = {
    // Index từ schema SQL
    @Index(name = "IX_Bills_patient_id", columnList = "patient_id"),
    @Index(name = "IX_Bills_appointment_id", columnList = "appointment_id"),
    @Index(name = "IX_Bills_payment_status", columnList = "payment_status") // Index cho cột status
})
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bill_id", nullable = false, updatable = false)
    private UUID billId;

    /**
     * Trạng thái thanh toán của hóa đơn.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 15)
    private BillPaymentStatus paymentStatus = BillPaymentStatus.Pending; // Gán giá trị mặc định

    /**
     * Ngày và giờ hóa đơn được tạo hoặc phát hành. DB có DEFAULT
     * CURRENT_TIMESTAMP, có thể gán mặc định ở Java.
     */
    @Column(name = "bill_datetime") // nullable=true là mặc định
    private LocalDateTime billDatetime = LocalDateTime.now();

    /**
     * Hạn chót thanh toán hóa đơn (tùy chọn).
     */
    @Column(name = "due_date") // DATE -> LocalDate
    private LocalDate dueDate;

    /**
     * Ngày và giờ hóa đơn được thanh toán (tùy chọn).
     */
    @Column(name = "payment_date") // DATETIME2(3) -> LocalDateTime
    private LocalDateTime paymentDate; // Vẫn giữ kiểu LocalDateTime

    @Enumerated(EnumType.STRING) // Lưu tên Enum vào DB
    /**
     * Phương thức thanh toán (ví dụ: Tiền mặt, Thẻ, Bảo hiểm).
     */
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod; // Thay đổi kiểu dữ liệu từ String sang PaymentMethod

    /**
     * Thời điểm bản ghi hóa đơn được tạo. Quản lý tự động bởi Hibernate.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi hóa đơn được cập nhật lần cuối. Quản lý tự động.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ ManyToOne (Phía sở hữu khóa ngoại) ---
    /**
     * Bệnh nhân liên quan đến hóa đơn này. Bắt buộc.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Cuộc hẹn chính liên quan đến hóa đơn này (tùy chọn, có thể null). DB có
     * ON DELETE SET NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "appointment_id", nullable = true)
    private Appointment appointment;

    // --- Mối quan hệ OneToMany (Phía không sở hữu - Inverse Side) ---
    /**
     * Danh sách các chi tiết mục trong hóa đơn này. Mối quan hệ master-detail,
     * xóa Bill nên xóa BillItems. CascadeType.ALL và orphanRemoval=true là phù
     * hợp. Khớp với ON DELETE CASCADE trong SQL cho BillItems.bill_id.
     */
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, // Xóa Bill sẽ xóa BillItems
            fetch = FetchType.LAZY, orphanRemoval = true) // Xóa BillItem khỏi Set sẽ xóa nó khỏi DB
    @Setter(AccessLevel.PACKAGE)
    private Set<BillItem> billItems = new HashSet<>();

    // --- Enum cho Payment Status (nếu chưa tạo file riêng) ---
    /*
     * public enum BillPaymentStatus {
     * Pending, Paid, Partially_Paid, Cancelled // Đổi Partially Paid thành tên hợp
     * lệ
     * }
     */
    // --- Helper methods quản lý quan hệ hai chiều ---
    // --- Cho Patient ---
    public void setPatient(Patient patient) {
        if (Objects.equals(this.patient, patient)) {
            return;
        }
        Patient oldPatient = this.patient;
        this.patient = null; // Ngắt trước
        if (oldPatient != null) {
            oldPatient.removeBillInternal(this); // Gọi lại internal

        }
        this.patient = patient;
        if (patient != null) {
            patient.addBillInternal(this); // Gọi lại internal

        }
    }

    // --- Cho Appointment (Optional) ---
    public void setAppointment(Appointment appointment) {
        // Tương tự Patient, nhưng Appointment là optional và có ON DELETE SET NULL
        // Có thể không cần quản lý hai chiều phức tạp ở đây nếu không cần truy cập
        // Bills từ Appointment
        if (Objects.equals(this.appointment, appointment)) {
            return;
        }
        // Có thể thêm logic gọi lại Appointment.removeBillInternal / addBillInternal
        // nếu cần
        this.appointment = appointment;
    }

    // --- Cho BillItems (QUAN TRỌNG vì Cascade ALL & orphanRemoval) ---
    public void addBillItem(BillItem billItem) {
        Objects.requireNonNull(billItem, "BillItem cannot be null");
        if (this.billItems == null) {
            this.billItems = new HashSet<>();
        }
        if (this.billItems.add(billItem)) {
            // Đồng bộ phía đối diện
            billItem.setBillInternal(this); // Gọi setter internal ở BillItem
        }
    }

    public void removeBillItem(BillItem billItem) {
        Objects.requireNonNull(billItem, "BillItem cannot be null");
        if (this.billItems != null) {
            if (this.billItems.remove(billItem)) {
                // Gỡ bỏ liên kết phía đối diện
                billItem.setBillInternal(null); // Gọi setter internal ở BillItem
            }
        }
    }

    /**
     * Internal method to remove a BillItem from the Bill's collection. This
     * ensures consistency in the bidirectional relationship.
     *
     * @param billItem The BillItem to remove.
     */
    void removeBillItemInternal(BillItem billItem) {
        if (this.billItems != null) {
            this.billItems.remove(billItem);
        }
    }

    /**
     * Internal method to add a BillItem to this Bill. This ensures
     * bidirectional consistency.
     *
     * @param billItem The BillItem to add.
     */
    void addBillItemInternal(BillItem billItem) {
        if (billItems == null) {
            billItems = new HashSet<>();
        }
        if (!billItems.contains(billItem)) {
            billItems.add(billItem);
            billItem.setBillInternal(this); // Maintain bidirectional relationship
        }
    }

    // --- Getter chỉ đọc cho Collection (Tùy chọn) ---
    public Set<BillItem> getBillItemsView() {
        return Collections.unmodifiableSet(this.billItems != null ? this.billItems : Collections.emptySet());
    }

    // --- Phương thức tính tổng tiền (Transient) ---
    /**
     * Tính tổng số tiền của hóa đơn dựa trên các mục chi tiết (BillItems).
     * Phương thức này không ánh xạ tới cột nào trong DB (@Transient).
     *
     * @return Tổng số tiền của hóa đơn, hoặc BigDecimal.ZERO nếu không có mục
     * nào.
     */
    @Transient // Đánh dấu là không ánh xạ tới DB
    public BigDecimal getTotalAmount() {
        if (this.billItems == null || this.billItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return this.billItems.stream()
                // Đảm bảo line_total không null trước khi cộng
                .map(BillItem::getLineTotal) // Giả sử BillItem có getter getLineTotal() trả về BigDecimal
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- equals() và hashCode() chuẩn ---
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        Bill bill = (Bill) o;
        return getBillId() != null && Objects.equals(getBillId(), bill.getBillId());
    }

    @Override
    public final int hashCode() {
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- JavaFX Property (nếu cần cho TableView binding) ---
    /**
     * Trả về một ObjectProperty cho billId, hữu ích cho JavaFX TableView
     * binding.
     *
     * @return ObjectProperty chứa UUID của billId.
     */
    public ObjectProperty<UUID> billIdProperty() {
        return new SimpleObjectProperty<>(this.billId);
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu các lớp Patient, Appointment (nếu cần), BillItem
    // phải có các phương thức setter hoặc internal helper tương ứng để hoàn thiện
    // việc đồng bộ hai chiều. Đặc biệt quan trọng với BillItem do CascadeType.ALL.
}
