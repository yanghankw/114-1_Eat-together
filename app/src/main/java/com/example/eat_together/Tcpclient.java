package com.example.eat_together;

import android.util.Log;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpClient {
    private static TcpClient instance;
    private Socket socket;
    private PrintWriter out;
    
    // ⚠️ 這裡填你電腦的 IPv4 位址！不要填 localhost
    private static final String SERVER_IP = "192.168.234.160"; 
    private static final int SERVER_PORT = 12345;

    // 單例模式：確保整個 App 只有一個連線
    public static TcpClient getInstance() {
        if (instance == null) {
            instance = new TcpClient();
        }
        return instance;
    }

    // 連線到伺服器 (必須在子執行緒呼叫)
    public void connect() {
        try {
            Log.d("TCP", "正在連線到 " + SERVER_IP + "...");
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            Log.d("TCP", "✅ 連線成功！");
            
            // 連線成功後，順便送一個登入指令測試
            sendMessage("LOGIN:TestUser:123456");
            
        } catch (Exception e) {
            Log.e("TCP", "❌ 連線失敗", e);
            e.printStackTrace();
        }
    }

    // 發送訊息給 Server
    public void sendMessage(String message) {
        new Thread(() -> {
            if (out != null) {
                out.println(message);
                Log.d("TCP", "已發送: " + message);
            }
        }).start();
    }
}