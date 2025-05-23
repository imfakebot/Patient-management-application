// package com.pma.controller;

// import com.pma.model.entity.Bill;
// import com.pma.model.enums.BillPaymentStatus;
// import com.pma.service.BillService;
// import com.pma.service.BillService.BillItemDTO;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.fxml.FXML;
// import javafx.scene.control.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.UUID;

// @Controller
// public class BillController {

//     @FXML private TableView<Bill> billTable;
//     @FXML private TableColumn<Bill, UUID> billIdColumn;
//     @FXML private TableColumn<Bill, String> patientNameColumn;
//     @FXML private TableColumn<Bill, BigDecimal> totalAmountColumn;
//     @FXML private TableColumn<Bill, BillPaymentStatus> paymentStatusColumn;
//     @FXML private TextField patientIdField;
//     @FXML private TextField appointmentIdField;
//     @FXML private TextField itemDescriptionField;
//     @FXML private TextField quantityField;
//     @FXML private TextField unitPriceField;
//     @FXML private Button addBillButton;
//     @FXML private Button updateStatusButton;

//     @Autowired
//     private BillService billService;

//     private ObservableList<Bill> billList = FXCollections.observableArrayList();

//     @FXML
//     private void initialize() {
//         billIdColumn.setCellValueFactory(cellData -> cellData.getValue().billIdProperty());
//         patientNameColumn.setCellValueFactory(cellData -> cellData.getValue().getPatient().fullNameProperty());
//         totalAmountColumn.setCellValueFactory(cellData -> cellData.getValue().totalAmountProperty());
//         paymentStatusColumn.setCellValueFactory(cellData -> cellData.getValue().paymentStatusProperty());
//         billTable.setItems(billList);

//         addBillButton.setOnAction(event -> createBill());
//         updateStatusButton.setOnAction(event -> updateBillStatus());
//     }

//     private void loadBillsByPatient(UUID patientId) {
//         billList.setAll(billService.getBillsByPatient(patientId, null).getContent());
//     }

//     private void createBill() {
//         try {
//             UUID patientId = UUID.fromString(patientIdField.getText());
//             String appointmentIdText = appointmentIdField.getText();
//             UUID appointmentId = appointmentIdText.isEmpty() ? null : UUID.fromString(appointmentIdText);

//             BillItemDTO itemDTO = new BillItemDTO();
//             itemDTO.setItemDescription(itemDescriptionField.getText());
//             itemDTO.setItemType(com.pma.model.enums.BillItemType.SERVICE);
//             itemDTO.setQuantity(Integer.parseInt(quantityField.getText()));
//             itemDTO.setUnitPrice(new BigDecimal(unitPriceField.getText()));

//             List<BillItemDTO> itemDTOs = new ArrayList<>();
//             itemDTOs.add(itemDTO);

//             Bill bill = new Bill();
//             bill.setBillDatetime(LocalDateTime.now());
//             billService.createBill(bill, patientId, appointmentId, itemDTOs);
//             loadBillsByPatient(patientId);
//             clearFields();
//             showAlert("Thành công", "Hóa đơn đã được tạo!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể tạo hóa đơn: " + e.getMessage());
//         }
//     }

//     private void updateBillStatus() {
//         Bill selected = billTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Lỗi", "Vui lòng chọn hóa đơn!");
//             return;
//         }
//         try {
//             billService.updatePaymentStatus(selected.getBillId(), BillPaymentStatus.PAID);
//             loadBillsByPatient(selected.getPatient().getPatientId());
//             showAlert("Thành công", "Trạng thái hóa đơn đã được cập nhật!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể cập nhật trạng thái: " + e.getMessage());
//         }
//     }

//     private void clearFields() {
//         patientIdField.clear();
//         appointmentIdField.clear();
//         itemDescriptionField.clear();
//         quantityField.clear();
//         unitPriceField.clear();
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }
// }