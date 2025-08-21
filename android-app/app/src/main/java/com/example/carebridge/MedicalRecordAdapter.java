package com.example.carebridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class MedicalRecordAdapter extends RecyclerView.Adapter<MedicalRecordAdapter.MedicalRecordViewHolder> {
    private List<Map<String, Object>> medicinesList;

    public MedicalRecordAdapter(List<Map<String, Object>> medicinesList) {
        this.medicinesList = medicinesList;
    }

    @NonNull
    @Override
    public MedicalRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medical_record, parent, false);
        return new MedicalRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicalRecordViewHolder holder, int position) {
        Map<String, Object> medicine = medicinesList.get(position);
        holder.medicineNameTextView.setText((String) medicine.get("name"));
        holder.medicineTimeTextView.setText((String) medicine.get("time"));
    }

    @Override
    public int getItemCount() {
        return medicinesList.size();
    }

    static class MedicalRecordViewHolder extends RecyclerView.ViewHolder {
        TextView medicineNameTextView;
        TextView medicineTimeTextView;

        public MedicalRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            medicineNameTextView = itemView.findViewById(R.id.medicineNameTextView);
            medicineTimeTextView = itemView.findViewById(R.id.medicineTimeTextView);
        }
    }
}