package com.launcher.mcpelauncher.activity;

import static android.webkit.WebSettings.LOAD_DEFAULT;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.launcher.mcpelauncher.MainActivity;
import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.database.DatabaseManager;
import com.launcher.mcpelauncher.databinding.ActivityDownloadVersionBinding;
import com.launcher.mcpelauncher.util.APKDownloadManager;
import com.launcher.mcpelauncher.util.DownloadUrlManager;
import com.launcher.mcpelauncher.util.DownloadWebViewClient;

public class DownloadVersionActivity extends AppCompatActivity {

    private ActivityDownloadVersionBinding binding;
    private DatabaseManager dbManager;
    private ProgressBar progressBar;

    private String versionName = "";
    private String versionEdition = "";
    private String versionDate = "";

    private APKDownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityDownloadVersionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null) {
            versionName = intent.getStringExtra("VERSION_NAME");
            versionEdition = intent.getStringExtra("VERSION_EDITION");
            versionDate = intent.getStringExtra("VERSION_DATE");
        }

        if (versionName == null || versionName.isEmpty()) {
            Toast.makeText(this, "No version specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbManager = DatabaseManager.getInstance(this);
        downloadManager = new APKDownloadManager(this);

        initsListener();
        setupToolbar();
        setupWebView();
        checkIfAlreadyDownloaded();
    }

    private void initsListener() {
        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DownloadVersionActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Download: " + versionName);
            if (versionEdition != null) {
                getSupportActionBar().setSubtitle(versionEdition);
            }
        }

        // Hiển thị thông tin version
        TextView tvVersionInfo = findViewById(R.id.tv_version_info);
        if (tvVersionInfo != null) {
            String info = "Version: " + versionName;
            if (versionEdition != null && !versionEdition.isEmpty()) {
                info += " | " + versionEdition;
            }
            if (versionDate != null && !versionDate.isEmpty()) {
                info += " | " + versionDate;
            }
            tvVersionInfo.setText(info);
        }

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Lấy ProgressBar từ layout
        progressBar = binding.progressBar;
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE); // Ẩn ban đầu
            progressBar.setProgress(0);
            progressBar.setMax(100);
        }
    }

    private void setupWebView() {
        // Lấy URL download dựa trên source setting
        String downloadUrl = DownloadUrlManager.getDownloadUrl(this, versionName);
        String sourceName = DownloadUrlManager.getSourceDisplayName(this);

        Toast.makeText(this,
                "Loading " + sourceName + " for " + versionName,
                Toast.LENGTH_SHORT).show();

        // Sử dụng DownloadWebViewClient tùy chỉnh
        binding.webView.setWebViewClient(new DownloadWebViewClient(this, versionName));

        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    if (newProgress == 100) {
                        // Ẩn progress bar khi load xong
                        progressBar.setVisibility(View.GONE);
                    } else if (progressBar.getVisibility() == View.GONE) {
                        // Hiện progress bar khi bắt đầu load
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // Cập nhật tiêu đề toolbar với tiêu đề trang web
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }
            }
        });

        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setLoadWithOverviewMode(true);
        binding.webView.getSettings().setUseWideViewPort(true);
        binding.webView.getSettings().setBuiltInZoomControls(true);
        binding.webView.getSettings().setDisplayZoomControls(false);
        binding.webView.getSettings().setSupportZoom(true);

        // Thêm JavaScript interface để phát hiện download
        binding.webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // === BẬT CACHE ĐỂ GIỮ TRẠNG THÁI TRANG ===
        binding.webView.getSettings().setCacheMode(LOAD_DEFAULT);

        // Giữ trạng thái trang khi quay lại (rất quan trọng!)
        binding.webView.getSettings().setSaveFormData(true);
        binding.webView.getSettings().setDatabaseEnabled(true);
        binding.webView.getSettings().setDomStorageEnabled(true);

        // Tải URL
        binding.webView.loadUrl(downloadUrl);
    }

    private void checkIfAlreadyDownloaded() {
        if (APKDownloadManager.isVersionDownloaded(versionName)) {
            Toast.makeText(this,
                    "Version " + versionName + " is already downloaded",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // JavaScript Interface để giao tiếp giữa WebView và Android
    private class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void detectAPKDownload(String url) {
            // Được gọi từ JavaScript khi phát hiện link download
            runOnUiThread(() -> {
                Toast.makeText(DownloadVersionActivity.this,
                        "APK download link detected: " + url,
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_download, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            binding.webView.reload();
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }
            return true;
        } else if (id == R.id.action_download_manual) {
            // Tùy chọn download thủ công
            String downloadUrl = DownloadUrlManager.getDownloadUrl(this, versionName);
            downloadManager.downloadAPK(downloadUrl, versionName);
            return true;
        } else if (id == R.id.action_check_download) {
            // Kiểm tra xem đã download chưa
            if (APKDownloadManager.isVersionDownloaded(versionName)) {
                Toast.makeText(this,
                        versionName + " is already downloaded",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        versionName + " is not downloaded yet",
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause WebView để tiết kiệm tài nguyên
        binding.webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume WebView
        binding.webView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dọn dẹp WebView
        binding.webView.destroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}