package com.example.eat_together;

import android.content.Intent;
import android.content.SharedPreferences;
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

// ★ 新增：Glide 的引用
import com.bumptech.glide.Glide;

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

    // 容器變數
    private LinearLayout layoutLogin;   // 未登入畫面
    private ScrollView layoutProfile;   // 已登入畫面

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

        // 4. 設定個性簽名點擊事件
        tvBio.setOnClickListener(v -> {
            Toast.makeText(getContext(), "未來可在此編輯個性簽名", Toast.LENGTH_SHORT).show();
        });

        // 修改登出按鈕事件
        btnLogoutIcon.setOnClickListener(v -> signOut());

        // 綁定去註冊頁面按鈕
        btnGoRegister = view.findViewById(R.id.btn_go_to_register);
        btnGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RegisterActivity.class);
            startActivity(intent);
        });

        // 讓使用者點擊名字時，可以修改名字
        tvName.setOnClickListener(v -> showEditNameDialog());

        // 設定按鈕也可以修改名字
        btnSettings.setOnClickListener(v -> showEditNameDialog());

        return view;
    }

    private void initViews(View view) {
        layoutLogin = view.findViewById(R.id.layout_login);
        layoutProfile = view.findViewById(R.id.layout_profile);

        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnEmailLogin = view.findViewById(R.id.btn_email_login);
        btnGoogleSignIn = view.findViewById(R.id.sign_in_button);
        btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);

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

    // ★★★ 新增：RoboHash 載入頭像功能 (整合 Glide) ★★★
    private void loadRoboAvatar(String key) {
        if (key == null || key.isEmpty()) return;

        // 組合網址: 使用 email 或 user_id 當作 key，這樣頭像會固定
        // set1 = 機器人, set2 = 怪獸, set4 = 貓咪
        String url = "https://robohash.org/" + key + ".png?set=set1";

        // 確保 Fragment 還活著，避免 Crash
        if (isAdded() && getContext() != null) {
            Glide.with(this)
                    .load(url)
                    .circleCrop() // 自動切成圓形
                    .placeholder(R.drawable.ic_launcher_background) // 載入中顯示的預設圖(可自行換成預設頭像資源)
                    .into(ivProfile);
        }
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
                updateNameOnServer(newName);
            }
        });

        // 設定「取消」按鈕
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateNameOnServer(String newName) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String userUuid = prefs.getString("user_id", null);

        if (userUuid == null) {
            Toast.makeText(getContext(), "錯誤：找不到使用者 ID", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            client.connect();

            String cmd = "UPDATE_NAME:" + userUuid + ":" + newName;
            String response = client.sendRequest(cmd);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (response != null && response.equals("UPDATE_NAME_SUCCESS")) {
                        tvName.setText(newName);
                        Toast.makeText(getContext(), "更名成功！", Toast.LENGTH_SHORT).show();

                        // 更新 SharedPreferences
                        prefs.edit()
                                .putString("username", newName)
                                .apply();

                    } else {
                        Toast.makeText(getContext(), "更名失敗，伺服器無回應", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    // --- 功能實作區 ---

    // ★ 修改：檢查 SharedPreferences 裡的紀錄並載入頭像
    private boolean checkLocalLogin() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String userEmail = prefs.getString("user_email", "使用者");
        String userName = prefs.getString("username", "匿名");

        if (userId != null) {
            // === 有紀錄，直接切換到已登入畫面 ===
            layoutLogin.setVisibility(View.GONE);
            layoutProfile.setVisibility(View.VISIBLE);

            tvName.setText(userName);
            tvEmail.setText(userEmail);
            tvBio.setText("歡迎回來！");

            // ★ 呼叫 RoboHash 載入頭像
            loadRoboAvatar(userEmail);

            return true;
        }
        return false;
    }

    // Email 登入邏輯
    private void emailLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) return;

        Toast.makeText(getContext(), "登入驗證中...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            TcpClient client = TcpClient.getInstance();
            client.connect();

            String command = "LOGIN:" + email + ":" + password;
            String response = client.sendRequest(command);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (response != null && response.startsWith("LOGIN_SUCCESS")) {
                        // 假設 Server 回傳： LOGIN_SUCCESS:userId:userName
                        String[] parts = response.split(":");

                        if (parts.length >= 2) {
                            String userId = parts[1];
                            String userName = (parts.length >= 3) ? parts[2] : "使用者";

                            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                            prefs.edit()
                                    .putString("user_id", userId)
                                    .putString("user_email", email)
                                    .putString("user_password", password)
                                    .putString("username", userName)
                                    .apply();

                            Toast.makeText(getContext(), "登入成功！", Toast.LENGTH_SHORT).show();

                            layoutLogin.setVisibility(View.GONE);
                            layoutProfile.setVisibility(View.VISIBLE);

                            tvName.setText(userName);
                            tvEmail.setText(email);

                            // ★ 登入成功後，立刻載入頭像
                            loadRoboAvatar(email);
                        }
                    } else {
                        Toast.makeText(getContext(), "登入失敗", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    // Google 登入邏輯
    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            updateUI(account);
        } catch (ApiException e) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
            Toast.makeText(getContext(), "Google 登入失敗", Toast.LENGTH_SHORT).show();
        }
    }

    // 登出邏輯
    private void signOut() {
        requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            updateUI(null);
            etEmail.setText("");
            etPassword.setText("");

            // 清空頭像 (放回預設圖，避免顯示上一個人的緩存)
            ivProfile.setImageResource(R.drawable.ic_launcher_background); // 這裡請換成你自己的預設圖資源

            Toast.makeText(getContext(), "已登出", Toast.LENGTH_SHORT).show();
        });
    }

    // UI 狀態切換 (含 Google 登入頭像處理)
    private void updateUI(GoogleSignInAccount account) {
        if (account != null) {
            layoutLogin.setVisibility(View.GONE);
            layoutProfile.setVisibility(View.VISIBLE);

            tvName.setText(account.getDisplayName());
            tvEmail.setText(account.getEmail());

            // ★ Google 登入也使用 RoboHash (或者你要用 account.getPhotoUrl() 也可以)
            loadRoboAvatar(account.getEmail());

        } else {
            layoutLogin.setVisibility(View.VISIBLE);
            layoutProfile.setVisibility(View.GONE);
        }
    }
}