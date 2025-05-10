package com.pma.util; // Hoặc package của bạn

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage; // Import để có thể lấy owner stage
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Lớp tiện ích cung cấp các phương thức static để hiển thị các loại Dialog
 * JavaFX phổ biến.
 */
public final class DialogUtil { // final class vì chỉ chứa static methods

    private static final Logger log = LoggerFactory.getLogger(DialogUtil.class);

    // Private constructor để ngăn chặn việc tạo instance
    private DialogUtil() {
    }

    /**
     * Hiển thị một Information Alert đơn giản.
     *
     * @param title Tiêu đề của dialog.
     * @param message Nội dung thông báo.
     */
    public static void showInfoAlert(String title, String message) {
        showAlert(AlertType.INFORMATION, title, null, message);
    }

    /**
     * Hiển thị một Warning Alert.
     *
     * @param title Tiêu đề của dialog.
     * @param message Nội dung cảnh báo.
     */
    public static void showWarningAlert(String title, String message) {
        showAlert(AlertType.WARNING, title, null, message);
    }

    /**
     * Hiển thị một Error Alert.
     *
     * @param title Tiêu đề của dialog.
     * @param message Nội dung lỗi.
     */
    public static void showErrorAlert(String title, String message) {
        showAlert(AlertType.ERROR, title, null, message);
    }

    /**
     * Hiển thị một Error Alert với header text.
     *
     * @param title Tiêu đề của dialog.
     * @param headerText Header text (thường là tóm tắt lỗi).
     * @param message Nội dung lỗi chi tiết.
     */
    public static void showErrorAlert(String title, String headerText, String message) {
        showAlert(AlertType.ERROR, title, headerText, message);
    }

    /**
     * Hiển thị một Confirmation Alert và trả về lựa chọn của người dùng.
     *
     * @param title Tiêu đề của dialog.
     * @param message Câu hỏi xác nhận.
     * @return Optional chứa ButtonType (OK hoặc CANCEL) mà người dùng đã chọn.
     */
    public static Optional<ButtonType> showConfirmationAlert(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);
        // Lấy owner stage hiện tại (nếu có) để dialog hiển thị đúng ngữ cảnh
        // Stage owner = (Stage) SceneUtil.getActiveWindow(); // Cần một SceneUtil hoặc cách lấy active window
        // alert.initOwner(owner);
        return alert.showAndWait();
    }

    /**
     * Hiển thị một Dialog chi tiết về Exception.
     *
     * @param title Tiêu đề của dialog.
     * @param headerText Thông tin header.
     * @param contentText Nội dung thông báo chung.
     * @param ex Exception cần hiển thị stack trace.
     */
    public static void showExceptionDialog(String title, String headerText, String contentText, Exception ex) {
        log.error(headerText + " - " + contentText, ex); // Ghi log exception

        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText == null || contentText.isEmpty() ? "An unexpected error occurred." : contentText);
        alert.initStyle(StageStyle.UTILITY);

        // Tạo phần mở rộng để hiển thị stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Đặt nội dung mở rộng vào dialog
        alert.getDialogPane().setExpandableContent(expContent);
        // Stage owner = (Stage) SceneUtil.getActiveWindow();
        // alert.initOwner(owner);
        alert.showAndWait();
    }

    /**
     * Phương thức helper chung để hiển thị Alert.
     */
    private static void showAlert(AlertType alertType, String title, String headerText, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        alert.initStyle(StageStyle.UTILITY);
        // Stage owner = (Stage) SceneUtil.getActiveWindow();
        // alert.initOwner(owner);
        alert.showAndWait();
    }
}
