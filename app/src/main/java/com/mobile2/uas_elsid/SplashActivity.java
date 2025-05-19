package com.mobile2.uas_elsid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.mobile2.uas_elsid.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set fullscreen flags
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Set layout hanya sekali
        setContentView(R.layout.activity_splash);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Setup animation
        LottieAnimationView animationView = findViewById(R.id.lottieAnimationView);
        animationView.playAnimation();

        // Check session and navigate
        new Handler().postDelayed(() -> {
            // Debug log untuk memeriksa status login
            boolean isLoggedIn = sessionManager.isLoggedIn();
            String userId = sessionManager.getUserId();
            String fullname = sessionManager.getFullname();

            if (isLoggedIn && userId != null && !userId.isEmpty()) {
                // User sudah login, langsung ke HomeActivity
                Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                // User belum login, ke LoginActivity
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            finish();
        }, SPLASH_DURATION);
    }
}