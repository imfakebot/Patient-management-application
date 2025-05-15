package com.pma;

import com.pma.util.UIManager; // Import UIManager
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext; // Import ConfigurableApplicationContext
import org.springframework.scheduling.annotation.EnableAsync;

import javafx.application.Application;
import javafx.application.Platform; // Import Platform
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@SpringBootApplication
@EnableAsync
public class App extends Application {

    private ConfigurableApplicationContext springContext;
    private UIManager uiManager;

    // Static field để lưu trữ args từ main, dùng cho SpringApplication.run trong init()
    private static String[] savedArgs;

    @Override
    public void init() throws Exception {
        // Khởi động Spring Boot application context
        // Truyền savedArgs để Spring có thể xử lý các tham số dòng lệnh nếu có
        springContext = SpringApplication.run(App.class, savedArgs);

        // Lấy UIManager bean từ Spring context
        // Tên bean mặc định thường là tên lớp viết thường chữ cái đầu ("uiManager")
        uiManager = springContext.getBean(UIManager.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Đảm bảo UIManager đã được lấy thành công từ Spring context
        if (uiManager == null) {
            System.err.println("UIManager bean is null. Spring context initialization might have failed.");
            Platform.exit(); // Thoát ứng dụng JavaFX nếu không có UIManager
            return;
        }

        // Thiết lập primaryStage cho UIManager để nó có thể quản lý cửa sổ chính
        uiManager.initializePrimaryStage(primaryStage);

        // Thiết lập các thuộc tính ban đầu cho Stage (tùy chọn, UIManager cũng có thể làm)
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setResizable(true);
        // primaryStage.setMaximized(true); // Có thể bỏ dòng này để dễ debug ban đầu

        // Yêu cầu UIManager chuyển sang màn hình ban đầu (ví dụ: màn hình login)
        // Dựa trên log trước đó có tiêu đề "Login Form", giả định màn hình login là màn hình khởi đầu.
        uiManager.switchToLoginScreen();

        // Phương thức loadScene trong UIManager sẽ gọi primaryStage.show()
        // nên bạn thường không cần gọi primaryStage.show() ở đây nữa.
    }

    @Override
    public void stop() throws Exception {
        // Đóng Spring context khi ứng dụng JavaFX dừng lại
        if (springContext != null) {
            springContext.close();
        }
        // Đảm bảo nền tảng JavaFX thoát sạch sẽ
        Platform.exit();
        // Tùy chọn: Thoát toàn bộ tiến trình
        // System.exit(0);
    }

    public static void main(String[] args) {
        // Lưu lại args để SpringApplication.run có thể sử dụng trong init()
        savedArgs = args;
        // Khởi chạy ứng dụng JavaFX. Lệnh này sẽ gọi init() rồi đến start().
        Application.launch(App.class, args);
    }
}
