package com.mobile2.uas_elsid.api.response;

import com.google.gson.annotations.SerializedName;
import com.mobile2.uas_elsid.model.User;

public class UserResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private User user;

    @SerializedName("avatar")
    private String avatar;

    // Getters
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public User getUser() { return user; }
    public String getAvatar() { return avatar; }
}