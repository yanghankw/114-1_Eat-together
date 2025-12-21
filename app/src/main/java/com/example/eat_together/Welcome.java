package com.example.eat_together;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler; // 用來處理延遲
import android.os.Looper;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Welcome extends AppCompatActivity {

    // 定義一個請求代碼，用來識別是哪一次的權限請求
    private static final int PERMISSION_REQUEST_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        ImageView imageView = findViewById(R.id.image);
        imageView.setImageResource(R.drawable.map);

        // 1. 檢查權限
        if (checkAllPermissions()) {
            // 如果原本就已經有權限了，直接開始倒數並跳轉
            startMainActivityWithDelay();
        } else {
            // 如果沒有權限，向使用者請求
            requestPermission();
        }
    }

    // 檢查是否所有需要的權限都已取得
    private boolean checkAllPermissions() {
        int fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fineLocation == PackageManager.PERMISSION_GRANTED && coarseLocation == PackageManager.PERMISSION_GRANTED;
    }

    // 發出請求
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                PERMISSION_REQUEST_CODE);
    }

    // ★★★ 關鍵：系統回傳使用者選擇結果的地方 ★★★
    // 當使用者點了「允許」或「拒絕」後，系統會自動呼叫這個方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 檢查是否真的授權了 (grantResults 長度大於 0 且第一個結果是 GRANTED)
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 使用者按了允許 -> 跳轉
                startMainActivityWithDelay();
            } else {
                // 使用者按了拒絕 -> 提示並關閉 App (或者停留在這裡)
                Toast.makeText(this, "需要定位權限才能使用本 App", Toast.LENGTH_LONG).show();
                // 您可以選擇 finish() 關閉 App，或是讓使用者卡在這裡
                // finish();
            }
        }
    }

    // 處理延遲跳轉的邏輯
    private void startMainActivityWithDelay() {
        // 使用 Handler 來延遲 2 秒，不會造成畫面凍結
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 跳轉到主頁
                Intent intent = new Intent(Welcome.this, MainActivity.class);
                startActivity(intent);
                finish(); // 結束 Welcome 頁面，這樣按返回鍵才不會回來
            }
        }, 2000); // 2000 毫秒 = 2 秒
    }
}