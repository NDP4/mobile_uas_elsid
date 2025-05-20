package com.mobile2.uas_elsid.model;

import com.google.gson.annotations.SerializedName;

public class CartItem {
    @SerializedName("id")
    private int id;

    @SerializedName("product_id")
    private int productId;

    @SerializedName("variant_id")
    private Integer variantId;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("product")
    private Product product;

    @SerializedName("variant")
    private ProductVariant variant;

    // Constructor
    public CartItem(Product product, ProductVariant variant, int quantity) {
        this.product = product;
        this.variant = variant;
        this.quantity = quantity;
        this.productId = product.getId();
        this.variantId = variant != null ? variant.getId() : null;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public Integer getVariantId() {
        return variantId;
    }

    public void setVariantId(Integer variantId) {
        this.variantId = variantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }
}