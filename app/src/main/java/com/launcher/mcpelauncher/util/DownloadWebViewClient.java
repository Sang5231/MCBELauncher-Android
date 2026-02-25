package com.launcher.mcpelauncher.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadWebViewClient extends WebViewClient {

    private static final String TAG = "DownloadWebViewClient";
    private final Context context;
    private final String versionName;
    private final APKDownloadManager downloadManager;
    private final ProgressBar progressBar;

    // ── Patterns cho file APK ─────────────────────────────────────
    private static final Pattern APK_PATTERN = Pattern.compile(
            "(?i)https?://[^\\s]+/minecraft[-\\d\\.]+\\.apk"
    );

    public DownloadWebViewClient(Context context, String versionName) {
        this(context, versionName, null);
    }

    public DownloadWebViewClient(Context context, String versionName, ProgressBar progressBar) {
        this.context = context;
        this.versionName = versionName;
        this.downloadManager = new APKDownloadManager(context);
        this.progressBar = progressBar;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url == null) return true;

        Log.d(TAG, "shouldOverrideUrlLoading: " + url);

        // 1. Ưu tiên: link tải APK → xử lý download
        if (isAPKDownloadLink(url)) {
            handleAPKDownload(url);
            return true;
        }

        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();

        if (scheme != null &&
                !scheme.equals("http") &&
                !scheme.equals("https") &&
                !scheme.equals("about") &&
                !scheme.equals("data") &&
                !scheme.equals("javascript")) {

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.d(TAG, "Opened external intent: " + url);
            } catch (Exception e) {
                Log.w(TAG, "Cannot open intent: " + url, e);
                Toast.makeText(context, "Cannot open link: " + url, Toast.LENGTH_SHORT).show();
            }
            return true;   // đã xử lý
        }

        // 3. Còn lại → load bình thường trong WebView
        return false;
    }

    private boolean isAPKDownloadLink(String url) {
        if (url == null) return false;

        // Kiểm tra đuôi .apk + chứa minecraft-
        if (!url.toLowerCase().endsWith(".apk")) return false;

        Matcher matcher = APK_PATTERN.matcher(url);
        return matcher.find();
    }

    private void handleAPKDownload(String downloadUrl) {
        Log.d(TAG, "APK download detected: " + downloadUrl);

        String extracted = extractVersionFromUrl(downloadUrl);
        if (extracted != null && extracted.equals(versionName.replace(".", "-"))) {
            downloadManager.downloadAPK(downloadUrl, versionName);
            Toast.makeText(context, "Starting download • " + versionName, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context,
                    "Downloaded file version mismatch\nExpected: " + versionName,
                    Toast.LENGTH_LONG).show();
            Log.w(TAG, "Version mismatch → expected " + versionName + ", found " + extracted);
        }
    }

    private String extractVersionFromUrl(String url) {
        Matcher m = Pattern.compile("minecraft-([\\d-]+)\\.apk").matcher(url);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (progressBar != null) {
            progressBar.setVisibility(android.view.View.VISIBLE);
            progressBar.setProgress(10);
        }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);

        if (errorCode == -10 || description.contains("ERR_UNKNOWN_URL_SCHEME")) {
            // Không hiển thị toast mỗi lần → tránh spam
            Log.w(TAG, "Ignored known scheme error: " + failingUrl);
        } else {
            Toast.makeText(context, "Load error: " + description, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Log.d(TAG, "Page finished loading: " + url);

        // Ẩn progress bar khi load xong
        if (progressBar != null) {
            progressBar.setVisibility(android.view.View.GONE);
        }

        // Có thể inject JavaScript để phát hiện link download
        injectDownloadDetectionScript(view);
    }

    private void injectDownloadDetectionScript(WebView view) {
        String javascript = "javascript:(function() {" +
                "function attachDownloadHandler(el, url) {" +
                "  el.onclick = function(e) {" +
                "    e.preventDefault(); e.stopPropagation();" +
                "    if (url) Android.detectAPKDownload(url);" +
                "    return false;" +
                "  };" +
                "  // Visual feedback (optional but helpful for user)" +
                "  el.style.backgroundColor = '#4CAF50';" +
                "  el.style.color = 'white';" +
                "  el.style.padding = '8px 12px';" +
                "  el.style.borderRadius = '6px';" +
                "  el.style.boxShadow = '0 2px 8px rgba(0,0,0,0.3)';" +
                "}" +

                "// 1. Find all <a> with .apk in href" +
                "var links = document.querySelectorAll('a[href$=\".apk\"], a[href*=\"minecraft-\"][href$=\".apk\"]');" +
                "for (var i = 0; i < links.length; i++) {" +
                "  var href = links[i].href;" +
                "  if (href && href.includes('.apk')) {" +
                "    attachDownloadHandler(links[i], href);" +
                "  }" +
                "}" +

                "// 2. Find buttons or elements with common download classes / text" +
                "var candidates = document.querySelectorAll(" +
                "  '.button, .btn, .button-block, [class*=\"download\"], " +
                "   [class*=\"btn-primary\"], [class*=\"button-blue\"], " +
                "   [role=\"button\"], button, [onclick]" +
                "');" +
                "for (var j = 0; j < candidates.length; j++) {" +
                "  var el = candidates[j];" +
                "  var text = (el.innerText || el.textContent || '').toLowerCase();" +
                "  var hasDownloadText = text.includes('download') || text.includes('apk') || text.includes('get');" +
                "  var hasDownloadClass = /download|apk|btn|button/.test(el.className || '');" +

                "  if (hasDownloadText || hasDownloadClass) {" +
                "    // Try direct href" +
                "    if (el.href && el.href.includes('.apk')) {" +
                "      attachDownloadHandler(el, el.href);" +
                "      continue;" +
                "    }" +
                "    // Try closest parent/ancestor <a>" +
                "    var parentA = el.closest('a');" +
                "    if (parentA && parentA.href && parentA.href.includes('.apk')) {" +
                "      attachDownloadHandler(el, parentA.href);" +
                "      continue;" +
                "    }" +
                "    // Try data-href or onclick extract (some sites hide real link)" +
                "    var dataHref = el.getAttribute('data-href') || el.getAttribute('data-url');" +
                "    if (dataHref && dataHref.includes('.apk')) {" +
                "      attachDownloadHandler(el, dataHref);" +
                "    }" +
                "  }" +
                "}" +

                "// 3. MutationObserver for dynamically loaded buttons (ads, lazy sections)" +
                "var observer = new MutationObserver(function(mutations) {" +
                "  mutations.forEach(function(mutation) {" +
                "    if (mutation.addedNodes.length) {" +
                "      // Re-run detection on new nodes" +
                "      var newLinks = document.querySelectorAll('a[href$=\".apk\"]');" +
                "      newLinks.forEach(function(lnk) {" +
                "        if (lnk.href.includes('.apk')) attachDownloadHandler(lnk, lnk.href);" +
                "      });" +
                "    }" +
                "  });" +
                "});" +
                "observer.observe(document.body, { childList: true, subtree: true });" +

                "})()";

        view.loadUrl(javascript);
    }
}