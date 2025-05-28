package com.pma.admin;

import java.time.LocalDateTime; // Giả định bạn có entity này
import java.util.List; // Giả định bạn có service này
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.pma.model.entity.Disease;
import com.pma.service.DiseaseService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

@Controller
public class AdminManageDiseasesController {

    private static final Logger log = LoggerFactory.getLogger(AdminManageDiseasesController.class);

    // Sidebar components
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

    // Form components
    @FXML
    private TextField diseaseCodeField;
    @FXML
    private TextField diseaseNameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;

    // Table components
    @FXML
    private TableView<Disease> diseasesTable;
    @FXML
    private TableColumn<Disease, String> diseaseCodeColumn;
    @FXML
    private TableColumn<Disease, String> diseaseNameColumn;
    @FXML
    private TableColumn<Disease, String> descriptionColumn;
    @FXML
    private TableColumn<Disease, LocalDateTime> createdAtColumn;
    @FXML
    private TableColumn<Disease, LocalDateTime> updatedAtColumn;

    private final DiseaseService diseaseService;
    private final UIManager uiManager;

    private ObservableList<Disease> diseaseList = FXCollections.observableArrayList();
    private Disease selectedDisease = null;

    @Autowired
    public AdminManageDiseasesController(DiseaseService diseaseService, UIManager uiManager) {
        this.diseaseService = diseaseService;
        this.uiManager = uiManager;
    }

    @FXML
    public void initialize() {
        log.info("Initializing AdminManageDiseasesController");
        setupTableColumns();
        loadDiseases();
        setupTableSelectionListener();

        // Đánh dấu nút sidebar hiện tại
        adminManageDiseasesButton.setStyle("-fx-background-color: -accent-color;");

        // Vô hiệu hóa nút cập nhật/xóa ban đầu
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void setupTableColumns() {
        diseaseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("diseaseCode"));
        diseaseNameColumn.setCellValueFactory(new PropertyValueFactory<>("name")); // Giả sử thuộc tính là 'name' trong Disease entity
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        // Tùy chọn: Thêm định dạng ngày tháng cho cột createdAt và updatedAt nếu cần
        // Ví dụ:
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        // createdAtColumn.setCellFactory(column -> new TableCell<Disease, LocalDateTime>() {
        //     @Override
        //     protected void updateItem(LocalDateTime item, boolean empty) {
        //         super.updateItem(item, empty);
        //         if (empty || item == null) {
        //             setText(null);
        //         } else {
        //             setText(formatter.format(item));
        //         }
        //     }
        // });
    }

    private void loadDiseases() {
        try {
            List<Disease> diseases = diseaseService.getAllDiseases(); // Giả sử service có phương thức này
            diseaseList.setAll(diseases);
            diseasesTable.setItems(diseaseList);
            log.info("Loaded {} diseases into the table.", diseases.size());
        } catch (Exception e) {
            log.error("Error loading diseases: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi tải dữ liệu", "Không thể tải danh sách bệnh.");
        }
    }

    private void setupTableSelectionListener() {
        diseasesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedDisease = newSelection;
                populateForm(selectedDisease);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
                addButton.setDisable(true); // Vô hiệu hóa nút "Thêm" khi đang chọn để sửa
            } else {
                // Không làm gì ở đây để tránh gọi clearForm() đệ quy nếu clearSelection() được gọi từ clearForm()
            }
        });
    }

    private void populateForm(Disease disease) {
        diseaseCodeField.setText(disease.getDiseaseCode());
        diseaseNameField.setText(disease.getDiseaseName());
        descriptionField.setText(disease.getDescription());
    }

    @FXML
    private void addDisease(ActionEvent event) {
        log.info("Add Disease button clicked.");
        String code = diseaseCodeField.getText().trim();
        String name = diseaseNameField.getText().trim();
        String description = descriptionField.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            DialogUtil.showWarningAlert("Thiếu thông tin", "Mã bệnh và Tên bệnh không được để trống.");
            return;
        }

        if (diseaseService.findByDiseaseCode(code).isPresent()) {
            DialogUtil.showErrorAlert("Lỗi", "Mã bệnh '" + code + "' đã tồn tại.");
            return;
        }

        Disease newDisease = new Disease();
        newDisease.setDiseaseCode(code);
        newDisease.setDiseaseName(name);
        newDisease.setDescription(description);

        try {
            Disease savedDisease = diseaseService.createDisease(newDisease);
            diseaseList.add(savedDisease);
            diseasesTable.getSelectionModel().select(savedDisease);
            log.info("Disease added: {}", savedDisease);
            DialogUtil.showInfoAlert("Thành công", "Đã thêm bệnh mới thành công.");
            clearFormAndSelection();
        } catch (Exception e) {
            log.error("Error adding disease: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi", "Không thể thêm bệnh: " + e.getMessage());
        }
    }

    @FXML
    private void updateDisease(ActionEvent event) {
        log.info("Update Disease button clicked.");
        if (selectedDisease == null) {
            DialogUtil.showWarningAlert("Chưa chọn bệnh", "Vui lòng chọn một bệnh để cập nhật.");
            return;
        }

        String code = diseaseCodeField.getText().trim();
        String name = diseaseNameField.getText().trim();
        String description = descriptionField.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            DialogUtil.showWarningAlert("Thiếu thông tin", "Mã bệnh và Tên bệnh không được để trống.");
            return;
        }

        Optional<Disease> existingDiseaseWithCode = diseaseService.findByDiseaseCode(code);
        if (existingDiseaseWithCode.isPresent() && !existingDiseaseWithCode.get().getId().equals(selectedDisease.getId())) {
            DialogUtil.showErrorAlert("Lỗi", "Mã bệnh '" + code + "' đã tồn tại cho một bệnh khác.");
            return;
        }

        selectedDisease.setDiseaseCode(code);
        selectedDisease.setDiseaseName(name);
        selectedDisease.setDescription(description);

        try {
            Disease updatedDisease = diseaseService.updateDisease(description, selectedDisease);
            int index = diseaseList.indexOf(selectedDisease); // Tìm theo object reference cũ
            if (index != -1) {
                diseaseList.set(index, updatedDisease); // Thay thế bằng object đã cập nhật
            } else { // Fallback nếu không tìm thấy (ít khi xảy ra nếu selectedDisease được quản lý đúng)
                loadDiseases();
            }
            diseasesTable.getSelectionModel().select(updatedDisease);
            log.info("Disease updated: {}", updatedDisease);
            DialogUtil.showInfoAlert("Thành công", "Đã cập nhật thông tin bệnh thành công.");
            clearFormAndSelection();
        } catch (Exception e) {
            log.error("Error updating disease: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi", "Không thể cập nhật bệnh: " + e.getMessage());
        }
    }

    @FXML
    private void deleteDisease(ActionEvent event) {
        log.info("Delete Disease button clicked.");
        if (selectedDisease == null) {
            DialogUtil.showWarningAlert("Chưa chọn bệnh", "Vui lòng chọn một bệnh để xóa.");
            return;
        }

        Optional<ButtonType> result = DialogUtil.showConfirmationAlert("Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa bệnh '" + selectedDisease.getDiseaseName() + "' không?");
        boolean confirmed = result.isPresent() && result.get() == ButtonType.OK;

        if (confirmed) {
            try {
                diseaseService.deleteDisease((String) selectedDisease.getId());
                diseaseList.remove(selectedDisease);
                log.info("Disease deleted: {}", selectedDisease);
                DialogUtil.showInfoAlert("Thành công", "Đã xóa bệnh thành công.");
                clearFormAndSelection();
            } catch (Exception e) {
                log.error("Error deleting disease: {}", e.getMessage(), e);
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("constraint")) {
                    DialogUtil.showErrorAlert("Lỗi", "Không thể xóa bệnh này vì nó đang được tham chiếu ở nơi khác (ví dụ: trong hồ sơ bệnh án).");
                } else {
                    DialogUtil.showErrorAlert("Lỗi", "Không thể xóa bệnh: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void clearForm(ActionEvent event) {
        clearFormAndSelection();
        log.info("Form cleared by button.");
    }

    private void clearFormAndSelection() {
        diseaseCodeField.clear();
        diseaseNameField.clear();
        descriptionField.clear();
        selectedDisease = null;
        diseasesTable.getSelectionModel().clearSelection(); // Xóa lựa chọn trong bảng
        // Sau khi clearSelection, listener sẽ được kích hoạt và đặt lại trạng thái các nút
        addButton.setDisable(false);
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    // --- Sidebar Navigation Methods ---
    @FXML
    private void loadAdminViewRevenue(ActionEvent event) {
        uiManager.switchToAdminViewRevenue();
    }

    @FXML
    private void loadAdminManageDoctors(ActionEvent event) {
        uiManager.switchToAdminManageDoctors();
    }

    @FXML
    private void loadAdminManagePatients(ActionEvent event) {
        uiManager.switchToAdminManagePatients();
    }

    @FXML
    private void loadAdminManageDepartments(ActionEvent event) {
        uiManager.switchToAdminManageDepartments();
    }

    @FXML
    private void loadAdminManageMedicines(ActionEvent event) {
        uiManager.switchToAdminManageMedicines();
    }

    @FXML
    private void loadAdminManageUserAccounts(ActionEvent event) {
        uiManager.switchToAdminManageUserAccounts();
    }

    @FXML
    private void loadAdminManageDiseases(ActionEvent event) {
        log.info("Admin Manage Diseases button clicked (current screen). Reloading data.");
        loadDiseases(); // Tải lại dữ liệu
    }
}
