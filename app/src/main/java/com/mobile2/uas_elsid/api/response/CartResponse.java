package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.CartItem;

import java.util.List;

public class CartResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("cart_items")
    private List<CartItem> cartItems;
    @SerializedName("total")
    private int total;

    public int getStatus() { return status; }
    public List<CartItem> getCartItems() { return cartItems; }
    public int getTotal() { return total; }
}