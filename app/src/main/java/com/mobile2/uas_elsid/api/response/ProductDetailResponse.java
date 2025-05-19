package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Product;

public class ProductDetailResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("product")
    private Product product;

    public int getStatus() { return status; }
    public Product getProduct() { return product; }
}