package com.mobile2.uas_elsid.ui.home;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.google.android.material.tabs.TabLayoutMediator;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.ReviewAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.ProductDetailResponse;
import com.mobile2.uas_elsid.api.response.ProductResponse;
import com.mobile2.uas_elsid.api.response.ReviewResponse;
import com.mobile2.uas_elsid.databinding.FragmentProductDetailBinding;
import com.mobile2.uas_elsid.model.Product;
import com.mobile2.uas_elsid.model.ProductReview;
import com.mobile2.uas_elsid.model.ProductVariant;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import com.mobile2.uas_elsid.adapter.ImageSliderAdapter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import es.dmoral.toasty.Toasty;



public class ProductDetailFragment extends Fragment {
    private FragmentProductDetailBinding binding;
    private Product currentProduct;
    private ImageSliderAdapter imageSliderAdapter;
    private ReviewAdapter reviewAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);

        // Get product ID from arguments
        if (getArguments() != null) {
            int productId = getArguments().getInt("product_id");
            loadProductDetails(productId);
        }

        reviewAdapter = new ReviewAdapter(requireContext());
        binding.reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.reviewsRecyclerView.setAdapter(reviewAdapter);

        setupImageSlider();
        return binding.getRoot();
    }

    private void setupImageSlider() {
        imageSliderAdapter = new ImageSliderAdapter(requireContext());
        binding.imageSlider.setAdapter(imageSliderAdapter);

        // Setup indicator
        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(
                binding.imageIndicator, binding.imageSlider, (tab, position) -> {});
        tabLayoutMediator.attach();
    }

    private void loadProductDetails(int productId) {
        ApiClient.getClient().getProduct(productId).enqueue(new Callback<ProductDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductDetailResponse> call,
                                   @NonNull Response<ProductDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Product product = response.body().getProduct();
                    if (product != null) {
                        currentProduct = product;
                        updateUI(product);
                        setupProductDetails(product);
                    } else {
                        showError("Product not found");
                    }
                } else {
                    showError("Failed to load product details");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductDetailResponse> call, @NonNull Throwable t) {
                showError("Failed to load product details: " + t.getMessage());
            }
        });
    }

    private void showError(String message) {
        if (binding != null) {
            binding.errorText.setText(message);
            binding.errorText.setVisibility(View.VISIBLE);
            binding.loadingIndicator.setVisibility(View.GONE);
        }
    }

    private void updateUI(Product product) {
        binding.titleText.setText(product.getTitle());
        binding.categoryText.setText(product.getCategory());
        binding.descriptionText.setText(product.getDescription());
        binding.viewCountText.setText(String.format("%d views", product.getViewCount()));

        // Handle price and discount
        if (product.getDiscount() > 0) {
            int discountedPrice = product.getPrice() -
                    (product.getPrice() * product.getDiscount() / 100);
            binding.mainPriceText.setText(formatPrice(discountedPrice));
            binding.originalPriceText.setText(formatPrice(product.getPrice()));
            binding.originalPriceText.setVisibility(View.VISIBLE);
            binding.discountText.setText(String.format("-%d%%", product.getDiscount()));
            binding.discountText.setVisibility(View.VISIBLE);
        } else {
            binding.mainPriceText.setText(formatPrice(product.getPrice()));
            binding.originalPriceText.setVisibility(View.GONE);
            binding.discountText.setVisibility(View.GONE);
        }

        // Set stock status
        binding.stockText.setText(String.format("Stock: %d", product.getMainStock()));

        // Update image slider
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageSliderAdapter.setImages(product.getImages());
        }

        // Handle variants if any
        if (product.hasVariants() && product.getVariants() != null) {
            setupVariants(product.getVariants());
        }
    }

    private void setupVariants(List<ProductVariant> variants) {
        binding.variantsLabel.setVisibility(View.VISIBLE);
        binding.variantsGroup.removeAllViews();

        for (ProductVariant variant : variants) {
            RadioButton radioButton = new RadioButton(requireContext());
            radioButton.setText(variant.getVariantName());
            radioButton.setTag(variant);
            binding.variantsGroup.addView(radioButton);
        }
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // mengHapus ",00" di akhir
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupProductDetails(Product product) {
        // Set initial description
        binding.descriptionText.setText(product.getDescription());
        binding.descriptionText.setMaxLines(3);

        // Setup see more button
        binding.seeMoreButton.setVisibility(View.VISIBLE);
        binding.seeMoreButton.setOnClickListener(v -> {
            if (binding.descriptionText.getMaxLines() == 3) {
                // Expand
                binding.descriptionText.setMaxLines(Integer.MAX_VALUE);
                binding.seeMoreButton.setText("See Less");
            } else {
                // Collapse
                binding.descriptionText.setMaxLines(3);
                binding.seeMoreButton.setText("See More");
            }
        });

        // Calculate and display total price
        int finalPrice = calculateFinalPrice(product);
        String formattedPrice = formatPrice(finalPrice);
        binding.finalPriceText.setText(formattedPrice);
    }

    private int calculateFinalPrice(Product product) {
        int basePrice = product.getPrice();
        int discount = product.getDiscount();
        if (discount > 0) {
            return basePrice - (basePrice * discount / 100);
        }
        return basePrice;
    }
    private void loadReviews(int productId) {
        ApiClient.getClient().getProductReviews(productId).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call, @NonNull Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductReview> reviews = response.body().getReviews();
                    if (reviews != null && !reviews.isEmpty()) {
                        reviewAdapter.setReviews(reviews);
                        calculateAverageRating(reviews);
                        binding.reviewsSection.setVisibility(View.VISIBLE);
                    } else {
                        binding.reviewsSection.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Failed to load reviews: " + t.getMessage(),
                        Toasty.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateAverageRating(List<ProductReview> reviews) {
        if (reviews.isEmpty()) return;

        float totalRating = 0;
        for (ProductReview review : reviews) {
            totalRating += review.getRating();
        }

        float averageRating = totalRating / reviews.size();
        binding.averageRatingText.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
        binding.averageRatingBar.setRating(averageRating);
        binding.totalReviewsText.setText(String.format(Locale.getDefault(),
                "Based on %d reviews", reviews.size()));
    }
}