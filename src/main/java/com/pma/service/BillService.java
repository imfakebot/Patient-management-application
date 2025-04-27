package com.pma.service;

import com.pma.model.entity.*; // Import các entity cần thiết
import com.pma.model.enums.BillItemType;
import com.pma.model.enums.BillPaymentStatus; // Import Enum
import com.pma.repository.*; // Import các repository cần thiết
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.Setter;

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

import java.math.BigDecimal; // Cần cho tính toán tiền
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set; // Cần cho BillItems
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BillService {

    private static final Logger log = LoggerFactory.getLogger(BillService.class);

    private final BillRepository billRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository; // Optional
    private final PrescriptionDetailRepository prescriptionDetailRepository; // Nếu cần tạo BillItem từ
                                                                             // PrescriptionDetail
    // Inject các Repository khác nếu cần tạo BillItem từ LabTest, Procedure...
    // private final LabTestRepository labTestRepository;
    // private final ProcedureRepository procedureRepository;

    @Autowired
    public BillService(BillRepository billRepository,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            PrescriptionDetailRepository prescriptionDetailRepository) {
        this.billRepository = billRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.prescriptionDetailRepository = prescriptionDetailRepository;
    }

    /**
     * Tạo một hóa đơn mới với các mục chi tiết.
     *
     * @param bill          Đối tượng Bill cơ bản (chưa có ID, Patient, Appointment,
     *                      Items).
     * @param patientId     ID của Patient.
     * @param appointmentId (Optional) ID của Appointment liên quan.
     * @param billItemDTOs  Danh sách các DTO chứa thông tin chi tiết hóa đơn.
     * @return Bill đã được lưu cùng các chi tiết.
     * @throws EntityNotFoundException  nếu Patient hoặc Appointment (nếu có) không
     *                                  tồn tại.
     * @throws IllegalArgumentException nếu thông tin chi tiết không hợp lệ.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Bill createBill(Bill bill, UUID patientId, UUID appointmentId, List<BillItemDTO> billItemDTOs) {
        log.info("Attempting to create bill for patientId: {}, appointmentId: {}", patientId, appointmentId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id: " + patientId));

        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Appointment not found with id: " + appointmentId));
            // Kiểm tra Appointment có thuộc Patient không
            if (!appointment.getPatient().getPatientId().equals(patientId)) {
                throw new IllegalArgumentException(
                        "Appointment " + appointmentId + " does not belong to patient " + patientId);
            }
        }

        if (billItemDTOs == null || billItemDTOs.isEmpty()) {
            throw new IllegalArgumentException("Bill must have at least one item.");
        }

        // --- Thiết lập Bill chính ---
        bill.setBillId(null);
        bill.setPatient(patient);
        bill.setAppointment(appointment);
        bill.setPaymentStatus(BillPaymentStatus.Pending); // Trạng thái ban đầu
        if (bill.getBillDatetime() == null) {
            bill.setBillDatetime(LocalDateTime.now());
        }

        // --- Tạo và thêm BillItems ---
        Set<BillItem> items = new HashSet<>();
        for (BillItemDTO dto : billItemDTOs) {
            if (dto.getQuantity() <= 0 || dto.getUnitPrice() == null
                    || dto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Invalid quantity or unit price for item: " + dto.getItemDescription());
            }

            BillItem item = new BillItem();
            item.setBillItemId(null);
            item.setItemDescription(dto.getItemDescription()); // Cần validation không rỗng
            item.setItemType(dto.getItemType()); // Cần validation Enum hợp lệ
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());

            // Xử lý liên kết tùy chọn (ví dụ với PrescriptionDetail)
            if (dto.getPrescriptionDetailId() != null) {
                PrescriptionDetail pd = prescriptionDetailRepository.findById(dto.getPrescriptionDetailId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "PrescriptionDetail not found with id: " + dto.getPrescriptionDetailId()));
                // Kiểm tra xem PrescriptionDetail có đúng của Patient không?
                if (!pd.getPrescription().getPatient().getPatientId().equals(patientId)) {
                    throw new IllegalArgumentException("PrescriptionDetail " + dto.getPrescriptionDetailId()
                            + " does not belong to patient " + patientId);
                }
                item.setPrescriptionDetail(pd);
            }
            // Tương tự xử lý liên kết với LabTest, Procedure nếu có DTO tương ứng

            // Dùng helper method của Bill để thêm item và tự động set ngược lại
            bill.addBillItem(item); // Giả sử helper này tự gọi item.setBillInternal(bill)

            items.add(item); // Vẫn thêm vào set tạm thời nếu cần
        }

        // Lưu Bill (và BillItems sẽ được cascade lưu theo)
        Bill savedBill = billRepository.save(bill);
        log.info("Successfully created bill with id: {}", savedBill.getBillId());

        // Tổng tiền có thể lấy qua savedBill.getTotalAmount() nhờ @Transient/@Formula

        return savedBill;
    }

    /**
     * Lấy Bill theo ID.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Bill getBillById(UUID id) {
        log.info("Fetching bill with id: {}", id);
        // Có thể dùng JOIN FETCH để tải luôn items nếu thường xuyên cần
        return billRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bill not found with id: " + id));
    }

    /**
     * Lấy danh sách hóa đơn của một bệnh nhân (có phân trang).
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Bill> getBillsByPatient(UUID patientId, Pageable pageable) {
        log.info("Fetching bills for patient id: {} with pagination: {}", patientId, pageable);
        return billRepository.findByPatient_PatientIdOrderByBillDatetimeDesc(patientId, pageable);
    }

    /**
     * Cập nhật trạng thái thanh toán của hóa đơn.
     *
     * @param billId        ID của hóa đơn.
     * @param newStatus     Trạng thái mới.
     * @param paymentMethod (Optional) Phương thức thanh toán nếu trạng thái là
     *                      Paid/Partially Paid.
     * @param paymentDate   (Optional) Ngày thanh toán, mặc định là now() nếu là
     *                      Paid/Partially Paid và không được cung cấp.
     * @return Bill đã được cập nhật.
     * @throws EntityNotFoundException nếu không tìm thấy Bill.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Bill updatePaymentStatus(UUID billId, BillPaymentStatus newStatus, String paymentMethod,
            LocalDateTime paymentDate) {
        log.info("Attempting to update payment status for bill id: {} to {}", billId, newStatus);
        Bill bill = getBillById(billId);

        // Logic cập nhật trạng thái, paymentDate, paymentMethod
        bill.setPaymentStatus(newStatus);
        if (newStatus == BillPaymentStatus.Paid || newStatus == BillPaymentStatus.Partially_Paid) {
            bill.setPaymentMethod(paymentMethod); // Cần validation paymentMethod nếu muốn
            bill.setPaymentDate(Objects.requireNonNullElseGet(paymentDate, LocalDateTime::now)); // Gán ngày hiện tại
                                                                                                 // nếu không cung cấp
        } else {
            // Nếu chuyển về Pending hoặc Cancelled, có thể xóa payment info
            bill.setPaymentMethod(null);
            bill.setPaymentDate(null);
        }

        log.info("Payment status updated successfully for bill id: {}", billId);
        return bill; // Thay đổi được lưu khi commit transaction
    }

    /**
     * Xóa một hóa đơn.
     * CẢNH BÁO: Do CascadeType.ALL cho billItems, việc này sẽ xóa tất cả chi tiết
     * hóa đơn liên quan.
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteBill(UUID billId) {
        log.warn("Attempting to DELETE bill with id: {} AND ALL ASSOCIATED ITEMS!", billId);
        if (!billRepository.existsById(billId)) {
            log.error("Deletion failed. Bill not found with id: {}", billId);
            throw new EntityNotFoundException("Bill not found with id: " + billId);
        }
        try {
            billRepository.deleteById(billId);
            log.info("Successfully deleted bill with id: {}", billId);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation during deletion of bill id: {}. Error: {}", billId, e.getMessage());
            throw new IllegalStateException(
                    "Could not delete bill with id " + billId + " due to data integrity issues.", e);
        }
    }

    // --- DTO đơn giản cho Bill Item ---
    // Nên tạo class này ở package riêng (ví dụ: com.pma.dto)
    @Getter
    @Setter
    public static class BillItemDTO {
        private String itemDescription;
        private BillItemType itemType; // Enum
        private int quantity;
        private BigDecimal unitPrice;
        private UUID prescriptionDetailId; // Optional
        // private UUID labTestId; // Optional
        // private UUID procedureId; // Optional
    }

    // --- Có thể thêm các phương thức truy vấn khác ---
    /*
     * @Transactional(readOnly = true)
     * public List<Bill> findBillsByStatusAndDueDate(BillPaymentStatus status,
     * LocalDate dueDate) { ... }
     */
}