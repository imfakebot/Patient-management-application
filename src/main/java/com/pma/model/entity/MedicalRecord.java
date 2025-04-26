package com.pma.model.entity; // <-- THAY ĐỔI PACKAGE NẾU CẦN

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy; // Cần cho equals/hashCode chuẩn

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entity đại diện cho bảng MedicalRecords trong cơ sở dữ liệu.
 * Lưu trữ các ghi chép y tế cho bệnh nhân.
 * Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn, FetchType.LAZY,
 * cascade đúng đắn và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// Luôn exclude các quan hệ LAZY và collections khỏi toString
@ToString(exclude = { "patient", "doctor", "appointment", "diagnoses", "prescriptions" })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "MedicalRecords", indexes = {
        // Index từ schema SQL
        @Index(name = "IX_MedicalRecords_patient_id", columnList = "patient_id"),
        @Index(name = "IX_MedicalRecords_doctor_id", columnList = "doctor_id"),
        @Index(name = "IX_MedicalRecords_appointment_id", columnList = "appointment_id"),
        @Index(name = "IX_MedicalRecords_record_date", columnList = "record_date")
})
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    /**
     * Bệnh nhân mà bản ghi này thuộc về. Bắt buộc. FetchType.LAZY.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc có Patient
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * Bác sĩ chịu trách nhiệm cho bản ghi này. Bắt buộc. FetchType.LAZY.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc có Doctor
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /**
     * Cuộc hẹn liên quan đến bản ghi này (tùy chọn). FetchType.LAZY.
     * DB có ON DELETE SET NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true) // Appointment có thể null
    @JoinColumn(name = "appointment_id", nullable = true)
    private Appointment appointment;

    /**
     * Ngày ghi nhận bản ghi y tế.
     */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /**
     * Ghi chú lâm sàng chi tiết. Sử dụng @Lob cho TEXT.
     */
    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Thời điểm bản ghi được tạo. Quản lý tự động.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi được cập nhật lần cuối. Quản lý tự động.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ OneToMany (Phía không sở hữu) ---

    /**
     * Danh sách các chẩn đoán liên quan đến bản ghi y tế này.
     * CascadeType.ALL và orphanRemoval=true phù hợp (khớp với ON DELETE CASCADE
     * trong SQL).
     */
    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, // Xóa MedicalRecord sẽ xóa Diagnoses
            fetch = FetchType.LAZY, orphanRemoval = true) // Xóa Diagnosis khỏi Set sẽ xóa nó khỏi DB
    @Setter(AccessLevel.PACKAGE)
    private Set<Diagnosis> diagnoses = new HashSet<>();

    /**
     * Danh sách các đơn thuốc có thể liên quan đến bản ghi y tế này.
     * Cascade chỉ PERSIST/MERGE (do DB có ON DELETE SET NULL).
     */
    @OneToMany(mappedBy = "medicalRecord", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, // Chỉ cascade
                                                                                                 // lưu/update
            fetch = FetchType.LAZY, orphanRemoval = false) // Không xóa Prescription khi gỡ khỏi Set
    @Setter(AccessLevel.PACKAGE)
    private Set<Prescription> prescriptions = new HashSet<>();

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
            oldPatient.removeMedicalRecordInternal(this); // Gọi lại internal ở Patient
        this.patient = patient;
        if (patient != null)
            patient.addMedicalRecordInternal(this); // Gọi lại internal ở Patient
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
            oldDoctor.removeMedicalRecordInternal(this); // Gọi lại internal ở Doctor
        this.doctor = doctor;
        if (doctor != null)
            doctor.addMedicalRecordInternal(this); // Gọi lại internal ở Doctor
    }

    // --- Cho Appointment (Optional) ---
    /**
     * Thiết lập Appointment. Quản lý hai chiều ít quan trọng hơn ở đây.
     * 
     * @param appointment Cuộc hẹn liên quan, hoặc null.
     */
    public void setAppointment(Appointment appointment) {
        // Setter đơn giản thường là đủ cho quan hệ tùy chọn với ON DELETE SET NULL
        // Trừ khi bạn cần truy cập MedicalRecords từ Appointment và muốn đồng bộ chặt
        // chẽ.
        this.appointment = appointment;
    }

    // --- Cho Diagnoses (QUAN TRỌNG vì Cascade ALL & orphanRemoval) ---
    /**
     * Thêm Diagnosis và đồng bộ hai chiều.
     * 
     * @param diagnosis Chẩn đoán cần thêm (không null).
     */
    public void addDiagnosis(Diagnosis diagnosis) {
        Objects.requireNonNull(diagnosis, "Diagnosis cannot be null");
        if (this.diagnoses == null)
            this.diagnoses = new HashSet<>();
        if (this.diagnoses.add(diagnosis)) {
            // Đồng bộ phía đối diện (Diagnosis)
            if (!this.equals(diagnosis.getMedicalRecord())) {
                diagnosis.setMedicalRecordInternal(this); // Gọi setter internal ở Diagnosis
            }
        }
    }

    /**
     * Xóa Diagnosis và đồng bộ hai chiều.
     * 
     * @param diagnosis Chẩn đoán cần xóa (không null).
     */
    public void removeDiagnosis(Diagnosis diagnosis) {
        Objects.requireNonNull(diagnosis, "Diagnosis cannot be null");
        if (this.diagnoses != null) {
            if (this.diagnoses.remove(diagnosis)) {
                // Đồng bộ phía đối diện (Diagnosis)
                if (this.equals(diagnosis.getMedicalRecord())) {
                    diagnosis.setMedicalRecordInternal(null); // Gọi setter internal ở Diagnosis
                }
            }
        }
    }

    // Internal methods cho Diagnosis gọi lại
    void addDiagnosisInternal(Diagnosis diagnosis) {
        if (this.diagnoses == null)
            this.diagnoses = new HashSet<>();
        this.diagnoses.add(diagnosis);
    }

    void removeDiagnosisInternal(Diagnosis diagnosis) {
        if (this.diagnoses != null)
            this.diagnoses.remove(diagnosis);
    }

    // --- Cho Prescriptions (Cascade PERSIST/MERGE) ---
    /**
     * Thêm Prescription và đồng bộ hai chiều.
     * 
     * @param prescription Đơn thuốc cần thêm (không null).
     */
    public void addPrescription(Prescription prescription) {
        Objects.requireNonNull(prescription, "Prescription cannot be null");
        if (this.prescriptions == null)
            this.prescriptions = new HashSet<>();
        if (this.prescriptions.add(prescription)) {
            if (!this.equals(prescription.getMedicalRecord())) {
                prescription.setMedicalRecord(this); // Gọi helper/setter ở Prescription
            }
        }
    }

    /**
     * Xóa Prescription và đồng bộ hai chiều.
     * 
     * @param prescription Đơn thuốc cần xóa (không null).
     */
    public void removePrescription(Prescription prescription) {
        Objects.requireNonNull(prescription, "Prescription cannot be null");
        if (this.prescriptions != null) {
            if (this.prescriptions.remove(prescription)) {
                if (this.equals(prescription.getMedicalRecord())) {
                    prescription.setMedicalRecord(null); // Gọi helper/setter ở Prescription
                }
            }
        }
    }

    // Internal methods cho Prescription gọi lại
    void addPrescriptionInternal(Prescription prescription) {
        if (this.prescriptions == null)
            this.prescriptions = new HashSet<>();
        this.prescriptions.add(prescription);
    }

    void removePrescriptionInternal(Prescription prescription) {
        if (this.prescriptions != null)
            this.prescriptions.remove(prescription);
    }

    // --- Getters chỉ đọc cho Collections (Tùy chọn) ---
    public Set<Diagnosis> getDiagnosesView() {
        return Collections.unmodifiableSet(this.diagnoses != null ? this.diagnoses : Collections.emptySet());
    }

    public Set<Prescription> getPrescriptionsView() {
        return Collections.unmodifiableSet(this.prescriptions != null ? this.prescriptions : Collections.emptySet());
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
        MedicalRecord that = (MedicalRecord) o;
        // So sánh dựa trên ID nếu đã có (khác null)
        return getRecordId() != null && Objects.equals(getRecordId(), that.getRecordId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu các lớp Patient, Doctor, Appointment (nếu cần), Diagnosis,
    // Prescription
    // phải có các phương thức setter hoặc internal helper tương ứng để hoàn thiện
    // việc đồng bộ hai chiều.
}