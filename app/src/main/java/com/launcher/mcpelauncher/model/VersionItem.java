package com.launcher.mcpelauncher.model;

public class VersionItem {
    public int id;
    public String edition; // RELEASE, PREVIEW, BETA, ALPHA
    public String version;
    public String date;
    public String imageUrl;
    public String state; // Latest, Latest Preview
    public String filePath;
    public boolean isDownloaded;
    public long downloadDate;

    // Thêm trường mới để lưu loại version cho filtering
    public String versionType; // release, beta_preview, alpha

    public VersionItem() {}

    public VersionItem(String edition, String version, String date, String imageUrl) {
        this.edition = edition;
        this.version = version;
        this.date = date;
        this.imageUrl = imageUrl;
        this.isDownloaded = false;
        this.downloadDate = 0;
        this.versionType = getVersionTypeFromEdition(edition);
    }

    public VersionItem(String edition, String version, String date, String imageUrl, String state) {
        this.edition = edition;
        this.version = version;
        this.date = date;
        this.imageUrl = imageUrl;
        this.state = state;
        this.isDownloaded = false;
        this.downloadDate = 0;
        this.versionType = getVersionTypeFromEdition(edition);
    }

    private String getVersionTypeFromEdition(String edition) {
        switch (edition) {
            case "RELEASE":
                return "release";
            case "PREVIEW":
            case "BETA":
                return "beta_preview";
            case "ALPHA":
                return "alpha";
            default:
                return "release";
        }
    }

    // Getter cho versionType
    public String getVersionType() {
        return versionType;
    }
}