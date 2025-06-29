package com.mobile2.uas_elsid.ui.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.mobile2.uas_elsid.LoginActivity;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.CourierAdapter;
import com.mobile2.uas_elsid.adapter.DetailPesananAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.CityResponse;
import com.mobile2.uas_elsid.api.response.CouponResponse;
import com.mobile2.uas_elsid.api.response.ErrorResponse;
import com.mobile2.uas_elsid.api.response.OrderResponse;
import com.mobile2.uas_elsid.api.response.PaymentResponse;
import com.mobile2.uas_elsid.api.response.ProvinceResponse;
import com.mobile2.uas_elsid.api.response.ShippingCostResponse;
import com.mobile2.uas_elsid.databinding.FragmentDetailPesananBinding;
import com.mobile2.uas_elsid.model.CartItem;
import com.mobile2.uas_elsid.model.City;
import com.mobile2.uas_elsid.model.Coupon;
import com.mobile2.uas_elsid.model.Province;
import com.mobile2.uas_elsid.utils.CartManager;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailPesananFragment extends Fragment {
    private FragmentDetailPesananBinding binding;
    private CartManager cartManager;
    private SessionManager sessionManager;
    private DetailPesananAdapter adapter;
    private CourierAdapter courierAdapter;
    private int totalWeight = 0;
    private int shippingCost = 0;
    private static final String ORIGIN_CITY = "501";
    private String selectedProvinceId;
    private String selectedCityId;
    private Coupon activeCoupon = null;
    private String selectedPaymentMethod = null; // Changed from "online" to null
    private List<Province> provinceList = new ArrayList<>();
    private List<City> cityList = new ArrayList<>();
    private ArrayAdapter<String> provinceAdapter;
    private ArrayAdapter<String> cityAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDetailPesananBinding.inflate(inflater, container, false);
        cartManager = CartManager.getInstance(requireContext());
        sessionManager = new SessionManager(requireContext());

        // Setup payment method selection
        setupPaymentMethodSelection();

        // tombol back
        binding.backButton.setOnClickListener(v ->
            Navigation.findNavController(v).popBackStack());

        // Setup views
        setupRecyclerView();
        setupCouponInput();
        setupCheckoutButton();
        setupAddressSection();
        setupShippingAddress();

        // Load data
        loadCartItems();
        loadProvinces();

        // Setup back button
        binding.backButton.setOnClickListener(v ->
            Navigation.findNavController(v).popBackStack());

        return binding.getRoot();
    }

    private void setupPaymentMethodSelection() {
        // Default to COD payment
        binding.codPaymentRadio.setChecked(true);
        selectedPaymentMethod = "cod";
        
        binding.paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.onlinePaymentRadio) {
                selectedPaymentMethod = "online";
            } else if (checkedId == R.id.codPaymentRadio) {
                selectedPaymentMethod = "cod";
            }
            Log.d("PaymentDebug", "Selected payment method: " + selectedPaymentMethod);
        });
    }

    private void setupAddressSection() {
        // Load provinces first
        loadProvinces();
        
        // Clear any existing address inputs
        binding.addressInput.setText("");
        binding.postalCodeInput.setText("");
        binding.provinceSpinner.setText("");
        binding.citySpinner.setText("");
    }

    private void setupRecyclerView() {
        // Setup order items recycler view
        adapter = new DetailPesananAdapter(requireContext());
        binding.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.orderItemsRecyclerView.setAdapter(adapter);

        // Setup courier recycler view with fixed height and decoration
        courierAdapter = new CourierAdapter(requireContext(), cost -> {
            shippingCost = cost.cost.get(0).value;
            calculateTotals();
        });
        binding.courierRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.courierRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        binding.courierRecyclerView.setAdapter(courierAdapter);
        binding.courierRecyclerView.setHasFixedSize(true);
    }

    private void loadCartItems() {
        cartManager.getCartItems(new CartManager.CartCallback() {
            @Override
            public void onSuccess(List<CartItem> items) {
                adapter.setItems(items);
                calculateTotals();
                totalWeight = calculateTotalWeight();

                // Only load shipping options if city is selected
                if (selectedCityId != null && !selectedCityId.isEmpty()) {
                    loadShippingOptions();
                }
            }

            @Override
            public void onError(String message) {
                Toasty.error(requireContext(), message).show();
            }
        });
    }

    private void loadShippingOptions() {
        String[] couriers = {"jne", "pos", "tiki"};
        courierAdapter.clearCosts();
        int weight = calculateTotalWeight();

        // Reset any previous errors
        binding.courierRecyclerView.setVisibility(View.VISIBLE);
        
        // Validate required fields
        if (selectedCityId == null || selectedCityId.isEmpty() || 
            binding.addressInput.getText().toString().trim().isEmpty() ||
            binding.postalCodeInput.getText().toString().trim().isEmpty()) {
            Log.d("ShippingDebug", "Missing required fields: " +
                "cityId=" + selectedCityId + 
                ", address=" + binding.addressInput.getText().toString() +
                ", postal=" + binding.postalCodeInput.getText().toString());
            Toasty.warning(requireContext(), "Please fill in all shipping fields").show();
            return;
        }

        // Ensure weight is valid
        if (weight <= 0) {
            Log.e("ShippingDebug", "Invalid weight: " + weight);
            Toasty.error(requireContext(), "Invalid item weight").show();
            return;
        }
        
        Log.d("ShippingDebug", "Calculating shipping for: " +
            "origin=" + ORIGIN_CITY +
            ", destination=" + selectedCityId +
            ", weight=" + weight);
        
        for (String courier : couriers) {
            Map<String, Object> request = new HashMap<>();
            request.put("origin", ORIGIN_CITY);
            request.put("destination", selectedCityId);
            request.put("weight", weight);
            request.put("courier", courier);

            Log.d("ShippingDebug", "Sending request for " + courier + ": " + new Gson().toJson(request));

            ApiClient.getClient().calculateShipping(request).enqueue(new Callback<ShippingCostResponse>() {
                @Override
                public void onResponse(@NonNull Call<ShippingCostResponse> call, 
                                     @NonNull Response<ShippingCostResponse> response) {
                    Log.d("ShippingDebug", "Got response for " + courier + ", code: " + response.code());
                    
                    if (!response.isSuccessful()) {
                        Log.e("ShippingDebug", "Response not successful: " + response.code());
                        if (response.errorBody() != null) {
                            try {
                                String errorBody = response.errorBody().string();
                                Log.e("ShippingDebug", "Error body: " + errorBody);
                                requireActivity().runOnUiThread(() -> {
                                    if (errorBody.contains("Internal Server Error")) {
//                                        Toasty.error(requireContext(),
//                                            "RajaOngkir service error, please try again later").show();
                                    } else {
                                        Toasty.error(requireContext(), "Error: " + errorBody).show();
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                                requireActivity().runOnUiThread(() -> 
                                    Toasty.error(requireContext(), "Error getting shipping rates").show());
                            }
                        }
                        return;
                    }

                    if (response.body() == null) {
                        Log.e("ShippingDebug", "Response body is null");
                        requireActivity().runOnUiThread(() -> {
                            Toasty.error(requireContext(), "No response data received").show();
                        });
                        return;
                    }

                    ShippingCostResponse shippingResponse = response.body();
                    Log.d("ShippingDebug", "Response body: " + new Gson().toJson(shippingResponse));
                    
                    if (shippingResponse.rajaongkir != null 
                        && shippingResponse.rajaongkir.results != null
                        && !shippingResponse.rajaongkir.results.isEmpty()
                        && shippingResponse.rajaongkir.results.get(0).costs != null 
                        && !shippingResponse.rajaongkir.results.get(0).costs.isEmpty()) {

                        List<ShippingCostResponse.Cost> newCosts = shippingResponse.rajaongkir.results.get(0).costs;
                        Log.d("ShippingDebug", "Got costs for " + courier + ": " + newCosts.size() + " services");

                        for (ShippingCostResponse.Cost cost : newCosts) {
                            cost.service = courier.toUpperCase() + " " + cost.service;
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            courierAdapter.addCosts(newCosts);
                            Log.d("ShippingDebug", "Updated adapter with " + courierAdapter.getCosts().size() + " total costs");
                        });
                    } else {
                        Log.e("ShippingDebug", "No shipping costs available in response");
                        requireActivity().runOnUiThread(() -> {
                            Toasty.warning(requireContext(), "No shipping options available for " + courier).show();
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ShippingCostResponse> call, @NonNull Throwable t) {
                    Log.e("ShippingDebug", "API call failed for " + courier + ": " + t.getMessage());
                    t.printStackTrace();
                    requireActivity().runOnUiThread(() -> {
                        Toasty.error(requireContext(), 
                            "Failed to get shipping rates. Please check your connection.").show();
                    });
                }
            });
        }
    }

    private void setupCouponInput() {
        binding.applyCouponButton.setOnClickListener(v -> {
            String code = binding.couponInput.getText().toString().trim();
            if (!code.isEmpty()) {
                applyCoupon(code);
            }
        });
    }

    private void applyCoupon(String code) {
        // If there's already an active coupon, remove it
        if (activeCoupon != null) {
            activeCoupon = null;
            binding.couponInput.setText("");
            binding.couponInput.setEnabled(true);
            binding.applyCouponButton.setText("Apply");
            binding.discountContainer.setVisibility(View.GONE);
            calculateTotals();
            return;
        }

        // Calculate subtotal for validation
        int subtotal = calculateSubtotal();

        // Prepare request body
        Map<String, String> request = new HashMap<>();
        request.put("code", code);
        request.put("user_id", sessionManager.getUserId());
        request.put("subtotal", String.valueOf(subtotal)); // Add subtotal to request

        ApiClient.getClient().validateCoupon(request).enqueue(new Callback<CouponResponse>() {
            @Override
            public void onResponse(@NonNull Call<CouponResponse> call, @NonNull Response<CouponResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCoupon() != null) {
                    activeCoupon = response.body().getCoupon();
                    binding.couponInput.setEnabled(false);
                    binding.applyCouponButton.setText("Remove");
                    binding.discountContainer.setVisibility(View.VISIBLE);
                    calculateTotals();
                    Toasty.success(requireContext(), "Coupon applied successfully").show();
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            String errorMessage = null;
                            try {
                                ErrorResponse error = new Gson().fromJson(errorBody, ErrorResponse.class);
                                if (error != null && error.getMessage() != null && !error.getMessage().isEmpty()) {
                                    errorMessage = error.getMessage();
                                }
                            } catch (Exception e) {
                                // Ignore JSON parse error, fallback to raw errorBody
                            }
                            if (errorMessage != null) {
                                Toasty.error(requireContext(), errorMessage).show();
                            } else {
                                Toasty.error(requireContext(), "Failed to apply coupon: " + errorBody).show();
                            }
                        } else {
                            Toasty.error(requireContext(), "Invalid coupon code").show();
                        }
                    } catch (IOException e) {
                        Toasty.error(requireContext(), "Error applying coupon").show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CouponResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
            }
        });
    }

    private void calculateTotals() {
        int subtotal = calculateSubtotal();
        int discount = 0;

        // Calculate discount if coupon is active
        if (activeCoupon != null) {
            if ("percentage".equals(activeCoupon.getDiscountType())) {
                discount = (int) (subtotal * (activeCoupon.getDiscountAmount() / 100.0));
            } else {
                discount = (int) activeCoupon.getDiscountAmount();
            }
            binding.discountContainer.setVisibility(View.VISIBLE);
            binding.discountText.setText("-" + formatPrice(discount));
        } else {
            binding.discountContainer.setVisibility(View.GONE);
        }

        int total = subtotal + shippingCost - discount;

        // Update summary views
        binding.subtotalText.setText(formatPrice(subtotal));
        binding.shippingText.setText(formatPrice(shippingCost));
        binding.totalText.setText(formatPrice(total));
        binding.bottomTotalText.setText(formatPrice(total));
    }

    private void calculateShipping(String origin, String destination, int weight, String courier) {
        Map<String, Object> request = new HashMap<>();
        request.put("origin", origin);
        request.put("destination", destination);
        request.put("weight", weight);
        request.put("courier", courier);

        ApiClient.getClient().calculateShipping(request).enqueue(new Callback<ShippingCostResponse>() {
            @Override
            public void onResponse(Call<ShippingCostResponse> call, Response<ShippingCostResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().rajaongkir != null
                        && !response.body().rajaongkir.results.isEmpty()
                        && !response.body().rajaongkir.results.get(0).costs.isEmpty()) {

                    // Get the first available service and its cost
                    ShippingCostResponse.Cost firstService = response.body().rajaongkir.results.get(0).costs.get(0);
                    shippingCost = firstService.cost.get(0).value;

                    // Update UI with new shipping cost
                    calculateTotals();
                } else {
                    Toasty.error(requireContext(), "Failed to get shipping cost").show();
                }
            }

            @Override
            public void onFailure(Call<ShippingCostResponse> call, Throwable t) {
                Toasty.error(requireContext(), "Failed to calculate shipping cost").show();
            }
        });
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Remove ",00"
    }

    private void showAddressSelectionDialog() {
        // Check if user has set their address
        if (sessionManager.getAddress() == null || sessionManager.getCity() == null ||
                sessionManager.getProvince() == null || sessionManager.getPostalCode() == null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Address Required")
                    .setMessage("Please set your delivery address in your profile first")
                    .setPositiveButton("Go to Profile", (dialog, which) -> {
                        Navigation.findNavController(requireView()).navigate(R.id.navigation_edit_profile);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Check if we have the city ID
        String cityId = sessionManager.getCityId();
        if (cityId == null || cityId.isEmpty()) {
            Toasty.error(requireContext(), "City ID not found. Please update your profile").show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_courier_list, null);
        builder.setView(dialogView);

        // Create dialog instance first
        AlertDialog dialog = builder.create();

        RecyclerView courierRecyclerView = dialogView.findViewById(R.id.courierRecyclerView);
        courierRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup adapter with dialog reference now available
        CourierAdapter adapter = new CourierAdapter(requireContext(), cost -> {
            // When courier is selected
            shippingCost = cost.cost.get(0).value;
            calculateTotals();
            dialog.dismiss(); // Now dialog is in scope
        });

        courierRecyclerView.setAdapter(adapter);

        // Load shipping costs for all couriers
        String[] couriers = {"jne", "pos", "tiki"};
        for (String courier : couriers) {
            Map<String, Object> request = new HashMap<>();
            request.put("origin", ORIGIN_CITY);
            request.put("destination", sessionManager.getCityId());
            request.put("weight", totalWeight);
            request.put("courier", courier);

            ApiClient.getClient().calculateShipping(request).enqueue(new Callback<ShippingCostResponse>() {
                @Override
                public void onResponse(Call<ShippingCostResponse> call, Response<ShippingCostResponse> response) {
                    if (response.isSuccessful() && response.body() != null
                            && response.body().rajaongkir != null
                            && !response.body().rajaongkir.results.isEmpty()) {

                        List<ShippingCostResponse.Cost> newCosts = response.body().rajaongkir.results.get(0).costs;
                        List<ShippingCostResponse.Cost> currentCosts = new ArrayList<>(adapter.getCosts());
                        currentCosts.addAll(newCosts);
                        adapter.setCosts(currentCosts);
                    }
                }

                @Override
                public void onFailure(Call<ShippingCostResponse> call, Throwable t) {
                    Toasty.error(requireContext(), "Failed to load shipping cost for " + courier).show();
                }
            });
        }

        dialog.show();
    }

    private void setupCheckoutButton() {
        binding.checkoutButton.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                View dialogView = getLayoutInflater().inflate(R.layout.layout_login_required_dialog, null);
                TextView messageText = dialogView.findViewById(R.id.loginMessageText);
                messageText.setText("Please login to complete your purchase");

                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

                // Set transparent background for rounded corners
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                // Setup button clicks
                dialogView.findViewById(R.id.loginButton).setOnClickListener(view -> {
                    dialog.dismiss();
                    startActivity(new Intent(requireActivity(), LoginActivity.class));
                });

                dialogView.findViewById(R.id.cancelButton).setOnClickListener(button -> {
                    dialog.dismiss();
                });

                dialog.show();
                return;
            }

            // Validate address
            if (sessionManager.getAddress() == null || sessionManager.getCity() == null ||
                    sessionManager.getProvince() == null || sessionManager.getPostalCode() == null) {
                Toasty.warning(requireContext(), "Please set your delivery address").show();
                return;
            }

            // Show confirmation dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Order")
                    .setMessage("Proceed with payment?")
                    .setPositiveButton("Yes", (dialog, which) -> handleCheckout())
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void handleCheckout() {
        // Validate payment method selection
        if (selectedPaymentMethod == null) {
            Toasty.error(requireContext(), "Please select a payment method").show();
            return;
        }

        // Validate required fields first
        String address = binding.addressInput.getText().toString().trim();
        String city = binding.citySpinner.getText().toString().trim();
        String province = binding.provinceSpinner.getText().toString().trim();
        String postalCode = binding.postalCodeInput.getText().toString().trim();

        if (address.isEmpty() || city.isEmpty() || province.isEmpty() || postalCode.isEmpty()) {
            Toasty.error(requireContext(), "Please fill in all shipping details").show();
            return;
        }

        String totalText = binding.totalText.getText().toString();
        String numericTotal = totalText.replaceAll("[^0-9]", "");
        int totalAmount = Integer.parseInt(numericTotal);

        // Get selected courier info
        ShippingCostResponse.Cost selectedCourier = courierAdapter.getSelectedCourier();
        if (selectedCourier == null) {
            Toasty.warning(requireContext(), "Please select a courier service").show();
            return;
        }

        String courierCode = selectedCourier.service.toLowerCase();
        String[] serviceParts = selectedCourier.service.split(" ");

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("courier", serviceParts[0].toLowerCase());
        orderData.put("courier_service", selectedCourier.service);
        orderData.put("user_id", sessionManager.getUserId());
        orderData.put("shipping_address", address);
        orderData.put("shipping_city", city);
        orderData.put("shipping_province", province);
        orderData.put("shipping_postal_code", postalCode);
        orderData.put("shipping_cost", shippingCost);
        orderData.put("total_amount", totalAmount);
        orderData.put("payment_method", selectedPaymentMethod);

        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (CartItem item : adapter.getItems()) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("product_id", item.getProduct().getId());
            itemMap.put("quantity", item.getQuantity());
            if (item.getVariant() != null) {
                itemMap.put("variant_id", item.getVariant().getId());
            }
            itemsList.add(itemMap);
        }
        orderData.put("items", new Gson().toJson(itemsList));

        if (activeCoupon != null) {
            orderData.put("coupon_code", activeCoupon.getCode());
        }

        Log.d("OrderDebug", "Sending order request: " + new Gson().toJson(orderData));

        ApiClient.getClient().createOrder(orderData).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OrderResponse orderResponse = response.body();
                    int orderId = orderResponse.getOrder().getId();

                    if ("online".equals(selectedPaymentMethod)) {
                        // Create payment request for Midtrans
                        Map<String, Object> paymentRequest = new HashMap<>();
                        paymentRequest.put("order_id", orderId);
                        paymentRequest.put("user_id", sessionManager.getUserId());
                        paymentRequest.put("total_amount", totalAmount);

                        // Add shipping info
                        paymentRequest.put("shipping_address", address);
                        paymentRequest.put("shipping_city", city);
                        paymentRequest.put("shipping_postal_code", postalCode);

                        // Customer details
                        paymentRequest.put("customer_name", sessionManager.getFullname());
                        paymentRequest.put("customer_email", sessionManager.getEmail());
                        paymentRequest.put("customer_phone", sessionManager.getPhone() != null ? 
                            sessionManager.getPhone() : "");

                        // Add item details
                        List<Map<String, Object>> itemDetails = new ArrayList<>();
                        for (CartItem item : adapter.getItems()) {
                            Map<String, Object> itemDetail = new HashMap<>();
                            itemDetail.put("id", item.getProduct().getId());
                            itemDetail.put("name", item.getProduct().getTitle());
                            itemDetail.put("price", item.getVariant() != null ? 
                                item.getVariant().getPrice() : item.getProduct().getPrice());
                            itemDetail.put("quantity", item.getQuantity());
                            itemDetails.add(itemDetail);
                        }
                        paymentRequest.put("items", itemDetails);

                        Log.d("PaymentDebug", "Sending payment request: " + new Gson().toJson(paymentRequest));

                        ApiClient.getClient().createPayment(paymentRequest).enqueue(new Callback<PaymentResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<PaymentResponse> call, @NonNull Response<PaymentResponse> response) {
                                if (response.isSuccessful() && response.body() != null 
                                    && response.body().getData() != null
                                    && response.body().getData().getPaymentUrl() != null) {
                                    
                                    String paymentUrl = response.body().getData().getPaymentUrl();
                                    Log.d("PaymentDebug", "Got payment URL: " + paymentUrl);
                                    
                                    Bundle args = new Bundle();
                                    args.putString("payment_url", paymentUrl);
                                    Navigation.findNavController(requireView())
                                            .navigate(R.id.action_navigation_detail_pesanan_to_navigation_payment_webview, args);
                                } else {
                                    String errorMsg = "Failed to initialize payment";
                                    if (response.errorBody() != null) {
                                        try {
                                            errorMsg = response.errorBody().string();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    Log.e("PaymentDebug", "Payment error: " + errorMsg);
                                    Toasty.error(requireContext(), errorMsg).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<PaymentResponse> call, @NonNull Throwable t) {
                                Log.e("PaymentDebug", "Payment network error: " + t.getMessage());
                                Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
                            }
                        });
                    } else {
                        // For COD, show success message and navigate to order detail
                        Toasty.success(requireContext(), "Order placed successfully with COD payment").show();
                        Bundle args = new Bundle();
                        args.putInt("order_id", orderId);
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_navigation_detail_pesanan_to_navigation_order_history, args);
                        
                        // Clear cart after successful order
                        cartManager.clearCart(new CartManager.CartCallback() {
                            @Override
                            public void onSuccess(List<CartItem> items) {
                                Log.d("CartDebug", "Cart cleared successfully");
                            }

                            @Override
                            public void onError(String message) {
                                Log.e("CartDebug", "Failed to clear cart: " + message);
                                if (isAdded()) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toasty.error(requireContext(), "Failed to clear cart: " + message).show();
                                    });
                                }
                            }
                        });
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            ErrorResponse error = new Gson().fromJson(errorBody, ErrorResponse.class);
                            Toasty.error(requireContext(), error.getMessage()).show();
                        }
                    } catch (IOException e) {
                        Toasty.error(requireContext(), "Failed to create order").show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
            }
        });
    }

    private int calculatePrice(int basePrice, int discount, int quantity) {
        int discountedPrice = basePrice - (basePrice * discount / 100);
        return discountedPrice * quantity;
    }

    private int calculateTotalWeight() {
        int totalWeight = 0;
        for (CartItem item : adapter.getItems()) {
            totalWeight += item.getProduct().getWeight() * item.getQuantity();
        }
        return Math.max(totalWeight, 1000); // Minimum 1kg
    }

    private void setupShippingAddress() {
        // Setup payment method
        binding.paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.onlinePaymentRadio) {
                selectedPaymentMethod = "online";
            } else if (checkedId == R.id.codPaymentRadio) {
                selectedPaymentMethod = "cod";
            }
        });

        // Setup province spinner
        provinceAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        binding.provinceSpinner.setAdapter(provinceAdapter);

        // Setup city spinner
        cityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        binding.citySpinner.setAdapter(cityAdapter);

        // Province selection listener
        binding.provinceSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProvince = provinceAdapter.getItem(position);
            binding.citySpinner.setText("", false); // Clear city selection
            cityList.clear(); // Clear previous cities
            cityAdapter.clear();

            for (Province province : provinceList) {
                if (province.getName().equals(selectedProvince)) {
                    selectedProvinceId = province.getId();
                    loadCities(selectedProvinceId);
                    break;
                }
            }
        });

        // City selection listener
        binding.citySpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = cityAdapter.getItem(position);
            for (City city : cityList) {
                if (city.getName().equals(selectedCity)) {
                    selectedCityId = city.getId();
                    calculateTotalWeight();
                    loadShippingOptions();
                    break;
                }
            }
        });

        // Add text change listeners for address and postal code
        binding.addressInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (selectedCityId != null) {
                    loadShippingOptions();
                }
            }
        });

        binding.postalCodeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (selectedCityId != null) {
                    loadShippingOptions();
                }
            }
        });

        // Pre-fill existing address if available
        if (sessionManager.getAddress() != null) {
            binding.addressInput.setText(sessionManager.getAddress());
        }
        if (sessionManager.getPostalCode() != null) {
            binding.postalCodeInput.setText(sessionManager.getPostalCode());
        }
    }

    private void loadProvinces() {
        ApiClient.getClient().getProvinces().enqueue(new Callback<ProvinceResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProvinceResponse> call,
                                   @NonNull Response<ProvinceResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null &&
                            response.body().rajaongkir != null &&
                            response.body().rajaongkir.getProvinces() != null) {

                        provinceList.clear();
                        provinceList.addAll(response.body().rajaongkir.getProvinces());

                        List<String> provinceNames = new ArrayList<>();
                        for (Province province : provinceList) {
                            provinceNames.add(province.getName());
                        }

                        provinceAdapter.clear();
                        provinceAdapter.addAll(provinceNames);
                        provinceAdapter.notifyDataSetChanged();
                    } else {
                        // Handle error response
                        if (response.code() == 500) {
                            // Use fallback data when API limit is reached
                            loadFallbackProvinces();
                        } else {
                            String error = "";
                            if (response.errorBody() != null) {
                                error = response.errorBody().string();
                            }
                            Log.e("ProvinceError", "Error loading provinces: " + error);
                            Toasty.error(requireContext(), "Failed to load provinces. Please try again later.").show();
                        }
                    }
                } catch (Exception e) {
                    Log.e("ProvinceError", "Error processing province response", e);
                    Toasty.error(requireContext(), "Error loading provinces: " + e.getMessage()).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProvinceResponse> call, @NonNull Throwable t) {
                Log.e("ProvinceError", "Network error loading provinces", t);
                loadFallbackProvinces();
                Toasty.error(requireContext(), "Network error. Using cached data.").show();
            }
        });
    }

    private void loadCities(String provinceId) {
        ApiClient.getClient().getCities(provinceId).enqueue(new Callback<CityResponse>() {
            @Override
            public void onResponse(@NonNull Call<CityResponse> call,
                                   @NonNull Response<CityResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().rajaongkir != null &&
                        response.body().rajaongkir.getCities() != null) {

                    cityList.clear();
                    cityList.addAll(response.body().rajaongkir.getCities());

                    List<String> cityNames = new ArrayList<>();
                    for (City city : cityList) {
                        cityNames.add(city.getName());
                    }

                    cityAdapter.clear();
                    cityAdapter.addAll(cityNames);
                    cityAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Failed to load cities: " + t.getMessage()).show();
            }
        });
    }

    private int calculateSubtotal() {
        int subtotal = 0;
        for (CartItem item : adapter.getItems()) {
            if (item.getVariant() != null) {
                subtotal += calculatePrice(
                    item.getVariant().getPrice(),
                    item.getVariant().getDiscount(),
                    item.getQuantity()
                );
            } else {
                subtotal += calculatePrice(
                    item.getProduct().getPrice(),
                    item.getProduct().getDiscount(),
                    item.getQuantity()
                );
            }
        }
        return subtotal;
    }

    private void loadFallbackProvinces() {
        // Data provinsi fallback
        List<String> provinces = new ArrayList<>();
        provinces.add("DKI Jakarta");
        provinces.add("Jawa Barat");
        provinces.add("Jawa Tengah");
        provinces.add("Jawa Timur");
        provinces.add("DI Yogyakarta");
        provinces.add("Banten");
        provinces.add("Bali");
        // Tambahkan provinsi lainnya sesuai kebutuhan

        provinceAdapter.clear();
        provinceAdapter.addAll(provinces);
        provinceAdapter.notifyDataSetChanged();

        // Simulasi Province ID untuk setiap provinsi
        Map<String, String> provinceIds = new HashMap<>();
        provinceIds.put("DKI Jakarta", "6");
        provinceIds.put("Jawa Barat", "9");
        provinceIds.put("Jawa Tengah", "10");
        provinceIds.put("Jawa Timur", "11");
        provinceIds.put("DI Yogyakarta", "5");
        provinceIds.put("Banten", "36");
        provinceIds.put("Bali", "1");

        // Ketika provinsi dipilih
        binding.provinceSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProvince = provinceAdapter.getItem(position);
            selectedProvinceId = provinceIds.get(selectedProvince);
            loadFallbackCities(selectedProvinceId);
        });
    }

    private void loadFallbackCities(String provinceId) {
        // Data kota fallback berdasarkan provinsi
        Map<String, List<String>> citiesMap = new HashMap<>();
        citiesMap.put("6", Arrays.asList("Jakarta Pusat", "Jakarta Utara", "Jakarta Barat", "Jakarta Selatan", "Jakarta Timur"));
        citiesMap.put("9", Arrays.asList("Bandung", "Bekasi", "Bogor", "Depok", "Cimahi"));
        citiesMap.put("10", Arrays.asList("Semarang", "Solo", "Magelang", "Pekalongan", "Tegal"));
        citiesMap.put("11", Arrays.asList("Surabaya", "Malang", "Sidoarjo", "Kediri", "Madiun"));
        citiesMap.put("5", Arrays.asList("Yogyakarta", "Bantul", "Sleman", "Kulon Progo", "Gunung Kidul"));
        citiesMap.put("36", Arrays.asList("Serang", "Tangerang", "Cilegon", "Tangerang Selatan"));
        citiesMap.put("1", Arrays.asList("Denpasar", "Badung", "Gianyar", "Tabanan", "Klungkung"));

        // Simulasi City ID untuk setiap kota
        Map<String, String> cityIds = new HashMap<>();
        // Jakarta
        cityIds.put("Jakarta Pusat", "152");
        cityIds.put("Jakarta Utara", "153");
        cityIds.put("Jakarta Barat", "154");
        cityIds.put("Jakarta Selatan", "155");
        cityIds.put("Jakarta Timur", "156");
        // Jawa Barat
        cityIds.put("Bandung", "22");
        cityIds.put("Bekasi", "55");
        cityIds.put("Bogor", "79");
        cityIds.put("Depok", "115");
        cityIds.put("Cimahi", "107");
        // Jawa Tengah
        cityIds.put("Semarang", "399");
        cityIds.put("Solo", "445");
        cityIds.put("Magelang", "232");
        cityIds.put("Pekalongan", "347");
        cityIds.put("Tegal", "450");
        // Jawa Timur
        cityIds.put("Surabaya", "444");
        cityIds.put("Malang", "255");
        cityIds.put("Sidoarjo", "409");
        cityIds.put("Kediri", "174");
        cityIds.put("Madiun", "247");
        // DI Yogyakarta
        cityIds.put("Yogyakarta", "501");
        cityIds.put("Bantul", "35");
        cityIds.put("Sleman", "419");
        cityIds.put("Kulon Progo", "203");
        cityIds.put("Gunung Kidul", "135");
        // Banten
        cityIds.put("Serang", "402");
        cityIds.put("Tangerang", "456");
        cityIds.put("Cilegon", "106");
        cityIds.put("Tangerang Selatan", "457");
        // Bali
        cityIds.put("Denpasar", "114");
        cityIds.put("Badung", "17");
        cityIds.put("Gianyar", "128");
        cityIds.put("Tabanan", "447");
        cityIds.put("Klungkung", "190");

        List<String> cities = citiesMap.get(provinceId);
        if (cities != null) {
            cityAdapter.clear();
            cityAdapter.addAll(cities);
            cityAdapter.notifyDataSetChanged();

            binding.citySpinner.setOnItemClickListener((parent, view, position, id) -> {
                String selectedCity = cityAdapter.getItem(position);
                selectedCityId = cityIds.getOrDefault(selectedCity, "501"); // Default ke Yogyakarta jika tidak ditemukan
            });
        }
    }
}