package com.example.carebridge;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private String currentUserUid;
    private String otherUserUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize UI elements
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        // Check for null views
        if (chatRecyclerView == null || messageEditText == null || sendButton == null) {
            Log.e(TAG, "UI initialization failed: chatRecyclerView=" + chatRecyclerView + ", messageEditText=" + messageEditText + ", sendButton=" + sendButton);
            Toast.makeText(this, "Chat UI initialization failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize chat messages and adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, redirecting to Login_Activity");
            startActivity(new Intent(this, Login_Activity.class));
            finish();
            return;
        }
        currentUserUid = currentUser.getUid();
        Log.d(TAG, "Current user UID: " + currentUserUid);

        // Get doctorUid and patientUid from intent
        String doctorUid = getIntent().getStringExtra("doctorUid");
        String patientUid = getIntent().getStringExtra("patientUid");

        if (doctorUid != null && patientUid != null) {
            // Intent from PatientsFragment (doctor view)
            if (currentUserUid.equals(doctorUid)) {
                otherUserUid = patientUid;
            } else {
                otherUserUid = doctorUid;
            }
            Log.d(TAG, "Using doctorUid=" + doctorUid + ", patientUid=" + patientUid);
            loadChatMessages();
        } else {
            // Fallback to fetching doctorUid from Firebase (patient view)
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserUid);
            userRef.child("doctorUid").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    otherUserUid = snapshot.getValue(String.class);
                    if (otherUserUid == null) {
                        Log.w(TAG, "No doctor linked");
                        Toast.makeText(ChatActivity.this, "No doctor linked", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.d(TAG, "Fetched doctorUid: " + otherUserUid);
                        loadChatMessages();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load doctorUid: " + error.getMessage());
                    Toast.makeText(ChatActivity.this, "Failed to load doctor: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }

        // Send button click listener
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                Log.d(TAG, "Sending message: " + message);
                sendMessage(message);
            } else {
                Log.w(TAG, "Empty message, not sending");
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChatMessages() {
        String chatId = generateChatId(currentUserUid, otherUserUid);
        Log.d(TAG, "Loading messages for chatId: " + chatId);
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMessages.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    String text = messageSnapshot.child("text").getValue(String.class);
                    if (text == null) {
                        text = messageSnapshot.child("message").getValue(String.class);
                    }
                    String senderUid = messageSnapshot.child("senderUid").getValue(String.class);
                    if (senderUid == null) {
                        senderUid = messageSnapshot.child("senderId").getValue(String.class);
                    }
                    Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                    if (text != null && senderUid != null && timestamp != null) {
                        boolean isUser = senderUid.equals(currentUserUid);
                        chatMessages.add(new ChatMessage(text, isUser));
                    } else {
                        Log.w(TAG, "Skipping invalid message: " + messageSnapshot.getKey());
                    }
                }
                chatAdapter.notifyDataSetChanged();
                if (!chatMessages.isEmpty()) {
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }
                Log.d(TAG, "Loaded " + chatMessages.size() + " messages");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat messages: " + error.getMessage());
                Toast.makeText(ChatActivity.this, "Failed to load messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String message) {
        String chatId = generateChatId(currentUserUid, otherUserUid);
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).push();
        ChatMessage chatMessage = new ChatMessage(message, true);
        chatMessage.setSenderUid(currentUserUid);
        chatMessage.setReceiverUid(otherUserUid);
        chatMessage.setTimestamp(System.currentTimeMillis());

        chatRef.setValue(chatMessage)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Message sent successfully");
                    messageEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message: " + e.getMessage());
                    Toast.makeText(ChatActivity.this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String generateChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}