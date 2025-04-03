package com.pma.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.pma.config.DatabaseConnection;

public class LoginService {
    private final DatabaseConnection db;

    // Constructor để khởi tạo DatabaseConnection
    public LoginService() {
        this.db = new DatabaseConnection();
    }

    // Phương thức kết nối cơ sở dữ liệu
    public Connection connect() {
        try {
            return db.getConnection();
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
        }
        return null; // Trả về null nếu kết nối thất bại
    }

    // Phương thức xác thực người dùng
    public boolean authenticate(String username, String password) {
        String storedProcedure = "{call authenticate_user(?, ?)}";
        try (Connection connection = connect()) {
            if (connection == null) {
                System.err.println("Failed to connect to the database.");
                return false;
            }

            try (CallableStatement callableStatement = connection.prepareCall(storedProcedure)) {
                // Gán giá trị cho các tham số trong stored procedure
                callableStatement.setString(1, username);
                callableStatement.setString(2, password);

                // Thực thi stored procedure
                ResultSet resultSet = callableStatement.executeQuery();
                if (resultSet.next()) {
                    // Kiểm tra nếu có ít nhất một bản ghi khớp
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during authentication: " + e.getMessage());
        }
        return false; // Trả về false nếu xác thực thất bại
    }

    // Phương thức đăng ký người dùng mới
    public boolean register(String username, String password) {
        String storedProcedure = "{call register_user(?, ?)}";
        try (Connection connection = connect()) {
            if (connection == null) {
                System.err.println("Failed to connect to the database.");
                return false;
            }

            try (CallableStatement callableStatement = connection.prepareCall(storedProcedure)) {
                // Gán giá trị cho các tham số trong stored procedure
                callableStatement.setString(1, username);
                callableStatement.setString(2, password);

                // Thực thi stored procedure
                int rowsAffected = callableStatement.executeUpdate();
                return rowsAffected > 0; // Trả về true nếu thêm thành công
            }
        } catch (SQLException e) {
            System.err.println("Error during registration: " + e.getMessage());
        }
        return false; // Trả về false nếu thêm thất bại
    }
}
