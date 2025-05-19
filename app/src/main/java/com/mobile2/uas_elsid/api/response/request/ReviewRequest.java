package com.mobile2.uas_elsid.api.response.request;

import com.google.gson.annotations.SerializedName;

public class ReviewRequest {
    @SerializedName("product_id")
    private int productId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("rating")
    private float rating;

    @SerializedName("review")
    private String review;

    // Constructor
    public ReviewRequest(int productId, int userId, float rating, String review) {
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.review = review;
    }
}