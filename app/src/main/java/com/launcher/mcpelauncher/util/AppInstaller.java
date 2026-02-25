package com.launcher.mcpelauncher.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.database.DatabaseManager;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class AppInstaller {

    private static final String TAG = "AppInstaller";
    // Danh sách các tên gói có thể của Minecraft
    private static final List<String> MINECRAFT_PACKAGE_NAMES = Arrays.asList(
            "com.mojang.minecraftpe",    // Phiên bản chính thức
            "com.mojang.minecraft",      // Phiên bản cũ
            "com.microsoft.minecraftpe", // Phiên bản Microsoft
            "com.mojang.minecraftpocket" // Phiên bản pocket cũ
    );

    public static void launchOrInstall(Context context, String versionName) {
        Log.d(TAG, "launchOrInstall called for version: " + versionName);

        // 1. Kiểm tra xem APK có tồn tại không
        if (!isAPKAvailable(versionName)) {
            Toast.makeText(context,
                    context.getString(R.string.apk_not_found) + versionName,
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "APK not available for version: " + versionName);
            return;
        }

        // 2. Tìm tên gói Minecraft đã cài đặt (nếu có)
        String installedPackage = getInstalledMinecraftPackage(context);
        String installedVersion = null;

        if (installedPackage != null) {
            installedVersion = getInstalledVersion(context, installedPackage);
        }

        Log.d(TAG, "Installed package: " + installedPackage + ", version: " + installedVersion);

        if (installedPackage == null) {
            // Trường hợp 1: Chưa cài Minecraft -> Cài đặt APK
            Log.d(TAG, "Minecraft not installed, installing...");
            installAPK(context, versionName);
        } else {
            // Trường hợp 2: Đã cài Minecraft -> Kiểm tra phiên bản
            if (installedVersion != null && installedVersion.equals(versionName)) {
                // Trường hợp 2a: Phiên bản trùng -> Khởi chạy
                Log.d(TAG, "Versions match, launching...");
                launchMinecraft(context, installedPackage);
            } else {
                // Trường hợp 2b: Phiên bản khác -> Hiển thị thông báo gỡ cài đặt
                Log.d(TAG, "Versions don't match, showing uninstall notice...");
                showVersionMismatchNotice(context, installedPackage, versionName, installedVersion);
            }
        }
    }

    /**
     * Hiển thị thông báo phiên bản không khớp
     */
    private static void showVersionMismatchNotice(Context context, String packageName, String targetVersion, String currentVersion) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(context);

        String message;
        if (currentVersion != null) {
            message = String.format(
                    "Đã phát hiện Minecraft phiên bản %s.\n\n" +
                            "Bạn muốn cài đặt phiên bản %s?\n\n" +
                            "⚠️ Vui lòng gỡ cài đặt phiên bản hiện tại thủ công từ Cài đặt → Ứng dụng trước khi cài đặt phiên bản mới.",
                    currentVersion, targetVersion);
        } else {
            message = "Bạn muốn cài đặt phiên bản " + targetVersion + "?\n\n" +
                    "⚠️ Vui lòng kiểm tra và gỡ phiên bản Minecraft cũ nếu có.";
        }

        builder.setTitle("Phiên bản khác nhau")
                .setMessage(message)
                .setPositiveButton("Cài đặt", (dialog, which) -> {
                    // Cài đặt APK (hệ thống sẽ hỏi xác nhận gỡ cài đặt)
                    installAPK(context, targetVersion);
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNeutralButton("Mở phiên bản hiện tại", (dialog, which) -> {
                    launchMinecraft(context, packageName);
                })
                .show();
    }

    /**
     * Tìm tên gói Minecraft đã cài đặt
     */
    private static String getInstalledMinecraftPackage(Context context) {
        PackageManager pm = context.getPackageManager();

        for (String packageName : MINECRAFT_PACKAGE_NAMES) {
            try {
                pm.getPackageInfo(packageName, 0);
                Log.d(TAG, "Found Minecraft with package: " + packageName);
                return packageName;
            } catch (PackageManager.NameNotFoundException e) {
                // Tiếp tục kiểm tra tên gói tiếp theo
                continue;
            } catch (Exception e) {
                Log.e(TAG, "Error checking package " + packageName + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "No Minecraft installation found");
        return null;
    }

    private static String getInstalledVersion(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            String version = packageInfo.versionName;
            Log.d(TAG, "Installed version of " + packageName + ": " + version);
            return version;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed version: " + e.getMessage(), e);
            return null;
        }
    }

    public static void installAPK(Context context, String versionName) {
        String filePath = APKDownloadManager.getDownloadPathForVersion(versionName);
        File apkFile = new File(filePath);

        if (!apkFile.exists() || apkFile.length() < 10 * 1024 * 1024) {
            Toast.makeText(context, "Invalid or missing APK file", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", apkFile);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open installer. Please enable \"Install unknown apps\"", Toast.LENGTH_LONG).show();
        }
    }

    private static void installAPKFile(Context context, File apkFile) {
        try {
            Log.d(TAG, "Preparing to install APK: " + apkFile.getAbsolutePath());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ (API 24+) cần FileProvider
                Log.d(TAG, "Using FileProvider for Android " + Build.VERSION.SDK_INT);

                String authority = context.getPackageName() + ".fileprovider";
                Log.d(TAG, "Authority: " + authority);

                try {
                    apkUri = FileProvider.getUriForFile(context, authority, apkFile);
                    Log.d(TAG, "FileProvider URI created: " + apkUri.toString());

                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "FileProvider failed: " + e.getMessage(), e);
                    Toast.makeText(context, "FileProvider error",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

            } else {
                // Android cũ: sử dụng Uri.fromFile
                Log.d(TAG, "Using Uri.fromFile for Android " + Build.VERSION.SDK_INT);
                apkUri = Uri.fromFile(apkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            // Kiểm tra xem có app nào có thể xử lý intent không
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                Log.d(TAG, "Starting install activity...");
                context.startActivity(intent);

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context,
                            "Opening the installer...",
                            Toast.LENGTH_SHORT).show();
                });

            } else {
                Log.e(TAG, "No activity found to handle install intent");
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context,
                            "Installer not found",
                            Toast.LENGTH_SHORT).show();
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error installing APK: " + e.getMessage(), e);

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context,
                        "Installation error",
                        Toast.LENGTH_LONG).show();
            });
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

                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context,
                            "Opening Minecraft...",
                            Toast.LENGTH_SHORT).show();
                });

            } else {
                Log.e(TAG, "No launch intent found for package: " + packageName);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context,
                            "Can't not open Minecraft",
                            Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching Minecraft: " + e.getMessage(), e);
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context,
                        "Lỗi",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    // Kiểm tra xem APK có tồn tại không
    public static boolean isAPKAvailable(String versionName) {
        String filePath = APKDownloadManager.getDownloadPathForVersion(versionName);
        File apkFile = new File(filePath);

        boolean exists = apkFile.exists();
        long fileSize = exists ? apkFile.length() : 0;

        Log.d(TAG, "APK available check - Version: " + versionName +
                ", Exists: " + exists + ", Size: " + fileSize);

        if (exists) {
            // Kiểm tra kích thước file (ít nhất 10MB cho APK Minecraft)
            return fileSize > 10 * 1024 * 1024;
        }

        return false;
    }

    // Lấy kích thước file APK
    public static String getAPKSize(String versionName) {
        String filePath = APKDownloadManager.getDownloadPathForVersion(versionName);
        File apkFile = new File(filePath);

        if (apkFile.exists()) {
            long sizeMB = apkFile.length() / (1024 * 1024);
            return sizeMB + " MB";
        }

        return "Unknown";
    }

    // Kiểm tra Minecraft đã cài đặt chưa
    public static boolean isMinecraftInstalled(Context context) {
        return getInstalledMinecraftPackage(context) != null;
    }

    // Lấy phiên bản Minecraft đã cài đặt
    public static String getInstalledMinecraftVersion(Context context) {
        String packageName = getInstalledMinecraftPackage(context);
        if (packageName != null) {
            return getInstalledVersion(context, packageName);
        }
        return null;
    }

    public static void handlePlayButtonClick(Context context, String versionName) {
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        String mode = dbManager.getInstallMode();

        // Kiểm tra APK đã tải chưa
        if (!APKDownloadManager.isVersionDownloaded(versionName)) {
            Toast.makeText(context, "APK file not found for " + versionName, Toast.LENGTH_LONG).show();
            return;
        }

        String installedPackage = getInstalledMinecraftPackage(context);

        switch (mode) {
            case "uninstall":
                if (installedPackage != null) {
                    // Gỡ trước rồi cài sau
                    uninstallThenInstall(context, versionName);
                } else {
                    // Chưa có app → cài trực tiếp
                    installAPK(context, versionName);
                }
                break;

            case "override":
                // Cài đè trực tiếp (hệ thống sẽ hỏi nếu cần overwrite)
                installAPK(context, versionName);
                break;

            case "manually":
                if (installedPackage != null) {
                    // Hướng dẫn gỡ thủ công
                    Toast.makeText(context,
                            "Please uninstall the existing Minecraft app manually first.\n\n" +
                                    "How to uninstall:\n" +
                                    "1. Long press the Minecraft app icon on home screen or app drawer\n" +
                                    "2. Tap 'Uninstall' or go to Settings → Apps → Minecraft → Uninstall",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Chưa có app → cho phép cài
                    installAPK(context, versionName);
                }
                break;

            default:
                // Fallback về uninstall
                if (installedPackage != null) {
                    uninstallThenInstall(context, versionName);
                } else {
                    installAPK(context, versionName);
                }
                break;
        }
    }

    private static void uninstallThenInstall(Context context, String versionName) {
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        dbManager.setTempVersionToInstallAfterUninstall(versionName);

        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
        uninstallIntent.setData(Uri.parse("package:com.mojang.minecraftpe"));

        // Thêm các package khác nếu cần (tùy phiên bản Minecraft)
        // uninstallIntent.setData(Uri.parse("package:com.microsoft.minecraftpe")); // nếu dùng package Microsoft

        try {
            context.startActivity(uninstallIntent);
            Toast.makeText(context,
                    "Please confirm the uninstall. The launcher will install the new version automatically afterward.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context,
                    "Cannot open uninstall screen. Please uninstall Minecraft manually.",
                    Toast.LENGTH_SHORT).show();
            // Fallback: vẫn cho cài nếu người dùng muốn
            installAPK(context, versionName);
        }
    }
}