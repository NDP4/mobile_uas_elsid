package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mobile2.uas_elsid.model.Banner;

import java.util.ArrayList;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private List<Banner> banners = new ArrayList<>();
    private Context context;

    public BannerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new BannerViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = banners.get(position);
        if (banner.getImages() != null && !banner.getImages().isEmpty()) {
            String imageUrl = banner.getImages().get(0).getImageUrl();
            String fullUrl = imageUrl.startsWith("http") ?
                    imageUrl : "https://apilumenmobileuaslinux.ndp.my.id/" + imageUrl;

            Glide.with(context)
                    .load(fullUrl)
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    public void setBanners(List<Banner> banners) {
        this.banners = banners;
        notifyDataSetChanged();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        BannerViewHolder(ImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }
    }
}