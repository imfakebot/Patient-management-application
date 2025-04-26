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

import java.time.LocalDateTime;
import java.util.Collections; // Có thể dùng cho getter chỉ đọc (tùy chọn)
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity đại diện cho bảng Diseases trong cơ sở dữ liệu.
 * Lưu trữ thông tin chuẩn hóa về các loại bệnh (ví dụ: theo ICD).
 * Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn, FetchType.LAZY,
 * và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// Luôn exclude collection LAZY khỏi toString
@ToString(exclude = { "diagnoses" })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "Diseases", uniqueConstraints = {
        // Ràng buộc UNIQUE cho disease_name
        @UniqueConstraint(name = "UQ_Diseases_DiseaseName", columnNames = { "disease_name" })
})
public class Disease {

    /**
     * Khóa chính: Mã bệnh (ví dụ: mã ICD-10). Kiểu VARCHAR(20).
     * Đây là assigned identifier, giá trị phải được cung cấp khi tạo entity.
     */
    @Id // Đánh dấu là khóa chính
    @Column(name = "disease_code", nullable = false, updatable = false, length = 20)
    // KHÔNG có @GeneratedValue vì khóa chính là assigned
    private String diseaseCode;

    /**
     * Tên đầy đủ của bệnh, yêu cầu là duy nhất và không được null.
     */
    @Column(name = "disease_name", nullable = false, length = 255) // unique được xử lý bởi @UniqueConstraint
    private String diseaseName;

    /**
     * Mô tả chi tiết về bệnh (tùy chọn). Sử dụng @Lob cho TEXT.
     */
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Thời điểm bản ghi bệnh được tạo. Quản lý tự động.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi bệnh được cập nhật lần cuối. Quản lý tự động.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Mối quan hệ OneToMany (Phía không sở hữu - Inverse Side) ---

    /**
     * Danh sách các chẩn đoán liên quan đến loại bệnh này.
     * 'mappedBy = "disease"' trỏ tới trường 'disease' trong lớp Diagnosis.
     * Xóa Disease không nên xóa Diagnoses (DB không có ON DELETE CASCADE).
     * Chỉ cascade PERSIST/MERGE là an toàn nếu cần.
     */
    @OneToMany(mappedBy = "disease", // Liên kết với trường 'disease' trong Entity Diagnosis
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }, // Cascade an toàn: Lưu/Cập nhật
            fetch = FetchType.LAZY, // Bắt buộc LAZY
            orphanRemoval = false // Không tự xóa Diagnosis con mồ côi
    )
    @Setter(AccessLevel.PACKAGE) // Hạn chế set trực tiếp collection từ bên ngoài
    private Set<Diagnosis> diagnoses = new HashSet<>(); // Khởi tạo sẵn

    // --- Helper methods quản lý quan hệ hai chiều an toàn ---

    /**
     * Thêm một chẩn đoán vào danh sách liên quan đến bệnh này và đồng bộ hai chiều.
     * 
     * @param diagnosis Chẩn đoán cần thêm (không được null).
     */
    public void addDiagnosis(Diagnosis diagnosis) {
        Objects.requireNonNull(diagnosis, "Diagnosis cannot be null");
        if (this.diagnoses == null) {
            this.diagnoses = new HashSet<>();
        }
        // Chỉ thêm và gọi lại nếu diagnosis chưa có trong collection
        if (this.diagnoses.add(diagnosis)) {
            // Đồng bộ phía đối diện (Diagnosis)
            if (!this.equals(diagnosis.getDisease())) {
                diagnosis.setDisease(this); // Gọi setter hoặc helper ở Diagnosis
            }
        }
    }

    /**
     * Xóa một chẩn đoán khỏi danh sách liên quan đến bệnh này và đồng bộ hai chiều.
     * 
     * @param diagnosis Chẩn đoán cần xóa (không được null).
     */
    public void removeDiagnosis(Diagnosis diagnosis) {
        Objects.requireNonNull(diagnosis, "Diagnosis cannot be null");
        if (this.diagnoses != null) {
            // Chỉ xóa và gọi lại nếu diagnosis có trong collection
            if (this.diagnoses.remove(diagnosis)) {
                // Đồng bộ phía đối diện (Diagnosis)
                if (this.equals(diagnosis.getDisease())) {
                    diagnosis.setDisease(null); // Gọi setter hoặc helper ở Diagnosis
                }
            }
        }
    }

    // --- Phương thức nội bộ (package-private) để Diagnosis gọi lại ---

    /**
     * Thêm Diagnosis vào collection nội bộ. Chỉ nên được gọi bởi
     * Diagnosis.setDisease().
     * 
     * @param diagnosis Diagnosis cần thêm.
     */
    void addDiagnosisInternal(Diagnosis diagnosis) {
        if (this.diagnoses == null) {
            this.diagnoses = new HashSet<>();
        }
        this.diagnoses.add(diagnosis); // Chỉ thêm, không gọi lại
    }

    /**
     * Xóa Diagnosis khỏi collection nội bộ. Chỉ nên được gọi bởi
     * Diagnosis.setDisease(null).
     * 
     * @param diagnosis Diagnosis cần xóa.
     */
    void removeDiagnosisInternal(Diagnosis diagnosis) {
        if (this.diagnoses != null) {
            this.diagnoses.remove(diagnosis); // Chỉ xóa, không gọi lại
        }
    }

    /**
     * Cung cấp một view chỉ đọc (unmodifiable) của danh sách chẩn đoán.
     * 
     * @return Một Set chỉ đọc chứa các chẩn đoán liên quan đến bệnh này.
     */
    public Set<Diagnosis> getDiagnosesView() {
        return Collections.unmodifiableSet(this.diagnoses != null ? this.diagnoses : Collections.emptySet());
    }

    // --- equals() và hashCode() chuẩn cho JPA/Hibernate ---
    // Đặc biệt cho assigned identifier (String)

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
        Disease disease = (Disease) o;
        // So sánh dựa trên diseaseCode nếu nó đã được gán (khác null và không rỗng)
        // Đối với assigned ID, đây là cách so sánh an toàn nhất.
        return getDiseaseCode() != null && !getDiseaseCode().isEmpty()
                && Objects.equals(getDiseaseCode(), disease.getDiseaseCode());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán,
        // không phụ thuộc vào assigned ID có thể chưa được gán.
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu lớp Diagnosis phải có các phương thức setter hoặc internal helper
    // tương ứng
    // (setDisease, add/removeDiagnosisInternal) để hoàn thiện việc đồng bộ hai
    // chiều.
}