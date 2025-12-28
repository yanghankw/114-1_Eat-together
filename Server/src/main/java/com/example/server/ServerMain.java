package com.example.server; // ⚠️ 注意：請保留您原本的第一行 package 設定，不要動它

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {

    private static final int PORT = 12345;
    public static ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("★ Server 啟動中... (強制 UTF-8 模式) ★");

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