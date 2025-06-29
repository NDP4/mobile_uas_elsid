package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile2.uas_elsid.R;
import com.mobile2.uas_elsid.api.response.ShippingCostResponse;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourierAdapter extends RecyclerView.Adapter<CourierAdapter.ViewHolder> {
    private final Context context;
    private final List<ShippingCostResponse.Cost> costs = new ArrayList<>();
    private int selectedPosition = -1;
    private final OnCourierSelectedListener listener;
    private ShippingCostResponse.Cost selectedCost = null;

    public interface OnCourierSelectedListener {
        void onCourierSelected(ShippingCostResponse.Cost cost);
    }

    public CourierAdapter(Context context, OnCourierSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setCosts(List<ShippingCostResponse.Cost> newCosts) {
        costs.clear();
        costs.addAll(newCosts);
        notifyDataSetChanged();
    }

    public void addCosts(List<ShippingCostResponse.Cost> newCosts) {
        // Check for duplicates before adding
        for (ShippingCostResponse.Cost newCost : newCosts) {
            boolean isDuplicate = false;
            for (ShippingCostResponse.Cost existingCost : costs) {
                if (existingCost.service.equals(newCost.service)) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                costs.add(newCost);
            }
        }
        notifyDataSetChanged();
    }

    public void clearCosts() {
        costs.clear();
        notifyDataSetChanged();
    }

    public List<ShippingCostResponse.Cost> getCosts() {
        return costs;
    }

    public ShippingCostResponse.Cost getSelectedCourier() {
        return selectedCost;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_courier, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShippingCostResponse.Cost cost = costs.get(position);

        // Set service name and description
        holder.serviceText.setText(cost.service);
        holder.descriptionText.setText(cost.description);

        // Set radio button state
        holder.radioButton.setChecked(position == selectedPosition);

        // Set cost
        if (!cost.cost.isEmpty()) {
            int shippingCost = cost.cost.get(0).value;
            holder.costText.setText(formatPrice(shippingCost));

            // Set estimated delivery time
            String etd = cost.cost.get(0).etd;
            String estimatedDays = etd.replaceAll("[^0-9-]", "");
            if (estimatedDays.contains("-")) {
                String[] range = estimatedDays.split("-");
                estimatedDays = range[1].trim(); // Use the higher number in the range
            }
            holder.etdText.setText(String.format("%s days", estimatedDays));
        }

        // Handle radio button selection
        holder.radioButton.setChecked(position == selectedPosition);
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            selectedCost = cost;
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            if (listener != null && !cost.cost.isEmpty()) {
                listener.onCourierSelected(cost);
            }
        });
    }


    @Override
    public int getItemCount() {
        return costs.size();
    }

    private String formatPrice(int price) {
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        String formatted = rupiahFormat.format(price);
        return formatted.substring(0, formatted.length() - 3); // Remove ",00"
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        TextView serviceText;
        TextView descriptionText;
        TextView costText;
        TextView etdText;

        ViewHolder(View view) {
            super(view);
            radioButton = view.findViewById(R.id.radioButton);
            serviceText = view.findViewById(R.id.serviceText);
            descriptionText = view.findViewById(R.id.descriptionText);
            costText = view.findViewById(R.id.courierCostText);
            etdText = view.findViewById(R.id.etdText);
        }
    }
}
