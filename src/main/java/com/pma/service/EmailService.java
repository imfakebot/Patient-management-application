package com.pma.service;

import java.time.Duration;

import com.pma.model.entity.Appointment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async; // Quan trọng
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EmailService {

    private static final Duration EMAIL_OTP_VALIDITY_DURATION = Duration.ofMinutes(5); // Define the duration

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender; // Bean được Spring Boot tự cấu hình

    // Lấy địa chỉ email người gửi từ application.properties (tùy chọn)
    @Value("${spring.mail.username}") // Hoặc mail.from.address nếu bạn định nghĩa riêng
    private String fromEmailAddress;

    /**
     * Gửi email xác nhận đặt lịch hẹn thành công. Chạy bất đồng bộ (@Async).
     *
     * @param appointment Đối tượng Appointment đã được lưu.
     */
    @Async // Chạy phương thức này trên một luồng khác
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true) // Mở transaction mới để đọc entity nếu cần
    public void sendSchedulingConfirmation(Appointment appointment) {
        if (appointment == null || appointment.getPatient() == null || appointment.getPatient().getEmail() == null) {
            log.warn(
                    "Cannot send scheduling confirmation. Invalid appointment or patient email is null for appointment id: {}",
                    appointment != null ? appointment.getAppointmentId() : "null");
            return;
        }

        try {
            // --- QUAN TRỌNG: Xử lý Lazy Loading ---
            // Do chạy Async, session gốc có thể đã đóng.
            // Cách 1: Truy cập các trường cần thiết TRƯỚC KHI gọi hàm này từ
            // AppointmentService.
            // Cách 2 (An toàn hơn): Lấy lại thông tin cần thiết bên trong hàm này.
            // (Ví dụ dưới đây giả sử appointment được truyền vào vẫn có thể truy cập được
            // do được gọi ngay sau save và transaction chưa commit hoàn toàn,
            // nhưng cách an toàn nhất là truyền ID và fetch lại.)

            String patientEmail = appointment.getPatient().getEmail();
            String patientName = appointment.getPatient().getFullName();
            String doctorName = (appointment.getDoctor() != null) ? appointment.getDoctor().getFullName() : "N/A";
            LocalDateTime dateTime = appointment.getAppointmentDatetime();

            log.info("Preparing scheduling confirmation email for appointment id: {} to {}",
                    appointment.getAppointmentId(), patientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(patientEmail);
            // message.setCc(doctorEmail); // Có thể CC cho bác sĩ nếu cần và có email
            message.setSubject("Xác nhận Lịch hẹn tại PMA");
            message.setText(String.format(
                    "Chào %s,\n\nLịch hẹn của bạn đã được đặt thành công.\n\nThông tin chi tiết:\n"
                    + "- Mã lịch hẹn: %s\n"
                    + "- Bác sĩ: %s\n"
                    + "- Thời gian: %s\n\n"
                    + "Vui lòng đến đúng giờ. Xin cảm ơn!\n\nPMA System",
                    patientName,
                    appointment.getAppointmentId(),
                    doctorName,
                    dateTime.toString() // Định dạng lại nếu cần
            ));

            mailSender.send(message);
            log.info("Successfully sent scheduling confirmation email for appointment id: {}",
                    appointment.getAppointmentId());

        } catch (MailException e) {
            log.error("Error sending scheduling confirmation email for appointment id: {}",
                    appointment.getAppointmentId(), e);
            // Có thể thêm logic retry hoặc thông báo lỗi vào hệ thống
        } catch (Exception e) {
            // Bắt các lỗi khác, ví dụ LazyInitializationException nếu xảy ra
            log.error("Unexpected error sending scheduling confirmation email for appointment id: {}",
                    appointment.getAppointmentId(), e);
        }
    }

    /**
     * Gửi email thông báo hủy lịch hẹn. Chạy bất đồng bộ (@Async).
     *
     * @param appointment Đối tượng Appointment đã bị hủy.
     * @param reason Lý do hủy (lấy từ updateNote trong AppointmentService).
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
            // Tương tự như trên, cần đảm bảo truy cập được dữ liệu
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
            // message.setCc(doctorEmail); // Thông báo cho bác sĩ?
            message.setSubject("Thông báo Hủy Lịch hẹn tại PMA");
            message.setText(String.format(
                    "Chào %s,\n\nChúng tôi rất tiếc phải thông báo lịch hẹn của bạn đã bị hủy.\n\nThông tin chi tiết:\n"
                    + "- Mã lịch hẹn: %s\n"
                    + "- Bác sĩ: %s\n"
                    + "- Thời gian đã đặt: %s\n"
                    + "- Lý do hủy: %s\n\n"
                    + "Vui lòng liên hệ với chúng tôi nếu bạn muốn đặt lại lịch hẹn khác. Xin cảm ơn!\n\nPMA System",
                    patientName,
                    appointment.getAppointmentId(),
                    doctorName,
                    dateTime.toString(), // Định dạng lại nếu cần
                    cancellationReason));

            mailSender.send(message);
            log.info("Successfully sent cancellation notification email for appointment id: {}",
                    appointment.getAppointmentId());

        } catch (MailException e) {
            log.error("Error sending cancellation notification email for appointment id: {}",
                    appointment.getAppointmentId(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending cancellation notification email for appointment id: {}",
                    appointment.getAppointmentId(), e);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Keep transaction management
    public void sendOtpEmail(String recipientEmail, String username, String otp) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send OTP email. Invalid recipient email for user: {}", username);
            return;
        }

        try {
            log.info("Preparing OTP email for user: {} to {}", username, recipientEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(recipientEmail);
            message.setSubject("Mã xác thực hai yếu tố - PMA System");
            message.setText(String.format(
                    "Chào %s,\n\n"
                    + "Mã xác thực của bạn là:\n\n"
                    + "%s\n\n"
                    + "Mã này sẽ hết hạn sau %d phút.\n\n"
                    + "⚠️ Lưu ý an toàn:\n"
                    + "- Không chia sẻ mã này với bất kỳ ai\n"
                    + "- PMA không bao giờ yêu cầu mã này qua điện thoại hoặc email\n"
                    + "- Nếu bạn không yêu cầu mã này, vui lòng bỏ qua và bảo mật tài khoản ngay\n\n"
                    + "Trân trọng,\n"
                    + "PMA System",
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
