package com.mobile2.uas_elsid;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.tabs.TabLayout;
import com.mobile2.uas_elsid.auth.AuthPagerAdapter;
import com.mobile2.uas_elsid.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load background image with Glide
        ImageView backgroundImage = findViewById(R.id.bgImage); // Add android:id="@+id/backgroundImage" to your ImageView
        Glide.with(this)
                .load(R.drawable.bg_login2)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .override(1080, 1920) // Set a reasonable max size
                .into(backgroundImage);

        // Set up ViewPager with AuthPagerAdapter
        ViewPager2 viewPager = binding.viewPager;
        viewPager.setAdapter(new AuthPagerAdapter(this));

        // Set up TabLayout with ViewPager
        TabLayout tabLayout = binding.tabLayout;
        TabLayout.Tab loginTab = tabLayout.newTab().setText("Login");
        TabLayout.Tab registerTab = tabLayout.newTab().setText("Register");

        tabLayout.addTab(loginTab);
        tabLayout.addTab(registerTab);

        // Sync TabLayout with ViewPager
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });
    }
    public void switchToLoginTab() {
        binding.viewPager.setCurrentItem(0); // 0 is the index for login tab
    }
}