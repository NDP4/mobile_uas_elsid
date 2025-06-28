package com.mobile2.uas_elsid.ui.profile;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.OrderHistoryAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.ApiService;
import com.mobile2.uas_elsid.api.response.OrderResponse;
import com.mobile2.uas_elsid.api.response.ReviewResponse;
import com.mobile2.uas_elsid.databinding.FragmentOrderHistoryBinding;
import com.mobile2.uas_elsid.model.Order;
import com.mobile2.uas_elsid.model.OrderItem;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryFragment extends Fragment implements OrderHistoryAdapter.OnOrderActionListener {
    private FragmentOrderHistoryBinding binding;
    private OrderHistoryAdapter adapter;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderHistoryBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        binding.backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        setupRecyclerView();
        loadOrderHistory();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        adapter = new OrderHistoryAdapter(requireContext(), this);
        adapter.setOnItemClickListener(order -> {
            // Navigasi ke OrderDetailFragment dengan mengirim order ID
            Bundle bundle = new Bundle();
            bundle.putInt("order_id", order.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_navigation_order_history_to_navigation_order_detail, bundle);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    // Implement required methods from OnOrderActionListener
    @Override
    public void onReorder(Order order) {
        Map<String, Object> reorderData = new HashMap<>();
        reorderData.put("order_id", order.getId());
        reorderData.put("user_id", sessionManager.getUserId());

        ApiClient.getClient().reorderItems(reorderData).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (!isAdded() || binding == null) {
                    return;
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_navigation_order_history_to_navigation_checkout);
                } else {
                    showError("Failed to reorder items");
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) {
                    return;
                }
                
                showError("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void onWriteReview(Order order) {
        // Create bundle to pass order details
        Bundle bundle = new Bundle();
        bundle.putInt("orderId", order.getId());

        // If there's only one item in the order, pass it directly
        if (order.getItems().size() == 1) {
            OrderItem item = order.getItems().get(0);
            bundle.putInt("productId", item.getProductId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_navigation_order_history_to_navigation_write_review, bundle);
        } else {
            // If multiple items, show dialog to choose which product to review
            String[] productNames = new String[order.getItems().size()];
            int[] productIds = new int[order.getItems().size()];

            for (int i = 0; i < order.getItems().size(); i++) {
                OrderItem item = order.getItems().get(i);
                productNames[i] = item.getProduct().getTitle();
                productIds[i] = item.getProductId();
            }

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Choose Product to Review")
                    .setItems(productNames, (dialog, which) -> {
                        bundle.putInt("productId", productIds[which]);
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_navigation_order_history_to_navigation_write_review, bundle);
                    })
                    .show();
        }
    }

    private void loadOrderHistory() {
        System.out.println("Loading order history for user: " + sessionManager.getUserId());

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyView.setVisibility(View.GONE);

        ApiClient.getClient().getUserOrders(sessionManager.getUserId())
                .enqueue(new Callback<OrderResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                        if (!isAdded() || binding == null) {
                            return;
                        }
                        
                        binding.progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Order> orders = response.body().getOrders();

                            if (orders != null && !orders.isEmpty()) {
                                // Sort orders by ID in descending order (newest first)
                                Collections.sort(orders, (o1, o2) -> Integer.compare(o2.getId(), o1.getId()));

                                adapter.setOrders(orders);
                                binding.recyclerView.setVisibility(View.VISIBLE);
                                binding.emptyView.setVisibility(View.GONE);
                            } else {
                                showEmptyState();
                            }
                        } else {
                            showError("Failed to load order history");
                            showEmptyState();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                        if (!isAdded() || binding == null) {
                            return;
                        }
                        
                        binding.progressBar.setVisibility(View.GONE);
                        showError("Network error: " + t.getMessage());
                        showEmptyState();
                    }
                });
    }

    private void showEmptyState() {
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyView.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
