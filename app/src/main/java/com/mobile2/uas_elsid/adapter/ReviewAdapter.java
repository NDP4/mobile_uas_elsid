package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductReview review = reviews.get(position);
        holder.userNameText.setText(review.getUser().getFullname());
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
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView userNameText;
        final RatingBar ratingBar;
        final TextView reviewText;
        final TextView dateText;

        ViewHolder(View itemView) {
            super(itemView);
            userNameText = itemView.findViewById(R.id.userNameText);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            reviewText = itemView.findViewById(R.id.reviewText);
            dateText = itemView.findViewById(R.id.dateText);
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