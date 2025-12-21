package com.example.server; // 記得確認 package 名稱

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ServerSupabaseHelper {

    // ★★★ 請填入您的 Supabase 專案資訊 ★★★
//    private static final String PROJECT_URL = "https://wttrpkzltuesmeofvtal.supabase.co";
//    private static final String API_KEY = "sb_publishable_F221TzkGRrNXN6FePJcO1A_ijBU5ip0";

    private static final String PROJECT_URL = "https://xkiopgopzmjuatxvnwci.supabase.co";
    private static final String API_KEY = "sb_publishable_pUe70HuQ4n3r4Vq5qlbytg_zJjZlP_3";
    public static boolean registerUser(String email, String password) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // ★ 關鍵：JSON 格式必須包含 "password" 欄位
            // 格式範例: {"email": "user@test.com", "password": "123456"}
            String jsonBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PROJECT_URL + "/auth/v1/signup")) // 這是註冊的 API
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 印出結果方便除錯
            System.out.println("Supabase 回應代碼: " + response.statusCode());
            System.out.println("Supabase 回應內容: " + response.body());

            // 200 OK 或 201 Created 都算成功
            return response.statusCode() == 200 || response.statusCode() == 201;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ... 原本的 imports 和變數 ...

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