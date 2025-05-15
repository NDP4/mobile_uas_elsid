package com.mobile2.uas_elsid.api;

import com.mobile2.uas_elsid.api.response.UserResponse;
import com.mobile2.uas_elsid.api.response.request.LoginRequest;
import com.mobile2.uas_elsid.api.response.request.RegisterRequest;
//import com.mobile2.uas_elsid.model.ApiResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/login")
    Call<UserResponse> login(@Body LoginRequest loginRequest);

    @POST("api/register")
    Call<UserResponse> register(@Body RegisterRequest registerRequest);
}