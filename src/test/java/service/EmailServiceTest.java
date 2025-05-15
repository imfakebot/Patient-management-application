package service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID; // Đảm bảo Enum này tồn tại và được import

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage; // Import any()
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.pma.model.entity.Appointment;
import com.pma.model.entity.Doctor;
import com.pma.model.entity.Patient;
import com.pma.model.enums.AppointmentStatus;
import com.pma.service.EmailService;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSenderMock;

    @InjectMocks
    private EmailService emailService;

    private Patient testPatient;
    private Doctor testDoctor;
    private Appointment testAppointment;
    private final String FROM_EMAIL = "noreply@pma-system.com";
    // Định nghĩa formatter ở đây để sử dụng nhất quán
    private final DateTimeFormatter EMAIL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    // Giả sử EmailService cũng dùng hằng số này hoặc một hằng số tương tự với cùng pattern
    private static final Duration EMAIL_OTP_VALIDITY_DURATION = Duration.ofMinutes(5);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmailAddress", FROM_EMAIL);
        // Giả sử EmailService có hằng số này và bạn muốn đảm bảo test dùng cùng giá trị
        // Nếu không, bạn có thể không cần set trường này trong test mà kiểm tra trực tiếp giá trị trong text email
        ReflectionTestUtils.setField(null, EmailService.class, "EMAIL_OTP_VALIDITY_DURATION", EMAIL_OTP_VALIDITY_DURATION, null);

        testPatient = new Patient();
        testPatient.setPatientId(UUID.randomUUID());
        testPatient.setFullName("Nguyễn Văn An");
        testPatient.setEmail("an.nguyen@example.com");

        testDoctor = new Doctor();
        testDoctor.setDoctorId(UUID.randomUUID());
        testDoctor.setFullName("BS. Trần Thị Bích");

        testAppointment = new Appointment();
        testAppointment.setAppointmentId(UUID.randomUUID());
        testAppointment.setPatient(testPatient);
        testAppointment.setDoctor(testDoctor);
        testAppointment.setAppointmentDatetime(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0));
        testAppointment.setReason("Khám tổng quát");
        testAppointment.setStatus(AppointmentStatus.Scheduled);
    }

    @Test
    void sendSchedulingConfirmation_whenValidAppointment_shouldSendCorrectEmail() {
        emailService.sendSchedulingConfirmation(testAppointment);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSenderMock, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertNotNull(sentMessage.getTo());
        assertEquals(1, sentMessage.getTo().length);
        assertEquals(testPatient.getEmail(), sentMessage.getTo()[0]);
        assertEquals(FROM_EMAIL, sentMessage.getFrom());
        assertEquals("Xác nhận Lịch hẹn tại PMA", sentMessage.getSubject()); // Giữ nguyên hoặc đổi theo EmailService

        String expectedFormattedTime = testAppointment.getAppointmentDatetime().format(EMAIL_DATE_TIME_FORMATTER);
        String emailText = sentMessage.getText();
        assertNotNull(emailText);
        assertTrue(emailText.contains(testPatient.getFullName()), "Email text should contain patient's full name.");
        assertTrue(emailText.contains(testAppointment.getAppointmentId().toString()), "Email text should contain appointment ID.");
        assertTrue(emailText.contains(testDoctor.getFullName()), "Email text should contain doctor's full name.");
        assertTrue(emailText.contains(expectedFormattedTime), "Email text should contain formatted appointment time. Expected: " + expectedFormattedTime + " but got: " + emailText);
    }

    @Test
    void sendSchedulingConfirmation_whenPatientIsNull_shouldNotSendEmail() {
        // Tạo appointment với patient null
        Appointment appointmentWithNullPatient = new Appointment();
        appointmentWithNullPatient.setAppointmentId(UUID.randomUUID());
        appointmentWithNullPatient.setPatient(null); // Patient là null
        appointmentWithNullPatient.setAppointmentDatetime(LocalDateTime.now().plusDays(1));

        emailService.sendSchedulingConfirmation(appointmentWithNullPatient);
        verify(mailSenderMock, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSchedulingConfirmation_whenPatientEmailIsNull_shouldNotSendEmail() {
        testPatient.setEmail(null);
        emailService.sendSchedulingConfirmation(testAppointment);
        verify(mailSenderMock, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendCancellationNotification_whenValidAppointmentAndReason_shouldSendCorrectEmail() {
        String cancellationReason = "Bác sĩ có lịch đột xuất";
        emailService.sendCancellationNotification(testAppointment, cancellationReason);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSenderMock, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals(testPatient.getEmail(), sentMessage.getTo()[0]);
        assertEquals("Thông báo Hủy Lịch hẹn tại PMA", sentMessage.getSubject()); // Giữ nguyên hoặc đổi theo EmailService
        String emailText = sentMessage.getText();
        assertNotNull(emailText);
        assertTrue(emailText.contains(testPatient.getFullName()));
        assertTrue(emailText.contains(testAppointment.getAppointmentId().toString()));
        String expectedFormattedTime = testAppointment.getAppointmentDatetime().format(EMAIL_DATE_TIME_FORMATTER);
        assertTrue(emailText.contains(expectedFormattedTime), "Email text should contain formatted appointment time. Expected: " + expectedFormattedTime + " but got: " + emailText);
        assertTrue(emailText.contains(cancellationReason));
    }

    @Test
    void sendCancellationNotification_whenReasonIsNull_shouldUseDefaultReason() {
        emailService.sendCancellationNotification(testAppointment, null);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSenderMock, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("Không có lý do cụ thể."));
    }

    @Test
    void sendOtpEmail_whenValidRecipient_shouldSendCorrectOtpEmail() {
        String username = "testUserOtp";
        String otp = "123456";
        emailService.sendOtpEmail(testPatient.getEmail(), username, otp);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSenderMock, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals(testPatient.getEmail(), sentMessage.getTo()[0]);
        // Giả sử tiêu đề và nội dung trong EmailService.sendOtpEmail đã được sửa lại (nếu cần)
        assertEquals("Your PMA Two-Factor Authentication Code", sentMessage.getSubject()); // Hoặc tiêu đề tiếng Việt
        String emailText = sentMessage.getText();
        assertNotNull(emailText);
        assertTrue(emailText.contains(username));
        assertTrue(emailText.contains(otp));
        assertTrue(emailText.contains("expire in " + EMAIL_OTP_VALIDITY_DURATION.toMinutes() + " minutes"));
    }

    @Test
    void sendOtpEmail_whenRecipientEmailIsBlank_shouldNotSendEmail() {
        emailService.sendOtpEmail(" ", "testUserOtp", "123456");
        verify(mailSenderMock, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendMethods_whenMailSenderThrowsException_shouldCatchAndLogError() {
        // Giả lập mailSender.send() ném ra một concrete MailException (hoặc một lớp con của nó)
        // Chú ý: new MailException("...") {} là tạo một anonymous inner class, điều này có thể khác với
        // việc ném một instance của một lớp con cụ thể của MailException như SMTPSendFailedException.
        // Để đơn giản, ta vẫn dùng anonymous class, nhưng nên lưu ý sự khác biệt này.
        doThrow(new MailException("Simulated Mail Send Error") {
        }).when(mailSenderMock).send(any(SimpleMailMessage.class));

        // Kiểm tra xem EmailService có bắt exception và không để nó lan ra ngoài không
        assertDoesNotThrow(() -> emailService.sendSchedulingConfirmation(testAppointment));

        // Vẫn kiểm tra xem send() có được gọi (dù nó ném lỗi)
        verify(mailSenderMock, times(1)).send(any(SimpleMailMessage.class));
        // Việc kiểm tra log lỗi thực sự cần thư viện hỗ trợ test log.
    }
}
