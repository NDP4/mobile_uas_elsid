package com.mobile2.uas_elsid.ui.checkout;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.CityResponse;
import com.mobile2.uas_elsid.api.response.ProvinceResponse;
import com.mobile2.uas_elsid.databinding.BottomSheetShippingAddressBinding;
import com.mobile2.uas_elsid.model.City;
import com.mobile2.uas_elsid.model.Province;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlternativeShippingBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetShippingAddressBinding binding;
    private BottomSheetListener listener;
    private final Map<String, String> provinceMap = new HashMap<>();
    private final Map<String, String> cityMap = new HashMap<>();
    private String selectedProvinceId;

    public interface BottomSheetListener {
        void onAddressSelected(String province, String city, String address, String postalCode,
                               String provinceId, String cityId);
    }

    public void setListener(BottomSheetListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetShippingAddressBinding.inflate(inflater, container, false);
        setupViews();
        return binding.getRoot();
    }

    private void setupViews() {
        // Setup close button
        binding.closeButton.setOnClickListener(v -> dismiss());

        // Setup province spinner
        setupProvinceSpinner();

        // Setup save button
        binding.saveAddressButton.setOnClickListener(v -> saveAddress());
    }

    private void setupProvinceSpinner() {
        ApiClient.getClient().getProvinces().enqueue(new Callback<ProvinceResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProvinceResponse> call,
                                   @NonNull Response<ProvinceResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().rajaongkir != null &&
                        response.body().rajaongkir.getProvinces() != null) {

                    List<String> provinceNames = new ArrayList<>();
                    for (Province province : response.body().rajaongkir.getProvinces()) {
                        provinceNames.add(province.getName());
                        provinceMap.put(province.getName(), province.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, provinceNames);
                    binding.provinceSpinner.setAdapter(adapter);

                    // Setup province selection listener
                    binding.provinceSpinner.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedProvince = (String) parent.getItemAtPosition(position);
                        selectedProvinceId = provinceMap.get(selectedProvince);
                        if (selectedProvinceId != null) {
                            loadCities(selectedProvinceId);
                        }
                        binding.citySpinner.setText("");
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProvinceResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Failed to load provinces").show();
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

                    List<String> cityNames = new ArrayList<>();
                    cityMap.clear();
                    for (City city : response.body().rajaongkir.getCities()) {
                        cityNames.add(city.getName());
                        cityMap.put(city.getName(), city.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, cityNames);
                    binding.citySpinner.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Failed to load cities").show();
            }
        });
    }

    private void saveAddress() {
        String province = binding.provinceSpinner.getText().toString();
        String city = binding.citySpinner.getText().toString();
        String address = binding.addressInput.getText().toString();
        String postalCode = binding.postalCodeInput.getText().toString();

        if (province.isEmpty() || city.isEmpty() || address.isEmpty() || postalCode.isEmpty()) {
            Toasty.warning(requireContext(), "Please fill in all fields").show();
            return;
        }

        String provinceId = provinceMap.get(province);
        String cityId = cityMap.get(city);

        if (provinceId == null || cityId == null) {
            Toasty.error(requireContext(), "Invalid province or city selected").show();
            return;
        }

        if (listener != null) {
            listener.onAddressSelected(province, city, address, postalCode, provinceId, cityId);
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

