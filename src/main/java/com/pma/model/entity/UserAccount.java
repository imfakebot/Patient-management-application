package com.pma.model.entity; // <-- THAY ĐỔI PACKAGE NẾU CẦN

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import com.pma.model.enums.UserRole;

import jakarta.persistence.Column; // Cần cho equals/hashCode chuẩn
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity đại diện cho bảng UserAccounts trong cơ sở dữ liệu. Quản lý thông tin
 * tài khoản người dùng hệ thống.
 */
@Getter
@Setter
// Exclude các quan hệ để tránh lỗi LAZY / vòng lặp
@ToString(exclude = {"patient", "doctor"})
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "UserAccounts", uniqueConstraints = {
    // Ràng buộc UNIQUE từ schema
    @UniqueConstraint(name = "UQ_UserAccounts_Username", columnNames = {"username"})
// Các Filtered Unique Index cho patient_id/doctor_id cần tạo trực tiếp trong DB
// Hoặc dùng @Table(uniqueConstraints = {@UniqueConstraint(columnNames =
// "patient_id", ...) })
// nhưng JPA chuẩn không hỗ trợ điều kiện WHERE IS NOT NULL.
// Có thể dùng @Column(unique=true) nhưng không hoàn toàn đúng logic filtered
// index.
}, indexes = {
    // Index từ schema SQL
    @Index(name = "IX_UserAccounts_role", columnList = "role"),
    // Filtered indexes cho patient_id/doctor_id tạo ở DB
    @Index(name = "IX_UserAccounts_patient_id_filtered", columnList = "patient_id"), // Chỉ mục thông thường
    @Index(name = "IX_UserAccounts_doctor_id_filtered", columnList = "doctor_id") // Chỉ mục thông thường
})
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Phù hợp UNIQUEIDENTIFIER + DEFAULT
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /**
     * Tên đăng nhập, yêu cầu là duy nhất.
     */
    @Column(name = "username", nullable = false, length = 255) // unique xử lý ở @Table
    private String username;

    /**
     * Lưu trữ BĂM (HASH) của mật khẩu, không bao giờ lưu mật khẩu gốc. Độ dài
     * cần đủ lớn cho các thuật toán băm hiện đại (bcrypt, scrypt).
     */
    @Column(name = "password_hash", nullable = false, length = 255) // Tăng độ dài nếu cần
    private String passwordHash; // Tên biến rõ ràng là hash

    /**
     * Vai trò của người dùng trong hệ thống. Sử dụng Enum.
     */
    @Enumerated(EnumType.STRING) // Lưu tên Enum
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role; // Sử dụng enum UserRole đã import

    // --- Mối quan hệ OneToOne (Phía sở hữu khóa ngoại - optional) ---
    // UserAccount có thể liên kết với Patient hoặc Doctor (hoặc không)
    /**
     * Bệnh nhân liên kết với tài khoản này (nếu có). Đây là phía sở hữu của mối
     * quan hệ OneToOne với Patient. DB có ON DELETE SET NULL. Cascade không nên
     * dùng ở phía này. FetchType.LAZY. Có thể thêm @JoinColumn(unique=true) nếu
     * muốn ràng buộc 1-1 ở mức JPA, nhưng filtered index của DB mạnh hơn.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    // cascade không nên đặt ở đây vì UserAccount là phía sở hữu FK tùy chọn
    // orphanRemoval cũng không hợp lý ở đây
    @JoinColumn(name = "patient_id", unique = false) // unique=false vì nhiều UserAccount có thể có patient_id=NULL
    // Ràng buộc 1 UserAccount / 1 Patient (khác null) xử lý ở DB
    private Patient patient;

    /**
     * Bác sĩ liên kết với tài khoản này (nếu có). Tương tự như liên kết với
     * Patient.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "doctor_id", unique = false) // unique=false vì nhiều UserAccount có thể có doctor_id=NULL
    private Doctor doctor;

    // --- Security & Audit Info ---
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    @Column(name = "is_active", nullable = false)
    private boolean active = true; // Giá trị mặc định trong Java

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0; // Giá trị mặc định

    /**
     * Cờ đánh dấu người dùng có cần nhập OTP do đăng nhập sai nhiều lần hay không.
     */
    @Column(name = "otp_required_for_login", nullable = false)
    private boolean otpRequiredForLogin = false; // Mặc định là false

    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false; // Giá trị mặc định

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "email_verification_expires")
    private LocalDateTime emailVerificationExpires;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false; // Giá trị mặc định

    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret; // Lưu secret key cho TOTP

    @Column(name = "email_otp_hash", length = 255) // Đổi tên cột và tăng độ dài cho hash
    private String emailOtpHash;

    @Column(name = "email_otp_expires_at")
    private LocalDateTime emailOtpExpiresAt;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_expires")
    private LocalDateTime passwordResetExpires;

    // --- Timestamps ---
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Enum cho Role (nếu chưa tạo file riêng) ---
    /*
     * public enum UserRole {
     * Patient, Doctor, Admin, Receptionist, Nurse, Lab_Staff // Đổi Lab Staff thành
     * tên hợp lệ
     * }
     */
    // --- Helper methods quản lý quan hệ hai chiều ---
    // --- Cho Patient (OneToOne) ---
    /**
     * Thiết lập Patient và đồng bộ hai chiều.
     *
     * @param patient Bệnh nhân liên quan, hoặc null.
     */
    public void setPatient(Patient patient) {
        // Tránh gọi lại không cần thiết
        if (Objects.equals(this.patient, patient)) {
            return;
        }
        // Ngắt kết nối khỏi patient cũ (nếu có)
        Patient oldPatient = this.patient;
        // Nếu đang gỡ bỏ liên kết (patient = null)
        if (patient == null) {
            this.patient = null; // Gỡ phía này
            if (oldPatient != null) {
                oldPatient.setUserAccountInternal(null); // Gọi internal ở Patient cũ để gỡ bỏ tham chiếu ngược
            }
        } // Nếu đang thiết lập liên kết mới
        else {
            // Nếu patient mới này đã có user account khác, hoặc user account này đã có
            // patient khác -> lỗi logic 1-1
            if (patient.getUserAccount() != null && !patient.getUserAccount().equals(this)) {
                throw new IllegalStateException(
                        "Patient " + patient.getPatientId() + " is already associated with another UserAccount.");
            }
            if (this.patient != null && !this.patient.equals(patient)) {
                // Đã ngắt ở trên hoặc nên throw lỗi nếu không cho đổi patient
                throw new IllegalStateException(
                        "UserAccount " + this.userId + " is already associated with another Patient.");
            }

            this.patient = patient; // Gán phía này
            patient.setUserAccountInternal(this); // Gọi internal ở Patient mới
        }
    }

    /**
     * Phương thức nội bộ được gọi bởi Patient.setUserAccount.
     *
     * @param patient Patient hoặc null.
     */
    void setPatientInternal(Patient patient) {
        this.patient = patient;
    }

    // --- Cho Doctor (OneToOne) ---
    /**
     * Thiết lập Doctor và đồng bộ hai chiều.
     *
     * @param doctor Bác sĩ liên quan, hoặc null.
     */
    public void setDoctor(Doctor doctor) {
        if (Objects.equals(this.doctor, doctor)) {
            return;
        }
        Doctor oldDoctor = this.doctor;
        if (doctor == null) {
            this.doctor = null;
            if (oldDoctor != null) {
                oldDoctor.setUserAccountInternal(null);
            }
        } else {
            if (doctor.getUserAccount() != null && !doctor.getUserAccount().equals(this)) {
                throw new IllegalStateException(
                        "Doctor " + doctor.getDoctorId() + " is already associated with another UserAccount.");
            }
            if (this.doctor != null && !this.doctor.equals(doctor)) {
                throw new IllegalStateException(
                        "UserAccount " + this.userId + " is already associated with another Doctor.");
            }
            this.doctor = doctor;
            doctor.setUserAccountInternal(this);
        }
    }

    /**
     * Phương thức nội bộ được gọi bởi Doctor.setUserAccount.
     *
     * @param doctor Doctor hoặc null.
     */
    void setDoctorInternal(Doctor doctor) {
        this.doctor = doctor;
    }

    /**
     * Lấy mã OTP email đã được mã hóa.
     */
    public String getEmailOtpHash() {
        return emailOtpHash;
    }

    /**
     * Cập nhật mã OTP email đã được mã hóa.
     */
    public void setEmailOtpHash(String emailOtpHash) {
        this.emailOtpHash = emailOtpHash;
    }

    /**
     * Lấy thời điểm hết hạn của mã OTP.
     */
    public LocalDateTime getEmailOtpExpiresAt() {
        return emailOtpExpiresAt;
    }

    /**
     * Cập nhật thời điểm hết hạn của mã OTP.
     */
    public void setEmailOtpExpiresAt(LocalDateTime emailOtpExpiresAt) {
        this.emailOtpExpiresAt = emailOtpExpiresAt;
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
        UserAccount that = (UserAccount) o;
        // So sánh dựa trên ID nếu đã có (khác null)
        return getUserId() != null && Objects.equals(getUserId(), that.getUserId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    public Object userIdProperty() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'userIdProperty'");
    }

    // --- Lưu ý về Helper Methods và Unique Constraints ---
    // Helper methods cho OneToOne cần kiểm tra ràng buộc 1-1 logic trong Java.
    // Ràng buộc 1 User / 1 Patient (khác null) cần được đảm bảo ở DB bằng Filtered
    // Unique Index.
    // Yêu cầu các lớp Patient và Doctor phải có phương thức internal
    // `setUserAccountInternal`.
}
