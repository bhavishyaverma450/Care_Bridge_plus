package com.example.carebridge;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ChatBookingActivity extends AppCompatActivity {
    private static final String TAG = "ChatBookingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_booking);

        // Initialize UI elements
        Button bookAppointmentButton = findViewById(R.id.bookAppointmentButton);
        Button chatDoctorButton = findViewById(R.id.chatDoctorButton);
        Button chatAiButton = findViewById(R.id.chatAiButton);

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        // Book Appointment
        bookAppointmentButton.setOnClickListener(v -> startActivity(new Intent(ChatBookingActivity.this, BookAppointmentActivity.class)));

        // Chat with Doctor
        chatDoctorButton.setOnClickListener(v -> FirebaseDatabase.getInstance().getReference("users").child(userId).child("doctorUid")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String doctorId = snapshot.getValue(String.class);
                        if (doctorId != null) {
                            startActivity(new Intent(ChatBookingActivity.this, ChatActivity.class).putExtra("doctorId", doctorId));
                        } else {
                            Log.w(TAG, "No doctor linked");
                            Toast.makeText(ChatBookingActivity.this, "No doctor linked", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch doctorUid: " + error.getMessage());
                        Toast.makeText(ChatBookingActivity.this, "Failed to fetch doctor: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }));

        // Chat with AI
        chatAiButton.setOnClickListener(v -> startActivity(new Intent(ChatBookingActivity.this, ChatAIActivity.class)));
    }
}