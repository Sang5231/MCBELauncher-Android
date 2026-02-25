package com.launcher.mcpelauncher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.activity.VersionInfoActivity;
import com.launcher.mcpelauncher.adapter.VersionAdapter;
import com.launcher.mcpelauncher.model.VersionItem;
import com.launcher.mcpelauncher.util.XmlParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VersionFragment extends Fragment {

    private RecyclerView rvVersions;
    private VersionAdapter adapter;
    private List<VersionItem> allVersions = new ArrayList<>();
    private List<VersionItem> displayedVersions = new ArrayList<>();
    private RadioGroup radioFilter;
    private boolean isDataLoaded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_version, container, false);

        rvVersions = view.findViewById(R.id.rv_versions);
        radioFilter = view.findViewById(R.id.radio_group_filter);

        setupRecyclerView();
        setupFilter();
        loadVersionData();

        return view;
    }

    private void setupRecyclerView() {
        rvVersions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new VersionAdapter(displayedVersions, this::onVersionClicked);
        rvVersions.setAdapter(adapter);
    }

    private void setupFilter() {
        radioFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == -1 || !isDataLoaded) return;
            applyFilterFromRadioId(checkedId);
        });

        radioFilter.post(() -> {
            radioFilter.check(R.id.rb_release);
        });
    }

    private void applyFilterFromRadioId(int radioId) {
        String filterType = "RELEASE";

        if (radioId == R.id.rb_all) {
            filterType = "ALL";
        } else if (radioId == R.id.rb_release) {
            filterType = "RELEASE";
        } else if (radioId == R.id.rb_preview) {
            filterType = "PREVIEW";
        } else if (radioId == R.id.rb_beta) {
            filterType = "BETA";
        } else if (radioId == R.id.rb_alpha) {
            filterType = "ALPHA";
        }

        filterVersions(filterType);
    }

    private void filterVersions(String type) {
        displayedVersions.clear();

        if ("ALL".equals(type)) {
            displayedVersions.addAll(allVersions); // đã sort sẵn
        } else {
            List<VersionItem> filtered = new ArrayList<>();
            for (VersionItem item : allVersions) {
                if (item.edition.equals(type)) {
                    filtered.add(item);
                }
            }
            // Sort lại danh sách đã lọc (để đảm bảo thứ tự mới nhất trong nhóm)
            sortList(filtered);
            displayedVersions.addAll(filtered);
        }

        adapter.updateList(displayedVersions);
    }

    // Sửa logic sắp xếp để xử lý đúng 26.x > 1.21.x > 1.20.x, v.v.
    private void sortList(List<VersionItem> list) {
        Collections.sort(list, new Comparator<VersionItem>() {
            @Override
            public int compare(VersionItem v1, VersionItem v2) {
                int priority1 = getPriority(v1);
                int priority2 = getPriority(v2);

                if (priority1 != priority2) {
                    return Integer.compare(priority1, priority2); // cao hơn đứng trước
                }

                return compareVersions(v1.version, v2.version);
            }

            private int getPriority(VersionItem item) {
                if ("Latest".equals(item.state)) return 7; //20
                if ("Latest Preview".equals(item.state)) return 8; //19
                switch (item.edition) {
                    case "RELEASE": return 10;
                    case "PREVIEW": return 9;
                    case "BETA":    return 19;
                    case "ALPHA":   return 20;
                    default:        return 0;
                }
            }

            private int compareVersions(String ver1, String ver2) {
                boolean is26_1 = ver1.startsWith("26.");
                boolean is26_2 = ver2.startsWith("26.");

                if (is26_1 && !is26_2) return -1;
                if (!is26_1 && is26_2) return 1;

                String[] parts1 = ver1.split("[^0-9]+");
                String[] parts2 = ver2.split("[^0-9]+");

                int len = Math.max(parts1.length, parts2.length);
                for (int i = 0; i < len; i++) {
                    int num1 = (i < parts1.length) ? safeParseInt(parts1[i]) : 0;
                    int num2 = (i < parts2.length) ? safeParseInt(parts2[i]) : 0;

                    if (num1 != num2) {
                        return Integer.compare(num2, num1); // đảo ngược → mới nhất trước
                    }
                }

                return Integer.compare(parts2.length, parts1.length);
            }

            private int safeParseInt(String s) {
                try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
            }
        });
    }

    private void sortVersions(List<VersionItem> versions) {
        Collections.sort(versions, new Comparator<VersionItem>() {
            @Override
            public int compare(VersionItem v1, VersionItem v2) {
                // Ưu tiên "Latest" và "Latest Preview"
                int priority1 = getPriority(v1);
                int priority2 = getPriority(v2);

                if (priority1 != priority2) {
                    return Integer.compare(priority1, priority2); // Priority cao hơn đứng trước
                }

                // So sánh phiên bản
                return compareVersions(v1.version, v2.version);
            }

            private int getPriority(VersionItem item) {
                // Giả sử sau này bạn thêm trường state vào VersionItem
                // Hiện tại đọc từ XML, bạn có thể thêm logic nếu cần
                // Ví dụ:
                // if ("Latest".equals(item.state)) return 3;
                // if ("Latest Preview".equals(item.state)) return 2;

                // Hiện tại ưu tiên RELEASE > PREVIEW > BETA > ALPHA
                switch (item.edition) {
                    case "RELEASE": return 10;
                    case "PREVIEW": return 9;
                    case "BETA":    return 8;
                    case "ALPHA":   return 7;
                    default:        return 0;
                }
            }

            private int compareVersions(String ver1, String ver2) {
                // 1. Phiên bản 26.x luôn lớn hơn 1.x.x
                boolean is26_1 = ver1.startsWith("26.");
                boolean is26_2 = ver2.startsWith("26.");

                if (is26_1 && !is26_2) return -1; // 26.x đứng trước 1.x
                if (!is26_1 && is26_2) return 1;

                // 2. So sánh từng phần số (1.21.131 vs 1.21.130)
                String[] parts1 = ver1.split("[^0-9]+"); // tách bằng mọi ký tự không phải số
                String[] parts2 = ver2.split("[^0-9]+");

                int len = Math.max(parts1.length, parts2.length);

                for (int i = 0; i < len; i++) {
                    int num1 = (i < parts1.length) ? safeParseInt(parts1[i]) : 0;
                    int num2 = (i < parts2.length) ? safeParseInt(parts2[i]) : 0;

                    if (num1 != num2) {
                        return Integer.compare(num2, num1); // đảo ngược → mới nhất trước
                    }
                }

                // Nếu bằng nhau, phiên bản dài hơn (có thêm build) coi là mới hơn
                return Integer.compare(parts2.length, parts1.length);
            }

            private int safeParseInt(String s) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        });
    }

    private void loadVersionData() {
        allVersions.clear();

        List<VersionItem> xmlVersions = XmlParser.parseVersionsFromXml(
                requireContext(),
                R.xml.version_list
        );

        if (!xmlVersions.isEmpty()) {
            // Chỉ sort toàn bộ danh sách một lần khi load
            sortList(xmlVersions);
            allVersions.addAll(xmlVersions);
        } else {
            Toast.makeText(getContext(),
                    "Không tìm thấy dữ liệu phiên bản",
                    Toast.LENGTH_SHORT).show();
        }

        isDataLoaded = true;
        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        int checkedId = radioFilter.getCheckedRadioButtonId();
        if (checkedId != -1) {
            applyFilterFromRadioId(checkedId);
        } else {
            filterVersions("ALL");
        }
    }

    private void onVersionClicked(VersionItem version) {
        String wikiUrl = generateWikiUrl(version);
        Intent intent = new Intent(getContext(), VersionInfoActivity.class);
        intent.putExtra("WIKI_URL", wikiUrl);
        intent.putExtra("VERSION", version.edition + " " + version.version);
        startActivity(intent);
    }

    private String generateWikiUrl(VersionItem version) {
        String base = "https://minecraft.wiki/w/";
        String ver = version.version.replace(" ", "_");

        switch (version.edition) {
            case "RELEASE":
                return base + "Bedrock_Edition_" + ver;
            case "PREVIEW":
                return base + "Bedrock_Edition_Preview_" + ver;
            case "BETA":
                try {
                    String[] parts = version.version.split("\\.");
                    int major = Integer.parseInt(parts[0]);
                    int minor = Integer.parseInt(parts[1]);
                    int patch = Integer.parseInt(parts[2]);

                    if (major < 1 ||
                            (major == 1 && minor < 18) ||
                            (major == 1 && minor == 18 && patch < 20) ||
                            (major == 1 && minor == 18 && patch == 20 && Integer.parseInt(parts[3].split("-")[0]) < 24)) {
                        return base + "Bedrock_Edition_beta_" + ver;
                    }
                } catch (Exception ignored) {
                }
                return base + "Bedrock_Edition_Preview_" + ver;
            case "ALPHA":
                return base + "Pocket_Edition_alpha_" + ver;
            default:
                return base + "Bedrock_Edition_" + ver;
        }
    }
}