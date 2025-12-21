package com.example.eat_together;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnRegister, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.et_reg_email);
        etPassword = findViewById(R.id.et_reg_password);
        btnRegister = findViewById(R.id.btn_confirm_register);
        btnBack = findViewById(R.id.btn_back_login);

        // 返回按鈕
        btnBack.setOnClickListener(v -> finish());

        // 註冊按鈕
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.length() < 6) {
                Toast.makeText(this, "請輸入有效 Email 及 6位數以上密碼", Toast.LENGTH_SHORT).show();
                return;
            }

            // 發送指令給 Server
            sendRegisterCommand(email, password);
        });
    }

    private void sendRegisterCommand(String email, String password) {
        // 1. 基本檢查：Supabase 要求密碼至少要 6 位數
        if (password.length() < 6) {
            Toast.makeText(this, "密碼長度不能少於 6 位", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            // 1. 確保連線
            TcpClient.getInstance().connect();

            // 2. 發送註冊指令
            String cmd = "REGISTER:" + email + ":" + password;
            TcpClient.getInstance().sendMessage(cmd);

            // 3. (模擬) 假設成功，實際情況可以監聽 Server 回傳
            // 為了簡單起見，我們發送後 1 秒自動當作成功
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            runOnUiThread(() -> {
                Toast.makeText(this, "註冊請求已發送", Toast.LENGTH_SHORT).show();
                finish(); // 關閉註冊頁面，回到登入頁
            });
        }).start();
    }
}