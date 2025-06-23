package com.pma.controller.patient;

import com.pma.model.entity.Appointment;
import com.pma.model.entity.Bill;
import com.pma.model.entity.BillItem;
import com.pma.service.BillService;
import com.pma.service.UserAccountService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Controller cho màn hình xem hóa đơn của bệnh nhân.
 */
@Controller
public class PatientViewBillsController {

    private static final Logger log = LoggerFactory.getLogger(PatientViewBillsController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Button patientBookAppointmentButton;
    @FXML private Button patientViewPrescriptionsButton;
    @FXML private Button patientMedicalHistoryButton;
    @FXML private Button patientUpdateProfileButton;
    @FXML private Button patientReviewButton;
    @FXML private Button patientViewBillsButton;

    @FXML private TableView<Bill> billsTable;
    @FXML private TableColumn<Bill, UUID> billIdColumn;
    @FXML private TableColumn<Bill, UUID> appointmentIdColumn;
    @FXML private TableColumn<Bill, String> paymentStatusColumn;
    @FXML private TableColumn<Bill, String> billDatetimeColumn;
    @FXML private TableColumn<Bill, String> dueDateColumn;
    @FXML private TableColumn<Bill, String> paymentDateColumn;
    @FXML private TableColumn<Bill, String> paymentMethodColumn;

    @FXML private TableView<BillItem> billItemsTable;
    @FXML private TableColumn<BillItem, String> itemDescriptionColumn;
    @FXML private TableColumn<BillItem, String> itemTypeColumn;
    @FXML private TableColumn<BillItem, Integer> quantityColumn;
    @FXML private TableColumn<BillItem, String> unitPriceColumn;
    @FXML private TableColumn<BillItem, String> lineTotalColumn;

    @FXML private Pagination pagination;

    @Autowired private BillService billService;
    @Autowired private UserAccountService userAccountService;
    @Autowired private UIManager uiManager;

    private UUID patientId;
    private final ObservableList<Bill> billList = FXCollections.observableArrayList();
    private final ObservableList<BillItem> billItemList = FXCollections.observableArrayList();
    private int currentPage = 0;
    private final int pageSize = 10;

    public void initData(String username) {
        userAccountService.findByUsername(username).ifPresent(user -> {
            if (user.getPatient() == null) {
                log.error("User {} is not linked to a patient", username);
                DialogUtil.showErrorAlert("Lỗi", "Không tìm thấy bệnh nhân", "Tài khoản không liên kết với bệnh nhân.");
                return;
            }
            this.patientId = user.getPatient().getPatientId();
            initialize();
            loadBills();
        });
    }

    /**
     * Khởi tạo các cột của bảng và thiết lập phân trang.
     */
@FXML
private void initialize() {
    // Thiết lập các cột cho billsTable
    billIdColumn.setCellValueFactory(cellData -> cellData.getValue().billIdProperty());
    appointmentIdColumn.setCellValueFactory(cellData -> {
        Bill bill = cellData.getValue();
        Appointment appointment = bill != null ? bill.getAppointment() : null;
        return new SimpleObjectProperty<>(
            appointment != null && appointment.getId() != null ? appointment.getId() : null
        );
    });
    paymentStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getPaymentStatus() != null ? cellData.getValue().getPaymentStatus().toString() : ""));
    billDatetimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getBillDatetime() != null ? cellData.getValue().getBillDatetime().format(DATE_TIME_FORMATTER) : ""));
    dueDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getDueDate() != null ? cellData.getValue().getDueDate().format(DATE_FORMATTER) : ""));
    paymentDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getPaymentDate() != null ? cellData.getValue().getPaymentDate().format(DATE_TIME_FORMATTER) : ""));
    paymentMethodColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getPaymentMethod() != null ? cellData.getValue().getPaymentMethod().toString() : ""));

    // Thiết lập các cột cho billItemsTable
    itemDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("itemDescription"));
    itemTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getItemType() != null ? cellData.getValue().getItemType().toString() : ""));
    quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
    unitPriceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getUnitPrice() != null ? cellData.getValue().getUnitPrice().toString() : ""));
    lineTotalColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getLineTotal() != null ? cellData.getValue().getLineTotal().toString() : ""));

    // Sự kiện khi chọn một hóa đơn từ billsTable
    billsTable.getSelectionModel().selectedItemProperty().addListener(
        (obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadBillItems(newSelection.getBillId());
            } else {
                billItemList.clear();
            }
        }
    );

    // Thiết lập phân trang
    pagination.setPageCount(1);
    pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
        currentPage = newIndex.intValue();
        loadBills();
    });

    billsTable.setItems(billList);
    billItemsTable.setItems(billItemList);
}

    /**
     * Tải danh sách hóa đơn của bệnh nhân với phân trang.
     */
    private void loadBills() {
        if (patientId == null) {
            log.error("Patient ID is null, cannot load bills");
            return;
        }
        try {
            Pageable pageable = PageRequest.of(currentPage, pageSize);
            Page<Bill> billPage = billService.getBillsByPatient(patientId, pageable);
            billList.setAll(billPage.getContent());
            pagination.setPageCount(billPage.getTotalPages());
            log.info("Loaded {} bills for patient ID: {}, page: {}", billPage.getNumberOfElements(), patientId, currentPage);
        } catch (Exception e) {
            log.error("Failed to load bills for patient ID: {}", patientId, e);
            DialogUtil.showExceptionDialog(
                    "Lỗi khi tải hóa đơn",
                    "Không thể tải danh sách hóa đơn",
                    "Đã xảy ra lỗi khi tải hóa đơn cho bệnh nhân.",
                    e
            );
        }
    }

    /**
     * Tải chi tiết các mục của hóa đơn dựa trên billId.
     */
    private void loadBillItems(UUID billId) {
        try {
            Bill bill = billService.getBillById(billId);
            List<BillItem> items = bill.getBillItemsView() != null ? List.copyOf(bill.getBillItemsView()) : List.of();
            billItemList.setAll(items);
            log.info("Loaded {} bill items for bill ID: {}", items.size(), billId);
        } catch (Exception e) {
            log.error("Failed to load bill items for bill ID: {}", billId, e);
            DialogUtil.showExceptionDialog(
                    "Lỗi khi tải chi tiết hóa đơn",
                    "Không thể tải chi tiết hóa đơn",
                    "Đã xảy ra lỗi khi tải chi tiết hóa đơn.",
                    e
            );
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