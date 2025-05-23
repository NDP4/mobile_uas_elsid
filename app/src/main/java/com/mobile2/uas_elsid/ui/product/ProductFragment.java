package com.mobile2.uas_elsid.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.adapter.ProductAdapter;
import com.mobile2.uas_elsid.adapter.SearchSuggestionsAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.ApiService;
import com.mobile2.uas_elsid.api.response.ProductResponse;
import com.mobile2.uas_elsid.databinding.FragmentProductBinding;
import com.mobile2.uas_elsid.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductFragment extends Fragment {

    private FragmentProductBinding binding;
    private SearchSuggestionsAdapter searchAdapter;
    private List<Product> allProducts = new ArrayList<>();
    private ProductAdapter productAdapter;
    private ApiService apiService;
    private boolean isLoading = false;
    public String searchQuery = null; // untuk menyimpan query pencarian
    private String selectedCategory = null; // untuk memilih kategori produk
    private double minPrice = 0;
    private double maxPrice = 10000000;
    private String sortBy = "newest";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedCategory = getArguments().getString("category");
            searchQuery = getArguments().getString("search_query");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProductBinding.inflate(inflater, container, false);

        setupRecyclerView();
        setupSwipeRefresh();
        setupSearch();
        setupSearchViewInteraction();
//        selectedCategory = null;
        setupFilterButton();
        loadProducts();

        if (selectedCategory != null) {
            // Update title jika ada kategori yang dipilih
            requireActivity().setTitle(selectedCategory);
        }

        return binding.getRoot();
    }

    private void setupSearch() {
        // Initialize search adapter
        searchAdapter = new SearchSuggestionsAdapter(requireContext());
        binding.searchSuggestionsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.searchSuggestionsList.setAdapter(searchAdapter);

        // Load products for search
        loadProductsForSearch();

        // Setup click listener
        searchAdapter.setOnSuggestionClickListener(product -> {
            // Navigate to product detail
            Bundle bundle = new Bundle();
            bundle.putInt("product_id", product.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.navigation_product_detail, bundle);

            // Clear search and hide suggestions
            binding.searchView.setQuery("", false);
            binding.searchView.clearFocus();
            binding.searchSuggestionsList.setVisibility(View.GONE);
        });

        // Setup search view listener
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                loadProducts();

                // Clear search and hide suggestions
                binding.searchView.setQuery("", false);
                binding.searchView.clearFocus();
                binding.searchSuggestionsList.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 0) {
                    binding.searchSuggestionsList.setVisibility(View.VISIBLE);
                    filterProducts(newText);
                } else {
                    binding.searchSuggestionsList.setVisibility(View.GONE);
                }
                return true;
            }
        });
    }
    private void loadProductsForSearch() {
        ApiClient.getClient().getProducts().enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call, @NonNull Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allProducts = response.body().getProducts();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Failed to load products for search").show();
            }
        });
    }
    private void filterProducts(String query) {
        if (allProducts == null) return;

        List<Product> filteredList = new ArrayList<>();
        String lowercaseQuery = query.toLowerCase();

        for (Product product : allProducts) {
            if (product.getTitle().toLowerCase().contains(lowercaseQuery)) {
                filteredList.add(product);
            }
        }

        searchAdapter.updateSuggestions(filteredList);
    }
    private void setupSearchViewInteraction() {
        binding.searchView.setOnSearchClickListener(v -> {
            binding.searchSuggestionsList.setVisibility(View.VISIBLE);
        });

        binding.searchView.setOnCloseListener(() -> {
            binding.searchSuggestionsList.setVisibility(View.GONE);
            return false;
        });
    }



    private void setupFilterButton() {
        binding.filterFab.setOnClickListener(v -> showFilterBottomSheet());
    }
    private void showFilterBottomSheet() {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_filter_bottom_sheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(bottomSheetView);

        // Initialize views
        RangeSlider priceRangeSlider = bottomSheetView.findViewById(R.id.priceRangeSlider);
        TextView minPriceText = bottomSheetView.findViewById(R.id.minPriceText);
        TextView maxPriceText = bottomSheetView.findViewById(R.id.maxPriceText);
        RadioGroup sortRadioGroup = bottomSheetView.findViewById(R.id.sortRadioGroup);
        MaterialButton resetButton = bottomSheetView.findViewById(R.id.resetButton);
        MaterialButton applyButton = bottomSheetView.findViewById(R.id.applyButton);

        // Set current values
        priceRangeSlider.setValues((float) minPrice, (float) maxPrice);
        priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            minPrice = values.get(0);
            maxPrice = values.get(1);
            minPriceText.setText(String.format("Rp %,.0f", minPrice));
            maxPriceText.setText(String.format("Rp %,.0f", maxPrice));
        });

        // Set current sort selection
        switch (sortBy) {
            case "price_asc":
                sortRadioGroup.check(R.id.sortPriceAsc);
                break;
            case "price_desc":
                sortRadioGroup.check(R.id.sortPriceDesc);
                break;
            default:
                sortRadioGroup.check(R.id.sortNewest);
                break;
        }

        // Handle reset
        resetButton.setOnClickListener(v -> {
            minPrice = 0;
            maxPrice = 10000000;
            sortBy = "newest";
            priceRangeSlider.setValues((float) minPrice, (float) maxPrice);
            sortRadioGroup.check(R.id.sortNewest);
        });

        // Handle apply
        applyButton.setOnClickListener(v -> {
            // Get selected sort option
            int selectedId = sortRadioGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.sortPriceAsc) {
                sortBy = "price_asc";
            } else if (selectedId == R.id.sortPriceDesc) {
                sortBy = "price_desc";
            } else {
                sortBy = "newest";
            }

            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }
    private void applyFilters() {
        if (isLoading) return;
        isLoading = true;

        binding.loadingView.loadingContainer.setVisibility(View.VISIBLE);
        apiService.getProducts().enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call, @NonNull Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> filteredProducts = response.body().getProducts().stream()
                            .filter(product -> {
                                double price = product.getPrice();
                                return price >= minPrice && price <= maxPrice;
                            })
                            .collect(Collectors.toList());

                    // Apply sorting
                    switch (sortBy) {
                        case "price_asc":
                            filteredProducts.sort((p1, p2) -> p1.getPrice() - p2.getPrice());
                            break;
                        case "price_desc":
                            filteredProducts.sort((p1, p2) -> p2.getPrice() - p1.getPrice());
                            break;
                        default:
                            // Sort by newest (already in correct order from API)
                            break;
                    }

                    productAdapter.setProducts(filteredProducts);
                    binding.emptyState.setVisibility(filteredProducts.isEmpty() ? View.VISIBLE : View.GONE);
                }

                binding.loadingView.loadingContainer.setVisibility(View.GONE);
                isLoading = false;
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                binding.loadingView.loadingContainer.setVisibility(View.GONE);
                isLoading = false;
                Toasty.error(requireContext(), "Error applying filters: " + t.getMessage()).show();
            }
        });
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
            // Clear search query and view
            searchQuery = null;
            binding.searchView.setQuery("", false);
            binding.searchView.clearFocus();
            binding.searchSuggestionsList.setVisibility(View.GONE);

            // Reset loading state
            isLoading = false;

            // Reload products
            loadProducts();
        });

        // Configure loading indicator colors
        binding.swipeRefresh.setColorSchemeResources(
                R.color.primary,
                R.color.primary_dark
        );
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

                    // Filter products based on category or search query
                    if (selectedCategory != null) {
                        filteredProducts = allProducts.stream()
                                .filter(product -> selectedCategory.equals(product.getCategory()))
                                .collect(Collectors.toList());
                    } else if (searchQuery != null && !searchQuery.isEmpty()) {
                        String query = searchQuery.toLowerCase();
                        filteredProducts = allProducts.stream()
                                .filter(product -> product.getTitle().toLowerCase().contains(query))
                                .collect(Collectors.toList());
                    } else {
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
    public void onResume() {
        super.onResume();
        if (binding != null) {
            binding.searchView.setQuery("", false);
            binding.searchSuggestionsList.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}