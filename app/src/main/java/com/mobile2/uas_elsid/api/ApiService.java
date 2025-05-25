package com.mobile2.uas_elsid.api;

import com.mobile2.uas_elsid.api.response.BannerResponse;
import com.mobile2.uas_elsid.api.response.CartResponse;
import com.mobile2.uas_elsid.api.response.CityResponse;
import com.mobile2.uas_elsid.api.response.CouponResponse;
import com.mobile2.uas_elsid.api.response.OrderResponse;
import com.mobile2.uas_elsid.api.response.PaymentResponse;
import com.mobile2.uas_elsid.api.response.ProductDetailResponse;
import com.mobile2.uas_elsid.api.response.ProductResponse;
import com.mobile2.uas_elsid.api.response.ProvinceResponse;
import com.mobile2.uas_elsid.api.response.ReviewResponse;
import com.mobile2.uas_elsid.api.response.ShippingCostResponse;
import com.mobile2.uas_elsid.api.response.UserResponse;
import com.mobile2.uas_elsid.api.response.WishlistCheckResponse;
import com.mobile2.uas_elsid.api.response.WishlistResponse;
import com.mobile2.uas_elsid.api.response.request.LoginRequest;
import com.mobile2.uas_elsid.api.response.request.RegisterRequest;
import com.mobile2.uas_elsid.api.response.request.ReviewRequest;
import com.mobile2.uas_elsid.model.CartItem;
//import com.mobile2.uas_elsid.model.ApiResponse;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/login")
    Call<UserResponse> login(@Body LoginRequest loginRequest);

    @POST("api/register")
    Call<UserResponse> register(@Body RegisterRequest registerRequest);

    @PUT("api/users/{id}")
    Call<UserResponse> updateUser(
            @Path("id") String userId,
            @Body Map<String, String> updateData
    );

    @Multipart
    @POST("api/users/avatar")
    Call<UserResponse> updateAvatar(
            @Part("user_id") RequestBody userIdBody,
            @Part MultipartBody.Part avatar
    );

    // banner
    @GET("api/banners")
    Call<BannerResponse> getBanners();

    // product
    @GET("api/products")
    Call<ProductResponse> getProducts();

//    @GET("api/products/{id}")
//    Call<ProductResponse> getProduct(@Path("id") int productId);
    @GET("api/products/{id}")
    Call<ProductDetailResponse> getProduct(@Path("id") int productId);

//    @POST("api/products/view-count")
//    Call<ProductResponse> updateViewCount(@Body Map<String, Integer> productId);
    @POST("api/products/view-count")
    Call<Void> updateProductViewCount(@Body Map<String, Integer> body);

    @GET("api/products/{productId}/variants")
    Call<ProductResponse> getProductVariants(@Path("productId") int productId);

    @GET("api/products/{productId}/reviews")
    Call<ReviewResponse> getProductReviews(@Path("productId") int productId);

    @POST("api/reviews")
    Call<ReviewResponse> addReview(@Body ReviewRequest reviewRequest);

    @PUT("api/reviews/{id}")
    Call<ReviewResponse> updateReview(@Path("id") int id, @Body ReviewRequest reviewRequest);

    @DELETE("api/reviews/{id}")
    Call<ReviewResponse> deleteReview(@Path("id") int id);

    @GET("api/users/{userId}/reviews")
    Call<ReviewResponse> getUserReviews(@Path("userId") int userId);

    @GET("api/cart/{userId}")
    Call<CartResponse> getCart(@Path("userId") String userId);

    @POST("api/cart")
    Call<CartResponse> addToCart(@Body Map<String, Object> request);
    @PUT("api/cart/{id}")
    Call<CartResponse> updateCartItem(@Path("id") int id, @Body CartItem cartItem);

    @DELETE("api/cart/{id}")
    Call<CartResponse> removeCartItem(@Path("id") int id);
    @GET("api/shipping/provinces")
    Call<ProvinceResponse> getProvinces();

    @GET("api/shipping/cities")
    Call<CityResponse> getCities(@Query("province") String provinceId);

    @POST("api/shipping/calculate")
    Call<ShippingCostResponse> calculateShipping(@Body Map<String, Object> request);
    @POST("api/coupons/validate")
    Call<CouponResponse> validateCoupon(@Body Map<String, String> request);
    @POST("api/orders")
    Call<OrderResponse> createOrder(@Body Map<String, Object> orderData);

//    @POST("api/payments/create")
//    Call<PaymentResponse> createPayment(@Body Map<String, Integer> request);
    @POST("api/payments/create")
    Call<PaymentResponse> createPayment(@Body Map<String, Object> request);

    @GET("api/payments/status/{orderId}")
    Call<PaymentResponse> checkPaymentStatus(@Path("orderId") int orderId);
    @DELETE("api/cart/clear/{userId}")
    Call<CartResponse> deleteCartItems(@Path("userId") String userId);
    @GET("api/users/{userId}/purchase-history")
    Call<OrderResponse> getPurchaseHistory(@Path("userId") String userId);

    @GET("api/orders/{id}")
    Call<OrderResponse> getOrder(@Path("id") int orderId);
    @POST("api/orders/reorder")
    Call<OrderResponse> reorderItems(@Body Map<String, Object> reorderData);
    @GET("api/users/{userId}/orders")
    Call<OrderResponse> getUserOrders(@Path("userId") String userId);

//    @GET("api/products/{productId}/reviews")
//    Call<ReviewResponse> getProductReviews(@Path("productId") int productId);

    @POST("api/reviews")
    Call<ReviewResponse> addReview(@Body Map<String, Object> reviewData);

    @GET("api/users/{userId}/wishlist")
    Call<WishlistResponse> getWishlist(@Path("userId") String userId);

    @POST("api/users/{userId}/wishlist")
    Call<WishlistResponse> addToWishlist(@Path("userId") String userId, @Body Map<String, Integer> request);

    @DELETE("api/users/{userId}/wishlist/{productId}")
    Call<WishlistResponse> removeFromWishlist(@Path("userId") String userId, @Path("productId") int productId);

    @GET("api/users/{userId}/wishlist/check/{productId}")
    Call<WishlistCheckResponse> checkWishlist(@Path("userId") String userId, @Path("productId") int productId);

}