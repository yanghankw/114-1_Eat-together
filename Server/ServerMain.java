import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    // 這裡設定 Server 要監聽的 Port (必須跟 Android TcpClient 裡寫的一樣)
    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("🚀 Server 啟動中...");
        System.out.println("👂 正在 Port " + PORT + " 等待手機連線...");
        System.out.println("=========================================");

        // 建立 ServerSocket，開始監聽
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            
            // 無窮迴圈：讓 Server 一直開著，不會處理完一個就結束
            while (true) {
                // 1. 等待連線 (程式會停在這行，直到有手機連上來)
                Socket clientSocket = serverSocket.accept();
                
                // 2. 顯示連線者的 IP (方便你除錯)
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                System.out.println("📲 新裝置連線成功！來自 IP: " + clientIP);

                // 3. 啟動新執行緒 (Thread)
                // 把這個客人交給 ClientHandler 處理，主程式繼續迴圈等待下一個人
                // 這樣你的 Server 才能同時服務多個人
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
            
        } catch (IOException e) {
            System.out.println("❌ Server 啟動失敗或發生錯誤");
            e.printStackTrace();
        }
    }
}