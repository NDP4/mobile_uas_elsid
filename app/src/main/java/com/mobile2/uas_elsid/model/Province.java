package com.mobile2.uas_elsid.model;


import com.google.gson.annotations.SerializedName;

public class Province {
    @SerializedName("province_id")
    private String id;
    @SerializedName("province")
    private String name;

    public String getId() { return id; }
    public String getName() { return name; }
}