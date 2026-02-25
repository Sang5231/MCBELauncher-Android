package com.launcher.mcpelauncher.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
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
import com.launcher.mcpelauncher.adapter.ManageVersionAdapter;
import com.launcher.mcpelauncher.database.DatabaseManager;
import com.launcher.mcpelauncher.model.VersionItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ManageVersionActivity extends AppCompatActivity implements ManageVersionAdapter.OnVersionActionListener {

    private RecyclerView recyclerView;
    private ManageVersionAdapter adapter;
    private DatabaseManager dbManager;
    private List<VersionItem> versionList;
    private List<VersionItem> filteredList;
    private RadioGroup radioFilter;
    private TextView tvCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_version); // CHỈ GỌI 1 LẦN

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo DatabaseManager
        dbManager = DatabaseManager.getInstance(this);
        tvCurrentState = findViewById(R.id.tv_current_state);

        // Khởi tạo danh sách
        versionList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Thiết lập toolbar
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        View btnDeleteAll = findViewById(R.id.btn_delete_all);
        if (btnDeleteAll != null) {
            btnDeleteAll.setOnClickListener(v -> showDeleteAllConfirmation());
        }

        // Thiết lập RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo adapter với danh sách rỗng
        adapter = new ManageVersionAdapter(filteredList, this);
        recyclerView.setAdapter(adapter);

        // Thiết lập RadioGroup filter
        setupFilter();

        // Load dữ liệu
        loadVersions();
    }

    private void setupFilter() {
        radioFilter = findViewById(R.id.radio_group_filter);

        if (radioFilter == null) {
            return;
        }

        radioFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1) return;
            applyFilterFromRadioId(checkedId);
        });

        // Đặt mặc định chọn Release
        radioFilter.post(() -> {
            radioFilter.check(R.id.rb_manage_release);
        });
    }

    private void applyFilterFromRadioId(int radioId) {
        String filterType = "release"; // Mặc định

        if (radioId == R.id.rb_manage_release) {
            filterType = "release";
        } else if (radioId == R.id.rb_manage_beta_preview) {
            filterType = "beta_preview";
        } else if (radioId == R.id.rb_manage_alpha) {
            filterType = "alpha";
        }

        applyFilter(filterType);
    }

    private void loadVersions() {
        try {
            // Lấy tất cả phiên bản từ database
            List<VersionItem> allVersions = dbManager.getAllVersions();
            Log.d("ManageVersion", "Loaded " + (allVersions != null ? allVersions.size() : 0) + " versions from DB");

            if (allVersions != null && !allVersions.isEmpty()) {
                tvCurrentState.setVisibility(View.GONE); // Sử dụng GONE thay vì INVISIBLE
                versionList.clear();
                versionList.addAll(allVersions);

                // Áp dụng filter hiện tại
                int checkedId = radioFilter.getCheckedRadioButtonId();
                if (checkedId != -1) {
                    applyFilterFromRadioId(checkedId);
                } else {
                    applyFilter("release"); // Mặc định
                }
            } else {
                versionList.clear();
                filteredList.clear();
                updateAdapter();
                tvCurrentState.setVisibility(View.VISIBLE);
                tvCurrentState.setText("No version has been downloaded yet!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading versions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            versionList = new ArrayList<>();
            tvCurrentState.setVisibility(View.VISIBLE);
            tvCurrentState.setText("Error loading versions");
        }
    }

    private void applyFilter(String filterType) {
        filteredList.clear();

        if (versionList == null || versionList.isEmpty()) {
            updateAdapter();
            tvCurrentState.setVisibility(View.VISIBLE);
            tvCurrentState.setText("No version has been downloaded yet!");
            return;
        }

        for (VersionItem item : versionList) {
            try {
                boolean shouldAdd = false;

                // Lấy versionType từ item
                String itemVersionType = item.getVersionType();

                // Nếu versionType null, xác định từ edition
                if (itemVersionType == null && item.edition != null) {
                    switch (item.edition.toUpperCase()) {
                        case "RELEASE":
                            itemVersionType = "release";
                            break;
                        case "PREVIEW":
                        case "BETA":
                            itemVersionType = "beta_preview";
                            break;
                        case "ALPHA":
                            itemVersionType = "alpha";
                            break;
                        default:
                            itemVersionType = "release";
                    }
                } else if (itemVersionType == null) {
                    itemVersionType = "release"; // Mặc định
                }

                // So sánh filter
                if ("release".equals(filterType) && "release".equals(itemVersionType)) {
                    shouldAdd = true;
                } else if ("beta_preview".equals(filterType) && "beta_preview".equals(itemVersionType)) {
                    shouldAdd = true;
                } else if ("alpha".equals(filterType) && "alpha".equals(itemVersionType)) {
                    shouldAdd = true;
                }

                if (shouldAdd) {
                    filteredList.add(item);
                }

            } catch (Exception e) {
                // Bỏ qua item có lỗi và tiếp tục
                e.printStackTrace();
            }
        }

        updateAdapter();

        // Cập nhật trạng thái hiển thị
        if (filteredList.isEmpty()) {
            tvCurrentState.setVisibility(View.VISIBLE);
            tvCurrentState.setText("No versions found for this filter");
        } else {
            tvCurrentState.setVisibility(View.GONE);
        }
    }

    private void updateAdapter() {
        if (adapter == null) {
            adapter = new ManageVersionAdapter(filteredList, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(filteredList);
        }
    }

    @Override
    public void onEditClicked(VersionItem version) {
        showEditDialog(version);
    }

    @Override
    public void onDeleteClicked(VersionItem version) {
        showDeleteConfirmation(version);
    }

    private void showEditDialog(VersionItem version) {
        if (version == null || version.version == null) {
            Toast.makeText(this, "Cannot edit empty version", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit");
        builder.setMessage("Current version name: " + version.version);

        final EditText input = new EditText(this);
        input.setText(version.version);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newVersionName = input.getText().toString().trim();
                if (!newVersionName.isEmpty() && !newVersionName.equals(version.version)) {
                    // TODO: Cập nhật tên phiên bản trong database
                    version.version = newVersionName;
                    dbManager.updateVersion(version);
                    Toast.makeText(ManageVersionActivity.this,
                            "Version name updated",
                            Toast.LENGTH_SHORT).show();
                    loadVersions(); // Reload dữ liệu
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void showDeleteConfirmation(VersionItem version) {
        if (version == null || version.version == null) {
            Toast.makeText(this, "Cannot delete empty version", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete " + version.version + "?\n\n" +
                        "This action will:\n" +
                        "1. Delete APK files from storage.\n" +
                        "2. This action cannot be undo.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteVersion(version);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteVersion(VersionItem version) {
        try {
            // 1. Xóa file APK nếu tồn tại
            if (version.filePath != null && !version.filePath.isEmpty()) {
                File apkFile = new File(version.filePath);
                if (apkFile.exists()) {
                    boolean deleted = apkFile.delete();
                    Log.d("ManageVersion", "APK deleted: " + deleted);
                }
            }

            // 2. Xóa phiên bản khỏi database
            dbManager.deleteVersion(version.version);

            // 3. Cập nhật UI
            loadVersions();

            Toast.makeText(this, "Version deleted " + version.version, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showDeleteAllConfirmation() {
        if (filteredList == null || filteredList.isEmpty()) {
            Toast.makeText(this, "No version to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete all")
                .setMessage("Are you sure you want to delete ALL versions?\n\n" +
                        "This action will:\n" +
                        "1. Delete all versions.\n" +
                        "2. This action cannot be undo.")
                .setPositiveButton("Delete all", (dialog, which) -> {
                    deleteAllVersions();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllVersions() {
        try {
            if (filteredList == null || filteredList.isEmpty()) {
                return;
            }

            int deletedCount = 0;
            int errorCount = 0;

            // Xóa tất cả file APK trong filteredList
            for (VersionItem version : filteredList) {
                try {
                    if (version.filePath != null && !version.filePath.isEmpty()) {
                        File apkFile = new File(version.filePath);
                        if (apkFile.exists()) {
                            if (apkFile.delete()) {
                                deletedCount++;
                            } else {
                                errorCount++;
                            }
                        }
                    }

                    // Xóa khỏi database
                    dbManager.deleteVersion(version.version);

                } catch (Exception e) {
                    errorCount++;
                    e.printStackTrace();
                }
            }

            // Cập nhật UI
            loadVersions();

            String message = "Deleted " + deletedCount + " versions";
            if (errorCount > 0) {
                message += " (" + errorCount + " errors)";
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVersions();
    }
}