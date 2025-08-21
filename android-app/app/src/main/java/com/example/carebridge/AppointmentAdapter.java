package com.example.carebridge;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {
    private final List<AppointmentModel> appointmentList;

    public AppointmentAdapter(List<AppointmentModel> appointmentList) {
        this.appointmentList = appointmentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false); // Your CardView layout
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppointmentModel model = appointmentList.get(position);
        holder.date.setText("Date: " + model.getDate());
        holder.time.setText("Time: " + model.getTime());
        holder.reason.setText("Reason: " + model.getReason());

        // Optional: Load patient name using UID
        String patientUid = model.getPatientUid();
        if (patientUid != null) {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(patientUid).child("name")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String patientName = snapshot.getValue(String.class);
                            if (patientName != null) {
                                holder.reason.setText("Patient: " + patientName + "\nReason: " + model.getReason());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Optional: handle error
                        }
                    });
        }

        // Long-click to delete appointment
        holder.itemView.setOnLongClickListener(v -> {
            final int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return false;

            AppointmentModel appointment = appointmentList.get(adapterPosition);
            String appointmentId = appointment.getAppointmentId();
            String patientUid1 = appointment.getPatientUid();
            String date = appointment.getDate();

            if (appointmentId == null || patientUid1 == null || date == null) {
                Toast.makeText(holder.itemView.getContext(), "Invalid data", Toast.LENGTH_SHORT).show();
                return false;
            }

            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Delete Appointment")
                    .setMessage("Are you sure you want to delete this appointment?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Get doctorUid from patient data
                        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                        db.child("users").child(patientUid1).child("doctorUid")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String doctorUid = snapshot.getValue(String.class);
                                        if (doctorUid != null) {
                                            db.child("appointments_by_patient").child(patientUid1).child(appointmentId).removeValue();
                                            db.child("appointments").child(doctorUid).child(date)
                                                    .orderByChild("appointmentId")
                                                    .equalTo(appointmentId)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            for (DataSnapshot ds : snapshot.getChildren()) {
                                                                ds.getRef().removeValue();
                                                            }
                                                            appointmentList.remove(adapterPosition);
                                                            notifyItemRemoved(adapterPosition);
                                                            notifyItemRangeChanged(adapterPosition, appointmentList.size());
                                                            Toast.makeText(holder.itemView.getContext(), "Appointment deleted", Toast.LENGTH_SHORT).show();
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {
                                                            Toast.makeText(holder.itemView.getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(holder.itemView.getContext(), "No doctor assigned", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(holder.itemView.getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });

    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date, time, reason;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.tvDate);
            time = itemView.findViewById(R.id.tvTime);
            reason = itemView.findViewById(R.id.tvReason);
        }
    }
}
