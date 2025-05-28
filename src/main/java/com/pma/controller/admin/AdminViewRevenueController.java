package com.pma.controller.admin;

import com.pma.model.entity.Appointment;
import com.pma.model.entity.Bill;
import com.pma.model.entity.BillItem;
import com.pma.model.entity.Patient;
import com.pma.model.entity.PrescriptionDetail;
import com.pma.model.enums.BillItemType;
import com.pma.model.enums.BillPaymentStatus;
import com.pma.model.enums.PaymentMethod;
import com.pma.service.BillService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminViewRevenueController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(AdminViewRevenueController.class);

    private final UIManager uiManager;
    private final BillService billService;

    @FXML
    private VBox sidebar;
    @FXML
    private Button adminViewRevenueButton;
    @FXML
    private Button adminManageDoctorsButton;
    @FXML
    private Button adminManagePatientsButton;
    @FXML
    private Button adminManageDepartmentsButton;
    @FXML
    private Button adminManageMedicinesButton;
    @FXML
    private Button adminManageUserAccountsButton;
    @FXML
    private Button adminManageDiseasesButton;

    // Bills Table
    @FXML
    private TableView<Bill> billsTable;
    @FXML
    private TableColumn<Bill, UUID> billIdColumn;
    @FXML
    private TableColumn<Bill, String> patientColumn;
    @FXML
    private TableColumn<Bill, String> appointmentColumn;
    @FXML
    private TableColumn<Bill, BillPaymentStatus> paymentStatusColumn;
    @FXML
    private TableColumn<Bill, LocalDateTime> billDatetimeColumn;
    @FXML
    private TableColumn<Bill, LocalDate> dueDateColumn;
    @FXML
    private TableColumn<Bill, LocalDateTime> paymentDateColumn;
    @FXML
    private TableColumn<Bill, PaymentMethod> paymentMethodColumn;
    @FXML
    private TableColumn<Bill, LocalDateTime> billCreatedAtColumn;
    @FXML
    private TableColumn<Bill, LocalDateTime> billUpdatedAtColumn;

    // Bill Items Table
    @FXML
    private TableView<BillItem> billItemsTable;
    @FXML
    private TableColumn<BillItem, String> itemDescriptionColumn; // Assuming BillItem.getItemDescription() returns String
    @FXML
    private TableColumn<BillItem, String> itemTypeColumn; // Assuming BillItem.getItemType() returns String or Enum
    @FXML
    private TableColumn<BillItem, Integer> quantityColumn;
    @FXML
    private TableColumn<BillItem, BigDecimal> unitPriceColumn;
    @FXML
    private TableColumn<BillItem, BigDecimal> lineTotalColumn;
    @FXML
    private TableColumn<BillItem, String> prescriptionDetailColumn;
    @FXML
    private TableColumn<BillItem, LocalDateTime> itemCreatedAtColumn;
    @FXML
    private TableColumn<BillItem, LocalDateTime> itemUpdatedAtColumn;

    private final ObservableList<Bill> billObservableList = FXCollections.observableArrayList();
    private final ObservableList<BillItem> billItemObservableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing AdminViewRevenueController");
        setupSidebar();
        setupBillsTable();
        setupBillItemsTable();
        loadBillsData();

        billsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        loadBillItemsForBill(newSelection);
                    } else {
                        billItemObservableList.clear();
                    }
                });
        log.info("AdminViewRevenueController initialized successfully");
    }

    private void setupSidebar() {
        // You can add logic here to highlight the active button, e.g.:
        // adminViewRevenueButton.getStyleClass().add("active-sidebar-button");
        // Assuming "active-sidebar-button" is defined in your CSS
    }

    private void setupBillsTable() {
        billIdColumn.setCellValueFactory(new PropertyValueFactory<>("billId"));
        patientColumn.setCellValueFactory(cellData -> {
            Patient patient = cellData.getValue().getPatient();
            return new SimpleStringProperty(patient != null ? patient.getFullName() : "N/A");
        });
        appointmentColumn.setCellValueFactory(cellData -> {
            Appointment appointment = cellData.getValue().getAppointment();
            return new SimpleStringProperty(appointment != null && appointment.getId() != null ? appointment.getId().toString() : "N/A");
        });
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        billDatetimeColumn.setCellValueFactory(new PropertyValueFactory<>("billDatetime"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        billCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        billUpdatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        billsTable.setItems(billObservableList);
    }

    private void setupBillItemsTable() {
        itemDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("itemDescription"));
        // Assuming BillItem has a method like getItemTypeString() or getItemType().name()
        // If BillItem.getItemType() returns an Enum:
        itemTypeColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty(cellData.getValue().getItemType() != null ? cellData.getValue().getItemType().name() : "N/A");
        });
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        lineTotalColumn.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        prescriptionDetailColumn.setCellValueFactory(cellData -> {
            PrescriptionDetail pd = cellData.getValue().getPrescriptionDetail();
            // Replace 'getSomeProperty()' with an actual property or method of PrescriptionDetail that you want to display
            return new SimpleStringProperty(pd != null ? pd.toString() : "N/A");
        });
        itemCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        itemUpdatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        billItemsTable.setItems(billItemObservableList);
    }

    private void loadBillsData() {
        try {
            log.debug("Loading bills data...");
            List<Bill> bills = billService.findAll(); // Assuming BillService has findAll()
            if (bills != null) {
                billObservableList.setAll(bills);
                log.info("Loaded {} bills.", bills.size());
            } else {
                billObservableList.clear();
                log.warn("BillService returned null for findAll().");
            }
        } catch (Exception e) {
            log.error("Error loading bills data: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi tải dữ liệu", "Không thể tải danh sách hóa đơn.");
            billObservableList.clear();
        }
        if (!billObservableList.isEmpty()) {
            billsTable.getSelectionModel().selectFirst();
        }
    }

    private void loadBillItemsForBill(Bill bill) {
        if (bill != null) {
            log.debug("Loading bill items for bill ID: {}", bill.getBillId());
            // Assuming Bill entity has a getBillItems() method that returns Set<BillItem> or List<BillItem>
            // and these items are eagerly fetched or fetched within the transaction.
            // If BillItems are lazily loaded and the session is closed, you'll need to fetch them via BillItemService.
            if (bill.getBillItems() != null) {
                billItemObservableList.setAll(bill.getBillItems());
                log.info("Loaded {} bill items for bill ID: {}.", bill.getBillItems().size(), bill.getBillId());
            } else {
                billItemObservableList.clear();
                log.warn("Bill (ID: {}) has null bill items.", bill.getBillId());
            }
        } else {
            billItemObservableList.clear();
            log.debug("No bill selected, clearing bill items table.");
        }
    }

    // --- Sidebar Navigation Methods ---
    @FXML
    private void loadAdminViewRevenue(ActionEvent event) {
        log.info("Admin View Revenue button clicked. Refreshing data.");
        loadBillsData(); // Refresh data on current view
    }

    @FXML
    private void loadAdminManageDoctors(ActionEvent event) {
        log.info("Navigating to Admin Manage Doctors screen.");
        uiManager.switchToAdminManageDoctors();
    }

    @FXML
    private void loadAdminManagePatients(ActionEvent event) {
        log.info("Navigating to Admin Manage Patients screen.");
        uiManager.switchToAdminManagePatients();
    }

    @FXML
    private void loadAdminManageDepartments(ActionEvent event) {
        log.info("Navigating to Admin Manage Departments screen.");
        uiManager.switchToAdminManageDepartments();
    }

    @FXML
    private void loadAdminManageMedicines(ActionEvent event) {
        log.info("Navigating to Admin Manage Medicines screen.");
        uiManager.switchToAdminManageMedicines();
    }

    @FXML
    private void loadAdminManageUserAccounts(ActionEvent event) {
        log.info("Navigating to Admin Manage User Accounts screen.");
        uiManager.switchToAdminManageUserAccounts();
    }

    @FXML
    private void loadAdminManageDiseases(ActionEvent event) {
        log.info("Navigating to Admin Manage Diseases screen.");
        uiManager.switchToAdminManageDiseases();
    }
}
