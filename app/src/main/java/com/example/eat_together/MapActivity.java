package com.example.eat_together;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.ValueAnimator;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
    private Button btnConfirm;

    private String currentPlaceName = "";
    private String currentPlaceAddress = "";

    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // ★ 修改 1: 一進畫面就先嘗試連線 TCP Server
        // 這樣等到使用者選好餐廳按按鈕時，連線通常已經準備好了
        TcpClient.getInstance().connect();

        // 初始化 Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCodnZMV_6vZGoj84AQ-52EUuKcLS4SiO0");
        }
        placesClient = Places.createClient(this);

        // ★ 先綁定 SearchView (移到最前面)
        searchView = findViewById(R.id.sv_location);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        FloatingActionButton btnSearchNearby = findViewById(R.id.btn_search_nearby);

        // 1. 找到元件
        LinearLayout bottomDrawer = findViewById(R.id.bottom_drawer_container);
        SearchView svHistory = findViewById(R.id.sv_history);
        View mapContainer = findViewById(R.id.map_container);

        // 2. 計算 RecyclerView 的高度 (220dp) 轉為像素
        // 這就是我們要隱藏(往下推)的距離
        final float slideDistance = 230 * getResources().getDisplayMetrics().density;

        // ★★★ 關鍵：初始狀態設定 ★★★
        // 使用 post 確保 Layout 已經渲染完成，能取得 bottomDrawer.getHeight()
        bottomDrawer.post(() -> {
            // 預設為收起狀態
            bottomDrawer.setTranslationY(slideDistance);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mapContainer.getLayoutParams();
            params.bottomMargin = (int) (bottomDrawer.getHeight() - slideDistance);
            mapContainer.setLayoutParams(params);
        });

        // 3. 定義動畫邏輯
        View.OnClickListener toggleAction = v -> {
            // 取得目前的位移量
            float currentTranslation = bottomDrawer.getTranslationY();

            if (currentTranslation > 0) {
                // 如果目前是縮下去的狀態 -> 往上滑 (顯示列表)
                animateDrawerAndMap(0);
                svHistory.setIconified(false); // 展開輸入框
            } else {
                // (選用) 如果目前是展開狀態 -> 往下滑 (隱藏列表)
                // animateDrawerAndMap(slideDistance);
            }
        };

        // 4. 綁定點擊事件
        svHistory.setOnClickListener(toggleAction);
        svHistory.setOnSearchClickListener(toggleAction); // 點擊放大鏡圖示
        
        // 點擊輸入框取得焦點時也觸發
        // --- 監聽搜尋框焦點 ---
        svHistory.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // 展開：位移回到 0
                animateDrawerAndMap(0);
            } else {
                // 收起：位移推下 slideDistance
                animateDrawerAndMap(slideDistance);
            }
        });

        // --- 移除 SearchView (頂部) 底線的程式碼 ---
        int plateId = searchView.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        if (plateId != 0) {
            View plateView = searchView.findViewById(plateId);
            if (plateView != null) {
                plateView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
        
        // ★★★ 新增：移除歷史紀錄搜尋框 (svHistory) 的底線 ★★★
        int historyPlateId = svHistory.getContext().getResources().getIdentifier("android:id/search_plate", null, null);
        if (historyPlateId != 0) {
            View plateView = svHistory.findViewById(historyPlateId);
            if (plateView != null) {
                plateView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }
        // ------------------------------------

        btnSearchNearby.setOnClickListener(v -> {
            searchNearbyRestaurants();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // --- 確認按鈕 ---
        btnConfirm.setOnClickListener(v -> {
            if (currentPlaceName.isEmpty()) {
                Toast.makeText(this, "請先選擇一個地點", Toast.LENGTH_SHORT).show();
                return;
            }

            // 發送 TCP 指令
            String msg = "NEW_EVENT:" + currentPlaceName + ":" + currentPlaceAddress;
            TcpClient.getInstance().sendMessage(msg);

            Toast.makeText(this, "已發送活動通知！", Toast.LENGTH_SHORT).show();

            // 跳轉到聊天室
            Intent intent = new Intent(MapActivity.this, ChatActivity.class);
            intent.putExtra("PLACE_NAME", currentPlaceName);
            intent.putExtra("PLACE_ADDRESS", currentPlaceAddress);
            intent.putExtra("CHAT_NAME", "美食討論群");
            startActivity(intent);
            finish();
        });

        // --- 搜尋框邏輯 (簡化版) ---
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

                        currentPlaceName = location;
                        currentPlaceAddress = (address.getAddressLine(0) != null) ? address.getAddressLine(0) : location;

                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng).title(currentPlaceName));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                        btnConfirm.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(MapActivity.this, "找不到地點", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });
    }

    private void animateDrawerAndMap(float targetTranslationY) {
        final LinearLayout bottomDrawer = findViewById(R.id.bottom_drawer_container);
        final View mapContainer = findViewById(R.id.map_container);

        // 取得目前的位移作為起點
        float startValue = bottomDrawer.getTranslationY();

        ValueAnimator animator = ValueAnimator.ofFloat(startValue, targetTranslationY);
        animator.setDuration(300); // 300 毫秒的平滑動畫
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();

            // 1. 同步移動抽屜的位置 (物理位置不變，繪圖位置改變)
            bottomDrawer.setTranslationY(animatedValue);

            // 2. 動態調整地圖的 MarginBottom (物理邊界改變)
            // 地圖底部留白 = 抽屜總高度 - 抽屜下移的位移量
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mapContainer.getLayoutParams();
            params.bottomMargin = (int) (bottomDrawer.getHeight() - animatedValue);
            mapContainer.setLayoutParams(params);
        });
        animator.start();
    }

    private void searchNearbyRestaurants() {
        if (mMap == null) return;
        LatLng center = mMap.getCameraPosition().target;

        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.RATING);
        CircularBounds circle = CircularBounds.newInstance(center, 1000.0);
        SearchByTextRequest searchRequest = SearchByTextRequest.builder("Restaurant", placeFields)
                .setMaxResultCount(10)
                .setLocationBias(circle)
                .build();

        placesClient.searchByText(searchRequest).addOnSuccessListener(response -> {
            mMap.clear();
            for (Place place : response.getPlaces()) {
                LatLng latLng = place.getLatLng();
                if (latLng == null) continue;

                String snippet = "評分: " + (place.getRating() != null ? place.getRating() : "無");

                // ★ 修改 2: 安全的圖片讀取，防止 gray 不存在導致閃退
                BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(); // 預設用紅點
                try {
                    Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.gray);
                    if (b != null) { // 檢查圖片是否讀取成功
                        Bitmap smallMarker = Bitmap.createScaledBitmap(b, 80, 133, false);
                        icon = BitmapDescriptorFactory.fromBitmap(smallMarker);
                    }
                } catch (Exception e) {
                    // 圖片讀取失敗不做事，直接用紅點
                }

                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(place.getName())
                        .snippet(snippet)
                        .icon(icon));
            }
        }).addOnFailureListener(e -> Log.e("Map", "Search failed: " + e.getMessage()));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 簡單權限檢查
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(23.6978, 120.9605), 7));

        mMap.setOnMarkerClickListener(marker -> {
            currentPlaceName = marker.getTitle();
            currentPlaceAddress = marker.getSnippet();
            btnConfirm.setVisibility(View.VISIBLE);
            marker.showInfoWindow();
            return true;
        });

        // ★★★ 新增：設定地圖點擊監聽器 ★★★
        mMap.setOnMapClickListener(latLng -> {
            // 1. 收回抽屜
            SearchView svHistory = findViewById(R.id.sv_history);

            // 2. 清除搜尋框焦點 (這會觸發上面的 onFocusChange，並自動隱藏鍵盤)
            if (svHistory != null) {
                svHistory.clearFocus();
            }
        });
    }
}
