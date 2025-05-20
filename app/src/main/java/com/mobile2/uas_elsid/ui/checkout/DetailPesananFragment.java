package com.mobile2.uas_elsid.ui.checkout;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.material.textfield.TextInputEditText;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.DetailPesananAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.CityResponse;
import com.mobile2.uas_elsid.api.response.CouponResponse;
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
        if (activeCoupon != null) {
            // Cancel coupon
            activeCoupon = null;
            binding.couponInput.setText("");
            binding.couponInput.setEnabled(true); // Enable input when canceling
            binding.applyCouponButton.setText("Apply");
            binding.discountContainer.setVisibility(View.GONE);
            calculateTotals();
            return;
        }

        Map<String, String> request = new HashMap<>();
        request.put("code", code);
        request.put("user_id", sessionManager.getUserId());
        request.put("subtotal", String.valueOf(calculateSubtotal()));

        ApiClient.getClient().validateCoupon(request).enqueue(new Callback<CouponResponse>() {
            @Override
            public void onResponse(Call<CouponResponse> call, Response<CouponResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCoupon() != null) {
                    Coupon coupon = response.body().getCoupon();
                    int subtotal = calculateSubtotal();

                    if (subtotal >= coupon.getMinPurchase()) {
                        activeCoupon = coupon;
                        binding.applyCouponButton.setText("Cancel");
                        binding.couponInput.setEnabled(false); // Disable input when coupon applied
                        binding.discountContainer.setVisibility(View.VISIBLE);
                        calculateTotals();
                        Toasty.success(requireContext(), "Coupon applied successfully!").show();
                    } else {
                        Toasty.error(requireContext(),
                                "Minimum purchase amount: " + formatPrice(coupon.getMinPurchase())).show();
                    }
                }  else {
                    // Simplified error message
                    Toasty.error(requireContext(), "Invalid coupon code").show();
                }
            }

            @Override
            public void onFailure(Call<CouponResponse> call, Throwable t) {
                Toasty.error(requireContext(), "Failed to validate coupon: " + t.getMessage()).show();
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
        int subtotal = calculateSubtotal();
        int discount = 0;
//        int subtotal = 0;
        totalWeight = 0;

        if (activeCoupon != null) {
            if ("percentage".equals(activeCoupon.getDiscountType())) {
                discount = (int) (subtotal * (activeCoupon.getDiscountAmount() / 100.0));
            } else {
                discount = (int) activeCoupon.getDiscountAmount();
            }
            binding.discountText.setText("-" + formatPrice(discount));
        }

        for (CartItem item : adapter.getItems()) {
            int itemPrice;
            if (item.getVariant() != null) {
                itemPrice = calculatePrice(
                        item.getVariant().getPrice(),
                        item.getVariant().getDiscount(),
                        item.getQuantity()
                );
                totalWeight += item.getProduct().getWeight() * item.getQuantity();
            } else {
                itemPrice = calculatePrice(
                        item.getProduct().getPrice(),
                        item.getProduct().getDiscount(),
                        item.getQuantity()
                );
                totalWeight += item.getProduct().getWeight() * item.getQuantity();
            }
            subtotal += itemPrice;
        }

        int total = subtotal + shippingCost - discount;

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
    private int calculatePrice(int basePrice, int discount, int quantity) {
        int discountedPrice = basePrice - (basePrice * discount / 100);
        return discountedPrice * quantity;
    }
    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Remove ",00"
    }
    private void showAddressSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_address_selection, null);
        builder.setView(dialogView);

        AutoCompleteTextView provinceSpinner = dialogView.findViewById(R.id.provinceSpinner);
        AutoCompleteTextView citySpinner = dialogView.findViewById(R.id.citySpinner);
        AutoCompleteTextView courierSpinner = dialogView.findViewById(R.id.courierSpinner);
        TextInputEditText addressInput = dialogView.findViewById(R.id.addressInput);

        // Setup adapters
        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line);
        provinceSpinner.setAdapter(provinceAdapter);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line);
        citySpinner.setAdapter(cityAdapter);

        // Reset all spinners and inputs
        provinceSpinner.setText("", false);
        citySpinner.setText("", false);
        courierSpinner.setText("", false);
        addressInput.setText("");

        provinceAdapter.clear();
        cityAdapter.clear();
        provinceAdapter.notifyDataSetChanged();
        cityAdapter.notifyDataSetChanged();

        // Setup courier adapter
        ArrayAdapter<String> courierAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"JNE", "POS", "TIKI"});
        courierSpinner.setAdapter(courierAdapter);

        // Simpan list dan selected values
        List<Province> provinceList = new ArrayList<>();
        List<City> cityList = new ArrayList<>();
        final String[] selectedCourier = {"jne"}; // Default courier menggunakan arry untuk mutable string
        final String[] selectedCityId = {""};

        // Load provinces
        ApiClient.getClient().getProvinces().enqueue(new Callback<ProvinceResponse>() {
            @Override
            public void onResponse(Call<ProvinceResponse> call, Response<ProvinceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().rajaongkir != null && response.body().rajaongkir.getProvinces() != null) {
                        provinceList.clear();
                        provinceList.addAll(response.body().rajaongkir.getProvinces());

                        List<String> provinceNames = new ArrayList<>();
                        for (Province province : provinceList) {
                            provinceNames.add(province.getName());
                        }

                        provinceAdapter.clear();
                        provinceAdapter.addAll(provinceNames);
                        provinceAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<ProvinceResponse> call, Throwable t) {
                Toasty.error(requireContext(), "Failed to load provinces: " + t.getMessage()).show();
            }
        });
        // Province selection listener
        provinceSpinner.setOnItemClickListener((parent, view, position, id) -> {
            citySpinner.setText("", false);
            cityAdapter.clear();
            cityAdapter.notifyDataSetChanged();
            selectedCityId[0] = "";
            courierSpinner.setText("", false);

            String provinceId = provinceList.get(position).getId();
            ApiClient.getClient().getCities(provinceId).enqueue(new Callback<CityResponse>() {
                @Override
                public void onResponse(Call<CityResponse> call, Response<CityResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().rajaongkir != null && response.body().rajaongkir.getCities() != null) {
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
                }

                @Override
                public void onFailure(Call<CityResponse> call, Throwable t) {
                    Toasty.error(requireContext(), "Failed to load cities: " + t.getMessage()).show();
                }
            });
        });

        // City selection listener
        citySpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedCityId[0] = cityList.get(position).getId();
            if (!courierSpinner.getText().toString().isEmpty()) {
                calculateShipping(ORIGIN_CITY, selectedCityId[0], totalWeight, selectedCourier[0]);
            }
        });

        // Courier selection listener
        courierSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedCourier[0] = courierAdapter.getItem(position).toLowerCase();
            if (!selectedCityId[0].isEmpty()) {
                calculateShipping(ORIGIN_CITY, selectedCityId[0], totalWeight, selectedCourier[0]);
            }
        });

        // Set current values if available
//        if (sessionManager.getAddress() != null) {
//            addressInput.setText(sessionManager.getAddress());
//        }
//        if (sessionManager.getProvince() != null) {
//            provinceSpinner.setText(sessionManager.getProvince(), false);
//        }
//        if (sessionManager.getCity() != null) {
//            citySpinner.setText(sessionManager.getCity(), false);
//        }

        // Set dialog buttons
        builder.setTitle("Select Delivery Address")
                .setPositiveButton("Save", (dialog, which) -> {
                    String address = addressInput.getText().toString();
                    String city = citySpinner.getText().toString();
                    String province = provinceSpinner.getText().toString();
                    String courier = courierSpinner.getText().toString();

                    if (address.isEmpty() || city.isEmpty() || province.isEmpty() || courier.isEmpty()) {
                        Toasty.error(requireContext(), "Please fill all fields").show();
                        return;
                    }

                    sessionManager.updateProfile(
                            sessionManager.getFullname(),
                            sessionManager.getPhone(),
                            address,
                            city,
                            province,
                            sessionManager.getPostalCode()
                    );

                    setupAddressSection();
                    calculateShipping(ORIGIN_CITY, selectedCityId[0], totalWeight, selectedCourier[0]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void setupCheckoutButton() {
        binding.checkoutButton.setOnClickListener(v -> {
            // Implement checkout process
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}