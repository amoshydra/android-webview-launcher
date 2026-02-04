package com.example.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private EditText urlInput;
    private EditText jsInput;
    private Button launchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        urlInput = findViewById(R.id.url_input);
        jsInput = findViewById(R.id.js_input);
        launchButton = findViewById(R.id.launch_button);

        urlInput.setText("https://example.com");
        jsInput.setText("alert('Hello from JavaScript!');");

        launchButton.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            String javascript = jsInput.getText().toString().trim();

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.putExtra("url", url.isEmpty() ? "https://example.com" : url);
            intent.putExtra("javascript", javascript);
            startActivity(intent);
        });
    }
}
