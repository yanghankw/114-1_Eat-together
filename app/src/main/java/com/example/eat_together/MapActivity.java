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
import android.widget.TextView;
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

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    // ⚠️ 請確認這是有效的 API Key
    private static final String GOOGLE_API_KEY = "AIzaSyCodnZMV_6vZGoj84AQ-52EUuKcLS4SiO0";
    // ⚠️ 請確認這是有效的 RapidAPI Key
    private static final String WEATHER_API_KEY = "e0d78a2ca3mshcbdc60fbf8215f9p1918a0jsn29db0f8f842e";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 嘗試連線 Server (建議包在 try-catch 避免 Server 沒開導致閃退)
        try {
            TcpClient.getInstance().connect();
        } catch (Exception e) {
            Log.e("MapActivity", "TCP 連線錯誤: " + e.getMessage());
        }

        // 初始化 Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), GOOGLE_API_KEY);
        }
        placesClient = Places.createClient(this);

        FloatingActionButton btnSearchNearby = findViewById(R.id.btn_search_nearby);
        searchView = findViewById(R.id.sv_location);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        tvWeather = findViewById(R.id.tv_weather_info);

        btnSearchNearby.setOnClickListener(v -> searchNearbyRestaurants());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirm.setOnClickListener(v -> {
            if (currentPlaceName.isEmpty()) {
                Toast.makeText(this, "請先選擇一個地點", Toast.LENGTH_SHORT).show();
                return;
            }
            // 避免 TCP 還沒連上就傳送導致閃退
            new Thread(() -> {
                try {
                    String msg = "NEW_EVENT:" + currentPlaceName + ":" + currentPlaceAddress;
                    TcpClient.getInstance().sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            Toast.makeText(this, "已發送活動通知！", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MapActivity.this, ChatActivity.class);
            intent.putExtra("PLACE_NAME", currentPlaceName);
            intent.putExtra("PLACE_ADDRESS", currentPlaceAddress);
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchView.getQuery().toString();

                if (location == null || location.equals("")) {
                    return false;
                }

                Toast.makeText(MapActivity.this, "搜尋中...", Toast.LENGTH_SHORT).show();

                // ★ 修改重點：將 Geocoder 搜尋移到背景執行緒，避免主畫面卡死閃退
                new Thread(() -> {
                    List<Address> addressList = null;
                    Geocoder geocoder = new Geocoder(MapActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    List<Address> finalAddressList = addressList;
                    runOnUiThread(() -> {
                        if (finalAddressList != null && !finalAddressList.isEmpty()) {
                            Address address = finalAddressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                            currentPlaceName = location;
                            currentPlaceAddress = (address.getAddressLine(0) != null) ? address.getAddressLine(0) : location;

                            if (mMap != null) {
                                mMap.clear();
                                mMap.addMarker(new MarkerOptions().position(latLng).title(currentPlaceName));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                            }
                            btnConfirm.setVisibility(View.VISIBLE);

                            // 抓取天氣
                            fetchWeather(latLng.latitude, latLng.longitude);
                        } else {
                            Toast.makeText(MapActivity.this, "找不到地點，請換個關鍵字", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });
    }

    private void fetchWeather(double lat, double lon) {
        tvWeather.setVisibility(View.VISIBLE);
        tvWeather.setText("正在查詢當地天氣...");

        // ★ 修改重點：這裡原本網址格式寫錯了，已修正
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
                e.printStackTrace();
                runOnUiThread(() -> tvWeather.setText("天氣讀取失敗 (網路錯誤)"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);

                        JSONObject current = json.getJSONObject("current");
                        double tempC = current.getDouble("temp_c");
                        String conditionText = current.getJSONObject("condition").getString("text");
                        String locName = json.getJSONObject("location").getString("name");

                        String weatherInfo = "📍 " + locName + " 天氣: " + conditionText + " | 溫度: " + tempC + "°C";

                        runOnUiThread(() -> tvWeather.setText(weatherInfo));

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> tvWeather.setText("天氣資料解析錯誤"));
                    }
                } else {
                    runOnUiThread(() -> tvWeather.setText("無法取得天氣 (Code: " + response.code() + ")"));
                }
            }
        });
    }

    private void searchNearbyRestaurants() {
        if (mMap == null) return;
        LatLng center = mMap.getCameraPosition().target;

        fetchWeather(center.latitude, center.longitude);

        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.RATING);
        CircularBounds circle = CircularBounds.newInstance(center, 1000.0);

        // 檢查 API Key 權限，如果 Key 沒開通 Places API，這行會失敗
        try {
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

                    BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(); // 預設紅色

                    // 安全讀取圖片，避免閃退
                    try {
                        // 確保 res/drawable/gray 存在，不然就用預設紅點
                        int resId = getResources().getIdentifier("gray", "drawable", getPackageName());
                        if (resId != 0) {
                            Bitmap b = BitmapFactory.decodeResource(getResources(), resId);
                            if (b != null) {
                                Bitmap smallMarker = Bitmap.createScaledBitmap(b, 80, 133, false);
                                icon = BitmapDescriptorFactory.fromBitmap(smallMarker);
                            }
                        }
                    } catch (Exception e) {
                        // 忽略圖片錯誤，使用預設圖標
                    }

                    mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(place.getName())
                            .snippet(snippet)
                            .icon(icon));
                }
            }).addOnFailureListener(e -> {
                Log.e("Map", "Search failed: " + e.getMessage());
                Toast.makeText(MapActivity.this, "搜尋附近失敗，請檢查 API Key", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Toast.makeText(MapActivity.this, "Places API 初始化失敗", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(23.6978, 120.9605), 7));

        mMap.setOnMarkerClickListener(marker -> {
            currentPlaceName = marker.getTitle();
            currentPlaceAddress = marker.getSnippet();
            btnConfirm.setVisibility(View.VISIBLE);

            fetchWeather(marker.getPosition().latitude, marker.getPosition().longitude);

            marker.showInfoWindow();
            return true;
        });
    }
}