package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

public class Wishlist {
    @SerializedName("id")
    private int id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("product_id")
    private int productId;
    @SerializedName("product")
    private Product product;

    // Getters
    public int getId() { return id; }
    public String getUserId() { return userId; }
    public int getProductId() { return productId; }
    public Product getProduct() { return product; }
}