package com.launcher.mcpelauncher.util;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.launcher.mcpelauncher.model.VersionItem;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class XmlParser {

    public static List<VersionItem> parseVersionsFromXml(Context context, int xmlResId) {
        List<VersionItem> versions = new ArrayList<>();

        try {
            XmlResourceParser parser = context.getResources().getXml(xmlResId);

            String currentName = "";
            String currentEdition = "RELEASE";
            String currentDate = "";
            String currentImageUrl = "";
            String currentTag = "";
            String currentState = "";

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        currentTag = parser.getName();
                        if ("version".equals(currentTag)) {
                            currentName = "";
                            currentEdition = "RELEASE";
                            currentDate = "";
                            currentImageUrl = "";
                            currentState = "";
                        }
                        break;

                    case XmlPullParser.TEXT:
                        String text = parser.getText().trim();
                        if (text.length() > 0) {
                            switch (currentTag) {
                                case "name":
                                    currentName = text;
                                    break;
                                case "edition":
                                    currentEdition = text;
                                    break;
                                case "date":
                                    currentDate = text;
                                    break;
                                case "imageUrl":
                                    if (!"NULL".equalsIgnoreCase(text)) {
                                        currentImageUrl = text;
                                    }
                                    break;
                                case "state":
                                    currentState = text;
                                    break;
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if ("version".equals(parser.getName())) {
                            if (!currentName.isEmpty()) {
                                VersionItem item = new VersionItem(
                                        currentEdition,
                                        currentName,
                                        currentDate,
                                        currentImageUrl,
                                        currentState
                                );
                                versions.add(item);
                            }
                        }
                        currentTag = "";
                        break;
                }
                eventType = parser.next();
            }

            parser.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return versions;
    }

    // Phương thức lọc version theo type
    public static List<VersionItem> filterVersionsByType(List<VersionItem> allVersions, String versionType) {
        List<VersionItem> filtered = new ArrayList<>();

        for (VersionItem version : allVersions) {
            if (version.getVersionType().equals(versionType)) {
                filtered.add(version);
            }
        }

        return filtered;
    }
}