package com.mobile2.uas_elsid.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

import com.mobile2.uas_elsid.HomeActivity;
import com.mobile2.uas_elsid.MainActivity;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.UserResponse;
import com.mobile2.uas_elsid.api.response.request.LoginRequest;
import com.mobile2.uas_elsid.databinding.FragmentLoginBinding;
import com.mobile2.uas_elsid.model.User;
import com.mobile2.uas_elsid.utils.SessionManager;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);

        binding.loginButton.setOnClickListener(v -> attemptLogin());

        return binding.getRoot();
    }

    private void attemptLogin() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toasty.error(requireContext(), "Please fill all fields").show();
            return;
        }

        LoginRequest loginRequest = new LoginRequest(email, password);
        ApiClient.getClient().login(loginRequest).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse apiResponse = response.body();
                    if (apiResponse.getStatus() == 1 && apiResponse.getUser() != null) {
                        User user = apiResponse.getUser();

                        // Debug log
                        System.out.println("Debug - Login Response:");
                        System.out.println("User ID: " + user.getUserId());
                        System.out.println("Fullname: " + user.getFullname());

                        // Create session
                        SessionManager sessionManager = new SessionManager(requireContext());
                        sessionManager.createLoginSession(
                                user.getUserId(),
                                user.getFullname(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getAddress(),
                                user.getAvatar()
                        );

                        // Verify session
                        if (sessionManager.getUserId() == null || sessionManager.getUserId().isEmpty()) {
                            Toasty.error(requireContext(), "Failed to save session").show();
                            return;
                        }

                        Toasty.success(requireContext(), apiResponse.getMessage()).show();
                        startActivity(new Intent(requireActivity(), HomeActivity.class));
                        requireActivity().finish();
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