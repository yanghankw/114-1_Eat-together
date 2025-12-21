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
        // ⚠️ 記得把下面的 API KEY 換成真的
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCodnZMV_6vZGoj84AQ-52EUuKcLS4SiO0");
        }
        placesClient = Places.createClient(this);

        FloatingActionButton btnSearchNearby = findViewById(R.id.btn_search_nearby);
        searchView = findViewById(R.id.sv_location);
        btnConfirm = findViewById(R.id.btn_confirm_location);

        btnSearchNearby.setOnClickListener(v -> searchNearbyRestaurants());

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
            startActivity(intent);
            // finish(); // 看需求決定要不要關閉地圖
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
    }
}