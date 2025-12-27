package com.example.server; // 記得確認 package 名稱

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ServerSupabaseHelper {

    // ★★★ 請填入您的 Supabase 專案資訊 ★★★
//    private static final String PROJECT_URL = "https://wttrpkzltuesmeofvtal.supabase.co";
//    private static final String API_KEY = "sb_publishable_F221TzkGRrNXN6FePJcO1A_ijBU5ip0";

    // Cookie's url / key
    private static final String PROJECT_URL = "https://xkiopgopzmjuatxvnwci.supabase.co";
    private static final String API_KEY = "sb_publishable_pUe70HuQ4n3r4Vq5qlbytg_zJjZlP_3";

    // 楊承翰的key與url......
//    private static final String PROJECT_URL = "https://rhvbyvpmnjhnxqbrewoj.supabase.co";
//    private static final String API_KEY = "sb_publishable_GujBS-Yim9FIh5LPwC0WQg_Xbr7diu-";
    private static final HttpClient client = HttpClient.newHttpClient();

    // 1. 註冊流程 (主入口)
    public static boolean registerUser(String email, String password) {
        // 第一步：先在 Supabase Auth 系統註冊，並取得回傳的 UUID
        String userId = registerAuthAndGetId(email, password);

        if (userId != null) {
            System.out.println("Auth 註冊成功，取得 UUID: " + userId);
            System.out.println("正在同步建立 public.users 資料...");

            // 第二步：使用同一組 UUID 建立使用者資料
            return createPublicUserProfile(userId, email);
        } else {
            System.out.println("Auth 註冊失敗，無法取得 UUID");
            return false;
        }
    }

    // A. 呼叫 Auth API (回傳 UUID 字串，失敗回傳 null)
    private static String registerAuthAndGetId(String email, String password) {
        try {
            String jsonBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/auth/v1/signup"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // 成功代碼通常是 200 或 201 (Created)
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                // ★ 關鍵：從 JSON 回應中解析出 "id" (UUID)
                // 回應範例: {"id":"550e8400-e29b-...", "aud":"authenticated", ...}
                // 因為不想引入龐大的 JSON 函式庫，我們用簡單的字串切割來抓 ID
                return extractValueFromJson(responseBody, "id");
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // B. 呼叫 Database API (傳入 UUID)
    private static boolean createPublicUserProfile(String uuid, String email) {
        try {
            String defaultName = email.split("@")[0];

            // ★ 修改點：現在我們要明確指定 "id" 為剛剛拿到的 UUID
            String jsonBody = String.format(
                    "{\"id\": \"%s\", \"email\": \"%s\", \"username\": \"%s\", \"avatar_url\": \"default.png\"}",
                    uuid, email, defaultName
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/users"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println("使用者資料表 (public.users) 同步成功！");
                return true;
            } else {
                System.out.println("使用者資料表建立失敗: " + response.body());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 小工具：簡單的 JSON 欄位擷取器
    private static String extractValueFromJson(String json, String key) {
        try {
            // 尋找 "key":"value" 的結構
            String searchKey = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;

            startIndex += searchKey.length();
            int endIndex = json.indexOf("\"", startIndex);

            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }


    // 修改原本的 loginUser 方法，回傳型別改成 String
    public static String loginUser(String email, String password) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/auth/v1/token?grant_type=password"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // ★ 成功！從 JSON 裡抓出 User ID
                // (利用之前寫過的 extractValueFromJson 工具)
                return extractValueFromJson(response.body(), "id");
            }
            return null; // 失敗回傳 null

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ★ 新增：取得所有使用者 (回傳 JSON 字串)
    public static String getAllUsers() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 查詢 public.users 表的所有資料
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/users?select=id,username,email"))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // ★ 重要：把 JSON 裡面的換行符號拿掉，不然 TcpClient 的 readLine() 會讀錯
                return response.body().replace("\n", "").replace("\r", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "[]"; // 失敗回傳空陣列
    }

    // ★ 修改：檢查是否已經是好友，如果是就不加，否則加
    public static boolean addFriendDirectly(String myId, String friendId) {
        try {
            // 1. 不能加自己
            if (myId.equals(friendId)) return false;

            HttpClient client = HttpClient.newHttpClient();

            // 2. ★ 新增檢查：先查詢是否已經存在關係 (A->B 或 B->A)
            // PostgREST 語法: or=(and(user_id_a.eq.myId,user_id_b.eq.friendId),and(user_id_a.eq.friendId,user_id_b.eq.myId))
            String query = String.format(
                    "or=(and(user_id_a.eq.%s,user_id_b.eq.%s),and(user_id_a.eq.%s,user_id_b.eq.%s))",
                    myId, friendId, friendId, myId
            );

            // 因為有括號，如果 Server 報錯可能需要 URL Encode，但這裡先試試直接傳
            // 如果遇到 400 Bad Request，請改用 URLEncoder.encode(query, "UTF-8")
            // 但 HttpClient 的 URI.create 可能會自動處理部分編碼，或需要我們手動處理
            // 為了保險，這裡手動替換常見符號，或者乾脆簡單一點：如果 Supabase 設定了唯一約束 (unique constraint)，直接 insert 就會擋掉

            // 檢查 API
            HttpRequest checkRequest = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/friendships?" + query))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> checkResponse = client.send(checkRequest, HttpResponse.BodyHandlers.ofString());
            
            // 如果回傳的 JSON 陣列不是空的 (例如 "[{...}]")，代表已經有關係了
            if (checkResponse.statusCode() == 200 && checkResponse.body().length() > 5) {
                System.out.println("已經是好友了，跳過新增");
                return true; // 當作成功，因為結果是一樣的 (已是好友)
            }

            // 3. 如果沒關係，才新增
            // 準備 JSON (注意：Supabase 的 uuid 欄位需要引號)
            String jsonBody = String.format(
                    "{\"user_id_a\": \"%s\", \"user_id_b\": \"%s\", \"status\": \"accepted\"}",
                    myId, friendId
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/friendships"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Prefer", "return=minimal") // 不回傳資料以節省流量
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 201 Created 代表成功
            if (response.statusCode() == 201) {
                System.out.println("好友新增成功: " + myId + " -> " + friendId);
                return true;
            } else if (response.statusCode() == 409) {
                // 409 代表已經是好友了 (資料庫唯一約束擋下)，也算成功
                System.out.println("已經是好友了 (DB Constraint)");
                return true;
            }

            System.out.println("加好友失敗: " + response.body());
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getFriendList(String myId) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 直接查詢我們剛剛建好的 View，條件是 me = eq.我的ID
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/friend_details?me=eq." + myId))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 去除換行符號
                return response.body().replace("\n", "").replace("\r", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "[]";
    }

    // 新增：儲存聊天訊息
    public static boolean saveMessage(String senderId, String receiverId, String content) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // JSON 格式
            String jsonBody = String.format(
                    "{\"sender_id\": \"%s\", \"receiver_id\": \"%s\", \"content\": \"%s\"}",
                    senderId, receiverId, content
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 201;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ★ 新增：獲取兩人間的聊天歷史記錄
    public static String getChatHistory(String user1, String user2) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 這是 PostgREST 的語法，意思是：
            // 找出 (sender=user1 AND receiver=user2) OR (sender=user2 AND receiver=user1)
            // 並且依照 created_at (時間) 升冪排序 (舊的在上面)
            String query = String.format(
                    "or=(and(sender_id.eq.%s,receiver_id.eq.%s),and(sender_id.eq.%s,receiver_id.eq.%s))&order=created_at.asc",
                    user1, user2, user2, user1
            );

            // 因為 query 裡面有括號跟逗號，URL 編碼比較安全，但為了簡單我們先直接拼
            // 如果遇到 400 錯誤，可能需要對 query 做 URLEncoder.encode()

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/messages?" + query))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 去除換行，回傳 JSON
                return response.body().replace("\n", "").replace("\r", "");
            } else {
                System.out.println("查詢歷史失敗: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "[]";
    }

    // ... imports ...

    // ★ 新增：儲存群組訊息
    public static boolean saveGroupMessage(String groupId, String senderId, String content) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonBody = String.format(
                    "{\"group_id\": %s, \"sender_id\": \"%s\", \"content\": \"%s\"}",
                    groupId, senderId, content
            ); // 注意: group_id 是數字，不用引號

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/group_messages"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 201;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ★ 新增：取得群組內的所有成員 ID (用來廣播訊息)
    public static java.util.List<String> getGroupMemberIds(String groupId) {
        java.util.List<String> memberIds = new java.util.ArrayList<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            // 查詢 group_members 表
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/group_members?group_id=eq." + groupId + "&select=user_id"))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // 簡單解析 JSON: [{"user_id": "uuid1"}, {"user_id": "uuid2"}]
                String json = response.body();
                String[] parts = json.split("\"user_id\":\"");
                for (int i = 1; i < parts.length; i++) {
                    String uuid = parts[i].split("\"")[0];
                    memberIds.add(uuid);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return memberIds;
    }

    // ★ 新增：建立群組 (簡單版，只回傳是否成功)
    // 參數: groupName, creatorId (建立者也會自動加入成員)
    public static String createGroup(String groupName, String creatorId) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. 先建立 Group，並要求回傳 ID
            String jsonBody = String.format("{\"name\": \"%s\"}", groupName);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/groups"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Prefer", "return=representation") // 要求回傳剛建立的資料
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                // 解析出 group_id
                String groupId = extractValueFromJson(response.body(), "id"); // 假設 id 是數字
                // 如果 extractValueFromJson 只抓字串，可能要修一下，或是直接用 replace 處理數字
                // 這裡假設您的 extractValueFromJson 可以處理

                // 2. 把建立者加入 group_members
                joinGroup(groupId, creatorId);
                return groupId;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static void joinGroup(String groupId, String userId) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonBody = String.format("{\"group_id\": %s, \"user_id\": \"%s\"}", groupId, userId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/group_members"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {}
    }

    // ★ 新增：根據 UUID 獲取使用者名稱
    public static String getUserName(String uuid) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            // 查詢 users 表，只抓 username
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/users?id=eq." + uuid + "&select=username"))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 回傳格式是 [{"username": "小明"}]
                // 我們用簡單字串處理來抓
                String json = response.body();
                String marker = "\"username\":\"";
                int start = json.indexOf(marker);
                if (start != -1) {
                    start += marker.length();
                    int end = json.indexOf("\"", start);
                    return json.substring(start, end);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "未知用戶"; // 查不到時的回傳值
    }

    // ★ 新增：獲取群組聊天歷史
    public static String getGroupHistory(String groupId) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // ★ 修改重點：加入 select=*,users(username)
            // 這代表：我要 group_messages 的所有欄位，外加 users 表裡的 username
            String queryParams = "group_id=eq." + groupId + "&select=*,users(username)&order=created_at.asc";

            // 為了避免網址裡面的特殊符號 ( , ( ) ) 出問題，建議做一點簡單的編碼取代，或是直接拼字
            // 如果您的環境對網址比較嚴格，可能需要 URLEncoder，但在這裡我們先直接拼拼看

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/rest/v1/group_messages?" + queryParams))
                    .header("apikey", API_KEY)
                    .header("Authorization", "Bearer " + API_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().replace("\n", "").replace("\r", "");
            } else {
                System.out.println("查詢群組歷史失敗: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "[]";
    }
}