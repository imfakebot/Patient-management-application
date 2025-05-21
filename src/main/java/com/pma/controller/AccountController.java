// package com.pma.controller;

// import com.pma.model.entity.UserAccount;
// import com.pma.service.UserAccountService;
// import com.pma.util.DialogUtil;

// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.fxml.FXML;
// import javafx.scene.control.*;
// import javafx.scene.image.Image;
// import javafx.scene.image.ImageView;
// import javafx.scene.input.MouseEvent;
// import javafx.scene.layout.VBox;
// import javafx.scene.Cursor;
// import com.pma.model.enums.UserRole;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import java.util.UUID;

// @Controller
// public class AccountController {

//     @FXML
//     private TableView<UserAccount> userTable;
//     // Thêm VBox cha để disable form khi cần
//     @FXML
//     private VBox formContainer; // Đặt fx:id="formContainer" cho VBox chứa các trường nhập liệu và nút
//     @FXML
//     private TableColumn<UserAccount, UUID> userIdColumn;
//     @FXML
//     private TableColumn<UserAccount, String> usernameColumn;
//     @FXML
//     private TableColumn<UserAccount, String> roleColumn;
//     @FXML
//     private TextField usernameField;
//     @FXML
//     private PasswordField passwordField;
//     @FXML
//     private TextField textPasswordField; // Để hiển thị mật khẩu
//     @FXML
//     private ImageView togglePasswordImageView; // Icon ẩn/hiện mật khẩu
//     @FXML
//     private ComboBox<String> roleComboBox;
//     @FXML
//     private Button addButton;
//     @FXML
//     private Button updateButton;
//     @FXML
//     private Button deleteButton;

//     // Thêm ProgressIndicator và ErrorLabel
//     @FXML
//     private ProgressIndicator progressIndicator;
//     @FXML
//     private Label errorLabel;

//     @Autowired
//     private UserAccountService userAccountService;

//     private final ObservableList<UserAccount> userList = FXCollections.observableArrayList();
//     private static final Logger log = LoggerFactory.getLogger(AccountController.class);

//     private boolean isPasswordVisible = false;
//     private Image eyeOpenImage;
//     private Image eyeClosedImage;

//     private Image loadImage(String path) {
//         try {
//             return new Image(getClass().getResourceAsStream(path));
//         } catch (Exception e) {
//             log.error("Failed to load image: {}", path, e);
//             return null;
//         }
//     }

//     @FXML
//     private void initialize() {
//         eyeOpenImage = loadImage("/com/pma/img/open.png");
//         eyeClosedImage = loadImage("/com/pma/img/closed.png");

//         // Cấu hình ẩn/hiện mật khẩu
//         if (textPasswordField != null && passwordField != null && togglePasswordImageView != null) {
//             textPasswordField.setManaged(false);
//             textPasswordField.setVisible(false);
//             passwordField.setManaged(true);
//             passwordField.setVisible(true);
//             if (eyeClosedImage != null) {
//                 togglePasswordImageView.setImage(eyeClosedImage);
//             }
//             togglePasswordImageView.setCursor(Cursor.HAND);
//             textPasswordField.textProperty().bindBidirectional(passwordField.textProperty());
//         } else {
//             log.warn("FXML elements for password toggle are not fully injected. Check fx:id assignments.");
//         }

//         userIdColumn.setCellValueFactory(cellData -> cellData.getValue().userIdProperty());
//         usernameColumn.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
//         roleColumn.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
//         userTable.setItems(userList);
//         // Lấy giá trị từ Enum UserRole để điền vào ComboBox
//         for (UserRole role : UserRole.values()) {
//             roleComboBox.getItems().add(role.name());
//         }
//         loadUsers();
//         clearError();
//         hideProgress();

//         addButton.setOnAction(event -> addUser());
//         updateButton.setOnAction(event -> updateUser());
//         deleteButton.setOnAction(event -> deleteUser());

//         // Hiển thị thông tin user được chọn lên form
//         userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
//             if (newSelection != null) {
//                 populateFields(newSelection);
//             } else {
//                 clearFields();
//             }
//         });
//     }

//     private void loadUsers() {
//         userList.setAll(userAccountService.getAllUsers(null).getContent()); // Giả sử có phương thức getAllUsers
//     }

//     private void addUser() {
//         try {
//             UserAccount user = new UserAccount();
//             user.setUsername(usernameField.getText());
//             // Lấy mật khẩu từ trường đang hiển thị
//             String password = isPasswordVisible ? textPasswordField.getText() : passwordField.getText();
//             if (password.isEmpty()) {
//                 showAlert("Lỗi", "Mật khẩu không được để trống.");
//                 return;
//             }
//             user.setPasswordHash(password); // Service sẽ mã hóa
//             user.setRole(UserRole.valueOf(roleComboBox.getValue()));
//             userAccountService.createUserAccount(user); // Sử dụng createUserAccount
//             loadUsers();
//             clearFields();
//             showAlert("Thành công", "Tài khoản đã được tạo!");
//         } catch (IllegalArgumentException e) {
//             log.warn("Error adding user: {}", e.getMessage());
//             showAlert("Lỗi", e.getMessage());
//         } catch (Exception e) {
//             log.error("Unexpected error adding user", e);
//             showAlert("Lỗi", "Không thể tạo tài khoản: " + e.getMessage());
//         }
//     }

//     private void updateUser() {
//         UserAccount selected = userTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Lỗi", "Vui lòng chọn tài khoản!");
//             return;
//         }
//         try {
//             selected.setUsername(usernameField.getText());
//             // Chỉ cập nhật mật khẩu nếu người dùng nhập mật khẩu mới
//             String password = isPasswordVisible ? textPasswordField.getText() : passwordField.getText();
//             if (password != null && !password.isEmpty()) {
//                 selected.setPasswordHash(password); // Service sẽ mã hóa nếu cần
//             } else {
//                 // Nếu không nhập mật khẩu mới, không thay đổi mật khẩu cũ
//                 // Lấy lại mật khẩu hash cũ từ DB để tránh ghi đè bằng chuỗi rỗng
//                 UserAccount dbUser = userAccountService.getUserById(selected.getUserId());
//                 selected.setPasswordHash(dbUser.getPasswordHash());
//             }
//             selected.setRole(UserRole.valueOf(roleComboBox.getValue()));
//             userAccountService.updateUserAccount(selected.getUserId(), selected); // Sử dụng updateUserAccount
//             loadUsers();
//             clearFields();
//             showAlert("Thành công", "Tài khoản đã được cập nhật!");
//         } catch (Exception e) {
//             log.error("Error updating user", e);
//             showAlert("Lỗi", "Không thể cập nhật tài khoản: " + e.getMessage());
//         }
//     }

//     private void deleteUser() {
//         UserAccount selected = userTable.getSelectionModel().getSelectedItem();
//         if (selected == null) {
//             showAlert("Lỗi", "Vui lòng chọn tài khoản!");
//             return;
//         }
//         try {
//             userAccountService.deleteUserAccount(selected.getUserId()); // Sử dụng deleteUserAccount
//             loadUsers();
//             clearFields();
//             showAlert("Thành công", "Tài khoản đã được xóa!");
//         } catch (Exception e) {
//             log.error("Error deleting user", e);
//             showAlert("Lỗi", "Không thể xóa tài khoản: " + e.getMessage());
//         }
//     }

//     private void clearFields() {
//         usernameField.clear();
//         passwordField.clear();
//         roleComboBox.setValue(null);
//         if (textPasswordField != null) {
//             textPasswordField.clear(); // Cũng xóa text field
//         }
//     }

//     private void showAlert(String title, String message) {
//         Alert alert = new Alert(Alert.AlertType.INFORMATION);
//         alert.setTitle(title);
//         alert.setHeaderText(null);
//         alert.setContentText(message);
//         alert.showAndWait();
//     }

//     private void populateFields(UserAccount user) {
//         usernameField.setText(user.getUsername());
//         // Không hiển thị mật khẩu cũ
//         passwordField.clear();
//         if (textPasswordField != null) {
//             textPasswordField.clear();
//         }
//         roleComboBox.setValue(user.getRole().name());
//     }

//     @FXML
//     private void togglePasswordVisibility(MouseEvent event) {
//         if (passwordField == null || textPasswordField == null || togglePasswordImageView == null || eyeOpenImage == null || eyeClosedImage == null) {
//             log.warn("Cannot toggle password visibility, FXML elements or images are not initialized.");
//             return;
//         }
//         isPasswordVisible = !isPasswordVisible;
//         Image currentImage = isPasswordVisible ? eyeOpenImage : eyeClosedImage;

//         if (isPasswordVisible) {
//             // textPasswordField đã được bind với passwordField, không cần copy thủ công
//             // chỉ cần quản lý visibility và managed state
//             textPasswordField.setManaged(true);
//             textPasswordField.setVisible(true);
//             passwordField.setManaged(false);
//             passwordField.setVisible(false);
//             togglePasswordImageView.setImage(currentImage);
//             textPasswordField.requestFocus();
//             textPasswordField.positionCaret(textPasswordField.getText().length());
//         } else {
//             passwordField.setManaged(true);
//             passwordField.setVisible(true);
//             textPasswordField.setManaged(false);
//             textPasswordField.setVisible(false);
//             togglePasswordImageView.setImage(currentImage);
//             passwordField.requestFocus();
//             passwordField.positionCaret(passwordField.getText().length());
//         }
//     }

//     private void showError(String message) {
//         if (errorLabel != null) {
//             errorLabel.setText(message);
//             errorLabel.setVisible(true);
//         } else {
//             DialogUtil.showErrorAlert("Lỗi", message);
//         }
//     }

//     private void clearError() {
//         if (errorLabel != null) {
//             errorLabel.setText("");
//             errorLabel.setVisible(false);
//         }
//     }

//     private void showProgress() {
//         if (progressIndicator != null) {
//             progressIndicator.setVisible(true);
//         }
//         if (formContainer != null) {
//             formContainer.setDisable(true);
//         }
//     }

//     private void hideProgress() {
//         if (progressIndicator != null) {
//             progressIndicator.setVisible(false);
//         }
//         if (formContainer != null) {
//             formContainer.setDisable(false);
//         }
//     }

//     // Cập nhật setFormDisabled để sử dụng formContainer
//     private void setFormDisabled(boolean disabled) {
//         if (formContainer != null) {
//             formContainer.setDisable(disabled);
//         }
//         // Đảm bảo nút cũng được disable/enable đúng cách cùng với progress
//         if (addButton != null && progressIndicator != null && progressIndicator.isVisible()) {
//             addButton.setDisable(true); // Ví dụ cho nút add, làm tương tự cho update, delete
//         } else if (addButton != null) {
//             addButton.setDisable(disabled);
//         }
//     }
// }
