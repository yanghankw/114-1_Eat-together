package com.example.eat_together;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
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
        // --- 加入這段程式碼來移除底線 ---
        int plateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        View plate = searchView.findViewById(plateId);
        if (plate != null) {
            plate.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

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
                        // 如果有詳細地址就抓，沒有就用輸入的名稱
                        if(address.getAddressLine(0) != null) {
                            currentPlaceAddress = address.getAddressLine(0);
                        } else {
                            currentPlaceAddress = location;
                        }

                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng).title(currentPlaceName));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

                        // ★★★ 搜尋成功，顯示按鈕 ★★★
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
        LatLng taiwan = new LatLng(23.6978, 120.9605);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(taiwan, 7));
    }
}