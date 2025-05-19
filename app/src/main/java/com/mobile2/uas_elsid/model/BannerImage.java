package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

// BannerImage.java
public class BannerImage {
    @SerializedName("id")
    private int id;
    @SerializedName("banner_id")
    private int bannerId;
    @SerializedName("image_url")
    private String imageUrl;
    @SerializedName("image_order")
    private int imageOrder;

    // Getters
    public int getId() { return id; }
    public int getBannerId() { return bannerId; }
    public String getImageUrl() { return imageUrl; }
    public int getImageOrder() { return imageOrder; }
}