package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }
}