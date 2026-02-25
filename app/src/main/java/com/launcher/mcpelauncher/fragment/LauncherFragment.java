package com.launcher.mcpelauncher.fragment;

import static android.content.ContentValues.TAG;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.activity.NewVersionActivity;
import com.launcher.mcpelauncher.activity.VersionActivity;
import com.launcher.mcpelauncher.adapter.VersionAdapter;
import com.launcher.mcpelauncher.database.DatabaseManager;
import com.launcher.mcpelauncher.model.VersionItem;
import com.launcher.mcpelauncher.util.APKDownloadManager;
import com.launcher.mcpelauncher.util.AppInstaller;
import com.launcher.mcpelauncher.util.StorageUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LauncherFragment extends Fragment {
    private LinearLayout playButtonContainer;
    private RecyclerView rvVersions;
    private VersionAdapter adapter;
    private ImageView btnNewVersion;
    private Button btnPlay, btnWiki, btnAllowExternal, btnEditOptions,btnOpen_mc;
    private TextView tvCurrentVersion;
    private DatabaseManager dbManager;
    private View rootView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_launcher, container, false);
        dbManager = DatabaseManager.getInstance(requireContext());
        initViews(view);
        setupButtonListeners(view);
        setupEditModeButton(view);
        return view;
    }

    private void initViews(View view) {
        try {
            btnNewVersion = view.findViewById(R.id.btn_new_version);
            btnPlay = view.findViewById(R.id.btn_play);
            btnWiki = view.findViewById(R.id.btn_wiki);
            btnAllowExternal = view.findViewById(R.id.btn_allow_external);
            btnEditOptions = view.findViewById(R.id.btn_edit_options);
            btnOpen_mc= view.findViewById(R.id.open_mc);

            // QUAN TRỌNG: TextView này nằm trong include_play_button.xml
            // Nên cần tìm từ rootView hoặc view đã include
            tvCurrentVersion = view.findViewById(R.id.tv_current_version);

            // Debug log
            if (tvCurrentVersion == null) {
                // Thử tìm trong các view con
                findTextViewRecursively(view);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupEditModeButton(View view) {
        Button btnEditXml = view.findViewById(R.id.btn_edit_xml);
        if (btnEditXml == null) {
            // Nếu chưa có nút trong layout, có thể tạo programmatically
            // hoặc đảm bảo thêm nút vào fragment_launcher.xml
            return;
        }

        // Kiểm tra trạng thái edit mode
        DatabaseManager dbManager = DatabaseManager.getInstance(requireContext());
        boolean isEditMode = dbManager.isEditModeEnabled();

        if (isEditMode) {
            btnEditXml.setVisibility(View.VISIBLE);
            btnEditXml.setOnClickListener(v -> {
                Toast.makeText(getContext(),
                        "Under construction",
                        Toast.LENGTH_SHORT).show();
                // TODO: Thêm chức năng chỉnh sửa XML
            });
        } else {
            btnEditXml.setVisibility(View.GONE);
        }
    }

    private void findTextViewRecursively(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            // Kiểm tra xem có phải là TextView chúng ta cần không
            if (textView.getText().toString().contains("Version")) {
                tvCurrentVersion = textView;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                findTextViewRecursively(viewGroup.getChildAt(i));
            }
        }
    }

    private void initializeViews(View view) {
        rvVersions = view.findViewById(R.id.rv_versions);
    }

    private void setupRecyclerView() {
        // Sử dụng GridLayout với 2 cột
        rvVersions.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Tạo adapter với dữ liệu mẫu
        rvVersions.setAdapter(adapter);
    }

    private void setupButtonListeners(View view) {
        // Wiki Button - Navigate to VersionFragment
        btnWiki.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), VersionActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Allow External Storage Button
        btnAllowExternal.setOnClickListener(v -> {
            if (StorageUtil.checkAndRequestStoragePermission(requireActivity())) {
                File downloadDir = StorageUtil.createDownloadDirectory(requireContext());
                Toast.makeText(requireContext(),
                        "You have granted permission",
                        Toast.LENGTH_LONG).show();
            }
        });

        // Edit Options.txt Button
        btnEditOptions.setOnClickListener(v -> {
            editPlayerName();
        });

        // Play Button
        btnPlay.setOnClickListener(v -> {
            String selectedVersion = dbManager.getSelectedVersion();

            if (selectedVersion == null || selectedVersion.trim().isEmpty()) {
                Toast.makeText(requireContext(), "No version selected", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi hàm xử lý chính
            AppInstaller.handlePlayButtonClick(requireContext(), selectedVersion);
        });



        // Nút "New Version" với animation trượt lên
        btnNewVersion.setOnClickListener(v -> {
            Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
            v.startAnimation(slideUp);

            // Đợi animation hoàn thành trước khi chuyển activity
            slideUp.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    Intent intent = new Intent(getActivity(), NewVersionActivity.class);
                    startActivity(intent);
                    // Thêm animation cho Activity
                    if (getActivity() != null) {
                        getActivity().overridePendingTransition(R.anim.slide_up, 0);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        });

        btnOpen_mc.setOnClickListener(v -> {
            final String MC_PACKAGE = "com.mojang.minecraftpe";  // Package name của Minecraft Bedrock

            PackageManager pm = requireContext().getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(MC_PACKAGE);

            if (intent != null) {
                // Nếu vẫn có intent (may mắn), mở bình thường
                try {
                    startActivity(intent);
                    Toast.makeText(requireContext(), "Launching Minecraft Bedrock Edition", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Failed to launch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                // Nếu null (app sideloaded), tìm và mở activity chính thủ công
                try {
                    // Tìm tất cả activity có thể launch của package
                    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    mainIntent.setPackage(MC_PACKAGE);

                    List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
                    if (!resolveInfos.isEmpty()) {
                        // Lấy activity đầu tiên (thường là MainActivity)
                        ResolveInfo resolveInfo = resolveInfos.get(0);
                        String className = resolveInfo.activityInfo.name;

                        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
                        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                        launchIntent.setComponent(new ComponentName(MC_PACKAGE, className));
                        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        startActivity(launchIntent);
                        Toast.makeText(requireContext(), "Launching Minecraft Bedrock Edition", Toast.LENGTH_SHORT).show();
                    } else {
                        // Không tìm thấy activity nào → chưa cài hoặc lỗi package
                        Toast.makeText(requireContext(), "Minecraft Bedrock Edition not found or not launchable", Toast.LENGTH_LONG).show();

                        // Mở Play Store để tải chính thức (tùy chọn)
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + MC_PACKAGE)));
                        } catch (Exception e) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + MC_PACKAGE)));
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private List<VersionItem> getVersions() {
        List<VersionItem> list = new ArrayList<>();
        return list;
    }

    private void editPlayerName() {
        String basePath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            basePath = Environment.getExternalStorageDirectory() +
                    "/Android/data/com.mojang.minecraftpe/files/games/com.mojang/minecraftpe/";
        } else {
            basePath = Environment.getExternalStorageDirectory() +
                    "/games/com.mojang/minecraftpe/";
        }

        File optionsFile = new File(basePath + "options.txt");

        if (optionsFile.exists()) {
            Toast.makeText(requireContext(),
                    "Options file found at: " + optionsFile.getPath(),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(),
                    "Options.txt not found! Make sure Minecraft PE is installed.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static void launchMinecraft(Context context, String packageName) {
        try {
            Log.d(TAG, "Attempting to launch Minecraft with package: " + packageName);

            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);

            if (launchIntent != null) {
                Log.d(TAG, "Launch intent found, starting activity...");
                context.startActivity(launchIntent);

                Toast.makeText(context,
                        context.getString(R.string.toast_launching_minecraft),
                        Toast.LENGTH_SHORT).show();

            } else {
                Log.e(TAG, "No launch intent found for package: " + packageName);
                Toast.makeText(context,
                        context.getString(R.string.cannot_launch_minecraft),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching Minecraft: " + e.getMessage(), e);
            Toast.makeText(context,
                    context.getString(R.string.error_launching) + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSelectedVersion() {
        String selectedVersion = dbManager.getSelectedVersion();
        if (selectedVersion != null && !selectedVersion.isEmpty()) {
            // Kiểm tra xem phiên bản đã được tải chưa
            boolean isDownloaded = APKDownloadManager.isVersionDownloaded(selectedVersion);

            if (isDownloaded) {
                // Kiểm tra xem APK có sẵn sàng không
                boolean isAPKAvailable = AppInstaller.isAPKAvailable(selectedVersion);
                String apkSize = AppInstaller.getAPKSize(selectedVersion);

                if (isAPKAvailable) {
                    tvCurrentVersion.setText("Version: " + selectedVersion + " ✓ (" + apkSize + ")");
                    updatePlayButton(true, selectedVersion);
                } else {
                    tvCurrentVersion.setText("Version: " + selectedVersion + " (File corrupted)");
                    updatePlayButton(false, selectedVersion);
                }
            } else {
                tvCurrentVersion.setText("Version: " + selectedVersion + " (Not downloaded)");
                updatePlayButton(false, selectedVersion);
            }
        } else {
            tvCurrentVersion.setText("Version: Not selected");
            updatePlayButton(false, null);
        }
    }

    private void updatePlayButton(boolean isReady, String version) {
        if (btnPlay != null) {
            if (isReady && version != null) {
                // APK đã sẵn sàng
                btnPlay.setText(getString(R.string.play_button_launch));
                btnPlay.setEnabled(true);
                btnPlay.setBackgroundResource(R.drawable.rounded_green_bg);
            } else if (version != null) {
                // Có version nhưng chưa download
                btnPlay.setText(getString(R.string.play_button_install));
                btnPlay.setEnabled(true);
                btnPlay.setBackgroundResource(R.drawable.rounded_yellow_bg);
            } else {
                // Không có version nào được chọn
                btnPlay.setText("PLAY");
                btnPlay.setEnabled(false);
                btnPlay.setBackgroundResource(R.drawable.rounded_gray_bg);
            }
        }
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            requireContext().getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String getInstalledVersionName(String packageName) {
        try {
            PackageInfo info = requireContext().getPackageManager().getPackageInfo(packageName, 0);
            return info.versionName; // Ví dụ: "1.21.131.1" hoặc "1.21.131"
        } catch (Exception e) {
            return "";
        }
    }

    private void launchMinecraftManually(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);

        if (intent != null) {
            context.startActivity(intent);
            Toast.makeText(context, "Đang khởi chạy Minecraft...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cách thủ công cho bản sideloaded
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setPackage(packageName);

            List<ResolveInfo> activities = pm.queryIntentActivities(mainIntent, 0);
            if (!activities.isEmpty()) {
                ResolveInfo info = activities.get(0);
                String activityName = info.activityInfo.name;

                Intent launchIntent = new Intent(Intent.ACTION_MAIN);
                launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                launchIntent.setComponent(new ComponentName(packageName, activityName));
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(launchIntent);
                Toast.makeText(context, "Đang khởi chạy Minecraft...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Không thể khởi chạy Minecraft", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi khởi chạy: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPlayStore(String packageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private void uninstallAndInstallThenLaunch(String version) {
        // Gửi intent gỡ cài đặt
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
        uninstallIntent.setData(Uri.parse("package:com.mojang.minecraftpe"));
        startActivityForResult(uninstallIntent, 1001); // Code request bất kỳ

        // Sau khi gỡ xong, người dùng sẽ quay lại → onActivityResult hoặc onResume sẽ chạy
        // Ta lưu tạm version cần cài vào một biến tạm hoặc SharedPreferences
        // Cách đơn giản: dùng Toast hướng dẫn + tự động cài khi quay lại

        // Hoặc tốt hơn: lưu vào Database một flag tạm
        dbManager.setTempVersionToInstallAfterUninstall(version);

        Toast.makeText(requireContext(),
                "Vui lòng xác nhận gỡ cài đặt → sau đó launcher sẽ tự động cài phiên bản mới",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSelectedVersion(); // hàm load version hiện tại của bạn

        String pendingVersion = dbManager.getTempVersionToInstallAfterUninstall();
        if (pendingVersion != null && !pendingVersion.isEmpty()) {
            dbManager.clearTempVersionToInstallAfterUninstall();

            if (APKDownloadManager.isVersionDownloaded(pendingVersion)) {
                Toast.makeText(requireContext(),
                        "Installing " + pendingVersion + " after uninstall...",
                        Toast.LENGTH_SHORT).show();
                AppInstaller.installAPK(requireContext(), pendingVersion);
            } else {
                Toast.makeText(requireContext(),
                        "APK for " + pendingVersion + " not found",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}