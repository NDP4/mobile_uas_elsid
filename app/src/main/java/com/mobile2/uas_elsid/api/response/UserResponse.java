package com.mobile2.uas_elsid.api.response;

import com.mobile2.uas_elsid.model.User;

public class UserResponse {
    private int status;
    private String message;
    private User user;

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public User getUser() { return user; }
}