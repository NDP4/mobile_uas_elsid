package com.mobile2.uas_elsid.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import com.mobile2.uas_elsid.R;

import android.os.Handler;
import android.os.Looper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.mobile2.uas_elsid.adapter.BannerAdapter;
import com.mobile2.uas_elsid.adapter.CategoryAdapter;
import com.mobile2.uas_elsid.adapter.ProductAdapter;
import com.mobile2.uas_elsid.api.ApiClient;
import com.mobile2.uas_elsid.api.response.BannerResponse;
import com.mobile2.uas_elsid.api.response.ProductResponse;
import com.mobile2.uas_elsid.databinding.FragmentHomeBinding;
import com.mobile2.uas_elsid.model.Banner;
import com.mobile2.uas_elsid.model.CartItem;
import com.mobile2.uas_elsid.model.Product;
import com.mobile2.uas_elsid.utils.CartManager;
import com.mobile2.uas_elsid.utils.SessionManager;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private FragmentHomeBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private SessionManager sessionManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 1002;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private BannerAdapter bannerAdapter;
    private Handler bannerHandler;
    private Runnable bannerRunnable;
    private static final long BANNER_DELAY = 3000; // 3 seconds
    private CategoryAdapter categoryAdapter;
    private RecyclerView categoryRecyclerView;
    private Set<String> uniqueCategories = new HashSet<>();
    private RecyclerView newArrivalsRecyclerView;
    private ProductAdapter newArrivalsAdapter;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Initialize RecyclerView and adapter
//        categoryRecyclerView = binding.categoryRecyclerView;
//        categoryAdapter = new CategoryAdapter(this); // Pass 'this' as the listener
//        categoryRecyclerView.setAdapter(categoryAdapter);
//        categoryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        // Setup New Arrivals RecyclerView
        newArrivalsRecyclerView = binding.newArrivalsRecyclerView;
        newArrivalsAdapter = new ProductAdapter(requireContext());
        newArrivalsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        newArrivalsRecyclerView.setAdapter(newArrivalsAdapter);
        newArrivalsAdapter.setOnProductClickListener(product -> {
            Bundle bundle = new Bundle();
            bundle.putInt("product_id", product.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.navigation_product_detail, bundle);
        });



        // Inisialisasi SwipeRefreshLayout
        swipeRefresh = binding.swipeRefresh;
        swipeRefresh.setOnRefreshListener(() -> {
            // Refresh semua data
//            setupBanner();
            setupCategories();
            loadProducts();
            loadNewArrivals();
            getCurrentLocation();

            // Hentikan animasi refresh setelah selesai
            swipeRefresh.setRefreshing(false);
        });

        // Konfigurasi warna loading indicator
        swipeRefresh.setColorSchemeResources(
                R.color.primary,
                R.color.primary_dark
        );




        sessionManager = new SessionManager(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupLocationCallback();
        binding.userNameText.setText(sessionManager.getFullname());
        checkLocationPermission();

        setupBanner();
        setupCategories();
        loadProducts();
        setupCartButton();
        loadNewArrivals();

        return binding.getRoot();
    }

    private void loadNewArrivals() {
        ApiClient.getClient().getProducts().enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call, @NonNull Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body().getProducts();
                    if (products != null && !products.isEmpty()) {
                        // Sort products by ID in descending order (newest first)
                        Collections.sort(products, (p1, p2) -> Integer.compare(p2.getId(), p1.getId()));

                        // Get only the first 5 newest products
                        List<Product> newArrivals = products.subList(0, Math.min(products.size(), 2));
                        newArrivalsAdapter.setProducts(newArrivals);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                Toasty.error(requireContext(), "Failed to load new arrivals: " + t.getMessage(),
                        Toasty.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCategories() {
        categoryAdapter = new CategoryAdapter(this);
        binding.categoryRecyclerView.setAdapter(categoryAdapter);
        binding.categoryRecyclerView.setLayoutManager(
                new GridLayoutManager(requireContext(), 4));
    }

    private void loadProducts() {
        ApiClient.getClient().getProducts().enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Product product : response.body().getProducts()) {
                        uniqueCategories.add(product.getCategory());
                    }
                    categoryAdapter.setCategories(new ArrayList<>(uniqueCategories));
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Toasty.error(requireContext(),
                        "Failed to load categories: " + t.getMessage()).show();
            }
        });
    }
    @Override
    public void onCategoryClick(String category) {
        Bundle bundle = new Bundle();
        bundle.putString("category", category);

        Navigation.findNavController(requireView())
                .navigate(R.id.navigation_product, bundle); // Langsung navigasi ke navigation_product
    }

    private void setupProductAdapters() {
        newArrivalsAdapter.setOnProductClickListener(product -> {
            Bundle bundle = new Bundle();
            bundle.putInt("product_id", product.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.navigation_product_detail, bundle);
        });
    }

    // fungsi untuk refresh lokasi
    private void setupLocationCallback() {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000) // Update setiap 10 detik
                .setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    getCityName(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */);
    }



    @Override
    public void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
        startBannerAutoScroll();
        updateCartBadge();
    }
    public void refreshCartBadge() {
        if (isAdded() && binding != null) {
            updateCartBadge();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
        stopBannerAutoScroll();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkLocationSettings();
        }
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(requireActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
            getCurrentLocation();
        });

        task.addOnFailureListener(requireActivity(), e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            } else {
                binding.baselocation.setText("Location permission denied");
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        binding.baselocation.setText("Getting location...");

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        getCityName(location);
                    } else {
                        binding.baselocation.setText("Unable to get location");
                        checkLocationSettings(); // Retry by checking settings
                    }
                })
                .addOnFailureListener(e -> {
                    binding.baselocation.setText("Location error");
                    Toasty.error(requireContext(), "Error getting location: " + e.getMessage()).show();
                });
    }

    // Fungsi untuk mendapatkan nama kota dan kecamatan dari lokasi
    private void getCityName(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String subLocality = address.getSubLocality(); // Kecamatan
                String cityName = address.getSubAdminArea(); // Kota/Kabupaten

                StringBuilder locationText = new StringBuilder();

                // Tambahkan kecamatan jika ada
                if (subLocality != null && !subLocality.isEmpty()) {
                    locationText.append(subLocality);
                }

                // Tambahkan kota/kabupaten jika ada
                if (cityName != null && !cityName.isEmpty()) {
                    // Tambahkan koma jika ada kecamatan sebelumnya
                    if (locationText.length() > 0) {
                        locationText.append(", ");
                    }
                    // Hapus kata "Kota" atau "Kabupaten" jika ada
                    cityName = cityName.replaceAll("(?i)kota\\s+", "")
                            .replaceAll("(?i)kabupaten\\s+", "");
                    locationText.append(cityName);
                }

                if (locationText.length() > 0) {
                    binding.baselocation.setText(locationText.toString());
                } else {
                    binding.baselocation.setText("Location not found");
                }
            } else {
                binding.baselocation.setText("Address not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            binding.baselocation.setText("Error getting location");
            Toasty.error(requireContext(), "Geocoding error: " + e.getMessage()).show();
        }
    }

    // Fungsi untuk mendapatkan nama kota dan kecamatan dan kelurahan dari lokasi
//    private void getCityName(Location location) {
//        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(
//                    location.getLatitude(), location.getLongitude(), 1);
//            if (addresses != null && !addresses.isEmpty()) {
//                Address address = addresses.get(0);
//                String kelurahan = address.getThoroughfare(); // Kelurahan
//                String kecamatan = address.getSubLocality(); // Kecamatan
//                String kota = address.getSubAdminArea(); // Kota/Kabupaten
//
//                StringBuilder locationText = new StringBuilder();
//
//                // Tambahkan kelurahan jika ada
//                if (kelurahan != null && !kelurahan.isEmpty()) {
//                    locationText.append(kelurahan);
//                }
//
//                // Tambahkan kecamatan jika ada
//                if (kecamatan != null && !kecamatan.isEmpty()) {
//                    if (locationText.length() > 0) {
//                        locationText.append(", ");
//                    }
//                    locationText.append(kecamatan);
//                }
//
//                // Tambahkan kota jika ada
//                if (kota != null && !kota.isEmpty()) {
//                    if (locationText.length() > 0) {
//                        locationText.append(", ");
//                    }
//                    // Hapus kata "Kota" atau "Kabupaten" jika ada
//                    kota = kota.replaceAll("(?i)kota\\s+", "")
//                            .replaceAll("(?i)kabupaten\\s+", "");
//                    locationText.append(kota);
//                }
//
//                if (locationText.length() > 0) {
//                    binding.baselocation.setText(locationText.toString());
//                } else {
//                    binding.baselocation.setText("Location not found");
//                }
//            } else {
//                binding.baselocation.setText("Address not found");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            binding.baselocation.setText("Error getting location");
//            Toasty.error(requireContext(), "Geocoding error: " + e.getMessage()).show();
//        }
//    }

    private void setupCartButton() {
        binding.cartButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_navigation_home_to_navigation_checkout);
        });
    }
    private void updateCartBadge() {
        CartManager.getInstance(requireContext()).getCartItems(new CartManager.CartCallback() {
            @Override
            public void onSuccess(List<CartItem> items) {
                if (binding != null) {
                    if (items != null && !items.isEmpty()) {
                        binding.cartBadge.setVisibility(View.VISIBLE);
                        binding.cartBadge.setText(String.valueOf(items.size()));
                    } else {
                        binding.cartBadge.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    binding.cartBadge.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupBanner() {
        bannerAdapter = new BannerAdapter(requireContext());
        binding.bannerViewPager.setAdapter(bannerAdapter);

        // Inisialisasi Handler
        bannerHandler = new Handler(Looper.getMainLooper());

        // Setup auto-scroll runnable
        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerAdapter.getItemCount() > 0) {
                    int currentItem = binding.bannerViewPager.getCurrentItem();
                    int totalItems = bannerAdapter.getItemCount();
                    int nextItem = (currentItem + 1) % totalItems;
                    binding.bannerViewPager.setCurrentItem(nextItem);
                    bannerHandler.postDelayed(this, BANNER_DELAY);
                }
            }
        };

        // Load banners from API
        loadBanners();
    }

    private void loadBanners() {
        ApiClient.getClient().getBanners().enqueue(new Callback<BannerResponse>() {
            @Override
            public void onResponse(Call<BannerResponse> call, Response<BannerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Banner> banners = response.body().getBanners();
                    bannerAdapter.setBanners(banners);

                    // Start auto-scroll
                    startBannerAutoScroll();
                }
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<BannerResponse> call, Throwable t) {
                Toasty.error(requireContext(), "Failed to load banners: " + t.getMessage()).show();
            }
        });
    }

    private void startBannerAutoScroll() {
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable); // Remove existing callbacks first
            bannerHandler.postDelayed(bannerRunnable, BANNER_DELAY);
        }
    }

    private void stopBannerAutoScroll() {
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bannerHandler != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
        bannerHandler = null;
        bannerRunnable = null;
        binding = null;
    }
}