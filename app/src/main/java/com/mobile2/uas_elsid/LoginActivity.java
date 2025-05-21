package com.mobile2.uas_elsid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.tabs.TabLayout;
import com.mobile2.uas_elsid.auth.AuthPagerAdapter;
import com.mobile2.uas_elsid.databinding.ActivityLoginBinding;
import com.mobile2.uas_elsid.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        setupBackgroundImage();
        setupViewPager();
        setupTabs();
        setupGuestLogin();
    }

    private void setupBackgroundImage() {
        ImageView backgroundImage = findViewById(R.id.bgImage);
        Glide.with(this)
                .load(R.drawable.bg_login2)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .override(1080, 1920)
                .into(backgroundImage);
    }

    private void setupViewPager() {
        ViewPager2 viewPager = binding.viewPager;
        viewPager.setAdapter(new AuthPagerAdapter(this));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position));
            }
        });
    }

    private void setupTabs() {
        TabLayout tabLayout = binding.tabLayout;

        TabLayout.Tab loginTab = tabLayout.newTab().setText("Login");
        TabLayout.Tab registerTab = tabLayout.newTab().setText("Register");

        tabLayout.addTab(loginTab);
        tabLayout.addTab(registerTab);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupGuestLogin() {
        binding.guestLoginButton.setOnClickListener(v -> {
            // Clear any existing session
            sessionManager.logout();

            // Set guest mode flag
            sessionManager.setGuestMode(true);

            // Navigate to HomeActivity
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    public void switchToLoginTab() {
        binding.viewPager.setCurrentItem(0);
    }
}