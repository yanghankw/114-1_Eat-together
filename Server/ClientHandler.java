public class ClientHandler {
    
}
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("📩 收到指令: " + message);

                if (message.startsWith("LOGIN:")) {
                    // 模擬登入成功
                    out.println("LOGIN_SUCCESS");
                } 
                else if (message.startsWith("NEW_EVENT:")) {
                    // 模擬建立活動
                    System.out.println("🎉 [模擬] 收到建立活動請求！");
                    out.println("EVENT_CREATED_SUCCESS"); 
                }
                else if (message.startsWith("GET_FRIENDS")) {
                    System.out.println("👥 收到請求：獲取好友列表");
                    
                    // 這裡未來要查資料庫，現在先回傳假資料給手機
                    // 格式範例: FRIEND_LIST:王小明,陳小美,林大華
                    out.println("FRIEND_LIST:Server小明,Server小美"); 
                }
                else {
                    out.println("UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            System.out.println("❌ 斷線: " + socket.getInetAddress());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }
}