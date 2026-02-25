package com.launcher.mcpelauncher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.activity.ManageVersionActivity;
import com.launcher.mcpelauncher.database.DatabaseManager;

public class SettingsFragment extends Fragment {

    private RadioGroup radioGroup;
    private DatabaseManager dbManager;

    // Constants cho source values
    public static final String SOURCE_MCPEDL = "mcpedl.org";
    public static final String SOURCE_MCPE_PLANET = "mcpe-planet.com";
    private RadioGroup radioGroupMode;
    private static final String MODE_UNINSTALL = "uninstall";
    private static final String MODE_OVERRIDE  = "override";
    private static final String MODE_MANUALLY  = "manually";

    private View rootView; // Lưu root view

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        // Khởi tạo DatabaseManager
        dbManager = DatabaseManager.getInstance(requireContext());

        radioGroup = rootView.findViewById(R.id.radio_group_source);
        radioGroupMode = rootView.findViewById(R.id.radio_group_mode);

        // Load saved preference từ DATABASE - sử dụng rootView thay vì getView()
        loadSavedSource(rootView);
        loadInstallMode();

        // Lắng nghe thay đổi
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedSource;
            if (checkedId == R.id.rb_mcpeplanet) {
                selectedSource = SOURCE_MCPE_PLANET; // "mcpe-planet.com"
            } else {
                selectedSource = SOURCE_MCPEDL; // "mcpedl.org"
            }

            // Lưu vào DATABASE
            dbManager.setDownloadSource(selectedSource);

        });

        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedMode;

            if (checkedId == R.id.rb_uninstall) {
                selectedMode = MODE_UNINSTALL;
            } else if (checkedId == R.id.rb_override) {
                selectedMode = MODE_OVERRIDE;
            } else if (checkedId == R.id.rb_manually) {
                selectedMode = MODE_MANUALLY;
            } else {
                selectedMode = MODE_UNINSTALL; // fallback
            }

            dbManager.setInstallMode(selectedMode);

            // Optional: Toast thông báo (bằng tiếng Anh như yêu cầu)
            Toast.makeText(getContext(), "Install mode set to: " + selectedMode, Toast.LENGTH_SHORT).show();
        });

        // Xử lý các nút khác
        setupOtherButtons(rootView);

        return rootView;
    }

    private void loadInstallMode() {
        String savedMode = dbManager.getInstallMode();

        if (MODE_UNINSTALL.equals(savedMode)) {
            radioGroupMode.check(R.id.rb_uninstall);
        } else if (MODE_OVERRIDE.equals(savedMode)) {
            radioGroupMode.check(R.id.rb_override);
        } else if (MODE_MANUALLY.equals(savedMode)) {
            radioGroupMode.check(R.id.rb_manually);
        } else {
            // Default
            radioGroupMode.check(R.id.rb_uninstall);
        }
    }

    private void loadSavedSource(View view) {
        // Lấy nguồn tải từ database
        String savedSource = dbManager.getDownloadSource();

        if (savedSource.equals(SOURCE_MCPE_PLANET)) {
            RadioButton rbPlanet = view.findViewById(R.id.rb_mcpeplanet);
            if (rbPlanet != null) {
                rbPlanet.setChecked(true);
            }
        } else {
            // Mặc định là mcpedl
            RadioButton rbMcpedl = view.findViewById(R.id.rb_mcpedl);
            if (rbMcpedl != null) {
                rbMcpedl.setChecked(true);
            }
        }
    }

    private void setupOtherButtons(View view) {
        // Nút "Manage version"
        View btnManageVersion = view.findViewById(R.id.btn_manage_version);
        if (btnManageVersion != null) {
            btnManageVersion.setOnClickListener(v -> {
                // Mở activity quản lý version
                try {
                    Intent intent = new Intent(getActivity(), ManageVersionActivity.class);
                    startActivity(intent);

                    // Có thể thêm animation nếu muốn
                    // getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                } catch (Exception e) {
                    Toast.makeText(getContext(),
                            "Lỗi khi mở quản lý phiên bản: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });
        }

        // Nút "Manage launcher"
        View btnManageLauncher = view.findViewById(R.id.btn_manage_launcher);
        if (btnManageLauncher != null) {
            btnManageLauncher.setOnClickListener(v -> {
                Toast.makeText(getContext(),
                        "Under construction",
                        Toast.LENGTH_SHORT).show();
                // TODO: Mở activity quản lý launcher
            });
        }

        // Switch "Edit version list"
        Switch swEditVer = view.findViewById(R.id.sw_edit_ver);
        if (swEditVer != null) {
            // Load trạng thái hiện tại từ database
            swEditVer.setChecked(dbManager.isEditModeEnabled());

            swEditVer.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Lưu trạng thái vào database
                dbManager.setEditModeEnabled(isChecked);

                Toast.makeText(getContext(),
                        "Edit mode: " + (isChecked ? "On" : "Off"),
                        Toast.LENGTH_SHORT).show();

                // TODO: Có thể thêm logic refresh fragment Launcher nếu cần
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load lại khi quay lại fragment
        if (rootView != null) {
            loadSavedSource(rootView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}