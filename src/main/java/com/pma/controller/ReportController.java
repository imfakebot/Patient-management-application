// package com.pma.controller;

// import com.pma.model.entity.Appointment;
// import com.pma.model.entity.Bill;
// import com.pma.model.entity.Patient;
// import com.pma.service.AppointmentService;
// import com.pma.service.BillService;
// import com.pma.service.PatientService;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.fxml.FXML;
// import javafx.scene.chart.BarChart;
// import javafx.scene.chart.PieChart;
// import javafx.scene.control.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import java.time.LocalDate;
// import java.time.LocalDateTime;

// @Controller
// public class ReportController {

//     @FXML private DatePicker fromDatePicker;
//     @FXML private DatePicker toDatePicker;
//     @FXML private ComboBox<String> reportTypeComboBox;
//     @FXML private Button generateButton;
//     @FXML private Button exportPdfButton;
//     @FXML private Button exportExcelButton;
//     @FXML private PieChart patientPieChart;
//     @FXML private BarChart<String, Number> revenueBarChart;
//     @FXML private TableView<ReportData> reportTable;
//     @FXML private TableColumn<ReportData, String> dateColumn;
//     @FXML private TableColumn<ReportData, Number> patientCountColumn;
//     @FXML private TableColumn<ReportData, Number> appointmentCountColumn;
//     @FXML private TableColumn<ReportData, Number> revenueColumn;

//     @Autowired
//     private PatientService patientService;
//     @Autowired
//     private AppointmentService appointmentService;
//     @Autowired
//     private BillService billService;

//     private ObservableList<ReportData> reportDataList = FXCollections.observableArrayList();

//     public static class ReportData {
//         private String date;
//         private int patientCount;
//         private int appointmentCount;
//         private double revenue;

//         public ReportData(String date, int patientCount, int appointmentCount, double revenue) {
//             this.date = date;
//             this.patientCount = patientCount;
//             this.appointmentCount = appointmentCount;
//             this.revenue = revenue;
//         }

//         public String getDate() { return date; }
//         public int getPatientCount() { return patientCount; }
//         public int getAppointmentCount() { return appointmentCount; }
//         public double getRevenue() { return revenue; }
//     }

//     @FXML
//     private void initialize() {
//         reportTypeComboBox.getItems().addAll("Bệnh nhân", "Cuộc hẹn", "Doanh thu");
//         dateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate()));
//         patientCountColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getPatientCount()));
//         appointmentCountColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getAppointmentCount()));
//         revenueColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getRevenue()));
//         reportTable.setItems(reportDataList);

//         generateButton.setOnAction(event -> generateReport());
//         exportPdfButton.setOnAction(event -> exportToPdf());
//         exportExcelButton.setOnAction(event -> exportToExcel());
//     }

//     private void generateReport() {
//         try {
//             LocalDateTime from = fromDatePicker.getValue().atStartOfDay();
//             LocalDateTime to = toDatePicker.getValue().atTime(23, 59, 59);
//             String type = reportTypeComboBox.getValue();

//             reportDataList.clear();
//             if ("Bệnh nhân".equals(type)) {
//                 long count = patientService.getAllPatients(null).getTotalElements();
//                 reportDataList.add(new ReportData(from.toLocalDate().toString(), (int) count, 0, 0));
//                 updatePieChart(count, "Bệnh nhân");
//             } else if ("Cuộc hẹn".equals(type)) {
//                 long count = appointmentService.getAppointmentsBetweenDates(from, to, null).getTotalElements();
//                 reportDataList.add(new ReportData(from.toLocalDate().toString(), 0, (int) count, 0));
//                 updatePieChart(count, "Cuộc hẹn");
//             } else if ("Doanh thu".equals(type)) {
//                 // Giả định tính tổng doanh thu từ BillService
//                 reportDataList.add(new ReportData(from.toLocalDate().toString(), 0, 0, 0));
//                 updateBarChart();
//             }
//             showAlert("Thành công", "Báo cáo đã được tạo!");
//         } catch (Exception e) {
//             showAlert("Lỗi", "Không thể tạo báo cáo: " + e.getMessage());
//         }
//     }

//     private void updatePieChart(long count, String type) {
//         patientPieChart.getData().clear();
//         patientPieChart.getData().add(new PieChart.Data(type, count));
//     }

//     private void updateBarChart() {
//         revenueBarChart.getData().clear();
//         // Giả định thêm dữ liệu doanh thu
//     }

//     private void exportToPdf() {
//         showAlert("Thông báo", "Chức năng xuất PDF chưa được triển khai!");
//     }

//     private void exportToExcel() {
//         showAlert("Thông báo", "Chức năng xuất Excel chưa được triển khai!");
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }
// }