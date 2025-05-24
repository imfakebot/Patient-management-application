package com.pma.util; // Hoặc package của bạn

import java.io.IOException; // Lớp Application chính của bạn
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Để đặt icon cho cửa sổ
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.pma.App;
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
     * Chuyển đến màn hình chính (Dashboard) sau khi đăng nhập.
     */
    public void switchToMainDashboard() {
        log.info("Switching to Main Dashboard Screen");
        loadAndSetScene("/com/pma/view/dashboard.fxml", "PMA - Dashboard", 1200, 800, true); // Ví dụ kích thước, cho phép resize
        // Có thể maximize cửa sổ sau khi vào màn hình chính
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
        try {
            URL fxmlUrl = Objects.requireNonNull(App.class.getResource(fxmlPath), "FXML file not found: " + fxmlPath);
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

            dialogStage.showAndWait(); // Hiển thị và chờ cho đến khi dialog đóng
            return loader.getController(); // Trả về controller của dialog
        } catch (IOException e) {
            log.error("Failed to load modal dialog FXML: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load dialog.", "FXML: " + fxmlPath, e);
            return null;
        }
    }

    /**
     * Tải FXML và đặt nó làm root cho Scene chính.
     *
     * @param fxmlPath Đường dẫn đến file FXML.
     * @param title Tiêu đề cửa sổ.
     * @param preferredWidth Chiều rộng mong muốn.
     * @param preferredHeight Chiều cao mong muốn.
     * @param resizable Cho phép thay đổi kích thước cửa sổ hay không.
     *
     */
    public void loadAndSetScene(String fxmlPath, String title, double preferredWidth, double preferredHeight, boolean resizable) {

        if (primaryStage == null) {

            log.error("Primary stage is not initialized in UIManager.");

            throw new IllegalStateException("Primary stage must be initialized before switching scenes.");

        }

        try {

            URL fxmlUrl = Objects.requireNonNull(App.class.getResource(fxmlPath), "FXML file not found: " + fxmlPath);
            log.info("Loading FXML from: {}", fxmlUrl); // Changed to info for better visibility during startup

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            // QUAN TRỌNG: Để Spring quản lý việc tạo Controller (nếu Controller là Spring Bean)
            loader.setControllerFactory(springContext::getBean);

            Parent rootNode = loader.load();

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

        } catch (IOException e) {
            log.error("Failed to load FXML scene: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load the screen.", "FXML: " + fxmlPath, e);
        } catch (NullPointerException e) {
            log.error("FXML file not found at path: {}", fxmlPath, e);
            DialogUtil.showErrorAlert("Configuration Error", "Cannot find FXML: " + fxmlPath);
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

        try {
            // Đảm bảo đường dẫn FXML nhất quán với các màn hình khác
            String fxmlPath = "/com/pma/fxml/TwoFactorAuthView.fxml"; // Giả sử đây là đường dẫn đúng
            URL fxmlUrl = Objects.requireNonNull(App.class.getResource(fxmlPath), "FXML file not found: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(springContext::getBean);

            Parent rootNode = loader.load();

            // Lấy controller và truyền dữ liệu
            TwoFactorAuthController controller = loader.getController();
            controller.initData(username, preAuthToken, infoMessage);

            // Logic tương tự loadAndSetScene
            if (mainScene == null) {
                mainScene = new Scene(rootNode, 400, 350); // Kích thước có thể tùy chỉnh
                primaryStage.setScene(mainScene);
            } else {
                mainScene.setRoot(rootNode);
                primaryStage.setWidth(400);
                primaryStage.setHeight(350); // Kích thước có thể tùy chỉnh
            }

            // Các thiết lập khác cho primaryStage
            primaryStage.setTitle("PMA - Two-Factor Authentication");
            primaryStage.setResizable(false);

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
            primaryStage.centerOnScreen();
            log.info("Scene switched to 2FA screen for user: {}", username);

        } catch (IOException e) {
            log.error("Failed to load 2FA FXML scene", e);
            DialogUtil.showExceptionDialog("UI Load Error",
                    "Could not load the 2FA screen.",
                    "FXML: /com/pma/fxml/TwoFactorAuthView.fxml", e); // Cập nhật đường dẫn trong thông báo lỗi
        } catch (NullPointerException e) {
            log.error("2FA FXML file not found", e);
            DialogUtil.showErrorAlert("Configuration Error", "Cannot find 2FA FXML: /com/pma/fxml/TwoFactorAuthView.fxml"); // Cập nhật đường dẫn
        }
    }

    /**
     * Chuyển đến màn hình Register.
     */
    public void switchToRegisterScreen() {
        log.info("Switching to Register Screen");
        double desiredWidth = 1200;
        double desiredHeight = 700;
        loadAndSetScene("/com/pma/fxml/register.fxml", "PMA - Đăng ký tài khoản", desiredWidth, desiredHeight, false);
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

        try {
            URL fxmlUrl = Objects.requireNonNull(App.class.getResource(fxmlPath), "FXML file not found: " + fxmlPath);
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
        } catch (IOException | NullPointerException e) {
            log.error("Failed to load 2FA Setup dialog FXML: " + fxmlPath, e);
            DialogUtil.showExceptionDialog("UI Load Error", "Could not load 2FA setup screen.", "FXML: " + fxmlPath, e);
            return false;
        }
    }
}
