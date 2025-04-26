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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections; // Cần cho getter view chỉ đọc
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// ---> IMPORT ENUM TỪ PACKAGE RIÊNG <---
import com.pma.model.enums.PrescriptionStatus; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN

/**
 * Entity đại diện cho bảng Prescriptions trong cơ sở dữ liệu.
 * Lưu trữ thông tin về một đơn thuốc.
 * Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn, FetchType.LAZY,
 * cascade đúng đắn và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// Luôn exclude các quan hệ LAZY và collections khỏi toString
@ToString(exclude = { "patient", "doctor", "medicalRecord", "prescriptionDetails" })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "Prescriptions", indexes = {
        // Index từ schema SQL
        @Index(name = "IX_Prescriptions_patient_id", columnList = "patient_id"),
        @Index(name = "IX_Prescriptions_doctor_id", columnList = "doctor_id"),
        @Index(name = "IX_Prescriptions_record_id", columnList = "record_id")
})
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Phù hợp UNIQUEIDENTIFIER + DEFAULT
    @Column(name = "prescription_id", nullable = false, updatable = false)
    private UUID prescriptionId;

    /**
     * Bệnh nhân được kê đơn thuốc này. Bắt buộc. FetchType.LAZY.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc có Patient
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Bác sĩ kê đơn thuốc này. Bắt buộc. FetchType.LAZY.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc có Doctor
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /**
     * Bản ghi y tế liên quan (tùy chọn). FetchType.LAZY.
     * DB có ON DELETE SET NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true) // MedicalRecord có thể null
    @JoinColumn(name = "record_id", nullable = true)
    private MedicalRecord medicalRecord;

    /**
     * Ngày kê đơn thuốc.
     */
    @Column(name = "prescription_date", nullable = false)
    private LocalDate prescriptionDate;

    /**
     * Ghi chú chung cho đơn thuốc. Sử dụng @Lob cho TEXT.
     */
    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Trạng thái của đơn thuốc. Sử dụng Enum để chuẩn hóa.
     */
    @Enumerated(EnumType.STRING) // Lưu tên Enum ("Active", "Completed", ...)
    @Column(name = "status", length = 15)
    private PrescriptionStatus status = PrescriptionStatus.Active; // Gán giá trị mặc định

    /**
     * Thời điểm bản ghi đơn thuốc được tạo. Quản lý tự động.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi đơn thuốc được cập nhật lần cuối. Quản lý tự động.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ OneToMany (Phía không sở hữu) ---

    /**
     * Danh sách các chi tiết thuốc trong đơn thuốc này.
     * Mối quan hệ master-detail, xóa Prescription nên xóa PrescriptionDetails.
     * CascadeType.ALL và orphanRemoval=true là phù hợp (khớp với ON DELETE CASCADE
     * trong SQL).
     * Fetch LAZY.
     */
    @OneToMany(mappedBy = "prescription", // Liên kết với trường 'prescription' trong PrescriptionDetail
            cascade = CascadeType.ALL, // Xóa Prescription sẽ xóa PrescriptionDetails
            fetch = FetchType.LAZY, // Bắt buộc LAZY
            orphanRemoval = true // Xóa PrescriptionDetail khỏi Set sẽ xóa nó khỏi DB
    )
    @Setter(AccessLevel.PACKAGE) // Hạn chế set trực tiếp
    private Set<PrescriptionDetail> prescriptionDetails = new HashSet<>(); // Khởi tạo sẵn

    // --- Helper methods quản lý quan hệ hai chiều ---

    // --- Cho Patient ---
    /**
     * Thiết lập Patient và đồng bộ hai chiều.
     * 
     * @param patient Bệnh nhân mới, hoặc null để gỡ bỏ.
     */
    public void setPatient(Patient patient) {
        if (Objects.equals(this.patient, patient))
            return;
        Patient oldPatient = this.patient;
        this.patient = null; // Ngắt trước
        if (oldPatient != null)
            oldPatient.removePrescriptionInternal(this); // Gọi lại internal ở Patient
        this.patient = patient;
        if (patient != null)
            patient.addPrescriptionInternal(this); // Gọi lại internal ở Patient
    }

    // --- Cho Doctor ---
    /**
     * Thiết lập Doctor và đồng bộ hai chiều.
     * 
     * @param doctor Bác sĩ mới, hoặc null để gỡ bỏ.
     */
    public void setDoctor(Doctor doctor) {
        if (Objects.equals(this.doctor, doctor))
            return;
        Doctor oldDoctor = this.doctor;
        this.doctor = null; // Ngắt trước
        if (oldDoctor != null)
            oldDoctor.removePrescriptionInternal(this); // Gọi lại internal ở Doctor
        this.doctor = doctor;
        if (doctor != null)
            doctor.addPrescriptionInternal(this); // Gọi lại internal ở Doctor
    }

    // --- Cho MedicalRecord (Optional) ---
    /**
     * Thiết lập MedicalRecord và đồng bộ hai chiều.
     * 
     * @param medicalRecord Bản ghi y tế mới, hoặc null để gỡ bỏ.
     */
    public void setMedicalRecord(MedicalRecord medicalRecord) {
        if (Objects.equals(this.medicalRecord, medicalRecord))
            return;
        MedicalRecord oldMedicalRecord = this.medicalRecord;
        this.medicalRecord = null; // Ngắt trước
        if (oldMedicalRecord != null)
            oldMedicalRecord.removePrescriptionInternal(this); // Gọi lại internal ở MedicalRecord
        this.medicalRecord = medicalRecord;
        if (medicalRecord != null)
            medicalRecord.addPrescriptionInternal(this); // Gọi lại internal ở MedicalRecord
    }

    // --- Cho PrescriptionDetails (QUAN TRỌNG vì Cascade ALL & orphanRemoval) ---
    /**
     * Thêm PrescriptionDetail và đồng bộ hai chiều.
     * 
     * @param detail Chi tiết đơn thuốc cần thêm (không null).
     */
    public void addPrescriptionDetail(PrescriptionDetail detail) {
        Objects.requireNonNull(detail, "PrescriptionDetail cannot be null");
        if (this.prescriptionDetails == null)
            this.prescriptionDetails = new HashSet<>();
        if (this.prescriptionDetails.add(detail)) {
            // Đồng bộ phía đối diện (PrescriptionDetail)
            if (!this.equals(detail.getPrescription())) {
                detail.setPrescriptionInternal(this); // Gọi setter internal ở PrescriptionDetail
            }
        }
    }

    /**
     * Xóa PrescriptionDetail và đồng bộ hai chiều.
     * 
     * @param detail Chi tiết đơn thuốc cần xóa (không null).
     */
    public void removePrescriptionDetail(PrescriptionDetail detail) {
        Objects.requireNonNull(detail, "PrescriptionDetail cannot be null");
        if (this.prescriptionDetails != null) {
            if (this.prescriptionDetails.remove(detail)) {
                // Đồng bộ phía đối diện (PrescriptionDetail)
                if (this.equals(detail.getPrescription())) {
                    detail.setPrescriptionInternal(null); // Gọi setter internal ở PrescriptionDetail
                }
            }
        }
    }

    // Internal methods cho PrescriptionDetail gọi lại
    void addPrescriptionDetailInternal(PrescriptionDetail detail) {
        if (this.prescriptionDetails == null) {
            this.prescriptionDetails = new HashSet<>();
        }
        this.prescriptionDetails.add(detail);
    }

    void removePrescriptionDetailInternal(PrescriptionDetail detail) {
        if (this.prescriptionDetails != null) {
            this.prescriptionDetails.remove(detail);
        }
    }

    // --- Getter chỉ đọc cho Collection (Tùy chọn) ---
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
        Prescription that = (Prescription) o;
        // So sánh dựa trên ID nếu đã có (khác null)
        return getPrescriptionId() != null && Objects.equals(getPrescriptionId(), that.getPrescriptionId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu các lớp Patient, Doctor, MedicalRecord, PrescriptionDetail
    // phải có các phương thức setter hoặc internal helper tương ứng để hoàn thiện
    // việc đồng bộ hai chiều. Đặc biệt quan trọng với PrescriptionDetail do
    // CascadeType.ALL.
}