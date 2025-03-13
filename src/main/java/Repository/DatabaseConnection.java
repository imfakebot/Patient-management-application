package Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private final String url = "jdbc:sqlserver://localhost:1433;databaseName=PMA;encrypt=true;trustServerCertificate=true;";
    private final String user = "sa";
    private final String password = "25012006anhahihi";

    /**
     * Establishes a connection to the SQL Server database using the provided URL,
     * user, and password.
     *
     * @return A {@link Connection} object representing the connection to the
     *         database.
     * @throws SQLException If a database access error occurs.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
