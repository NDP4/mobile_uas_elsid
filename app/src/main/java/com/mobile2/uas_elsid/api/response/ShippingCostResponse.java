package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ShippingCostResponse {
    @SerializedName("rajaongkir")
    public RajaOngkirData rajaongkir;

    public static class RajaOngkirData {
        @SerializedName("results")
        public List<CourierResult> results;
    }

    public static class CourierResult {
        @SerializedName("costs")
        public List<Cost> costs;
    }

    public static class Cost {
        @SerializedName("service")
        public String service;
        @SerializedName("cost")
        public List<CostDetail> cost;
    }

    public static class CostDetail {
        @SerializedName("value")
        public int value;
        @SerializedName("etd")
        public String etd;
    }
}