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

import java.io.IOException;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SearchView searchView;
    private Button btnConfirm; // 新增按鈕變數

    // 暫存搜尋到的地點資訊
    private String currentPlaceName = "";
    private String currentPlaceAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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
    }
}