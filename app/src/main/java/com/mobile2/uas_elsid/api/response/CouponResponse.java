package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Coupon;

public class CouponResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("coupon")
    private Coupon coupon;

    public int getStatus() { return status; }
    public Coupon getCoupon() { return coupon; }
}