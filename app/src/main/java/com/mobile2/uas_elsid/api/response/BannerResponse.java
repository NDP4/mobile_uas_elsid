package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Banner;

import java.util.List;

// BannerResponse.java
public class BannerResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("banners")
    private List<Banner> banners;

    public int getStatus() { return status; }
    public List<Banner> getBanners() { return banners; }
}