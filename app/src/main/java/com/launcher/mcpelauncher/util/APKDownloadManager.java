package com.launcher.mcpelauncher.util;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.launcher.mcpelauncher.database.DatabaseManager;
import com.launcher.mcpelauncher.model.VersionItem;

import java.io.File;

public class APKDownloadManager {

    private static final String TAG = "APKDownloadManager";

    // Thư mục lưu APK
    private static final String APP_FOLDER = "MCBELauncher";
    private static final String LAUNCHER_FOLDER = "launcher";
    private static final String VERSION_FOLDER = "version";

    private Context context;
    private DownloadManager downloadManager;
    private long downloadId;
    private String versionName;
    private DatabaseManager dbManager;

    public APKDownloadManager(Context context) {
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.dbManager = DatabaseManager.getInstance(context);
    }

    public void downloadAPK(String downloadUrl, String versionName) {
        this.versionName = versionName;

        // Tạo tên file
        String fileName = "minecraft-" + versionName.replace(".", "-") + ".apk";

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDir = new File(downloadDir, APP_FOLDER);

        if (!appDir.exists()) {
            appDir.mkdirs();
        }

        File apkFile = new File(appDir, fileName);
        Uri destinationUri = Uri.fromFile(apkFile);

        Log.d(TAG, "Download destination: " + apkFile.getAbsolutePath());

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("Downloading Minecraft " + versionName);
        request.setDescription("Downloading APK file...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(destinationUri);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        // Bắt đầu download
        downloadId = downloadManager.enqueue(request);

        // Đăng ký BroadcastReceiver để xử lý khi hoàn tất
        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (completedId != downloadId) return;

                // Kiểm tra trạng thái download
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);

                if (cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(statusIndex);

                    int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(reasonIndex);

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        // Download thành công
                        if (apkFile.exists() && apkFile.length() > 10 * 1024 * 1024) { // >10MB
                            // Cập nhật database
                            VersionItem version = new VersionItem();
                            version.version = versionName;
                            version.filePath = apkFile.getAbsolutePath();
                            version.isDownloaded = true;
                            version.downloadDate = System.currentTimeMillis();

                            // Lấy thông tin chi tiết từ bản ghi hiện có (nếu có)
                            VersionItem existing = dbManager.getVersionByName(versionName);
                            if (existing != null) {
                                version.edition = existing.edition;
                                version.date = existing.date;
                                version.imageUrl = existing.imageUrl;
                                version.state = existing.state;
                            }

                            dbManager.addVersion(version);

                            Toast.makeText(context, "Done: " + versionName, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, "File download failed or incomplete", Toast.LENGTH_LONG).show();
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        Toast.makeText(context, "Failure: " + getErrorReason(reason), Toast.LENGTH_LONG).show();
                    }
                }
                cursor.close();

                // Hủy đăng ký receiver để tránh leak
                try {
                    context.unregisterReceiver(this);
                } catch (Exception e) {
                    // Đã hủy rồi
                }
            }
        };

        // Đăng ký receiver
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(context, downloadReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
    }

    private void registerDownloadReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                    if (id == downloadId) {
                        checkDownloadStatus(id);
                    }
                } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
                    // Mở Downloads app khi click notification
                    Intent viewDownloads = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                    viewDownloads.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(viewDownloads);
                }
            }
        };

        // Đăng ký receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void checkDownloadStatus(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                @SuppressLint("Range") int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        // Download thành công
                        @SuppressLint("Range") String localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        @SuppressLint("Range") long fileSize = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                        Log.d(TAG, "Download successful: " + localUri);
                        handleDownloadSuccess(localUri, fileSize);
                        break;

                    case DownloadManager.STATUS_FAILED:
                        // Download thất bại
                        Log.e(TAG, "Download failed. Reason: " + reason);
                        Toast.makeText(context, "Download failed: " + getErrorReason(reason),
                                Toast.LENGTH_LONG).show();
                        break;

                    case DownloadManager.STATUS_PAUSED:
                        Log.w(TAG, "Download paused. Reason: " + reason);
                        break;

                    case DownloadManager.STATUS_PENDING:
                        Log.d(TAG, "Download pending");
                        break;

                    case DownloadManager.STATUS_RUNNING:
                        Log.d(TAG, "Download running");
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking download status: " + e.getMessage(), e);
        }
    }

    private void handleDownloadSuccess(String localUri, long fileSize) {
        if (localUri == null) {
            Toast.makeText(context, "Download failed: No file path", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Kiểm tra xem phiên bản đã tồn tại trong database chưa
            boolean exists = dbManager.isVersionInDatabase(versionName);
            Log.d(TAG, "Version " + versionName + " exists in DB: " + exists);

            String filePath = getFilePathFromUri(localUri);

            if (filePath == null) {
                Uri uri = Uri.parse(localUri);
                filePath = uri.getPath();
            }

            Log.d(TAG, "Downloaded file URI: " + localUri);
            Log.d(TAG, "Converted file path: " + filePath);

            if (filePath != null) {
                File downloadedFile = new File(filePath);

                if (downloadedFile.exists()) {
                    // Lưu vào database
                    dbManager.markVersionAsDownloaded(versionName, downloadedFile.getAbsolutePath());

                    // Log chi tiết
                    Log.d(TAG, "APK saved to database: " + versionName);
                    Log.d(TAG, "File size: " + (downloadedFile.length() / 1024 / 1024) + " MB");
                    Log.d(TAG, "File path: " + downloadedFile.getAbsolutePath());

                    Toast.makeText(context,
                            "✓ Download completed: " + versionName +
                                    "\nSize: " + (downloadedFile.length() / 1024 / 1024) + " MB",
                            Toast.LENGTH_LONG).show();

                } else {
                    findAndSaveAPKFromDownloads();
                }
            } else {
                findAndSaveAPKFromDownloads();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling download success: " + e.getMessage(), e);
            Toast.makeText(context, "Error saving download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Phương thức chuyển đổi content:// URI sang file path
    private String getFilePathFromUri(String uriString) {
        if (uriString == null) return null;

        if (uriString.startsWith("content://")) {
            try {
                Uri uri = Uri.parse(uriString);
                android.database.Cursor cursor = context.getContentResolver().query(
                        uri,
                        new String[]{android.provider.MediaStore.MediaColumns.DATA},
                        null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    String filePath = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA));
                    cursor.close();
                    return filePath;
                }

                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file path from URI: " + e.getMessage(), e);
            }
        }

        // Trả về nguyên bản nếu không phải content:// URI
        return uriString;
    }

    // Tìm file APK trong thư mục Downloads
    private void findAndSaveAPKFromDownloads() {
        File downloadDir = getDownloadDirectory();
        String expectedFileName = "minecraft-" + versionName.replace(".", "-") + ".apk";
        File expectedFile = new File(downloadDir, expectedFileName);

        if (expectedFile.exists()) {
            // File tồn tại ở vị trí dự kiến
            dbManager.markVersionAsDownloaded(versionName, expectedFile.getAbsolutePath());
            Toast.makeText(context,
                    "Found APK at expected location: " + expectedFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "APK found at expected location: " + expectedFile.getAbsolutePath());
            return;
        }

        // Tìm kiếm trong toàn bộ Download directory
        File[] files = downloadDir.listFiles((dir, name) ->
                name.startsWith("minecraft-") && name.endsWith(".apk"));

        if (files != null && files.length > 0) {
            // Lấy file mới nhất
            File latestFile = files[0];
            for (File file : files) {
                if (file.lastModified() > latestFile.lastModified()) {
                    latestFile = file;
                }
            }

            dbManager.markVersionAsDownloaded(versionName, latestFile.getAbsolutePath());
            Toast.makeText(context,
                    "Found APK: " + latestFile.getName() + "\nPath: " + latestFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "APK found: " + latestFile.getAbsolutePath());
        } else {
            Toast.makeText(context,
                    "Download completed but APK file not found. Please check Downloads folder.",
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "APK file not found in download directory: " + downloadDir.getAbsolutePath());
        }
    }

    private String getErrorReason(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                return "Cannot resume download";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return "Device not found";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                return "File already exists";
            case DownloadManager.ERROR_FILE_ERROR:
                return "File error";
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                return "HTTP data error";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                return "Insufficient space";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                return "Too many redirects";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                return "Unhandled HTTP code";
            case DownloadManager.ERROR_UNKNOWN:
            default:
                return "Unknown error";
        }
    }

    public static File getDownloadDirectory() {
        File downloadDir;

        // SỬA: Luôn sử dụng Download directory công khai
        downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDir = new File(downloadDir, APP_FOLDER);

        // Tạo thư mục nếu chưa tồn tại
        if (!appDir.exists()) {
            boolean created = appDir.mkdirs();
            Log.d(TAG, "App directory created: " + created + " at " + appDir.getAbsolutePath());
        }

        return appDir;
    }

    public static String getDownloadPathForVersion(String versionName) {
        File downloadDir = getDownloadDirectory();
        String fileName = "minecraft-" + versionName.replace(".", "-") + ".apk";
        return new File(downloadDir, fileName).getAbsolutePath();
    }

    public static boolean isVersionDownloaded(String versionName) {
        File downloadDir = getDownloadDirectory();
        String fileName = "minecraft-" + versionName.replace(".", "-") + ".apk";
        File apkFile = new File(downloadDir, fileName);
        return apkFile.exists();
    }

    public static File getAPKFile(String versionName) {
        File downloadDir = getDownloadDirectory();
        String fileName = "minecraft-" + versionName.replace(".", "-") + ".apk";
        return new File(downloadDir, fileName);
    }
}