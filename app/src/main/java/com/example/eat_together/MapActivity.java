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
    private Button btnConfirm; // 確認按鈕

    // 暫存使用者目前選中的地點資訊
    private String currentPlaceName = "";
    private String currentPlaceAddress = "";

    private PlacesClient placesClient; // Google Places 客戶端

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 1. 初始化 Places SDK
        // ⚠️ 注意：為了安全，建議將 API Key 移至 local.properties 或 AndroidManifest，不要直接寫在 Code 裡
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "你的_API_KEY_記得換回來");
        }
        placesClient = Places.createClient(this);

        // 2. 初始化 UI 元件
        FloatingActionButton btnSearchNearby = findViewById(R.id.btn_search_nearby);
        searchView = findViewById(R.id.sv_location);
        btnConfirm = findViewById(R.id.btn_confirm_location);

        // 3. 設定按鈕點擊事件：搜尋附近餐廳
        btnSearchNearby.setOnClickListener(v -> {
            searchNearbyRestaurants();
        });

        // 4. 初始化地圖 Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // ==========================================
        // 🔥 關鍵修改：確認地點並通知 Server
        // ==========================================
        btnConfirm.setOnClickListener(v -> {
            // A. 檢查是否有地點資料
            if (currentPlaceName.isEmpty()) {
                Toast.makeText(this, "請先選擇一個地點", Toast.LENGTH_SHORT).show();
                return;
            }

            // B. 透過 TCP 通知 Server 建立新活動
            // 格式範例： NEW_EVENT:屋馬燒肉:台中市西屯區...
            String msg = "NEW_EVENT:" + currentPlaceName + ":" + currentPlaceAddress;
            
            // 呼叫 TcpClient 發送 (一定要確認 TcpClient 已經連線)
            TcpClient.getInstance().sendMessage(msg);

            // C. 跳轉到 ChatActivity
            Intent intent = new Intent(MapActivity.this, ChatActivity.class);
            // 放入資料傳給下一頁
            intent.putExtra("PLACE_NAME", currentPlaceName);
            intent.putExtra("PLACE_ADDRESS", currentPlaceAddress);
            intent.putExtra("CHAT_NAME", "美食討論群");

            startActivity(intent);
            finish(); // 結束地圖頁面
        });
        // ==========================================

        // --- 搜尋框監聽器 (輸入地址搜尋) ---
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();
                List<Address> addressList = null;

                if (location != null && !location.equals("")) {
                    Geocoder geocoder = new Geocoder(MapActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                        // 更新選中地點資訊
                        currentPlaceName = location;
                        currentPlaceAddress = (address.getAddressLine(0) != null) ? address.getAddressLine(0) : location;

                        mMap.clear();

                        // 製作自訂圖標 (如果有 gray.png)
                        // 如果沒有 gray 圖片，這裡會報錯，建議先用預設圖標測試
                        // BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.gray); 
                        
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(currentPlaceName)
                                // .icon(icon) // 若無圖片先註解這行
                        );

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                        btnConfirm.setVisibility(View.VISIBLE); // 顯示確認按鈕

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

    // --- 核心功能：搜尋附近餐廳 ---
    private void searchNearbyRestaurants() {
        if (mMap == null) return;

        LatLng center = mMap.getCameraPosition().target;

        // 定義要取得的欄位
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.RATING);

        // 設定搜尋半徑 (1000公尺)
        CircularBounds circle = CircularBounds.newInstance(center, 1000.0);

        // 建立搜尋請求
        SearchByTextRequest searchRequest = SearchByTextRequest.builder("Restaurant", placeFields)
                .setMaxResultCount(10)
                .setLocationBias(circle)
                .build();

        placesClient.searchByText(searchRequest).addOnSuccessListener(response -> {
            mMap.clear(); // 清除舊標記

            for (Place place : response.getPlaces()) {
                LatLng latLng = place.getLatLng();
                String name = place.getName();
                String address = place.getAddress();
                Double rating = place.getRating();
                String snippet = "評分: " + (rating != null ? rating : "無") + " / " + address;

                // 處理自訂圖標 (縮放)
                try {
                    // ⚠️ 請確認 drawable 資料夾有 gray 這張圖，否則改用 defaultMarker()
                    Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.gray);
                    Bitmap smallMarker = Bitmap.createScaledBitmap(b, 80, 133, false);
                    BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);

                    if (latLng != null) {
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(name)
                                .snippet(snippet)
                                .icon(smallMarkerIcon));
                    }
                } catch (Exception e) {
                    // 如果圖片讀取失敗，用預設紅點
                    if (latLng != null) {
                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(name)
                                .snippet(snippet));
                    }
                }
            }
            Toast.makeText(MapActivity.this, "找到附近 " + response.getPlaces().size() + " 間餐廳", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(exception -> {
            Log.e("MapActivity", "Place not found: " + exception.getMessage());
            Toast.makeText(MapActivity.this, "搜尋失敗 (檢查 API Key)", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 權限檢查
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 建議加入 requestPermissions 邏輯，這裡先 return
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        
        // 設定地圖內縮，避開頂部 UI
        mMap.setPadding(0, 200, 0, 0);

        // 移動鏡頭到台灣中心 (預設)
        LatLng taiwan = new LatLng(23.6978, 120.9605);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taiwan, 7));

        // 點擊標記事件
        mMap.setOnMarkerClickListener(marker -> {
            currentPlaceName = marker.getTitle();
            currentPlaceAddress = marker.getSnippet(); // 或是自己處理字串

            btnConfirm.setVisibility(View.VISIBLE);
            marker.showInfoWindow();
            return true;
        });
    }
}