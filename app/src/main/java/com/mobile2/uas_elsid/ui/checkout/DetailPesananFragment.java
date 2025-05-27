package com.mobile2.uas_elsid.ui.checkout;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
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
    private int totalWeight = 0;
    private int shippingCost = 0;
    private static final String ORIGIN_CITY = "501";
    private Coupon activeCoupon = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDetailPesananBinding.inflate(inflater, container, false);
        cartManager = CartManager.getInstance(requireContext());
        sessionManager = new SessionManager(requireContext());

        setupRecyclerView();
        loadCartItems();
        setupCouponInput();
        setupCheckoutButton();
        setupAddressSection();

        return binding.getRoot();
    }
    // rajaongkir
    private void setupAddressSection() {
        String address = sessionManager.getAddress();
        String city = sessionManager.getCity();
        String province = sessionManager.getProvince();
        String postalCode = sessionManager.getPostalCode();

        String fullAddress = String.format("%s\n%s, %s %s",
                address, city, province, postalCode);
        binding.addressText.setText(fullAddress);

        binding.changeAddressButton.setOnClickListener(v -> {
            // Show address selection dialog
            showAddressSelectionDialog();
        });
    }

    private void setupRecyclerView() {
        adapter = new DetailPesananAdapter(requireContext());
        binding.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.orderItemsRecyclerView.setAdapter(adapter);
    }

    private void loadCartItems() {
        cartManager.getCartItems(new CartManager.CartCallback() {
            @Override
            public void onSuccess(List<CartItem> items) {
                adapter.setItems(items);
                calculateTotals();
            }

            @Override
            public void onError(String message) {
                Toasty.error(requireContext(), message).show();
            }
        });
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
                            Toasty.error(requireContext(), "Failed to apply coupon: " + errorBody).show();
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


    private void calculateTotals() {
        // Hitung subtotal sekali saja
        int subtotal = calculateSubtotal();
        int discount = 0;
        totalWeight = 0;

        // Hitung total berat untuk shipping
        for (CartItem item : adapter.getItems()) {
            totalWeight += item.getProduct().getWeight() * item.getQuantity();
        }

        // Hitung diskon jika ada coupon aktif
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

        // Hitung total
        int total = subtotal + shippingCost - discount;

        // Update UI
        binding.subtotalText.setText(formatPrice(subtotal));
        binding.shippingText.setText(formatPrice(shippingCost));
        binding.totalText.setText(formatPrice(total));
        binding.bottomTotalText.setText(formatPrice(total));
    }

    // Method terpisah untuk menghitung harga per item
    private int calculatePrice(int basePrice, int discount, int quantity) {
        int discountedPrice = basePrice - (basePrice * discount / 100);
        return discountedPrice * quantity;
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
        String totaltext = binding.totalText.getText().toString();
        String numericTotal = totaltext.replaceAll("[^0-9]", "");
        int totalAmount = Integer.parseInt(numericTotal);

        // Calculate discount amount
        int discountAmount = 0;
        if (activeCoupon != null) {
            if ("percentage".equals(activeCoupon.getDiscountType())) {
                discountAmount = (int) (calculateSubtotal() * (activeCoupon.getDiscountAmount() / 100.0));
            } else {
                discountAmount = (int) activeCoupon.getDiscountAmount();
            }
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("user_id", sessionManager.getUserId());
        orderData.put("shipping_address", sessionManager.getAddress());
        orderData.put("shipping_city", sessionManager.getCity());
        orderData.put("shipping_province", sessionManager.getProvince());
        orderData.put("shipping_postal_code", sessionManager.getPostalCode());
        orderData.put("shipping_cost", shippingCost);
        orderData.put("total_amount", totalAmount);
        orderData.put("courier", "jne");
        orderData.put("courier_service", "reg");

        // Add discount information
        if (activeCoupon != null) {
            orderData.put("coupon_code", activeCoupon.getCode());
            orderData.put("discount_amount", discountAmount);
            orderData.put("subtotal", calculateSubtotal()); // Add subtotal for proper calculation
        }

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

        ApiClient.getClient().createOrder(orderData).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OrderResponse orderResponse = response.body();
                    // Get order ID from the response
                    int orderId = orderResponse.getOrder().getId();

                    // Create payment request
                    Map<String, Object> paymentRequest = new HashMap<>();
                    paymentRequest.put("order_id", orderId);

                    // Call create payment API
                    ApiClient.getClient().createPayment(paymentRequest).enqueue(new Callback<PaymentResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<PaymentResponse> call, @NonNull Response<PaymentResponse> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getData() != null
                                    && response.body().getData().getPaymentUrl() != null) {

                                String paymentUrl = response.body().getData().getPaymentUrl();
                                // Navigate to payment webview
                                Bundle args = new Bundle();
                                args.putString("payment_url", paymentUrl);
                                Navigation.findNavController(requireView())
                                        .navigate(R.id.action_navigation_detail_pesanan_to_navigation_payment_webview, args);
                            } else {
                                Toasty.error(requireContext(), "Failed to initialize payment").show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<PaymentResponse> call, @NonNull Throwable t) {
                            Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
                        }
                    });
                } else {
//                    Toasty.error(requireContext(), "Failed to create order").show();
                    if (!response.isSuccessful()) {
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                ErrorResponse error = new Gson().fromJson(errorBody, ErrorResponse.class);
                                Toasty.error(requireContext(), error.getMessage()).show();
                            } else {
                                Toasty.error(requireContext(), "Failed to create order").show();
                            }
                        } catch (IOException e) {
                            Toasty.error(requireContext(), "Failed to create order").show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
