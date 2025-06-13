package com.mobile2.uas_elsid.ui.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobile2.uas_elsid.LoginActivity;
import com.mobile2.uas_elsid.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mobile2.uas_elsid.adapter.CartAdapter;
import com.mobile2.uas_elsid.databinding.FragmentCheckoutBinding;
import com.mobile2.uas_elsid.model.CartItem;
import com.mobile2.uas_elsid.utils.CartManager;
import com.mobile2.uas_elsid.utils.SessionManager;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;

public class CheckoutFragment extends Fragment implements CartAdapter.CartItemListener {
    private FragmentCheckoutBinding binding;
    private CartAdapter cartAdapter;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        setupRecyclerView();
        setupCheckoutButton();
        updateEmptyState();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(requireContext(), this, null);
        binding.recyclerViewCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewCart.setAdapter(cartAdapter);
        binding.loginRequiredView.setVisibility(View.GONE);
    }

    @Override
    public void onQuantityChanged() {
        updateEmptyState();
    }

    @Override
    public void onItemRemoved() {
        updateEmptyState();
    }

    @Override
    public void onProductClicked(int productId) {
        Bundle args = new Bundle();
        args.putInt("product_id", productId);
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_home);
        navController.navigate(R.id.navigation_product_detail, args);
    }

    private void updateEmptyState() {
        CartManager.getInstance(requireContext()).getCartItems(new CartManager.CartCallback() {
            @Override
            public void onSuccess(List<CartItem> items) {
                if (items.isEmpty()) {
                    binding.recyclerViewCart.setVisibility(View.GONE);
                    binding.emptyStateView.setVisibility(View.VISIBLE);
                    binding.subtotalLayout.setVisibility(View.GONE);
                    binding.checkoutButton.setVisibility(View.GONE);
                } else {
                    binding.recyclerViewCart.setVisibility(View.VISIBLE);
                    binding.emptyStateView.setVisibility(View.GONE);
                    binding.subtotalLayout.setVisibility(View.VISIBLE);
                    binding.checkoutButton.setVisibility(View.VISIBLE);
                    updateSubtotal(items);
                }
            }

            @Override
            public void onError(String message) {
                Toasty.error(requireContext(), message).show();
            }
        });
    }

    private void updateSubtotal(List<CartItem> items) {
        int subtotal = 0;
        for (CartItem item : items) {
            if (item.getVariant() != null) {
                int price = item.getVariant().getPrice() -
                        (item.getVariant().getPrice() * item.getVariant().getDiscount() / 100);
                subtotal += price * item.getQuantity();
            } else {
                int price = item.getProduct().getPrice() -
                        (item.getProduct().getPrice() * item.getProduct().getDiscount() / 100);
                subtotal += price * item.getQuantity();
            }
        }

        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formattedSubtotal = rupiahFormat.format(subtotal)
                .substring(0, rupiahFormat.format(subtotal).length() - 3);
        binding.subtotalText.setText(formattedSubtotal);
    }
    private void setupCheckoutButton() {
        binding.checkoutButton.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                View dialogView = getLayoutInflater().inflate(R.layout.layout_login_required_dialog, null);
                TextView messageText = dialogView.findViewById(R.id.loginMessageText);
                messageText.setText("Please login to complete your purchase");

                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                dialogView.findViewById(R.id.loginButton).setOnClickListener(view -> {
                    dialog.dismiss();
                    startActivity(new Intent(requireActivity(), LoginActivity.class));
                });

                dialogView.findViewById(R.id.cancelButton).setOnClickListener(button -> {
                    dialog.dismiss();
                });

                dialog.show();
                return;
            }

            NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_activity_home);

            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                navController.navigate(R.id.action_navigation_checkout_to_navigation_detail_pesanan);
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

