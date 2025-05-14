package com.pma.service;

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
            message.setText(String.format(
                    "Dear %s,\n\n"
                    + "Your appointment has been successfully scheduled.\n\n"
                    + "Appointment Details:\n"
                    + "- Appointment ID: %s\n"
                    + "- Doctor: %s\n"
                    + "- Date & Time: %s\n\n"
                    + "Please arrive on time.\n\n"
                    + "Best regards,\n"
                    + "PMA System",
                    patientName,
                    appointment.getAppointmentId(),
                    doctorName,
                    dateTime.format(EMAIL_DATE_TIME_FORMATTER)
            ));

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

            message.setText(String.format(
                    "Dear %s,\n\n"
                    + "We regret to inform you that your appointment has been cancelled.\n\n"
                    + "Appointment Details:\n"
                    + "- Appointment ID: %s\n"
                    + "- Doctor: %s\n"
                    + "- Scheduled Time: %s\n"
                    + "- Cancellation Reason: %s\n\n"
                    + "Please contact us if you would like to reschedule.\n\n"
                    + "Best regards,\n"
                    + "PMA System",
                    patientName,
                    appointment.getAppointmentId(),
                    doctorName,
                    dateTime
                            .format(EMAIL_DATE_TIME_FORMATTER),
                    cancellationReason
            ));

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendOtpEmail(String recipientEmail, String username,
            String otp
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send OTP email. Invalid recipient email for user: {}", username);
            return;
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

        } catch (MailException e) {
            log.error("Failed to send OTP email to user: {} at {}: {}",
                    username, recipientEmail, e.getMessage());
        }
    }
}
