package com.mobile2.uas_elsid.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.mobile2.uas_elsid.HomeActivity;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.ErrorResponse;
import com.mobile2.uas_elsid.api.response.UserResponse;
import com.mobile2.uas_elsid.api.response.request.LoginRequest;
import com.mobile2.uas_elsid.databinding.FragmentLoginBinding;
import com.mobile2.uas_elsid.model.User;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.io.IOException;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        binding.loginButton.setOnClickListener(v -> attemptLogin());
        return binding.getRoot();
    }

    private void attemptLogin() {
        // Reset error states
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            binding.emailLayout.setError("Email is required");
            binding.emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setError("Password is required");
            binding.passwordInput.requestFocus();
            return;
        }

        // Show loading state
        binding.loginButton.setEnabled(false);

        LoginRequest loginRequest = new LoginRequest(email, password);
        ApiClient.getClient().login(loginRequest).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                binding.loginButton.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    UserResponse apiResponse = response.body();
                    if (apiResponse.getStatus() == 1 && apiResponse.getUser() != null) {
                        User user = apiResponse.getUser();
                        SessionManager sessionManager = new SessionManager(requireContext());
                        sessionManager.createLoginSession(
                                user.getUserId(),
                                user.getFullname(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getAddress(),
                                user.getAvatar()
                        );

                        if (sessionManager.getUserId() == null || sessionManager.getUserId().isEmpty()) {
                            Toasty.error(requireContext(), "Failed to save session").show();
                            return;
                        }

                        Toasty.success(requireContext(), "Login successful").show();
                        startActivity(new Intent(requireActivity(), HomeActivity.class));
                        requireActivity().finish();
                    } else {
                        String errorMessage = apiResponse.getMessage() != null ?
                                apiResponse.getMessage() : "Invalid email or password";
                        binding.passwordLayout.setError(errorMessage);
                        binding.passwordInput.requestFocus();
                        Toasty.error(requireContext(), errorMessage).show();
                    }
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            ErrorResponse errorResponse = new Gson().fromJson(errorBody, ErrorResponse.class);
                            String errorMessage = errorResponse != null && errorResponse.getMessage() != null ?
                                    errorResponse.getMessage() : "Invalid email or password";

                            binding.passwordLayout.setError(errorMessage);
                            binding.passwordInput.requestFocus();
                            Toasty.error(requireContext(), errorMessage).show();
                        }
                    } catch (IOException e) {
                        binding.passwordLayout.setError("Network error occurred");
                        binding.passwordInput.requestFocus();
                        Toasty.error(requireContext(), "Network error occurred").show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                binding.loginButton.setEnabled(true);
                binding.passwordLayout.setError("Network error: " + t.getMessage());
                binding.passwordInput.requestFocus();
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