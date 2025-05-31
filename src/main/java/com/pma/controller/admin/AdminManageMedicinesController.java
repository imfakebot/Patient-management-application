package com.pma.controller.admin;

import java.math.BigDecimal; // Giả định bạn có entity này
import java.net.URL; // Giả định bạn có service này
import java.time.LocalDateTime;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.pma.model.entity.Medicine;
import com.pma.model.enums.MedicineStatus;
import com.pma.service.MedicineService;
import com.pma.util.DialogUtil;
import com.pma.util.UIManager;

import jakarta.persistence.EntityNotFoundException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminManageMedicinesController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(AdminManageMedicinesController.class);

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
    private TextField medicineNameField;
    @FXML
    private TextField manufacturerField;
    @FXML
    private TextField unitField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField stockQuantityField;
    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button clearButton;

    // Pagination Controls (Assume these are added in FXML)
    @FXML
    private Button previousPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Label pageInfoLabel;
    @FXML
    private TableView<Medicine> medicinesTable;
    @FXML
    private TableColumn<Medicine, String> medicineNameColumn;
    @FXML
    private TableColumn<Medicine, String> manufacturerColumn;
    @FXML
    private TableColumn<Medicine, String> unitColumn;
    @FXML
    private TableColumn<Medicine, String> descriptionColumn;
    @FXML
    private TableColumn<Medicine, BigDecimal> priceColumn;
    @FXML
    private TableColumn<Medicine, Integer> stockQuantityColumn;
    @FXML
    private TableColumn<Medicine, String> statusColumn;
    @FXML
    private TableColumn<Medicine, LocalDateTime> createdAtColumn;
    @FXML
    private TableColumn<Medicine, LocalDateTime> updatedAtColumn;

    private final UIManager uiManager;
    private final MedicineService medicineService; // Inject service

    private final ObservableList<Medicine> medicineObservableList = FXCollections.observableArrayList();
    private static final ObservableList<String> STATUS_OPTIONS
            = FXCollections.observableArrayList("Available", "Unavailable", "Discontinued");

    // Pagination state
    private int currentPage = 0;
    private final int pageSize = 20; // Or make this configurable
    private int totalPages = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing AdminManageMedicinesController");
        statusCombo.setItems(STATUS_OPTIONS);
        setupTableColumns();
        loadMedicinesDataForCurrentPage(); // Load initial page

        medicinesTable.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newSelection) -> {
                    if (newSelection != null) {
                        populateForm(newSelection);
                        updateButton.setDisable(false);
                        deleteButton.setDisable(false);
                    } else {
                        clearForm(null);
                    }
                });
        log.info("AdminManageMedicinesController initialized successfully");
    }

   
    private void setupTableColumns() {
        medicineNameColumn.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        manufacturerColumn.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        medicinesTable.setItems(medicineObservableList);
    }

    private void loadMedicinesDataForCurrentPage() {
        try {
            log.debug("Loading medicines data for page: {} with page size: {}", currentPage, pageSize);
            Pageable pageRequest = PageRequest.of(currentPage, pageSize);
            Page<Medicine> medicinePage = medicineService.getAllMedicines(pageRequest);

            medicineObservableList.setAll(medicinePage.getContent());
            totalPages = medicinePage.getTotalPages();

            log.info("Loaded {} medicines for page {}/{}. Total medicines: {}",
                    medicinePage.getNumberOfElements(),
                    currentPage + 1, // Display 1-based page number
                    totalPages,
                    medicinePage.getTotalElements());

            updatePaginationControls();
        } catch (Exception e) {
            log.error("Error loading medicines data: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi tải dữ liệu", "Không thể tải danh sách thuốc.");
            medicineObservableList.clear();
            totalPages = 0;
            updatePaginationControls(); // Update controls even on error
        }
        clearForm(null); // Clear form and selection after loading
    }

    private void populateForm(Medicine medicine) {
        if (medicine != null) {
            medicineNameField.setText(medicine.getMedicineName());
            manufacturerField.setText(medicine.getManufacturer());
            unitField.setText(medicine.getUnit());
            descriptionField.setText(medicine.getDescription());
            priceField.setText(medicine.getPrice() != null ? medicine.getPrice().toString() : "");
            stockQuantityField.setText(String.valueOf(medicine.getStockQuantity()));
            statusCombo.setValue(medicine.getStatus() != null ? medicine.getStatus().name() : null);
            updateButton.setDisable(false);
            deleteButton.setDisable(false);
        } else {
            clearForm(null);
        }
    }

    private void updatePaginationControls() {
        if (pageInfoLabel != null) {
            pageInfoLabel.setText(String.format("Trang %d / %d", currentPage + 1, Math.max(totalPages, 1)));
        }
        if (previousPageButton != null) {
            previousPageButton.setDisable(currentPage <= 0);
        }
        if (nextPageButton != null) {
            nextPageButton.setDisable(currentPage >= totalPages - 1);
        }
        // Disable add/update/delete if no data or no selection
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        if (medicineNameField.getText() == null || medicineNameField.getText().trim().isEmpty()) {
            errors.append("- Tên thuốc không được để trống.\n");
        }
        if (manufacturerField.getText() == null || manufacturerField.getText().trim().isEmpty()) {
            errors.append("- Nhà sản xuất không được để trống.\n");
        }
        if (unitField.getText() == null || unitField.getText().trim().isEmpty()) {
            errors.append("- Đơn vị không được để trống.\n");
        }
        try {
            if (priceField.getText() != null && !priceField.getText().trim().isEmpty()) {
                BigDecimal price = new BigDecimal(priceField.getText().trim());
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    errors.append("- Giá phải là số không âm.\n");
                }
            } else {
                errors.append("- Giá không được để trống.\n");
            }
        } catch (NumberFormatException e) {
            errors.append("- Giá không hợp lệ (phải là số).\n");
        }
        try {
            if (stockQuantityField.getText() != null && !stockQuantityField.getText().trim().isEmpty()) {
                int stock = Integer.parseInt(stockQuantityField.getText().trim());
                if (stock < 0) {
                    errors.append("- Số lượng tồn phải là số không âm.\n");
                }
            } else {
                errors.append("- Số lượng tồn không được để trống.\n");
            }
        } catch (NumberFormatException e) {
            errors.append("- Số lượng tồn không hợp lệ (phải là số nguyên).\n");
        }
        if (statusCombo.getValue() == null || statusCombo.getValue().trim().isEmpty()) {
            errors.append("- Trạng thái không được để trống.\n");
        }

        if (!errors.isEmpty()) {
            DialogUtil.showWarningAlert("Thiếu thông tin hoặc dữ liệu không hợp lệ", errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    void addMedicine(ActionEvent event) {
        log.info("Add Medicine button clicked.");
        if (!validateInput()) {
            return;
        }

        Medicine newMedicine = new Medicine();
        newMedicine.setMedicineName(medicineNameField.getText().trim());
        newMedicine.setManufacturer(manufacturerField.getText().trim());
        newMedicine.setUnit(unitField.getText().trim());
        newMedicine.setDescription(descriptionField.getText().trim());
        newMedicine.setPrice(new BigDecimal(priceField.getText().trim()));
        newMedicine.setStockQuantity(Integer.parseInt(stockQuantityField.getText().trim()));
        newMedicine.setStatus(MedicineStatus.valueOf(statusCombo.getValue().toUpperCase()));
        newMedicine.setCreatedAt(LocalDateTime.now());
        newMedicine.setUpdatedAt(LocalDateTime.now());

        try {
            Medicine savedMedicine = medicineService.createMedicine(newMedicine);
            medicineObservableList.add(savedMedicine);
            medicinesTable.getSelectionModel().select(savedMedicine);
            DialogUtil.showSuccessAlert("Thành công", "Đã thêm thuốc mới thành công.");
            clearForm(null);
        } catch (IllegalArgumentException | DataIntegrityViolationException e) {
            log.error("Data integrity violation while adding medicine: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi Trùng lặp", "Không thể thêm thuốc. Tên thuốc và nhà sản xuất có thể đã tồn tại.");
        } catch (Exception e) {
            log.error("Error adding medicine: {}", e.getMessage(), e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể thêm thuốc.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void updateMedicine(ActionEvent event) {
        log.info("Update Medicine button clicked.");
        Medicine selectedMedicine = medicinesTable.getSelectionModel().getSelectedItem();
        if (selectedMedicine == null) {
            DialogUtil.showWarningAlert("Chưa chọn Thuốc", "Vui lòng chọn một loại thuốc để cập nhật.");
            return;
        }
        if (!validateInput()) {
            return;
        }

        Medicine medicineToUpdate = new Medicine(); // Tạo đối tượng mới để truyền vào service
        medicineToUpdate.setMedicineName(medicineNameField.getText().trim());
        medicineToUpdate.setManufacturer(manufacturerField.getText().trim());
        medicineToUpdate.setUnit(unitField.getText().trim());
        medicineToUpdate.setDescription(descriptionField.getText().trim());
        medicineToUpdate.setPrice(new BigDecimal(priceField.getText().trim()));
        medicineToUpdate.setStockQuantity(Integer.parseInt(stockQuantityField.getText().trim()));
        medicineToUpdate.setStatus(MedicineStatus.valueOf(statusCombo.getValue().toUpperCase()));

        try {
            Medicine updatedMedicine = medicineService.updateMedicine(selectedMedicine.getMedicineId(), medicineToUpdate);
            int index = medicineObservableList.indexOf(selectedMedicine);
            if (index != -1) {
                medicineObservableList.set(index, updatedMedicine);
                medicinesTable.getSelectionModel().select(updatedMedicine);
            } else {
                loadMedicinesDataForCurrentPage(); // Fallback
            }
            DialogUtil.showSuccessAlert("Thành công", "Đã cập nhật thông tin thuốc thành công.");
            // clearForm(null); // Form is cleared by selection listener if selection changes
        } catch (EntityNotFoundException e) {
            log.error("Medicine not found for update: {}", selectedMedicine.getMedicineId(), e);
            DialogUtil.showErrorAlert("Không tìm thấy", "Không tìm thấy thuốc để cập nhật. Có thể đã bị xóa.");
            loadMedicinesDataForCurrentPage();
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating medicine: {}", e.getMessage(), e);
            DialogUtil.showErrorAlert("Lỗi Trùng lặp", "Không thể cập nhật. Tên thuốc và nhà sản xuất mới có thể đã tồn tại.");
        } catch (Exception e) {
            log.error("Error updating medicine: {}", e.getMessage(), e);
            DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể cập nhật thuốc.", "Vui lòng thử lại sau.", e);
        }
    }

    @FXML
    void deleteMedicine(ActionEvent event) {
        log.info("Delete Medicine button clicked.");
        Medicine selectedMedicine = medicinesTable.getSelectionModel().getSelectedItem();
        if (selectedMedicine == null) {
            DialogUtil.showWarningAlert("Chưa chọn Thuốc", "Vui lòng chọn một loại thuốc để xóa.");
            return;
        }

        boolean confirmed = DialogUtil.showConfirmation("Xác nhận Xóa",
                "Bạn có chắc chắn muốn xóa thuốc '" + selectedMedicine.getMedicineName() + "' không?");
        if (confirmed) {
            try {
                medicineService.deleteMedicine(selectedMedicine.getMedicineId());
                medicineObservableList.remove(selectedMedicine);
                DialogUtil.showSuccessAlert("Thành công", "Đã xóa thuốc thành công.");
                clearForm(null);
            } catch (EntityNotFoundException e) {
                log.error("Medicine not found for deletion: {}", selectedMedicine.getMedicineId(), e);
                DialogUtil.showErrorAlert("Không tìm thấy", "Không tìm thấy thuốc để xóa. Có thể đã bị xóa.");
                loadMedicinesDataForCurrentPage();
            } catch (DataIntegrityViolationException e) {
                log.error("Cannot delete medicine due to existing references: {}", selectedMedicine.getMedicineId(), e);
                DialogUtil.showErrorAlert("Không thể Xóa", "Không thể xóa thuốc này do có các dữ liệu liên quan (ví dụ: trong đơn thuốc).");
            } catch (Exception e) {
                log.error("Error deleting medicine: {}", e.getMessage(), e);
                DialogUtil.showExceptionDialog("Lỗi Hệ thống", "Không thể xóa thuốc.", "Vui lòng thử lại sau.", e);
            }
        }
    }

    @FXML
    void clearForm(ActionEvent event) {
        log.debug("Clear form button clicked.");
        medicineNameField.clear();
        manufacturerField.clear();
        unitField.clear();
        descriptionField.clear();
        priceField.clear();
        stockQuantityField.clear();
        statusCombo.getSelectionModel().clearSelection();
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        medicinesTable.getSelectionModel().clearSelection();
    }

    // Event handlers for pagination buttons
    @FXML
    void goToPreviousPage(ActionEvent event) {
        if (currentPage > 0) {
            currentPage--;
            loadMedicinesDataForCurrentPage();
        }
    }

    @FXML
    void goToNextPage(ActionEvent event) {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadMedicinesDataForCurrentPage();
        }
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
        currentPage = 0; // Reset to first page when navigating to this screen
        loadMedicinesDataForCurrentPage();
        /* Already on this screen, refresh data */ }

    @FXML
    void loadAdminManageUserAccounts(ActionEvent event) {
        uiManager.switchToAdminManageUserAccounts();
    }

    @FXML
    void loadAdminManageDiseases(ActionEvent event) {
        uiManager.switchToAdminManageDiseases();
    }
}
