package com.mobile2.uas_elsid.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mobile2.uas_elsid.LoginActivity;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.UserResponse;
import com.mobile2.uas_elsid.databinding.FragmentProfileBinding;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SessionManager sessionManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        // Check login status
        if (!sessionManager.isLoggedIn()) {
            View dialogView = getLayoutInflater().inflate(R.layout.layout_login_required_dialog, null);
            TextView messageText = dialogView.findViewById(R.id.loginMessageText);
            messageText.setText("Please login to view your profile");
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();

            // Set transparent background for rounded corners
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Setup button clicks
            dialogView.findViewById(R.id.loginButton).setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(requireActivity(), LoginActivity.class));
                requireActivity().finish();
            });

            dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> {
                dialog.dismiss();
                requireActivity().onBackPressed();
            });

            dialog.show();
        }

        setupUserInfo();
        setupButtons();

        return binding.getRoot();
    }

    private void setupUserInfo() {
        binding.fullnameText.setText(sessionManager.getFullname());
        binding.emailText.setText(sessionManager.getEmail());
        binding.phoneText.setText(sessionManager.getPhone());
        binding.addressText.setText(sessionManager.getAddress());

        // refresh avatar
        refreshAvatar();

//        // Load avatar
//        String avatarPath = sessionManager.getAvatarPath();
//        if (avatarPath != null && !avatarPath.isEmpty()) {
//            // Construct full URL if path is not already a complete URL
//            String fullUrl = avatarPath.startsWith("http") ?
//                    avatarPath : "https://apilumenmobileuaslinux.ndp.my.id/" + avatarPath;
//
//            System.out.println("Loading profile avatar from: " + fullUrl); // Debug log
//
//            Glide.with(this)
//                    .load(fullUrl)
//                    .placeholder(R.drawable.default_avatar)
//                    .error(R.drawable.default_avatar)
//                    .skipMemoryCache(true) // Skip memory cache
//                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Skip disk cache
//                    .into(binding.avatarImage);
//        }
        // Load avatar
        String avatarPath = sessionManager.getAvatarPath();
        if (avatarPath != null && !avatarPath.isEmpty()) {
            String baseUrl = "https://apilumenmobileuas.ndp.my.id/";
            String fullUrl = avatarPath.startsWith("http") ? avatarPath : baseUrl + avatarPath;

            // Debug log
            System.out.println("Loading avatar from: " + fullUrl);

            Glide.with(requireContext())
                    .load(fullUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable caching
                    .skipMemoryCache(true) // Skip memory cache
                    .into(binding.avatarImage);
        } else {
            // Set default avatar if no path
            binding.avatarImage.setImageResource(R.drawable.default_avatar);
        }
    }

    private void setupButtons() {
        binding.editProfileButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_edit_profile)
        );

        binding.wishlistButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_wishlist)
        );

        binding.aboutButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_about)
        );

        binding.changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        binding.orderHistoryButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_order_history)
        );

        binding.logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(requireActivity(), LoginActivity.class));
            requireActivity().finish();
        });
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        TextInputEditText oldPasswordInput = dialogView.findViewById(R.id.oldPasswordInput);
        TextInputEditText newPasswordInput = dialogView.findViewById(R.id.newPasswordInput);
        TextInputEditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPasswordInput);
        MaterialButton changeButton = dialogView.findViewById(R.id.changePasswordButton);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        changeButton.setOnClickListener(v -> {
            String oldPassword = oldPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            // Validasi input
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toasty.error(requireContext(), "Please fill all fields").show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toasty.error(requireContext(), "New passwords don't match").show();
                return;
            }

            // Set loading state
            progressBar.setVisibility(View.VISIBLE);
            changeButton.setEnabled(false);

            // Buat request 
            Map<String, Object> request = new HashMap<>();
            request.put("user_id", sessionManager.getUserId());
            request.put("old_password", oldPassword);
            request.put("new_password", newPassword); 
            request.put("confirm_password", confirmPassword);

            // Panggil API
            ApiClient.getClient().changePassword(request).enqueue(new Callback<UserResponse>() {
                @Override
                public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                    progressBar.setVisibility(View.GONE);
                    changeButton.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        UserResponse apiResponse = response.body();
                        if (apiResponse.getStatus() == 1) {
                            dialog.dismiss();
                            Toasty.success(requireContext(), apiResponse.getMessage()).show();
                            
                            // Logout user after successful password change
                            sessionManager.logout();
                            startActivity(new Intent(requireActivity(), LoginActivity.class));
                            requireActivity().finish();
                        } else {
                            Toasty.error(requireContext(), apiResponse.getMessage()).show();
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ? 
                                    response.errorBody().string() : "Unknown error";
                            Toasty.error(requireContext(), errorBody).show();
                        } catch (IOException e) {
                            Toasty.error(requireContext(), "Network error").show();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    changeButton.setEnabled(true);
                    Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Show dialog
        dialog.show();
    }

    public void refreshAvatar() {
        String avatarPath = sessionManager.getAvatarPath();
        if (avatarPath != null && !avatarPath.isEmpty()) {
            String baseUrl = "https://apilumenmobileuas.ndp.my.id/";
            String fullUrl = avatarPath.startsWith("http") ? avatarPath : baseUrl + avatarPath;

            System.out.println("Refreshing avatar from: " + fullUrl);

            if (binding != null && isAdded()) {
                Glide.with(requireContext())
                        .load(fullUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(binding.avatarImage);
            }
        } else {
            if (binding != null) {
                binding.avatarImage.setImageResource(R.drawable.default_avatar);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void onResume() {
        super.onResume();
        refreshAvatar();
    }
}