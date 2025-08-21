package com.example.carebridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {
    private final List<AlertModel> alertList;

    public AlertAdapter(List<AlertModel> alertList) {
        this.alertList = alertList;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        AlertModel alert = alertList.get(position);
        holder.message.setText(alert.getMessage());
        holder.timestamp.setText(alert.getTimestamp()); // if you want to display timestamp
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView message, timestamp;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.tvAlertMessage);
            timestamp = itemView.findViewById(R.id.tvAlertTime);
        }
    }
}
