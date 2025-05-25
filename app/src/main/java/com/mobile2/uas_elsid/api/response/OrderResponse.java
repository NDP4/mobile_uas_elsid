package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Order;

import java.util.List;

public class OrderResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private OrderData data;

    @SerializedName("orders")
    private List<Order> orders;
    @SerializedName("order")
    private Order order;

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public OrderData getData() {
        return data;
    }

    public List<Order> getOrders() {
        return orders;
    }
    public Order getOrder() {
        return order;
    }

    public static class OrderData {
        @SerializedName("order")
        private Order order;

        @SerializedName("can_reorder")
        private boolean canReorder;

        public Order getOrder() {
            return order;
        }

        public boolean isCanReorder() {
            return canReorder;
        }
    }
}