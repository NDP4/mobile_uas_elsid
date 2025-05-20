package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.Province;

import java.util.List;

// ProvinceResponse.java
public class ProvinceResponse {
    @SerializedName("rajaongkir")
    public RajaongkirData rajaongkir;

    public static class RajaongkirData {
        @SerializedName("results")
        private List<Province> results;

        public List<Province> getProvinces() { return results; }
    }
}


