package com.pma.model.entity; // <-- THAY ĐỔI PACKAGE NẾU CẦN

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy; // Cần cho equals/hashCode chuẩn

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// ---> IMPORT ENUM TỪ PACKAGE RIÊNG <---
import com.pma.model.enums.DiagnosisStatus; // <-- THAY ĐỔI PACKAGE ENUM NẾU CẦN

/**
 * Entity đại diện cho bảng Diagnoses trong cơ sở dữ liệu.
 * Lưu trữ chi tiết về một chẩn đoán cụ thể cho một bản ghi y tế.
 * Phiên bản tối ưu, sử dụng Lombok, equals/hashCode chuẩn, FetchType.LAZY,
 * và helper methods quản lý quan hệ hai chiều.
 */
@Getter
@Setter
// Luôn exclude các quan hệ LAZY khỏi toString
@ToString(exclude = { "medicalRecord", "disease" })
@NoArgsConstructor // Bắt buộc cho JPA
@Entity
@Table(name = "Diagnoses", indexes = {
        // Index từ schema SQL
        @Index(name = "IX_Diagnoses_record_id", columnList = "record_id"),
        @Index(name = "IX_Diagnoses_disease_code", columnList = "disease_code")
})
public class Diagnosis {

    /**
     * Khóa chính của chẩn đoán, kiểu UUID, tự sinh bởi cơ sở dữ liệu.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "diagnosis_id", nullable = false, updatable = false)
    private UUID diagnosisId;

    /**
     * Bản ghi y tế mà chẩn đoán này thuộc về. Bắt buộc.
     * QUAN TRỌNG: FetchType.LAZY.
     * DB có ON DELETE CASCADE, việc xóa MedicalRecord sẽ xóa Diagnosis này ở DB.
     * CascadeType.ALL/REMOVE ở phía MedicalRecord sẽ đảm bảo tính nhất quán trong
     * bộ nhớ.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc phải có MedicalRecord
    @JoinColumn(name = "record_id", nullable = false) // Liên kết cột FK
    private MedicalRecord medicalRecord;

    /**
     * Loại bệnh được chẩn đoán (tham chiếu tới bảng Diseases). Bắt buộc.
     * QUAN TRỌNG: FetchType.LAZY.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Bắt buộc phải có Disease
    @JoinColumn(name = "disease_code", nullable = false) // Liên kết cột FK (disease_code)
    private Disease disease;

    /**
     * Mô tả chi tiết về chẩn đoán này cho bệnh nhân cụ thể (tùy chọn).
     * Sử dụng @Lob cho kiểu TEXT.
     */
    @Lob
    @Column(name = "diagnosis_description", columnDefinition = "TEXT")
    private String diagnosisDescription;

    /**
     * Ngày chẩn đoán được thực hiện hoặc ghi nhận.
     * Gán giá trị mặc định trong Java là thực hành tốt.
     */
    @Column(name = "diagnosis_date") // nullable=true trong DB (do có DEFAULT), nhưng nên có giá trị
    private LocalDate diagnosisDate = LocalDate.now(); // Gán mặc định khi tạo object

    /**
     * Trạng thái của chẩn đoán (ví dụ: Active, Resolved).
     * Sử dụng Enum để chuẩn hóa và đảm bảo an toàn kiểu.
     */
    @Enumerated(EnumType.STRING) // Lưu tên Enum ("Active", "Chronic", ...)
    @Column(name = "status", length = 20) // nullable=true khớp với SQL
    private DiagnosisStatus status; // Sử dụng enum DiagnosisStatus đã import

    /**
     * Thời điểm bản ghi chẩn đoán được tạo. Quản lý tự động.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi chẩn đoán được cập nhật lần cuối. Quản lý tự động.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Helper methods quản lý quan hệ hai chiều an toàn ---

    /**
     * Thiết lập MedicalRecord cho chẩn đoán này và đồng bộ hai chiều.
     * Đây là cách được khuyến nghị để thiết lập mối quan hệ.
     * Yêu cầu MedicalRecord có addDiagnosisInternal/removeDiagnosisInternal.
     * 
     * @param medicalRecord Bản ghi y tế mới, hoặc null để gỡ bỏ.
     */
    public void setMedicalRecord(MedicalRecord medicalRecord) {
        // Tránh gọi lại không cần thiết
        if (Objects.equals(this.medicalRecord, medicalRecord)) {
            return;
        }
        // Ngắt kết nối khỏi record cũ (nếu có)
        MedicalRecord oldMedicalRecord = this.medicalRecord;
        this.medicalRecord = null; // Ngắt trước
        if (oldMedicalRecord != null) {
            oldMedicalRecord.removeDiagnosisInternal(this); // Gọi lại internal ở MedicalRecord
        }
        // Kết nối với record mới (nếu không null)
        this.medicalRecord = medicalRecord;
        if (medicalRecord != null) {
            medicalRecord.addDiagnosisInternal(this); // Gọi lại internal ở MedicalRecord
        }
    }

    /**
     * Phương thức nội bộ (package-private) được gọi bởi MedicalRecord.
     * Chỉ cập nhật tham chiếu phía này, không gọi lại MedicalRecord.
     * 
     * @param medicalRecord Bản ghi y tế hoặc null.
     */
    void setMedicalRecordInternal(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
    }

    /**
     * Thiết lập Disease cho chẩn đoán này và đồng bộ hai chiều.
     * Yêu cầu Disease có addDiagnosisInternal/removeDiagnosisInternal.
     * 
     * @param disease Loại bệnh mới, hoặc null để gỡ bỏ.
     */
    public void setDisease(Disease disease) {
        // Tránh gọi lại không cần thiết
        if (Objects.equals(this.disease, disease)) {
            return;
        }
        // Ngắt kết nối khỏi disease cũ (nếu có)
        Disease oldDisease = this.disease;
        this.disease = null; // Ngắt trước
        if (oldDisease != null) {
            oldDisease.removeDiagnosisInternal(this); // Gọi lại internal ở Disease
        }
        // Kết nối với disease mới (nếu không null)
        this.disease = disease;
        if (disease != null) {
            disease.addDiagnosisInternal(this); // Gọi lại internal ở Disease
        }
    }

    /**
     * Phương thức nội bộ (package-private) được gọi bởi Disease.
     * Chỉ cập nhật tham chiếu phía này, không gọi lại Disease.
     * 
     * @param disease Loại bệnh hoặc null.
     */
    void setDiseaseInternal(Disease disease) {
        this.disease = disease;
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
        Diagnosis diagnosis = (Diagnosis) o;
        // So sánh dựa trên ID nếu đã có (khác null)
        return getDiagnosisId() != null && Objects.equals(getDiagnosisId(), diagnosis.getDiagnosisId());
    }

    @Override
    public final int hashCode() {
        // Sử dụng hashCode của class thực sự để đảm bảo tính nhất quán
        return (this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass()).hashCode();
    }

    // --- Lưu ý về Helper Methods ---
    // Yêu cầu các lớp MedicalRecord và Disease phải có các phương thức
    // internal helper tương ứng (add/removeDiagnosisInternal) để hoàn thiện
    // việc đồng bộ hai chiều.
}