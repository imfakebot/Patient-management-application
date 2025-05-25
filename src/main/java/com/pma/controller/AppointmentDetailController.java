// package com.pma.controller;

// import java.util.UUID;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import com.pma.model.entity.Appointment;
// import com.pma.model.enums.AppointmentStatus;
// import com.pma.service.AppointmentService;

// import javafx.fxml.FXML;
// import javafx.scene.control.Alert;
// import javafx.scene.control.Button;
// import javafx.scene.control.ComboBox;
// import javafx.scene.control.Label;
// import javafx.scene.control.TextArea;

// @Controller
// public class AppointmentDetailController {

//     @FXML private Label appointmentIdLabel;
//     @FXML private Label patientNameLabel;
//     @FXML private Label doctorNameLabel;
//     @FXML private Label dateTimeLabel;
//     @FXML private ComboBox<AppointmentStatus> statusComboBox;
//     @FXML private TextArea noteArea;
//     @FXML private Button updateButton;

//     @Autowired
//     private AppointmentService appointmentService;

//     private Appointment currentAppointment;

//     @FXML
//     private void initialize() {
//         statusComboBox.getItems().addAll(AppointmentStatus.values());
//         updateButton.setOnAction(event -> updateStatus());
//     }

//     public void setAppointment(UUID appointmentId) {
//         try {
//             currentAppointment = appointmentService.getAppointmentById(appointmentId);
//             appointmentIdLabel.setText(currentAppointment.getAppointmentId().toString());
//             patientNameLabel.setText(currentAppointment.getPatient().getFullName());
//             doctorNameLabel.setText(currentAppointment.getDoctor() != null ? currentAppointment.getDoctor().getFullName() : "N/A");
//             dateTimeLabel.setText(currentAppointment.getAppointmentDatetime().toString());
//             statusComboBox.setValue(currentAppointment.getStatus());
//             noteArea.setText(currentAppointment.getReason());
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể tải chi tiết lịch hẹn: " + e.getMessage());
//         }
//     }

//     private void updateStatus() {
//         try {
//             AppointmentStatus newStatus = statusComboBox.getValue();
//             String note = noteArea.getText();
//             appointmentService.updateAppointmentStatus(currentAppointment.getAppointmentId(), newStatus, note);
//             showAlert("Thành công", "Trạng thái lịch hẹn đã được cập nhật!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể cập nhật trạng thái: " + e.getMessage());
//         }
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }
// }
