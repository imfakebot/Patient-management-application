package com.pma.controller;

import com.pma.model.entity.UserAccount;
import com.pma.service.UserAccountService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class AccountController {

    @FXML private TableView<UserAccount> userTable;
    @FXML private TableColumn<UserAccount, UUID> userIdColumn;
    @FXML private TableColumn<UserAccount, String> usernameColumn;
    @FXML private TableColumn<UserAccount, String> roleColumn;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    @Autowired
    private UserAccountService userAccountService;

    private ObservableList<UserAccount> userList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        userIdColumn.setCellValueFactory(cellData -> cellData.getValue().userIdProperty());
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        roleColumn.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        userTable.setItems(userList);
        roleComboBox.getItems().addAll("ADMIN", "DOCTOR", "STAFF");
        loadUsers();

        addButton.setOnAction(event -> addUser());
        updateButton.setOnAction(event -> updateUser());
        deleteButton.setOnAction(event -> deleteUser());
    }

    private void loadUsers() {
        userList.setAll(userAccountService.loadUserByUsername(null).getContent());
    }

    private void addUser() {
        try {
            UserAccount user = new UserAccount();
            user.setUsername(usernameField.getText());
            user.setPassword(passwordField.getText());
            user.setRole(roleComboBox.getValue());
            userAccountService.createUser(user);
            loadUsers();
            clearFields();
            showAlert("Thành công", "Tài khoản đã được tạo!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể tạo tài khoản: " + e.getMessage());
        }
    }

    private void updateUser() {
        UserAccount selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn tài khoản!");
            return;
        }
        try {
            selected.setUsername(usernameField.getText());
            selected.setPassword(passwordField.getText());
            selected.setRole(roleComboBox.getValue());
            userAccountService.updateUser(selected.getUserId(), selected);
            loadUsers();
            clearFields();
            showAlert("Thành công", "Tài khoản đã được cập nhật!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể cập nhật tài khoản: " + e.getMessage());
        }
    }

    private void deleteUser() {
        UserAccount selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn tài khoản!");
            return;
        }
        try {
            userAccountService.deleteUser(selected.getUserId());
            loadUsers();
            clearFields();
            showAlert("Thành công", "Tài khoản đã được xóa!");
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể xóa tài khoản: " + e.getMessage());
        }
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        roleComboBox.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}