package com.example.server; // ⚠️ 注意：請保留您原本的第一行 package 設定，不要動它

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("Server starting...");
        System.out.println("Waiting for connection on Port " + PORT + "...");
        System.out.println("=========================================");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                // 等待連線
                Socket clientSocket = serverSocket.accept();

                String clientIP = clientSocket.getInetAddress().getHostAddress();
                System.out.println("New client connected from IP: " + clientIP);

                // 啟動執行緒
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.out.println("Server failed to start or error occurred.");
            e.printStackTrace();
        }
    }
}