package com.example.eat_together;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpClient {
    private static TcpClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // 監聽器介面
    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    private OnMessageReceivedListener listener;
    private static final String SERVER_IP = "10.0.2.2"; // 模擬器用 IP
    private static final int SERVER_PORT = 12345;

    private boolean isRunning = false;

    // ★★★ 新增：同步控制變數 (用來解決讀取衝突) ★★★
    private final Object lock = new Object(); // 鎖
    private boolean isWaitingForReply = false; // 是否有人在等回應
    private String replyMessage = null;       // 暫存回應的變數

    public static TcpClient getInstance() {
        if (instance == null) {
            instance = new TcpClient();
        }
        return instance;
    }

    public void setListener(OnMessageReceivedListener listener) {
        this.listener = listener;
    }

    public void connect() {
        if (isConnected()) return;

        new Thread(() -> {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

                Log.d("TCP", "✅ 連線成功！");
                startReadingLoop();

            } catch (Exception e) {
                Log.e("TCP", "連線失敗", e);
            }
        }).start();
    }

    // ★★★ 核心修改：接收迴圈 (加入攔截邏輯) ★★★
    private void startReadingLoop() {
        isRunning = true;
        try {
            String message;
            while (isRunning && in != null && (message = in.readLine()) != null) {
                Log.d("TCP", "收到訊息: " + message);

                // 判斷是否有人在用 sendRequest 等這則訊息
                synchronized (lock) {
                    if (isWaitingForReply) {
                        replyMessage = message;    // 把訊息存下來給 sendRequest
                        isWaitingForReply = false; // 關閉等待旗標
                        lock.notifyAll();          // 叫醒 sendRequest
                        continue;                  // ★ 這則訊息被攔截了，不傳給 Listener
                    }
                }

                // 如果沒人攔截，就正常傳給聊天室介面
                if (listener != null) {
                    listener.onMessageReceived(message);
                }
            }
        } catch (Exception e) {
            Log.e("TCP", "讀取斷線", e);
        } finally {
            isRunning = false;
        }
    }

    public void sendMessage(String message) {
        new Thread(() -> {
            if (out != null && !out.checkError()) {
                out.println(message);
                out.flush();
            }
        }).start();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ★★★ 重新實作：同步請求方法 (讓 ChatsFragment 呼叫) ★★★
    // 這個方法會暫停目前的 Thread，直到收到回應
    public String sendRequest(String command) {
        synchronized (lock) {
            try {
                // 1. 設定旗標：告訴迴圈「我要攔截下一則訊息」
                isWaitingForReply = true;
                replyMessage = null;

                // 2. 發送指令
                if (out != null && !out.checkError()) {
                    out.println(command);
                    out.flush();
                } else {
                    isWaitingForReply = false;
                    return null;
                }

                // 3. 等待回應 (最多等 5 秒，避免永久卡死)
                lock.wait(5000);

                // 4. 回傳攔截到的訊息
                return replyMessage;

            } catch (InterruptedException e) {
                e.printStackTrace();
                isWaitingForReply = false;
                return null;
            }
        }
    }

    // ★★★ 補充：為了相容性，保留 readMessage 但回傳 null ★★★
    // 因為現在讀取都由 startReadingLoop 統一管理，外部不應該直接呼叫這個了
    public String readMessage() {
        Log.w("TCP", "請勿直接呼叫 readMessage，請改用 Listener 或 sendRequest");
        return null;
    }
}