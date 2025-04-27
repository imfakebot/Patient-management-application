package com.pma.service; // Đảm bảo đúng package

import com.pma.model.entity.Department; // Import Department nếu cần xử lý liên kết
import com.pma.model.entity.Doctor; // Import Entity Doctor
import com.pma.model.entity.UserAccount; // Import UserAccount nếu cần xử lý liên kết
import com.pma.repository.DepartmentRepository; // Import để lấy Department
import com.pma.repository.DoctorRepository; // Import Repository Doctor
import com.pma.repository.UserAccountRepository; // Import nếu cần xử lý UserAccount
import jakarta.persistence.EntityNotFoundException; // Exception chuẩn
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Lớp Service cho việc quản lý các nghiệp vụ liên quan đến Doctor.
 */
@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository; // Cần để gán Department
    private final UserAccountRepository userAccountRepository; // Cần nếu quản lý UserAccount ở đây

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            UserAccountRepository userAccountRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Tạo mới một bác sĩ.
     * Kiểm tra trùng lặp phone, email, medicalLicense.
     * Yêu cầu departmentId phải hợp lệ.
     *
     * @param doctor       Đối tượng Doctor chứa thông tin cần tạo (ID nên là null).
     * @param departmentId UUID của Department mà bác sĩ sẽ thuộc về.
     * @return Doctor đã được lưu.
     * @throws EntityNotFoundException  nếu departmentId không tồn tại.
     * @throws IllegalArgumentException nếu phone, email, hoặc medicalLicense đã tồn
     *                                  tại.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Doctor createDoctor(Doctor doctor, UUID departmentId) {
        log.info("Attempting to create doctor: {}", doctor.getFullName());

        // --- Validation ---
        // Kiểm tra Department tồn tại
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.warn("Doctor creation failed. Department not found with id: {}", departmentId);
                    return new EntityNotFoundException("Department not found with id: " + departmentId);
                });

        // Kiểm tra trùng lặp các trường UNIQUE
        doctorRepository.findByPhone(doctor.getPhone()).ifPresent(existing -> {
            log.warn("Doctor creation failed. Phone number already exists: {}", doctor.getPhone());
            throw new IllegalArgumentException("Phone number '" + doctor.getPhone() + "' is already registered.");
        });
        doctorRepository.findByEmail(doctor.getEmail()).ifPresent(existing -> {
            log.warn("Doctor creation failed. Email already exists: {}", doctor.getEmail());
            throw new IllegalArgumentException("Email '" + doctor.getEmail() + "' is already registered.");
        });
        doctorRepository.findByMedicalLicense(doctor.getMedicalLicense()).ifPresent(existing -> {
            log.warn("Doctor creation failed. Medical license already exists: {}", doctor.getMedicalLicense());
            throw new IllegalArgumentException(
                    "Medical license '" + doctor.getMedicalLicense() + "' is already registered.");
        });

        // --- Thiết lập quan hệ và lưu ---
        doctor.setDoctorId(null); // Đảm bảo tạo mới
        doctor.setDepartment(department); // Thiết lập quan hệ hai chiều (cần Department có addDoctorInternal)

        Doctor savedDoctor = doctorRepository.save(doctor);
        log.info("Successfully created doctor with id: {}", savedDoctor.getDoctorId());
        return savedDoctor;
    }

    /**
     * Lấy thông tin chi tiết của một bác sĩ bằng ID.
     *
     * @param id UUID của Doctor.
     * @return Đối tượng Doctor.
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Doctor getDoctorById(UUID id) {
        log.info("Fetching doctor with id: {}", id);
        return doctorRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Doctor not found with id: {}", id);
                    return new EntityNotFoundException("Doctor not found with id: " + id);
                });
    }

    /**
     * Lấy danh sách tất cả bác sĩ (có thể cần phân trang).
     *
     * @return List các Doctor.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Doctor> getAllDoctors() {
        log.info("Fetching all doctors");
        List<Doctor> doctors = doctorRepository.findAll();
        log.info("Found {} doctors", doctors.size());
        return doctors;
    }

    /**
     * Lấy danh sách bác sĩ có phân trang và sắp xếp.
     *
     * @param pageable Thông tin phân trang.
     * @return Page chứa danh sách Doctor.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Doctor> getAllDoctors(Pageable pageable) {
        log.info("Fetching doctors with pagination: {}", pageable);
        Page<Doctor> doctorPage = doctorRepository.findAll(pageable);
        log.info("Found {} doctors on page {}/{}", doctorPage.getNumberOfElements(), pageable.getPageNumber(),
                doctorPage.getTotalPages());
        return doctorPage;
    }

    /**
     * Lấy danh sách bác sĩ thuộc một khoa cụ thể.
     *
     * @param departmentId UUID của Department.
     * @return List các Doctor thuộc khoa đó.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Doctor> getDoctorsByDepartment(UUID departmentId) {
        log.info("Fetching doctors for department id: {}", departmentId);
        // Kiểm tra khoa tồn tại nếu cần, hoặc để Repository xử lý
        // if (!departmentRepository.existsById(departmentId)) { ... }
        return doctorRepository.findByDepartment_DepartmentId(departmentId);
    }

    /**
     * Cập nhật thông tin của một bác sĩ.
     * Cho phép thay đổi khoa (department).
     *
     * @param id              UUID của Doctor cần cập nhật.
     * @param doctorDetails   Đối tượng chứa thông tin mới.
     * @param newDepartmentId (Optional) UUID của khoa mới, nếu cần thay đổi. Null
     *                        nếu không đổi khoa.
     * @return Doctor đã được cập nhật.
     * @throws EntityNotFoundException  nếu không tìm thấy Doctor hoặc Department
     *                                  (nếu newDepartmentId được cung cấp).
     * @throws IllegalArgumentException nếu phone/email/license mới bị trùng.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Doctor updateDoctor(UUID id, Doctor doctorDetails, UUID newDepartmentId) {
        log.info("Attempting to update doctor with id: {}", id);
        Doctor existingDoctor = getDoctorById(id); // Lấy entity đang quản lý

        // --- Validation trùng lặp (tương tự createDoctor) ---
        if (doctorDetails.getPhone() != null && !doctorDetails.getPhone().equals(existingDoctor.getPhone())) {
            doctorRepository.findByPhone(doctorDetails.getPhone()).ifPresent(other -> {
                if (!other.getDoctorId().equals(id)) { // Đảm bảo không phải là chính nó
                    throw new IllegalArgumentException("Phone number already exists.");
                }
            });
            existingDoctor.setPhone(doctorDetails.getPhone());
        }
        if (doctorDetails.getEmail() != null && !doctorDetails.getEmail().equals(existingDoctor.getEmail())) {
            doctorRepository.findByEmail(doctorDetails.getEmail()).ifPresent(other -> {
                if (!other.getDoctorId().equals(id)) {
                    throw new IllegalArgumentException("Email already exists.");
                }
            });
            existingDoctor.setEmail(doctorDetails.getEmail());
        }
        if (doctorDetails.getMedicalLicense() != null
                && !doctorDetails.getMedicalLicense().equals(existingDoctor.getMedicalLicense())) {
            doctorRepository.findByMedicalLicense(doctorDetails.getMedicalLicense()).ifPresent(other -> {
                if (!other.getDoctorId().equals(id)) {
                    throw new IllegalArgumentException("Medical license already exists.");
                }
            });
            existingDoctor.setMedicalLicense(doctorDetails.getMedicalLicense());
        }

        // --- Cập nhật các trường khác ---
        if (doctorDetails.getFullName() != null)
            existingDoctor.setFullName(doctorDetails.getFullName());
        if (doctorDetails.getDateOfBirth() != null)
            existingDoctor.setDateOfBirth(doctorDetails.getDateOfBirth());
        if (doctorDetails.getGender() != null)
            existingDoctor.setGender(doctorDetails.getGender());
        if (doctorDetails.getSpecialty() != null)
            existingDoctor.setSpecialty(doctorDetails.getSpecialty());
        if (doctorDetails.getYearsOfExperience() != null)
            existingDoctor.setYearsOfExperience(doctorDetails.getYearsOfExperience());
        if (doctorDetails.getSalary() != null)
            existingDoctor.setSalary(doctorDetails.getSalary());
        if (doctorDetails.getStatus() != null)
            existingDoctor.setStatus(doctorDetails.getStatus());

        // --- Cập nhật Department nếu có yêu cầu ---
        if (newDepartmentId != null && (existingDoctor.getDepartment() == null
                || !newDepartmentId.equals(existingDoctor.getDepartment().getDepartmentId()))) {
            log.info("Attempting to change department for doctor id: {} to department id: {}", id, newDepartmentId);
            Department newDepartment = departmentRepository.findById(newDepartmentId)
                    .orElseThrow(
                            () -> new EntityNotFoundException("New Department not found with id: " + newDepartmentId));
            // Dùng helper method để cập nhật quan hệ hai chiều
            existingDoctor.setDepartment(newDepartment);
            log.info("Doctor {} moved to department {}", id, newDepartmentId);
        }

        // Transaction commit sẽ tự động lưu thay đổi
        log.info("Doctor update process completed for id: {}", id);
        return existingDoctor;
    }

    /**
     * Xóa một bác sĩ theo ID.
     * **LƯU Ý:** Việc xóa Doctor có thể làm các Foreign Key trong Appointment,
     * MedicalRecord, Prescription
     * bị set thành NULL (do ON DELETE SET NULL hoặc ON DELETE NO ACTION/RESTRICT ở
     * DB)
     * hoặc gây lỗi nếu có ràng buộc khác. Cần kiểm tra nghiệp vụ cẩn thận.
     * Nếu Doctor có UserAccount liên kết (với CascadeType.ALL), UserAccount cũng sẽ
     * bị xóa.
     *
     * @param id UUID của Doctor cần xóa.
     * @throws EntityNotFoundException nếu không tìm thấy Doctor.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteDoctor(UUID id) {
        log.warn(
                "Attempting to DELETE doctor with id: {}. This might set related foreign keys to NULL or delete associated UserAccount.",
                id);
        // Kiểm tra sự tồn tại
        if (!doctorRepository.existsById(id)) {
            log.error("Deletion failed. Doctor not found with id: {}", id);
            throw new EntityNotFoundException("Doctor not found with id: " + id);
        }
        try {
            // Việc xóa UserAccount (nếu cascade=ALL) sẽ diễn ra tự động ở đây
            doctorRepository.deleteById(id);
            log.info("Successfully deleted doctor with id: {}", id);
        } catch (DataIntegrityViolationException e) {
            // Bắt lỗi nếu có ràng buộc khóa ngoại khác ngăn cản việc xóa
            log.error(
                    "Data integrity violation during deletion of doctor id: {}. Check if related records (Appointments, MedicalRecords, Prescriptions) prevent deletion. Error: {}",
                    id, e.getMessage());
            throw new IllegalStateException("Could not delete doctor with id " + id
                    + " due to data integrity issues. Associated records might still exist.", e);
        }
    }

    // --- Các phương thức tiện ích hoặc tìm kiếm khác ---
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Doctor> findDoctorByEmail(String email) {
        log.debug("Searching for doctor by email: {}", email);
        return doctorRepository.findByEmail(email);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Doctor> findDoctorsBySpecialty(String specialty) {
        log.debug("Searching for doctors by specialty: {}", specialty);
        return doctorRepository.findBySpecialtyIgnoreCase(specialty);
    }

    // --- Các phương thức quản lý UserAccount có thể đặt ở UserAccountService ---
}