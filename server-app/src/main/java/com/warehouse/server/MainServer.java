package com.warehouse.server;

import com.warehouse.dao.DatabaseConnection;
import com.warehouse.service.AuthService;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Điểm khởi động Server
 * - Lắng nghe kết nối TCP
 * - Dùng ThreadPool (10 thread) để xử lý nhiều client song song
 */
public class MainServer {
    private static final Logger logger = Logger.getLogger(MainServer.class.getName());

    private static int PORT = 9000;
    private static int MAX_THREADS = 10;

    public static void main(String[] args) {
        loadConfig();

        // Kiểm tra kết nối database
        if (!DatabaseConnection.testConnection()) {
            logger.severe("KHÔNG THỂ KẾT NỐI DATABASE! Kiểm tra MySQL và config.properties");
            System.exit(1);
        }
        logger.info("✓ Kết nối database thành công");

        AuthService authService = new AuthService();
        RequestHandler requestHandler = new RequestHandler(authService);

        // ThreadPool: tối đa MAX_THREADS client cùng lúc
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("===========================================");
            logger.info("  WAREHOUSE SERVER đang chạy trên cổng " + PORT);
            logger.info("  ThreadPool: " + MAX_THREADS + " threads");
            logger.info("  Nhấn Ctrl+C để dừng");
            logger.info("===========================================");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Không đặt timeout: Client (desktop app) giữ kết nối mở suốt
                    // phiên làm việc, có thể idle (chờ người dùng) bất kỳ lúc nào
                    // giữa các request — timeout ở đây sẽ tự ngắt kết nối hợp lệ.
                    executor.submit(new ClientHandler(clientSocket, requestHandler));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        logger.warning("Lỗi chấp nhận kết nối: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.severe("Không thể khởi động server trên cổng " + PORT + ": " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream is = MainServer.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
                PORT = Integer.parseInt(props.getProperty("server.port", "9000"));
                MAX_THREADS = Integer.parseInt(props.getProperty("server.max_threads", "10"));
            }
        } catch (Exception e) {
            logger.warning("Không đọc được config.properties, dùng mặc định: port=" + PORT);
        }
    }
}