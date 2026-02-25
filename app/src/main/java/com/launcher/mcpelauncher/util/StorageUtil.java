package com.launcher.mcpelauncher.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;

public class StorageUtil {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_STORAGE_CODE = 101;

    // Kiểm tra và yêu cầu quyền storage
    public static boolean checkAndRequestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - sử dụng MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
                    activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
                    return false;
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
                    return false;
                }
            }
            return true;
        } else {
            // Android 10 trở xuống - sử dụng WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
                return false;
            }
            return true;
        }
    }

    // Lấy thư mục download dựa trên Android version
    public static File getDownloadDirectory(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: sử dụng thư mục riêng trong app
            return new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "");
        } else {
            // Android 10 trở xuống: sử dụng thư mục chung
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "");
        }
    }

    // Tạo thư mục download nếu chưa tồn tại
    public static File createDownloadDirectory(Context context) {
        File downloadDir = getDownloadDirectory(context);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        return downloadDir;
    }

    // Kiểm tra đã có quyền chưa
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
}