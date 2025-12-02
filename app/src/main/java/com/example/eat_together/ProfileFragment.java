package com.example.eat_together;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

    private GoogleSignInClient mGoogleSignInClient;
    private SignInButton signInButton;
    private Button signOutButton;
    private TextView tvName;
    private ImageView ivProfile;

    // 定義登入結果的處理器
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1) { // -1 代表 RESULT_OK
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        signInButton = view.findViewById(R.id.sign_in_button);
        signOutButton = view.findViewById(R.id.btn_sign_out);
        tvName = view.findViewById(R.id.tv_name);
        ivProfile = view.findViewById(R.id.iv_profile_pic);

        // 1. 設定 Google 登入選項
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail() // 要求取得 Email
                .build();

        // 2. 建立 GoogleSignInClient
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // 3. 設定按鈕點擊事件
        signInButton.setOnClickListener(v -> signIn());
        signOutButton.setOnClickListener(v -> signOut());

        // 設定按鈕樣式
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 檢查使用者是否已經登入過
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        updateUI(account);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(requireActivity(), task -> updateUI(null));
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // 登入成功，更新 UI
            updateUI(account);
        } catch (ApiException e) {
            // 登入失敗
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
            Toast.makeText(getContext(), "登入失敗", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        if (account != null) {
            // 已登入狀態
            tvName.setText("歡迎, " + account.getDisplayName());
            signInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);

            // 這裡可以獲取更多資訊，例如 account.getEmail(), account.getId()
            // 未來可以把這些資訊傳給您的 TCP Server
        } else {
            // 未登入狀態
            tvName.setText("尚未登入");
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
        }
    }
}