package com.mobile2.uas_elsid.ui.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayoutMediator;
import com.mobile2.uas_elsid.LoginActivity;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.ReviewAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.ProductDetailResponse;
import com.mobile2.uas_elsid.api.response.ProductResponse;
import com.mobile2.uas_elsid.api.response.ReviewResponse;
import com.mobile2.uas_elsid.api.response.WishlistCheckResponse;
import com.mobile2.uas_elsid.api.response.WishlistResponse;
import com.mobile2.uas_elsid.databinding.FragmentProductDetailBinding;
import com.mobile2.uas_elsid.model.CartItem;
import com.mobile2.uas_elsid.model.Product;
import com.mobile2.uas_elsid.model.ProductReview;
import com.mobile2.uas_elsid.model.ProductVariant;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mobile2.uas_elsid.adapter.ImageSliderAdapter;
import com.mobile2.uas_elsid.utils.CartManager;
import com.mobile2.uas_elsid.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import es.dmoral.toasty.Toasty;



public class ProductDetailFragment extends Fragment {
    private FragmentProductDetailBinding binding;
    private Product currentProduct;
    private ImageSliderAdapter imageSliderAdapter;
    private ReviewAdapter reviewAdapter;
    private ProductVariant selectedVariant;
    private SessionManager sessionManager;
    private FloatingActionButton wishlistButton;
    private boolean isInWishlist = false;
    private int productId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        // Get product ID from arguments
        if (getArguments() != null) {
            int productId = getArguments().getInt("product_id");
            loadProductDetails(productId);
        }
        // Check wishlist status if user is logged in
        if (sessionManager.isLoggedIn()) {
            checkWishlistStatus();
        }

        wishlistButton = binding.wishlistButton;
        setupWishlistButton();

        reviewAdapter = new ReviewAdapter(requireContext());
        binding.reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.reviewsRecyclerView.setAdapter(reviewAdapter);

        setupImageSlider();
        setupReviews();
        setupAddToCart();
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
    private void setupWishlistButton() {
        if (!sessionManager.isLoggedIn()) {
            wishlistButton.setVisibility(View.GONE);
            return;
        }

        wishlistButton.setVisibility(View.VISIBLE);

        if (currentProduct != null) {
            // Check if product is in wishlist
            ApiClient.getClient().checkWishlist(sessionManager.getUserId(), currentProduct.getId())
                    .enqueue(new Callback<WishlistCheckResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<WishlistCheckResponse> call,
                                               @NonNull Response<WishlistCheckResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                isInWishlist = response.body().isExists();
                                updateWishlistButtonState();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<WishlistCheckResponse> call, @NonNull Throwable t) {
                            Toasty.error(requireContext(), "Failed to check wishlist status").show();
                        }
                    });
        }

        wishlistButton.setOnClickListener(v -> toggleWishlist());
    }
    private void updateWishlistButtonState() {
        if (isInWishlist) {
            wishlistButton.setImageResource(R.drawable.ic_favorite);
            wishlistButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error));
        } else {
            wishlistButton.setImageResource(R.drawable.ic_favorite_border);
            wishlistButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.wishlist_icon_inactive));
        }
    }
    private void toggleWishlist() {
        if (!sessionManager.isLoggedIn()) {
            showLoginDialog();
            return;
        }

        if (currentProduct == null) return;

        if (isInWishlist) {
            removeFromWishlist();
        } else {
            addToWishlist();
        }
    }
    private void showLoginDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Login Required")
                .setMessage("Please login to add items to your wishlist")
                .setPositiveButton("Login", (dialog, which) -> {
                    startActivity(new Intent(requireActivity(), LoginActivity.class));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkWishlistStatus() {
        System.out.println("Debug - Checking wishlist for userId: " + sessionManager.getUserId() + ", productId: " + productId);

        ApiClient.getClient().checkWishlist(sessionManager.getUserId(), productId)
                .enqueue(new Callback<WishlistCheckResponse>() {
                    @Override
                    public void onResponse(Call<WishlistCheckResponse> call, Response<WishlistCheckResponse> response) {
                        System.out.println("Debug - Wishlist check response: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            isInWishlist = response.body().isExists();
                            updateWishlistButton();
                        }
                    }

                    @Override
                    public void onFailure(Call<WishlistCheckResponse> call, Throwable t) {
                        // Handle error
                    }
                });
    }

    private void updateWishlistButton() {
        int iconResource = isInWishlist ?
                R.drawable.ic_favorite :
                R.drawable.ic_favorite_border;
        wishlistButton.setImageResource(iconResource);
    }

    private void addToWishlist() {
        Map<String, Integer> request = new HashMap<>();
        request.put("product_id", currentProduct.getId());

        ApiClient.getClient().addToWishlist(sessionManager.getUserId(), request)
                .enqueue(new Callback<WishlistResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WishlistResponse> call,
                                           @NonNull Response<WishlistResponse> response) {
                        if (response.isSuccessful()) {
                            isInWishlist = true;
                            updateWishlistButtonState();
                            Toasty.success(requireContext(), "Added to wishlist").show();
                        } else {
                            Toasty.error(requireContext(), "Failed to add to wishlist").show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WishlistResponse> call, @NonNull Throwable t) {
                        Toasty.error(requireContext(), "Network error").show();
                    }
                });
    }

    private void removeFromWishlist() {
        ApiClient.getClient().removeFromWishlist(sessionManager.getUserId(), currentProduct.getId())
                .enqueue(new Callback<WishlistResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WishlistResponse> call,
                                           @NonNull Response<WishlistResponse> response) {
                        if (response.isSuccessful()) {
                            isInWishlist = false;
                            updateWishlistButtonState();
                            Toasty.success(requireContext(), "Removed from wishlist").show();
                        } else {
                            Toasty.error(requireContext(), "Failed to remove from wishlist").show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WishlistResponse> call, @NonNull Throwable t) {
                        Toasty.error(requireContext(), "Network error").show();
                    }
                });
    }

    private void loadProductDetails(int productId) {
        ApiClient.getClient().getProduct(productId).enqueue(new Callback<ProductDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductDetailResponse> call,
                                   @NonNull Response<ProductDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Product product = response.body().getProduct();
                    if (product != null) {
                        // Log debugging
                        System.out.println("Product ID: " + product.getId());
                        System.out.println("Has Variants: " + product.hasVariants());
                        System.out.println("Variants List: " +
                                (product.getVariants() != null ? product.getVariants().size() : "null"));

                        currentProduct = product;
                        updateUI(product);
                        setupProductDetails(product);
                        updateViewCount(productId);
                        loadReviews(productId);
                        setupWishlistButton();
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

    private void updateViewCount(int productId) {
        Map<String, Integer> body = new HashMap<>();
        body.put("product_id", productId);

        ApiClient.getClient().updateProductViewCount(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    // View count updated successfully
                    // Update the UI if needed
                    if (currentProduct != null) {
                        currentProduct.setViewCount(currentProduct.getViewCount() + 1);
                        binding.viewCountText.setText(String.format("%d views", currentProduct.getViewCount()));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Silent fail - don't show error to user since this is not critical
                System.out.println("Failed to update view count: " + t.getMessage());
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

        if (product.getPurchaseCount() > 0) {
            binding.purchaseCountText.setText(String.format("%d sold", product.getPurchaseCount()));
            binding.purchaseCountText.setVisibility(View.VISIBLE);
        } else {
            binding.purchaseCountText.setVisibility(View.GONE);
        }

        // initial price calculation
        int finalPrice = calculateFinalPrice(product);
        binding.finalPriceText.setText(formatPrice(finalPrice));

        // set up varian if available
        setupVariantsView(product);

        // Handle variants if any
        if (product.hasVariants() && product.getVariants() != null) {
            setupVariants(product.getVariants());
        } else {
            // Handle non-variant product price and stock
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
            binding.stockText.setText(String.format("Stock: %d", product.getMainStock()));
            binding.addToCartButton.setEnabled(product.getMainStock() > 0);
            binding.variantsLayout.setVisibility(View.GONE);
        }

        // Update image slider
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageSliderAdapter.setImages(product.getImages());
        }
    }

    private void setupVariants(List<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            binding.variantsLayout.setVisibility(View.GONE);
            return;
        }

        binding.variantsLayout.setVisibility(View.VISIBLE);
        binding.variantsGroup.removeAllViews();

        // Create radio buttons for each variant
        for (ProductVariant variant : variants) {
            RadioButton radioButton = new RadioButton(requireContext());
            radioButton.setText(getVariantText(variant));
            radioButton.setTag(variant);
            binding.variantsGroup.addView(radioButton);
        }

        // Handle variant selection
        binding.variantsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedButton = group.findViewById(checkedId);
            if (selectedButton != null) {
                selectedVariant = (ProductVariant) selectedButton.getTag();
                updatePriceAndStock(selectedVariant);
            }
        });

        // Select first variant by default
        if (!variants.isEmpty()) {
            RadioButton firstButton = (RadioButton) binding.variantsGroup.getChildAt(0);
            firstButton.setChecked(true);
        }
    }
    private void updatePriceAndStock(ProductVariant variant) {
        // Update price display
        if (variant.getDiscount() > 0) {
            int discountedPrice = variant.getPrice() -
                    (variant.getPrice() * variant.getDiscount() / 100);
            binding.mainPriceText.setText(formatPrice(discountedPrice));
            binding.originalPriceText.setText(formatPrice(variant.getPrice()));
            binding.originalPriceText.setVisibility(View.VISIBLE);
            binding.discountText.setText(String.format("-%d%%", variant.getDiscount()));
            binding.discountText.setVisibility(View.VISIBLE);
        } else {
            binding.mainPriceText.setText(formatPrice(variant.getPrice()));
            binding.originalPriceText.setVisibility(View.GONE);
            binding.discountText.setVisibility(View.GONE);
        }

        // Update stock status
        binding.stockText.setText(String.format("Stock: %d", variant.getStock()));

        // Update add to cart button state
        binding.addToCartButton.setEnabled(variant.getStock() > 0);

        // Update final price
        int finalPrice = calculateFinalPrice(variant);
        binding.finalPriceText.setText(formatPrice(finalPrice));
    }
    private String getVariantText(ProductVariant variant) {
        StringBuilder text = new StringBuilder(variant.getVariantName());
        if (variant.getStock() <= 0) {
            text.append(" (Out of Stock)");
        }
        return text.toString();
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
        // If there's a selected variant, use its price and discount
        if (selectedVariant != null) {
            return selectedVariant.getPrice() -
                    (selectedVariant.getPrice() * selectedVariant.getDiscount() / 100);
        }

        // Otherwise use the main product's price and discount
        return product.getPrice() - (product.getPrice() * product.getDiscount() / 100);
    }

    private int calculateFinalPrice(ProductVariant variant) {
        return variant.getPrice() - (variant.getPrice() * variant.getDiscount() / 100);
    }


    // Update UI when variant is selected
    private void onVariantSelected(ProductVariant variant) {
        selectedVariant = variant;

        // Update price display
        int finalPrice = calculateFinalPrice(variant);
        binding.finalPriceText.setText(formatPrice(finalPrice));

        // Update stock status
        binding.stockText.setText(String.format("Stock: %d", variant.getStock()));

        // Enable/disable add to cart button based on stock
        binding.addToCartButton.setEnabled(variant.getStock() > 0);
    }

    private void setupVariantsView(Product product) {
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            binding.variantsLayout.setVisibility(View.VISIBLE);
            binding.variantsLabel.setVisibility(View.VISIBLE);

            RadioGroup variantsGroup = binding.variantsGroup;
            variantsGroup.removeAllViews();

            for (ProductVariant variant : product.getVariants()) {
                RadioButton radioButton = new RadioButton(requireContext());
                radioButton.setText(String.format("%s - %s (%d in stock)",
                        variant.getVariantName(),
                        formatPrice(calculateFinalPrice(variant)),
                        variant.getStock()));
                radioButton.setTag(variant);

                radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        onVariantSelected(variant);
                    }
                });

                variantsGroup.addView(radioButton);
            }
        } else {
            binding.variantsLayout.setVisibility(View.GONE);
            binding.variantsLabel.setVisibility(View.GONE);
        }
    }

    private void setupAddToCart() {
        binding.addToCartButton.setOnClickListener(v -> {
//            if (!sessionManager.isLoggedIn()) {
//                Toasty.warning(requireContext(), "Please login to add items to cart").show();
//                return;
//            }
            if (!sessionManager.isLoggedIn()) {
                // Show login prompt dialog instead of toast
                new AlertDialog.Builder(requireContext())
                        .setTitle("Login Required")
                        .setMessage("Please login to add items to cart. Would you like to login now?")
                        .setPositiveButton("Login", (dialog, which) -> {
                            // Navigate to login
                            startActivity(new Intent(requireActivity(), LoginActivity.class));
                        })
                        .setNegativeButton("Continue as Guest", (dialog, which) -> {
                            // Allow viewing but not adding to cart
                            dialog.dismiss();
                        })
                        .show();
                return;
            }

            if (currentProduct == null) {
                Toasty.error(requireContext(), "Product not found").show();
                return;
            }

            // Check if product has variants but none selected
            if (currentProduct.hasVariants() && selectedVariant == null) {
                Toasty.warning(requireContext(), "Please select a product variant").show();
                return;
            }

            // Create cart item
            CartItem cartItem = new CartItem(currentProduct, selectedVariant, 1);

            // Add to cart
            CartManager.getInstance(requireContext()).addToCart(cartItem, new CartManager.CartCallback() {
                @Override
                public void onSuccess(List<CartItem> items) {
                    Toasty.success(requireContext(), "Added to cart").show();
                }

                @Override
                public void onError(String message) {
                    Toasty.error(requireContext(), message).show();
                }
            });
        });
    }

    private void loadReviews(int productId) {
        System.out.println("Starting to load reviews for product: " + productId);
        binding.reviewsSection.setVisibility(View.VISIBLE);

        ApiClient.getClient().getProductReviews(productId).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call, @NonNull Response<ReviewResponse> response) {
                System.out.println("Review response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    List<ProductReview> reviews = response.body().getReviews();
                    System.out.println("Number of reviews: " + (reviews != null ? reviews.size() : "null"));

                    if (reviews != null && !reviews.isEmpty()) {
                        reviewAdapter.setReviews(reviews);
                        calculateAverageRating(reviews);
                        binding.reviewsSection.setVisibility(View.VISIBLE);
                        System.out.println("Reviews populated and section made visible");
                    } else {
                        binding.reviewsSection.setVisibility(View.VISIBLE);
                        System.out.println("No reviews available");
                    }
                } else {
                    // Safe error body handling
                    String errorMessage = "Unknown error";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                            errorMessage = "Error reading error response";
                        }
                    }
                    System.out.println("Review response unsuccessful: " + errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                System.out.println("Review loading failed: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }
    private void setupReviews() {
        // Debug log
        System.out.println("Setting up reviews section");

        reviewAdapter = new ReviewAdapter(requireContext());
        binding.reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.reviewsRecyclerView.setAdapter(reviewAdapter);

        // Disable nested scrolling if inside NestedScrollView
        binding.reviewsRecyclerView.setNestedScrollingEnabled(false);

        // Add dividers between items
        binding.reviewsRecyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        );

        // Debug log
        System.out.println("Reviews section setup complete");
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