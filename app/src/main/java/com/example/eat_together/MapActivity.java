package com.example.eat_together;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SearchView searchView;
    private Button btnConfirm; // 新增按鈕變數

    // 暫存搜尋到的地點資訊
    private String currentPlaceName = "";
    private String currentPlaceAddress = "";

    // ... 原本的變數 ...
    private PlacesClient placesClient; // 定義 Places 客戶端

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 1. 初始化 Places SDK (請務必確認 AndroidManifest 裡有填 API Key)
        if (!Places.isInitialized()) {
            // 這裡填入您的 API KEY，建議直接讀取 Manifest 裡的，或者暫時貼字串
            // 為了安全，建議用 BuildConfig 或讀取 Manifest，這裡示範用字串：
            Places.initialize(getApplicationContext(), "AIzaSyCodnZMV_6vZGoj84AQ-52EUuKcLS4SiO0");
        }
        placesClient = Places.createClient(this);

        // ... 原本的 findViewById ...
        FloatingActionButton btnSearchNearby = findViewById(R.id.btn_search_nearby);

        // 2. 設定按鈕點擊事件
        btnSearchNearby.setOnClickListener(v -> {
            searchNearbyRestaurants();
        });

        searchView = findViewById(R.id.sv_location);
        btnConfirm = findViewById(R.id.btn_confirm_location); // 綁定按鈕

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // --- 按鈕點擊事件 ---
        btnConfirm.setOnClickListener(v -> {
            // 準備跳轉到 ChatActivity
            Intent intent = new Intent(MapActivity.this, ChatActivity.class);

            // 放入資料 (Key, Value)
            intent.putExtra("PLACE_NAME", currentPlaceName);
            intent.putExtra("PLACE_ADDRESS", currentPlaceAddress);

            // 為了測試方便，我們先假定傳給一個預設的群組名稱
            intent.putExtra("CHAT_NAME", "美食討論群");

            startActivity(intent);
            finish(); // 結束地圖頁面，這樣按返回鍵不會又回到地圖
        });

        // --- 搜尋監聽器 ---
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();
                List<Address> addressList = null;

                if (location != null || !location.equals("")) {
                    Geocoder geocoder = new Geocoder(MapActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                        // 儲存地點資訊
                        currentPlaceName = location;
                        if(address.getAddressLine(0) != null) {
                            currentPlaceAddress = address.getAddressLine(0);
                        } else {
                            currentPlaceAddress = location;
                        }

                        mMap.clear();

                        // ▼▼▼▼▼▼▼▼▼▼ 新增：製作自訂圖標 (開始) ▼▼▼▼▼▼▼▼▼▼
                        // 1. 設定圖標大小
                        int height = 133;
                        int width = 80;

                        // 2. 讀取圖片資源
                        // 注意：請確認 res/drawable 資料夾裡有沒有 'gray' 這張圖
                        // 如果沒有，請改用 R.drawable.ic_home 或其他存在的圖片
                        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.gray);

                        // 3. 縮放圖片
                        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

                        // 4. 轉換成地圖用的格式
                        BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
                        // ▲▲▲▲▲▲▲▲▲▲ 新增：製作自訂圖標 (結束) ▲▲▲▲▲▲▲▲▲▲


                        // ▼▼▼▼▼▼▼▼▼▼ 修改：加入 .icon(smallMarkerIcon) ▼▼▼▼▼▼▼▼▼▼
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(currentPlaceName)
                                .icon(smallMarkerIcon)); // ★ 這裡設定圖標
                        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

                        btnConfirm.setVisibility(View.VISIBLE);

                    } else {
                        Toast.makeText(MapActivity.this, "找不到地點", Toast.LENGTH_SHORT).show();
                        btnConfirm.setVisibility(View.GONE);
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    // ★★★ 核心功能：搜尋目前鏡頭附近的餐廳 ★★★
    private void searchNearbyRestaurants() {
        if (mMap == null) return;

        // 1. 取得地圖目前的中心點
        LatLng center = mMap.getCameraPosition().target;

        // 2. 定義要回傳哪些資料 (名字、座標、地址、評分、ID)
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.RATING); // 加上評分

        // 3. 設定搜尋半徑 (例如 1000 公尺)
        CircularBounds circle = CircularBounds.newInstance(center, 1000.0);

        // 4. 建立搜尋請求 (搜尋關鍵字：Restaurant)
        SearchByTextRequest searchRequest = SearchByTextRequest.builder("Restaurant", placeFields)
                .setMaxResultCount(10) // 限制只抓 10 間，省流量
                .setLocationBias(circle) // 偏好搜尋圓圈範圍內
                .build();

        // 5. 發送請求
        placesClient.searchByText(searchRequest).addOnSuccessListener(response -> {
            mMap.clear(); // 清除舊標記

            for (Place place : response.getPlaces()) {
                LatLng latLng = place.getLatLng();
                String name = place.getName();
                String address = place.getAddress();
                Double rating = place.getRating();

                // 處理評分顯示
                String snippet = "評分: " + (rating != null ? rating : "無") + " / " + address;

                if (latLng != null) {
                    mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(name)
                            .snippet(snippet)); // 點擊標記會顯示評分和地址
                }
            }

            // 提示使用者
            Toast.makeText(MapActivity.this, "找到附近 " + response.getPlaces().size() + " 間餐廳", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(exception -> {
            Log.e("MapActivity", "Place not found: " + exception.getMessage());
            Toast.makeText(MapActivity.this, "搜尋失敗，請檢查 API Key 權限", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // ... (原本的權限檢查) ...
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        // ... (原本的 UI 設定) ...
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // ▼▼▼▼▼▼▼▼▼▼ 新增這行：設定地圖內縮 ▼▼▼▼▼▼▼▼▼▼
        // 參數順序：左, 上, 右, 下 (單位是像素 pixel)
        // 設定上方 (Top) 內縮 200 像素，把按鈕擠下來，避開搜尋框
        mMap.setPadding(0, 200, 0, 0);
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        // ... (原本的移動鏡頭) ...
        LatLng taiwan = new LatLng(23.6978, 120.9605);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taiwan, 7));

        // 加入標記點擊監聽
        mMap.setOnMarkerClickListener(marker -> {
            // 當使用者點擊某個餐廳標記時
            currentPlaceName = marker.getTitle();
            currentPlaceAddress = marker.getSnippet(); // 這裡可能會包含評分文字，您可以自行處理字串切割

            // 顯示確認按鈕
            btnConfirm.setVisibility(View.VISIBLE);

            // 顯示資訊視窗 (就是那個小白框)
            marker.showInfoWindow();
            return true; // 回傳 true 代表我們自己處理了點擊，地圖不用再預設動作
        });
    }
}