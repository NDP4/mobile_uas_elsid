package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.model.ProductReview;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private List<ProductReview> reviews = new ArrayList<>();
    private final Context context;

    public ReviewAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductReview review = reviews.get(position);

        // Set user info
        holder.userNameText.setText(review.getUser().getFullname());
        if (review.getUser().getAvatar() != null && !review.getUser().getAvatar().isEmpty()) {
            Glide.with(context)
                    .load("https://apilumenmobileuas.ndp.my.id/" + review.getUser().getAvatar())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(holder.userAvatar);
        }

        // Set review content
        holder.ratingBar.setRating(review.getRating());
        holder.reviewText.setText(review.getReview());
        holder.dateText.setText(formatDate(review.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public void setReviews(List<ProductReview> reviews) {
        this.reviews = reviews;
        System.out.println("Setting " + reviews.size() + " reviews in adapter");
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar;
        TextView userNameText, reviewText, dateText;
        RatingBar ratingBar;

        ViewHolder(View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userNameText = itemView.findViewById(R.id.userNameText);
            reviewText = itemView.findViewById(R.id.reviewText);
            dateText = itemView.findViewById(R.id.dateText);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            serverFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = serverFormat.parse(dateString);
            return date != null ? displayFormat.format(date) : dateString;
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }
}