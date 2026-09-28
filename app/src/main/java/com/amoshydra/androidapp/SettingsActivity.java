package com.amoshydra.androidapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.webkit.HttpCache;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.io.File;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    /** Sentinel meaning "no override"; any real value is a non-negative pixel count. */
    private static final int NO_OVERRIDE = -1;

    private EditText urlInput;
    private EditText jsInput;
    private EditText cacheQuotaInput;
    private TextView cacheIndicator;
    private TextView webviewInfo;
    private Button queryCacheButton;
    private Button launchButton;
    private RadioGroup cacheRadioGroup;
    private SwitchCompat edgeToEdgeSwitch;
    private EditText insetTopInput;
    private EditText insetRightInput;
    private EditText insetBottomInput;
    private EditText insetLeftInput;
    private TextView insetLabel;
    private TextView insetHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        urlInput = findViewById(R.id.url_input);
        jsInput = findViewById(R.id.js_input);
        cacheQuotaInput = findViewById(R.id.cache_quota_input);
        cacheIndicator = findViewById(R.id.cache_indicator);
        webviewInfo = findViewById(R.id.webview_info);
        queryCacheButton = findViewById(R.id.query_cache_button);
        launchButton = findViewById(R.id.launch_button);
        cacheRadioGroup = findViewById(R.id.cache_radio_group);
        edgeToEdgeSwitch = findViewById(R.id.edge_to_edge_switch);
        insetTopInput = findViewById(R.id.inset_top_input);
        insetRightInput = findViewById(R.id.inset_right_input);
        insetBottomInput = findViewById(R.id.inset_bottom_input);
        insetLeftInput = findViewById(R.id.inset_left_input);
        insetLabel = findViewById(R.id.inset_label);
        insetHint = findViewById(R.id.inset_hint);

        showWebViewInfo();

        // Load saved preferences
        SharedPreferences prefs = getSharedPreferences("WebViewPreferences", MODE_PRIVATE);
        int savedCacheMode = prefs.getInt("cache_mode", WebSettings.LOAD_DEFAULT);
        edgeToEdgeSwitch.setChecked(prefs.getBoolean("edge_to_edge", false));

        restoreInsetPref(prefs, "inset_override_top", insetTopInput);
        restoreInsetPref(prefs, "inset_override_right", insetRightInput);
        restoreInsetPref(prefs, "inset_override_bottom", insetBottomInput);
        restoreInsetPref(prefs, "inset_override_left", insetLeftInput);

        // The override only has any effect while edge-to-edge is on, because otherwise the
        // framework consumes the insets before the WebView ever sees them.
        edgeToEdgeSwitch.setOnCheckedChangeListener((v, checked) -> applyInsetInputsEnabled(checked));
        applyInsetInputsEnabled(edgeToEdgeSwitch.isChecked());
        
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
            editor.putBoolean("edge_to_edge", edgeToEdgeSwitch.isChecked());
            int insetTop = readInsetOverride(insetTopInput);
            int insetRight = readInsetOverride(insetRightInput);
            int insetBottom = readInsetOverride(insetBottomInput);
            int insetLeft = readInsetOverride(insetLeftInput);
            editor.putInt("inset_override_top", insetTop);
            editor.putInt("inset_override_right", insetRight);
            editor.putInt("inset_override_bottom", insetBottom);
            editor.putInt("inset_override_left", insetLeft);
            editor.apply();

            measureCache(url);

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("javascript", javascript);
            intent.putExtra("quota_bytes", getRequestedQuota());
            intent.putExtra("edge_to_edge", edgeToEdgeSwitch.isChecked());
            intent.putExtra("inset_top", insetTop);
            intent.putExtra("inset_right", insetRight);
            intent.putExtra("inset_bottom", insetBottom);
            intent.putExtra("inset_left", insetLeft);
            startActivity(intent);
        });
    }

    private String resolveUrl() {
        String url = urlInput.getText().toString().trim();
        return url.isEmpty() ? "https://example.com" : url;
    }

    private void restoreInsetPref(SharedPreferences prefs, String key, EditText field) {
        int value = prefs.getInt(key, NO_OVERRIDE);
        if (value != NO_OVERRIDE) {
            field.setText(String.valueOf(value));
        }
    }

    /** @return the requested inset in pixels, or {@link #NO_OVERRIDE} to leave it native. */
    private int readInsetOverride(EditText field) {
        String text = field.getText().toString().trim();
        if (text.isEmpty()) {
            return NO_OVERRIDE;
        }
        try {
            int value = Integer.parseInt(text);
            return value < 0 ? NO_OVERRIDE : value;
        } catch (NumberFormatException e) {
            return NO_OVERRIDE;
        }
    }

    private void applyInsetInputsEnabled(boolean edgeToEdgeOn) {
        float alpha = edgeToEdgeOn ? 1.0f : 0.4f;
        EditText[] inputs = {insetTopInput, insetRightInput, insetBottomInput, insetLeftInput};
        for (EditText input : inputs) {
            input.setEnabled(edgeToEdgeOn);
            input.setAlpha(alpha);
        }
        insetLabel.setEnabled(edgeToEdgeOn);
        insetLabel.setAlpha(alpha);
        insetHint.setEnabled(edgeToEdgeOn);
        insetHint.setAlpha(alpha);
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
            long cacheDirBytes = directorySize(getCacheDir());
            String quotaLine = readHttpCacheQuota();

            WebStorage.getInstance().getUsageForOrigin(origin, usage -> {
                StringBuilder message = new StringBuilder()
                        .append("Origin: ").append(origin).append('\n')
                        .append("WebStorage usage: ").append(formatBytes(usage)).append('\n')
                        .append("App cache dir: ").append(formatBytes(cacheDirBytes)).append('\n')
                        .append(quotaLine);
                if (requestedQuota >= 0) {
                    message.append("\nQuota request: ").append(formatBytes(requestedQuota))
                            .append(" (enforced by WebView; result shown after launch)");
                }
                cacheIndicator.setText(message.toString());
            });
        } catch (Exception e) {
            cacheIndicator.setText("Cache query failed: " + e.getMessage());
        }
    }

    private boolean isQuotaSupported() {
        return WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
                && WebViewFeature.isFeatureSupported(WebViewFeature.HTTP_CACHE_MANAGER);
    }

    private void showWebViewInfo() {
        PackageInfo pkg = WebViewCompat.getCurrentWebViewPackage(this);
        String version = (pkg != null && pkg.versionName != null) ? pkg.versionName : "unknown";
        int major = 0;
        try {
            major = Integer.parseInt(version.split("\\.")[0]);
        } catch (NumberFormatException ignored) {
        }
        boolean supported = isQuotaSupported();

        webviewInfo.setText("WebView " + version + (major > 0 ? " (M" + major + ")" : "")
                + "\nHTTP cache quota: " + (supported ? "supported" : "not supported (needs M151+)"));

        cacheQuotaInput.setEnabled(supported);
        if (!supported) {
            cacheQuotaInput.setText("");
            cacheQuotaInput.setHint("Unavailable: needs WebView M151+"
                    + (major > 0 ? ", this device has M" + major : ""));
        }
    }

    private String readHttpCacheQuota() {
        if (!isQuotaSupported()) {
            return "HTTP cache quota: unsupported (needs WebView M151+)";
        }
        try {
            HttpCache cache = ProfileStore.getInstance()
                    .getOrCreateProfile(Profile.DEFAULT_PROFILE_NAME)
                    .getHttpCache();
            return "HTTP cache quota: " + formatBytes(cache.getQuotaBytes())
                    + (cache.isUsingDefaultQuota() ? " (auto default)" : " (set)");
        } catch (Exception e) {
            return "HTTP cache quota: unavailable (" + e.getMessage() + ")";
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