package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;

public class WishlistCheckResponse {
    @SerializedName("status")
    private int status;
    @SerializedName("exists")
    private boolean exists;

    public int getStatus() { return status; }
    public boolean isExists() { return exists; }
}