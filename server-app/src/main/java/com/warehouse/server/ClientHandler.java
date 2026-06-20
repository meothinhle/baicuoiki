package com.warehouse.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.warehouse.model.Request;
import com.warehouse.model.Response;
import com.warehouse.util.GsonFactory;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Mỗi Client kết nối được xử lý bởi một Thread riêng
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket clientSocket;
    private final RequestHandler requestHandler;
    private final Gson gson = GsonFactory.create();

    public ClientHandler(Socket clientSocket, RequestHandler requestHandler) {
        this.clientSocket = clientSocket;
        this.requestHandler = requestHandler;
    }

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        logger.info("Client kết nối: " + clientIp);

        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    Request request = gson.fromJson(line, Request.class);
                    Response response = requestHandler.handle(request, clientIp);
                    writer.println(gson.toJson(response));
                } catch (Exception e) {
                    logger.warning("Lỗi parse request từ " + clientIp + ": " + e.getMessage());
                    writer.println(gson.toJson(Response.fail("Request không hợp lệ")));
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            logger.info("Client ngắt kết nối: " + clientIp);
        } catch (IOException e) {
            logger.warning("Lỗi IO với client " + clientIp + ": " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
            logger.info("Đóng kết nối: " + clientIp);
        }
    }
}
