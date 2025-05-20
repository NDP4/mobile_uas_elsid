package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.City;

import java.util.List;

// CityResponse.java
public class CityResponse {
    @SerializedName("rajaongkir")
    public RajaongkirData rajaongkir;

    public static class RajaongkirData {
        @SerializedName("results")
        private List<City> results;

        public List<City> getCities() { return results; }
    }
}