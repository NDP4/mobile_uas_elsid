package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.ProductReview;
import java.util.List;

public class ReviewResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("reviews")
    private List<ProductReview> reviews;

    public int isStatus() { return status; }
    public String getMessage() { return message; }
    public List<ProductReview> getReviews() { return reviews; }
}