package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Coupon;

public class CouponResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("coupon")
    private Coupon coupon;

    // Getters
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public Coupon getCoupon() { return coupon; }
}