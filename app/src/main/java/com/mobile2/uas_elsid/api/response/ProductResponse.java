package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Product;
import java.util.List;

public class ProductResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("products")
    private List<Product> products;

    public int getStatus() { return status; }
    public List<Product> getProducts() { return products; }
}