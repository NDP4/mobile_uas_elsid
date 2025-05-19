package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

// Banner.java
public class Banner {
    @SerializedName("id")
    private int id;
    @SerializedName("title")
    private String title;
    @SerializedName("status")
    private String status;
    @SerializedName("images")
    private List<BannerImage> images;

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public List<BannerImage> getImages() { return images; }
}