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

import java.time.LocalDateTime;
import java.util.Collections; // Có thể dùng cho getter chỉ đọc (tùy chọn)
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entity đại diện cho bảng Departments trong cơ sở dữ liệu.
 * Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn,
 * FetchType.LAZY, cascade an toàn và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// Luôn exclude collection LAZY khỏi toString để tránh lỗi và tăng hiệu năng
@ToString(exclude = { "doctors" })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "Departments", uniqueConstraints = {
        // Định nghĩa UNIQUE constraint cho department_name
        @UniqueConstraint(name = "UQ_Departments_DepartmentName", columnNames = { "department_name" })
})
public class Department {

    /**
     * Khóa chính của khoa, kiểu UUID, tự sinh bởi cơ sở dữ liệu.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Phù hợp UNIQUEIDENTIFIER + DEFAULT
    @Column(name = "department_id", nullable = false, updatable = false)
    private UUID departmentId;

    /**
     * Tên của khoa, yêu cầu là duy nhất và không được null.
     */
    @Column(name = "department_name", nullable = false, length = 100) // unique được xử lý bởi @UniqueConstraint
    private String departmentName;

    /**
     * Thời điểm bản ghi khoa được tạo. Quản lý tự động bởi Hibernate.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi khoa được cập nhật lần cuối. Quản lý tự động bởi Hibernate.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ OneToMany (Phía không sở hữu - Inverse Side) ---

    /**
     * Danh sách các bác sĩ thuộc khoa này.
     * - mappedBy: Trỏ đến trường 'department' trong lớp Doctor.
     * - cascade: PERSIST và MERGE là an toàn, không nên dùng REMOVE.
     * - fetch: LAZY là bắt buộc cho hiệu năng.
     * - orphanRemoval=false: Gỡ Doctor khỏi Set không xóa Doctor khỏi DB.
     * - Collection được khởi tạo để tránh NullPointerException.
     * - Setter được giới hạn truy cập để khuyến khích dùng helper methods.
     */
    @OneToMany(mappedBy = "department", // Liên kết với trường 'department' trong Entity Doctor
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }, // Cascade an toàn: Lưu/Cập nhật
            fetch = FetchType.LAZY, // Bắt buộc LAZY
            orphanRemoval = false // Không tự xóa Doctor con mồ côi
    )
    @Setter(AccessLevel.PACKAGE) // Hạn chế set trực tiếp collection từ bên ngoài
    private Set<Doctor> doctors = new HashSet<>(); // Khởi tạo sẵn

    // --- Helper methods quản lý quan hệ hai chiều an toàn ---
    // Các phương thức này được gọi từ bên ngoài để quản lý doctors.

    /**
     * Thêm một bác sĩ vào khoa này và đảm bảo mối quan hệ hai chiều được đồng bộ.
     * Phương thức này cũng sẽ gọi Doctor.setDepartment(this).
     * 
     * @param doctor Bác sĩ cần thêm (không được null).
     */
    public void addDoctor(Doctor doctor) {
        Objects.requireNonNull(doctor, "Doctor cannot be null");
        if (this.doctors == null) {
            this.doctors = new HashSet<>();
        }
        // Chỉ thêm và gọi lại nếu doctor chưa có trong collection
        if (this.doctors.add(doctor)) {
            // Đồng bộ phía đối diện (Doctor)
            // Quan trọng: Đảm bảo Doctor.setDepartment xử lý an toàn, không gọi lại vô hạn
            if (!this.equals(doctor.getDepartment())) {
                doctor.setDepartment(this); // Gọi setter hoặc helper ở Doctor
            }
        }
    }

    /**
     * Xóa một bác sĩ khỏi khoa này và đảm bảo mối quan hệ hai chiều được đồng bộ.
     * Phương thức này cũng sẽ gọi Doctor.setDepartment(null).
     * 
     * @param doctor Bác sĩ cần xóa (không được null).
     */
    public void removeDoctor(Doctor doctor) {
        Objects.requireNonNull(doctor, "Doctor cannot be null");
        if (this.doctors != null) {
            // Chỉ xóa và gọi lại nếu doctor có trong collection
            if (this.doctors.remove(doctor)) {
                // Đồng bộ phía đối diện (Doctor)
                // Chỉ set null nếu Doctor đang tham chiếu đến Department này
                if (this.equals(doctor.getDepartment())) {
                    doctor.setDepartment(null); // Gọi setter hoặc helper ở Doctor
                }
            }
        }
    }

    // --- Phương thức nội bộ (package-private) để Doctor gọi lại ---
    // Giúp đóng gói logic quản lý collection bên trong Department khi
    // Doctor.setDepartment được gọi.

    /**
     * Thêm Doctor vào collection nội bộ. Chỉ nên được gọi bởi
     * Doctor.setDepartment().
     * 
     * @param doctor Doctor cần thêm.
     */
    void addDoctorInternal(Doctor doctor) {
        if (this.doctors == null) {
            this.doctors = new HashSet<>();
        }
        this.doctors.add(doctor); // Chỉ thêm vào collection, không gọi lại Doctor
    }

    /**
     * Xóa Doctor khỏi collection nội bộ. Chỉ nên được gọi bởi
     * Doctor.setDepartment(null).
     * 
     * @param doctor Doctor cần xóa.
     */
    void removeDoctorInternal(Doctor doctor) {
        if (this.doctors != null) {
            this.doctors.remove(doctor); // Chỉ xóa khỏi collection, không gọi lại Doctor
        }
    }

    /**
     * Cung cấp một view chỉ đọc (unmodifiable) của danh sách bác sĩ.
     * An toàn hơn là trả về tham chiếu trực tiếp tới Set nội bộ.
     * 
     * @return Một Set chỉ đọc chứa các bác sĩ thuộc khoa này.
     */
    public Set<Doctor> getDoctorsView() {
        // Trả về set trống nếu doctors là null, hoặc set chỉ đọc nếu không null
        return Collections.unmodifiableSet(this.doctors != null ? this.doctors : Collections.emptySet());
    }

    // --- equals() và hashCode() chuẩn cho JPA/Hibernate ---
    // Đảm bảo hoạt động đúng với LAZY loading và Hibernate proxies.
    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        // Lấy class thực sự, bỏ qua proxy
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        // So sánh class thực sự
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        // Ép kiểu an toàn
        Department that = (Department) o;
        // So sánh dựa trên ID nếu đã có (khác null)
        return getDepartmentId() != null && Objects.equals(getDepartmentId(), that.getDepartmentId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý quan trọng về quản lý quan hệ hai chiều ---
    // Để các helper methods hoạt động chính xác và an toàn nhất, lớp Doctor nên:
    // 1. Có phương thức `setDepartment(Department department)`
    // 2. Bên trong `setDepartment`, nó nên:
    // a. Ngắt kết nối khỏi Department cũ (nếu có) bằng cách gọi
    // `oldDepartment.removeDoctorInternal(this);`
    // b. Gán `this.department = newDepartment;`
    // c. Kết nối với Department mới (nếu không null) bằng cách gọi
    // `newDepartment.addDoctorInternal(this);`
    // => Bằng cách này, logic thêm/xóa khỏi collection được thực hiện bởi
    // Department thông qua các phương thức internal,
    // và tránh được hoàn toàn nguy cơ vòng lặp vô hạn khi đồng bộ hai chiều.
}