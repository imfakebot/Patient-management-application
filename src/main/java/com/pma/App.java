package com.pma;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Nạp file FXML. Giả sử file FXML tên "login.fxml" nằm cùng thư mục resource
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pma/login.fxml"));
            Parent root = loader.load();

            // Đặt tiêu đề cửa sổ, tạo Scene, hiển thị
            primaryStage.setTitle("JavaFX Login");
            primaryStage.setScene(new Scene(root, 1200, 675)); // Kích thước như file FXML
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm main để chạy ứng dụng JavaFX
    public static void main(String[] args) {
        launch(args);
    }
}
