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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// ---> IMPORT ENUMS TỪ PACKAGE RIÊNG <---
import com.pma.model.enums.Gender; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN
import com.pma.model.enums.DoctorStatus; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN

/**
 * Entity đại diện cho bảng Doctors trong cơ sở dữ liệu.
 * Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn, FetchType.LAZY,
 * cascade an toàn và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// Luôn exclude các collection và quan hệ LAZY khỏi toString
@ToString(exclude = { "department", "appointments", "medicalRecords", "prescriptions", "userAccount" })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "Doctors", uniqueConstraints = {
        // Các ràng buộc UNIQUE từ schema
        @UniqueConstraint(name = "UQ_Doctors_Phone", columnNames = { "phone" }),
        @UniqueConstraint(name = "UQ_Doctors_Email", columnNames = { "email" }),
        @UniqueConstraint(name = "UQ_Doctors_MedicalLicense", columnNames = { "medical_license" })
}, indexes = {
        // Các index từ schema SQL
        @Index(name = "IX_Doctors_department_id", columnList = "department_id"),
        @Index(name = "IX_Doctors_full_name", columnList = "full_name")
})
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "doctor_id", nullable = false, updatable = false)
    private UUID doctorId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender; // Sử dụng enum Gender đã import

    @Column(name = "phone", nullable = false, length = 15) // unique đã xử lý ở @Table
    private String phone;

    @Column(name = "email", nullable = false, length = 255) // unique đã xử lý ở @Table
    private String email;

    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    @Column(name = "medical_license", nullable = false, length = 50) // unique đã xử lý ở @Table
    private String medicalLicense;

    @Column(name = "years_of_experience") // nullable = true là mặc định
    private Integer yearsOfExperience; // INT -> Integer

    @Column(name = "salary", precision = 12, scale = 2) // DECIMAL(12, 2), nullable=true
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private DoctorStatus status = DoctorStatus.ACTIVE; // Gán giá trị mặc định

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ ManyToOne ---

    /**
     * Khoa mà bác sĩ thuộc về. Bắt buộc. FetchType.LAZY.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc phải có Department
    @JoinColumn(name = "department_id", nullable = false) // Liên kết cột FK
    private Department department;

    // --- Mối quan hệ OneToMany (Phía không sở hữu) ---

    /**
     * Danh sách các cuộc hẹn do bác sĩ này thực hiện.
     * Cascade chỉ PERSIST/MERGE. Không REMOVE (do DB có ON DELETE SET NULL).
     * Fetch LAZY. orphanRemoval=false.
     */
    @OneToMany(mappedBy = "doctor", cascade = { CascadeType.PERSIST,
            CascadeType.MERGE }, fetch = FetchType.LAZY, orphanRemoval = false)
    @Setter(AccessLevel.PACKAGE)
    private Set<Appointment> appointments = new HashSet<>();

    /**
     * Danh sách các bản ghi y tế do bác sĩ này tạo.
     * Cascade chỉ PERSIST/MERGE. Không REMOVE. Fetch LAZY. orphanRemoval=false.
     */
    @OneToMany(mappedBy = "doctor", cascade = { CascadeType.PERSIST,
            CascadeType.MERGE }, fetch = FetchType.LAZY, orphanRemoval = false)
    @Setter(AccessLevel.PACKAGE)
    private Set<MedicalRecord> medicalRecords = new HashSet<>();

    /**
     * Danh sách các đơn thuốc do bác sĩ này kê.
     * Cascade chỉ PERSIST/MERGE. Không REMOVE. Fetch LAZY. orphanRemoval=false.
     */
    @OneToMany(mappedBy = "doctor", cascade = { CascadeType.PERSIST,
            CascadeType.MERGE }, fetch = FetchType.LAZY, orphanRemoval = false)
    @Setter(AccessLevel.PACKAGE)
    private Set<Prescription> prescriptions = new HashSet<>();

    // --- Mối quan hệ OneToOne ---

    /**
     * Tài khoản người dùng liên kết với bác sĩ này (nếu có).
     * Cascade ALL và orphanRemoval=true thường hợp lý ở đây. Fetch LAZY.
     */
    @OneToOne(mappedBy = "doctor", cascade = CascadeType.ALL, // Xóa Doctor sẽ xóa UserAccount
            fetch = FetchType.LAZY, orphanRemoval = true, // Xóa UserAccount khỏi Doctor sẽ xóa UserAccount khỏi DB
            optional = true) // Doctor có thể không có UserAccount
    private UserAccount userAccount;

    // --- Helper methods quản lý quan hệ hai chiều ---

    // --- Cho Department ---
    /**
     * Thiết lập Department cho bác sĩ này và đồng bộ hai chiều.
     * 
     * @param department Khoa mới, hoặc null để gỡ bỏ.
     */
    public void setDepartment(Department department) {
        // Tránh gọi lại không cần thiết
        if (Objects.equals(this.department, department)) {
            return;
        }
        // Ngắt kết nối khỏi department cũ (nếu có)
        Department oldDepartment = this.department;
        this.department = null; // Ngắt trước
        if (oldDepartment != null) {
            oldDepartment.removeDoctorInternal(this); // Gọi lại internal ở Department
        }
        // Kết nối với department mới (nếu không null)
        this.department = department;
        if (department != null) {
            department.addDoctorInternal(this); // Gọi lại internal ở Department
        }
    }

    // --- Cho Appointments ---
    public void addAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "Appointment cannot be null");
        if (this.appointments == null)
            this.appointments = new HashSet<>();
        if (this.appointments.add(appointment)) {
            if (!this.equals(appointment.getDoctor())) {
                appointment.setDoctor(this); // Gọi helper/setter ở Appointment
            }
        }
    }

    public void removeAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "Appointment cannot be null");
        if (this.appointments != null) {
            if (this.appointments.remove(appointment)) {
                if (this.equals(appointment.getDoctor())) {
                    appointment.setDoctor(null); // Gọi helper/setter ở Appointment
                }
            }
        }
    }

    // Internal methods for Appointment to call back
    void addAppointmentInternal(Appointment appointment) {
        if (this.appointments == null)
            this.appointments = new HashSet<>();
        this.appointments.add(appointment);
    }

    void removeAppointmentInternal(Appointment appointment) {
        if (this.appointments != null)
            this.appointments.remove(appointment);
    }

    // --- Cho MedicalRecords (Tương tự Appointments) ---
    public void addMedicalRecord(MedicalRecord medicalRecord) {
        Objects.requireNonNull(medicalRecord, "MedicalRecord cannot be null");
        if (this.medicalRecords == null)
            this.medicalRecords = new HashSet<>();
        if (this.medicalRecords.add(medicalRecord)) {
            if (!this.equals(medicalRecord.getDoctor())) {
                medicalRecord.setDoctor(this); // Gọi helper/setter ở MedicalRecord
            }
        }
    }

    public void removeMedicalRecord(MedicalRecord medicalRecord) {
        Objects.requireNonNull(medicalRecord, "MedicalRecord cannot be null");
        if (this.medicalRecords != null) {
            if (this.medicalRecords.remove(medicalRecord)) {
                if (this.equals(medicalRecord.getDoctor())) {
                    medicalRecord.setDoctor(null); // Gọi helper/setter ở MedicalRecord
                }
            }
        }
    }

    // Internal methods for MedicalRecord to call back
    void addMedicalRecordInternal(MedicalRecord medicalRecord) {
        if (this.medicalRecords == null)
            this.medicalRecords = new HashSet<>();
        this.medicalRecords.add(medicalRecord);
    }

    void removeMedicalRecordInternal(MedicalRecord medicalRecord) {
        if (this.medicalRecords != null)
            this.medicalRecords.remove(medicalRecord);
    }

    // --- Cho Prescriptions (Tương tự Appointments) ---
    public void addPrescription(Prescription prescription) {
        Objects.requireNonNull(prescription, "Prescription cannot be null");
        if (this.prescriptions == null)
            this.prescriptions = new HashSet<>();
        if (this.prescriptions.add(prescription)) {
            if (!this.equals(prescription.getDoctor())) {
                prescription.setDoctor(this); // Gọi helper/setter ở Prescription
            }
        }
    }

    public void removePrescription(Prescription prescription) {
        Objects.requireNonNull(prescription, "Prescription cannot be null");
        if (this.prescriptions != null) {
            if (this.prescriptions.remove(prescription)) {
                if (this.equals(prescription.getDoctor())) {
                    prescription.setDoctor(null); // Gọi helper/setter ở Prescription
                }
            }
        }
    }

    // Internal methods for Prescription to call back
    void addPrescriptionInternal(Prescription prescription) {
        if (this.prescriptions == null)
            this.prescriptions = new HashSet<>();
        this.prescriptions.add(prescription);
    }

    void removePrescriptionInternal(Prescription prescription) {
        if (this.prescriptions != null)
            this.prescriptions.remove(prescription);
    }

    // --- Cho UserAccount (OneToOne) ---
    /**
     * Thiết lập UserAccount và đồng bộ hai chiều.
     * 
     * @param userAccount Tài khoản mới, hoặc null để gỡ bỏ.
     */
    public void setUserAccount(UserAccount userAccount) {
        // Tránh gán lại không cần thiết
        if (Objects.equals(this.userAccount, userAccount)) {
            return;
        }

        // Nếu đang gán userAccount mới (khác null)
        if (userAccount != null) {
            // Nếu Doctor này đã có userAccount khác, ngắt kết nối cũ trước
            if (this.userAccount != null) {
                this.userAccount.setDoctorInternal(null); // Ngắt phía UserAccount cũ
            }
            userAccount.setDoctorInternal(this); // Thiết lập phía UserAccount mới
        }
        // Nếu đang gỡ bỏ userAccount (gán null)
        else {
            // Nếu Doctor đang có userAccount, ngắt kết nối
            if (this.userAccount != null) {
                this.userAccount.setDoctorInternal(null); // Ngắt phía UserAccount cũ
            }
        }
        // Cập nhật tham chiếu ở phía Doctor
        this.userAccount = userAccount;
    }

    // Internal method for UserAccount to call back
    void setUserAccountInternal(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    // --- Getters chỉ đọc cho Collections (Tùy chọn) ---
    public Set<Appointment> getAppointmentsView() {
        return Collections.unmodifiableSet(this.appointments != null ? this.appointments : Collections.emptySet());
    }

    public Set<MedicalRecord> getMedicalRecordsView() {
        return Collections.unmodifiableSet(this.medicalRecords != null ? this.medicalRecords : Collections.emptySet());
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
        Doctor doctor = (Doctor) o;
        return getDoctorId() != null && Objects.equals(getDoctorId(), doctor.getDoctorId());
    }

    @Override
    public final int hashCode() {
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu các lớp Department, Appointment, MedicalRecord, Prescription,
    // UserAccount
    // phải có các phương thức setter hoặc internal helper tương ứng để hoàn thiện
    // việc đồng bộ hai chiều.
}