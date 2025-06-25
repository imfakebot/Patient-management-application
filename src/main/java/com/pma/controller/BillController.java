// package com.pma.controller;

// import com.pma.model.entity.Bill;
// import com.pma.model.enums.BillPaymentStatus;
// import com.pma.service.BillService;
// import com.pma.service.BillService.BillItemDTO;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.fxml.FXML;
// import javafx.scene.control.*;
// import javafx.scene.control.cell.PropertyValueFactory; // Thêm import này
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.UUID;

// @Controller
// public class BillController {

//     @FXML
//     private TableView<Bill> billTable;
//     @FXML
//     private TableColumn<Bill, UUID> billIdColumn;
//     @FXML
//     private TableColumn<Bill, String> patientNameColumn;
//     @FXML
//     private TableColumn<Bill, BigDecimal> totalAmountColumn;
//     @FXML
//     private TableColumn<Bill, BillPaymentStatus> paymentStatusColumn;
//     @FXML
//     private TextField patientIdField;
//     @FXML
//     private TextField appointmentIdField;
//     @FXML
//     private TextField itemDescriptionField;
//     @FXML
//     private TextField quantityField;
//     @FXML
//     private TextField unitPriceField;
//     @FXML
//     private Button addBillButton;
//     @FXML
//     private Button updateStatusButton;

//     @Autowired
//     private BillService billService;

//     private ObservableList<Bill> billList = FXCollections.observableArrayList();

//     @FXML
//     private void initialize() {
//         // Sử dụng PropertyValueFactory để trích xuất giá trị từ thuộc tính của Bill
//         billIdColumn.setCellValueFactory(new PropertyValueFactory<>("billId"));
//         // Để lấy patientName, bạn cần đảm bảo Bill có getter getPatient() và Patient có getter getFullName()
//         // Hoặc bạn có thể tạo một cột tùy chỉnh nếu logic phức tạp hơn
//         patientNameColumn.setCellValueFactory(new PropertyValueFactory<>("patientFullNameForDisplay")); // Giả sử bạn thêm một phương thức getter trong Bill để hiển thị tên bệnh nhân
//         totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount")); // Bill đã có getTotalAmount()
//         paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
//         billTable.setItems(billList);

//         addBillButton.setOnAction(event -> createBill());
//         updateStatusButton.setOnAction(event -> updateBillStatus());
//     }

//     private void loadBillsByPatient(UUID patientId) {
//         billList.setAll(billService.getBillsByPatient(patientId, null).getContent());
//     }

//     private void createBill() {
//         try {
//             String patientIdStr = patientIdField.getText().trim();
//             String appointmentIdStr = appointmentIdField.getText().trim();
//             String itemDescStr = itemDescriptionField.getText().trim();
//             String quantityStr = quantityField.getText().trim();
//             String unitPriceStr = unitPriceField.getText().trim();

//             if (patientIdStr.isEmpty() || itemDescStr.isEmpty() || quantityStr.isEmpty() || unitPriceStr.isEmpty()) {
//                 showAlert("Lỗi", "Vui lòng điền đầy đủ thông tin bắt buộc (Patient ID, Item Description, Quantity, Unit Price).");
//                 return;
//             }

//             UUID patientId;
//             UUID appointmentId = null;
//             int quantity;
//             BigDecimal unitPrice;

//             try {
//                 patientId = UUID.fromString(patientIdStr);
//             } catch (IllegalArgumentException e) {
//                 showAlert("Lỗi", "Patient ID không hợp lệ.");
//                 return;
//             }

//             if (!appointmentIdStr.isEmpty()) {
//                 try {
//                     appointmentId = UUID.fromString(appointmentIdStr);
//                 } catch (IllegalArgumentException e) {
//                     showAlert("Lỗi", "Appointment ID không hợp lệ.");
//                     return;
//                 }
//             }

//             try {
//                 quantity = Integer.parseInt(quantityStr);
//                 unitPrice = new BigDecimal(unitPriceStr);
//                 if (quantity <= 0 || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
//                     showAlert("Lỗi", "Số lượng phải lớn hơn 0 và đơn giá không được âm.");
//                     return;
//                 }
//             } catch (NumberFormatException e) {
//                 showAlert("Lỗi", "Số lượng hoặc đơn giá không hợp lệ.");
//                 return;
//             }

//             BillItemDTO itemDTO = new BillItemDTO();
//             itemDTO.setItemDescription(itemDescStr);
//             // Thay thế BillItemType.SERVICE bằng một giá trị hợp lệ từ enum BillItemType đã được sửa đổi.
//             // Ví dụ: sử dụng CONSULTATION. Bạn có thể cần một ComboBox trên UI để người dùng chọn.
//             itemDTO.setItemType(com.pma.model.enums.BillItemType.CONSULTATION);
//             itemDTO.setQuantity(quantity);
//             itemDTO.setUnitPrice(unitPrice);

//             List<BillItemDTO> itemDTOs = new ArrayList<>();
//             itemDTOs.add(itemDTO);

//             Bill bill = new Bill();
//             bill.setBillDatetime(LocalDateTime.now());
//             billService.createBill(bill, patientId, appointmentId, itemDTOs);
//             loadBillsByPatient(patientId);
//             clearFields();
//             showAlert("Thành công", "Hóa đơn đã được tạo!");
//         } catch (Exception e) {
//             // Ghi log lỗi chi tiết hơn ở đây nếu cần
//             // log.error("Error creating bill", e);
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
//             // Kiểm tra null cho patient trước khi truy cập
//             if (selected.getPatient() == null) {
//                 showAlert("Lỗi", "Không tìm thấy thông tin bệnh nhân cho hóa đơn này.");
//                 return;
//             }
//             billService.updatePaymentStatus(selected.getBillId(), BillPaymentStatus.Paid, null, null);
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
