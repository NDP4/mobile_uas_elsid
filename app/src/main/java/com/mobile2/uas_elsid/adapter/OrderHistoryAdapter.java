package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.model.Order;
import com.mobile2.uas_elsid.model.OrderItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {
    private List<Order> orders = new ArrayList<>();
    private Context context;
    private OnOrderActionListener listener;
    private OnItemClickListener itemClickListener;
    public interface OnItemClickListener {
        void onItemClick(Order order);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public interface OnOrderActionListener {
        void onReorder(Order order);
        void onWriteReview(Order order);
    }

    public OrderHistoryAdapter(Context context, OnOrderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);

        // Set order ID and status
        holder.orderIdText.setText(String.format("Order #%d", order.getId()));
        holder.orderStatusText.setText(order.getStatus());

        // Format currency
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String totalAmount = rupiahFormat.format(order.getTotalAmount())
                .replace(",00", "");
        holder.totalAmountText.setText(totalAmount);

        // Set total items
        int totalItems = order.getItems().size();
        holder.totalItemsText.setText(String.format("%d items", totalItems));

        // Set up the nested RecyclerView for order items
        OrderItemAdapter itemsAdapter = new OrderItemAdapter(order.getItems());
        holder.orderItemsRecyclerView.setAdapter(itemsAdapter);
        holder.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Handle button visibility based on order status
        boolean isDelivered = "delivered".equalsIgnoreCase(order.getStatus());
        boolean canReorder = "completed".equalsIgnoreCase(order.getStatus()) &&
                "settlement".equalsIgnoreCase(order.getPaymentStatus());

        holder.reorderButton.setVisibility(canReorder ? View.VISIBLE : View.GONE);
        holder.writeReviewButton.setVisibility(isDelivered ? View.VISIBLE : View.GONE);

        // Set button click listeners
        holder.reorderButton.setOnClickListener(v -> {
            if (listener != null) listener.onReorder(order);
        });

        holder.writeReviewButton.setOnClickListener(v -> {
            if (listener != null) listener.onWriteReview(order);
        });
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void setOrders(List<Order> newOrders) {
        this.orders.clear();
        this.orders.addAll(newOrders);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdText, orderStatusText, totalItemsText, totalAmountText;
        RecyclerView orderItemsRecyclerView;
        MaterialButton reorderButton, writeReviewButton;

        ViewHolder(View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.orderIdText);
            orderStatusText = itemView.findViewById(R.id.orderStatusText);
            totalItemsText = itemView.findViewById(R.id.totalItemsText);
            totalAmountText = itemView.findViewById(R.id.totalAmountText);
            orderItemsRecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView);
            reorderButton = itemView.findViewById(R.id.reorderButton);
            writeReviewButton = itemView.findViewById(R.id.writeReviewButton);
        }
    }

    // Inner adapter for order items
    private static class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ItemViewHolder> {
        private final List<OrderItem> items;

        OrderItemAdapter(List<OrderItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            OrderItem item = items.get(position);
            String itemText = String.format("%dx %s",
                    item.getQuantity(),
                    item.getProduct().getTitle());
            holder.textView.setText(itemText);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ItemViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }
}