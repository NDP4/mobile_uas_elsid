package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class PaymentStatusResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("data")
    private PaymentData data;

    public int getStatus() {
        return status;
    }

    public PaymentData getData() {
        return data;
    }

    public static class PaymentData {
        @SerializedName("status_code")
        private String statusCode;

        @SerializedName("transaction_id")
        private String transactionId;

        @SerializedName("gross_amount")
        private String grossAmount;

        @SerializedName("currency")
        private String currency;

        @SerializedName("order_id")
        private String orderId;

        @SerializedName("payment_type")
        private String paymentType;

        @SerializedName("signature_key")
        private String signatureKey;

        @SerializedName("transaction_status")
        private String transactionStatus;

        @SerializedName("fraud_status")
        private String fraudStatus;

        @SerializedName("status_message")
        private String statusMessage;

        @SerializedName("merchant_id")
        private String merchantId;

        @SerializedName("transaction_time")
        private String transactionTime;

        @SerializedName("settlement_time")
        private String settlementTime;

        @SerializedName("expiry_time")
        private String expiryTime;

        @SerializedName("payment_status")
        private String paymentStatus;

        @SerializedName("payment_url")
        private String paymentUrl;

        @SerializedName("va_numbers")
        private List<VaNumber> vaNumbers;

        @SerializedName("payment_amounts")
        private List<Object> paymentAmounts;

        public String getStatusCode() { return statusCode; }
        public String getTransactionId() { return transactionId; }
        public String getGrossAmount() { return grossAmount; }
        public String getCurrency() { return currency; }
        public String getOrderId() { return orderId; }
        public String getPaymentType() { return paymentType; }
        public String getSignatureKey() { return signatureKey; }
        public String getTransactionStatus() { return transactionStatus; }
        public String getFraudStatus() { return fraudStatus; }
        public String getStatusMessage() { return statusMessage; }
        public String getMerchantId() { return merchantId; }
        public String getTransactionTime() { return transactionTime; }
        public String getSettlementTime() { return settlementTime; }
        public String getExpiryTime() { return expiryTime; }
        public String getPaymentStatus() { return paymentStatus; }
        public String getPaymentUrl() { return paymentUrl; }
        public List<VaNumber> getVaNumbers() { return vaNumbers; }
        public List<Object> getPaymentAmounts() { return paymentAmounts; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status_code", statusCode);
            map.put("transaction_id", transactionId);
            map.put("gross_amount", grossAmount);
            map.put("currency", currency);
            map.put("order_id", orderId);
            map.put("payment_type", paymentType);
            map.put("signature_key", signatureKey);
            map.put("transaction_status", transactionStatus);
            map.put("fraud_status", fraudStatus);
            map.put("status_message", statusMessage);
            map.put("merchant_id", merchantId);
            map.put("va_numbers", vaNumbers);
            map.put("payment_amounts", paymentAmounts);
            map.put("transaction_time", transactionTime);
            map.put("settlement_time", settlementTime);
            map.put("expiry_time", expiryTime);
            map.put("payment_status", paymentStatus);
            map.put("payment_url", paymentUrl);
            return map;
        }
    }

    public static class VaNumber {
        @SerializedName("bank")
        private String bank;

        @SerializedName("va_number")
        private String vaNumber;

        public String getBank() { return bank; }
        public String getVaNumber() { return vaNumber; }
    }
}