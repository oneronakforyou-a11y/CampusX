package com.example.campusx.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusx.R;
import com.example.campusx.data.FirebaseRepository;
import com.example.campusx.ui.main.MainActivity;
import com.example.campusx.ui.onboarding.OnboardingActivity;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SystemBarsHelper.applySystemBarPadding(findViewById(R.id.splash_root));

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Navigate to onboarding after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> nextScreen = FirebaseRepository.getInstance().getCurrentFirebaseUser() != null
                    ? MainActivity.class
                    : OnboardingActivity.class;
            Intent intent = new Intent(SplashActivity.this, nextScreen);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}
