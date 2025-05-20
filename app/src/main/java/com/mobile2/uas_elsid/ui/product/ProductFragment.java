package com.mobile2.uas_elsid.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.ProductAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.ApiService;
import com.mobile2.uas_elsid.api.response.ProductResponse;
import com.mobile2.uas_elsid.databinding.FragmentProductBinding;
import com.mobile2.uas_elsid.model.Product;

import java.util.List;
import java.util.stream.Collectors;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductFragment extends Fragment {

    private FragmentProductBinding binding;
    private ProductAdapter productAdapter;
    private ApiService apiService;
    private boolean isLoading = false;
    private String selectedCategory = null; // untuk memilih kategori produk

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedCategory = getArguments().getString("category");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductBinding.inflate(inflater, container, false);

        setupRecyclerView();
        setupSwipeRefresh();
//        selectedCategory = null;
        loadProducts();

        if (selectedCategory != null) {
            // Update title jika ada kategori yang dipilih
            requireActivity().setTitle(selectedCategory);
        }

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(requireContext());
        binding.recyclerViewProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerViewProducts.setAdapter(productAdapter);

        productAdapter.setOnProductClickListener(product -> {
            // Navigate to product detail
            Bundle args = new Bundle();
            args.putInt("product_id", product.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.navigation_product_detail, args);
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            selectedCategory = null; // Reset category filter when refreshing
            loadProducts();
        });
    }

    private void loadProducts() {
        if (isLoading) return;
        isLoading = true;

        if (!binding.swipeRefresh.isRefreshing()) {
            binding.loadingView.loadingContainer.setVisibility(View.VISIBLE);
        }

        apiService = ApiClient.getClient();

        apiService.getProducts().enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call, @NonNull Response<ProductResponse> response) {
                binding.loadingView.loadingContainer.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    List<Product> allProducts = response.body().getProducts();
                    List<Product> filteredProducts;

                    // Filter products if category is selected and not refreshing
                    if (selectedCategory != null && !binding.swipeRefresh.isRefreshing()) {
                        filteredProducts = allProducts.stream()
                                .filter(product -> selectedCategory.equals(product.getCategory()))
                                .collect(Collectors.toList());
                    } else {
                        // Show all products if no category selected or refreshing
                        filteredProducts = allProducts;
                    }

                    productAdapter.setProducts(filteredProducts);

                    if (filteredProducts.isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                    }
                } else {
                    Toasty.error(requireContext(), "Failed to load products").show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                binding.loadingView.loadingContainer.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                isLoading = false;
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