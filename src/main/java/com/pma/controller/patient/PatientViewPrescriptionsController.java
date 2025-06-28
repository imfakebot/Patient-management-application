package com.pma.controller.patient;

import com.pma.model.entity.Prescription;
import com.pma.model.entity.PrescriptionDetail;
import com.pma.repository.PrescriptionRepository;
import com.pma.service.PrescriptionService;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller cho màn hình xem danh sách đơn thuốc của bệnh nhân
 * (patient_view_prescriptions.fxml). Hiển thị danh sách đơn thuốc và chi tiết
 * thuốc khi bệnh nhân chọn một đơn thuốc.
 *
 *
 */
@Component
public class PatientViewPrescriptionsController {

    // Đối tượng Logger để ghi log hoạt động của controller
    private static final Logger log = LoggerFactory.getLogger(PatientViewPrescriptionsController.class);

    @FXML
    private Button patientBookAppointmentButton;
    @FXML
    private Button patientViewPrescriptionsButton;
    @FXML
    private Button patientMedicalHistoryButton;
    @FXML
    private Button patientUpdateProfileButton;
    @FXML
    private Button patientReviewButton;
    @FXML
    private Button patientViewBillsButton;
    @FXML
    private Button logoutBtn;

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
    private PrescriptionService prescriptionService; // Inject PrescriptionService

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private UIManager uiManager;

    private final ObservableList<Prescription> prescriptionList = FXCollections.observableArrayList();

    private final ObservableList<PrescriptionDetail> prescriptionDetailList = FXCollections.observableArrayList();

    private UUID patientId;

    /**
     * Khởi tạo controller, thiết lập các cột của bảng và tải danh sách đơn
     * thuốc.
     */
    @FXML
    public void initialize() {
        log.info("Khởi tạo PatientViewPrescriptionsController");
        setupPrescriptionsTable();
        setupPrescriptionDetailsTable();
    }

    /**
     * Khởi tạo dữ liệu cho controller với ID của bệnh nhân.
     *
     * @param patientId ID của bệnh nhân.
     */
    public void initData(UUID patientId) {
        this.patientId = patientId;
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
        statusColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStatus().toString()
                ));

        // Gán danh sách đơn thuốc vào bảng
        prescriptionsTable.setItems(prescriptionList);

        // Thêm listener để cập nhật bảng chi tiết khi chọn một đơn thuốc
        prescriptionsTable.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newSelection) -> {
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
        medicineColumn.setCellValueFactory(cellData
                -> new javafx.beans.property.SimpleStringProperty(
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
        if (patientId == null) {
            log.error("Patient ID is null. Cannot load prescriptions.");
            DialogUtil.showErrorAlert("Lỗi Dữ liệu", "Không thể tải đơn thuốc vì thiếu thông tin bệnh nhân.");
            return;
        }
        try {
            log.info("Tải đơn thuốc cho bệnh nhân có ID: {}", patientId);
            List<Prescription> prescriptions = prescriptionRepository.findByPatient_PatientId(patientId);
            prescriptionList.setAll(prescriptions);

            // Hiển thị thông báo nếu không có đơn thuốc
            if (prescriptions.isEmpty()) {
                DialogUtil.showInfoAlert("Thông báo", "Bạn chưa có đơn thuốc nào.");
            } else {
                // Tự động chọn đơn thuốc đầu tiên để hiển thị chi tiết
                prescriptionsTable.getSelectionModel().selectFirst();
            }

        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách đơn thuốc cho patientId: {}", patientId, e);
            DialogUtil.showExceptionDialog("Lỗi tải dữ liệu", "Không thể tải danh sách đơn thuốc.",
                    "Vui lòng thử lại sau.", e);
        }
    }

    /**
     * Tải chi tiết đơn thuốc dựa trên ID đơn thuốc.
     *
     * @param prescriptionId ID của đơn thuốc.
     */
    private void loadPrescriptionDetails(UUID prescriptionId) {
        try {
            // Sử dụng PrescriptionService để lấy toàn bộ thông tin đơn thuốc, bao gồm cả chi tiết
            Optional<Prescription> prescriptionOptional = prescriptionService.getPrescriptionByIdWithDetails(prescriptionId);
            if (prescriptionOptional.isPresent()) {
                Prescription prescription = prescriptionOptional.get();
                prescriptionDetailList.setAll(prescription.getPrescriptionDetailsView());
            } else {
                log.warn("Prescription not found with ID: {}", prescriptionId);
                prescriptionDetailList.clear(); // Clear details if prescription not found
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải chi tiết đơn thuốc cho prescriptionId: {}", prescriptionId, e);
            DialogUtil.showExceptionDialog("Lỗi tải dữ liệu", "Không thể tải chi tiết đơn thuốc.",
                    "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    private void loadPatientBookAppointment() {
        if (this.patientId == null) {
            log.warn("Cannot switch to Book Appointment screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientBookAppointment(this.patientId);
    }

    @FXML
    private void loadPatientViewPrescriptions() {
        if (this.patientId == null) {
            log.warn("Cannot switch to View Prescriptions screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientViewPrescriptions(this.patientId);
    }

    @FXML
    private void loadPatientMedicalHistory() {
        if (this.patientId == null) {
            log.warn("Cannot switch to Medical History screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientMedicalHistory(this.patientId);
    }

    @FXML
    private void loadPatientUpdateProfile() {
        if (this.patientId == null) {
            log.warn("Cannot switch to Update Profile screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientUpdateProfile(this.patientId);
    }

    @FXML
    private void loadPatientViewBills() {
        if (this.patientId == null) {
            log.warn("Cannot switch to View Bills screen because patientId is null.");
            DialogUtil.showErrorAlert("Lỗi", "Không thể lấy thông tin bệnh nhân để điều hướng.");
            return;
        }
        uiManager.switchToPatientViewBills(this.patientId);
    }

    @FXML
    private void logout() {
        uiManager.switchToLoginScreen();
    }
}
