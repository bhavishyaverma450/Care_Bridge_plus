package com.example.carebridge;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    private List<Patient> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(String patientUid);
    }

    public PatientAdapter(List<Patient> patients, OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_card, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Patient patient = patients.get(position);
        holder.patientNameText.setText(patient.getName());
        holder.diseaseNameTextView.setText("Condition: " + (patient.getConditions() != null ? patient.getConditions() : "N/A"));
        holder.ageTextView.setText("BldGp: " + (patient.getBloodGroup() != null ? patient.getBloodGroup() : "N/A"));
        holder.patientPic.setImageResource(R.drawable.male_avatar); // Hardcoded image

        holder.chatButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPatientClick(patient.getUid());
            }
        });

        // Navigate to PatientProfileActivity
        holder.profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), PatientProfileActivity.class);
            intent.putExtra("patientUid", patient.getUid());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return patients != null ? patients.size() : 0;
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView patientNameText, diseaseNameTextView, ageTextView;
        ImageView patientPic;
        ImageButton chatButton, profileButton;

        PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            patientNameText = itemView.findViewById(R.id.patientNameTextView);
            diseaseNameTextView = itemView.findViewById(R.id.diseaseNameTextView);
            ageTextView = itemView.findViewById(R.id.ageTextView);
            patientPic = itemView.findViewById(R.id.patientPic);
            chatButton = itemView.findViewById(R.id.chatButton);
            profileButton = itemView.findViewById(R.id.profileButton);
        }
    }
}