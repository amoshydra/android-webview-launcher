package com.example.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

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
