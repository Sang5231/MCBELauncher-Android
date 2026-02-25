package com.launcher.mcpelauncher.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.databinding.ActivityAddVersionBinding;

public class AddVersionActivity extends AppCompatActivity {

    private ActivityAddVersionBinding binding;
    private Button btnRelease, btnBetaPreview, btnAlpha, btnCancel, btnOk;
    private TextView tvSelected;

    private String selectedVersionType = "";
    private String selectedTypeDisplay = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddVersionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnRelease = binding.btnRelease;
        btnBetaPreview = binding.btnBetaPreview;
        btnAlpha = binding.btnAlpha;
        btnCancel = binding.btnCancel;
        btnOk = binding.btnOk;
        tvSelected = binding.tvSelected;

        // Ẩn nút OK vì không cần dùng nữa
//        btnOk.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        // Nút Release - mở thẳng danh sách Release
        btnRelease.setOnClickListener(v -> {
            openVersionList("release", "Release");
        });

        // Nút Beta/Preview - mở thẳng danh sách Beta/Preview
        btnBetaPreview.setOnClickListener(v -> {
            openVersionList("beta_preview", "Beta/Preview");
        });

        // Nút Alpha - mở thẳng danh sách Alpha
        btnAlpha.setOnClickListener(v -> {
            openVersionList("alpha", "Alpha");
        });

        // Nút Cancel
        btnCancel.setOnClickListener(v -> {
            finish();
        });

        // Nút OK (đã ẩn)
        btnOk.setOnClickListener(v -> {
            Toast.makeText(this, "Under construction", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSelectedText(String text) {
        if (tvSelected != null) {
            tvSelected.setText(text);
        }
    }

    private void openVersionList(String versionType, String displayName) {
        Intent intent = new Intent(this, DownloadVersionListActivity.class);
        intent.putExtra("VERSION_TYPE", versionType);
        intent.putExtra("DISPLAY_NAME", displayName);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}