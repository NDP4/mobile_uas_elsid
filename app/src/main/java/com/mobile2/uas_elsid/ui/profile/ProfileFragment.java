package com.mobile2.uas_elsid.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mobile2.uas_elsid.LoginActivity;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.databinding.FragmentProfileBinding;
import com.mobile2.uas_elsid.utils.SessionManager;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SessionManager sessionManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        setupUserInfo();
        setupButtons();

        return binding.getRoot();
    }

    private void setupUserInfo() {
        binding.fullnameText.setText(sessionManager.getFullname());
        binding.emailText.setText(sessionManager.getEmail());
        binding.phoneText.setText(sessionManager.getPhone());
        binding.addressText.setText(sessionManager.getAddress());

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
            String baseUrl = "https://apilumenmobileuaslinux.ndp.my.id/";
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

        binding.logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            startActivity(new Intent(requireActivity(), LoginActivity.class));
            requireActivity().finish();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}