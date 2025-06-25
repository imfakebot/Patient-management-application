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
import javafx.application.Platform;
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
        try {
            this.primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("/com/pma/img/app_icon.png")));
        } catch (Exception e) {
            log.warn("Could not load application icon.", e);
        }
    }

    /**
     * Chuyển đến màn hình Login.
     */
    public void switchToLoginScreen() {
        log.info("Switching to Login Screen");
        // Login screen should not be maximized by default.
        loadAndSetScene("/com/pma/fxml/login.fxml", "PMA - Login", 600, 450, false, (Consumer<Object>) null, false);
    }

    // Helper method that dashboard-like screens call to ensure maximization.
    private <T> void setSceneAndMaximize(String fxmlPath, String title, double preferredWidth, double preferredHeight, boolean resizable, Consumer<T> controllerInitializer) {
        loadAndSetScene(fxmlPath, title, preferredWidth, preferredHeight, resizable, controllerInitializer, true);
    }

    public void switchToDoctorDashboard() {
        log.info("Switching to Doctor Dashboard Screen");
        setSceneAndMaximize("/com/pma/fxml/dashboard.fxml", "PMA - Dashboard", 1200, 800, true, null);
    }

    public void switchToAdminDashboard() {
        log.info("Switching to Admin Dashboard Screen");
        setSceneAndMaximize("/com/pma/fxml/admin_manage_patients.fxml", "PMA - Admin Dashboard", 1200, 800, true, null);
    }

    public void switchToPatientBookAppointment(UUID patientId) {
        log.info("Switching to Patient Book Appointment Screen for patient ID: {}", patientId);
        setSceneAndMaximize("/com/pma/fxml/patient_book_appointment.fxml",
                "PMA - Patient Book Appointment",
                1200, 800, true,
                (com.pma.controller.patient.PatientBookAppointmentController controller) -> controller.initData(patientId)
        );
    }

    public void switchToPatientViewPrescriptions() {
        log.info("Switching to Patient View Prescriptions Screen");
        setSceneAndMaximize("/com/pma/fxml/patient_view_prescriptions.fxml", "PMA - Patient View Prescriptions", 1200, 800, true, null);
    }

    public void switchToPatientMedicalHistory() {
        log.info("Switching to Patient Medical History Screen");
        setSceneAndMaximize("/com/pma/fxml/patient_medical_history.fxml", "PMA - Patient Medical History", 1200, 800, true, null);
    }

    public void switchToPatientUpdateProfile() {
        log.info("Switching to Patient Update Profile Screen");
        setSceneAndMaximize("/com/pma/fxml/patient_update_profile.fxml", "PMA - Patient UpdateProfile", 1200, 800, true, null);
    }

    public void switchToPatientReview() {
        log.info("Switching to Patient Review Screen");
        setSceneAndMaximize("/com/pma/fxml/patient_review.fxml", "PMA - Patient Review", 1200, 800, true, null);
    }

    public void switchToPatientViewBills() {
        log.info("Switching to Patient View Bills Screen");
        setSceneAndMaximize("/com/pma/fxml/patient_view_bills.fxml", "PMA - Patient ViewBills", 1200, 800, true, null);
    }

    public void switchToDoctorViewPatients() {
        log.info("Switching to Doctor View Patients Screen");
        setSceneAndMaximize("/com/pma/fxml/doctor_view_patients.fxml", "PMA - Doctor View Patients", 1200, 800, true, null);
    }

    public void switchToDoctorMedicalRecords() {
        log.info("Switching to Doctor Medical Records Screen");
        setSceneAndMaximize("/com/pma/fxml/doctor_medical_records.fxml", "PMA - Doctor Medical Records", 1200, 800, true, null);
    }

    public void switchToDoctorPrescribe() {
        log.info("Switching to Doctor Prescribe Screen");
        setSceneAndMaximize("/com/pma/fxml/doctor_prescribe.fxml", "PMA - Doctor Prescribe", 1200, 800, true, null);
    }

    public void switchToDoctorBookAppointment() {
        log.info("Switching to Doctor Book Appointment Screen");
        setSceneAndMaximize("/com/pma/fxml/doctor_book_appointment.fxml", "PMA - Doctor Book Appointment", 1200, 800, true, null);
    }

    public void switchToAdminViewRevenue() {
        log.info("Switching to Admin View Revenue Screen");
        setSceneAndMaximize("/com/pma/fxml/admin_view_revenue.fxml", "PMA - Admin View Revenue", 1200, 800, true, null);
    }

    public void switchToAdminManageDoctors() {
        log.info("Switching to Admin Manage Doctors Screen");
        setSceneAndMaximize("/com/pma/fxml/admin_manage_doctors.fxml", "PMA - Admin Manage Doctors", 1200, 800, true, null);
    }

    public void switchToAdminManagePatients() {
        log.info("Switching to Admin Manage Patients Screen");
        setSceneAndMaximize("/com/pma/fxml/admin_manage_patients.fxml", "PMA - Admin Manage Patients", 1200, 800, true, null);
    }

    public void switchToAdminManageDepartments() {
        log.info("Switching to Admin Manage Departments Screen");
        setSceneAndMaximize("/com/pma/fxml/admin_manage_departments.fxml", "PMA - Admin Manage Departments", 1200, 800, true, null);
    }

    public void switchToAdminManageMedicines() {
        log.info("Switching to Admin Manage Medicines Screen");
        setSceneAndMaximize("/com/pma/fxml/admin_manage_medicines.fxml", "PMA - Admin Manage Medicines", 1200, 800, true, null);
    }

    public void switchToAdminManageUserAccounts() {
        log.info("Switching to Admin Manage User Accounts Screen");
        setSceneAndMaximize("/com/pma/fxml/admin_manage_user_accounts.fxml", "PMA - Admin Manage User Accounts", 1200, 800, true, null);
    }

    public void switchToAdminManageDiseases() {
        log.info("Switching to Admin Manage Diseases Screen");
        setSceneAndMaximize("/com/pma/fxml/admin_manage_diseases.fxml", "PMA - Admin Manage Diseases", 1200, 800, true, null);
    }

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
            loader.setControllerFactory(springContext::getBean);

            Parent dialogRoot = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(owner != null ? owner : this.primaryStage);
            dialogStage.initStyle(StageStyle.UTILITY);

            Scene dialogScene = new Scene(dialogRoot);
            dialogStage.setScene(dialogScene);

            if (primaryStage != null && !primaryStage.getIcons().isEmpty()) {
                dialogStage.getIcons().add(primaryStage.getIcons().get(0));
            }

            T controller = loader.getController();
            if (controller == null && !fxmlPath.contains("some_controllerless_dialog.fxml")) {
                log.warn("Controller for modal dialog FXML {} was null after loading.", fxmlPath);
            }

            dialogStage.showAndWait();
            return controller;
        } catch (IOException e) {
            log.error("Failed to load modal dialog FXML: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load dialog.", "FXML: " + fxmlPath, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error opening modal dialog for FXML {}:", fxmlPath, e);
            DialogUtil.showExceptionDialog("Unexpected UI Error",
                    "An unexpected error occurred while opening the dialog.",
                    "FXML: " + fxmlPath + ", Error: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Overload for screens that don't need controller initialization or
     * specific maximization behavior by default. Kept for compatibility, but
     * most screen switches should use the more specific version or the
     * setSceneAndMaximize helper.
     */
    public void loadAndSetScene(String fxmlPath, String title, double preferredWidth, double preferredHeight, boolean resizable) {
        loadAndSetScene(fxmlPath, title, preferredWidth, preferredHeight, resizable, (Consumer<Object>) null, false);
    }

    private <T> void loadAndSetScene(String fxmlPath, String title, double preferredWidth, double preferredHeight, boolean resizable, Consumer<T> controllerInitializer, boolean attemptMaximize) {
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
            }

            primaryStage.setTitle(title);
            primaryStage.setResizable(resizable);

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }

            if (attemptMaximize) {
                Runnable maximizeAction = () -> {
                    if (primaryStage != null) {
                        primaryStage.setMaximized(true);
                    }
                };
                if (Platform.isFxApplicationThread()) {
                    maximizeAction.run();
                } else {
                    Platform.runLater(maximizeAction);
                }
            } else {
                if (primaryStage.isMaximized()) {
                    primaryStage.setMaximized(false);
                }
                primaryStage.setWidth(preferredWidth);
                primaryStage.setHeight(preferredHeight);
                primaryStage.centerOnScreen();
            }
            log.info("Scene switched to: {} with title: {}", fxmlPath, title);
        } catch (IOException | IllegalStateException e) {
            log.error("Failed to load or set FXML scene: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load the screen.", "FXML: " + fxmlPath, e);
        }
    }

    public void switchToTwoFactorAuthScreen(String username, Authentication preAuthToken, String infoMessage) {
        log.info("Switching to Two-Factor Authentication Screen for user: {}", username);
        if (primaryStage == null || springContext == null) {
            log.error("UIManager not properly initialized. PrimaryStage or SpringContext is null.");
            DialogUtil.showErrorAlert("Critical Error", "UI Manager not properly initialized.");
            return;
        }

        String fxmlPathForAuth = "/com/pma/fxml/TwoFactorAuthView.fxml";
        URL fxmlUrl = App.class.getResource(fxmlPathForAuth);
        if (fxmlUrl == null) {
            log.error("2FA FXML file not found at path: {}", fxmlPathForAuth);
            DialogUtil.showErrorAlert("Configuration Error", "Cannot find 2FA FXML: " + fxmlPathForAuth);
            return;
        }

        try {
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

            // For 2FA screen, explicitly set size, do not maximize.
            loadAndSetScene(fxmlPathForAuth, "PMA - Two-Factor Authentication", 400, 350, false,
                    (TwoFactorAuthController c) -> c.initData(username, preAuthToken, infoMessage), // re-init in case loadAndSetScene reloads
                    false); // attemptMaximize = false

        } catch (IOException e) {
            log.error("Failed to load 2FA FXML scene: " + fxmlPathForAuth, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load the 2FA screen.", "FXML: " + fxmlPathForAuth, e);
        } catch (Exception e) {
            log.error("Unexpected error switching to 2FA screen for FXML {}:", fxmlPathForAuth, e);
            DialogUtil.showExceptionDialog("Unexpected UI Error", "An unexpected error occurred while trying to show the 2FA screen.", "Details: " + e.getMessage(), e);
        }
    }

    // This method was a bit redundant with loadAndSetScene, 
    // integrated its purpose into the main loadAndSetScene for non-maximizing screens.
    // private void setupPrimaryStageForScene(Parent rootNode, String title, double width, double height, boolean resizable) { ... }
    public void switchToRegisterScreen() {
        log.info("Switching to Register Screen");
        double desiredWidth = 1200;
        double desiredHeight = 700;
        loadAndSetScene("/com/pma/fxml/register.fxml", "PMA - Đăng ký tài khoản", desiredWidth, desiredHeight, false, null, false);
    }

    private void applyFadeTransition(Parent node) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public boolean show2FASetupDialog(UUID userId) {
        String fxmlPath = "/com/pma/fxml/TwoFactorSetupView.fxml";
        String title = "Set Up Two-Factor Authentication";
        log.info("Opening 2FA Setup dialog for user ID: {}", userId);

        if (springContext == null) {
            log.error("Spring ApplicationContext is not initialized in UIManager. Cannot open 2FA setup dialog.");
            DialogUtil.showErrorAlert("Critical Error", "Application context not available.");
            return false;
        }

        URL fxmlUrl = App.class.getResource(fxmlPath);
        if (fxmlUrl == null) {
            log.error("FXML file not found for 2FA Setup dialog at path: {}", fxmlPath);
            DialogUtil.showErrorAlert("Configuration Error", "Cannot find FXML for 2FA Setup: " + fxmlPath);
            return false;
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(springContext::getBean);

            Parent dialogRoot = loader.load();
            TwoFactorSetupController controller = loader.getController();
            if (controller == null) {
                log.error("Failed to get controller for FXML: {}", fxmlPath);
                DialogUtil.showErrorAlert("UI Error", "Could not initialize 2FA setup screen controller.");
                return false;
            }

            controller.initData(userId);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(this.primaryStage);
            dialogStage.initStyle(StageStyle.UTILITY);

            Scene dialogScene = new Scene(dialogRoot);
            dialogStage.setScene(dialogScene);

            if (primaryStage != null && !primaryStage.getIcons().isEmpty()) {
                dialogStage.getIcons().add(primaryStage.getIcons().get(0));
            }

            dialogStage.showAndWait();

            return controller.isSetupSuccessful();
        } catch (IOException e) {
            log.error("Failed to load 2FA Setup dialog FXML: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load 2FA setup screen.", "FXML: " + fxmlPath, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error opening 2FA setup dialog for FXML {}:", fxmlPath, e);
            DialogUtil.showExceptionDialog("Unexpected UI Error",
                    "An unexpected error occurred while opening the 2FA setup dialog.", "FXML: " + fxmlPath + ", Error: " + e.getMessage(), e);
            return false;
        }
    }

    public void navigateAfterLogin(Authentication authentication) {
        Objects.requireNonNull(authentication, "Authentication object cannot be null for navigation.");
        String username = authentication.getName();

        if (authentication.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_ADMIN".equals(ga.getAuthority()))) {
            log.info("User {} is ADMIN, navigating to admin dashboard.", username);
            switchToAdminDashboard();
        } else if (authentication.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_DOCTOR".equals(ga.getAuthority()))) {
            log.info("User {} is DOCTOR, navigating to doctor dashboard.", username);
            switchToDoctorDashboard();
        } else if (authentication.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_PATIENT".equals(ga.getAuthority()))) {
            log.info("User {} is PATIENT, navigating to patient dashboard.", username);
            switchToPatientDashboard();
        } else {
            log.warn("User {} has no recognized role or no specific dashboard. Navigating to default main dashboard.", username);
            switchToLoginScreen(); // Fallback to login or a default screen if no role matches
        }
    }

    // private void switchToSignup() { // This method was defined but not implemented
    //     throw new UnsupportedOperationException("Unimplemented method 'switchToSignup'");
    // }
    private void switchToPatientDashboard() {
        log.info("Switching to Patient Dashboard Screen");
        setSceneAndMaximize("/com/pma/fxml/patient_book_appointment.fxml", "PMA - Patient Appointment", 1200, 800, true, null);
    }

    public void switchToForgotPasswordScreen() {
        log.info("Navigating to Forgot Password Screen.");
        loadAndSetScene("/com/pma/fxml/forgot_password_request.fxml", "Yêu cầu đặt lại mật khẩu", 450, 400, false, null, false);
    }

    public void switchToResetPasswordScreen(String username) {
        log.info("Navigating to Reset Password Screen for user: {}", username);
        loadAndSetScene("/com/pma/fxml/reset_password.fxml", "Đặt lại mật khẩu", 450, 550, false,
                (ResetPasswordController controller) -> controller.initData(username), false);
    }

    // The following method was a duplicate/older version of setSceneAndMaximize and is not needed
    // if the generic <T> setSceneAndMaximize and the main loadAndSetScene cover all cases.
    // private void setSceneAndMaximize(String fxmlPath, String title, int defaultWidth, int defaultHeight, boolean resizable, Consumer<Object> controllerCallback) { ... }
}
