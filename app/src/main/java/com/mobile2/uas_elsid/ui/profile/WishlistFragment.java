package com.mobile2.uas_elsid.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.mobile2.uas_elsid.LoginActivity;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.ProductAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.WishlistResponse;
import com.mobile2.uas_elsid.databinding.FragmentWishlistBinding;
import com.mobile2.uas_elsid.model.Product;
import com.mobile2.uas_elsid.model.Wishlist;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WishlistFragment extends Fragment {
    private FragmentWishlistBinding binding;
    private ProductAdapter productAdapter;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        binding.backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        if (!sessionManager.isLoggedIn()) {
            showLoginDialog();
            return binding.getRoot();
        }

        setupRecyclerView();
        loadWishlist();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(requireContext());
        binding.wishlistRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.wishlistRecyclerView.setAdapter(productAdapter);

        // Add click listener for products
        productAdapter.setOnProductClickListener(product -> {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putInt("product_id", product.getId());
            navController.navigate(R.id.navigation_product_detail, args);
        });
    }

    private void loadWishlist() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyStateLayout.setVisibility(View.GONE);

        String userId = sessionManager.getUserId();
        ApiClient.getClient().getWishlist(userId).enqueue(new Callback<WishlistResponse>() {
            @Override
            public void onResponse(@NonNull Call<WishlistResponse> call, @NonNull Response<WishlistResponse> response) {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<Wishlist> wishlist = response.body().getWishlist();
                    if (wishlist != null && !wishlist.isEmpty()) {
                        List<Product> products = new ArrayList<>();
                        for (Wishlist item : wishlist) {
                            if (item.getProduct() != null) {
                                Product product = item.getProduct();
                                // Tambahkan log untuk debugging
                                System.out.println("Debug - Product in wishlist: " + product.getId());
                                System.out.println("Debug - Product images: " +
                                        (product.getImages() != null ? product.getImages().size() : "null"));
                                products.add(product);
                            }
                        }

                        if (!products.isEmpty()) {
                            productAdapter.setProducts(products);
                            binding.wishlistRecyclerView.setVisibility(View.VISIBLE);
                            binding.emptyStateLayout.setVisibility(View.GONE);
                        } else {
                            showEmptyState();
                        }
                    } else {
                        showEmptyState();
                    }
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WishlistResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        binding.wishlistRecyclerView.setVisibility(View.GONE);
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void showLoginDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Login Required")
                .setMessage("Please login to view your wishlist")
                .setPositiveButton("Login", (dialog, which) -> {
                    startActivity(new Intent(requireActivity(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> requireActivity().onBackPressed())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            loadWishlist();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}