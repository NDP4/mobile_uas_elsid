package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.model.CartItem;
import com.mobile2.uas_elsid.model.ProductImage;
import com.mobile2.uas_elsid.utils.CartManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
//    private List<CartItem> cartItems;
    private List<CartItem> cartItems = new ArrayList<>();
    private Context context;
    private CartItemListener listener;
    private String userId;

    public interface CartItemListener {
        void onQuantityChanged();
        void onItemRemoved();
        void onProductClicked(int productId);
    }

    public CartAdapter(Context context, CartItemListener listener, String userId) {
        this.context = context;
        this.listener = listener;
//        this.cartItems = CartManager.getInstance().getCartItems();
//        this.userId = userId;
//        this.cartItems = CartManager.getInstance(context).getCartItems(userId);
        loadCartItems();

    }
    private void loadCartItems() {
        CartManager.getInstance(context).getCartItems(new CartManager.CartCallback() {
            @Override
            public void onSuccess(List<CartItem> items) {
                cartItems = items;
                notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                Toasty.error(context, message).show();
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            CartItem item = cartItems.get(position);

            // Set product title
            holder.titleText.setText(item.getProduct().getTitle());

            // Set variant text if exists
            if (item.getVariant() != null) {
                holder.variantText.setText(item.getVariant().getVariantName());
                holder.variantText.setVisibility(View.VISIBLE);
            } else {
                holder.variantText.setVisibility(View.GONE);
            }

            // Set price
            int price = item.getVariant() != null ?
                    calculateVariantPrice(item) :
                    calculateProductPrice(item);
            holder.priceText.setText(formatPrice(price));

            // Set quantity
            holder.quantityText.setText(String.valueOf(item.getQuantity()));

            // Add click listener to the whole item
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClicked(item.getProduct().getId());
                }
            });

            // Load image safely
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                ProductImage firstImage = item.getProduct().getImages().get(0);
                String imageUrl = "https://apilumenmobileuas.ndp.my.id/" + firstImage.getImageUrl();

                try {
                    Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .into(holder.productImage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            // Handle remove button click with debounce
            holder.removeButton.setOnClickListener(new View.OnClickListener() {
                private boolean isClicked = false;

                @Override
                public void onClick(View v) {
                    if (!isClicked) {
                        isClicked = true;
                        holder.removeButton.setEnabled(false);
                        removeItem(holder, item, holder.getAdapterPosition());
                        holder.removeButton.setEnabled(true);
                        isClicked = false;
                    }
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeItem(ViewHolder holder, CartItem item, int position) {
        // Disable button to prevent multiple clicks
        holder.removeButton.setEnabled(false);

        CartManager.getInstance(context).removeFromCart(item.getId(), new CartManager.CartCallback() {
            @Override
            public void onSuccess(List<CartItem> items) {
                try {
                    // Remove item locally
                    cartItems.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());

                    // Notify listener
                    if (listener != null) {
                        listener.onItemRemoved();
                    }

                    Toasty.success(context, "Item removed from cart").show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toasty.error(context, "Error updating cart view").show();
                } finally {
                    holder.removeButton.setEnabled(true);
                }
            }

            @Override
            public void onError(String message) {
                try {
                    holder.removeButton.setEnabled(true);
                    // Check if error is "No query results" which means item was already deleted
                    if (message.contains("No query results")) {
                        // Item was already deleted from server, update local view
                        cartItems.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, getItemCount());

                        if (listener != null) {
                            listener.onItemRemoved();
                        }

                        Toasty.success(context, "Item removed from cart").show();
                    } else {
                        Toasty.error(context, "Error: " + message).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toasty.error(context, "Error updating cart view").show();
                }
            }
        });
    }
    private int calculateVariantPrice(CartItem item) {
        int basePrice = item.getVariant().getPrice();
        int discount = item.getVariant().getDiscount();
        return basePrice - (basePrice * discount / 100);
    }
    private int calculateProductPrice(CartItem item) {
        int basePrice = item.getProduct().getPrice();
        int discount = item.getProduct().getDiscount();
        return basePrice - (basePrice * discount / 100);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }
    public void updateItems(List<CartItem> newItems) {
        if (newItems != null) {
            cartItems = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }
    }

    private int calculateDiscountedPrice(int originalPrice, int discountPercentage) {
        return originalPrice - (originalPrice * discountPercentage / 100);
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Remove ",00"
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView titleText;
        TextView variantText;
        TextView quantityText;
        TextView priceText;
        ImageButton removeButton;

        ViewHolder(View view) {
            super(view);
            productImage = view.findViewById(R.id.productImage);
            titleText = view.findViewById(R.id.titleText);
            variantText = view.findViewById(R.id.variantText);
            quantityText = view.findViewById(R.id.quantityText);
            priceText = view.findViewById(R.id.priceText);
            removeButton = view.findViewById(R.id.removeButton);
        }
    }
}
