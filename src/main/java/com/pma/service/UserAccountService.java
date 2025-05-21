package com.pma.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pma.model.entity.Doctor;
import com.pma.model.entity.Patient;
import com.pma.model.entity.UserAccount;
import com.pma.repository.DoctorRepository;
import com.pma.repository.PatientRepository;
import com.pma.repository.UserAccountRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

import jakarta.persistence.EntityNotFoundException;

/**
 * Lớp Service cho việc quản lý UserAccount và tích hợp với Spring Security.
 * Implement UserDetailsService để cung cấp thông tin user cho quá trình xác
 * thực.
 */
@Service
public class UserAccountService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserAccountRepository userAccountRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final Duration EMAIL_OTP_VALIDITY_DURATION = Duration.ofMinutes(5);
    private static final int EMAIL_OTP_LENGTH = 6;

    @Autowired
    public UserAccountService(UserAccountRepository userAccountRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            @Lazy PasswordEncoder passwordEncoder,
            @Lazy EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Attempting to load user by username: {}", username);

        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Username not found: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        log.info("User found: {}. Loading details...", username);

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userAccount.getRole().name());

        return new User(userAccount.getUsername(),
                userAccount.getPasswordHash(),
                userAccount.isActive(),
                true,
                true,
                userAccount.getLockoutUntil() == null || userAccount.getLockoutUntil().isBefore(LocalDateTime.now()),
                Collections.singletonList(authority));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public UserAccount createUserAccount(UserAccount userAccount) {
        log.info("Attempting to create user account for username: {}", userAccount.getUsername());

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

        userAccount.setUserId(null);
        userAccount.setPasswordHash(passwordEncoder.encode(userAccount.getPasswordHash()));
        userAccount.setActive(true);

        UserAccount savedUser = userAccountRepository.save(userAccount);
        log.info("Successfully created user account with id: {} for username: {}", savedUser.getUserId(),
                savedUser.getUsername());
        return savedUser;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void linkPatientToUserAccount(UUID userId, UUID patientId) {
        log.info("Attempting to link user account {} to patient {}", userId, patientId);
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + patientId));

        if (userAccount.getPatient() != null || userAccount.getDoctor() != null) {
            throw new IllegalStateException("UserAccount " + userId + " is already linked to a patient or doctor.");
        }
        if (patient.getUserAccount() != null) {
            throw new IllegalStateException("Patient " + patientId + " is already linked to UserAccount "
                    + patient.getUserAccount().getUserId());
        }

        userAccount.setPatient(patient);
        log.info("Successfully linked user account {} to patient {}", userId, patientId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void linkDoctorToUserAccount(UUID userId, UUID doctorId) {
        log.info("Attempting to link user account {} to doctor {}", userId, doctorId);
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found with id: " + doctorId));

        if (userAccount.getPatient() != null || userAccount.getDoctor() != null) {
            throw new IllegalStateException("UserAccount " + userId + " is already linked to a patient or doctor.");
        }
        if (doctor.getUserAccount() != null) {
            throw new IllegalStateException(
                    "Doctor " + doctorId + " is already linked to UserAccount " + doctor.getUserAccount().getUserId());
        }

        userAccount.setDoctor(doctor);
        log.info("Successfully linked user account {} to doctor {}", userId, doctorId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateUserLoginInfo(String username, String ipAddress) {
        log.debug("Updating last login info for user: {}", username);
        userAccountRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userAccountRepository.save(user);
            log.info("Updated last login info for user: {}", username);
        });
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void handleFailedLoginAttempt(String username) {
        log.warn("Handling failed login attempt for username: {}", username);
        userAccountRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            int maxAttempts = 5;
            long lockoutMinutes = 15;
            if (attempts >= maxAttempts) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
                log.warn("User account locked for username: {} until {}", username, user.getLockoutUntil());
            }
            userAccountRepository.save(user);
        });
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<UserAccount> findByUsername(String username) {
        return userAccountRepository.findByUsername(username);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public String enableTotpTwoFactor(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();

        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);
        userAccountRepository.save(user);

        log.info("TOTP 2FA enabled for user: {}", user.getUsername());
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL("PMA", user.getUsername(), key);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void disableTwoFactor(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId));

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setEmailOtpHash(null); // Sử dụng tên trường mới
        user.setEmailOtpExpiresAt(null); // Sử dụng tên trường mới
        userAccountRepository.save(user);

        log.info("2FA disabled for user: {}", user.getUsername());
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void generateAndSendEmailOtp(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId));

        if (user.getPatient() == null || user.getPatient().getEmail() == null) {
            throw new IllegalStateException("User does not have an email address configured for 2FA.");
        }

        String otp = new Random().ints(0, 10)
                .limit(EMAIL_OTP_LENGTH)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
        String hashedOtp = passwordEncoder.encode(otp); // Băm mã OTP

        user.setEmailOtpHash(hashedOtp); // Lưu mã OTP đã băm
        user.setEmailOtpExpiresAt(LocalDateTime.now().plus(EMAIL_OTP_VALIDITY_DURATION)); // Lưu thời gian hết hạn
        userAccountRepository.save(user);

        String recipientEmail = user.getPatient().getEmail();
        emailService.sendOtpEmail(recipientEmail, user.getUsername(), otp);

        log.info("Email OTP sent to user: {}", user.getUsername());
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public boolean verifyTwoFactorCode(UUID userId, String code) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId));
        if (!user.isTwoFactorEnabled()) {
            log.warn("2FA is not enabled for user: {}", user.getUsername());
            return false;
        }

        if (user.getTwoFactorSecret() != null) {
            try {
                GoogleAuthenticator gAuth = new GoogleAuthenticator();
                boolean isValid = gAuth.authorize(user.getTwoFactorSecret(), Integer.parseInt(code));
                if (isValid) {
                    log.info("TOTP verification successful for user: {}", user.getUsername());
                    clearEmailOtp(user);
                    return true;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid TOTP format: {}", code);
            }
        }

        if (user.getEmailOtpHash() != null && user.getEmailOtpExpiresAt() != null) { // Kiểm tra hash và thời gian hết hạn
            if (LocalDateTime.now().isAfter(user.getEmailOtpExpiresAt())) {
                log.warn("Email OTP expired for user: {}", user.getUsername());
                clearEmailOtp(user);
                return false;
            }
            // Xác minh mã người dùng nhập (code) với mã đã băm (user.getEmailOtpHash())
            if (passwordEncoder.matches(code, user.getEmailOtpHash())) {
                log.info("Email OTP (hashed) verification successful for user: {}", user.getUsername());
                clearEmailOtp(user);
                return true;
            }
        }

        log.warn("2FA verification failed for user: {}", user.getUsername());
        return false;
    }

    private void clearEmailOtp(UserAccount user) {
        user.setEmailOtpHash(null); // Sử dụng tên trường mới
        user.setEmailOtpExpiresAt(null); // Sử dụng tên trường mới
        userAccountRepository.save(user);
    }

    /**
     * Lớp lồng nhau tĩnh (static nested class) để chứa secret key và dữ liệu
     * URL cho mã QR khi thiết lập 2FA.
     */
    public static class TwoFactorSecretAndQrData {

        private final String secret;
        private final String qrCodeData;

        /**
         * Khởi tạo đối tượng chứa secret và dữ liệu QR.
         *
         * @param secret Secret key được tạo cho TOTP.
         * @param qrCodeData Chuỗi URL (thường là otpauth://...) để tạo mã QR.
         */
        public TwoFactorSecretAndQrData(String secret, String qrCodeData) {
            this.secret = secret;
            this.qrCodeData = qrCodeData;
        }

        public String getSecret() {
            return secret;
        }

        public String getQrCodeData() {
            return qrCodeData;
        }
    }

    @Transactional(readOnly = true) // Chỉ đọc user, chưa lưu secret
    public TwoFactorSecretAndQrData generateNewTwoFactorSecretAndQrData(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId + " for 2FA setup."));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        // Tạo một secret key mới và các mã scratch (mã dự phòng)
        // Bạn có thể chọn lưu các mã scratch này nếu muốn cung cấp cho người dùng
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();

        // Tạo URL cho QR code
        // "PMA_System" là tên nhà phát hành (issuer), user.getUsername() là tên tài khoản
        // Bạn có thể tùy chỉnh tên nhà phát hành
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL("PMA_System", user.getUsername(), key);

        // Quan trọng: KHÔNG lưu secret vào UserAccount ở bước này.
        // Secret chỉ nên được lưu sau khi người dùng xác nhận thành công mã OTP đầu tiên
        // trong quá trình thiết lập (thông qua phương thức verifyAndEnableTwoFactor).
        log.info("Generated new 2FA secret and QR data for user: {}", user.getUsername());
        return new TwoFactorSecretAndQrData(secret, qrCodeUrl);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public boolean verifyAndEnableTwoFactor(UUID userId, String secretToVerifyAndSave, String otpCode) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("UserAccount not found with id: " + userId + " for 2FA verification."));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        try {
            // Chuyển đổi OTP từ String sang int
            int otp = Integer.parseInt(otpCode);
            boolean isValid = gAuth.authorize(secretToVerifyAndSave, otp);

            if (isValid) {
                user.setTwoFactorSecret(secretToVerifyAndSave); // Lưu secret key
                user.setTwoFactorEnabled(true);          // Kích hoạt 2FA
                userAccountRepository.save(user);
                log.info("TOTP 2FA successfully verified and enabled for user: {}", user.getUsername());
                clearEmailOtp(user); // Xóa thông tin OTP email cũ nếu có
                return true;
            } else {
                log.warn("Invalid TOTP code during setup for user: {}", user.getUsername());
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid OTP format: {} for user: {}. OTP must be a number.", otpCode, user.getUsername());
            return false;
        }
    }
}
