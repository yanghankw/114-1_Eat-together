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

    // ★ 新增：登入驗證方法
    public static boolean loginUser(String email, String password) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // Supabase 登入是用 /auth/v1/token?grant_type=password
            // Body 格式: {"email": "...", "password": "..."}
            String jsonBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/auth/v1/token?grant_type=password"))
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 印出結果方便除錯
            System.out.println("Supabase Login 回應: " + response.statusCode());

            // 200 代表帳密正確，登入成功
            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}