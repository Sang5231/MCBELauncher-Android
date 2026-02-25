package com.launcher.mcpelauncher;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.launcher.mcpelauncher.fragment.InfoFragment;
import com.launcher.mcpelauncher.fragment.LauncherFragment;
import com.launcher.mcpelauncher.fragment.SettingsFragment;
import com.launcher.mcpelauncher.util.AppInstaller;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Khởi tạo Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setItemIconTintList(null);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_launcher) {
                loadFragment(new LauncherFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                return true;
            } else if (itemId == R.id.nav_info) {
                loadFragment(new InfoFragment());
                return true;
            }
            return false;
        });

        // Mặc định load LauncherFragment
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_launcher);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        // Nếu ở fragment Launcher, thoát app
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof LauncherFragment) {
            finish();
        } else {
            // Quay về LauncherFragment
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_launcher);
        }
        super.onBackPressed();
    }

    @Nullable
    @Override
    public ActionBar getSupportActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Launcher");
        return super.getSupportActionBar();
    }
}