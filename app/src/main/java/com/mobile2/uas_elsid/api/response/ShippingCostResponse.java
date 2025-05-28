package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ShippingCostResponse {
    @SerializedName("rajaongkir")
    public RajaOngkir rajaongkir;

    public static class RajaOngkir {
        @SerializedName("results")
        public List<Result> results;
    }

    public static class Result {
        @SerializedName("code")
        public String code;

        @SerializedName("name")
        public String name;

        @SerializedName("costs")
        public List<Cost> costs;
    }

    public static class Cost {
        @SerializedName("service")
        public String service;

        @SerializedName("description")
        public String description;

        @SerializedName("cost")
        public List<CostDetail> cost;
    }

    public static class CostDetail {
        @SerializedName("value")
        public int value;

        @SerializedName("etd")
        public String etd;

        @SerializedName("note")
        public String note;
    }
}
