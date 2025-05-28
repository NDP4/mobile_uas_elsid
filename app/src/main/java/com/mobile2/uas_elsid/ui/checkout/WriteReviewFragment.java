package com.mobile2.uas_elsid.ui.checkout;

import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.ProductDetailResponse;
import com.mobile2.uas_elsid.api.response.ReviewResponse;
import com.mobile2.uas_elsid.databinding.FragmentWriteReviewBinding;
import com.mobile2.uas_elsid.model.Product;
import com.mobile2.uas_elsid.model.ProductReview;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WriteReviewFragment extends Fragment {
    private FragmentWriteReviewBinding binding;
    private SessionManager sessionManager;
    private int orderId;
    private int productId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWriteReviewBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        // Setup back button dengan explicit navigation
        binding.backButton.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

        // Get arguments
        orderId = getArguments().getInt("orderId");
        productId = getArguments().getInt("productId");

        // Load product details
        loadProductDetails();

        // Check if user has already reviewed this product
        checkExistingReview();

        binding.backButton.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });

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

    private void loadProductDetails() {
        ApiClient.getClient().getProduct(productId).enqueue(new Callback<ProductDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductDetailResponse> call, @NonNull Response<ProductDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Product product = response.body().getProduct();
                    if (product != null) {
                        displayProductInfo(product);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductDetailResponse> call, @NonNull Throwable t) {
                showError("Failed to load product details");
            }
        });
    }

    private void displayProductInfo(Product product) {
        if (!isAdded()) return;  // Skip if fragment is not attached

        // Load product image
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            String imageUrl = product.getImages().get(0).getImageUrl();
            Context context = getContext();
            if (context != null) {  // Check if context is available
                Glide.with(context)
                        .load("https://apilumenmobileuas.ndp.my.id/" + imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(binding.productImage);
            }
        }

        // Set product title and price only if view is still available
        if (binding != null) {
            binding.productTitle.setText(product.getTitle());

            // Format and set price
            NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            String formattedPrice = rupiahFormat.format(product.getPrice()).replace(",00", "");
            binding.productPrice.setText(formattedPrice);
        }
    }

    private void checkExistingReview() {
        ApiClient.getClient().getProductReviews(productId).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call, @NonNull Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductReview> reviews = response.body().getReviews();
                    if (reviews != null) {
                        // Check if user has already reviewed this product
                        String userId = sessionManager.getUserId();
                        boolean hasReviewed = reviews.stream()
                                .anyMatch(review -> String.valueOf(review.getUser().getUserId()).equals(userId));

                        if (hasReviewed) {
                            showError("You have already reviewed this product");
                            binding.submitButton.setEnabled(false);
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                showError("Failed to check existing reviews");
            }
        });
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

