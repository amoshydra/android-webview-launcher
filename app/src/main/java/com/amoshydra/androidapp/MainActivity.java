package com.amoshydra.androidapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.webkit.HttpCache;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    /** Sentinel meaning "leave env(safe-area-inset-bottom) at the real system value". */
    private static final int NO_INSET_OVERRIDE = -1;

    private InsetAwareWebView webView;
    private String javascriptCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        javascriptCode = intent.getStringExtra("javascript");

        // Must run before setContentView so the first layout pass already
        // measures against the final window configuration.
        applyWindowMode(intent.getBooleanExtra("edge_to_edge", false));

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        // Must be set before the first inset dispatch, so it lands before loadUrl.
        webView.setInsetOverride(
                readInsetExtra("inset_top"),
                readInsetExtra("inset_right"),
                readInsetExtra("inset_bottom"),
                readInsetExtra("inset_left"));

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        int cacheMode = getCacheModeFromPreferences();
        webSettings.setCacheMode(cacheMode);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (javascriptCode != null && !javascriptCode.isEmpty()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        webView.evaluateJavascript(javascriptCode, null);
                    }, 2000);
                }
            }
        });

        applyCacheQuota();
        webView.loadUrl(url != null ? url : "https://example.com");
    }

    /**
     * Chooses between the default fullscreen look and true edge-to-edge.
     *
     * <p>Edge-to-edge deliberately adds no inset listener and injects no CSS. Since WebView
     * M136 the page's own {@code env(safe-area-inset-*)} is populated from the real
     * {@link android.view.WindowInsets} it receives, so padding here or in CSS would
     * double-inset. Letting the insets reach the WebView untouched is the whole mechanism.
     *
     * <p>The status bar is shown (transparent) rather than hidden in this mode. Hiding it
     * zeroes the {@code statusBars()} inset, which would leave the page with a top inset of
     * zero and hide the display cutout from it.
     */
    private void applyWindowMode(boolean edgeToEdge) {
        Window window = getWindow();

        if (edgeToEdge) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Without this, three-button navigation gets an opaque scrim that
                // would sit on top of the page.
                window.setNavigationBarContrastEnforced(false);
            }
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
            WindowCompat.setDecorFitsSystemWindows(window, true);
        }
    }

    private void applyCacheQuota() {
        long quotaBytes = getIntent().getLongExtra("quota_bytes", -1);
        if (quotaBytes < 0) {
            return;
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
                || !WebViewFeature.isFeatureSupported(WebViewFeature.HTTP_CACHE_MANAGER)) {
            Toast.makeText(this, "Cache quota API not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            HttpCache cache = WebViewCompat.getProfile(webView).getHttpCache();
            long before = cache.getQuotaBytes();
            cache.setQuotaBytes(quotaBytes);
            long after = cache.getQuotaBytes();
            Toast.makeText(this, "Cache quota: " + formatBytes(before) + " -> " + formatBytes(after)
                    + " (requested " + formatBytes(quotaBytes) + ")", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Cache quota set failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** @return the override in pixels, or null to leave that edge at the real system value. */
    private Integer readInsetExtra(String key) {
        int value = getIntent().getIntExtra(key, NO_INSET_OVERRIDE);
        return value == NO_INSET_OVERRIDE ? null : value;
    }

    private String formatBytes(long bytes) {        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.US, "%.2f MB", mb);
        }
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    private int getCacheModeFromPreferences() {
        SharedPreferences prefs = getSharedPreferences("WebViewPreferences", MODE_PRIVATE);
        return prefs.getInt("cache_mode", WebSettings.LOAD_DEFAULT);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
