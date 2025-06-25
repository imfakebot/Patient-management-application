package com.pma.service; // Đảm bảo đúng package

import java.util.List; // Import Department nếu cần xử lý liên kết
import java.util.Optional; // Import Entity Doctor
import java.util.UUID; // Import UserAccount nếu cần xử lý liên kết

import org.slf4j.Logger; // Import để lấy Department
import org.slf4j.LoggerFactory; // Import Repository Doctor
import org.springframework.beans.factory.annotation.Autowired; // Import nếu cần xử lý UserAccount
import org.springframework.dao.DataIntegrityViolationException; // Exception chuẩn
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pma.model.entity.Department;
import com.pma.model.entity.Doctor;
import com.pma.model.entity.UserAccount;
import com.pma.model.enums.UserRole;
import com.pma.repository.DepartmentRepository;
import com.pma.repository.DoctorRepository;
import com.pma.repository.UserAccountRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Lớp Service cho việc quản lý các nghiệp vụ liên quan đến Doctor.
 */
@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository; // Cần để gán Department
    private final UserAccountRepository userAccountRepository; // Cần nếu quản lý UserAccount ở đây
    private final UserAccountService userAccountService; // Thêm UserAccountService
    private final EmailService emailService; // Thêm EmailService 

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
            DepartmentRepository departmentRepository,
            UserAccountRepository userAccountRepository,
            UserAccountService userAccountService,
            EmailService emailService) {
        // @Lazy PasswordEncoder passwordEncoder) { // If you need to inject PasswordEncoder here
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.userAccountService = userAccountService;
        this.emailService = emailService;
        // this.passwordEncoder = passwordEncoder; // Assign it
    }

    /**
     * Tạo mới một bác sĩ. Kiểm tra trùng lặp phone, email, medicalLicense. Yêu
     * cầu departmentId phải hợp lệ.
     *
     * @param doctor Đối tượng Doctor chứa thông tin cần tạo (ID nên là null).
     * @param departmentId UUID của Department mà bác sĩ sẽ thuộc về.
     * @return Doctor đã được lưu.
     * @throws EntityNotFoundException nếu departmentId không tồn tại.
     * @throws IllegalArgumentException nếu phone, email, hoặc medicalLicense đã
     * tồn tại.
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
        doctorRepository.findByPhone(doctor.getPhone()).ifPresent(_ -> {
            log.warn("Doctor creation failed. Phone number already exists: {}", doctor.getPhone());
            throw new IllegalArgumentException("Phone number '" + doctor.getPhone() + "' is already registered.");
        });
        doctorRepository.findByEmail(doctor.getEmail()).ifPresent(_ -> {
            log.warn("Doctor creation failed. Email already exists: {}", doctor.getEmail());
            throw new IllegalArgumentException("Email '" + doctor.getEmail() + "' is already registered.");
        });
        doctorRepository.findByMedicalLicense(doctor.getMedicalLicense()).ifPresent(_ -> {
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
        List<Doctor> doctors = doctorRepository.findAllWithDepartments(); // Sử dụng phương thức mới
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
     * Cập nhật thông tin của một bác sĩ. Cho phép thay đổi khoa (department).
     *
     * @param id UUID của Doctor cần cập nhật.
     * @param doctorDetails Đối tượng chứa thông tin mới.
     * @param newDepartmentId (Optional) UUID của khoa mới, nếu cần thay đổi.
     * Null nếu không đổi khoa.
     * @return Doctor đã được cập nhật.
     * @throws EntityNotFoundException nếu không tìm thấy Doctor hoặc Department
     * (nếu newDepartmentId được cung cấp).
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
        if (doctorDetails.getFullName() != null) {
            existingDoctor.setFullName(doctorDetails.getFullName());
        }
        if (doctorDetails.getDateOfBirth() != null) {
            existingDoctor.setDateOfBirth(doctorDetails.getDateOfBirth());
        }
        if (doctorDetails.getGender() != null) {
            existingDoctor.setGender(doctorDetails.getGender());
        }
        if (doctorDetails.getSpecialty() != null) {
            existingDoctor.setSpecialty(doctorDetails.getSpecialty());
        }
        if (doctorDetails.getYearsOfExperience() != null) {
            existingDoctor.setYearsOfExperience(doctorDetails.getYearsOfExperience());
        }
        if (doctorDetails.getSalary() != null) {
            existingDoctor.setSalary(doctorDetails.getSalary());
        }
        if (doctorDetails.getStatus() != null) {
            existingDoctor.setStatus(doctorDetails.getStatus());
        }

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
     * Xóa một bác sĩ theo ID. **LƯU Ý:** Việc xóa Doctor có thể làm các Foreign
     * Key trong Appointment, MedicalRecord, Prescription bị set thành NULL (do
     * ON DELETE SET NULL hoặc ON DELETE NO ACTION/RESTRICT ở DB) hoặc gây lỗi
     * nếu có ràng buộc khác. Cần kiểm tra nghiệp vụ cẩn thận. Nếu Doctor có
     * UserAccount liên kết (với CascadeType.ALL), UserAccount cũng sẽ bị xóa.
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
        // Lấy thông tin bác sĩ TRƯỚC KHI XÓA để gửi email
        Doctor doctorToDelete = getDoctorById(id); // Ném EntityNotFoundException nếu không tồn tại

        String doctorEmail = doctorToDelete.getEmail();
        String doctorFullName = doctorToDelete.getFullName();

        try {
            // Việc xóa UserAccount (nếu cascade=ALL) sẽ diễn ra tự động ở đây
            doctorRepository.deleteById(doctorToDelete.getDoctorId());
            log.info("Successfully deleted doctor with id: {}", doctorToDelete.getDoctorId());

            // Gửi email thông báo sau khi xóa thành công
            if (doctorEmail != null && !doctorEmail.isBlank()) {
                String adminUsername = "Quản trị viên"; // Placeholder
                try {
                    emailService.sendAccountDeletionNotification(doctorEmail, doctorFullName, "Bác sĩ", adminUsername);
                    log.info("Requested to send account deletion notification to former doctor: {}", doctorEmail);
                } catch (Exception ex) {
                    log.error("Failed to send deletion notification email to doctor {}: {}", doctorEmail, ex.getMessage());
                }
            } else {
                log.warn("Doctor {} (ID: {}) did not have an email. Deletion notification not sent.", doctorFullName, id);
            }

        } catch (DataIntegrityViolationException e) {
            // Bắt lỗi nếu có ràng buộc khóa ngoại khác ngăn cản việc xóa
            log.error(
                    "Data integrity violation during deletion of doctor id: {}. Check if related records (Appointments, MedicalRecords, Prescriptions) prevent deletion. Error: {}",
                    id, e.getMessage());
            throw new IllegalStateException("Could not delete doctor with id " + id
                    + " due to data integrity issues. Associated records might still exist.", e);
        }
    }

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

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Doctor> findAll() {
        log.info("Fetching all doctors via findAll()");
        List<Doctor> doctors = doctorRepository.findAll();
        log.info("Found {} doctors via findAll()", doctors.size());
        return doctors;
    }

    /**
     * Tạo bác sĩ mới, tạo tài khoản người dùng liên kết và gửi email thông tin
     * tài khoản.
     *
     * @param doctorData Thông tin bác sĩ (chưa có ID).
     * @param departmentId ID của Department mà bác sĩ sẽ thuộc về.
     * @param username Tên đăng nhập cho tài khoản.
     * @param rawPassword Mật khẩu gốc (chưa băm).
     * @return Doctor đã được tạo và lưu, kèm theo tài khoản.
     * @throws IllegalArgumentException nếu thông tin không hợp lệ hoặc username
     * đã tồn tại.
     * @throws EntityNotFoundException nếu departmentId không tồn tại hoặc có
     * lỗi không mong muốn.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Doctor createDoctorWithAccountAndSendCredentials(Doctor doctorData, UUID departmentId, String username, String rawPassword) {
        log.info("Attempting to create doctor '{}' with new user account '{}' in department '{}'",
                doctorData.getFullName(), username, departmentId);

        // Bước 1: Tạo Doctor (bao gồm validation trùng phone/email/license và gán department)
        Doctor savedDoctor = createDoctor(doctorData, departmentId);
        log.info("Doctor {} created successfully with ID: {}", savedDoctor.getFullName(), savedDoctor.getDoctorId());

        // Bước 2: Tạo UserAccount
        UserAccount newUserAccount = new UserAccount();
        newUserAccount.setUsername(username);
        newUserAccount.setRole(UserRole.DOCTOR);
        // UserAccountService.createUserAccount sẽ tự động set active=true

        newUserAccount.setPasswordHash(rawPassword); // This will be hashed by UserAccountService

        UserAccount createdAccount = userAccountService.createUserAccount(newUserAccount);
        log.info("UserAccount {} created successfully for Doctor ID: {}", createdAccount.getUsername(), savedDoctor.getDoctorId());

        // Bước 3: Liên kết UserAccount với Doctor
        userAccountService.linkDoctorToUserAccount(createdAccount.getUserId(), savedDoctor.getDoctorId());
        log.info("Successfully linked UserAccount {} to Doctor {}", createdAccount.getUserId(), savedDoctor.getDoctorId());

        // Bước 4: Gửi email thông tin tài khoản
        if (savedDoctor.getEmail() != null && !savedDoctor.getEmail().isBlank()) {
            try {
                emailService.sendNewDoctorAccountCredentials(
                        savedDoctor.getEmail(),
                        savedDoctor.getFullName(), // Gửi tên đầy đủ của bác sĩ
                        createdAccount.getUsername(), // Username
                        rawPassword // Gửi mật khẩu tạm thời
                );
                log.info("Requested to send new account credentials email to doctor: {}", savedDoctor.getEmail());
            } catch (Exception ex) {
                log.error("Failed to send new account credentials email to doctor {}: {}", savedDoctor.getEmail(), ex.getMessage());
                // Không throw lại để không rollback transaction chính
            }
        } else {
            log.warn("Doctor {} (ID: {}) does not have an email. Cannot send account credentials.",
                    savedDoctor.getFullName(), savedDoctor.getDoctorId());
        }

        // Gán lại UserAccount vào Doctor để trả về (nếu cần thông tin UserAccount ngay)
        savedDoctor.setUserAccount(createdAccount); // Giả sử UserAccount được set 2 chiều
        return savedDoctor;
    }
    // --- Các phương thức quản lý UserAccount có thể đặt ở UserAccountService ---
}
