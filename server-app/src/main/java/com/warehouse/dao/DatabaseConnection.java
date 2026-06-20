package com.warehouse.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Quản lý kết nối JDBC đến MySQL
 */
public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
                DB_URL = props.getProperty("database.url",
                        "jdbc:mysql://localhost:3307/warehouse_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true");
                DB_USER = props.getProperty("database.username", "root");
                DB_PASSWORD = props.getProperty("database.password", "");
            } else {
                // Mặc định nếu không có file config
                DB_URL = "jdbc:mysql://localhost:3307/warehouse_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true";
                DB_USER = "root";
                DB_PASSWORD = "";
            }
        } catch (IOException e) {
            logger.warning("Không đọc được config.properties, dùng cấu hình mặc định");
            DB_URL = "jdbc:mysql://localhost:3307/warehouse_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true";
            DB_USER = "root";
            DB_PASSWORD = "";
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy MySQL JDBC Driver", e);
        }
    }

    /**
     * Lấy kết nối mới đến database
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Đóng kết nối an toàn
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warning("Lỗi đóng kết nối: " + e.getMessage());
            }
        }
    }

    /**
     * Kiểm tra kết nối database
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.severe("Lỗi kết nối database: " + e.getMessage());
            return false;
        }
    }
}
