package com.launcher.mcpelauncher.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.launcher.mcpelauncher.model.VersionItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseManager instance;
    private Context context;
    private static final String DATABASE_NAME = "MCBELauncher.db";
    private static final int DATABASE_VERSION = 1;

    // Table: Settings (lưu cài đặt nguồn tải)
    public static final String TABLE_SETTINGS = "app_settings"; // Đổi tên bảng cho rõ ràng
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SETTING_KEY = "setting_key"; // Đổi tên cột
    public static final String COLUMN_SETTING_VALUE = "setting_value";

    // Table: DownloadedVersions (lưu phiên bản đã tải)
    public static final String TABLE_VERSIONS = "downloaded_versions";
    public static final String COLUMN_VERSION_ID = "version_id";
    public static final String COLUMN_EDITION = "edition";
    public static final String COLUMN_VERSION = "version";
    public static final String COLUMN_RELEASE_DATE = "release_date";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_STATE = "state";
    public static final String COLUMN_FILE_PATH = "file_path";
    public static final String COLUMN_IS_DOWNLOADED = "is_downloaded";
    public static final String COLUMN_DOWNLOAD_DATE = "download_date";

    // Setting keys
    public static final String SETTING_DOWNLOAD_SOURCE = "download_source"; // "mcpedl" hoặc "mcpe-planet"
    public static final String SETTING_SELECTED_VERSION = "selected_version"; // Phiên bản đang được chọn để chơi
    public static final String SETTING_INSTALL_MODE = "install_mode";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng settings với tên cột đã sửa
        String CREATE_SETTINGS_TABLE = "CREATE TABLE " + TABLE_SETTINGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SETTING_KEY + " TEXT NOT NULL UNIQUE,"
                + COLUMN_SETTING_VALUE + " TEXT"
                + ")";
        db.execSQL(CREATE_SETTINGS_TABLE);

        // Tạo bảng downloaded_versions
        String CREATE_VERSIONS_TABLE = "CREATE TABLE " + TABLE_VERSIONS + "("
                + COLUMN_VERSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EDITION + " TEXT,"
                + COLUMN_VERSION + " TEXT NOT NULL UNIQUE,"  // ← QUAN TRỌNG: UNIQUE
                + COLUMN_RELEASE_DATE + " TEXT,"
                + COLUMN_IMAGE_URL + " TEXT,"
                + COLUMN_STATE + " TEXT,"
                + COLUMN_FILE_PATH + " TEXT,"
                + COLUMN_IS_DOWNLOADED + " INTEGER DEFAULT 0,"
                + COLUMN_DOWNLOAD_DATE + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_VERSIONS_TABLE);

        // Chèn cài đặt mặc định
        initializeDefaultSettings(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VERSIONS);
        onCreate(db);
    }

    private void initializeDefaultSettings(SQLiteDatabase db) {
        // Thiết lập nguồn tải mặc định là mcpedl.org
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETTING_KEY, SETTING_DOWNLOAD_SOURCE);
        values.put(COLUMN_SETTING_VALUE, "mcpedl.org"); // Sửa thành .org
        db.insert(TABLE_SETTINGS, null, values);

        // Thiết lập phiên bản được chọn mặc định (rỗng)
        values.clear();
        values.put(COLUMN_SETTING_KEY, SETTING_SELECTED_VERSION);
        values.put(COLUMN_SETTING_VALUE, "");
        db.insert(TABLE_SETTINGS, null, values);

        values.clear();
        values.put(COLUMN_SETTING_KEY, "edit_mode_enabled");
        values.put(COLUMN_SETTING_VALUE, "false"); // Mặc định tắt
        db.insert(TABLE_SETTINGS, null, values);
    }

    // =============== SETTINGS METHODS ===============

    public String getSetting(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        String value = "";

        Cursor cursor = db.query(TABLE_SETTINGS,
                new String[]{COLUMN_SETTING_VALUE},
                COLUMN_SETTING_KEY + " = ?",
                new String[]{key},
                null, null, null);

        if (cursor.moveToFirst()) {
            value = cursor.getString(0);
        }
        cursor.close();
        db.close();

        return value;
    }

    public void updateSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETTING_VALUE, value);

        int rows = db.update(TABLE_SETTINGS, values,
                COLUMN_SETTING_KEY + " = ?",
                new String[]{key});

        // Nếu chưa có key này, thêm mới
        if (rows == 0) {
            values.put(COLUMN_SETTING_KEY, key);
            db.insert(TABLE_SETTINGS, null, values);
        }

        db.close();
    }

    // =============== VERSION METHODS ===============

    public void addVersion(VersionItem version) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EDITION, version.edition);
        values.put(COLUMN_VERSION, version.version);
        values.put(COLUMN_RELEASE_DATE, version.date);
        values.put(COLUMN_IMAGE_URL, version.imageUrl != null ? version.imageUrl : "");
        values.put(COLUMN_STATE, version.state);
        values.put(COLUMN_FILE_PATH, version.filePath);
        values.put(COLUMN_IS_DOWNLOADED, version.isDownloaded ? 1 : 0);
        values.put(COLUMN_DOWNLOAD_DATE, version.downloadDate);

        // Nếu version đã tồn tại → thay thế hoàn toàn bản ghi cũ
        db.insertWithOnConflict(TABLE_VERSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<VersionItem> getAllDownloadedVersions() {
        List<VersionItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_VERSIONS, null,
                COLUMN_IS_DOWNLOADED + " = 1", null, null, null,
                COLUMN_DOWNLOAD_DATE + " DESC"); // Sắp xếp mới nhất trước

        while (cursor.moveToNext()) {
            list.add(cursorToVersionItem(cursor));
        }
        cursor.close();
        db.close();
        return list;
    }

    // Phương thức mới: Lấy tất cả phiên bản (cả đã tải và chưa tải)
    public List<VersionItem> getAllVersions() {
        List<VersionItem> versions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_VERSIONS +
                " ORDER BY " + COLUMN_DOWNLOAD_DATE + " DESC, " +
                COLUMN_VERSION + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                VersionItem version = cursorToVersionItem(cursor);
                versions.add(version);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return versions;
    }

    // Phương thức helper để chuyển Cursor thành VersionItem
    private VersionItem cursorToVersionItem(Cursor cursor) {
        VersionItem version = new VersionItem();

        // Lấy index của các cột
        int idIndex = cursor.getColumnIndex(COLUMN_VERSION_ID);
        int editionIndex = cursor.getColumnIndex(COLUMN_EDITION);
        int versionIndex = cursor.getColumnIndex(COLUMN_VERSION);
        int dateIndex = cursor.getColumnIndex(COLUMN_RELEASE_DATE);
        int imageUrlIndex = cursor.getColumnIndex(COLUMN_IMAGE_URL);
        int stateIndex = cursor.getColumnIndex(COLUMN_STATE);
        int filePathIndex = cursor.getColumnIndex(COLUMN_FILE_PATH);
        int isDownloadedIndex = cursor.getColumnIndex(COLUMN_IS_DOWNLOADED);
        int downloadDateIndex = cursor.getColumnIndex(COLUMN_DOWNLOAD_DATE);

        // Kiểm tra và gán giá trị
        if (idIndex != -1) version.id = cursor.getInt(idIndex);
        if (editionIndex != -1) version.edition = cursor.getString(editionIndex);
        if (versionIndex != -1) version.version = cursor.getString(versionIndex);
        if (dateIndex != -1) version.date = cursor.getString(dateIndex);
        if (imageUrlIndex != -1) version.imageUrl = cursor.getString(imageUrlIndex);
        if (stateIndex != -1) version.state = cursor.getString(stateIndex);
        if (filePathIndex != -1) version.filePath = cursor.getString(filePathIndex);
        if (isDownloadedIndex != -1) version.isDownloaded = cursor.getInt(isDownloadedIndex) == 1;
        if (downloadDateIndex != -1) version.downloadDate = cursor.getLong(downloadDateIndex);

        return version;
    }

    public void markVersionAsDownloaded(String versionName, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Kiểm tra xem phiên bản đã tồn tại chưa
        Cursor cursor = db.query(TABLE_VERSIONS,
                new String[]{COLUMN_VERSION_ID, COLUMN_EDITION, COLUMN_RELEASE_DATE},
                COLUMN_VERSION + " = ?",
                new String[]{versionName},
                null, null, null);

        ContentValues values = new ContentValues();
        values.put(COLUMN_VERSION, versionName);
        values.put(COLUMN_FILE_PATH, filePath);
        values.put(COLUMN_IS_DOWNLOADED, 1);
        values.put(COLUMN_DOWNLOAD_DATE, System.currentTimeMillis());

        if (cursor.moveToFirst()) {
            // Phiên bản đã tồn tại - CẬP NHẬT
            int idIndex = cursor.getColumnIndex(COLUMN_VERSION_ID);
            int editionIndex = cursor.getColumnIndex(COLUMN_EDITION);
            int dateIndex = cursor.getColumnIndex(COLUMN_RELEASE_DATE);

            // Giữ lại thông tin cũ nếu có
            if (editionIndex != -1) {
                String edition = cursor.getString(editionIndex);
                if (edition != null && !edition.isEmpty()) {
                    values.put(COLUMN_EDITION, edition);
                } else {
                    values.put(COLUMN_EDITION, "RELEASE"); // Mặc định
                }
            }

            if (dateIndex != -1) {
                String date = cursor.getString(dateIndex);
                if (date != null && !date.isEmpty()) {
                    values.put(COLUMN_RELEASE_DATE, date);
                }
            }

            // Cập nhật phiên bản đã tồn tại
            db.update(TABLE_VERSIONS, values,
                    COLUMN_VERSION_ID + " = ?",
                    new String[]{String.valueOf(cursor.getInt(idIndex))});

            Log.d("DatabaseHelper", "Updated existing version: " + versionName);
        } else {
            // Thêm phiên bản mới
            // Thêm các giá trị mặc định cho các trường khác
            values.put(COLUMN_EDITION, "RELEASE");
            values.put(COLUMN_RELEASE_DATE, "Unknown");
            values.put(COLUMN_IMAGE_URL, "");
            values.put(COLUMN_STATE, "");

            long newId = db.insert(TABLE_VERSIONS, null, values);
            Log.d("DatabaseHelper", "Added new version: " + versionName + " with ID: " + newId);
        }

        cursor.close();
        db.close();
    }

    public void fixVersionDuplicates() {
        SQLiteDatabase db = this.getWritableDatabase();

        // Tìm các phiên bản trùng lặp
        String duplicateQuery = "SELECT " + COLUMN_VERSION + ", COUNT(*) as count " +
                "FROM " + TABLE_VERSIONS +
                " GROUP BY " + COLUMN_VERSION +
                " HAVING count > 1";

        Cursor cursor = db.rawQuery(duplicateQuery, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String version = cursor.getString(cursor.getColumnIndex(COLUMN_VERSION));
                @SuppressLint("Range") int count = cursor.getInt(cursor.getColumnIndex("count"));

                Log.d("DatabaseHelper", "Found duplicate: " + version + " (" + count + " entries)");

                // Giữ lại bản ghi mới nhất và xóa các bản cũ
                String keepLatestQuery = "DELETE FROM " + TABLE_VERSIONS +
                        " WHERE " + COLUMN_VERSION + " = ? AND " +
                        COLUMN_VERSION_ID + " NOT IN (" +
                        "SELECT MAX(" + COLUMN_VERSION_ID + ") FROM " + TABLE_VERSIONS +
                        " WHERE " + COLUMN_VERSION + " = ?)";

                db.execSQL(keepLatestQuery, new String[]{version, version});

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
    }

    // Thêm phương thức để lấy version item đầy đủ
    public VersionItem getVersionItem(String versionName) {
        SQLiteDatabase db = this.getReadableDatabase();
        VersionItem version = null;

        Cursor cursor = db.query(TABLE_VERSIONS,
                null, // Tất cả các cột
                COLUMN_VERSION + " = ?",
                new String[]{versionName},
                null, null, null);

        if (cursor.moveToFirst()) {
            version = cursorToVersionItem(cursor);
        }

        cursor.close();
        db.close();
        return version;
    }

    public boolean isVersionDownloaded(String versionName) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean downloaded = false;

        Cursor cursor = db.query(TABLE_VERSIONS,
                new String[]{COLUMN_IS_DOWNLOADED},
                COLUMN_VERSION + " = ?",
                new String[]{versionName},
                null, null, null);

        if (cursor.moveToFirst()) {
            downloaded = cursor.getInt(0) == 1;
        }

        cursor.close();
        db.close();
        return downloaded;
    }

    public String getSelectedVersion() {
        return getSetting(SETTING_SELECTED_VERSION);
    }

    public void setSelectedVersion(String version) {
        updateSetting(SETTING_SELECTED_VERSION, version);
    }

    public String getDownloadSource() {
        return getSetting(SETTING_DOWNLOAD_SOURCE);
    }

    public void setDownloadSource(String source) {
        updateSetting(SETTING_DOWNLOAD_SOURCE, source);
    }

    // Phương thức mới: Xóa một phiên bản
    public void deleteVersion(String versionName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_VERSIONS, COLUMN_VERSION + " = ?", new String[]{versionName});
        db.close();
    }

    // Phương thức mới: Xóa tất cả phiên bản
    public void clearAllVersions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_VERSIONS, null, null);
        db.close();
    }

    // Phương thức mới: Kiểm tra xem database có trống không
    public boolean isDatabaseEmpty() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_VERSIONS, null);
        boolean isEmpty = false;

        if (cursor.moveToFirst()) {
            isEmpty = cursor.getInt(0) == 0;
        }

        cursor.close();
        db.close();
        return isEmpty;
    }

    public VersionItem getVersionByName(String versionName) {
        SQLiteDatabase db = this.getReadableDatabase();
        VersionItem version = null;

        Cursor cursor = db.query(TABLE_VERSIONS, null,
                COLUMN_VERSION + " = ?", new String[]{versionName},
                null, null, null);

        if (cursor.moveToFirst()) {
            version = cursorToVersionItem(cursor);
        }
        cursor.close();
        db.close();
        return version;
    }

    public boolean isEditModeEnabled() {
        String value = getSetting("edit_mode_enabled");
        return "true".equals(value);
    }

    public void setEditModeEnabled(boolean enabled) {
        updateSetting("edit_mode_enabled", enabled ? "true" : "false");
    }

    public String getInstallMode() {
        String mode = getSetting(SETTING_INSTALL_MODE);
        // Default về "uninstall" nếu chưa có
        return (mode == null || mode.isEmpty()) ? "uninstall" : mode;
    }

    public void setInstallMode(String mode) {
        if ("uninstall".equals(mode) || "override".equals(mode) || "manually".equals(mode)) {
            updateSetting(SETTING_INSTALL_MODE, mode);
        }
    }
}