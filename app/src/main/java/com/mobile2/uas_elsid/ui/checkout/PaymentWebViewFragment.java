package com.mobile2.uas_elsid.ui.checkout;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.CartResponse;
import com.mobile2.uas_elsid.model.CartItem;
import com.mobile2.uas_elsid.utils.CartManager;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.util.List;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentWebViewFragment extends Fragment {
    private static final String CHANNEL_ID = "payment_notification";
    private static final int NOTIFICATION_ID = 1;

    private WebView webView;
    private String paymentUrl;
    private SessionManager sessionManager;
    private CartManager cartManager;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public static PaymentWebViewFragment newInstance(String paymentUrl) {
        PaymentWebViewFragment fragment = new PaymentWebViewFragment();
        Bundle args = new Bundle();
        args.putString("payment_url", paymentUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission is granted, show notification
                showPaymentSuccessNotification();
            } else {
                Toasty.error(requireContext(), "Notification permission denied").show();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        webView = new WebView(requireContext());
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        sessionManager = new SessionManager(requireContext());
        cartManager = CartManager.getInstance(requireContext());

        if (getArguments() != null) {
            paymentUrl = getArguments().getString("payment_url");
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        createNotificationChannel();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Check if the URL contains success indicators
                if (url.contains("payment_status=success") || url.contains("transaction_status=settlement")) {
                    // Clear cart first, then navigate after successful clearing
                    clearCart(new CartManager.CartCallback() {
                        @Override
                        public void onSuccess(List<CartItem> items) {
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    // Show success message
                                    Toasty.success(requireContext(), "Payment successful and cart cleared").show();
                                    // Request notification permission and show notification
                                    checkNotificationPermissionAndShow();
                                    // Navigate to Home Fragment
                                    Navigation.findNavController(requireView())
                                            .navigate(R.id.action_navigation_payment_webview_to_navigation_home);
                                });
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toasty.error(requireContext(), "Error clearing cart: " + message).show();
                                    // Navigate anyway even if cart clearing failed
                                    Navigation.findNavController(requireView())
                                            .navigate(R.id.action_navigation_payment_webview_to_navigation_home);
                                });
                            }
                        }
                    });
                    return true;
                }
                view.loadUrl(url);
                return true;
            }
        });

        if (paymentUrl != null) {
            webView.loadUrl(paymentUrl);
        }

        return webView;
    }

    private void checkNotificationPermissionAndShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                showPaymentSuccessNotification();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            showPaymentSuccessNotification();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Payment Notifications";
            String description = "Channel for payment notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showPaymentSuccessNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_success)
                .setContentTitle("Payment Successful")
                .setContentText("Your payment has been processed successfully")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 1000})
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());

        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Toasty.error(requireContext(), "Failed to show notification: " + e.getMessage()).show();
        }
    }

    private void clearCart(CartManager.CartCallback finalCallback) {
        String userId = sessionManager.getUserId();
        ApiClient.getClient().deleteCartItems(userId).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(@NonNull Call<CartResponse> call, @NonNull Response<CartResponse> response) {
                if (!isAdded() || getActivity() == null || webView == null) {
                    return;
                }
                
                if (response.isSuccessful()) {
                    // Clear local cart data
                    cartManager.clearCart(finalCallback);
                } else {
                    getActivity().runOnUiThread(() -> {
                        finalCallback.onError("Failed to clear cart from server");
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<CartResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getActivity() == null || webView == null) {
                    return;
                }
                
                getActivity().runOnUiThread(() -> {
                    finalCallback.onError("Network error: " + t.getMessage());
                });
            }
        });
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null) {
            webView.goBack();
        }
    }
}

