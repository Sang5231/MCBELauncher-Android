package com.launcher.mcpelauncher.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.adapter.VersionAdapter;
import com.launcher.mcpelauncher.database.DatabaseManager;
import com.launcher.mcpelauncher.databinding.ActivityDownloadVersionListBinding;
import com.launcher.mcpelauncher.model.VersionItem;
import com.launcher.mcpelauncher.util.APKDownloadManager;
import com.launcher.mcpelauncher.util.XmlParser;

import java.util.ArrayList;
import java.util.List;

public class DownloadVersionListActivity extends AppCompatActivity {

    private ActivityDownloadVersionListBinding binding;
    private RecyclerView rvVersions;
    private TextView tvTitle;

    private List<VersionItem> allVersions = new ArrayList<>();
    private List<VersionItem> filteredVersions = new ArrayList<>();
    private VersionAdapter versionAdapter;
    private DatabaseManager dbManager;

    private String versionType = "";
    private String displayName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDownloadVersionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null) {
            versionType = intent.getStringExtra("VERSION_TYPE");
            displayName = intent.getStringExtra("DISPLAY_NAME");
        }

        if (versionType == null || versionType.isEmpty()) {
            Toast.makeText(this, "No version type specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbManager = DatabaseManager.getInstance(this);
        setupToolbar(); // Thiết lập Toolbar
        initViews();
        setupRecyclerView();
        loadVersionsFromXml();

        // Cập nhật tiêu đề
        updateTitle();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select Version");
            if (displayName != null) {
                getSupportActionBar().setSubtitle(displayName);
            }
        }

        // Xử lý nút back trên Toolbar
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        rvVersions = binding.rvVersions;
        tvTitle = binding.tvTitle;
    }

    private void updateTitle() {
        if (tvTitle != null && displayName != null) {
            tvTitle.setText(displayName + " Versions");
        }
    }

    private void setupRecyclerView() {
        versionAdapter = new VersionAdapter(filteredVersions, new VersionAdapter.OnVersionClickListener() {
            @Override
            public void onVersionClick(VersionItem version) {
                // Khi chọn một phiên bản để tải - mở DownloadVersionActivity
                openDownloadActivity(version);
            }
        });

        rvVersions.setLayoutManager(new LinearLayoutManager(this));
        rvVersions.setAdapter(versionAdapter);
    }

    private void loadVersionsFromXml() {
        // Load tất cả versions từ XML
        allVersions = XmlParser.parseVersionsFromXml(this, R.xml.version_list);

        if (allVersions.isEmpty()) {
            Toast.makeText(this, "No versions found in XML", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lọc theo loại version
        filterVersionsByType(versionType);
    }

    private void filterVersionsByType(String versionType) {
        filteredVersions.clear();

        for (VersionItem version : allVersions) {
            if (version.getVersionType().equals(versionType)) {
                filteredVersions.add(version);
            }
        }

        if (versionAdapter != null) {
            versionAdapter.updateList(filteredVersions);
        }

        if (filteredVersions.isEmpty()) {
            Toast.makeText(this, "No " + versionType + " versions available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDownloadActivity(VersionItem version) {
        // Thêm vào database trước để hiển thị trong danh sách "Downloaded"
        version.isDownloaded = false; // Chưa tải xong
        version.filePath = APKDownloadManager.getDownloadPathForVersion(version.version);
        version.downloadDate = 0;

        dbManager.addVersion(version); // ← Sẽ INSERT mới hoặc UPDATE nếu có

        // Mở activity tải
        Intent intent = new Intent(this, DownloadVersionActivity.class);
        intent.putExtra("VERSION_NAME", version.version);
        intent.putExtra("VERSION_EDITION", version.edition);
        intent.putExtra("VERSION_DATE", version.date);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, R.anim.slide_down);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}