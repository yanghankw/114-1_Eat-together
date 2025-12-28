package com.example.eat_together;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog; // ★ 新增
import android.app.TimePickerDialog; // ★ 新增

// Google Location Services
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

// Google Maps
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// Google Places & Net
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds; // 記得確認有這個 import
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;           // ★ 新增
import java.util.Locale;             // ★ 新增

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SearchView searchView;
    private Button btnConfirm;
    private TextView tvWeather;

    private String currentPlaceName = "";
    private String currentPlaceAddress = "";

    private PlacesClient placesClient;
    private final OkHttpClient httpClient = new OkHttpClient();

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // ⚠️ API Keys
    private static final String GOOGLE_API_KEY = "AIzaSyCodnZMV_6vZGoj84AQ-52EUuKcLS4SiO0";
    private static final String WEATHER_API_KEY = "e0d78a2ca3mshcbdc60fbf8215f9p1918a0jsn29db0f8f842e";

    // 預設地點 (彰化)
    private static final LatLng DEFAULT_LOCATION_CHANGHUA = new LatLng(24.1788, 120.6467);
    private String groupId; // ★ 新增：用來記錄是哪個群組要辦活動

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // ★ 1. 接收群組 ID (從 ChatActivity 傳過來的)
        groupId = getIntent().getStringExtra("GROUP_ID");
        if (groupId == null) {
            // 如果沒有群組ID (可能是直接開啟地圖測試)，可以給個預設值或是提示
            // groupId = "1"; // 測試用
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (!TcpClient.getInstance().isConnected()) {
                TcpClient.getInstance().connect();
                // 注意：如果真的斷線重連，這裡其實還需要補做自動登入 (Auto Login)
                // 但通常只要 ChatActivity 沒斷線，這裡就不會進來
            }
        } catch (Exception e) {
            Log.e("MapActivity", "TCP Error: " + e.getMessage());
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), GOOGLE_API_KEY);
        }
        placesClient = Places.createClient(this);

        searchView = findViewById(R.id.sv_location);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        tvWeather = findViewById(R.id.tv_weather_info);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirm.setOnClickListener(v -> {
            if (currentPlaceName.isEmpty()) return;
            showDateTimePicker();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query == null || query.isEmpty()) return false;

                searchView.clearFocus();
                Toast.makeText(MapActivity.this, "搜尋附近 1km: " + query, Toast.LENGTH_SHORT).show();

                // 1. 取得目前地圖的中心點 (作為搜尋基準)
                LatLng center = mMap.getCameraPosition().target;

                // 2. 設定搜尋範圍：以中心點為圓心，半徑 1000 公尺 (1公里)
                CircularBounds circle = CircularBounds.newInstance(center, 1000.0);

                List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);

                // 3. 建立請求：設定 LocationBias (偏好搜尋範圍內) 並抓取 10 筆資料
                SearchByTextRequest request = SearchByTextRequest.builder(query, placeFields)
                        .setMaxResultCount(10) // ★ 修改：列出最多 10 間店
                        .setLocationBias(circle) // ★ 修改：鎖定 1 公里範圍
                        .build();

                placesClient.searchByText(request).addOnSuccessListener(response -> {
                    mMap.clear(); // 清除舊標記

                    if (!response.getPlaces().isEmpty()) {
                        // ★ 修改：使用迴圈，將所有搜尋到的結果都插上圖釘
                        for (Place place : response.getPlaces()) {
                            if (place.getLatLng() != null) {
                                mMap.addMarker(new MarkerOptions()
                                        .position(place.getLatLng())
                                        .title(place.getName())
                                        .snippet(place.getAddress()));
                            }
                        }

                        // 將鏡頭移動到「第一筆」結果，讓使用者看到搜尋區域
                        Place firstPlace = response.getPlaces().get(0);
                        if (firstPlace.getLatLng() != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPlace.getLatLng(), 15));

                            // 預設選中第一筆，方便直接按確認
                            currentPlaceName = firstPlace.getName();
                            currentPlaceAddress = firstPlace.getAddress();
                            btnConfirm.setVisibility(View.VISIBLE);

                            // 更新天氣
                            fetchWeather(firstPlace.getLatLng().latitude, firstPlace.getLatLng().longitude);
                        }

                        Toast.makeText(MapActivity.this, "找到 " + response.getPlaces().size() + " 間相關店家", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(MapActivity.this, "附近 1km 內找不到「" + query + "」", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Log.e("MapSearch", "搜尋失敗: " + e.getMessage());
                    Toast.makeText(MapActivity.this, "搜尋錯誤，請檢查 API Key", Toast.LENGTH_SHORT).show();
                });
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION_CHANGHUA, 14));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(1000)
                    .build();

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult == null) return;
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));
                            fetchWeather(location.getLatitude(), location.getLongitude());
                            fusedLocationClient.removeLocationUpdates(this);
                            return;
                        }
                    }
                }
            };
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Toast.makeText(this, "請開啟定位權限以取得精確位置", Toast.LENGTH_SHORT).show();
        }

        // 點擊圖釘時的事件
        mMap.setOnMarkerClickListener(marker -> {
            // 更新目前選中的地點資訊
            currentPlaceName = marker.getTitle();
            currentPlaceAddress = marker.getSnippet();

            // 顯示確認按鈕
            btnConfirm.setVisibility(View.VISIBLE);

            // 顯示圖釘上面的資訊小視窗
            marker.showInfoWindow();

            // 順便查該地點天氣
            fetchWeather(marker.getPosition().latitude, marker.getPosition().longitude);
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // ★★★ 3. 新增：日期與時間選擇器 ★★★
    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();

        // A. 先選日期
        new DatePickerDialog(this, (view, year, month, day) -> {

            // B. 再選時間
            new TimePickerDialog(this, (timeView, hour, minute) -> {

                // C. 格式化時間 (ISO 8601 格式，例如: 2025-01-01T18:30:00+08:00)
                // 為了讓 Supabase 準確讀到台灣時間，我們手動加上 +08:00
                String timeStr = String.format(Locale.getDefault(),
                        "%04d-%02d-%02dT%02d:%02d:00+08:00",
                        year, month + 1, day, hour, minute);

                // D. 發送指令
                sendCreateEvent(timeStr);

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ★★★ 4. 新增：發送建立指令 ★★★
    private void sendCreateEvent(String timeStr) {
        if (groupId == null) {
            Toast.makeText(this, "錯誤：無法識別群組 ID", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // 指令格式: CREATE_EVENT:群組ID:餐廳名:時間
                String cmd = "CREATE_EVENT:" + groupId + ":" + currentPlaceName + ":" + timeStr;

                TcpClient.getInstance().sendMessage(cmd);

                runOnUiThread(() -> {
                    Toast.makeText(this, "聚餐活動建立成功！", Toast.LENGTH_SHORT).show();
                    finish(); // 關閉地圖，回到聊天室
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void fetchWeather(double lat, double lon) {
        tvWeather.setVisibility(View.VISIBLE);
        tvWeather.setText("查詢天氣中...");
        String url = "https://weatherapi-com.p.rapidapi.com/current.json?q=" + lat + "," + lon;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-RapidAPI-Key", WEATHER_API_KEY)
                .addHeader("X-RapidAPI-Host", "weatherapi-com.p.rapidapi.com")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> tvWeather.setText("天氣讀取失敗"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject current = json.getJSONObject("current");
                        String info = "📍 " + json.getJSONObject("location").getString("name") +
                                " | " + current.getJSONObject("condition").getString("text") +
                                " " + current.getDouble("temp_c") + "°C";
                        runOnUiThread(() -> tvWeather.setText(info));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}