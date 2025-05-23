package com.mobile2.uas_elsid.ui.checkout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.mobile2.uas_elsid.R;

public class PaymentWebViewFragment extends Fragment {
    private WebView webView;
    private String paymentUrl;

    public static PaymentWebViewFragment newInstance(String paymentUrl) {
        PaymentWebViewFragment fragment = new PaymentWebViewFragment();
        Bundle args = new Bundle();
        args.putString("payment_url", paymentUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        webView = new WebView(requireContext());
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        if (getArguments() != null) {
            paymentUrl = getArguments().getString("payment_url");
        }

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Check if the URL contains success indicators
                if (url.contains("payment_status=success") || url.contains("transaction_status=settlement")) {
                    // Navigate to Home Fragment
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_navigation_payment_webview_to_navigation_home);
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

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null) {
            webView.goBack();
        }
    }
}