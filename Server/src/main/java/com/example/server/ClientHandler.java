package com.example.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String myUserId = null; // 記住這個連線屬於誰

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
                // 為了不讓 Log 太亂，MSG 指令可以不印出來，或是只印簡短版
                if (!message.startsWith("MSG:")) {
                    System.out.println("Received command: " + message);
                }

                // 1. 處理註冊
                if (message.startsWith("REGISTER:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String newEmail = parts[1];
                        String newPassword = parts[2];
                        boolean success = ServerSupabaseHelper.registerUser(newEmail, newPassword);
                        if (success) out.println("REGISTER_SUCCESS");
                        else out.println("REGISTER_FAIL:Supabase拒絕連線");
                    } else {
                        out.println("REGISTER_FAIL:格式錯誤");
                    }
                }
                // 2. 處理登入 (★ 修改重點：註冊到線上名單)
                else if (message.startsWith("LOGIN:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String email = parts[1];
                        String password = parts[2];
                        System.out.println("驗證登入中: " + email);

                        String userId = ServerSupabaseHelper.loginUser(email, password);

                        if (userId != null) {
                            // ★★★ 關鍵修改開始 ★★★
                            this.myUserId = userId; // 1. 記住我是誰
                            ServerMain.onlineUsers.put(userId, this); // 2. 把自己寫到「牆上白板」

                            out.println("LOGIN_SUCCESS:" + userId);
                            System.out.println("使用者上線: " + userId);
                            // ★★★ 關鍵修改結束 ★★★
                        } else {
                            out.println("LOGIN_FAIL");
                        }
                    }
                }
                // 3. 處理新活動
                else if (message.startsWith("NEW_EVENT:")) {
                    out.println("EVENT_CREATED_SUCCESS");
                }
                // 4. 獲取全部用戶
                else if (message.startsWith("GET_ALL_USERS")) {
                    String usersJson = ServerSupabaseHelper.getAllUsers();
                    out.println("USERS_JSON:" + usersJson);
                }
                // 5. 加好友
                else if (message.startsWith("ADD_FRIEND:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String myId = parts[1];
                        String targetId = parts[2];
                        boolean success = ServerSupabaseHelper.addFriendDirectly(myId, targetId);
                        if (success) out.println("ADD_FRIEND_SUCCESS");
                        else out.println("ADD_FRIEND_FAIL");
                    }
                }
                // 6. 獲取好友
                else if (message.startsWith("GET_FRIENDS:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 2) {
                        String myId = parts[1];
                        String json = ServerSupabaseHelper.getFriendList(myId);
                        out.println("FRIENDS_JSON:" + json);
                    }
                }
                // 7. 處理聊天訊息
                else if (message.startsWith("MSG:")) {
                    String[] parts = message.split(":", 3);

                    // ★ 修改這裡：加上 else 判斷
                    if (parts.length == 3) {
                        if (myUserId != null) {
                            // === 已登入，正常處理 ===
                            String receiverId = parts[1];
                            String content = parts[2];
                            System.out.println(myUserId + " 傳給 " + receiverId + ": " + content);

                            ServerSupabaseHelper.saveMessage(myUserId, receiverId, content);

                            ClientHandler receiverHandler = ServerMain.onlineUsers.get(receiverId);
                            if (receiverHandler != null) {
                                receiverHandler.sendMessage("NEW_MSG:" + myUserId + ":" + content);
                            }
                        } else {
                            // === ★ 未登入，印出錯誤！ ===
                            System.out.println("⚠️ 忽略訊息：使用者尚未登入 (myUserId is null)");
                            out.println("ERROR:請重新登入");
                        }
                    }
                }
                else {
                    out.println("UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            // ★★★ 斷線處理：把自己從白板上擦掉 ★★★
            if (myUserId != null) {
                ServerMain.onlineUsers.remove(myUserId);
                System.out.println("使用者下線: " + myUserId);
            }
            try { socket.close(); } catch (IOException e) {}
        }
    }

    // ★★★ 輔助方法：讓其他 Handler 可以呼叫這個方法傳訊息給我 ★★★
    public void sendMessage(String msg) {
        out.println(msg);
    }
}