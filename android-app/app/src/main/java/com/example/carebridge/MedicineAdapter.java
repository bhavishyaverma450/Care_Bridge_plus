package com.example.carebridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {
    private final List<Medicine> medicineList;
    private final OnMedicineActionListener onEditClick;
    private final OnMedicineActionListener onDeleteClick;

    public interface OnMedicineActionListener {
        void onAction(Medicine medicine);
    }

    public MedicineAdapter(List<Medicine> medicineList, OnMedicineActionListener onEditClick, OnMedicineActionListener onDeleteClick) {
        this.medicineList = medicineList;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicine medicine = medicineList.get(position);
        holder.medicineName.setText(medicine.getName());
        holder.reminderTime.setText(medicine.getTime());
        holder.itemView.findViewById(R.id.edit_button).setOnClickListener(v -> onEditClick.onAction(medicine));
        holder.itemView.findViewById(R.id.delete_button).setOnClickListener(v -> onDeleteClick.onAction(medicine));
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView medicineName, reminderTime;

        ViewHolder(View itemView) {
            super(itemView);
            medicineName = itemView.findViewById(R.id.medicineName);
            reminderTime = itemView.findViewById(R.id.reminderTime);
        }
    }
}