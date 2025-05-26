package com.pma.util;

import java.io.IOException; // Lớp Application chính
import java.net.URL;
import java.util.Objects;
import java.util.UUID; // Added for controller initialization
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Để đặt icon cho cửa sổ
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.pma.App;
import com.pma.controller.ResetPasswordController; // Added for ResetPasswordScreen
import com.pma.controller.TwoFactorAuthController;
import com.pma.controller.TwoFactorSetupController;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Quản lý việc tải và chuyển đổi giữa các màn hình (Scenes) trong ứng dụng
 * JavaFX. Tích hợp với Spring Context để tạo Controller là Spring Beans.
 */
@Component
public class UIManager {

    private static final Logger log = LoggerFactory.getLogger(UIManager.class);
    private Stage primaryStage;
    private Scene mainScene; // Có thể giữ tham chiếu đến scene chính để thay đổi root

    // Cache các FXML đã load để tăng tốc (tùy chọn)
    // private final Map<String, Parent> viewCache = new HashMap<>();
    @Autowired
    private ApplicationContext springContext; // Spring Application Context

    /**
     * Thiết lập Stage chính cho UIManager. Nên được gọi một lần từ phương thức
     * start() của lớp Application.
     *
     * @param primaryStage Stage chính của ứng dụng.
     */
    public void initializePrimaryStage(Stage primaryStage) {
        Objects.requireNonNull(primaryStage, "Primary stage cannot be null");
        this.primaryStage = primaryStage;
        // Thiết lập icon cho ứng dụng (ví dụ)
        try {
            this.primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("/com/pma/img/app_icon.png"))); // Thay bằng icon của bạn
        } catch (Exception e) {
            log.warn("Could not load application icon.", e);
        }
    }

    /**
     * Chuyển đến màn hình Login.
     */
    public void switchToLoginScreen() {
        log.info("Switching to Login Screen");
        loadAndSetScene("/com/pma/fxml/login.fxml", "PMA - Login", 600, 450, false);
    }

    /**
     * Chuyển đến màn hình Doctor Dashboard.
     */
    public void switchToDoctorDashboard() {
        log.info("Switching to Doctor Dashboard Screen");
        loadAndSetScene("/com/pma/view/dashboard.fxml", "PMA - Dashboard", 1200, 800, true); // Ví dụ kích thước, cho phép resize
        // Có thể maximize cửa sổ sau khi vào màn hình chính
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    /**
     * Chuyển đến màn hình Admin Dashboard.
     */
    public void switchToAdminDashboard() {
        log.info("Switching to Admin Dashboard Screen");
        // Giả sử file FXML của bạn là "admin_dashboard.fxml" hoặc một tên tương tự
        loadAndSetScene("/com/pma/fxml/admin_manage_patients.fxml", "PMA - Admin Dashboard", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    // Thêm các phương thức chuyển màn hình cụ thể khác ở đây
    // public void switchToPatientManagementScreen() {
    //     loadAndSetScene("/com/pma/view/PatientManagementView.fxml", "PMA - Patient Management", 1000, 700, true);
    // }
    /**
     * Mở một cửa sổ dialog (modal) mới.
     *
     * @param fxmlPath Đường dẫn đến file FXML của dialog.
     * @param title Tiêu đề của dialog.
     * @param owner Stage sở hữu dialog này (dialog sẽ là modal đối với owner).
     * @return Controller của dialog đã được load (nếu cần tương tác).
     */
    public <T> T openModalDialog(String fxmlPath, String title, Stage owner) {
        log.info("Opening modal dialog: {} with title: {}", fxmlPath, title);
        if (springContext == null) {
            log.error("Spring ApplicationContext is not initialized in UIManager. Cannot open modal dialog: {}", fxmlPath);
            DialogUtil.showErrorAlert("Critical Error", "Application context not available. Cannot open dialog.");
            return null;
        }

        URL fxmlUrl = null;
        try {
            fxmlUrl = App.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                log.error("FXML file not found for modal dialog at path: {}", fxmlPath);
                DialogUtil.showErrorAlert("Configuration Error", "Cannot find FXML for dialog: " + fxmlPath);
                return null;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(springContext::getBean); // Sử dụng Spring để tạo controller

            Parent dialogRoot = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL); // Chặn tương tác với cửa sổ owner
            dialogStage.initOwner(owner != null ? owner : this.primaryStage); // Set owner (nếu có)
            dialogStage.initStyle(StageStyle.UTILITY); // Kiểu cửa sổ đơn giản hơn

            Scene dialogScene = new Scene(dialogRoot);
            // Có thể thêm CSS cho dialog nếu cần
            // dialogScene.getStylesheets().add(App.class.getResource("/com/pma/css/dialog_style.css").toExternalForm());
            dialogStage.setScene(dialogScene);

            // Thiết lập icon cho dialog (giống cửa sổ chính)
            if (primaryStage != null && !primaryStage.getIcons().isEmpty()) {
                dialogStage.getIcons().add(primaryStage.getIcons().get(0));
            }

            T controller = loader.getController(); // Lấy controller sau khi load
            if (controller == null && !fxmlPath.contains("some_controllerless_dialog.fxml")) { // Example condition
                log.warn("Controller for modal dialog FXML {} was null after loading.", fxmlPath);
                // Depending on whether a controller is always expected, this might be an error.
            }

            dialogStage.showAndWait(); // Hiển thị và chờ cho đến khi dialog đóng
            return controller; // Trả về controller của dialog
        } catch (IOException e) {
            log.error("Failed to load modal dialog FXML: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load dialog.", "FXML: " + fxmlPath, e);
            return null;
        } catch (Exception e) { // Catch any other unexpected errors
            log.error("Unexpected error opening modal dialog for FXML {}:", fxmlPath, e);
            DialogUtil.showExceptionDialog("Unexpected UI Error",
                    "An unexpected error occurred while opening the dialog.",
                    "FXML: " + fxmlPath + ", Error: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Tải FXML, đặt nó làm root cho Scene chính, và tùy chọn khởi tạo
     * controller.
     *
     * @param fxmlPath Đường dẫn đến file FXML.
     * @param title Tiêu đề cửa sổ.
     * @param preferredWidth Chiều rộng mong muốn.
     * @param preferredHeight Chiều cao mong muốn.
     * @param resizable Cho phép thay đổi kích thước cửa sổ hay không.
     * @param controllerInitializer Hàm để khởi tạo controller sau khi load (có
     * thể null).
     *
     */
    public void loadAndSetScene(String fxmlPath, String title, double preferredWidth, double preferredHeight, boolean resizable) {
        if (primaryStage == null) {
            log.error("Primary stage is not initialized in UIManager.");
            throw new IllegalStateException("Primary stage must be initialized before switching scenes.");
        }
        if (springContext == null) {
            log.error("Spring ApplicationContext is not initialized in UIManager. Cannot load FXML: {}", fxmlPath);
            DialogUtil.showErrorAlert("Critical Error", "Application context not available. Cannot switch scenes.");
            return;
        }

        URL fxmlUrl = null;
        try {
            fxmlUrl = App.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                log.error("FXML file not found at path: {}", fxmlPath);
                DialogUtil.showErrorAlert("Configuration Error", "Cannot find FXML: " + fxmlPath);
                return;
            }
            log.info("Loading FXML from: {}", fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            // QUAN TRỌNG: Để Spring quản lý việc tạo Controller (nếu Controller là Spring Bean)
            loader.setControllerFactory(springContext::getBean);

            Parent rootNode = loader.load();

            // Khởi tạo controller nếu có initializer được cung cấp
            // This part is moved to the new overloaded method to avoid type issues with a generic Consumer here.
            // Instead, the specific methods calling this will handle controller retrieval and initialization if needed.
            // For a generic solution, we'd need to make this method generic or pass Object and cast.
            // For now, let's keep it simple and handle controller init in specific switch methods or a new generic one.
            // The new overloaded method below will handle this.
            // The current method will now call the overloaded one.
            // This specific method is kept for calls that don't need controller initialization.
            // However, to make it truly flexible, we should have one core method.
            // Let's refactor: create one core method and have this one call it.
            // Tạo hiệu ứng fade khi chuyển scene (tùy chọn)
            // applyFadeTransition(rootNode);
            // Nếu mainScene chưa được tạo, hoặc nếu chúng ta muốn mỗi màn hình có kích thước riêng
            // thì tạo Scene mới. Nếu không, chỉ thay đổi root.
            // Hiện tại, logic này tạo Scene mới nếu chưa có, sau đó chỉ thay root và kích thước.
            if (mainScene == null) {
                mainScene = new Scene(rootNode, preferredWidth, preferredHeight);
                // Có thể thêm CSS chung cho ứng dụng ở đây
                // mainScene.getStylesheets().add(App.class.getResource("/com/pma/css/global_style.css").toExternalForm());
                primaryStage.setScene(mainScene);
            } else {
                mainScene.setRoot(rootNode); // Thay đổi root của scene hiện tại
                // Điều chỉnh kích thước cửa sổ cho scene mới
                primaryStage.setWidth(preferredWidth);
                primaryStage.setHeight(preferredHeight);
            }

            primaryStage.setTitle(title);

            primaryStage.setResizable(resizable);

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }

            primaryStage.centerOnScreen(); // Căn giữa sau khi đặt kích thước

            log.info("Scene switched to: {} with title: {}", fxmlPath, title);

        } catch (IOException e) { // Catches errors from loader.load()
            log.error("Failed to load FXML scene: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load the screen.", "FXML: " + fxmlPath, e);
        } catch (Exception e) { // Catch any other unexpected errors
            log.error("Unexpected error loading scene for FXML {}:", fxmlPath, e);
            DialogUtil.showExceptionDialog("Unexpected UI Error",
                    "An unexpected error occurred while loading the screen.",
                    "FXML: " + fxmlPath + ", Error: " + e.getMessage(), e);
        }
    }

    /**
     * Tải FXML, đặt nó làm root cho Scene chính, và tùy chọn khởi tạo
     * controller.
     *
     * @param fxmlPath Đường dẫn đến file FXML.
     * @param title Tiêu đề cửa sổ.
     * @param preferredWidth Chiều rộng mong muốn.
     * @param preferredHeight Chiều cao mong muốn.
     * @param resizable Cho phép thay đổi kích thước cửa sổ hay không.
     * @param controllerInitializer Hàm để khởi tạo controller sau khi load (có
     * thể null).
     * @param <T> Kiểu của controller.
     */
    public <T> void loadAndSetScene(String fxmlPath, String title, double preferredWidth, double preferredHeight, boolean resizable, Consumer<T> controllerInitializer) {
        if (primaryStage == null) {
            log.error("Primary stage is not initialized in UIManager.");
            throw new IllegalStateException("Primary stage must be initialized before switching scenes.");
        }
        if (springContext == null) {
            log.error("Spring ApplicationContext is not initialized in UIManager. Cannot load FXML: {}", fxmlPath);
            DialogUtil.showErrorAlert("Critical Error", "Application context not available. Cannot switch scenes.");
            return;
        }

        URL fxmlUrl = null;
        try {
            fxmlUrl = App.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                log.error("FXML file not found at path: {}", fxmlPath);
                DialogUtil.showErrorAlert("Configuration Error", "Cannot find FXML: " + fxmlPath);
                return;
            }
            log.info("Loading FXML from: {}", fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(springContext::getBean);

            Parent rootNode = loader.load();

            if (controllerInitializer != null) {
                T controller = loader.getController();
                if (controller != null) {
                    controllerInitializer.accept(controller);
                } else {
                    log.warn("Controller not found for FXML: {} while trying to initialize.", fxmlPath);
                }
            }

            if (mainScene == null) {
                mainScene = new Scene(rootNode, preferredWidth, preferredHeight);
                primaryStage.setScene(mainScene);
            } else {
                mainScene.setRoot(rootNode);
                primaryStage.setWidth(preferredWidth);
                primaryStage.setHeight(preferredHeight);
            }
            primaryStage.setTitle(title);
            primaryStage.setResizable(resizable);
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
            primaryStage.centerOnScreen();
            log.info("Scene switched to: {} with title: {}", fxmlPath, title);
        } catch (IOException | IllegalStateException e) {
            log.error("Failed to load or set FXML scene: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load the screen.", "FXML: " + fxmlPath, e);
        }
    }

    /**
     *
     * Chuyển đến màn hình xác thực hai yếu tố (2FA).
     *
     * @param username Tên người dùng đang xác thực
     * @param preAuthToken Token xác thực đã qua bước 1 (username/password)
     * @param infoMessage Thông báo hiển thị cho người dùng trên màn hình 2FA.
     */
    public void switchToTwoFactorAuthScreen(String username, Authentication preAuthToken, String infoMessage) {
        log.info("Switching to Two-Factor Authentication Screen for user: {}", username);
        if (primaryStage == null) {
            log.error("Primary stage is not initialized in UIManager. Cannot switch to 2FA screen.");
            DialogUtil.showErrorAlert("Critical Error", "UI Manager not properly initialized.");
            return;
        }
        if (springContext == null) {
            log.error("Spring ApplicationContext is not initialized in UIManager. Cannot load 2FA screen.");
            DialogUtil.showErrorAlert("Critical Error", "Application context not available.");
            return;
        }

        String fxmlPathForAuth = "/com/pma/fxml/TwoFactorAuthView.fxml"; // Correct FXML for OTP/2FA verification
        URL fxmlUrl = null;
        try {
            fxmlUrl = App.class.getResource(fxmlPathForAuth);
            if (fxmlUrl == null) {
                log.error("2FA FXML file not found at path: {}", fxmlPathForAuth);
                DialogUtil.showErrorAlert("Configuration Error", "Cannot find 2FA FXML: " + fxmlPathForAuth);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(springContext::getBean);

            Parent rootNode = loader.load();

            TwoFactorAuthController controller = loader.getController();
            if (controller == null) {
                log.error("Failed to get controller for 2FA FXML: {}", fxmlPathForAuth);
                DialogUtil.showErrorAlert("UI Load Error", "Could not initialize 2FA screen controller.");
                return;
            }
            controller.initData(username, preAuthToken, infoMessage);

            // Sử dụng lại logic loadAndSetScene chung, nhưng vì controller.initData đã được gọi,
            // chúng ta chỉ cần đảm bảo scene được set đúng cách.
            // Thay vì gọi loadAndSetScene trực tiếp (vì nó sẽ load lại FXML), ta set root và stage props.
            setupPrimaryStageForScene(rootNode, "PMA - Two-Factor Authentication", 400, 350, false);

        } catch (IOException e) {
            log.error("Failed to load 2FA FXML scene: " + fxmlPathForAuth, e);
            DialogUtil.showExceptionDialog("UI Load Error",
                    "Could not load the 2FA screen.",
                    "FXML: " + fxmlPathForAuth, e);
        } catch (Exception e) { // Catch any other unexpected errors
            log.error("Unexpected error switching to 2FA screen for FXML {}:", fxmlPathForAuth, e);
            DialogUtil.showExceptionDialog("Unexpected UI Error",
                    "An unexpected error occurred while trying to show the 2FA screen.",
                    "Details: " + e.getMessage(), e);
        }
    }

    private void setupPrimaryStageForScene(Parent rootNode, String title, double width, double height, boolean resizable) {
        if (mainScene == null) {
            mainScene = new Scene(rootNode, width, height);
            primaryStage.setScene(mainScene);
        } else {
            mainScene.setRoot(rootNode);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
        }
        primaryStage.setTitle(title);
        primaryStage.setResizable(resizable);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        primaryStage.centerOnScreen();
    }

    /**
     * Chuyển đến màn hình Register.
     */
    public void switchToRegisterScreen() {
        log.info("Switching to Register Screen");
        double desiredWidth = 1200;
        double desiredHeight = 700;
        loadAndSetScene("/com/pma/fxml/register.fxml", "PMA - Đăng ký tài khoản", desiredWidth, desiredHeight, false, null);
    }

    /**
     * Áp dụng hiệu ứng Fade In đơn giản cho một Node.
     *
     * @param node Node cần áp dụng hiệu ứng.
     */
    private void applyFadeTransition(Parent node) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    /**
     * Lấy Stage chính của ứng dụng.
     *
     * @return Stage chính.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public boolean show2FASetupDialog(UUID userId) {
        String fxmlPath = "/com/pma/fxml/TwoFactorSetupView.fxml"; // Đường dẫn đến FXML của bạn
        String title = "Set Up Two-Factor Authentication";
        log.info("Opening 2FA Setup dialog for user ID: {}", userId);

        if (springContext == null) {
            log.error("Spring ApplicationContext is not initialized in UIManager. Cannot open 2FA setup dialog.");
            DialogUtil.showErrorAlert("Critical Error", "Application context not available.");
            return false;
        }

        URL fxmlUrl = null;
        try {
            fxmlUrl = App.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                log.error("FXML file not found for 2FA Setup dialog at path: {}", fxmlPath);
                DialogUtil.showErrorAlert("Configuration Error", "Cannot find FXML for 2FA Setup: " + fxmlPath);
                return false;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(springContext::getBean); // Sử dụng Spring để tạo controller

            Parent dialogRoot = loader.load();
            TwoFactorSetupController controller = loader.getController(); // Lấy controller
            if (controller == null) {
                log.error("Failed to get controller for FXML: {}", fxmlPath);
                DialogUtil.showErrorAlert("UI Error", "Could not initialize 2FA setup screen controller.");
                return false;
            }

            controller.initData(userId); // Truyền userId cho controller

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(this.primaryStage);
            dialogStage.initStyle(StageStyle.UTILITY);

            Scene dialogScene = new Scene(dialogRoot);
            // Bạn có thể thêm CSS cho dialog nếu cần
            // dialogScene.getStylesheets().add(App.class.getResource("/com/pma/css/dialog_style.css").toExternalForm());
            dialogStage.setScene(dialogScene);

            if (primaryStage != null && !primaryStage.getIcons().isEmpty()) {
                dialogStage.getIcons().add(primaryStage.getIcons().get(0));
            }

            dialogStage.showAndWait(); // Hiển thị và chờ

            return controller.isSetupSuccessful(); // Lấy kết quả từ controller
        } catch (IOException e) {
            log.error("Failed to load 2FA Setup dialog FXML: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load 2FA setup screen.", "FXML: " + fxmlPath, e);
            return false;
        } catch (Exception e) { // Catch any other unexpected errors
            log.error("Unexpected error opening 2FA setup dialog for FXML {}:", fxmlPath, e);
            DialogUtil.showExceptionDialog("Unexpected UI Error",
                    "An unexpected error occurred while opening the 2FA setup dialog.", "FXML: " + fxmlPath + ", Error: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Điều hướng người dùng sau khi đăng nhập thành công, dựa trên vai trò của
     * họ.
     *
     * @param authentication Đối tượng Authentication chứa thông tin người dùng
     * và vai trò.
     */
    public void navigateAfterLogin(Authentication authentication) {
        Objects.requireNonNull(authentication, "Authentication object cannot be null for navigation.");
        String username = authentication.getName();

        // Giả sử vai trò trong Spring Security có tiền tố "ROLE_" (ví dụ: "ROLE_ADMIN", "ROLE_PATIENT")
        // và UserRole enum của bạn (com.pma.model.enums.UserRole) có các giá trị như ADMIN, PATIENT, DOCTOR.
        if (authentication.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_ADMIN".equals(ga.getAuthority()))) {
            log.info("User {} is ADMIN, navigating to admin dashboard.", username);
            switchToAdminDashboard(); // Chuyển đến màn hình admin
            // switchToMainDashboard(); // Xóa hoặc comment dòng này

        } else if (authentication.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_DOCTOR".equals(ga.getAuthority()))) {
            log.info("User {} is DOCTOR, navigating to doctor dashboard.", username);
            switchToDoctorDashboard(); // Chuyển đến màn hình dashboard của bác sĩ

        } else if (authentication.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_PATIENT".equals(ga.getAuthority()))) {
            log.info("User {} is PATIENT, navigating to patient dashboard.", username);
            switchToPatientDashboard(); // Chuyển đến màn hình dashboard của bệnh nhân
        } else {
            log.warn("User {} has no recognized role or no specific dashboard. Navigating to default main dashboard.", username);
            switchToDoctorDashboard(); // Hoặc một màn hình mặc định khác nếu cần
        }
        // Có thể maximize cửa sổ sau khi vào màn hình chính
        if (primaryStage != null && !primaryStage.isMaximized() && primaryStage.isShowing()) {
            primaryStage.setMaximized(true);
        }
    }

    /**
     * Chuyển đến màn hình Patient Dashboard.
     */
    private void switchToPatientDashboard() {
        log.info("Switching to Patient Dashboard Screen");
        loadAndSetScene("/com/pma/fxml/patient_dashboard.fxml", "PMA - Patient Dashboard", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    /**
     * Chuyển đến màn hình yêu cầu đặt lại mật khẩu.
     */
    public void switchToForgotPasswordScreen() {
        log.info("Navigating to Forgot Password Screen."); // Updated log message
        loadAndSetScene("/com/pma/fxml/forgot_password_request.fxml", "Yêu cầu đặt lại mật khẩu", 450, 400, false, null); // Corrected FXML path
    }

    /**
     * Chuyển đến màn hình đặt lại mật khẩu.
     *
     * @param username Tên người dùng (hoặc email) để đặt lại mật khẩu.
     */
    public void switchToResetPasswordScreen(String username) {
        log.info("Navigating to Reset Password Screen for user: {}", username);
        loadAndSetScene("/com/pma/fxml/reset_password.fxml", "Đặt lại mật khẩu", 450, 550, false,
                (ResetPasswordController controller) -> controller.initData(username));
    }

    public void switchToAdminViewRevenue() {
        log.info("Switching to Admin View Revenue Screen");
        loadAndSetScene("/com/pma/fxml/admin_view_revenue.fxml", "PMA - Admin View Revenue", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    public void switchToAdminManageDoctors() {
        log.info("Switching to Admin Manage Doctors Screen");
        loadAndSetScene("/com/pma/fxml/admin_manage_doctors.fxml", "PMA - Admin Manage Doctors", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    public void switchToAdminManagePatients() {
        log.info("Switching to Admin Manage Patients Screen");
        loadAndSetScene("/com/pma/fxml/admin_manage_patients.fxml", "PMA - Admin Manage Patients", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    public void switchToAdminManageDepartments() {
        log.info("Switching to Admin Manage Departments Screen");
        loadAndSetScene("/com/pma/fxml/admin_manage_departments.fxml", "PMA - Admin Manage Departments", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    public void switchToAdminManageMedicines() {
        log.info("Switching to Admin Manage Medicines Screen");
        loadAndSetScene("/com/pma/fxml/admin_manage_medicines.fxml", "PMA - Admin Manage Medicines", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    public void switchToAdminManageUserAccounts() {
        log.info("Switching to Admin Manage User Accounts Screen");
        loadAndSetScene("/com/pma/fxml/admin_manage_user_accounts.fxml", "PMA - Admin Manage User Accounts", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    public void switchToAdminManageDiseases() {
        log.info("Switching to Admin Manage Diseases Screen");
        loadAndSetScene("/com/pma/fxml/admin_manage_diseases.fxml", "PMA - Admin Manage Diseases", 1200, 800, true);
        if (primaryStage != null && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }
}
