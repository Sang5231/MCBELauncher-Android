package com.launcher.mcpelauncher.activity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.launcher.mcpelauncher.database.DatabaseHelper;
import com.launcher.mcpelauncher.database.DatabaseManager;
import com.launcher.mcpelauncher.databinding.ActivityNewVersionBinding;
import com.launcher.mcpelauncher.model.VersionItem;
import com.launcher.mcpelauncher.util.APKDownloadManager;

import java.util.ArrayList;
import java.util.List;

public class NewVersionActivity extends AppCompatActivity {

    private ActivityNewVersionBinding binding;
    private ImageView btnCancel;
    private Button btnNoDataNew, btnDataNew;
    private DatabaseManager dbManager;
    private VersionAdapter versionAdapter;
    private List<VersionItem> downloadedVersions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityNewVersionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo DatabaseManager
        dbManager = DatabaseManager.getInstance(this);

        initViews();
        setupClickListeners();
        setupRecyclerView();
        loadDownloadedVersions();
    }

    private void initViews() {
        btnCancel = binding.btnCancel;
        btnNoDataNew = binding.btnNodataNew;
        btnDataNew = binding.btnDataNew;
    }

    private void setupRecyclerView() {
        versionAdapter = new VersionAdapter(downloadedVersions, new VersionAdapter.OnVersionClickListener() {
            @Override
            public void onVersionClick(VersionItem version) {
                // Khi chọn một phiên bản đã tải
                selectVersion(version);
            }
        });

        binding.rvHaveData.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHaveData.setAdapter(versionAdapter);
    }

    private void loadDownloadedVersions() {
        // Lấy danh sách các phiên bản ĐÃ TẢI từ database (dữ liệu thật)
        downloadedVersions.clear();

        // Lấy tất cả versions từ database
        List<VersionItem> allVersions = dbManager.getAllVersions();

        // Chỉ lấy những phiên bản đã được tải (isDownloaded = true)
        // VÀ có file APK thực sự tồn tại trên thiết bị
        for (VersionItem version : allVersions) {
            if (version.isDownloaded) {
                // Kiểm tra xem file APK có thực sự tồn tại không
                if (version.filePath != null && !version.filePath.isEmpty()) {
                    // Kiểm tra file tồn tại trên hệ thống
                    java.io.File apkFile = new java.io.File(version.filePath);
                    if (apkFile.exists()) {
                        downloadedVersions.add(version);
                    } else {
                        // File không tồn tại, cập nhật lại database
                        version.isDownloaded = false;
                        updateVersionInDatabase(version);
                    }
                } else {
                    // Không có file path, cập nhật lại database
                    version.isDownloaded = false;
                    updateVersionInDatabase(version);
                }
            }
        }

        // Nếu không có phiên bản nào, kiểm tra thư mục download
        if (downloadedVersions.isEmpty()) {
            checkDownloadDirectoryForAPKs();
        }

        // Cập nhật RecyclerView
        if (versionAdapter != null) {
            versionAdapter.updateList(downloadedVersions);
        }

        // Kiểm tra và hiển thị layout phù hợp
        checkAndDisplayLayout();
    }

    private void updateVersionInDatabase(VersionItem version) {
        // Cập nhật version trong database
        dbManager.addVersion(version);
    }

    private void checkDownloadDirectoryForAPKs() {
        // Kiểm tra thư mục download có file APK nào không
        java.io.File downloadDir = APKDownloadManager.getDownloadDirectory();

        if (downloadDir.exists() && downloadDir.isDirectory()) {
            java.io.File[] files = downloadDir.listFiles((dir, name) -> name.endsWith(".apk"));

            if (files != null && files.length > 0) {
                // Có file APK trong thư mục, thêm vào database
                for (java.io.File apkFile : files) {
                    String fileName = apkFile.getName();
                    // Trích xuất version từ tên file (minecraft-1-21-130.apk -> 1.21.130)
                    String versionName = extractVersionFromFileName(fileName);

                    if (versionName != null) {
                        // Kiểm tra xem đã có trong database chưa
                        if (!isVersionInDatabase(versionName)) {
                            // Thêm vào database
                            VersionItem newVersion = new VersionItem();
                            newVersion.version = versionName;
                            newVersion.edition = "RELEASE"; // Mặc định
                            newVersion.date = "";
                            newVersion.filePath = apkFile.getAbsolutePath();
                            newVersion.isDownloaded = true;
                            newVersion.downloadDate = apkFile.lastModified();

                            dbManager.addVersion(newVersion);
                            downloadedVersions.add(newVersion);

                            Toast.makeText(this,
                                    "Found APK: " + versionName,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }

    private String extractVersionFromFileName(String fileName) {
        // minecraft-1-21-130.apk -> 1.21.130
        if (fileName.startsWith("minecraft-") && fileName.endsWith(".apk")) {
            String versionPart = fileName.substring(10, fileName.length() - 4); // bỏ "minecraft-" và ".apk"
            return versionPart.replace("-", ".");
        }
        return null;
    }

    private boolean isVersionInDatabase(String versionName) {
        for (VersionItem version : downloadedVersions) {
            if (version.version.equals(versionName)) {
                return true;
            }
        }
        return false;
    }

    private void checkAndDisplayLayout() {
        if (downloadedVersions.isEmpty()) {
            // Không có phiên bản nào đã tải
            binding.llNoData.setVisibility(View.VISIBLE);
            binding.llHaveData.setVisibility(View.GONE);

            // Cập nhật text cho nút
            binding.tvNoData.setText("No downloaded versions yet!");
            binding.btnNodataNew.setText("Download New Version");
        } else {
            // Có phiên bản đã tải
            binding.llNoData.setVisibility(View.GONE);
            binding.llHaveData.setVisibility(View.VISIBLE);

            // Cập nhật text cho nút
            binding.btnDataNew.setText("Download New Version");

            // Hiển thị số lượng phiên bản đã tải
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(downloadedVersions.size() + " versions downloaded");
            }
        }
    }

    private void selectVersion(VersionItem version) {
        // Lưu phiên bản được chọn vào database
        dbManager.setSelectedVersion(version.version);

        // Hiển thị thông báo với thông tin đầy đủ
        StringBuilder message = new StringBuilder();
        message.append("Selected: ").append(version.version);

        if (version.isDownloaded) {
            message.append(" ✓");

            // Hiển thị thông tin file
            if (version.filePath != null && !version.filePath.isEmpty()) {
                java.io.File apkFile = new java.io.File(version.filePath);
                if (apkFile.exists()) {
                    long fileSizeMB = apkFile.length() / (1024 * 1024);
                    message.append("\nFile: ").append(apkFile.getName());
                    message.append("\nSize: ").append(fileSizeMB).append(" MB");
                    message.append("\nPath: ").append(apkFile.getParent());
                }
            }
        } else {
            message.append(" (Not downloaded)");
        }

        // Thêm thông tin edition và date nếu có
        if (version.edition != null && !version.edition.isEmpty()) {
            message.append("\nEdition: ").append(version.edition);
        }
        if (version.date != null && !version.date.isEmpty() && !version.date.equals("Unknown")) {
            message.append("\nDate: ").append(version.date);
        }

//        Toast.makeText(this, message.toString(), Toast.LENGTH_LONG).show();

        // Quay về màn hình chính với animation
        finishWithAnimation();
    }

    private void finishWithAnimation() {
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        binding.getRoot().startAnimation(slideDown);

        slideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                finish();
                overridePendingTransition(0, R.anim.slide_down);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void setupClickListeners() {
        // Nút Cancel với animation trượt xuống
        btnCancel.setOnClickListener(v -> finishWithAnimation());

        // Nút "New version" trong layout không có dữ liệu
        btnNoDataNew.setOnClickListener(v -> {
            // Mở AddVersionActivity để chọn loại version
            Intent intent = new Intent(this, AddVersionActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, 0);
        });

        // Nút "New version" trong layout có dữ liệu
        btnDataNew.setOnClickListener(v -> {
            // Mở AddVersionActivity để chọn loại version
            Intent intent = new Intent(this, AddVersionActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up, 0);
        });
    }

    // Trong NewVersionActivity, thêm phương thức kiểm tra database
    private void checkDatabaseConsistency() {
        // Kiểm tra và sửa các bản ghi trùng lặp
        SQLiteDatabase db = dbManager.getWritableDatabase();
        if (db != null) {
            // Thực hiện kiểm tra
            String checkQuery = "SELECT " + DatabaseHelper.COLUMN_VERSION +
                    ", COUNT(*) FROM " + DatabaseHelper.TABLE_VERSIONS +
                    " GROUP BY " + DatabaseHelper.COLUMN_VERSION +
                    " HAVING COUNT(*) > 1";

            Cursor cursor = db.rawQuery(checkQuery, null);
            if (cursor.getCount() > 0) {
                Toast.makeText(this, "Found duplicate entries in database", Toast.LENGTH_SHORT).show();
                // Sửa lỗi
                fixDuplicateVersions(db);
            }
            cursor.close();
        }
    }

    private void fixDuplicateVersions(SQLiteDatabase db) {
        // Giữ lại bản ghi mới nhất của mỗi phiên bản
        String fixQuery = "DELETE FROM " + DatabaseHelper.TABLE_VERSIONS +
                " WHERE " + DatabaseHelper.COLUMN_VERSION_ID + " NOT IN (" +
                "SELECT MAX(" + DatabaseHelper.COLUMN_VERSION_ID + ") FROM " +
                DatabaseHelper.TABLE_VERSIONS + " GROUP BY " + DatabaseHelper.COLUMN_VERSION + ")";

        db.execSQL(fixQuery);
        Toast.makeText(this, "Database consistency fixed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        finishWithAnimation();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load lại danh sách khi quay lại activity (phòng khi có thay đổi)
        loadDownloadedVersions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}