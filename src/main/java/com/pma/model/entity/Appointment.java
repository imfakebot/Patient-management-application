package com.pma.model.entity; // <-- THAY ĐỔI PACKAGE NẾU CẦN

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import com.pma.model.enums.AppointmentStatus;

import jakarta.persistence.Column; // Quan trọng cho equals/hashCode
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity đại diện cho bảng Appointments trong cơ sở dữ liệu.
 * Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn,
 * FetchType.LAZY và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// ToString luôn exclude các quan hệ LAZY để tránh lỗi và vấn đề hiệu năng
@ToString(exclude = { "patient", "doctor" })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "Appointments", indexes = { // Thêm index từ schema SQL
        @Index(name = "IX_Appointments_patient_id", columnList = "patient_id"),
        @Index(name = "IX_Appointments_doctor_id", columnList = "doctor_id"),
        @Index(name = "IX_Appointments_appointment_datetime", columnList = "appointment_datetime")
})
public class Appointment {

    /**
     * Khóa chính của cuộc hẹn, kiểu UUID, tự sinh bởi cơ sở dữ liệu.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Tối ưu cho UNIQUEIDENTIFIER DEFAULT NEWSEQUENTIALID
    @Column(name = "appointment_id", nullable = false, updatable = false)
    private UUID appointmentId;

    /**
     * Bệnh nhân liên quan đến cuộc hẹn này.
     * QUAN TRỌNG: Luôn dùng FetchType.LAZY cho @ManyToOne/@OneToOne
     * để tránh tải dữ liệu không cần thiết và gây lỗi N+1.
     * Mối quan hệ là bắt buộc (optional = false).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc có Patient
    @JoinColumn(name = "patient_id", nullable = false) // Liên kết cột FK, không null
    private Patient patient;

    /**
     * Bác sĩ thực hiện cuộc hẹn (có thể null).
     * QUAN TRỌNG: FetchType.LAZY.
     * Mối quan hệ là tùy chọn (optional = true).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true) // Doctor có thể null
    @JoinColumn(name = "doctor_id", nullable = true) // Liên kết cột FK, cho phép null
    private Doctor doctor;

    /**
     * Thời gian diễn ra cuộc hẹn. Kiểu DATETIME2(3) -> LocalDateTime.
     */
    @Column(name = "appointment_datetime", nullable = false)
    private LocalDateTime appointmentDatetime;

    /**
     * Lý do của cuộc hẹn (chi tiết).
     * Sử dụng @Lob cho kiểu TEXT/NTEXT để hỗ trợ chuỗi dài hiệu quả.
     */
    @Lob
    @Column(name = "reason", columnDefinition = "TEXT") // Hoặc NTEXT nếu DB dùng Unicode
    private String reason;

    /**
     * Loại cuộc hẹn (ví dụ: Khám lần đầu, Tái khám).
     * Kiểu VARCHAR(50). Cân nhắc dùng Enum AppointmentType nếu danh sách cố định.
     */
    @Column(name = "appointment_type", length = 50)
    private String appointmentType;

    /**
     * Trạng thái hiện tại của cuộc hẹn.
     * Sử dụng Enum để đảm bảo tính nhất quán.
     * Sử dụng EnumType.STRING là an toàn nhất khi enum thay đổi.
     * Gán giá trị mặc định trong Java để phản ánh DEFAULT của DB.
     */
    @Enumerated(EnumType.STRING) // Lưu tên Enum ("Scheduled", "Completed", ...)
    @Column(name = "status", length = 15)
    private AppointmentStatus status = AppointmentStatus.Scheduled; // Gán mặc định

    /**
     * Thời điểm bản ghi cuộc hẹn được tạo.
     * Được quản lý tự động bởi Hibernate (@CreationTimestamp).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi cuộc hẹn được cập nhật lần cuối.
     * Được quản lý tự động bởi Hibernate (@UpdateTimestamp).
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- KHÔNG nên định nghĩa OneToMany ở đây trừ khi thực sự cần ---
    // (Ví dụ: @OneToMany(mappedBy = "appointment") private Set<MedicalRecord>
    // medicalRecords;)
    // Giữ Entity tập trung vào các mối quan hệ chính (ManyToOne).

    // --- Helper methods để quản lý quan hệ hai chiều một cách an toàn và nhất quán
    // ---
    // Đảm bảo trạng thái của object graph trong bộ nhớ luôn đúng.

    /**
     * Thiết lập Bệnh nhân cho cuộc hẹn này, đồng thời quản lý mối quan hệ hai
     * chiều.
     * Phương thức này sẽ tự động thêm/xóa cuộc hẹn này khỏi danh sách của Patient
     * cũ/mới.
     * Yêu cầu lớp Patient phải có phương thức addAppointmentInternal và
     * removeAppointmentInternal.
     *
     * @param patient Bệnh nhân mới, hoặc null để gỡ bỏ liên kết.
     */
    public void setPatient(Patient patient) {
        // Tránh gán lại chính nó hoặc gọi không cần thiết
        if (Objects.equals(this.patient, patient)) {
            return;
        }

        // Ngắt kết nối khỏi patient cũ (nếu có)
        Patient oldPatient = this.patient;
        this.patient = null; // Ngắt kết nối ở phía này trước
        if (oldPatient != null) {
            oldPatient.removeAppointmentInternal(this); // Gọi helper method nội bộ ở Patient để xóa khỏi collection
        }

        // Kết nối với patient mới (nếu không null)
        this.patient = patient; // Gán tham chiếu ở phía này
        if (patient != null) {
            patient.addAppointmentInternal(this); // Gọi helper method nội bộ ở Patient để thêm vào collection
        }
    }

    /**
     * Thiết lập Bác sĩ cho cuộc hẹn này, đồng thời quản lý mối quan hệ hai chiều.
     * Phương thức này sẽ tự động thêm/xóa cuộc hẹn này khỏi danh sách của Doctor
     * cũ/mới.
     * Yêu cầu lớp Doctor phải có phương thức addAppointmentInternal và
     * removeAppointmentInternal.
     *
     * @param doctor Bác sĩ mới, hoặc null để gỡ bỏ liên kết.
     */
    public void setDoctor(Doctor doctor) {
        // Tránh gán lại chính nó hoặc gọi không cần thiết
        if (Objects.equals(this.doctor, doctor)) {
            return;
        }

        // Ngắt kết nối khỏi doctor cũ (nếu có)
        Doctor oldDoctor = this.doctor;
        this.doctor = null; // Ngắt kết nối ở phía này trước
        if (oldDoctor != null) {
            oldDoctor.removeAppointmentInternal(this); // Gọi helper method nội bộ ở Doctor
        }

        // Kết nối với doctor mới (nếu không null)
        this.doctor = doctor; // Gán tham chiếu ở phía này
        if (doctor != null) {
            doctor.addAppointmentInternal(this); // Gọi helper method nội bộ ở Doctor
        }
    }

    // --- equals() và hashCode() chuẩn cho JPA/Hibernate ---
    // Đảm bảo hoạt động đúng với LAZY loading và Hibernate proxies.
    // Chỉ dựa vào ID (nếu có) và class thực sự.

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        // Kiểm tra null và proxy an toàn
        if (o == null)
            return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        // So sánh class thực sự
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        // Ép kiểu an toàn sau khi kiểm tra class
        Appointment that = (Appointment) o;
        // Chỉ so sánh ID nếu nó không null (đối tượng đã được lưu hoặc gán ID)
        // Nếu ID null, các đối tượng chỉ bằng nhau nếu là cùng một instance (đã check ở
        // dòng đầu)
        return getAppointmentId() != null && Objects.equals(getAppointmentId(), that.getAppointmentId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để nhất quán với equals,
        // ngay cả khi ID là null hoặc đối tượng là proxy.
        // Không nên dùng ID ở đây vì nó có thể null ban đầu, gây thay đổi hashCode khi
        // đối tượng được lưu.
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý quan trọng cho helper methods `setPatient`/`setDoctor` ---
    // Các phương thức này yêu cầu các lớp liên quan (Patient, Doctor) phải có
    // các phương thức helper nội bộ (ví dụ: addAppointmentInternal,
    // removeAppointmentInternal)
    // với quyền truy cập phù hợp (package-private hoặc protected) để quản lý
    // collection phía @OneToMany một cách an toàn (khởi tạo nếu null, thêm/xóa phần
    // tử).
    // Ví dụ trong Patient.java:
    //
    // @OneToMany(mappedBy = "patient", ...)
    // @Getter(AccessLevel.PACKAGE) // Giới hạn getter nếu cần
    // private Set<Appointment> appointments = new HashSet<>();
    //
    // void addAppointmentInternal(Appointment appointment) {
    // if (this.appointments == null) this.appointments = new HashSet<>();
    // this.appointments.add(appointment);
    // // KHÔNG gọi lại appointment.setPatient(this) ở đây
    // }
    //
    // void removeAppointmentInternal(Appointment appointment) {
    // if (this.appointments != null) this.appointments.remove(appointment);
    // // KHÔNG gọi lại appointment.setPatient(null) ở đây
    // }
}