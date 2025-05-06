package com.pma.model.entity; // <-- THAY ĐỔI PACKAGE NẾU CẦN

import java.time.LocalDate;
import java.time.LocalDateTime; // Cần cho Setter(AccessLevel)
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp; // Cần cho equals/hashCode chuẩn
import org.hibernate.proxy.HibernateProxy;

import com.pma.model.enums.Gender;

import jakarta.persistence.CascadeType; // Cần cho getter view chỉ đọc
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
// Cân nhắc tạo Enum BloodType nếu muốn chặt chẽ hơn

/**
 * Entity đại diện cho bảng Patients trong cơ sở dữ liệu. Lưu trữ thông tin chi
 * tiết về bệnh nhân. Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn,
 * FetchType.LAZY, cascade đúng đắn và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// Luôn exclude các collection và quan hệ LAZY khỏi toString
@ToString(exclude = {"appointments", "medicalRecords", "prescriptions", "bills", "userAccount"})
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "Patients", uniqueConstraints = {
    // Các ràng buộc UNIQUE từ schema
    @UniqueConstraint(name = "UQ_Patients_Phone", columnNames = {"phone"}),
    @UniqueConstraint(name = "UQ_Patients_Email", columnNames = {"email"}) // Email có thể null nên UNIQUE
// constraint có thể khác biệt tùy DB
}, indexes = {
    // Các index từ schema SQL
    @Index(name = "IX_Patients_full_name", columnList = "full_name")
// Index cho phone, email đã được bao gồm trong UNIQUE constraints
})
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth; // DATE -> LocalDate

    @Enumerated(EnumType.STRING) // Lưu tên Enum
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender; // Sử dụng enum Gender đã import

    @Column(name = "phone", nullable = false, length = 15) // unique đã xử lý ở @Table
    private String phone;

    @Column(name = "email", length = 255) // nullable = true, unique đã xử lý ở @Table
    private String email;

    // --- Address Fields ---
    // Cân nhắc @Embeddable Address nếu cần tái sử dụng
    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 50)
    private String country;

    // --- Medical Info ---
    @Column(name = "blood_type", length = 3) // CHECK constraint ở DB
    private String bloodType; // Có thể dùng Enum BloodType nếu muốn

    @Lob // Cho kiểu TEXT
    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Lob // Cho kiểu TEXT
    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    // --- Other Info ---
    @Column(name = "insurance_number", length = 50)
    private String insuranceNumber;

    @Column(name = "emergency_contact_name", length = 255)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 15)
    private String emergencyContactPhone;

    // --- Timestamps ---
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ OneToMany (Phía không sở hữu) ---
    /**
     * Danh sách các cuộc hẹn của bệnh nhân này. CascadeType.ALL: Xóa Patient sẽ
     * xóa tất cả Appointments. THẬN TRỌNG! orphanRemoval=true: Xóa Appointment
     * khỏi Set này cũng xóa nó khỏi DB. Fetch LAZY.
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, // Xem xét kỹ lưỡng việc dùng ALL
            fetch = FetchType.LAZY, orphanRemoval = true)
    @Setter(AccessLevel.PACKAGE)
    private Set<Appointment> appointments = new HashSet<>();

    /**
     * Danh sách các bản ghi y tế của bệnh nhân này. CascadeType.ALL và
     * orphanRemoval=true thường hợp lý.
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Setter(AccessLevel.PACKAGE)
    private Set<MedicalRecord> medicalRecords = new HashSet<>();

    /**
     * Danh sách các đơn thuốc của bệnh nhân này. CascadeType.ALL và
     * orphanRemoval=true thường hợp lý.
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Setter(AccessLevel.PACKAGE)
    private Set<Prescription> prescriptions = new HashSet<>();

    /**
     * Danh sách các hóa đơn của bệnh nhân này. CascadeType.ALL và
     * orphanRemoval=true thường hợp lý.
     */
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Setter(AccessLevel.PACKAGE)
    private Set<Bill> bills = new HashSet<>();

    // --- Mối quan hệ OneToOne ---
    /**
     * Tài khoản người dùng liên kết (nếu có). Cascade ALL và orphanRemoval=true
     * thường hợp lý. Fetch LAZY.
     */
    @OneToOne(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, optional = true) // Patient
    // có
    // thể
    // không
    // có
    // UserAccount
    private UserAccount userAccount;

    // --- Helper methods quản lý quan hệ hai chiều ---
    // --- Cho Appointments ---
    public void addAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "Appointment cannot be null");
        if (this.appointments == null) {
            this.appointments = new HashSet<>();
        }
        if (this.appointments.add(appointment)) {
            if (!this.equals(appointment.getPatient())) {
                appointment.setPatient(this); // Gọi helper/setter ở Appointment
            }
        }
    }

    public void removeAppointment(Appointment appointment) {
        Objects.requireNonNull(appointment, "Appointment cannot be null");
        if (this.appointments != null) {
            if (this.appointments.remove(appointment)) {
                if (this.equals(appointment.getPatient())) {
                    appointment.setPatient(null); // Gọi helper/setter ở Appointment
                }
            }
        }
    }

    // Internal methods cho Appointment gọi lại
    void addAppointmentInternal(Appointment appointment) {
        if (this.appointments == null) {
            this.appointments = new HashSet<>();
        }
        this.appointments.add(appointment);
    }

    void removeAppointmentInternal(Appointment appointment) {
        if (this.appointments != null) {
            this.appointments.remove(appointment);
        }
    }

    // --- Cho MedicalRecords (Tương tự Appointments) ---
    public void addMedicalRecord(MedicalRecord medicalRecord) {
        Objects.requireNonNull(medicalRecord, "MedicalRecord cannot be null");
        if (this.medicalRecords == null) {
            this.medicalRecords = new HashSet<>();
        }
        if (this.medicalRecords.add(medicalRecord)) {
            if (!this.equals(medicalRecord.getPatient())) {
                medicalRecord.setPatient(this);
            }
        }
    }

    public void removeMedicalRecord(MedicalRecord medicalRecord) {
        Objects.requireNonNull(medicalRecord, "MedicalRecord cannot be null");
        if (this.medicalRecords != null) {
            if (this.medicalRecords.remove(medicalRecord)) {
                if (this.equals(medicalRecord.getPatient())) {
                    medicalRecord.setPatient(null);
                }
            }
        }
    }

    void addMedicalRecordInternal(MedicalRecord medicalRecord) {
        if (this.medicalRecords == null) {
            this.medicalRecords = new HashSet<>();
        }
        this.medicalRecords.add(medicalRecord);
    }

    void removeMedicalRecordInternal(MedicalRecord medicalRecord) {
        if (this.medicalRecords != null) {
            this.medicalRecords.remove(medicalRecord);
        }
    }

    // --- Cho Prescriptions (Tương tự Appointments) ---
    public void addPrescription(Prescription prescription) {
        Objects.requireNonNull(prescription, "Prescription cannot be null");
        if (this.prescriptions == null) {
            this.prescriptions = new HashSet<>();
        }
        if (this.prescriptions.add(prescription)) {
            if (!this.equals(prescription.getPatient())) {
                prescription.setPatient(this);
            }
        }
    }

    public void removePrescription(Prescription prescription) {
        Objects.requireNonNull(prescription, "Prescription cannot be null");
        if (this.prescriptions != null) {
            if (this.prescriptions.remove(prescription)) {
                if (this.equals(prescription.getPatient())) {
                    prescription.setPatient(null);
                }
            }
        }
    }

    void addPrescriptionInternal(Prescription prescription) {
        if (this.prescriptions == null) {
            this.prescriptions = new HashSet<>();
        }
        this.prescriptions.add(prescription);
    }

    void removePrescriptionInternal(Prescription prescription) {
        if (this.prescriptions != null) {
            this.prescriptions.remove(prescription);
        }
    }

    // --- Cho Bills (Tương tự Appointments) ---
    public void addBill(Bill bill) {
        Objects.requireNonNull(bill, "Bill cannot be null");
        if (this.bills == null) {
            this.bills = new HashSet<>();
        }
        if (this.bills.add(bill)) {
            if (!this.equals(bill.getPatient())) {
                bill.setPatient(this);
            }
        }
    }

    public void removeBill(Bill bill) {
        Objects.requireNonNull(bill, "Bill cannot be null");
        if (this.bills != null) {
            if (this.bills.remove(bill)) {
                if (this.equals(bill.getPatient())) {
                    bill.setPatient(null);
                }
            }
        }
    }

    void addBillInternal(Bill bill) {
        if (this.bills == null) {
            this.bills = new HashSet<>();
        }
        this.bills.add(bill);
    }

    void removeBillInternal(Bill bill) {
        if (this.bills != null) {
            this.bills.remove(bill);
        }
    }

    // --- Cho UserAccount (OneToOne) ---
    public void setUserAccount(UserAccount userAccount) {
        if (Objects.equals(this.userAccount, userAccount)) {
            return;
        }
        // Ngắt kết nối cũ và thiết lập mới ở cả hai phía
        UserAccount oldUserAccount = this.userAccount;
        this.userAccount = null; // Ngắt phía này trước
        if (oldUserAccount != null) {
            oldUserAccount.setPatientInternal(null); // Gọi internal ở UserAccount cũ
        }
        this.userAccount = userAccount; // Gán phía này
        if (userAccount != null) {
            userAccount.setPatientInternal(this); // Gọi internal ở UserAccount mới

        }
    }

    // Internal method cho UserAccount gọi lại
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

    public Set<Bill> getBillsView() {
        return Collections.unmodifiableSet(this.bills != null ? this.bills : Collections.emptySet());
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
        Patient patient = (Patient) o;
        // So sánh dựa trên ID nếu đã có (khác null)
        return getPatientId() != null && Objects.equals(getPatientId(), patient.getPatientId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu các lớp liên quan (Appointment, MedicalRecord, Prescription, Bill,
    // UserAccount)
    // phải có các phương thức setter hoặc internal helper tương ứng để hoàn thiện
    // việc đồng bộ hai chiều. Việc sử dụng CascadeType.ALL yêu cầu quản lý hai
    // chiều cẩn thận.
}
