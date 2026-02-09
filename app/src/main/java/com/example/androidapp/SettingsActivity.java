package com.example.androidapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private EditText urlInput;
    private EditText jsInput;
    private Button launchButton;
    private RadioGroup cacheRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        urlInput = findViewById(R.id.url_input);
        jsInput = findViewById(R.id.js_input);
        launchButton = findViewById(R.id.launch_button);
        cacheRadioGroup = findViewById(R.id.cache_radio_group);

        urlInput.setText("https://example.com");
        jsInput.setText("alert('Hello from JavaScript!');");

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

        launchButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
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

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.putExtra("url", url.isEmpty() ? "https://example.com" : url);
            intent.putExtra("javascript", javascript);
            startActivity(intent);
        });
    }
}