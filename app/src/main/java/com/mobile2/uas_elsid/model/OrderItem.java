package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

public class OrderItem {
    @SerializedName("id")
    private int id;

    @SerializedName("order_id")
    private int orderId;

    @SerializedName("product_id")
    private int productId;

    @SerializedName("variant_id")
    private Integer variantId;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("price")
    private int price;

    @SerializedName("subtotal")
    private int subtotal;

    @SerializedName("product")
    private Product product;

    @SerializedName("variant")
    private ProductVariant variant;

    // Getters
    public int getId() { return id; }
    public int getOrderId() { return orderId; }
    public int getProductId() { return productId; }
    public Integer getVariantId() { return variantId; }
    public int getQuantity() { return quantity; }
    public int getPrice() { return price; }
    public int getSubtotal() { return subtotal; }
    public Product getProduct() { return product; }
    public ProductVariant getVariant() { return variant; }
}