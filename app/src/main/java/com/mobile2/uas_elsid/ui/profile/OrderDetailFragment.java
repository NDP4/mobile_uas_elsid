package com.mobile2.uas_elsid.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.DetailPesananAdapter;
import com.mobile2.uas_elsid.adapter.OrderDetailAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.OrderResponse;
import com.mobile2.uas_elsid.databinding.FragmentOrderDetailBinding;
import com.mobile2.uas_elsid.model.Order;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.text.NumberFormat;
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
        ApiClient.getClient().getOrder(orderId).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Order order = response.body().getData().getOrder();
                    if (order != null) {
                        updateOrderDetails(order);
                    } else {
                        showError("Order data is missing");
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                } else {
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
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                showError("Network error: " + t.getMessage());
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    private void updateOrderDetails(Order order) {
        // Update UI with order details
        binding.orderStatusText.setText(order.getStatus());
        binding.orderDateText.setText("Ordered on " + order.getCreatedAt());

        String address = String.format("%s\n%s, %s %s",
                order.getShippingAddress(),
                order.getShippingCity(),
                order.getShippingProvince(),
                order.getShippingPostalCode());
        binding.shippingAddressText.setText(address);

        // Set items to adapter
        adapter.setItems(order.getItems());

        // Set discount info if available
        if (order.getCouponUsage() != null) {
            adapter.setDiscountInfo(
                order.getCouponUsage().getDiscountAmount(),
                order.getCouponUsage().getCoupon().getCode()
            );
        }

        // Update order summary
        binding.subtotalText.setText(formatPrice(order.getSubtotal()));
        binding.shippingCostText.setText(formatPrice(order.getShippingCost()));

        // Show discount in summary if available
        if (order.getCouponUsage() != null) {
            binding.discountContainer.setVisibility(View.VISIBLE);
            binding.discountText.setText("-" + formatPrice(order.getCouponUsage().getDiscountAmount()));
        } else {
            binding.discountContainer.setVisibility(View.GONE);
        }

        binding.totalText.setText(formatPrice(order.getTotalAmount()));
    }

    private void setupReorderButton() {
        binding.reorderButton.setOnClickListener(v -> {
            Map<String, Object> reorderData = new HashMap<>();
            reorderData.put("order_id", orderId);
            reorderData.put("user_id", sessionManager.getUserId());

            ApiClient.getClient().reorderItems(reorderData).enqueue(new Callback<OrderResponse>() {
                @Override
                public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_navigation_order_detail_to_navigation_detail_pesanan);
                    } else {
                        showError("Failed to reorder items");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                    showError("Network error: " + t.getMessage());
                }
            });
        });
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3);
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
