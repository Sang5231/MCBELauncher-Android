package com.launcher.mcpelauncher.util;

import android.util.Log;

public class MCPEDLHandle {

    private static final String TAG = "MCPEDLHandle";

    public static String generateDownloadUrl(String version) {
        String cleanVersion = version.trim();

        // Nếu version có 4 phần (preview/beta kiểu 1.x.y.z)
        String[] parts = cleanVersion.split("\\.");
        if (parts.length == 4) {
            // Format: minecraft-pe-1-21-120-25-apk/
            String dashed = cleanVersion.replace(".", "-");
            Log.d(TAG, "4-part preview (MCPEDL): " + dashed + "-apk");
            return String.format("https://mcpedl.org/minecraft-pe-%s-apk/", dashed);
        }

        // Các trường hợp còn lại giữ nguyên logic cũ
        if (parts.length >= 3) {
            String major = parts[0];
            String minor = parts[1];
            String patch = parts[2];

            try {
                int majorNum = Integer.parseInt(major);

                // Đặc biệt 26.0.26
                if (majorNum == 26 && minor.equals("0") && patch.equals("26")) {
                    return String.format("https://mcpedl.org/minecraft-pe-1-%s-%s-%s/", major, minor, patch);
                }

                // Preview >=26.x.x
                if (majorNum >= 26) {
                    return String.format("https://mcpedl.org/minecraft-pe-%s-%s-%s/", major, minor, patch);
                }

                // Release thường: thêm -apk
                return String.format("https://mcpedl.org/minecraft-pe-%s-%s-%s-apk/", major, minor, patch);

            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing version: " + cleanVersion, e);
            }
        } else if (parts.length == 2) {
            return String.format("https://mcpedl.org/minecraft-pe-%s-%s/", parts[0], parts[1]);
        }

        // Fallback
        return getFallbackUrl(cleanVersion);
    }

    private static String getFallbackUrl(String version) {
        String fallback = String.format("https://mcpedl.org/minecraft-pe-%s/",
                version.replace(".", "-"));
        Log.d(TAG, "Using fallback URL: " + fallback);
        return fallback;
    }

    public static String getBaseUrl() {
        return "https://mcpedl.org/";
    }

    public static boolean isValidVersion(String version) {
        return version != null && version.matches("^\\d+(\\.\\d+)*$");
    }

    // Phương thức test để kiểm tra logic
//    public static void testUrlGeneration() {
//        String[] testVersions = {
//                "1.21.131",    // minecraft-pe-1-21-131-apk
//                "1.21.130",    // minecraft-pe-1-21-130-apk
//                "1.20.0",      // minecraft-pe-1-20-0-apk
//                "26.0.23",     // minecraft-pe-26-0-23
//                "26.0.25",     // minecraft-pe-26-0-25
//                "26.0.26",     // minecraft-pe-1-26-0-26 (đặc biệt!)
//                "26.0.27",     // minecraft-pe-26-0-27 (nếu có trong tương lai)
//                "27.0.1",      // minecraft-pe-27-0-1
//                "1.21.50.22",  // minecraft-pe-1-21-50-22 (4 phần)
//                "26.0",        // minecraft-pe-26-0
//        };
//
//        for (String version : testVersions) {
//            String url = generateDownloadUrl(version);
//            Log.d(TAG, version + " -> " + url);
//        }
//    }

}