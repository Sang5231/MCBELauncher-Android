package com.launcher.mcpelauncher.util;

import com.launcher.mcpelauncher.database.DatabaseManager;
import android.content.Context;

public class DownloadUrlManager {

    public static String getDownloadUrl(Context context, String version) {
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        String source = dbManager.getDownloadSource();

        // Default value nếu source null
        if (source == null || source.isEmpty()) {
            source = "mcpedl.org";
        }

        if (source.contains("mcpe-planet")) {
            return MCPEPlanetHandle.generateDownloadUrl(version);
        } else {
            // Default to MCPEDL
            return MCPEDLHandle.generateDownloadUrl(version);
        }
    }

    public static String getSourceDisplayName(Context context) {
        DatabaseManager dbManager = DatabaseManager.getInstance(context);
        String source = dbManager.getDownloadSource();

        // Default value nếu source null
        if (source == null || source.isEmpty()) {
            source = "mcpedl.org";
        }

        if (source.contains("mcpe-planet")) {
            return "MCPE-Planet";
        } else {
            return "MCPEDL";
        }
    }
}