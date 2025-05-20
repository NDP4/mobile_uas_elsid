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
import com.mobile2.uas_elsid.model.CartItem;
import com.mobile2.uas_elsid.model.ProductImage;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailPesananAdapter extends RecyclerView.Adapter<DetailPesananAdapter.ViewHolder> {
    private final Context context;
    private List<CartItem> items = new ArrayList<>();

    public DetailPesananAdapter(Context context) {
        this.context = context;
    }

    public List<CartItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_detail_pesanan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = items.get(position);

        // Set product title
        holder.titleText.setText(item.getProduct().getTitle());

        // Set variant if exists
        if (item.getVariant() != null) {
            holder.variantText.setText(item.getVariant().getVariantName());
            holder.variantText.setVisibility(View.VISIBLE);
        } else {
            holder.variantText.setVisibility(View.GONE);
        }

        // Set quantity
        holder.quantityText.setText(String.format("Qty: %d", item.getQuantity()));

        // Calculate and set price
        int finalPrice;
        if (item.getVariant() != null) {
            finalPrice = calculatePrice(
                    item.getVariant().getPrice(),
                    item.getVariant().getDiscount(),
                    item.getQuantity()
            );
        } else {
            finalPrice = calculatePrice(
                    item.getProduct().getPrice(),
                    item.getProduct().getDiscount(),
                    item.getQuantity()
            );
        }
        holder.priceText.setText(formatPrice(finalPrice));

        // Load image
        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            ProductImage firstImage = item.getProduct().getImages().get(0);
            String imageUrl = "https://apilumenmobileuas.ndp.my.id/" + firstImage.getImageUrl();

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.productImage);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<CartItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    private int calculatePrice(int basePrice, int discount, int quantity) {
        int discountedPrice = basePrice - (basePrice * discount / 100);
        return discountedPrice * quantity;
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Remove ",00"
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView titleText;
        TextView variantText;
        TextView quantityText;
        TextView priceText;

        ViewHolder(View view) {
            super(view);
            productImage = view.findViewById(R.id.productImage);
            titleText = view.findViewById(R.id.titleText);
            variantText = view.findViewById(R.id.variantText);
            quantityText = view.findViewById(R.id.quantityText);
            priceText = view.findViewById(R.id.priceText);
        }
    }
}