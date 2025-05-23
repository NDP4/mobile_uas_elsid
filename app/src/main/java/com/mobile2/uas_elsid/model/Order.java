package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Order {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("total_amount")
    private int totalAmount;

    @SerializedName("shipping_cost")
    private int shippingCost;

    @SerializedName("courier")
    private String courier;

    @SerializedName("courier_service")
    private String courierService;

    @SerializedName("shipping_address")
    private String shippingAddress;

    @SerializedName("shipping_city")
    private String shippingCity;

    @SerializedName("shipping_province")
    private String shippingProvince;

    @SerializedName("shipping_postal_code")
    private String shippingPostalCode;

    @SerializedName("status")
    private String status;

    @SerializedName("payment_status")
    private String paymentStatus;

    @SerializedName("payment_url")
    private String paymentUrl;

    @SerializedName("items")
    private List<OrderItem> items;

    @SerializedName("user")
    private User user;

    // Getters
    public int getId() { return id; }
    public String getUserId() { return userId; }
    public int getTotalAmount() { return totalAmount; }
    public int getShippingCost() { return shippingCost; }
    public String getCourier() { return courier; }
    public String getCourierService() { return courierService; }
    public String getShippingAddress() { return shippingAddress; }
    public String getShippingCity() { return shippingCity; }
    public String getShippingProvince() { return shippingProvince; }
    public String getShippingPostalCode() { return shippingPostalCode; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentUrl() { return paymentUrl; }
    public List<OrderItem> getItems() { return items; }
    public User getUser() { return user; }
}