package com.example.server; // ⚠️ 注意：請保留您原本的第一行 package 設定，不要動它

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
                // 顯示收到的訊息 (英文)
                System.out.println("Received command: " + message);

                // 1. 處理註冊

                if (message.startsWith("REGISTER:")) {
                    // 用 ":" 切割字串
                    String[] parts = message.split(":");

                    // 檢查切出來是不是有 3 份 (指令 + 帳號 + 密碼)
                    if (parts.length == 3) {
                        String newEmail = parts[1];
                        String newPassword = parts[2]; // ★ 這裡是關鍵，抓出密碼

                        System.out.println("收到註冊請求 - 帳號: " + newEmail + ", 密碼: " + newPassword);

                        // 把帳號跟密碼都傳進去
                        boolean success = ServerSupabaseHelper.registerUser(newEmail, newPassword);

                        if (success) {
                            out.println("REGISTER_SUCCESS");
                        } else {
                            out.println("REGISTER_FAIL:Supabase拒絕連線");
                        }
                    } else {
                        out.println("REGISTER_FAIL:格式錯誤");
                    }
                }
                // 2. 處理登入
                else if (message.startsWith("LOGIN:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String email = parts[1];
                        String password = parts[2];

                        System.out.println("驗證登入中: " + email);

                        // ★ 關鍵修改：去 Supabase 檢查帳密
                        boolean isValid = ServerSupabaseHelper.loginUser(email, password);

                        if (isValid) {
                            out.println("LOGIN_SUCCESS");
                            System.out.println("登入成功！");
                        } else {
                            out.println("LOGIN_FAIL"); // 帳密錯誤
                            System.out.println("登入失敗：帳密錯誤");
                        }
                    } else {
                        out.println("LOGIN_FAIL:格式錯誤");
                    }
                }
                // 3. 處理新活動
                else if (message.startsWith("NEW_EVENT:")) {
                    System.out.println("[Mock] Event created request received!");
                    out.println("EVENT_CREATED_SUCCESS");
                }
                // 4. 獲取全部用戶
                else if (message.startsWith("GET_ALL_USERS")) {
                    System.out.println("收到請求：獲取所有用戶列表");

                    // 1. 去 Supabase 抓資料
                    String usersJson = ServerSupabaseHelper.getAllUsers();

                    // 2. 回傳給手機 (加上前綴字串方便辨識)
                    out.println("USERS_JSON:" + usersJson);
                }
                // 5. 獲取好友
                else if (message.startsWith("GET_FRIENDS")) {
                    System.out.println("Request received: Get Friends List");
                    // 回傳英文名字，避免手機端解碼錯誤
                    out.println("FRIEND_LIST:Alice,Bob,Charlie");
                }
                else {
                    out.println("UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }
}