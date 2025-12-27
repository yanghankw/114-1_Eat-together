package com.example.eat_together;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class ProfileFragment extends Fragment {

    // Google Sign In
    private GoogleSignInClient mGoogleSignInClient;
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleSignInResult(task);
                }
            }
    );

    // UI 元件 - 登入表單
    private EditText etEmail, etPassword;
    private Button btnEmailLogin;

    private Button btnGoRegister;
    private SignInButton btnGoogleSignIn;

    // UI 元件 - 個人資料
    private ImageView ivProfile;
    private TextView tvName, tvEmail, tvBio;


    // 修改容器變數類型
    private LinearLayout layoutLogin;   // 未登入畫面
    private ScrollView layoutProfile;   // 已登入畫面 (改成 ScrollView)

    // 新增綁定
    private TextView btnLogoutIcon;
    private TextView btnSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 1. 綁定元件
        initViews(view);

        // 2. 設定 Google 登入選項
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // 3. 設定按鈕事件
        btnGoogleSignIn.setOnClickListener(v -> googleSignIn());

        btnEmailLogin.setOnClickListener(v -> emailLogin());

        // 4. 設定個性簽名點擊事件 (模擬編輯功能)
        tvBio.setOnClickListener(v -> {
            Toast.makeText(getContext(), "未來可在此編輯個性簽名", Toast.LENGTH_SHORT).show();
        });

        // 修改登出按鈕事件 (綁定到右上角圖示 或 設定按鈕)
        btnLogoutIcon.setOnClickListener(v -> signOut());

        // 在 onCreateView 裡綁定並設定監聽
        btnGoRegister = view.findViewById(R.id.btn_go_to_register);

        btnGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RegisterActivity.class);
            startActivity(intent);
        });
// 讓使用者點擊名字時，可以修改名字
        tvName.setOnClickListener(v -> {
            showEditNameDialog();
        });

// 或者你也可以綁定在 settings 按鈕
        btnSettings.setOnClickListener(v -> {
            showEditNameDialog();
        });
        return view;
    }

    private void initViews(View view) {
        layoutLogin = view.findViewById(R.id.layout_login);
        layoutProfile = view.findViewById(R.id.layout_profile);

        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnEmailLogin = view.findViewById(R.id.btn_email_login);
        btnGoogleSignIn = view.findViewById(R.id.sign_in_button);
        btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE); // 改成寬版按鈕比較好看

        // 綁定新的 Profile 介面元件
        ivProfile = view.findViewById(R.id.iv_profile_pic);
        tvName = view.findViewById(R.id.tv_name);
        tvEmail = view.findViewById(R.id.tv_email);
        tvBio = view.findViewById(R.id.tv_bio);

        btnLogoutIcon = view.findViewById(R.id.btn_logout_icon);
        btnSettings = view.findViewById(R.id.btn_settings);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ★ 修改邏輯：先檢查我們自己的登入，再檢查 Google
        if (checkLocalLogin()) {
            return; // 如果本地已有登入紀錄，就不用管 Google 了
        }
        // 檢查是否已經有 Google 登入紀錄
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        updateUI(account);
    }

    // 顯示修改名字的對話框
    private void showEditNameDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("修改暱稱");

        // 設定輸入框
        final EditText input = new EditText(getContext());
        input.setHint("請輸入新名字");
        builder.setView(input);

        // 設定「確定」按鈕
        builder.setPositiveButton("確定", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                // 執行你原本想寫的 Server 請求
                updateNameOnServer(newName);
            }
        });

        // 設定「取消」按鈕
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

// 在 ProfileFragment.java 中

    private void updateNameOnServer(String newName) {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String userUuid = prefs.getString("user_id", null);
//        String userName = prefs.getString("username", null);
        if (userUuid == null) {
            Toast.makeText(getContext(), "錯誤：找不到使用者 ID", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            client.connect();

            String cmd = "UPDATE_NAME:" + userUuid + ":" + newName;
            String response = client.sendRequest(cmd);

            getActivity().runOnUiThread(() -> {
                if (response != null && response.equals("UPDATE_NAME_SUCCESS")) {
                    tvName.setText(newName);
                    Toast.makeText(getContext(), "更名成功！", Toast.LENGTH_SHORT).show();

                    // ★★★ 關鍵修正：必須更新 SharedPreferences ★★★
                    // 請注意：這裡的 Key 必須是 "username"，因為你在 checkLocalLogin 是用這個 Key 讀取的
                    prefs.edit()
                            .putString("username", newName)
                            .apply();

                } else {
                    Toast.makeText(getContext(), "更名失敗，伺服器無回應", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // --- 功能實作區 ---

    // ★ 新增這個方法：檢查 SharedPreferences 裡的紀錄
    private boolean checkLocalLogin() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String userEmail = prefs.getString("user_email", "使用者"); // 建議您在登入成功時順便存 email
        String userName = prefs.getString("username", "匿名");

        if (userId != null) {
            // === 有紀錄，直接切換到已登入畫面 ===
            layoutLogin.setVisibility(View.GONE);
            layoutProfile.setVisibility(View.VISIBLE);

            tvName.setText(userName);
            tvEmail.setText(userEmail);
            tvBio.setText("歡迎回來！");
            return true;
        }
        return false;
    }

    // 1. Email 登入邏輯

    // 修改按鈕點擊事件，讓它變成「註冊/登入」二合一，或是區分開來
    // 這裡示範：發送註冊指令
    // 1. Email 登入邏輯
    // ProfileFragment.java

    private void emailLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) return;

        // 顯示登入中...
        Toast.makeText(getContext(), "登入驗證中...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            client.connect(); // 確保連線

            // 1. 組合指令 LOGIN:帳號:密碼
            String command = "LOGIN:" + email + ":" + password;

            // 2. 發送並等待結果 (使用剛剛寫的新方法)
            String response = client.sendRequest(command);

            // 3. 回到主執行緒處理 UI
            getActivity().runOnUiThread(() -> {
                // ★ 修改判斷邏輯
                if (response != null && response.startsWith("LOGIN_SUCCESS")) {
                    // 切割出 UUID
                    // response 可能是 "LOGIN_SUCCESS:550e8400-..."
                    String[] parts = response.split(":");
                    if (parts.length == 2) {
                        String userId = parts[1];

                        // ★ 儲存 ID 到手機 (SharedPreferences)
                        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_id", userId)
                                .putString("user_email", email)     // 記得存 Email
                                .putString("user_password", password) // ★ 關鍵：存密碼
                                .apply();

                        Toast.makeText(getContext(), "登入成功！", Toast.LENGTH_SHORT).show();
                        // ... 切換 UI ...
                        layoutLogin.setVisibility(View.GONE);
                        layoutProfile.setVisibility(View.VISIBLE);
                        tvName.setText(userId);
                        tvEmail.setText(email);
                    }
                } else {
                    Toast.makeText(getContext(), "登入失敗", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // 2. Google 登入邏輯
    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            updateUI(account); // 登入成功
        } catch (ApiException e) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
            Toast.makeText(getContext(), "Google 登入失敗", Toast.LENGTH_SHORT).show();
        }
    }

    // 3. 登出邏輯
    private void signOut() {
        // 清除 SharedPreferences (本地登入紀錄)
        requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // 如果是 Google 登入，需要呼叫 Google 的登出
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            // 清除 UI
            updateUI(null);

            // 也要清空輸入框
            etEmail.setText("");
            etPassword.setText("");
            Toast.makeText(getContext(), "已登出", Toast.LENGTH_SHORT).show();
        });
    }

    // 4. UI 狀態切換 (核心邏輯)
    private void updateUI(GoogleSignInAccount account) {
        if (account != null) {
            // === 已登入 (Google) ===
            layoutLogin.setVisibility(View.GONE);
            layoutProfile.setVisibility(View.VISIBLE);

            tvName.setText(account.getDisplayName());
            tvEmail.setText(account.getEmail());
            // 如果有頭像 URL，未來可以用 Glide 載入，目前先用預設圖
            // ivProfile.setImageURI(account.getPhotoUrl());

        } else {
            // === 未登入 ===
            layoutLogin.setVisibility(View.VISIBLE);
            layoutProfile.setVisibility(View.GONE);
        }
    }
}