// CourierResponse.java
package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CourierResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("data")
    private List<Courier> data;

    public List<Courier> getData() {
        return data;
    }

    public static class Courier {
        @SerializedName("code")
        private String code;

        @SerializedName("name")
        private String name;

        @SerializedName("services")
        private List<String> services;

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public List<String> getServices() {
            return services;
        }
    }
}