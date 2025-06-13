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
        if (isGuestMode()) {
            SharedPreferences prefs = context.getSharedPreferences(GUEST_CART_PREFS, Context.MODE_PRIVATE);
            String cartJson = prefs.getString(GUEST_CART_ITEMS, "");
            List<CartItem> guestCart;
            
            try {
                Type type = new TypeToken<List<CartItem>>(){}.getType();
                guestCart = new Gson().fromJson(cartJson, type);
                if (guestCart == null) {
                    guestCart = new ArrayList<>();
                }
                
                // Check for existing item
                boolean itemFound = false;
                for (CartItem existingItem : guestCart) {
                    if (isSameProduct(existingItem, item)) {
                        // Update quantity instead of adding new item
                        existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                        itemFound = true;
                        break;
                    }
                }
                
                // Add new item if not found
                if (!itemFound) {
                    guestCart.add(item);
                }
                
                // Save updated cart
                String updatedJson = new Gson().toJson(guestCart);
                prefs.edit().putString(GUEST_CART_ITEMS, updatedJson).apply();
                
                callback.onSuccess(guestCart);
            } catch (Exception e) {
                callback.onError("Error adding item to guest cart");
            }
            return;
        }

        // Handle logged in user cart
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

    private boolean isSameProduct(CartItem item1, CartItem item2) {
        // Check if products are the same
        boolean sameProduct = item1.getProduct().getId() == item2.getProduct().getId();
        
        // If variants exist, check if they match
        if (item1.getVariant() != null && item2.getVariant() != null) {
            return sameProduct && item1.getVariant().getId() == item2.getVariant().getId();
        }
        
        // If no variants, just check product ID
        return sameProduct;
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
        if (isGuestMode()) {
            SharedPreferences prefs = context.getSharedPreferences(GUEST_CART_PREFS, Context.MODE_PRIVATE);
            String cartJson = prefs.getString(GUEST_CART_ITEMS, "");
            
            try {
                Type type = new TypeToken<List<CartItem>>(){}.getType();
                List<CartItem> guestCart = new Gson().fromJson(cartJson, type);
                
                if (guestCart != null) {
                    // Find and remove specific item
                    boolean removed = guestCart.removeIf(item -> 
                        item.getProduct().getId() == cartItemId || item.getId() == cartItemId);
                    
                    if (removed) {
                        // Save updated cart
                        String updatedJson = new Gson().toJson(guestCart);
                        prefs.edit().putString(GUEST_CART_ITEMS, updatedJson).apply();
                        callback.onSuccess(guestCart);
                    } else {
                        callback.onError("Item not found in cart");
                    }
                } else {
                    callback.onError("Cart is empty");
                }
            } catch (Exception e) {
                callback.onError("Error removing item from guest cart");
            }
            return;
        }

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
        if (isGuestMode()) {
            SharedPreferences prefs = context.getSharedPreferences(GUEST_CART_PREFS, Context.MODE_PRIVATE);
            prefs.edit().remove(GUEST_CART_ITEMS).apply();
            callback.onSuccess(new ArrayList<>());
            return;
        }

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