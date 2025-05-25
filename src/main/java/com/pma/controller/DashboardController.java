// package com.pma.controller;

// import com.pma.service.AppointmentService;
// import com.pma.service.BillService;
// import com.pma.service.PatientService;

// import javafx.fxml.FXML;
// import javafx.scene.control.Button;
// import javafx.scene.control.Label;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import java.time.LocalDateTime;

// import javafx.scene.control.Alert;

// @Controller
// public class DashboardController {

//     @FXML
//     private Label patientCountLabel;
//     @FXML
//     private Label appointmentCountLabel;
//     @FXML
//     private Label revenueLabel;
//     @FXML
//     private Button patientButton;
//     @FXML
//     private Button appointmentButton;
//     @FXML
//     private Button billButton;
//     @FXML
//     private Button reportButton;

//     @Autowired
//     private PatientService patientService;
//     @Autowired
//     private AppointmentService appointmentService;
//     @Autowired
//     private BillService billService;

//     @FXML
//     private void initialize() {
//         loadStatistics();
//         patientButton.setOnAction(event -> navigateTo("patient_management.fxml"));
//         appointmentButton.setOnAction(event -> navigateTo("appointment_management.fxml"));
//         billButton.setOnAction(event -> navigateTo("bill_management.fxml"));
//         reportButton.setOnAction(event -> navigateTo("report.fxml"));
//     }

//     private void loadStatistics() {
//         try {
//             long patientCount = patientService.getAllPatients(null).getTotalElements();
//             long appointmentCount = appointmentService.getAppointmentsBetweenDates(
//                     LocalDateTime.now().minusDays(30),
//                      LocalDateTime.now(),
//                      null).getTotalElements();
//             patientCountLabel.setText(String.valueOf(patientCount));
//             appointmentCountLabel.setText(String.valueOf(appointmentCount));
//             revenueLabel.setText("N/A"); // Cần logic tính doanh thu từ BillService
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể tải thống kê: " + e.getMessage());
//         }
//     }

//     private void navigateTo(String fxml) {
//         // Giả định có phương thức điều hướng
//         // Thay bằng logic thực tế của ứng dụng
//         System.out.println("Navigate to: " + fxml);
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }
// }
