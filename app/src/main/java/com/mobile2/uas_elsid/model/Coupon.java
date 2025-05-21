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
    @SerializedName("usage_limit")
    private int usageLimit;

    @SerializedName("used_count")
    private int usedCount;

    @SerializedName("valid_from")
    private String validFrom;

    @SerializedName("valid_until")
    private String validUntil;

    @SerializedName("is_active")
    private boolean isActive;

    // Getters
    public int getId() { return id; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public double getDiscountAmount() { return discountAmount; }
    public String getDiscountType() { return discountType; }
    public int getMinPurchase() { return minPurchase; }
    public int getUsageLimit() { return usageLimit; }
    public int getUsedCount() { return usedCount; }
    public String getValidFrom() { return validFrom; }
    public String getValidUntil() { return validUntil; }
    public boolean isActive() { return isActive; }
}