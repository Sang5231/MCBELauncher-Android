package com.launcher.mcpelauncher.activity;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.launcher.mcpelauncher.R;
import com.launcher.mcpelauncher.databinding.ActivityVersionInfoBinding;

public class VersionInfoActivity extends AppCompatActivity {

    private ActivityVersionInfoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVersionInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Version Details");
        }

        String url = getIntent().getStringExtra("WIKI_URL");
        String version = getIntent().getStringExtra("VERSION");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(version);
        }

        binding.webView.setWebViewClient(new WebViewClient());
        binding.webView.setWebChromeClient(new WebChromeClient());
        binding.webView.getSettings().setJavaScriptEnabled(true); // cần cho một số tính năng wiki
        binding.webView.loadUrl(url);

        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}