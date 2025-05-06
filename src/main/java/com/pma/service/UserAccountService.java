package com.pma.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails; // Interface quyền
import org.springframework.security.core.userdetails.UserDetailsService; // Implement quyền đơn giản
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Lớp User của Spring Security
import org.springframework.security.crypto.password.PasswordEncoder; // Interface UserDetails
import org.springframework.stereotype.Service; // Interface cần implement
import org.springframework.transaction.annotation.Isolation; // Exception chuẩn
import org.springframework.transaction.annotation.Propagation; // Để mã hóa mật khẩu
import org.springframework.transaction.annotation.Transactional;

import com.pma.model.entity.Doctor;
import com.pma.model.entity.Patient;
import com.pma.model.entity.UserAccount;
import com.pma.repository.DoctorRepository;
import com.pma.repository.PatientRepository; // Để tạo danh sách quyền đơn giản
import com.pma.repository.UserAccountRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Lớp Service cho việc quản lý UserAccount và tích hợp với Spring Security.
 * Implement UserDetailsService để cung cấp thông tin user cho quá trình xác
 * thực.
 */
@Service
public class UserAccountService implements UserDetailsService { // Implement UserDetailsService

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserAccountRepository userAccountRepository;
    private final PatientRepository patientRepository; // Cần để liên kết khi tạo tk Patient
    private final DoctorRepository doctorRepository; // Cần để liên kết khi tạo tk Doctor
    private final PasswordEncoder passwordEncoder; // Bean để mã hóa mật khẩu

    @Autowired
    public UserAccountService(UserAccountRepository userAccountRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            PasswordEncoder passwordEncoder) { // Inject PasswordEncoder
        this.userAccountRepository = userAccountRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Phương thức cốt lõi của UserDetailsService. Được Spring Security gọi tự
     * động khi người dùng cố gắng đăng nhập. Nhiệm vụ: Tìm UserAccount theo
     * username, nếu thấy thì tạo và trả về đối tượng UserDetails.
     *
     * @param username Tên đăng nhập do người dùng nhập.
     * @return Đối tượng UserDetails chứa thông tin user (username, password
     * hash, roles/authorities).
     * @throws UsernameNotFoundException nếu không tìm thấy user với username
     * cung cấp.
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS) // Chỉ đọc
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Attempting to load user by username: {}", username);

        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Username not found: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        log.info("User found: {}. Loading details...", username);

        // Tạo danh sách quyền (authorities) từ vai trò (role) của UserAccount
        // Spring Security thường yêu cầu tiền tố "ROLE_" cho vai trò
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userAccount.getRole().name()); // Lấy tên Enum
        // làm vai trò

        // Tạo đối tượng UserDetails chuẩn của Spring Security
        // User(username, passwordHash, enabled, accountNonExpired,
        // credentialsNonExpired, accountNonLocked, authorities)
        return new User(userAccount.getUsername(),
                userAccount.getPasswordHash(),
                userAccount.isActive(), // enabled
                true, // accountNonExpired (có thể thêm logic kiểm tra sau)
                true, // credentialsNonExpired (có thể thêm logic kiểm tra sau)
                userAccount.getLockoutUntil() == null || userAccount.getLockoutUntil().isBefore(LocalDateTime.now()), // accountNonLocked
                Collections.singletonList(authority)); // Danh sách quyền
    }

    /**
     * Tạo một tài khoản người dùng mới. Mật khẩu sẽ được mã hóa trước khi lưu.
     *
     * @param userAccount Đối tượng UserAccount với thông tin cơ bản (username,
     * password thô, role).
     * @return UserAccount đã được lưu với mật khẩu đã mã hóa.
     * @throws IllegalArgumentException nếu username đã tồn tại.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public UserAccount createUserAccount(UserAccount userAccount) {
        log.info("Attempting to create user account for username: {}", userAccount.getUsername());

        // --- Validation ---
        if (userAccountRepository.findByUsername(userAccount.getUsername()).isPresent()) {
            log.warn("User creation failed. Username already exists: {}", userAccount.getUsername());
            throw new IllegalArgumentException("Username '" + userAccount.getUsername() + "' already exists.");
        }
        if (userAccount.getPasswordHash() == null || userAccount.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        if (userAccount.getRole() == null) {
            throw new IllegalArgumentException("User role must be specified.");
        }

        // --- Xử lý ---
        userAccount.setUserId(null); // Đảm bảo tạo mới
        // Quan trọng: Mã hóa mật khẩu thô trước khi lưu
        userAccount.setPasswordHash(passwordEncoder.encode(userAccount.getPasswordHash()));
        userAccount.setActive(true); // Kích hoạt tài khoản mặc định
        // Các trường khác có thể set giá trị mặc định nếu cần

        // --- Data Access ---
        UserAccount savedUser = userAccountRepository.save(userAccount);
        log.info("Successfully created user account with id: {} for username: {}", savedUser.getUserId(),
                savedUser.getUsername());
        return savedUser;
    }

    /**
     * Liên kết một UserAccount với một Patient. Chỉ thực hiện nếu cả hai chưa
     * được liên kết.
     *
     * @param userId ID của UserAccount.
     * @param patientId ID của Patient.
     * @throws EntityNotFoundException nếu UserAccount hoặc Patient không tồn
     * tại.
     * @throws IllegalStateException nếu UserAccount hoặc Patient đã được liên
     * kết.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void linkPatientToUserAccount(UUID userId, UUID patientId) {
        log.info("Attempting to link user account {} to patient {}", userId, patientId);
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + patientId));

        // --- Validation liên kết 1-1 ---
        if (userAccount.getPatient() != null || userAccount.getDoctor() != null) {
            throw new IllegalStateException("UserAccount " + userId + " is already linked to a patient or doctor.");
        }
        if (patient.getUserAccount() != null) {
            throw new IllegalStateException("Patient " + patientId + " is already linked to UserAccount "
                    + patient.getUserAccount().getUserId());
        }

        // --- Thực hiện liên kết (dùng helper method) ---
        userAccount.setPatient(patient); // Helper này sẽ gọi lại patient.setUserAccountInternal
        // Không cần gọi userAccountRepository.save() vì entity được quản lý trong
        // transaction
        log.info("Successfully linked user account {} to patient {}", userId, patientId);
    }

    /**
     * Liên kết một UserAccount với một Doctor. Chỉ thực hiện nếu cả hai chưa
     * được liên kết.
     *
     * @param userId ID của UserAccount.
     * @param doctorId ID của Doctor.
     * @throws EntityNotFoundException nếu UserAccount hoặc Doctor không tồn
     * tại.
     * @throws IllegalStateException nếu UserAccount hoặc Doctor đã được liên
     * kết.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void linkDoctorToUserAccount(UUID userId, UUID doctorId) {
        log.info("Attempting to link user account {} to doctor {}", userId, doctorId);
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + doctorId));

        // --- Validation liên kết 1-1 ---
        if (userAccount.getPatient() != null || userAccount.getDoctor() != null) {
            throw new IllegalStateException("UserAccount " + userId + " is already linked to a patient or doctor.");
        }
        if (doctor.getUserAccount() != null) {
            throw new IllegalStateException(
                    "Doctor " + doctorId + " is already linked to UserAccount " + doctor.getUserAccount().getUserId());
        }

        // --- Thực hiện liên kết (dùng helper method) ---
        userAccount.setDoctor(doctor); // Helper này sẽ gọi lại doctor.setUserAccountInternal
        log.info("Successfully linked user account {} to doctor {}", userId, doctorId);
    }

    /**
     * Cập nhật thông tin đăng nhập cuối cùng. Thường được gọi sau khi xác thực
     * thành công.
     *
     * @param username Tên đăng nhập.
     * @param ipAddress Địa chỉ IP đăng nhập.
     */
    @Transactional(propagation = Propagation.REQUIRED) // Cần transaction để cập nhật
    public void updateUserLoginInfo(String username, String ipAddress) {
        log.debug("Updating last login info for user: {}", username);
        userAccountRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            user.setFailedLoginAttempts(0); // Reset số lần đăng nhập sai
            user.setLockoutUntil(null); // Mở khóa nếu đang bị khóa
            userAccountRepository.save(user); // Lưu thay đổi
            log.info("Updated last login info for user: {}", username);
        });
        // Không ném lỗi nếu không tìm thấy user ở đây vì có thể xảy ra race condition
    }

    /**
     * Xử lý khi đăng nhập thất bại. Tăng bộ đếm và có thể khóa tài khoản.
     *
     * @param username Tên đăng nhập đã cố gắng.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleFailedLoginAttempt(String username) {
        log.warn("Handling failed login attempt for username: {}", username);
        userAccountRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            // Logic khóa tài khoản đơn giản: khóa 15 phút sau 5 lần sai
            int maxAttempts = 5;
            long lockoutMinutes = 15;
            if (attempts >= maxAttempts) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
                log.warn("User account locked for username: {} until {}", username, user.getLockoutUntil());
            }
            userAccountRepository.save(user);
        });
    }

    // --- Các phương thức khác ---
    // getById, getAll (có phân trang), updateRole, changePassword, verifyEmail,
    // generatePasswordResetToken, etc.
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<UserAccount> findByUsername(String username) {
        return userAccountRepository.findByUsername(username);
    }

    // ... (Thêm các phương thức cần thiết khác) ...
}
