package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RatingBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.ApiService;
import com.mobile2.uas_elsid.api.response.WishlistCheckResponse;
import com.mobile2.uas_elsid.api.response.WishlistResponse;
import com.mobile2.uas_elsid.api.response.ReviewResponse;
import com.mobile2.uas_elsid.model.Product;
import com.mobile2.uas_elsid.model.ProductReview;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private final Context context;
    private List<Product> products = new ArrayList<>();
    private ApiService apiService;
    private SessionManager sessionManager;

    public ProductAdapter(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
        this.apiService = ApiClient.getClient();
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private OnProductClickListener listener;

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);

        holder.titleText.setText(product.getTitle());
        holder.categoryText.setText(product.getCategory());
        holder.viewCountText.setText(String.format(Locale.getDefault(), "%d views", product.getViewCount()));

        if (product.getPurchaseCount() > 0) {
            holder.purchaseCountText.setText(String.format("%d sold", product.getPurchaseCount()));
            holder.purchaseCountText.setVisibility(View.VISIBLE);
        } else {
            holder.purchaseCountText.setVisibility(View.GONE);
        }

        // Price formatting
        if (product.getDiscount() > 0) {
            int discountedPrice = product.getPrice() - (product.getPrice() * product.getDiscount() / 100);
            holder.priceText.setText(formatPrice(discountedPrice));
            holder.originalPriceText.setText(formatPrice(product.getPrice()));
            holder.originalPriceText.setPaintFlags(holder.originalPriceText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.originalPriceText.setVisibility(View.VISIBLE);
            holder.discountText.setText(String.format("-%d%%", product.getDiscount()));
            holder.discountText.setVisibility(View.VISIBLE);
        } else {
            holder.priceText.setText(formatPrice(product.getPrice()));
            holder.originalPriceText.setVisibility(View.GONE);
            holder.discountText.setVisibility(View.GONE);
        }

        // Image loading
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            String imageUrl = "https://apilumenmobileuas.ndp.my.id/" + product.getImages().get(0).getImageUrl();
            System.out.println("Debug - Image URL: " + imageUrl);
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.productImage);
        } else {
            System.out.println("Debug - No images for product: " + product.getId());
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }

        holder.soldOutLabel.setVisibility(product.getMainStock() == 0 ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });

        // Wishlist button setup
        setupWishlistButton(holder, product);

        // Load product reviews to calculate average rating
        apiService.getProductReviews(product.getId()).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewResponse> call, @NonNull Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getReviews() != null) {
                    List<ProductReview> reviews = response.body().getReviews();
                    if (!reviews.isEmpty()) {
                        float totalRating = 0;
                        for (ProductReview review : reviews) {
                            totalRating += review.getRating();
                        }
                        float averageRating = totalRating / reviews.size();

                        holder.ratingBar.setRating(averageRating);
                        holder.ratingText.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
                        holder.ratingBar.setVisibility(View.VISIBLE);
                        holder.ratingText.setVisibility(View.VISIBLE);
                    } else {
                        holder.ratingBar.setVisibility(View.GONE);
                        holder.ratingText.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReviewResponse> call, @NonNull Throwable t) {
                holder.ratingBar.setVisibility(View.GONE);
                holder.ratingText.setVisibility(View.GONE);
            }
        });
    }

    private void setupWishlistButton(@NonNull ViewHolder holder, Product product) {
        if (!sessionManager.isLoggedIn()) {
            holder.wishlistButton.setVisibility(View.GONE);
            return;
        }

        holder.wishlistButton.setVisibility(View.VISIBLE);

        apiService.checkWishlist(sessionManager.getUserId(), product.getId())
                .enqueue(new Callback<WishlistCheckResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WishlistCheckResponse> call,
                                           @NonNull Response<WishlistCheckResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            boolean isInWishlist = response.body().isExists();
                            updateWishlistButtonState(holder.wishlistButton, isInWishlist);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WishlistCheckResponse> call, @NonNull Throwable t) {
                        // Handle error silently
                    }
                });

        holder.wishlistButton.setOnClickListener(v -> toggleWishlist(holder.wishlistButton, product));
    }

    private void updateWishlistButtonState(FloatingActionButton button, boolean isInWishlist) {
        if (isInWishlist) {
            button.setImageResource(R.drawable.ic_favorite);
            button.setColorFilter(ContextCompat.getColor(context, R.color.error));
        } else {
            button.setImageResource(R.drawable.ic_favorite_border);
            button.setColorFilter(ContextCompat.getColor(context, R.color.wishlist_icon_inactive));
        }
    }

    private void toggleWishlist(FloatingActionButton button, Product product) {
        if (!sessionManager.isLoggedIn()) {
            return;
        }

        String userId = sessionManager.getUserId();
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", userId);
        request.put("product_id", product.getId());

        apiService.checkWishlist(userId, product.getId())
                .enqueue(new Callback<WishlistCheckResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WishlistCheckResponse> call,
                                           @NonNull Response<WishlistCheckResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            boolean isInWishlist = response.body().isExists();

                            if (isInWishlist) {
                                removeFromWishlist(button, userId, product.getId());
                            } else {
                                addToWishlist(button, userId, product.getId());
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WishlistCheckResponse> call, @NonNull Throwable t) {
                        Toasty.error(context, "Network error").show();
                    }
                });
    }

    private void addToWishlist(FloatingActionButton button, String userId, int productId) {
        Map<String, Integer> request = new HashMap<>();
        request.put("product_id", productId);

        apiService.addToWishlist(userId, request)
                .enqueue(new Callback<WishlistResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WishlistResponse> call,
                                           @NonNull Response<WishlistResponse> response) {
                        if (response.isSuccessful()) {
                            updateWishlistButtonState(button, true);
                            Toasty.success(context, "Added to wishlist").show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WishlistResponse> call, @NonNull Throwable t) {
                        Toasty.error(context, "Failed to add to wishlist").show();
                    }
                });
    }

    private void removeFromWishlist(FloatingActionButton button, String userId, int productId) {
        apiService.removeFromWishlist(userId, productId)
                .enqueue(new Callback<WishlistResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WishlistResponse> call,
                                           @NonNull Response<WishlistResponse> response) {
                        if (response.isSuccessful()) {
                            updateWishlistButtonState(button, false);
                            Toasty.success(context, "Removed from wishlist").show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WishlistResponse> call, @NonNull Throwable t) {
                        Toasty.error(context, "Failed to remove from wishlist").show();
                    }
                });
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void setProducts(List<Product> newProducts) {
        this.products.clear();
        this.products.addAll(newProducts);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView titleText, categoryText, viewCountText, priceText;
        TextView originalPriceText, discountText, soldOutLabel;
        TextView purchaseCountText, ratingText;
        RatingBar ratingBar;
        FloatingActionButton wishlistButton;

        ViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            viewCountText = itemView.findViewById(R.id.viewCountText);
            purchaseCountText = itemView.findViewById(R.id.purchaseCountText);
            priceText = itemView.findViewById(R.id.priceText);
            originalPriceText = itemView.findViewById(R.id.originalPriceText);
            discountText = itemView.findViewById(R.id.discountText);
            soldOutLabel = itemView.findViewById(R.id.soldOutLabel);
            wishlistButton = itemView.findViewById(R.id.wishlistButton);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            ratingText = itemView.findViewById(R.id.ratingText);
        }
    }
}
