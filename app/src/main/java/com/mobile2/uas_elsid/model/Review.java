package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

public class Review {
    @SerializedName("id")
    private int id;

    @SerializedName("product_id")
    private int productId;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("rating")
    private int rating;

    @SerializedName("review")
    private String review;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("user")
    private User user;

    // Getters
    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getUserId() { return userId; }
    public int getRating() { return rating; }
    public String getReview() { return review; }
    public String getCreatedAt() { return createdAt; }
    public User getUser() { return user; }
}