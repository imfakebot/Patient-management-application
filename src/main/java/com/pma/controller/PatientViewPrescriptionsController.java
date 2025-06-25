// package com.pma.controller;

// import java.time.LocalDate;
// import java.util.UUID;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import com.pma.model.entity.Prescription;
// import com.pma.model.entity.PrescriptionDetail;
// import com.pma.model.enums.PrescriptionStatus;
// import com.pma.service.PrescriptionService;

// import javafx.beans.property.SimpleIntegerProperty;
// import javafx.beans.property.SimpleObjectProperty;
// import javafx.beans.property.SimpleStringProperty;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.event.ActionEvent;
// import javafx.fxml.FXML;
// import javafx.scene.control.Button;
// import javafx.scene.control.TableColumn;
// import javafx.scene.control.TableView;

// @Controller
// public class PatientViewPrescriptionsController {

//     private static final Logger log = LoggerFactory.getLogger(PatientViewPrescriptionsController.class);

//     // Các fx:id từ file FXML
//     @FXML
//     private TableView<Prescription> prescriptionTable;
//     @FXML
//     private TableColumn<Prescription, LocalDate> prescriptionDateColumn;
//     @FXML
//     private TableColumn<Prescription, String> doctorColumn;
//     @FXML
//     private TableColumn<Prescription, String> notesColumn;
//     @FXML
//     private TableColumn<Prescription, PrescriptionStatus> statusColumn;
//     @FXML
//     private TableView<PrescriptionDetail> prescriptionDetailTable;
//     @FXML
//     private TableColumn<PrescriptionDetail, String> medicineColumn;
//     @FXML
//     private TableColumn<PrescriptionDetail, Integer> quantityColumn;
//     @FXML
//     private TableColumn<PrescriptionDetail, String> dosageColumn;
//     @FXML
//     private TableColumn<PrescriptionDetail, String> instructionsColumn;
//     @FXML
//     private Button patientBookAppointmentButton;
//     @FXML
//     private Button patientViewPrescriptionsButton;
//     @FXML
//     private Button patientMedicalHistoryButton;
//     @FXML 
//     private Button patientUpdateProfileButton;
//     @FXML
//     private Button patientReviewButton;
//     @FXML 
//     private Button patientViewBillsButton;

//     @Autowired
//     private PrescriptionService prescriptionService;

//     // Danh sách dữ liệu cho các TableView
//     private final ObservableList<Prescription> prescriptionList = FXCollections.observableArrayList();
//     private final ObservableList<PrescriptionDetail> prescriptionDetailList = FXCollections.observableArrayList();

//     // UUID của bệnh nhân (giả định được truyền từ màn hình đăng nhập hoặc khác)
//     private UUID patientId; // Cần được thiết lập từ màn hình trước hoặc session

//     @FXML
//     public void initialize() {
//         log.info("Khởi tạo PatientViewPrescriptionsController");

//         // Thiết lập các cột trong prescriptionTable
//         prescriptionDateColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getPrescriptionDate()));
//         doctorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDoctor().getFullName()));
//         notesColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNotes() != null ? cellData.getValue().getNotes() : ""));
//         statusColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStatus()));

//         // Thiết lập các cột trong prescriptionDetailTable
//         medicineColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMedicine().getMedicineName()));
//         quantityColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
//         dosageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDosage()));
//         instructionsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getInstructions() != null ? cellData.getValue().getInstructions() : ""));

//         // Gán danh sách dữ liệu cho các TableView
//         prescriptionTable.setItems(prescriptionList);
//         prescriptionDetailTable.setItems(prescriptionDetailList);

//         // Xử lý sự kiện chọn đơn thuốc để hiển thị chi tiết
//         prescriptionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
//             if (newSelection != null) {
//                 loadPrescriptionDetails(newSelection);
//             } else {
//                 prescriptionDetailList.clear(); // Xóa chi tiết nếu không có đơn thuốc nào được chọn
//             }
//         });
//     }

//     /**
//      * Thiết lập patientId để lấy dữ liệu đơn thuốc.
//      * Gọi từ màn hình trước khi chuyển sang màn hình này.
//      *
//      * @param patientId UUID của bệnh nhân
//      */
//     public void setPatientId(UUID patientId) {
//         this.patientId = patientId;
//         loadPrescriptions();
//     }

//     /**
//      * Tải danh sách đơn thuốc của bệnh nhân.
//      */
//     private void loadPrescriptions() {
//         if (patientId == null) {
//             log.warn("patientId chưa được thiết lập, không thể tải đơn thuốc");
//             return;
//         }

//         try {
//             // Lấy danh sách đơn thuốc với phân trang (có thể bỏ phân trang nếu muốn lấy tất cả)
//             prescriptionList.clear();
//             prescriptionList.addAll(prescriptionService.getPrescriptionsByPatient(patientId, null).getContent());
//             log.info("Đã tải {} đơn thuốc cho bệnh nhân id: {}", prescriptionList.size(), patientId);

//             // Xóa chi tiết nếu không có đơn thuốc nào được chọn
//             prescriptionDetailList.clear();
//         } catch (Exception e) {
//             log.error("Lỗi khi tải đơn thuốc cho bệnh nhân id: {}", patientId, e);
//             // Có thể hiển thị thông báo lỗi trên giao diện nếu cần
//         }
//     }

//     /**
//      * Tải chi tiết đơn thuốc khi một đơn thuốc được chọn.
//      *
//      * @param prescription Đơn thuốc được chọn
//      */
//     private void loadPrescriptionDetails(Prescription prescription) {
//         try {
//             // Lấy đơn thuốc kèm chi tiết (có thể cần JOIN FETCH nếu hiệu suất là vấn đề)
//             Prescription fullPrescription = prescriptionService.getPrescriptionById(prescription.getPrescriptionId());
//             prescriptionDetailList.clear();
//             prescriptionDetailList.addAll(fullPrescription.getPrescriptionDetailsView());
//             log.info("Đã tải {} chi tiết đơn thuốc cho prescription id: {}", prescriptionDetailList.size(), prescription.getPrescriptionId());
//         } catch (Exception e) {
//             log.error("Lỗi khi tải chi tiết đơn thuốc id: {}", prescription.getPrescriptionId(), e);
//             // Có thể hiển thị thông báo lỗi trên giao diện nếu cần
//         }
//     }

//         /**
//      * Xử lý nút "Đặt lịch hẹn".
//      */
//     @FXML
//     private void loadPatientBookAppointment(ActionEvent event) {
//         log.info("View Prescriptions button clicked for patientId: {}", patientId);
//         loadPage("@../resources/com/pma/fxml/patient_book_appointment.fxml", "Danh sách đơn thuốc");
//     }

//     /**
//      * Xử lý nút "Xem hồ sơ y tế".
//      */
//     @FXML
//     private void loadPatientMedicalHistory(ActionEvent event) {
//         log.info("View Medical Records button clicked for patientId: {}", patientId);
//         loadPage("@../resources/com/pma/fxml/patient_medical_history.fxml", "Hồ sơ y tế");
//     }

//     /**
//      * Xử lý nút "Cập nhật hồ sơ".
//      */
//     @FXML
//     private void loadPatientUpdateProfile(ActionEvent event) {
//         log.info("View Appointments button clicked for patientId: {}", patientId);
//         loadPage("@../resources/com/pma/fxml/patient_update_profile.fxml", "Danh sách lịch hẹn");
//     }

//     /**
//      * Xử lý nút "Gửi đánh giá".
//      */
//     @FXML
//     private void loadPatientReview(ActionEvent event) {
//         log.info("View Bills button clicked for patientId: {}", patientId);
//         loadPage("@../resources/com/pma/fxml/patient_review.fxml", "Danh sách hóa đơn");
//     }

//     /**
//      * Xử lý nút "Xem hóa đơn".
//      */
//     @FXML
//     private void loadPatientViewBills(ActionEvent event) {
//         log.info("View Bills button clicked for patientId: {}", patientId);
//         loadPage("@../resources/com/pma/fxml/patient_view_bills.fxml", "Danh sách hóa đơn");
//     }

//     private void loadPage(String resourcescompmafxmlpatient_medical_histor, String hồ_sơ_y_tế) {
//         throw new UnsupportedOperationException("Not supported yet.");
//     }
// }
