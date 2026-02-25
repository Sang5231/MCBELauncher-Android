package com.launcher.mcpelauncher.database;

import static android.provider.Contacts.Settings.getSetting;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.launcher.mcpelauncher.model.VersionItem;

import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private DatabaseHelper dbHelper;
    private Context context; // Đã khai báo

    private static final String PREF_NAME = "MCBE_Launcher_Prefs";
    private static final String KEY_TEMP_VERSION_AFTER_UNINSTALL = "temp_version_after_uninstall";

    private DatabaseManager(Context context) {
        // SỬA TẠI ĐÂY: Lưu context để dùng cho SharedPreferences
        this.context = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(this.context);
    }

    public static synchronized DatabaseManager getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null!");
        }
        if (instance == null) {
            instance = new DatabaseManager(context);
        }
        return instance;
    }

    // === THÊM PHƯƠNG THỨC NÀY ĐỂ DÙNG SAU KHI ĐÃ KHỞI TẠO ===
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseManager chưa được khởi tạo! Gọi getInstance(Context) trước.");
        }
        return instance;
    }

    // Settings methods
    public String getSelectedVersion() {
        return dbHelper.getSelectedVersion();
    }

    public void setSelectedVersion(String version) {
        dbHelper.setSelectedVersion(version);
    }

    public String getDownloadSource() {
        return dbHelper.getDownloadSource();
    }

    public void setDownloadSource(String source) {
        dbHelper.setDownloadSource(source);
    }

    // Version methods
    public List<VersionItem> getAllDownloadedVersions() {
        return dbHelper.getAllDownloadedVersions();
    }

    public List<VersionItem> getAllVersions() {
        return dbHelper.getAllVersions();
    }

    public void addVersion(VersionItem version) {
        dbHelper.addVersion(version);
    }

    public void markVersionAsDownloaded(String versionName, String filePath) {
        dbHelper.markVersionAsDownloaded(versionName, filePath);
    }

    public boolean isVersionDownloaded(String versionName) {
        return dbHelper.isVersionDownloaded(versionName);
    }

    public void updateVersion(VersionItem version) {
        dbHelper.addVersion(version); // CONFLICT_REPLACE sẽ update
    }

    public void deleteVersion(String versionName) {
        dbHelper.deleteVersion(versionName);
    }

    public void clearAllVersions() {
        dbHelper.clearAllVersions();
    }

    public boolean isDatabaseEmpty() {
        return dbHelper.isDatabaseEmpty();
    }

    public boolean isVersionInDatabase(String versionName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean exists = false;

        Cursor cursor = db.query(DatabaseHelper.TABLE_VERSIONS,
                new String[]{DatabaseHelper.COLUMN_VERSION_ID},
                DatabaseHelper.COLUMN_VERSION + " = ?",
                new String[]{versionName},
                null, null, null);

        exists = cursor.moveToFirst();
        cursor.close();
        db.close();

        return exists;
    }

    public SQLiteDatabase getWritableDatabase() {
        return dbHelper.getWritableDatabase();
    }

    public SQLiteDatabase getReadableDatabase() {
        return dbHelper.getReadableDatabase();
    }

    public void fixDatabaseConsistency() {
        dbHelper.fixVersionDuplicates();
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    public VersionItem getVersionByName(String versionName) {
        return dbHelper.getVersionByName(versionName);
    }

    // === SỬA AN TOÀN: Kiểm tra context trước khi dùng ===
    private SharedPreferences getPrefs() {
        if (context == null) {
            throw new IllegalStateException("Context is null! DatabaseManager chưa được khởi tạo đúng cách.");
        }
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setTempVersionToInstallAfterUninstall(String version) {
        getPrefs().edit()
                .putString(KEY_TEMP_VERSION_AFTER_UNINSTALL, version)
                .apply();
    }

    public String getTempVersionToInstallAfterUninstall() {
        return getPrefs().getString(KEY_TEMP_VERSION_AFTER_UNINSTALL, null);
    }

    public void clearTempVersionToInstallAfterUninstall() {
        getPrefs().edit()
                .remove(KEY_TEMP_VERSION_AFTER_UNINSTALL)
                .apply();
    }

    public boolean isEditModeEnabled() {
        return dbHelper.isEditModeEnabled();
    }

    public void setEditModeEnabled(boolean enabled) {
        dbHelper.setEditModeEnabled(enabled);
    }

    public String getInstallMode() {
        return dbHelper.getInstallMode();
    }

    public void setInstallMode(String mode) {
        dbHelper.setInstallMode(mode);
    }
}