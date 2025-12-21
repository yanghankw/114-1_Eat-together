package com.example.eat_together;

import android.util.Log;
import java.io.PrintWriter;
import java.net.Socket;

import java.io.BufferedReader; // 記得 import
import java.io.InputStreamReader; // 記得 import

public class TcpClient {
    private static TcpClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in; // ★ 新增這行

    // ⚠️ 請確認這是您電腦的 IP (模擬器請用 10.0.2.2，實機請用 192.168.x.x)
    private static final String SERVER_IP = "10.0.2.2";
    private static final int SERVER_PORT = 12345;

    public static TcpClient getInstance() {
        if (instance == null) {
            instance = new TcpClient();
        }
        return instance;
    }

    // ★ 修改 1: 將連線動作強制放到背景執行緒
    public void connect() {
        new Thread(() -> {
            try {
                // ... 連線 socket ...
                socket = new Socket(SERVER_IP, SERVER_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                // ★ 新增這行：初始化輸入流，這樣才能聽到 Server 說話
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Log.d("TCP", "✅ 連線成功！");
            } catch (Exception e) { /* ... */ }
        }).start();
    }

    // ★ 新增這個方法：發送訊息並「等待」對方回應 (同步方法)
    public String sendRequest(String message) {
        try {
            if (out != null && in != null) {
                out.println(message); // 發送
                return in.readLine(); // 等待並讀取一行回應
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // 連線有問題
    }

    // ★ 修改 2: 發送前檢查是否已連線
    public void sendMessage(final String message) {
        new Thread(() -> {
            if (out != null) {
                out.println(message);
                Log.d("TCP", "已發送: " + message);
            } else {
                Log.e("TCP", "❌ 發送失敗：尚未連線或連線中斷");
                // 嘗試重新連線 (選擇性)
                // connect();
            }
        }).start();
    }
}