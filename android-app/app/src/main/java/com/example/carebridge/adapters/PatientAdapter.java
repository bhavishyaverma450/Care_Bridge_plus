package com.example.carebridge.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carebridge.ChatActivity;
import com.example.carebridge.R;
import com.example.carebridge.activities.PatientProfileActivity;
import com.example.carebridge.models.Patient;
import com.google.firebase.auth.FirebaseAuth; // Added import

import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {
    private List<Patient> patients;
    private Context context;

    public PatientAdapter(Context context, List<Patient> patients) {
        this.context = context;
        this.patients = patients;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_patient_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient patient = patients.get(position);
        holder.patientPic.setImageResource(R.drawable.male_avatar); // Default avatar
        holder.patientNameTextView.setText(patient.getName() != null ? patient.getName() : "Unknown");
        holder.diseaseNameTextView.setText("Dcs: " + (patient.getDisease() != null ? patient.getDisease() : "N/A"));
        holder.ageTextView.setText("Age: " + (patient.getAge() != null ? patient.getAge() : "N/A"));

        holder.chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("patient_uid", patient.getUid());
            intent.putExtra("doctor_uid", FirebaseAuth.getInstance().getCurrentUser().getUid()); // Added doctor UID
            intent.putExtra("patient_name", patient.getName());
            context.startActivity(intent);
        });

        holder.profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, PatientProfileActivity.class);
            intent.putExtra("patient_uid", patient.getUid());
            intent.putExtra("patient_name", patient.getName());
            intent.putExtra("disease_name", patient.getDisease());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView patientPic;
        TextView patientNameTextView, diseaseNameTextView, ageTextView;
        ImageButton chatButton, profileButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            patientPic = itemView.findViewById(R.id.patientPic);
//            patientNameTextView = itemView.findViewById(R.id.patientNameTextView);
//            diseaseNameTextView = itemView.findViewById(R.id.diseaseNameTextView);
//            ageTextView = itemView.findViewById(R.id.ageTextView);
//            chatButton = itemView.findViewById(R.id.chatButton);
//            profileButton = itemView.findViewById(R.id.profileButton);
        }
    }
}