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
            startActivity(intent);
        });
    }

    private String resolveUrl() {
        String url = urlInput.getText().toString().trim();
        return url.isEmpty() ? "https://example.com" : url;
    }

    private void measureCache(String url) {
        try {
            Uri uri = Uri.parse(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                cacheIndicator.setText("Cannot query cache: invalid URL '" + url + "'");
                return;
            }
            String origin = uri.getScheme() + "://" + uri.getHost();
            String quotaText = cacheQuotaInput.getText().toString().trim();
            boolean hasQuota = !quotaText.isEmpty();
            long newQuota = hasQuota ? Long.parseLong(quotaText) : 0;

            WebStorage storage = WebStorage.getInstance();
            storage.getQuotaForOrigin(origin, quota ->
                storage.getUsageForOrigin(origin, usage -> {
                    String message = "Origin: " + origin + "\n"
                            + "Usage: " + usage + " bytes\n"
                            + "Quota: " + quota + " bytes";
                    if (hasQuota) {
                        storage.setQuotaForOrigin(origin, newQuota);
                        message += "\nSet quota to " + newQuota + " bytes";
                    }
                    cacheIndicator.setText(message);
                })
            );
        } catch (Exception e) {
            cacheIndicator.setText("Cache query failed: " + e.getMessage());
        }
    }
}