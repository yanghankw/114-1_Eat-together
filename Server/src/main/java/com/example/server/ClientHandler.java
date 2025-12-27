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
                    if (parts.length == 3) {
                        if (myUserId != null) {
                            String receiverId = parts[1];
                            String content = parts[2];

                            // ★ 1. 先去查我的名字
                            String myName = ServerSupabaseHelper.getUserName(myUserId);

                            ServerSupabaseHelper.saveMessage(myUserId, receiverId, content);

                            ClientHandler receiverHandler = ServerMain.onlineUsers.get(receiverId);
                            if (receiverHandler != null) {
                                // ★ 2. 轉發時，加入名字 (格式: NEW_MSG:ID:Name:Content)
                                receiverHandler.sendMessage("NEW_MSG:" + myUserId + ":" + myName + ":" + content);
                            }
                        } else {
                            out.println("ERROR:請重新登入");
                        }
                    }
                }
                // ★ 新增：獲取歷史訊息
                else if (message.startsWith("GET_CHAT_HISTORY:")) {
                    // 格式: GET_CHAT_HISTORY:我的ID:對方ID
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String myId = parts[1];
                        String friendId = parts[2];

                        System.out.println("查詢歷史記錄: " + myId + " <-> " + friendId);

                        String historyJson = ServerSupabaseHelper.getChatHistory(myId, friendId);
                        out.println("HISTORY_JSON:" + historyJson);
                    }
                }
                // ★ 新增：處理群組歷史查詢
                else if (message.startsWith("GET_GROUP_HISTORY:")) {
                    String[] parts = message.split(":");
                    if (parts.length == 2) {
                        String groupId = parts[1];
                        System.out.println("查詢群組歷史: " + groupId);

                        String historyJson = ServerSupabaseHelper.getGroupHistory(groupId);
                        // 注意：這裡我們回傳一樣的標頭 "HISTORY_JSON"，這樣 App 端不用改解析邏輯就能共用！
                        out.println("HISTORY_JSON:" + historyJson);
                    }
                }
                // ★ 新增：處理群組訊息
                else if (message.startsWith("GROUP_MSG:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3 && myUserId != null) {
                        String groupId = parts[1];
                        String content = parts[2];

                        // ★ 1. 先去查我的名字
                        String myName = ServerSupabaseHelper.getUserName(myUserId);

                        ServerSupabaseHelper.saveGroupMessage(groupId, myUserId, content);
                        java.util.List<String> members = ServerSupabaseHelper.getGroupMemberIds(groupId);

                        for (String memberId : members) {
                            if (memberId.equals(myUserId)) continue;

                            ClientHandler handler = ServerMain.onlineUsers.get(memberId);
                            if (handler != null) {
                                // ★ 2. 轉發時，加入名字 (格式: NEW_GROUP_MSG:GroupID:ID:Name:Content)
                                handler.sendMessage("NEW_GROUP_MSG:" + groupId + ":" + myUserId + ":" + myName + ":" + content);
                            }
                        }
                    }
                }
                // ★ 新增：建立群組指令
                else if (message.startsWith("CREATE_GROUP:")) {
                    // 格式: CREATE_GROUP:群組名
                    String[] parts = message.split(":", 2);
                    if (parts.length == 2 && myUserId != null) {
                        String groupName = parts[1];
                        String groupId = ServerSupabaseHelper.createGroup(groupName, myUserId);
                        if (groupId != null) {
                            out.println("CREATE_GROUP_SUCCESS:" + groupId + ":" + groupName);
                        } else {
                            out.println("CREATE_GROUP_FAIL");
                        }
                    }
                }

                // ★★★ 在這裡插入 UPDATE_NAME 的邏輯 ★★★
                else if (message.startsWith("UPDATE_NAME:")) {
                    // 指令格式: UPDATE_NAME:使用者UUID:新名字
                    String[] parts = message.split(":");
                    if (parts.length == 3) {
                        String targetUuid = parts[1];
                        String newName = parts[2];

                        System.out.println("收到改名請求: " + targetUuid + " -> " + newName);

                        // 呼叫 ServerSupabaseHelper 的方法
                        boolean success = ServerSupabaseHelper.updateUsername(targetUuid, newName);

                        if (success) {
                            out.println("UPDATE_NAME_SUCCESS");
                        } else {
                            out.println("UPDATE_NAME_FAIL");
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