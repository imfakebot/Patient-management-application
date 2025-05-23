// package com.pma.controller;

// import com.pma.model.entity.Appointment;
// import com.pma.model.enums.AppointmentStatus;
// import com.pma.service.AppointmentService;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.fxml.FXML;
// import javafx.scene.control.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
// import java.util.UUID;

// @Controller
// public class AppointmentController {

//     @FXML private TableView<Appointment> appointmentTable;
//     @FXML private TableColumn<Appointment, UUID> appointmentIdColumn;
//     @FXML private TableColumn<Appointment, String> patientNameColumn;
//     @FXML private TableColumn<Appointment, String> doctorNameColumn;
//     @FXML private TableColumn<Appointment, LocalDateTime> dateTimeColumn;
//     @FXML private TableColumn<Appointment, AppointmentStatus> statusColumn;
//     @FXML private TextField patientIdField;
//     @FXML private TextField doctorIdField;
//     @FXML private TextField dateTimeField;
//     @FXML private TextField reasonField;
//     @FXML private Button scheduleButton;
//     @FXML private Button cancelButton;

//     @Autowired
//     private AppointmentService appointmentService;

//     private ObservableList<Appointment> appointmentList = FXCollections.observableArrayList();
//     private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

//     @FXML
//     private void initialize() {
//         appointmentIdColumn.setCellValueFactory(cellData -> cellData.getValue().appointmentIdProperty());
//         patientNameColumn.setCellValueFactory(cellData -> cellData.getValue().getPatient().fullNameProperty());
//         doctorNameColumn.setCellValueFactory(cellData -> {
//             var doctor = cellData.getValue().getDoctor();
//             return doctor != null ? doctor.fullNameProperty() : null;
//         });
//         dateTimeColumn.setCellValueFactory(cellData -> cellData.getValue().appointmentDatetimeProperty());
//         statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
//         appointmentTable.setItems(appointmentList);
//         loadAppointments();

//         scheduleButton.setOnAction(event -> scheduleAppointment());
//         cancelButton.setOnAction(event -> cancelAppointment());
//     }

//     private void loadAppointments() {
//         appointmentList.setAll(appointmentService.getAppointmentsBetweenDates(
//             LocalDateTime.now().minusDays(30), LocalDateTime.now().plusDays(30), null).getContent());
//     }

//     private void scheduleAppointment() {
//         try {
//             UUID patientId = UUID.fromString(patientIdField.getText());
//             String doctorIdText = doctorIdField.getText();
//             UUID doctorId = doctorIdText.isEmpty() ? null : UUID.fromString(doctorIdText);
//             LocalDateTime dateTime = LocalDateTime.parse(dateTimeField.getText(), formatter);
//             String reason = reasonField.getText();

//             Appointment appointment = new Appointment();
//             appointment.setAppointmentDatetime(dateTime);
//             appointment.setReason(reason);
//             appointmentService.scheduleAppointment(appointment, patientId, doctorId);
//             loadAppointments();
//             clearFields();
//             showAlert("Thành công", "Lịch hẹn đã được đặt!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể đặt lịch hẹn: " + e.getMessage());
//         }
//     }

//     private void cancelAppointment() {
//         Appointment selected = appointmentTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Lỗi", "Vui lòng chọn lịch hẹn!");
//             return;
//         }
//         try {
//             appointmentService.cancelAppointment(selected.getAppointmentId(), reasonField.getText());
//             loadAppointments();
//             clearFields();
//             showAlert("Thành công", "Lịch hẹn đã được hủy!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể hủy lịch hẹn: " + e.getMessage());
//         }
//     }

//     private void clearFields() {
//         patientIdField.clear();
//         doctorIdField.clear();
//         dateTimeField.clear();
//         reasonField.clear();
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }
// }