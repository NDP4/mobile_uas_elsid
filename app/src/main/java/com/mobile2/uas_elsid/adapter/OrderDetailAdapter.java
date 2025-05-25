package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.model.OrderItem;
import com.mobile2.uas_elsid.model.ProductImage;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.ViewHolder> {

    private final Context context;
    private List<OrderItem> items;
    private final NumberFormat rupiahFormat;
    private int discountAmount = 0;
    private String couponCode = null;

    public OrderDetailAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
        this.rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setDiscountInfo(int discountAmount, String couponCode) {
        this.discountAmount = discountAmount;
        this.couponCode = couponCode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_detail_pesanan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = items.get(position);

        // Load product image
        ProductImage firstImage = item.getProduct().getImages().get(0);
        String imageUrl = "https://apilumenmobileuas.ndp.my.id/" + firstImage.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.productImage);
        }

        // Set product title
        holder.titleText.setText(item.getProduct().getTitle());

        // Set variant if exists
        if (item.getVariant() != null) {
            holder.variantText.setText(item.getVariant().getVariantName());
            holder.variantText.setVisibility(View.VISIBLE);
        } else {
            holder.variantText.setVisibility(View.GONE);
        }

        // Set quantity and price
        holder.quantityText.setText(String.format("Qty: %d", item.getQuantity()));

        // Calculate and set price
        int price = calculatePrice(
            item.getVariant() != null ? item.getVariant().getPrice() : item.getProduct().getPrice(),
            item.getVariant() != null ? item.getVariant().getDiscount() : item.getProduct().getDiscount(),
            item.getQuantity()
        );
        holder.priceText.setText(formatPrice(price));

        // Show discount if applicable
        if (discountAmount > 0 && couponCode != null && holder.discountText != null) {
            holder.discountText.setVisibility(View.VISIBLE);
            holder.discountText.setText(String.format("-%s (Kupon: %s)",
                formatPrice(discountAmount), couponCode));
        } else if (holder.discountText != null) {
            holder.discountText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int calculatePrice(int basePrice, int discount, int quantity) {
        int discountedPrice = basePrice - (basePrice * discount / 100);
        return discountedPrice * quantity;
    }

    private String formatPrice(int price) {
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Remove ",00"
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView titleText, variantText, quantityText, priceText, discountText;

        ViewHolder(View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            titleText = itemView.findViewById(R.id.titleText);
            variantText = itemView.findViewById(R.id.variantText);
            quantityText = itemView.findViewById(R.id.quantityText);
            priceText = itemView.findViewById(R.id.priceText);
            discountText = itemView.findViewById(R.id.discountText);
        }
    }
}

