package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Product {
    @SerializedName("id")
    private int id;
    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;
    @SerializedName("category")
    private String category;
    @SerializedName("price")
    private int price;
    @SerializedName("discount")
    private int discount;
    @SerializedName("main_stock")
    private int mainStock;
    @SerializedName("weight")
    private int weight;
    @SerializedName("status")
    private String status;
    @SerializedName("has_variants")
    private boolean hasVariants;
    @SerializedName("purchase_count")
    private int purchaseCount;
    @SerializedName("view_count")
    private int viewCount;
    @SerializedName("images")
    private List<ProductImage> images;
    @SerializedName("variants")
    private List<ProductVariant> variants;
    @SerializedName("created_at")
    private String createdAt;

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getPrice() { return price; }
    public int getDiscount() { return discount; }
    public int getMainStock() { return mainStock; }
    public int getWeight() { return weight; }
    public String getStatus() { return status; }
    public boolean hasVariants() { return hasVariants; }
    public int getPurchaseCount() { return purchaseCount; }
    public int getViewCount() { return viewCount; }
    public List<ProductImage> getImages() { return images; }
    public List<ProductVariant> getVariants() { return variants; }
    public String getCreatedAt() { return createdAt; }
}