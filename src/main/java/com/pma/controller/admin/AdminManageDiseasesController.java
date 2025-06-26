package com.pma.controller.admin;

import com.pma.model.entity.Disease;
import com.pma.service.DiseaseService; // Giả định bạn có service này
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityNotFoundException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class AdminManageDiseasesController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(AdminManageDiseasesController.class);

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
    @FXML
    private Button patientBookAppointmentButton;

    @FXML
    private Button patientViewPrescriptionsButton;

    @FXML
    private Button patientMedicalHistoryButton;

    @FXML
    private Button patientUpdateProfileButton;

    @FXML
    private Button patientViewBillsButton;

    @FXML
    private Button logoutBtn;

    @FXML
    private TextField diseaseCodeField;
    @FXML
    private TextField diseaseNameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField searchField;

    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;

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

    private final UIManager uiManager;
    private final DiseaseService diseaseService; // Inject service

    private final ObservableList<Disease> diseaseObservableList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing AdminManageDiseasesController");
        setupTableColumns();
        loadDiseasesData();

        diseasesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        populateForm(newSelection);
                        diseaseCodeField.setDisable(true); // Disable code field when updating
                    } else {
                        clearForm(null);
                        diseaseCodeField.setDisable(false); // Enable code field for new entry
                    }
                });
        log.info("AdminManageDiseasesController initialized successfully");
    }

    private void setupTableColumns() {
        diseaseCodeColumn.setCellValueFactory(new PropertyValueFactory<>("diseaseCode")); // or "id" if you use getId()
        diseaseNameColumn.setCellValueFactory(new PropertyValueFactory<>("diseaseName"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        diseasesTable.setItems(diseaseObservableList);
    }

    private void loadDiseasesData() {
        try {
            log.debug("Loading diseases data...");
            List<Disease> diseases = diseaseService.getAllDiseases();
            diseaseObservableList.setAll(diseases);
            log.info("Loaded {} diseases.", diseases.size());
        } catch (Exception e) {
            log.error("Error loading diseases data: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi tải dữ liệu", "Không thể tải danh sách bệnh.");
            diseaseObservableList.clear();
        }
    }

    private void populateForm(Disease disease) {
        if (disease != null) {
            diseaseCodeField.setText(disease.getDiseaseCode());
            diseaseNameField.setText(disease.getDiseaseName());
            descriptionField.setText(disease.getDescription());
        }
    }

    private boolean validateInput(boolean isUpdate) {
        StringBuilder errors = new StringBuilder();
        if (!isUpdate && (diseaseCodeField.getText() == null || diseaseCodeField.getText().trim().isEmpty())) {
            errors.append("- Mã bệnh không được để trống.\n");
        }
        if (diseaseNameField.getText() == null || diseaseNameField.getText().trim().isEmpty()) {
            errors.append("- Tên bệnh không được để trống.\n");
        }
        // Mã bệnh có thể có ràng buộc về độ dài hoặc ký tự, tùy theo yêu cầu
        // Ví dụ: if (diseaseCodeField.getText().trim().length() > 20) errors.append("- Mã bệnh quá dài (tối đa 20 ký tự).\n");

        if (!errors.isEmpty()) {
            DialogUtil.showWarningAlert("Thiếu thông tin hoặc dữ liệu không hợp lệ", errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    void addDisease(ActionEvent event) {
        log.info("Add Disease button clicked.");
        if (!validateInput(false)) {
            return;
        }

        Disease newDisease = new Disease();
        newDisease.setDiseaseCode(diseaseCodeField.getText().trim());
        newDisease.setDiseaseName(diseaseNameField.getText().trim());
        newDisease.setDescription(descriptionField.getText().trim());

        try {
            Disease savedDisease = diseaseService.createDisease(newDisease);
            diseaseObservableList.add(savedDisease);
            diseasesTable.getSelectionModel().select(savedDisease);
            DialogUtil.showSuccessAlert("Thành công", "Đã thêm bệnh mới thành công.");
            clearForm(null);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while adding disease: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi Trùng lặp", "Không thể thêm bệnh. Mã bệnh hoặc tên bệnh có thể đã tồn tại.");
        } catch (Exception e) {
            log.error("Error adding disease: {}", e.getMessage(), e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể thêm bệnh.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void updateDisease(ActionEvent event) {
        log.info("Update Disease button clicked.");
        Disease selectedDisease = diseasesTable.getSelectionModel().getSelectedItem();
        if (selectedDisease == null) {
            DialogUtil.showWarningAlert("Chưa chọn Bệnh", "Vui lòng chọn một bệnh để cập nhật.");
            return;
        }
        if (!validateInput(true)) {
            return; // Mã bệnh không cần validate khi update vì nó không đổi
        }
        Disease diseaseToUpdate = new Disease(); // Tạo đối tượng mới để truyền vào service
        // diseaseCode không được cập nhật, nó là khóa chính
        diseaseToUpdate.setDiseaseName(diseaseNameField.getText().trim());
        diseaseToUpdate.setDescription(descriptionField.getText().trim());

        try {
            // Service nên nhận ID (diseaseCode) và đối tượng chứa thông tin cập nhật
            Disease updatedDisease = diseaseService.updateDisease(selectedDisease.getDiseaseCode(), diseaseToUpdate);
            int index = diseaseObservableList.indexOf(selectedDisease);
            if (index != -1) {
                diseaseObservableList.set(index, updatedDisease);
                diseasesTable.getSelectionModel().select(updatedDisease);
            } else {
                loadDiseasesData(); // Fallback
            }
            DialogUtil.showSuccessAlert("Thành công", "Đã cập nhật thông tin bệnh thành công.");
            clearForm(null);
        } catch (EntityNotFoundException e) {
            log.error("Disease not found for update: {}", selectedDisease.getDiseaseCode(), e);
            DialogUtil.showErrorAlert("Không tìm thấy", "Không tìm thấy bệnh để cập nhật. Có thể đã bị xóa.");
            loadDiseasesData();
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating disease: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi Trùng lặp", "Không thể cập nhật. Tên bệnh mới có thể đã tồn tại.");
        } catch (Exception e) {
            log.error("Error updating disease: {}", e.getMessage(), e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể cập nhật bệnh.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void deleteDisease(ActionEvent event) {
        log.info("Delete Disease button clicked.");
        Disease selectedDisease = diseasesTable.getSelectionModel().getSelectedItem();
        if (selectedDisease == null) {
            DialogUtil.showWarningAlert("Chưa chọn Bệnh", "Vui lòng chọn một bệnh để xóa.");
            return;
        }

        boolean confirmed = DialogUtil.showConfirmation("Xác nhận Xóa",
                "Bạn có chắc chắn muốn xóa bệnh '" + selectedDisease.getDiseaseName() + "' (Mã: " + selectedDisease.getDiseaseCode() + ") không?");
        if (confirmed) {
            try {
                diseaseService.deleteDisease(selectedDisease.getDiseaseCode());
                diseaseObservableList.remove(selectedDisease);
                DialogUtil.showSuccessAlert("Thành công", "Đã xóa bệnh thành công.");
                clearForm(null);
            } catch (EntityNotFoundException e) {
                log.error("Disease not found for deletion: {}", selectedDisease.getDiseaseCode(), e);
                DialogUtil.showErrorAlert("Không tìm thấy", "Không tìm thấy bệnh để xóa. Có thể đã bị xóa.");
                loadDiseasesData();
            } catch (DataIntegrityViolationException e) {
                log.error("Cannot delete disease due to existing references: {}", selectedDisease.getDiseaseCode(), e);
                DialogUtil.showErrorAlert("Không thể Xóa", "Không thể xóa bệnh này do có các dữ liệu liên quan (ví dụ: trong chẩn đoán).");
            } catch (Exception e) {
                log.error("Error deleting disease: {}", e.getMessage(), e);
                DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể xóa bệnh.", "Vui lòng thử lại sau.", e);
            }
        }
    }

    @FXML
    void clearForm(ActionEvent event) {
        log.debug("Clear form button clicked.");
        diseaseCodeField.clear();
        diseaseNameField.clear();
        descriptionField.clear();
        diseasesTable.getSelectionModel().clearSelection();
        diseaseCodeField.setDisable(false); // Luôn enable khi clear form
    }

    // Sidebar navigation methods
    @FXML
    void loadAdminViewRevenue(ActionEvent event) {
        uiManager.switchToAdminViewRevenue();
    }

    @FXML
    void loadAdminManageDoctors(ActionEvent event) {
        uiManager.switchToAdminManageDoctors();
    }

    @FXML
    void loadAdminManagePatients(ActionEvent event) {
        uiManager.switchToAdminManagePatients();
    }

    @FXML
    void loadAdminManageDepartments(ActionEvent event) {
        uiManager.switchToAdminManageDepartments();
    }

    @FXML
    void loadAdminManageMedicines(ActionEvent event) {
        uiManager.switchToAdminManageMedicines();
    }

    @FXML
    void loadAdminManageUserAccounts(ActionEvent event) {
        uiManager.switchToAdminManageUserAccounts();
    }

    @FXML
    void logout() {
        uiManager.switchToLoginScreen();
    }

    @FXML
    void loadAdminManageDiseases(ActionEvent event) {
        loadDiseasesData();
        /* Already on this screen, refresh data */ }

    @FXML
    private void handleSearch(ActionEvent event) {
        filterDiseases(searchField.getText());
    }

    @FXML
    private void clearSearch(ActionEvent event) {
        searchField.clear();
        loadDiseasesData(); // Reset to show all diseases
    }

    private void filterDiseases(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            loadDiseasesData();
            return;
        }

        String searchLower = searchText.toLowerCase().trim();
        ObservableList<Disease> allDiseases = diseasesTable.getItems();
        ObservableList<Disease> filteredList = allDiseases.filtered(disease
                -> disease.getDiseaseCode().toLowerCase().contains(searchLower)
                || disease.getDiseaseName().toLowerCase().contains(searchLower)
        );

        diseasesTable.setItems(filteredList);
    }
}
