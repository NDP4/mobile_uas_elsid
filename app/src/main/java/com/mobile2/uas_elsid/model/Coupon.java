package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

public class Coupon {
    @SerializedName("id")
    private int id;

    @SerializedName("code")
    private String code;

    @SerializedName("description")
    private String description;

    @SerializedName("discount_amount")
    private double discountAmount;

    @SerializedName("discount_type")
    private String discountType;

    @SerializedName("min_purchase")
    private int minPurchase;

    // Getters
    public int getId() { return id; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public double getDiscountAmount() { return discountAmount; }
    public String getDiscountType() { return discountType; }
    public int getMinPurchase() { return minPurchase; }
}