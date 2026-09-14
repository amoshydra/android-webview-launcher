package com.amoshydra.androidapp;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.webkit.HttpCache;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private String javascriptCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        javascriptCode = intent.getStringExtra("javascript");

        webView = findViewById(R.id.webview);
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

        webView.loadUrl(url != null ? url : "https://example.com");
        applyCacheQuota();
    }

    private void applyCacheQuota() {
        long quotaBytes = getIntent().getLongExtra("quota_bytes", -1);
        if (quotaBytes <= 0) {
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

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
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
