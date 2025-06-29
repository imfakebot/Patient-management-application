package com.pma.service;

import java.util.concurrent.CompletableFuture;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

import com.pma.model.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import java.util.concurrent.Future;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service xử lý gửi email trong hệ thống. Cung cấp các phương thức gửi email
 * thông báo và xác thực.
 */
@Service
public class EmailService {

    /**
     * Thời gian hiệu lực của mã OTP gửi qua email (5 phút)
     */
    private static final Duration EMAIL_OTP_VALIDITY_DURATION = Duration.ofMinutes(5);

    /**
     * Định dạng ngày giờ cho nội dung email
     */
    private static final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER
            = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Email gửi đi của hệ thống, được cấu hình trong application.properties
     */
    @Value("${spring.mail.username}")
    private String fromEmailAddress;

    /**
     * Gửi email xác nhận đặt lịch hẹn thành công.
     *
     * @param appointment Thông tin lịch hẹn cần gửi xác nhận
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void sendSchedulingConfirmation(Appointment appointment) {
        if (appointment == null || appointment.getPatient() == null || appointment.getPatient().getEmail() == null) {
            log.warn(
                    "Cannot send scheduling confirmation. Invalid appointment or patient email is null for appointment id: {}",
                    appointment != null ? appointment.getAppointmentId() : "null");
            return;
        }

        try {
            String patientEmail = appointment.getPatient().getEmail();
            String patientName = appointment.getPatient().getFullName();
            String doctorName = (appointment.getDoctor() != null) ? appointment.getDoctor().getFullName() : "N/A";
            LocalDateTime dateTime = appointment.getAppointmentDatetime();

            log.info("Preparing scheduling confirmation email for appointment id: {} to {}",
                    appointment.getAppointmentId(), patientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(patientEmail);
            message.setSubject("Appointment Confirmation - PMA System");
            message.setText(String.format("""
                                          Dear %s,

                                          Your appointment has been successfully scheduled.

                                          Appointment Details:
                                          - Appointment ID: %s
                                          - Doctor: %s
                                          - Date & Time: %s

                                          Please arrive on time.

                                          Best regards,
                                          PMA System""", patientName, appointment.getAppointmentId(), doctorName,
                    dateTime.format(EMAIL_DATE_TIME_FORMATTER)));

            mailSender.send(message);
            log.info("Successfully sent scheduling confirmation email for appointment id: {}",
                    appointment.getAppointmentId());

        } catch (MailException e) {
            log.error("Error sending scheduling confirmation email for appointment id: {}",
                    appointment.getAppointmentId(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending scheduling confirmation email for appointment id: {}",
                    appointment.getAppointmentId(), e);
        }
    }

    /**
     * Gửi email thông báo hủy lịch hẹn.
     *
     * @param appointment Thông tin lịch hẹn bị hủy
     * @param reason Lý do hủy lịch hẹn (có thể null)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void sendCancellationNotification(Appointment appointment, String reason) {
        if (appointment == null || appointment.getPatient() == null || appointment.getPatient().getEmail() == null) {
            log.warn(
                    "Cannot send cancellation notification. Invalid appointment or patient email is null for appointment id: {}",
                    appointment != null ? appointment.getAppointmentId() : "null");
            return;

        }

        try {

            String patientEmail = appointment.getPatient().getEmail();

            String patientName = appointment.getPatient().getFullName();

            String doctorName = (appointment.getDoctor() != null) ? appointment.getDoctor().getFullName() : "N/A";

            LocalDateTime dateTime = appointment.getAppointmentDatetime();

            String cancellationReason = (reason != null && !reason.isBlank()) ? reason : "Không có lý do cụ thể.";

            log.info("Preparing cancellation notification email for appointment id: {} to {}",
                    appointment.getAppointmentId(), patientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(patientEmail);

            message.setSubject("Appointment Cancellation Notice - PMA System");
            message.setText(String.format("""
                                          Dear %s,

                                          We regret to inform you that your appointment has been cancelled.

                                          Appointment Details:
                                          - Appointment ID: %s
                                          - Doctor: %s
                                          - Scheduled Time: %s
                                          - Cancellation Reason: %s

                                          Please contact us if you would like to reschedule.

                                          Best regards,
                                          PMA System""", patientName, appointment.getAppointmentId(), doctorName,
                    dateTime.format(EMAIL_DATE_TIME_FORMATTER), cancellationReason));

            mailSender.send(message);

            log.info("Successfully sent cancellation notification email for appointment id: {}",
                    appointment.getAppointmentId()
            );

        } catch (MailException e) {
            log.error("Error sending cancellation notification email for appointment id: {}",
                    appointment.getAppointmentId(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending cancellation notification email for appointment id: {}",
                    appointment.getAppointmentId(), e);
        }
    }

    /**
     * Gửi mã OTP qua email cho xác thực hai yếu tố. Email chứa mã OTP và cảnh
     * báo bảo mật.
     *
     * @param recipientEmail Email người nhận
     * @param username Tên người dùng để hiển thị trong email
     * @param otp Mã OTP cần gửi
     */
    @Async
    public Future<Void> sendOtpEmail(String recipientEmail, String username,
            String otp
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send OTP email. Invalid recipient email for user: {}", username);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.info("Preparing OTP email for user: {} to {}", username, recipientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(recipientEmail);
            message.setSubject("Two-Factor Authentication Code - PMA System");
            message.setText(String.format("""
                                          Dear %s,
                                          
                                          Your authentication code is:
                                          
                                          %s
                                          
                                          This code will expire in %d minutes.
                                          
                                          \u26a0\ufe0f Security Notice:
                                          - Never share this code with anyone
                                          - PMA will never ask for this code via phone or email
                                          - If you didn't request this code, please secure your account immediately
                                          
                                          Best regards,
                                          PMA System""",
                    username,
                    otp,
                    EMAIL_OTP_VALIDITY_DURATION.toMinutes()
            ));

            mailSender.send(message);
            log.info("Successfully sent OTP email to user: {}", username);
            return CompletableFuture.completedFuture(null);
        } catch (MailException e) {
            log.error("Failed to send OTP email to user: {} at {}: {}",
                    username, recipientEmail, e.getMessage());
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(
                    new RuntimeException("Failed to send OTP email to " + recipientEmail
                            + ". Please check mail server configuration or network.", e));
            return future;
        }
    }

    @Async
    public Future<Void> sendPasswordResetEmail(String recipientEmail, String username, String resetToken) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send password reset email. Invalid recipient email for user: {}", username);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.info("Preparing password reset email for user: {} to {}", username, recipientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(recipientEmail);
            message.setSubject("Password Reset Request - PMA System");
            message.setText(String.format("""
                                          Dear %s,

                                          A password reset was requested for your account.
                                          
                                          Please use the following token along with your username on the 'Reset Password with Token' screen in the application:
                                          %s
                                          
                                          This token will expire in %d hour(s).
                                          
                                          If you did not request a password reset, please ignore this email or contact support if you have concerns.
                                          
                                          Best regards,
                                          PMA System""",
                    username,
                    resetToken,
                    UserAccountService.PASSWORD_RESET_TOKEN_VALIDITY_DURATION.toHours()
            ));

            mailSender.send(message);
            log.info("Successfully sent password reset email to user: {}", username);
            return CompletableFuture.completedFuture(null);
        } catch (MailException e) {
            log.error("Failed to send password reset email to user: {} at {}: {}",
                    username, recipientEmail, e.getMessage());
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(
                    new RuntimeException("Failed to send password reset email to " + recipientEmail
                            + ". Please check mail server configuration or network.", e));
            return future;
        }
    }

    /**
     * Gửi email thông báo thông tin tài khoản mới cho người dùng (ví dụ: bệnh
     * nhân). Email này chứa tên đăng nhập và mật khẩu được tạo tự động.
     *
     * @param recipientEmail Email của người nhận.
     * @param recipientName Tên của người nhận (để cá nhân hóa email).
     * @param username Tên đăng nhập mới.
     * @param rawPassword Mật khẩu mới (chưa băm).
     * @return Future<Void> để theo dõi việc gửi email bất đồng bộ.
     */
    @Async
    public Future<Void> sendNewAccountCredentials(String recipientEmail, String recipientName, String username, String temporaryPassword) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send new account credentials. Invalid recipient email for user: {}", username);
            return CompletableFuture.completedFuture(null);
        }
        if (temporaryPassword == null || temporaryPassword.isBlank()) {
            log.warn("Cannot send new account credentials. Temporary password is blank for user: {}", username);
            // Không nên gửi email nếu không có mật khẩu
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.info("Preparing new account credentials email for user: {} to {}", username, recipientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(recipientEmail);
            message.setSubject("Thông tin tài khoản của bạn tại Hệ thống PMA");
            message.setText(String.format("""
                                          Chào %s,
                                          
                                          Tài khoản của bạn tại Hệ thống Quản lý (PMA) đã được tạo.
                                          Dưới đây là thông tin đăng nhập của bạn:
                                          
                                          Tên đăng nhập: %s
                                          Mật khẩu: %s
                                          (Vui lòng đổi mật khẩu này ngay sau khi đăng nhập lần đầu tiên)
                                          \u26a0\ufe0f Quan trọng:
                                          - Vì lý do bảo mật, vui lòng đổi mật khẩu ngay sau lần đăng nhập đầu tiên.
                                          - Không chia sẻ thông tin tài khoản này với bất kỳ ai.
                                          
                                          Trân trọng,
                                          Đội ngũ Hệ thống PMA""",
                    recipientName,
                    username, // Username
                    temporaryPassword // Temporary password
            ));

            mailSender.send(message);
            log.info("Successfully sent new account credentials email to user: {}", username);
            return CompletableFuture.completedFuture(null);
        } catch (MailException e) {
            log.error("Failed to send new account credentials email to user: {} at {}: {}", username, recipientEmail, e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Failed to send new account credentials email to " + recipientEmail, e));
            return future;
        }
    }

    /**
     * Gửi email thông báo thông tin tài khoản mới cho bác sĩ. Email này chứa
     * tên đăng nhập và mật khẩu được tạo tự động.
     *
     * @param recipientEmail Email của bác sĩ.
     * @param recipientName Tên của bác sĩ (để cá nhân hóa email).
     * @param username Tên đăng nhập mới.
     * @param rawPassword Mật khẩu mới (chưa băm).
     * @return Future<Void> để theo dõi việc gửi email bất đồng bộ.
     */
    @Async
    public Future<Void> sendNewDoctorAccountCredentials(String recipientEmail, String recipientName, String username, String temporaryPassword) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send new doctor account credentials. Invalid recipient email for doctor: {}", username);
            return CompletableFuture.completedFuture(null);
        }
        if (temporaryPassword == null || temporaryPassword.isBlank()) {
            log.warn("Cannot send new doctor account credentials. Temporary password is blank for doctor: {}", username);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.info("Preparing new account credentials email for doctor: {} to {}", username, recipientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(recipientEmail);
            message.setSubject("Thông tin tài khoản Bác sĩ của bạn tại Hệ thống PMA");
            message.setText(String.format("""
                                          Chào Bác sĩ %s,
                                          
                                          Tài khoản bác sĩ của bạn tại Hệ thống Quản lý (PMA) đã được tạo.
                                          Dưới đây là thông tin đăng nhập của bạn:
                                          
                                          Tên đăng nhập: %s
                                          Mật khẩu: %s
                                          (Vui lòng đổi mật khẩu này ngay sau khi đăng nhập lần đầu tiên)
                                          \u26a0\ufe0f Quan trọng:
                                          - Vì lý do bảo mật, vui lòng đổi mật khẩu ngay sau lần đăng nhập đầu tiên.
                                          - Không chia sẻ thông tin tài khoản này với bất kỳ ai.
                                          
                                          Trân trọng,
                                          Đội ngũ Hệ thống PMA""",
                    recipientName, // Sử dụng tên bác sĩ
                    username, // Username
                    temporaryPassword // Temporary password
            ));

            mailSender.send(message);
            log.info("Successfully sent new account credentials email to doctor: {}", username);
            return CompletableFuture.completedFuture(null);
        } catch (MailException e) {
            log.error("Failed to send new account credentials email to doctor: {} at {}: {}", username, recipientEmail, e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Failed to send new account credentials email to " + recipientEmail, e));
            return future;
        }
    }

    /**
     * Gửi email thông báo tài khoản đã bị xóa.
     *
     * @param recipientEmail Email của người nhận.
     * @param recipientName Tên của người nhận.
     * @param accountType Loại tài khoản (ví dụ: "Bệnh nhân", "Bác sĩ").
     * @param adminUsername Tên của quản trị viên đã thực hiện hành động xóa (có
     * thể là "Quản trị viên hệ thống" nếu không có thông tin cụ thể).
     * @return Future<Void> để theo dõi việc gửi email bất đồng bộ.
     */
    @Async
    public Future<Void> sendAccountDeletionNotification(String recipientEmail, String recipientName, String accountType, String adminUsername) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send account deletion notification. Invalid recipient email for {} formerly known as {}.", accountType, recipientName);
            return CompletableFuture.completedFuture(null);
        }

        String effectiveAdminName = (adminUsername != null && !adminUsername.isBlank()) ? adminUsername : "Quản trị viên hệ thống";

        try {
            log.info("Preparing account deletion notification email for {} {} to {}", accountType, recipientName, recipientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(recipientEmail);
            message.setSubject("Thông báo Xóa Tài khoản - Hệ thống PMA");
            message.setText(String.format("""
                                          Chào %s,
                                          
                                          Chúng tôi xin thông báo tài khoản %s của bạn tại Hệ thống Quản lý (PMA) đã bị xóa.
                                          
                                          Tài khoản của bạn đã được %s xóa khỏi hệ thống.
                                          
                                          Nếu bạn có bất kỳ thắc mắc nào hoặc cho rằng đây là một sự nhầm lẫn, vui lòng liên hệ với bộ phận hỗ trợ của chúng tôi.
                                          
                                          Trân trọng,
                                          Đội ngũ Hệ thống PMA""",
                    recipientName,
                    accountType.toLowerCase(), // "bệnh nhân" hoặc "bác sĩ"
                    effectiveAdminName
            ));

            mailSender.send(message);
            log.info("Successfully sent account deletion notification email to {} for former {} {}", recipientEmail, accountType, recipientName);
            return CompletableFuture.completedFuture(null);
        } catch (MailException e) {
            log.error("Failed to send account deletion notification email to {} for former {} {}: {}", recipientEmail, accountType, recipientName, e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Failed to send account deletion notification email to " + recipientEmail, e));
            return future;
        }
    }
}
