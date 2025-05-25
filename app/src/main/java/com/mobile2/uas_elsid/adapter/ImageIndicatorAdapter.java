package com.mobile2.uas_elsid.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.mobile2.uas_elsid.R;

import java.util.ArrayList;
import java.util.List;

public class ImageIndicatorAdapter extends RecyclerView.Adapter<ImageIndicatorAdapter.ViewHolder> {
    private List<String> imageUrls;
    private int selectedPosition = 0;
    private ViewPager2 viewPager;

    public ImageIndicatorAdapter(ViewPager2 viewPager) {
        this.viewPager = viewPager;
        this.imageUrls = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_indicator, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        // Load thumbnail
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.thumbnailImage);

        // Highlight selected thumbnail
        MaterialCardView cardView = (MaterialCardView) holder.itemView;
        cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(),
                position == selectedPosition ? R.color.primary : R.color.indicator_stroke));

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            viewPager.setCurrentItem(position, true);
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public void setImages(List<String> urls) {
        this.imageUrls = urls;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImage;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
        }
    }
}