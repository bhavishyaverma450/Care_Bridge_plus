package com.example.carebridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PillIntakeReceiver extends BroadcastReceiver {
    private static final String TAG = "PillIntakeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean taken = intent.getBooleanExtra("taken", false);
        String pillName = intent.getStringExtra("pillName");

        if (pillName == null) {
            Log.e(TAG, "Pill name not provided");
            return;
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("medicines");

        userRef.orderByChild("name").equalTo(pillName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String medicineId = ds.getKey();
                    if (medicineId != null) {
                        ds.getRef().child("taken").setValue(taken)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Medicine intake updated: " + pillName + ", taken: " + taken))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update medicine intake: " + e.getMessage(), e));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch medicines: " + error.getMessage(), error.toException());
            }
        });
    }
}