package com.mobile2.uas_elsid.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.ApiService;
import com.mobile2.uas_elsid.api.response.CartResponse;
import com.mobile2.uas_elsid.model.CartItem;

import org.json.JSONObject;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartManager {
    private static CartManager instance;
    private final ApiService apiService;
    private final SessionManager sessionManager;
    private final Context context;
    private final String GUEST_CART_PREFS = "guest_cart";
    private final String GUEST_CART_ITEMS = "guest_cart_items";
    private List<CartItem> cartItems = new ArrayList<>();

    private boolean isGuestMode() {
        SessionManager sessionManager = new SessionManager(context);
        return !sessionManager.isLoggedIn();
    }

    // Add CartCallback interface
    public interface CartCallback {
        void onSuccess(List<CartItem> items);
        void onError(String message);
    }

    private CartManager(Context context) {
        this.context = context;
        this.apiService = ApiClient.getClient();
        this.sessionManager = new SessionManager(context);
    }

    // Add getInstance method
    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }

    // Update addToCart method
    public void addToCart(CartItem item, CartCallback callback) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onError("User not logged in");
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("user_id", userId);
        request.put("product_id", item.getProductId());
        request.put("variant_id", item.getVariantId());
        request.put("quantity", item.getQuantity());

        apiService.addToCart(request).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getCartItems());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Unknown error";
                        callback.onError(errorBody);
                    } catch (IOException e) {
                        callback.onError("Error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Update getCartItems method
//    public void getCartItems(CartCallback callback) {
//        String userId = sessionManager.getUserId();
//        if (userId == null || userId.isEmpty()) {
//            callback.onError("User not logged in");
//            return;
//        }
//
//
//        apiService.getCart(userId).enqueue(new Callback<CartResponse>() {
//            @Override
//            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    callback.onSuccess(response.body().getCartItems());
//                } else {
//                    try {
//                        String errorBody = response.errorBody() != null ?
//                                response.errorBody().string() : "Unknown error";
//                        callback.onError(errorBody);
//                    } catch (IOException e) {
//                        callback.onError("Error: " + e.getMessage());
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<CartResponse> call, Throwable t) {
//                callback.onError("Network error: " + t.getMessage());
//            }
//        });
//    }
    public void getCartItems(CartCallback callback) {
        if (isGuestMode()) {
            // Return guest cart items from SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(GUEST_CART_PREFS, Context.MODE_PRIVATE);
            String guestCartJson = prefs.getString(GUEST_CART_ITEMS, "");

            try {
                Type type = new TypeToken<List<CartItem>>(){}.getType();
                List<CartItem> guestItems = new Gson().fromJson(guestCartJson, type);
                callback.onSuccess(guestItems != null ? guestItems : new ArrayList<>());
            } catch (Exception e) {
                callback.onError("Error loading guest cart");
            }
        } else {
            String userId = sessionManager.getUserId();
            if (userId == null || userId.isEmpty()) {
                callback.onError("User not logged in");
                return;
            }


            apiService.getCart(userId).enqueue(new Callback<CartResponse>() {
                @Override
                public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body().getCartItems());
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ?
                                    response.errorBody().string() : "Unknown error";
                            callback.onError(errorBody);
                        } catch (IOException e) {
                            callback.onError("Error: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onFailure(Call<CartResponse> call, Throwable t) {
                    callback.onError("Network error: " + t.getMessage());
                }
            });
        }
    }
    public void removeFromCart(int cartItemId, CartCallback callback) {
        apiService.removeCartItem(cartItemId).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getCartItems());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Unknown error";
                        callback.onError(errorBody);
                    } catch (IOException e) {
                        callback.onError("Error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    public void clearCart(CartCallback callback) {
        if (!sessionManager.isLoggedIn()) {
            callback.onError("User not logged in");
            return;
        }

        ApiClient.getClient().deleteCartItems(sessionManager.getUserId()).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(@NonNull Call<CartResponse> call, @NonNull Response<CartResponse> response) {
                if (response.isSuccessful()) {
                    cartItems.clear();
                    callback.onSuccess(cartItems);
                } else {
                    callback.onError("Failed to clear cart");
                }
            }

            @Override
            public void onFailure(@NonNull Call<CartResponse> call, @NonNull Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}