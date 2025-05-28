package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.api.response.ShippingCostResponse;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourierAdapter extends RecyclerView.Adapter<CourierAdapter.ViewHolder> {
    private final Context context;
    private List<ShippingCostResponse.Cost> costs = new ArrayList<>();
    private final OnCourierSelectedListener listener;
    private int selectedPosition = -1;

    // menambahkan getter untuk seleced courier
    public ShippingCostResponse.Cost getSelectedCourier() {
        if (selectedPosition >= 0 && selectedPosition < costs.size()) {
            return costs.get(selectedPosition);
        }
        return null;
    }

    public interface OnCourierSelectedListener {
        void onCourierSelected(ShippingCostResponse.Cost cost);
    }

    public CourierAdapter(Context context, OnCourierSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_courier_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShippingCostResponse.Cost cost = costs.get(position);

        // Set service name
        holder.courierNameText.setText(cost.service);

        // Set estimation
        holder.estimationText.setText(cost.cost.get(0).etd + " hari");

        // Set shipping cost
        holder.costText.setText(formatPrice(cost.cost.get(0).value));

        // Set courier logo based on service code
//        String serviceCode = cost.service.toLowerCase();
        String courierCode = cost.service.toLowerCase();
        if (courierCode.contains("jne")) {
            holder.courierLogo.setImageResource(R.drawable.logo_jne);
        } else if (courierCode.contains("tiki")) {
            holder.courierLogo.setImageResource(R.drawable.logo_tiki);
        } else if (courierCode.contains("pos")) {
            holder.courierLogo.setImageResource(R.drawable.logo_pos);
        }

        MaterialCardView cardView = (MaterialCardView) holder.itemView;
        cardView.setChecked(position == selectedPosition);

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onCourierSelected(cost);
            }
        });
    }

    @Override
    public int getItemCount() {
        return costs.size();
    }

    public void setCosts(List<ShippingCostResponse.Cost> costs) {
        this.costs = costs;
        notifyDataSetChanged();
    }

    public List<ShippingCostResponse.Cost> getCosts() {
        return costs;
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Remove ",00"
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView courierLogo;
        TextView courierNameText;
        TextView estimationText;
        TextView costText;

        ViewHolder(View view) {
            super(view);
            courierLogo = view.findViewById(R.id.courierLogo);
            courierNameText = view.findViewById(R.id.courierNameText);
            estimationText = view.findViewById(R.id.estimationText);
            costText = view.findViewById(R.id.costText);
        }
    }
}
