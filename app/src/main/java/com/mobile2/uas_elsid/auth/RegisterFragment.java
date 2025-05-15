// RegisterFragment.java
package com.mobile2.uas_elsid.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import com.mobile2.uas_elsid.LoginActivity;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.UserResponse;
import com.mobile2.uas_elsid.api.response.request.RegisterRequest;
import com.mobile2.uas_elsid.databinding.FragmentRegisterBinding;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);

        binding.registerButton.setOnClickListener(v -> attemptRegister());

        return binding.getRoot();
    }

    private void attemptRegister() {
        String fullname = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (fullname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toasty.error(requireContext(), "Please fill all fields").show();
            return;
        }

        RegisterRequest registerRequest = new RegisterRequest(fullname, email, password);
        ApiClient.getClient().register(registerRequest).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse apiResponse = response.body();
                    if (apiResponse.getStatus() == 1) {
                        Toasty.success(requireContext(), apiResponse.getMessage()).show();
                        // Switch to login tab
                        if (getActivity() instanceof LoginActivity) {
                            ((LoginActivity) getActivity()).switchToLoginTab();
                        }
                    } else {
                        Toasty.error(requireContext(), apiResponse.getMessage()).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
            }
        });
    }
}