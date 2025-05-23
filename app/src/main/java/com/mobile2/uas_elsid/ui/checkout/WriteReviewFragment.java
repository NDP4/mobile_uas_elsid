package com.mobile2.uas_elsid.ui.checkout;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.ReviewResponse;
import com.mobile2.uas_elsid.databinding.FragmentWriteReviewBinding;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WriteReviewFragment extends Fragment {
    private FragmentWriteReviewBinding binding;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWriteReviewBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        // Get arguments
        int orderId = getArguments().getInt("orderId");
        int productId = getArguments().getInt("productId");

        binding.submitButton.setOnClickListener(v -> {
            // Convert rating to integer (round to nearest whole number)
            int rating = Math.round(binding.ratingBar.getRating());
            String review = binding.reviewEditText.getText().toString();

            if (review.isEmpty()) {
                showError("Please write your review");
                return;
            }

            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("user_id", sessionManager.getUserId());
            reviewData.put("product_id", productId);
            reviewData.put("order_id", orderId);
            reviewData.put("rating", rating);
            reviewData.put("review", review);

            submitReview(reviewData);
        });

        return binding.getRoot();
    }

    private void submitReview(Map<String, Object> reviewData) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.submitButton.setEnabled(false);

        ApiClient.getClient().addReview(reviewData).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call, @NonNull Response<ReviewResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.submitButton.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Toasty.success(requireContext(), "Review submitted successfully", Toast.LENGTH_SHORT, true).show();
                    Navigation.findNavController(requireView()).popBackStack();
                } else {
                    showError("Failed to submit review");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.submitButton.setEnabled(true);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        Toasty.error(requireContext(), message, Toast.LENGTH_LONG, true).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}