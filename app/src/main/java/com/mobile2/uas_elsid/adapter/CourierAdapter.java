package com.mobile2.uas_elsid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private List<ShippingCostResponse.Cost> costs = new ArrayList<>();
    private final Context context;
    private OnCourierSelectedListener listener;
    private int selectedPosition = -1;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_courier, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShippingCostResponse.Cost cost = costs.get(position);

        // Set kurir name dan service
        holder.courierNameText.setText(cost.service.toUpperCase());

        // Set estimasi
        String estimation = cost.cost.get(0).etd + " hari";
        holder.estimationText.setText(estimation);

        // Set ongkos kirim
        holder.costText.setText(formatPrice(cost.cost.get(0).value));

        // Handle item selection
        holder.itemView.setSelected(selectedPosition == position);
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            listener.onCourierSelected(cost);
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
        TextView courierNameText;
        TextView estimationText;
        TextView costText;

        ViewHolder(View view) {
            super(view);
            courierNameText = view.findViewById(R.id.courierNameText);
            estimationText = view.findViewById(R.id.estimationText);
            costText = view.findViewById(R.id.costText);
        }
    }
}
