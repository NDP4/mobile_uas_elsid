package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.model.Product;
import com.mobile2.uas_elsid.model.ProductImage;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private final Context context;
    private List<Product> products = new ArrayList<>();

    public ProductAdapter(Context context) {
        this.context = context;
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

        // Set product title
        holder.titleText.setText(product.getTitle());

        // Set category
        holder.categoryText.setText(product.getCategory());

        // Set view count
        holder.viewCountText.setText(String.format(Locale.getDefault(), "%d views", product.getViewCount()));

        // Format price in Indonesian Rupiah
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        holder.priceText.setText(rupiahFormat.format(product.getPrice()));

        // Handle discount if any
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

        // Load product image
        if (!product.getImages().isEmpty()) {
            ProductImage firstImage = product.getImages().get(0);
            String imageUrl = "https://apilumenmobileuas.ndp.my.id/" + firstImage.getImageUrl();
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.productImage);
        }

        // Show sold out label if stock is 0
        holder.soldOutLabel.setVisibility(product.getMainStock() == 0 ? View.VISIBLE : View.GONE);

        // Set click listener for the item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    public void bindPurchaseCount(Product product, TextView purchaseCountBadge) {
        if (purchaseCountBadge != null && product.getPurchaseCount() > 0) {
            purchaseCountBadge.setVisibility(View.VISIBLE);
            purchaseCountBadge.setText(product.getPurchaseCount() + " terjual");
        } else {
            purchaseCountBadge.setVisibility(View.GONE);
        }
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Hapus ",00" di akhir
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

        ViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            titleText = itemView.findViewById(R.id.titleText);
            categoryText = itemView.findViewById(R.id.categoryText);
            viewCountText = itemView.findViewById(R.id.viewCountText);
            priceText = itemView.findViewById(R.id.priceText);
            originalPriceText = itemView.findViewById(R.id.originalPriceText);
            discountText = itemView.findViewById(R.id.discountText);
            soldOutLabel = itemView.findViewById(R.id.soldOutLabel);
        }
    }
}