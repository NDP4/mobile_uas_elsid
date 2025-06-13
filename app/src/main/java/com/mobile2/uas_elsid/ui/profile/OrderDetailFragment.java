package com.mobile2.uas_elsid.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.OrderDetailAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.OrderResponse;
import com.mobile2.uas_elsid.api.response.PaymentStatusResponse;
import com.mobile2.uas_elsid.databinding.FragmentOrderDetailBinding;
import com.mobile2.uas_elsid.model.Order;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private OrderDetailAdapter adapter;
    private SessionManager sessionManager;
    private int orderId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        // Setup back button
        binding.backButton.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

        // Get order ID from arguments
        orderId = getArguments().getInt("order_id", -1);
        if (orderId == -1) {
            showError("Invalid order ID");
            return binding.getRoot();
        }

        setupRecyclerView();
        loadOrderDetails();
        setupReorderButton();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new OrderDetailAdapter(requireContext());
        binding.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.orderItemsRecyclerView.setAdapter(adapter);
    }

    private void loadOrderDetails() {
        showLoading(true);
        ApiClient.getClient().getOrder(orderId).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Order order = response.body().getData().getOrder();
                    if (order != null) {
                        updateOrderDetails(response.body());
                        checkPaymentStatus(order);
                    } else {
                        showError("Order data is missing");
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                showLoading(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void updateOrderDetails(OrderResponse response) {
        Order order = response.getData().getOrder();
        if (order == null) return;

        // Order ID and Status
        binding.orderIdText.setText(String.format("Order #%d", order.getId()));
        binding.orderStatusText.setText(order.getStatus() != null ? order.getStatus() : "Pending");
        binding.orderDateText.setText(formatDate(order.getCreatedAt()));

        // Payment Details
        String paymentMethod = "cod".equalsIgnoreCase(order.getPaymentMethod()) ? "Cash on Delivery" : "Bank Transfer";
        binding.paymentMethodText.setText(paymentMethod);
        binding.paymentStatusText.setText(order.getPaymentStatus() != null ? order.getPaymentStatus() : "Pending");
        
        // Set payment status color
        int statusColor = getPaymentStatusColor(order.getPaymentStatus());
        binding.paymentStatusText.setTextColor(statusColor);

        // Show payment info for non-COD orders
        if (order.getPaymentMethod() != null && !order.getPaymentMethod().equalsIgnoreCase("cod")) {
            binding.onlinePaymentContainer.setVisibility(View.VISIBLE);
            
            // Show view payment button for certain payment statuses
            String paymentStatus = order.getPaymentStatus();
            boolean showPaymentButton = paymentStatus != null && 
                (paymentStatus.equalsIgnoreCase("paid") || 
                 paymentStatus.equalsIgnoreCase("expired") ||
                 paymentStatus.equalsIgnoreCase("failed"));
                 
            if (showPaymentButton && order.getPaymentUrl() != null) {
                binding.viewPaymentButton.setVisibility(View.VISIBLE);
                binding.viewPaymentButton.setOnClickListener(v -> {
                    // Toggle WebView visibility
                    if (binding.paymentWebview.getVisibility() == View.VISIBLE) {
                        binding.paymentWebview.setVisibility(View.GONE);
                        binding.viewPaymentButton.setText("View Payment Details");
                    } else {
                        binding.paymentWebview.setVisibility(View.VISIBLE);
                        binding.viewPaymentButton.setText("Hide Payment Details");
                        
                        // Setup WebView
                        WebSettings webSettings = binding.paymentWebview.getSettings();
                        webSettings.setJavaScriptEnabled(true);
                        webSettings.setDomStorageEnabled(true);
                        webSettings.setLoadWithOverviewMode(true);
                        webSettings.setUseWideViewPort(true);
                        webSettings.setSupportZoom(true);
                        webSettings.setBuiltInZoomControls(true);
                        webSettings.setDisplayZoomControls(false);
                        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                        webSettings.setDefaultTextEncodingName("utf-8");
                        binding.paymentWebview.clearCache(true);
                        
                        binding.paymentWebview.setWebViewClient(new WebViewClient());
                        binding.paymentWebview.loadUrl(order.getPaymentUrl());
                    }
                });
            } else {
                binding.viewPaymentButton.setVisibility(View.GONE);
            }
        } else {
            binding.onlinePaymentContainer.setVisibility(View.GONE);
            binding.paymentWebview.setVisibility(View.GONE);
        }

        // Shipping Details
        if (order.getUser() != null) {
            binding.recipientNameText.setText(order.getUser().getFullname());
        }

        String address = String.format("%s\n%s, %s %s",
                order.getShippingAddress() != null ? order.getShippingAddress() : "",
                order.getShippingCity() != null ? order.getShippingCity() : "",
                order.getShippingProvince() != null ? order.getShippingProvince() : "",
                order.getShippingPostalCode() != null ? order.getShippingPostalCode() : "");
        binding.shippingAddressText.setText(address);

        // Courier Details
        String courier = String.format("%s %s", 
            order.getCourier() != null ? order.getCourier() : "",
            order.getCourierService() != null ? order.getCourierService() : "");
        binding.courierText.setText(courier);
        
        Integer estimatedDays = order.getEstimatedDays();
        if (estimatedDays != null && estimatedDays > 0) {
            binding.estimatedDeliveryText.setText(String.format("Estimated delivery: %d days", estimatedDays));
            binding.estimatedDeliveryText.setVisibility(View.VISIBLE);
        } else {
            binding.estimatedDeliveryText.setVisibility(View.GONE);
        }

        // Order Items
        if (order.getItems() != null) {
            adapter.setItems(order.getItems());
        }

        // Set discount info if available
        if (order.getCouponUsage() != null && order.getCouponUsage().getCoupon() != null) {
            adapter.setDiscountInfo(
                    order.getCouponUsage().getDiscountAmount(),
                    order.getCouponUsage().getCoupon().getCode()
            );
            binding.discountContainer.setVisibility(View.VISIBLE);
            binding.discountText.setText("-" + formatPrice(order.getCouponUsage().getDiscountAmount()));
        } else {
            binding.discountContainer.setVisibility(View.GONE);
        }

        // Payment Summary
        binding.subtotalText.setText(formatPrice(order.getSubtotal()));
        binding.shippingCostText.setText(formatPrice(order.getShippingCost()));
        binding.totalText.setText(formatPrice(order.getTotalAmount()));

        // Show payment info for all non-COD orders
        if (order.getPaymentUrl() != null && !"cod".equalsIgnoreCase(order.getPaymentMethod())) {
            binding.paymentWebview.setVisibility(View.VISIBLE);
            WebSettings webSettings = binding.paymentWebview.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setDefaultTextEncodingName("utf-8");
            binding.paymentWebview.clearCache(true);
            
            binding.paymentWebview.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
            });
            
            binding.paymentWebview.loadUrl(order.getPaymentUrl());
        } else {
            binding.paymentWebview.setVisibility(View.GONE);
        }
    }

    private void checkPaymentStatus(Order order) {
        if (order != null && order.getPaymentMethod() != null && 
            !order.getPaymentMethod().equals("cod") && 
            order.getPaymentUrl() != null) {
            
            ApiClient.getClient().checkPaymentStatus(order.getId(), sessionManager.getUserId())
                    .enqueue(new Callback<PaymentStatusResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<PaymentStatusResponse> call,
                                           @NonNull Response<PaymentStatusResponse> response) {
                            if (response.isSuccessful() && response.body() != null && 
                                response.body().getData() != null) {
                                updatePaymentDetails(response.body().getData().toMap());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<PaymentStatusResponse> call, @NonNull Throwable t) {
                            showError("Failed to check payment status");
                        }
                    });
        }
    }

    private void updatePaymentDetails(Map<String, Object> paymentData) {
        if (paymentData != null) {
            binding.onlinePaymentContainer.setVisibility(View.VISIBLE);
            
            String transactionId = (String) paymentData.get("transaction_id");
            binding.transactionIdText.setText("Transaction ID: " + transactionId);

            String paymentTime = (String) paymentData.get("settlement_time");
            if (paymentTime != null) {
                binding.paymentTimeText.setText("Paid on " + formatDate(paymentTime));
            }

            String paymentUrl = (String) paymentData.get("payment_url");
            if (paymentUrl != null && !paymentData.get("payment_status").equals("paid")) {
                // Setup WebView
                binding.paymentWebview.setVisibility(View.VISIBLE);
                
                // Enable JavaScript
                WebSettings webSettings = binding.paymentWebview.getSettings();
                webSettings.setJavaScriptEnabled(true);
                
                // Set WebViewClient to handle redirects within the WebView
                binding.paymentWebview.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        // Check if payment is completed
                        if (url.contains("payment_status=success") || 
                            url.contains("transaction_status=settlement")) {
                            // Refresh order details
                            loadOrderDetails();
                        }
                    }
                });
                
                // Load Midtrans URL
                binding.paymentWebview.loadUrl(paymentUrl);
            } else {
                binding.paymentWebview.setVisibility(View.GONE);
            }
        }
    }

    private int getPaymentStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "paid":
                return ContextCompat.getColor(requireContext(), R.color.success);
            case "pending":
                return ContextCompat.getColor(requireContext(), R.color.warning);
            case "expired":
            case "failed":
                return ContextCompat.getColor(requireContext(), R.color.error);
            default:
                return ContextCompat.getColor(requireContext(), R.color.text_primary);
        }
    }

    private void setupReorderButton() {
        binding.reorderButton.setOnClickListener(v -> {
            Map<String, Object> reorderData = new HashMap<>();
            reorderData.put("order_id", orderId);
            reorderData.put("user_id", sessionManager.getUserId());

            binding.reorderButton.setEnabled(false);
            ApiClient.getClient().reorderItems(reorderData).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                    binding.reorderButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_navigation_order_detail_to_navigation_detail_pesanan);
                    } else {
                        showError("Failed to reorder items");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                    binding.reorderButton.setEnabled(true);
                    showError("Network error: " + t.getMessage());
                }
            });
        });
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US);
            Date date = inputFormat.parse(dateStr);
            return date != null ? outputFormat.format(date) : dateStr;
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3);
    }

    private void handleErrorResponse(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                showError("Server error: " + errorBody);
            } else {
                showError("Failed to load order details (Status " + response.code() + ")");
            }
        } catch (Exception e) {
            showError("Failed to load order details");
        }
        Navigation.findNavController(requireView()).navigateUp();
    }

    private void showLoading(boolean isLoading) {
        // TODO: Implement loading indicator
    }

    private void showError(String message) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
