package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Order;

public class OrderResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("order")
    private Order order;

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Order getOrder() {
        return order;
    }
}