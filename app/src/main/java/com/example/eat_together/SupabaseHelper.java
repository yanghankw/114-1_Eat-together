package com.example.eat_together; // 確認套件名稱

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseHelper {

    // ★★★ 請填入您第一步複製的資料 ★★★
//    private static final String SUPABASE_URL = "https://wttrpkzltuesmeofvtal.supabase.co";
//    private static final String SUPABASE_KEY = "sb_publishable_F221TzkGRrNXN6FePJcO1A_ijBU5ip0";

    // cookie url / key
    private static final String SUPABASE_URL = "https://xkiopgopzmjuatxvnwci.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_pUe70HuQ4n3r4Vq5qlbytg_zJjZlP_3";

    private final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // 1. 取得餐廳列表 (GET)
    public String getRestaurants() throws IOException {
        // Supabase 格式: URL/table_name?select=*
        String url = SUPABASE_URL + "/restaurants?select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY) // 匿名存取也要帶 Bearer
                .build();

        // 執行請求 (這會在呼叫的地方被包在執行緒裡)
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string(); // 回傳 JSON 字串
        }
    }

    // 2. 新增餐廳 (POST)
    // 傳入的 jsonString 格式範例: {"name": "麥當勞", "address": "台中市...", "rating": 4.5}
    public String addRestaurant(String jsonString) throws IOException {
        String url = SUPABASE_URL + "/restaurants";

        RequestBody body = RequestBody.create(jsonString, JSON);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal") // 告訴 Supabase 不用回傳完整資料，省流量
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return "Success";
        }
    }
}