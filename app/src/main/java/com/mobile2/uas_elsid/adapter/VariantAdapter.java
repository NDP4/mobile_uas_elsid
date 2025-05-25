package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.model.ProductVariant;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VariantAdapter extends RecyclerView.Adapter<VariantAdapter.ViewHolder> {
    private List<ProductVariant> variants = new ArrayList<>();
    private int selectedPosition = -1;
    private OnVariantSelectedListener listener;
    private Context context;

    public interface OnVariantSelectedListener {
        void onVariantSelected(ProductVariant variant);
    }

    public VariantAdapter(Context context, OnVariantSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_variant_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductVariant variant = variants.get(position);
        MaterialCardView card = (MaterialCardView) holder.itemView;

        // Set variant details
        holder.nameText.setText(variant.getVariantName());
        holder.priceText.setText(formatPrice(calculateFinalPrice(variant)));
        holder.stockText.setText(String.format("%d tersisa", variant.getStock()));

        // Update card appearance
        card.setStrokeColor(ContextCompat.getColor(context,
                position == selectedPosition ? R.color.primary : R.color.surface));
        card.setCardBackgroundColor(ContextCompat.getColor(context,
                position == selectedPosition ? R.color.surface : android.R.color.white));
        card.setChecked(position == selectedPosition);

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            if (variant.getStock() > 0) {
                int oldPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
                listener.onVariantSelected(variant);
            }
        });

        // Disable card if out of stock
        card.setEnabled(variant.getStock() > 0);
        if (variant.getStock() <= 0) {
            holder.stockText.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.stockText.setText("Stok habis");
            card.setAlpha(0.5f);
        } else {
            holder.stockText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            card.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return variants.size();
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
        notifyDataSetChanged();
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Remove ",00"
    }

    private int calculateFinalPrice(ProductVariant variant) {
        return variant.getPrice() - (variant.getPrice() * variant.getDiscount() / 100);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView priceText;
        TextView stockText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.variantNameText);
            priceText = itemView.findViewById(R.id.variantPriceText);
            stockText = itemView.findViewById(R.id.variantStockText);
        }
    }
}
