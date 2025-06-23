package com.pma.controller.patient;

import com.pma.model.entity.Prescription;
import com.pma.model.entity.PrescriptionDetail;
import com.pma.model.entity.UserAccount;
import com.pma.model.entity.Patient;
import com.pma.repository.PrescriptionRepository;
import com.pma.repository.PrescriptionDetailRepository;
import com.pma.repository.UserAccountRepository;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller cho màn hình xem danh sách đơn thuốc của bệnh nhân (patient_view_prescriptions.fxml).
 * Hiển thị danh sách đơn thuốc và chi tiết thuốc khi bệnh nhân chọn một đơn thuốc.
 */
@Component
public class PatientViewPrescriptionsController {

    // Đối tượng Logger để ghi log hoạt động của controller
    private static final Logger log = LoggerFactory.getLogger(PatientViewPrescriptionsController.class);

    @FXML private Button patientBookAppointmentButton;
    @FXML private Button patientViewPrescriptionsButton;
    @FXML private Button patientMedicalHistoryButton;
    @FXML private Button patientUpdateProfileButton;
    @FXML private Button patientReviewButton;
    @FXML private Button patientViewBillsButton;

    @FXML
    private TableView<Prescription> prescriptionsTable;
    
    @FXML
    private TableColumn<Prescription, LocalDate> prescriptionDateColumn;
    
    @FXML
    private TableColumn<Prescription, String> doctorColumn;
    
    @FXML
    private TableColumn<Prescription, String> notesColumn;
    
    @FXML
    private TableColumn<Prescription, String> statusColumn;

    @FXML
    private TableView<PrescriptionDetail> prescriptionDetailsTable;
    
    @FXML
    private TableColumn<PrescriptionDetail, String> medicineColumn;
    
    @FXML
    private TableColumn<PrescriptionDetail, Integer> quantityColumn;
    
    @FXML
    private TableColumn<PrescriptionDetail, BigDecimal> unitPriceColumn;
    
    @FXML
    private TableColumn<PrescriptionDetail, String> dosageColumn;
    
    @FXML
    private TableColumn<PrescriptionDetail, String> instructionsColumn;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionDetailRepository prescriptionDetailRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UIManager uiManager;

    private ObservableList<Prescription> prescriptionList = FXCollections.observableArrayList();
    
    private ObservableList<PrescriptionDetail> prescriptionDetailList = FXCollections.observableArrayList();

    /**
     * Khởi tạo controller, thiết lập các cột của bảng và tải danh sách đơn thuốc.
     */
    @FXML
    public void initialize() {
        log.info("Khởi tạo PatientViewPrescriptionsController");
        setupPrescriptionsTable();
        setupPrescriptionDetailsTable();
        loadPrescriptions();
    }

    /**
     * Thiết lập các cột cho bảng danh sách đơn thuốc.
     */
    private void setupPrescriptionsTable() {
        // Gán giá trị cho cột ngày kê đơn từ thuộc tính prescriptionDate
        prescriptionDateColumn.setCellValueFactory(new PropertyValueFactory<>("prescriptionDate"));
        
        // Gán giá trị cho cột bác sĩ, lấy tên từ Doctor.fullName, xử lý trường hợp null
        doctorColumn.setCellValueFactory(cellData -> {
            String doctorName = cellData.getValue().getDoctor() != null 
                ? cellData.getValue().getDoctor().getFullName() 
                : "Không xác định";
            return new javafx.beans.property.SimpleStringProperty(doctorName);
        });
        
        // Gán giá trị cho cột ghi chú từ thuộc tính notes
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        
        // Gán giá trị cho cột trạng thái từ thuộc tính status (PrescriptionStatus)
        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatus().toString()
            ));

        // Gán danh sách đơn thuốc vào bảng
        prescriptionsTable.setItems(prescriptionList);

        // Thêm listener để cập nhật bảng chi tiết khi chọn một đơn thuốc
        prescriptionsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadPrescriptionDetails(newSelection.getPrescriptionId());
                } else {
                    prescriptionDetailList.clear();
                }
            });
    }

    /**
     * Thiết lập các cột cho bảng chi tiết đơn thuốc.
     */
    private void setupPrescriptionDetailsTable() {
        // Gán giá trị cho cột tên thuốc, lấy từ Medicine.medicineName, xử lý trường hợp null
        medicineColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getMedicine() != null 
                ? cellData.getValue().getMedicine().getMedicineName() 
                : "Không xác định"
            ));
        
        // Gán giá trị cho cột số lượng từ thuộc tính quantity
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        // Gán giá trị cho cột đơn giá từ thuộc tính unitPrice
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        
        // Gán giá trị cho cột liều lượng từ thuộc tính dosage
        dosageColumn.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        
        // Gán giá trị cho cột hướng dẫn sử dụng từ thuộc tính instructions
        instructionsColumn.setCellValueFactory(new PropertyValueFactory<>("instructions"));

        // Gán danh sách chi tiết đơn thuốc vào bảng
        prescriptionDetailsTable.setItems(prescriptionDetailList);
    }

    /**
     * Tải danh sách đơn thuốc của bệnh nhân hiện tại từ cơ sở dữ liệu.
     */
    private void loadPrescriptions() {
        try {
            // Lấy thông tin xác thực từ Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("Không tìm thấy người dùng được xác thực.");
                DialogUtil.showErrorAlert("Lỗi xác thực", "Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.");
                uiManager.switchToLoginScreen();
                return;
            }

            // Lấy tên đăng nhập từ thông tin xác thực
            String username = authentication.getName();
            log.info("Tải đơn thuốc cho người dùng: {}", username);

            // Tìm UserAccount từ tên đăng nhập
            UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng cho tên đăng nhập: " + username));

            // Lấy thông tin bệnh nhân từ UserAccount
            Patient patient = userAccount.getPatient();
            if (patient == null) {
                log.error("Không tìm thấy bệnh nhân cho người dùng: {}", username);
                DialogUtil.showErrorAlert("Lỗi dữ liệu", "Không tìm thấy thông tin bệnh nhân cho tài khoản này.");
                return;
            }

            // Lấy ID bệnh nhân và truy vấn danh sách đơn thuốc
            UUID patientId = patient.getPatientId();
            List<Prescription> prescriptions = prescriptionRepository.findByPatient_PatientId(patientId);
            prescriptionList.setAll(prescriptions);

            // Hiển thị thông báo nếu không có đơn thuốc
            if (prescriptions.isEmpty()) {
                DialogUtil.showInfoAlert("Thông báo", "Bạn chưa có đơn thuốc nào.");
            }

        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách đơn thuốc cho người dùng.", e);
            DialogUtil.showExceptionDialog("Lỗi tải dữ liệu", "Không thể tải danh sách đơn thuốc.", 
                "Vui lòng thử lại sau.", e);
        }
    }

    /**
     * Tải chi tiết đơn thuốc dựa trên ID đơn thuốc.
     * @param prescriptionId ID của đơn thuốc.
     */
    private void loadPrescriptionDetails(UUID prescriptionId) {
        try {
            // Truy vấn danh sách chi tiết đơn thuốc theo prescriptionId
            List<PrescriptionDetail> details = prescriptionDetailRepository.findByPrescription_PrescriptionId(prescriptionId);
            prescriptionDetailList.setAll(details);
        } catch (Exception e) {
            log.error("Lỗi khi tải chi tiết đơn thuốc cho prescriptionId: {}", prescriptionId, e);
            DialogUtil.showExceptionDialog("Lỗi tải dữ liệu", "Không thể tải chi tiết đơn thuốc.", 
                "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    private void loadPatientBookAppointment() {
        uiManager.switchToPatientBookAppointment(null);
    }
    
    @FXML
    private void loadPatientViewPrescriptions() {
        uiManager.switchToPatientViewPrescriptions();
    }

    @FXML 
    private void loadPatientMedicalHistory() {
        uiManager.switchToPatientMedicalHistory();
    }

    @FXML
    private void loadPatientUpdateProfile() {
        uiManager.switchToPatientUpdateProfile();
    }
    @FXML 
    private void loadPatientReview() {
        uiManager.switchToPatientReview();
    }

    @FXML 
    private void loadPatientViewBills() {
        uiManager.switchToPatientViewBills();
    }
}
