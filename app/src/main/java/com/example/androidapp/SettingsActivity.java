package com.example.androidapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private EditText urlInput;
    private EditText jsInput;
    private EditText cacheQuotaInput;
    private TextView cacheIndicator;
    private Button queryCacheButton;
    private Button launchButton;
    private RadioGroup cacheRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        urlInput = findViewById(R.id.url_input);
        jsInput = findViewById(R.id.js_input);
        cacheQuotaInput = findViewById(R.id.cache_quota_input);
        cacheIndicator = findViewById(R.id.cache_indicator);
        queryCacheButton = findViewById(R.id.query_cache_button);
        launchButton = findViewById(R.id.launch_button);
        cacheRadioGroup = findViewById(R.id.cache_radio_group);

        // Load saved cache preference
        SharedPreferences prefs = getSharedPreferences("WebViewPreferences", MODE_PRIVATE);
        int savedCacheMode = prefs.getInt("cache_mode", WebSettings.LOAD_DEFAULT);
        
        switch (savedCacheMode) {
            case WebSettings.LOAD_NO_CACHE:
                cacheRadioGroup.check(R.id.cache_no_cache);
                break;
            case WebSettings.LOAD_CACHE_ONLY:
                cacheRadioGroup.check(R.id.cache_cache_only);
                break;
            case WebSettings.LOAD_CACHE_ELSE_NETWORK:
                cacheRadioGroup.check(R.id.cache_cache_else_network);
                break;
            default:
                cacheRadioGroup.check(R.id.cache_default);
        }

        queryCacheButton.setOnClickListener(v -> {
            String url = resolveUrl();
            measureCache(url);
        });

        launchButton.setOnClickListener(v -> {
            String url = resolveUrl();
            String javascript = jsInput.getText().toString().trim();
            
            // Save cache preference
            SharedPreferences settingsPrefs = getSharedPreferences("WebViewPreferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = settingsPrefs.edit();
            
            int selectedCacheMode = WebSettings.LOAD_DEFAULT;
            int selectedRadioButtonId = cacheRadioGroup.getCheckedRadioButtonId();
            
            if (selectedRadioButtonId == R.id.cache_no_cache) {
                selectedCacheMode = WebSettings.LOAD_NO_CACHE;
            } else if (selectedRadioButtonId == R.id.cache_cache_only) {
                selectedCacheMode = WebSettings.LOAD_CACHE_ONLY;
            } else if (selectedRadioButtonId == R.id.cache_cache_else_network) {
                selectedCacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
            }
            
            editor.putInt("cache_mode", selectedCacheMode);
            editor.apply();

            measureCache(url);

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("javascript", javascript);
            intent.putExtra("quota_bytes", getRequestedQuota());
            startActivity(intent);
        });
    }

    private String resolveUrl() {
        String url = urlInput.getText().toString().trim();
        return url.isEmpty() ? "https://example.com" : url;
    }

    private long getRequestedQuota() {
        String quotaText = cacheQuotaInput.getText().toString().trim();
        if (quotaText.isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(quotaText);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void measureCache(String url) {
        try {
            Uri uri = Uri.parse(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                cacheIndicator.setText("Cannot query cache: invalid URL '" + url + "'");
                return;
            }
            String origin = uri.getScheme() + "://" + uri.getHost();
            long requestedQuota = getRequestedQuota();
            long httpCacheBytes = directorySize(getCacheDir());

            WebStorage storage = WebStorage.getInstance();
            storage.getUsageForOrigin(origin, usage ->
                storage.getQuotaForOrigin(origin, quota -> {
                    StringBuilder message = new StringBuilder()
                            .append("Origin: ").append(origin).append('\n')
                            .append("WebStorage usage: ").append(formatBytes(usage)).append('\n')
                            .append("Quota (legacy, not enforced): ").append(formatBytes(quota)).append('\n')
                            .append("HTTP cache on disk: ").append(formatBytes(httpCacheBytes));
                    if (requestedQuota > 0) {
                        message.append("\nQuota request: ").append(formatBytes(requestedQuota))
                                .append(" (applied inside the WebView)");
                    }
                    cacheIndicator.setText(message.toString());
                })
            );
        } catch (Exception e) {
            cacheIndicator.setText("Cache query failed: " + e.getMessage());
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

    private long directorySize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }
        long total = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                total += directorySize(file);
            } else {
                total += file.length();
            }
        }
        return total;
    }
}