package com.example.carebridge;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class PatientsFragment extends Fragment {
    private static final String TAG = "PatientsFragment";
    private RecyclerView patientsRecyclerView;
    private PatientAdapter patientAdapter;
    private List<Patient> patients;
    private String doctorUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patients, container, false);

        // Initialize UI elements
        patientsRecyclerView = view.findViewById(R.id.rv_patients);
        if (patientsRecyclerView == null) {
            Log.e(TAG, "patientsRecyclerView is null. Check fragment_patients.xml for ID 'rv_patients'");
            Toast.makeText(getContext(), "Failed to initialize patient list", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not logged in, redirecting to Login_Activity");
            startActivity(new Intent(getActivity(), Login_Activity.class));
            getActivity().finish();
            return view;
        }
        doctorUid = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Current doctorUid: " + doctorUid);

        // Initialize patient list and adapter
        patients = new ArrayList<>();
        patientAdapter = new PatientAdapter(patients, patientUid -> {
            Log.d(TAG, "Opening chat with patient: " + patientUid);
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("doctorUid", doctorUid);
            intent.putExtra("patientUid", patientUid);
            startActivity(intent);
        });
        patientsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        patientsRecyclerView.setAdapter(patientAdapter);

        // Load patients
        loadPatients();

        return view;
    }

    private void loadPatients() {
        DatabaseReference doctorRef = FirebaseDatabase.getInstance().getReference("users").child(doctorUid).child("patients");
        doctorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                patients.clear();
                if (!snapshot.exists()) {
                    Log.d(TAG, "No patients node found for doctor");
                    Toast.makeText(getContext(), "No linked patients found", Toast.LENGTH_SHORT).show();
                    patientAdapter.notifyDataSetChanged();
                    return;
                }
                for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                    String patientUid = patientSnapshot.getKey();
                    DatabaseReference patientRef = FirebaseDatabase.getInstance().getReference("users").child(patientUid);
                    patientRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            String name = userSnapshot.child("name").getValue(String.class);
                            String bloodGroup = userSnapshot.child("bloodGroup").getValue(String.class);
                            String conditions = userSnapshot.child("conditions").getValue(String.class);
                            String role = userSnapshot.child("role").getValue(String.class);
                            if ("patient".equals(role) && name != null) {
                                patients.add(new Patient(patientUid, name, bloodGroup != null ? bloodGroup : "N/A", conditions != null ? conditions : "N/A"));
                                patientAdapter.notifyDataSetChanged();
                                Log.d(TAG, "Added patient: " + name + " (" + patientUid + ")");
                            }
                            if (patients.isEmpty()) {
                                Toast.makeText(getContext(), "No linked patients found", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "Loaded " + patients.size() + " patients");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to load patient data for " + patientUid + ": " + error.getMessage());
                            Toast.makeText(getContext(), "Failed to load patient data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load patients: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load patients: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

class Patient {
    private String uid;
    private String name;
    private String bloodGroup;
    private String conditions;

    public Patient(String uid, String name, String bloodGroup, String conditions) {
        this.uid = uid;
        this.name = name;
        this.bloodGroup = bloodGroup;
        this.conditions = conditions;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public String getConditions() {
        return conditions;
    }
}