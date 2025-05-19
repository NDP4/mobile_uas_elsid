package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

public class ProductImage {
    @SerializedName("id")
    private int id;
    @SerializedName("product_id")
    private int productId;
    @SerializedName("image_url")
    private String imageUrl;
    @SerializedName("image_order")
    private int imageOrder;

    // Getters
    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getImageUrl() { return imageUrl; }
    public int getImageOrder() { return imageOrder; }
}