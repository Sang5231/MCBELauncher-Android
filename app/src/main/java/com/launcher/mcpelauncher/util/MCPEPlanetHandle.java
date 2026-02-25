package com.launcher.mcpelauncher.util;

import static android.content.ContentValues.TAG;

import android.util.Log;

public class MCPEPlanetHandle {

    public static String generateDownloadUrl(String version) {
        String cleanVersion = version.trim();
        String[] parts = cleanVersion.split("\\.");

        if (parts.length == 4) {
            // Preview 4 phần: https://mcpe-planet.com/downloads/minecraft-pe-1-21/1-21-120-25/
            String major = parts[0];
            String minor = parts[1];
            String subFolder = String.format("%s-%s-%s-%s", major, minor, parts[2], parts[3]);
            Log.d(TAG, "4-part preview (MCPE-Planet): minecraft-pe-1-" + major + "/" + subFolder);
            return String.format("https://mcpe-planet.com/downloads/minecraft-pe-1-%s/%s/", major, subFolder);
        }

        if (parts.length >= 3) {
            String major = parts[0];
            try {
                int majorNum = Integer.parseInt(major);
                if (majorNum >= 26) {
                    // Preview >=26: minecraft-pe-1-26/0-26/ (hoặc tương tự)
                    return String.format("https://mcpe-planet.com/downloads/minecraft-pe-1-%s/%s-%s/",
                            major, parts.length > 1 ? parts[1] : "0", parts.length > 2 ? parts[2] : "0");
                }
            } catch (NumberFormatException ignored) {}

            // Release thường: minecraft-pe-1-21/1-21-130/
            String minor = parts[1];
            String patch = parts[2];
            return String.format("https://mcpe-planet.com/downloads/minecraft-pe-1-%s/%s-%s-%s/",
                    major, major, minor, patch);
        } else if (parts.length == 2) {
            return String.format("https://mcpe-planet.com/downloads/minecraft-pe-1-%s/%s-%s/",
                    parts[0], parts[0], parts[1]);
        }

        // Fallback
        return String.format("https://mcpe-planet.com/downloads/minecraft-pe-%s/",
                cleanVersion.replace(".", "-"));
    }

    public static String getBaseUrl() {
        return "https://mcpe-planet.com/";
    }

    public static boolean isValidVersion(String version) {
        return version != null && version.matches("^\\d+(\\.\\d+)*$");
    }
}