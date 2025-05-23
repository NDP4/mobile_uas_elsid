package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Wishlist;

import java.util.List;

public class WishlistResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("wishlist")
    private List<Wishlist> wishlist;
    @SerializedName("message")
    private String message;

    public int getStatus() { return status; }
    public List<Wishlist> getWishlist() { return wishlist; }
    public String getMessage() { return message; }
}
