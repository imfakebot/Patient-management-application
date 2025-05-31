package com.pma.service;

import java.util.List; // Import Entity Patient
import java.util.Objects; // Import nếu cần xử lý liên kết UserAccount
import java.util.Optional; // Import Repository Patient
import java.util.UUID; // Import nếu cần kiểm tra UserAccount

import org.slf4j.Logger; // Exception chuẩn
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page; // Có thể cần cho lỗi UNIQUE
import org.springframework.data.domain.Pageable; // Cho phân trang
import org.springframework.stereotype.Service; // Cho phân trang
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pma.model.entity.Patient;
import com.pma.model.entity.UserAccount;
import com.pma.repository.PatientRepository;
import com.pma.model.enums.UserRole;
import com.pma.repository.UserAccountRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Lớp Service cho việc quản lý các nghiệp vụ liên quan đến Patient.
 */
@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final UserAccountRepository userAccountRepository; // Inject nếu cần kiểm tra/liên kết UserAccount
    private final UserAccountService userAccountService; // Thêm UserAccountService
    private final EmailService emailService; // Thêm EmailService

    @Autowired
    public PatientService(PatientRepository patientRepository, UserAccountRepository userAccountRepository, UserAccountService userAccountService, EmailService emailService) {
        this.patientRepository = patientRepository;
        this.userAccountRepository = userAccountRepository;
        this.userAccountService = userAccountService;
        this.emailService = emailService;
    }

    /**
     * Đăng ký hoặc tạo mới một bệnh nhân. Kiểm tra trùng lặp số điện thoại và
     * email trước khi lưu.
     *
     * @param patient Đối tượng Patient chứa thông tin cần tạo.
     * @return Patient đã được lưu.
     * @throws IllegalArgumentException nếu phone hoặc email đã tồn tại.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Patient registerPatient(Patient patient) {
        log.info("Attempting to register patient: {}", patient.getFullName());

        // --- Validation ---
        // Kiểm tra trùng số điện thoại
        patientRepository.findByPhone(patient.getPhone()).ifPresent(_ -> {
            log.warn("Registration failed. Phone number already exists: {}", patient.getPhone());
            throw new IllegalArgumentException("Phone number '" + patient.getPhone() + "' is already registered.");
        });

        // Kiểm tra trùng email (nếu email được cung cấp)
        if (patient.getEmail() != null && !patient.getEmail().trim().isEmpty()) {
            patientRepository.findByEmail(patient.getEmail()).ifPresent(_ -> {
                log.warn("Registration failed. Email already exists: {}", patient.getEmail());
                throw new IllegalArgumentException("Email '" + patient.getEmail() + "' is already registered.");
            });
        }

        // Đảm bảo ID là null khi tạo mới
        patient.setPatientId(null);

        // --- Data Access ---
        Patient savedPatient = patientRepository.save(patient);
        log.info("Successfully registered patient with id: {}", savedPatient.getPatientId());
        return savedPatient;
    }

    /**
     * Lấy thông tin chi tiết của một bệnh nhân bằng ID.
     *
     * @param id UUID của Patient.
     * @return Đối tượng Patient.
     * @throws EntityNotFoundException nếu không tìm thấy.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Patient getPatientById(UUID id) {
        log.info("Fetching patient with id: {}", id);
        return patientRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Patient not found with id: {}", id);
                    return new EntityNotFoundException("Patient not found with id: " + id);
                });
    }

    /**
     * Lấy danh sách tất cả bệnh nhân (có thể cần phân trang cho ứng dụng thực
     * tế).
     *
     * @return List các Patient.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Patient> getAllPatients() {
        log.info("Fetching all patients");
        List<Patient> patients = patientRepository.findAll();
        log.info("Found {} patients", patients.size());
        return patients;
    }

    /**
     * Lấy danh sách bệnh nhân có phân trang và sắp xếp.
     *
     * @param pageable Đối tượng chứa thông tin phân trang (số trang, kích
     * thước, sắp xếp).
     * @return Page chứa danh sách Patient cho trang yêu cầu.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Patient> getAllPatients(Pageable pageable) {
        log.info("Fetching patients with pagination: {}", pageable);
        Page<Patient> patientPage = patientRepository.findAll(pageable); // JpaRepository hỗ trợ sẵn Pageable
        log.info("Found {} patients on page {}/{}", patientPage.getNumberOfElements(), pageable.getPageNumber(),
                patientPage.getTotalPages());
        return patientPage;
    }

    /**
     * Cập nhật thông tin cơ bản của bệnh nhân. Không cập nhật các collection
     * liên quan ở đây (nên có service riêng).
     *
     * @param id UUID của Patient cần cập nhật.
     * @param patientDetails Đối tượng chứa thông tin mới (ví dụ: tên, địa chỉ,
     * phone, email...).
     * @return Patient đã được cập nhật.
     * @throws EntityNotFoundException nếu không tìm thấy Patient.
     * @throws IllegalArgumentException nếu phone/email mới bị trùng với người
     * khác.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Patient updatePatientDetails(UUID id, Patient patientDetails) {
        log.info("Attempting to update patient with id: {}", id);
        Patient existingPatient = getPatientById(id); // Lấy entity đang được quản lý

        // --- Validation ---
        // Kiểm tra trùng phone nếu có thay đổi
        if (patientDetails.getPhone() != null && !patientDetails.getPhone().equals(existingPatient.getPhone())) {
            patientRepository.findByPhone(patientDetails.getPhone()).ifPresent(otherPatient -> {
                log.warn("Update failed for patient id: {}. New phone {} conflicts with patient id: {}", id,
                        patientDetails.getPhone(), otherPatient.getPatientId());
                throw new IllegalArgumentException(
                        "Phone number '" + patientDetails.getPhone() + "' is already registered by another patient.");
            });
            existingPatient.setPhone(patientDetails.getPhone());
            log.info("Patient phone updated for id: {}", id);
        }

        // Kiểm tra trùng email nếu có thay đổi và email mới không rỗng
        if (patientDetails.getEmail() != null && !patientDetails.getEmail().trim().isEmpty()
                && !Objects.equals(patientDetails.getEmail(), existingPatient.getEmail())) {
            patientRepository.findByEmail(patientDetails.getEmail()).ifPresent(otherPatient -> {
                log.warn("Update failed for patient id: {}. New email {} conflicts with patient id: {}", id,
                        patientDetails.getEmail(), otherPatient.getPatientId());
                throw new IllegalArgumentException(
                        "Email '" + patientDetails.getEmail() + "' is already registered by another patient.");
            });
            existingPatient.setEmail(patientDetails.getEmail());
            log.info("Patient email updated for id: {}", id);
        } else if (patientDetails.getEmail() != null && patientDetails.getEmail().trim().isEmpty()) {
            // Cho phép set email thành rỗng/null nếu cần
            existingPatient.setEmail(null);
            log.info("Patient email cleared for id: {}", id);
        }

        // --- Cập nhật các trường khác (ví dụ) ---
        if (patientDetails.getFullName() != null) {
            existingPatient.setFullName(patientDetails.getFullName());
        }
        if (patientDetails.getDateOfBirth() != null) {
            existingPatient.setDateOfBirth(patientDetails.getDateOfBirth());
        }
        if (patientDetails.getGender() != null) {
            existingPatient.setGender(patientDetails.getGender());
        }
        // Cập nhật các trường địa chỉ, thông tin y tế, liên hệ khẩn cấp... tương tự
        if (patientDetails.getAddressLine1() != null) {
            existingPatient.setAddressLine1(patientDetails.getAddressLine1());
        }
        if (patientDetails.getAddressLine2() != null) {
            existingPatient.setAddressLine2(patientDetails.getAddressLine2());
        }
        if (patientDetails.getCity() != null) {
            existingPatient.setCity(patientDetails.getCity());
        }
        // ... (thêm các trường khác) ...
        if (patientDetails.getBloodType() != null) {
            existingPatient.setBloodType(patientDetails.getBloodType());
        }
        if (patientDetails.getAllergies() != null) {
            existingPatient.setAllergies(patientDetails.getAllergies());
        }
        // ...

        // Transaction commit sẽ tự động lưu thay đổi vào DB
        log.info("Patient details update process completed for id: {}", id);
        return existingPatient;
    }

    /**
     * Xóa một bệnh nhân theo ID. **CẢNH BÁO:** Do cấu hình CascadeType.ALL và
     * orphanRemoval=true trong Patient, việc xóa Patient cũng sẽ xóa TẤT CẢ các
     * bản ghi Appointment, MedicalRecord, Prescription, Bill, và UserAccount
     * liên quan đến bệnh nhân này. Hãy đảm bảo đây là hành vi mong muốn hoặc
     * thay đổi cấu hình cascade.
     *
     * @param id UUID của Patient cần xóa.
     * @throws EntityNotFoundException nếu không tìm thấy Patient.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deletePatient(UUID id) {
        log.warn(
                "Attempting to DELETE patient with id: {} AND ALL ASSOCIATED DATA (Appointments, Records, Prescriptions, Bills, UserAccount) due to CascadeType.ALL!",
                id);
        // Kiểm tra sự tồn tại trước khi xóa để có exception rõ ràng hơn
        // Lấy thông tin bệnh nhân TRƯỚC KHI XÓA để gửi email
        Patient patientToDelete = getPatientById(id); // Ném EntityNotFoundException nếu không tồn tại

        String patientEmail = patientToDelete.getEmail();
        String patientFullName = patientToDelete.getFullName();

        try {
            patientRepository.deleteById(patientToDelete.getPatientId()); // Hoặc patientRepository.delete(patientToDelete);
            log.info("Successfully deleted patient with id: {}", patientToDelete.getPatientId());

            // Gửi email thông báo sau khi xóa thành công
            if (patientEmail != null && !patientEmail.isBlank()) {
                // Trong môi trường thực tế, bạn có thể muốn lấy tên admin đang đăng nhập
                // Ví dụ: String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
                // Hiện tại, chúng ta sẽ dùng một placeholder.
                String adminUsername = "Quản trị viên"; // Placeholder, có thể truyền từ controller nếu cần
                try {
                    emailService.sendAccountDeletionNotification(patientEmail, patientFullName, "Bệnh nhân", adminUsername);
                    log.info("Requested to send account deletion notification to former patient: {}", patientEmail);
                } catch (Exception ex) {
                    log.error("Failed to send deletion notification email to patient {}: {}", patientEmail, ex.getMessage());
                }
            } else {
                log.warn("Patient {} (ID: {}) did not have an email. Deletion notification not sent.", patientFullName, id);
            }

        } catch (DataIntegrityViolationException e) {
            // Mặc dù cascade ALL, vẫn có thể có ràng buộc khác hoặc lỗi không mong muốn
            log.error("Data integrity violation during deletion of patient id: {}. Error: {}", id, e.getMessage());
            throw new IllegalStateException("Could not delete patient with id " + id + " due to data integrity issues.",
                    e);
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Patient> findPatientByEmail(String email) {
        log.debug("Searching for patient by email: {}", email);
        return patientRepository.findByEmail(email);
    }

    // --- Các phương thức tiện ích khác ---
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Patient> findPatientByPhone(String phone) {
        log.debug("Searching for patient by phone: {}", phone);
        return patientRepository.findByPhone(phone);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Patient> searchPatientsByName(String nameFragment) {
        log.debug("Searching for patients with name containing: {}", nameFragment);
        return patientRepository.findByFullNameContainingIgnoreCase(nameFragment);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Patient> findAll() {
        log.info("Fetching all patients via findAll()");
        List<Patient> patients = patientRepository.findAll();
        log.info("Found {} patients via findAll()", patients.size());
        return patients;
    }

    /**
     * Tạo bệnh nhân mới, tạo tài khoản người dùng liên kết và gửi email thông
     * tin tài khoản.
     *
     * @param patientData Thông tin bệnh nhân (chưa có ID).
     * @param username Tên đăng nhập cho tài khoản.
     * @param rawPassword Mật khẩu gốc (chưa băm).
     * @return Patient đã được tạo và lưu, kèm theo tài khoản.
     * @throws IllegalArgumentException nếu thông tin không hợp lệ hoặc username
     * đã tồn tại.
     * @throws EntityNotFoundException nếu có lỗi không mong muốn.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Patient createPatientWithAccountAndSendCredentials(Patient patientData, String username, String rawPassword) {
        log.info("Attempting to create patient '{}' with new user account '{}'", patientData.getFullName(), username);

        // Bước 1: Đăng ký bệnh nhân (bao gồm validation trùng phone/email của bệnh nhân)
        Patient savedPatient = registerPatient(patientData); // registerPatient đã xử lý validation trùng phone/email
        log.info("Patient {} registered successfully with ID: {}", savedPatient.getFullName(), savedPatient.getPatientId());

        // Bước 2: Tạo UserAccount
        UserAccount newUserAccount = new UserAccount();
        newUserAccount.setUsername(username);
        newUserAccount.setPasswordHash(rawPassword); // UserAccountService sẽ băm mật khẩu này
        newUserAccount.setRole(UserRole.PATIENT);
        // UserAccountService.createUserAccount sẽ tự động set active=true

        UserAccount createdAccount = userAccountService.createUserAccount(newUserAccount);
        log.info("UserAccount {} created successfully for Patient ID: {}", createdAccount.getUsername(), savedPatient.getPatientId());

        // Bước 3: Liên kết UserAccount với Patient
        userAccountService.linkPatientToUserAccount(createdAccount.getUserId(), savedPatient.getPatientId());
        log.info("Successfully linked UserAccount {} to Patient {}", createdAccount.getUserId(), savedPatient.getPatientId());

        // Bước 4: Gửi email thông tin tài khoản
        if (savedPatient.getEmail() != null && !savedPatient.getEmail().isBlank()) {
            try {
                emailService.sendNewAccountCredentials(
                        savedPatient.getEmail(),
                        savedPatient.getFullName(),
                        createdAccount.getUsername(),
                        rawPassword // Gửi mật khẩu gốc
                );
                log.info("Requested to send new account credentials email to patient: {}", savedPatient.getEmail());
            } catch (Exception ex) {
                log.error("Failed to send new account credentials email to patient {}: {}", savedPatient.getEmail(), ex.getMessage());
                // Không ném lại exception để không rollback transaction chính
            }
        } else {
            log.warn("Patient {} (ID: {}) does not have an email. Cannot send account credentials.",
                    savedPatient.getFullName(), savedPatient.getPatientId());
        }

        // Gán lại UserAccount vào Patient để trả về (nếu cần thông tin UserAccount ngay)
        // Hoặc bạn có thể fetch lại Patient nếu UserAccount được load LAZY
        savedPatient.setUserAccount(createdAccount); // Giả sử UserAccount được set 2 chiều
        return savedPatient;
    }

    // --- Có thể thêm các phương thức để quản lý liên kết UserAccount ---
    /*
     * @Transactional
     * public void linkUserAccountToPatient(UUID patientId, UUID userId) {
     * Patient patient = getPatientById(patientId);
     * UserAccount userAccount = userAccountRepository.findById(userId)
     * .orElseThrow(() -> new
     * EntityNotFoundException("UserAccount not found with id: " + userId));
     * 
     * // Kiểm tra xem userAccount hoặc patient đã được liên kết chưa (logic 1-1)
     * if (userAccount.getPatient() != null || userAccount.getDoctor() != null) {
     * throw new IllegalStateException("UserAccount " + userId +
     * " is already linked.");
     * }
     * if (patient.getUserAccount() != null) {
     * throw new IllegalStateException("Patient " + patientId +
     * " is already linked to a UserAccount.");
     * }
     * 
     * // Dùng helper method để thiết lập hai chiều
     * userAccount.setPatient(patient);
     * // Hoặc patient.setUserAccount(userAccount); // Chỉ gọi 1 phía nếu helper
     * method xử lý cả 2 chiều
     * // Không cần gọi save rõ ràng vì các entity đang được quản lý trong
     * transaction
     * }
     * 
     * @Transactional
     * public void unlinkUserAccountFromPatient(UUID patientId) {
     * Patient patient = getPatientById(patientId);
     * if (patient.getUserAccount() != null) {
     * patient.getUserAccount().setPatient(null); // Gọi helper method để ngắt 2
     * chiều
     * // Hoặc patient.setUserAccount(null);
     * }
     * }
     */
}
