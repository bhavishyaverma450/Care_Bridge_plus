package com.example.carebridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;

public class DoctorVisitAdapter extends RecyclerView.Adapter<DoctorVisitAdapter.VisitViewHolder> {
    private final List<AppointmentModel> visitList;

    public DoctorVisitAdapter(List<AppointmentModel> visitList) {
        this.visitList = visitList;
    }

    public void updateList(List<AppointmentModel> newList) {
        visitList.clear();
        visitList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VisitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_visit, parent, false);
        return new VisitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VisitViewHolder holder, int position) {
        AppointmentModel appointment = visitList.get(position);
        holder.dateTimeText.setText(String.format(
                holder.itemView.getContext().getString(R.string.appointment_date_time),
                appointment.getDate(), appointment.getTime()));
        holder.reasonText.setText(appointment.getReason());
        holder.doctorNameText.setText(String.format(
                holder.itemView.getContext().getString(R.string.appointment_doctor_name),
                appointment.getDoctor()));
        holder.patientUidText.setText(appointment.getPatientUid());

        final String patientUid = appointment.getPatientUid();
        if (patientUid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(patientUid).child("name")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String patientName = snapshot.getValue(String.class);
                            if (patientName != null) {
                                holder.patientUidText.setText(String.format(
                                        holder.itemView.getContext().getString(R.string.appointment_patient_name),
                                        patientName));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle error silently
                        }
                    });
        }

        // Long-click to delete appointment
        holder.itemView.setOnLongClickListener(v -> {
            final int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return false;
            }
            final AppointmentModel appointmentToDelete = visitList.get(adapterPosition);
            final String appointmentId = appointmentToDelete.getAppointmentId();
            final String date = appointmentToDelete.getDate();
            if (patientUid != null) {
                DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                db.child("users").child(patientUid).child("doctorUid").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String doctorUid = snapshot.getValue(String.class);
                        if (doctorUid != null) {
                            db.child("appointments").child(doctorUid).child(date).child(appointmentId).removeValue();
                            db.child("appointments_by_patient").child(patientUid).child(appointmentId).removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        visitList.remove(adapterPosition);
                                        notifyItemRemoved(adapterPosition);
                                        notifyItemRangeChanged(adapterPosition, visitList.size());
                                        Toast.makeText(holder.itemView.getContext(), "Appointment cancelled", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(holder.itemView.getContext(), "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(holder.itemView.getContext(), "No doctor linked", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(holder.itemView.getContext(), "Failed to fetch doctor: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return visitList.size();
    }

    static class VisitViewHolder extends RecyclerView.ViewHolder {
        TextView dateTimeText, reasonText, doctorNameText, patientUidText;

        VisitViewHolder(View itemView) {
            super(itemView);
            dateTimeText = itemView.findViewById(R.id.visit_date);
            reasonText = itemView.findViewById(R.id.reasonText);
            doctorNameText = itemView.findViewById(R.id.doctor_name);
            patientUidText = itemView.findViewById(R.id.patientUidText);
        }
    }
}