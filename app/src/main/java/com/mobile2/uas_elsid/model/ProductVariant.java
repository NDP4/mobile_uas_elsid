package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

public class ProductVariant {
    @SerializedName("id")
    private int id;
    @SerializedName("product_id")
    private int productId;
    @SerializedName("variant_name")
    private String variantName;
    @SerializedName("price")
    private int price;
    @SerializedName("discount")
    private int discount;
    @SerializedName("stock")
    private int stock;

    // Getters
    public int getId() { return id; }
    public int getProductId() { return productId; }
    public String getVariantName() { return variantName; }
    public int getPrice() { return price; }
    public int getDiscount() { return discount; }
    public int getStock() { return stock; }
}