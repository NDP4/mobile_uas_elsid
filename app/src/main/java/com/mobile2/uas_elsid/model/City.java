package com.mobile2.uas_elsid.model;


import com.google.gson.annotations.SerializedName;

public class City {
    @SerializedName("city_id")
    private String id;
    @SerializedName("city_name")
    private String name;
    @SerializedName("province_id")
    private String provinceId;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getProvinceId() { return provinceId; }
}