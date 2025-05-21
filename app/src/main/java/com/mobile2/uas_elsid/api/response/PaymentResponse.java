package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;

public class PaymentResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private PaymentData data;

    public static class PaymentData {
        @SerializedName("token")
        private String token;

        @SerializedName("payment_url")
        private String paymentUrl;

        public String getToken() { return token; }
        public String getPaymentUrl() { return paymentUrl; }
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public PaymentData getData() { return data; }
}