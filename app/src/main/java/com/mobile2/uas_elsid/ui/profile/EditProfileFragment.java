package com.mobile2.uas_elsid.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mobile2.uas_elsid.LoginActivity;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.UserResponse;
import com.mobile2.uas_elsid.databinding.FragmentEditProfileBinding;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class EditProfileFragment extends Fragment {
    private FragmentEditProfileBinding binding;
    private SessionManager sessionManager;
    private static final int PICK_IMAGE_REQUEST = 1;
    private String selectedImagePath;
    private boolean isFragmentActive = true;
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());
        binding.avatarImage.setOnClickListener(v -> checkPermissionAndPickImage());

        // Debug current session state
        String userId = sessionManager.getUserId();
        boolean isLoggedIn = sessionManager.isLoggedIn();

        System.out.println("Debug - EditProfile State:");
        System.out.println("UserId: [" + userId + "]");
        System.out.println("IsLoggedIn: " + isLoggedIn);

        if (!isLoggedIn || userId == null || userId.trim().isEmpty()) {
            System.out.println("Debug - Invalid session detected");
            Toasty.error(requireContext(), "Session invalid, please login again").show();
            sessionManager.logout();
            startActivity(new Intent(requireActivity(), LoginActivity.class));
            requireActivity().finish();
            return binding.getRoot();
        }

        setupUI();
        setupUserData();
        return binding.getRoot();
    }

    private void checkPermissionAndPickImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        } else {
            if (requireContext().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        }
    }

    private void saveUserData() {
        String userId = sessionManager.getUserId();
        String email = sessionManager.getEmail();

        // Validate user ID
        if (userId == null || userId.isEmpty()) {
            Toasty.error(requireContext(), "Invalid session").show();
            return;
        }

        // Create request map
        Map<String, String> updateData = new HashMap<>();
        updateData.put("email", email);
        updateData.put("fullname", binding.fullnameInput.getText().toString().trim());
        updateData.put("phone", binding.phoneInput.getText().toString().trim());
        updateData.put("address", binding.addressInput.getText().toString().trim());
        updateData.put("province", binding.provinceInput.getText().toString().trim());
        updateData.put("city", binding.cityInput.getText().toString().trim());
        updateData.put("postal_code", binding.postalCodeInput.getText().toString().trim());

        // Show loading
        binding.loadingIndicator.setVisibility(View.VISIBLE);

        // Make API call dengan userId
        ApiClient.getClient().updateUser(userId, updateData)
                .enqueue(new Callback<UserResponse>() {
                    @Override
                    public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                        if (!isFragmentActive || binding == null) return;
                        binding.loadingIndicator.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            UserResponse userResponse = response.body();
                            if (userResponse.getStatus() == 1) {
                                // Update session data
                                sessionManager.updateProfile(
                                        updateData.get("fullname"),
                                        updateData.get("phone"),
                                        updateData.get("address"),
                                        updateData.get("city"),
                                        updateData.get("province"),
                                        updateData.get("postal_code"),
                                        null, // provinceId not needed
                                        null  // cityId not needed
                                );
                                Toasty.success(requireContext(), "Profile updated successfully").show();
                                Navigation.findNavController(binding.getRoot()).navigateUp();
                            } else {
                                Toasty.error(requireContext(), userResponse.getMessage()).show();
                            }
                        } else {
                            Toasty.error(requireContext(), "Failed to update profile").show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserResponse> call, Throwable t) {
                        if (!isFragmentActive || binding == null) return;
                        binding.loadingIndicator.setVisibility(View.GONE);
                        Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
                    }
                });
    }

    private void uploadAvatar(String userId) {
        if (selectedImagePath == null) {
            Toasty.error(requireContext(), "No image selected").show();
            return;
        }

        File file = new File(selectedImagePath);
        if (!file.exists()) {
            Toasty.error(requireContext(), "File not found").show();
            return;
        }

        binding.loadingIndicator.setVisibility(View.VISIBLE);

        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), userId);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part avatarPart = MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);

        ApiClient.getClient().updateAvatar(userIdBody, avatarPart)
                .enqueue(new Callback<UserResponse>() {
                    @Override
                    public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                        if (!isFragmentActive) return;
                        binding.loadingIndicator.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            UserResponse userResponse = response.body();
                            if (userResponse.getStatus() == 1 && userResponse.getUser() != null) {
                                String avatarPath = userResponse.getUser().getAvatar();
                                // Simpan path avatar baru
                                sessionManager.saveAvatarPath(avatarPath);

                                // Refresh avatar di EditProfileFragment
                                loadImageFromPath(avatarPath);

                                // Refresh avatar di ProfileFragment
                                if (getActivity() != null) {
                                    Fragment parentFragment = getParentFragmentManager()
                                            .findFragmentById(R.id.nav_host_fragment_activity_home);
                                    if (parentFragment != null) {
                                        Fragment profileFragment = parentFragment
                                                .getChildFragmentManager()
                                                .findFragmentByTag("ProfileFragment");
                                        if (profileFragment instanceof ProfileFragment) {
                                            ((ProfileFragment) profileFragment).refreshAvatar();
                                        }
                                    }
                                }

                                Toasty.success(requireContext(), "Avatar updated successfully").show();
                            } else {
                                Toasty.error(requireContext(), userResponse.getMessage()).show();
                            }
                        } else {
                            Toasty.error(requireContext(), "Failed to update avatar").show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserResponse> call, Throwable t) {
                        if (!isFragmentActive) return;
                        binding.loadingIndicator.setVisibility(View.GONE);
                        Toasty.error(requireContext(), "Network error: " + t.getMessage()).show();
                    }
                });
    }

    public void refreshAvatar() {
        String avatarPath = sessionManager.getAvatarPath();
        if (avatarPath != null && !avatarPath.isEmpty()) {
            String baseUrl = "https://apilumenmobileuas.ndp.my.id/";
            String fullUrl = avatarPath.startsWith("http") ? avatarPath : baseUrl + avatarPath;

            // Debug log
            System.out.println("Refreshing avatar from: " + fullUrl);

            Glide.with(requireContext())
                    .load(fullUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.avatarImage);
        }
    }

    private void setupUI() {
        binding.saveButton.setOnClickListener(v -> saveUserData());
        binding.avatarImage.setOnClickListener(v -> openImagePicker());
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupUserData() {
        // Populate fields with existing user data
        binding.fullnameInput.setText(sessionManager.getFullname());
        binding.phoneInput.setText(sessionManager.getPhone());
        binding.addressInput.setText(sessionManager.getAddress());
        binding.provinceInput.setText(sessionManager.getProvince());
        binding.cityInput.setText(sessionManager.getCity());
        binding.postalCodeInput.setText(sessionManager.getPostalCode());

        // Load avatar if exists
        String avatarPath = sessionManager.getAvatarPath();
        System.out.println("Current avatar path: " + avatarPath); // Debug log

        if (avatarPath != null && !avatarPath.isEmpty()) {
            loadImageFromPath(avatarPath);
        } else {
            System.out.println("No avatar path found in session"); // Debug log
        }
    }

    private void setupAvatarClick() {
        binding.avatarImage.setOnClickListener(v -> openImagePicker());

        String avatarPath = sessionManager.getAvatarPath();
        if (!avatarPath.isEmpty()) {
            loadImageFromPath(avatarPath);
        }
    }
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    // Show selected image in ImageView
                    Glide.with(this)
                            .load(selectedImageUri)
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar)
                            .into(binding.avatarImage);

                    // Get file path for upload
                    selectedImagePath = getRealPathFromUri(selectedImageUri);
                    if (selectedImagePath != null) {
                        uploadAvatar(sessionManager.getUserId());
                    } else {
                        Toasty.error(requireContext(), "Failed to get image path").show();
                    }
                } catch (Exception e) {
                    Toasty.error(requireContext(), "Error: " + e.getMessage()).show();
                }
            }
        }
    }
    private String getRealPathFromUri(Uri uri) {
        try {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {MediaStore.Images.Media.DATA};
                try (Cursor cursor = requireContext().getContentResolver()
                        .query(uri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        return cursor.getString(columnIndex);
                    }
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
            Toasty.error(requireContext(), "Error getting file path: " + e.getMessage()).show();
        }
        return null;
    }

    private void loadImageFromPath(String path) {
        if (path != null && !path.isEmpty()) {
            String fullUrl = path.startsWith("http") ?
                    path : "https://apilumenmobileuas.ndp.my.id/" + path;

            System.out.println("Loading avatar from: " + fullUrl); // Debug log

            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .skipMemoryCache(true) // Skip cache
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Skip disk cache
                    .into(binding.avatarImage);
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );
    }

    private void loadUserData() {
        binding.fullnameInput.setText(sessionManager.getFullname());
        binding.phoneInput.setText(sessionManager.getPhone());
        binding.addressInput.setText(sessionManager.getAddress());
        binding.cityInput.setText(sessionManager.getCity());
        binding.provinceInput.setText(sessionManager.getProvince());
        binding.postalCodeInput.setText(sessionManager.getPostalCode());
    }

    private void setupSaveButton() {
        binding.saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveUserData();
                Navigation.findNavController(v).navigateUp();
                Toasty.success(requireContext(), "Profile updated successfully").show();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (binding.fullnameInput.getText().toString().trim().isEmpty()) {
            binding.fullnameContainer.setError("Full name is required");
            isValid = false;
        } else {
            binding.fullnameContainer.setError(null);
        }

        if (binding.phoneInput.getText().toString().trim().isEmpty()) {
            binding.phoneContainer.setError("Phone number is required");
            isValid = false;
        } else {
            binding.phoneContainer.setError(null);
        }

        if (binding.provinceInput.getText().toString().trim().isEmpty()) {
            binding.provinceContainer.setError("Province is required");
            isValid = false;
        } else {
            binding.provinceContainer.setError(null);
        }

        if (binding.cityInput.getText().toString().trim().isEmpty()) {
            binding.cityContainer.setError("City is required");
            isValid = false;
        } else {
            binding.cityContainer.setError(null);
        }

        return isValid;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        binding = null;
    }
}

