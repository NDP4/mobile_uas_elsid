package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;

public class BaseResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("message")
    private String message;

    public int getStatus() { return status; }
    public String getMessage() { return message; }
}