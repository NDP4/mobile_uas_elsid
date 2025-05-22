package com.mobile2.uas_elsid;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.mobile2.uas_elsid.databinding.ActivityHomeBinding;
import com.mobile2.uas_elsid.ui.checkout.PaymentWebViewFragment;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_home);
        NavController navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    public void onBackPressed() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_home);
        Fragment currentFragment = navController.getCurrentDestination().getId() == R.id.navigation_payment_webview ?
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_home) : null;

        if (currentFragment instanceof PaymentWebViewFragment) {
            PaymentWebViewFragment webViewFragment = (PaymentWebViewFragment) currentFragment;
            if (webViewFragment.canGoBack()) {
                webViewFragment.goBack();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }
}