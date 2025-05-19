package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

public class ProductReview {
    @SerializedName("id")
    private int id;
    @SerializedName("product_id")
    private int productId;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("rating")
    private float rating;
    @SerializedName("review")
    private String review;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("user")
    private User user;

    // Add getters
    public int getId() { return id; }
    public int getProductId() { return productId; }
    public float getRating() { return rating; }
    public String getReview() { return review; }
    public String getCreatedAt() { return createdAt; }
    public User getUser() { return user; }
}