package com.example.aireader;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);  // Set your splash layout with image and text

        // Optionally, add a delay or allow the user to click to go to MainActivity
        new Handler().postDelayed(() -> {
            // After the delay, move to MainActivity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();  // Close SplashActivity so it can't be accessed back
        }, 3000);  // 3-second delay (you can adjust time as needed)
    }
}
