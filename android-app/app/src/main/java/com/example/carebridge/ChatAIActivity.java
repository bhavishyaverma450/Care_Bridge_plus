package com.example.carebridge;

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
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;

public class ChatAIActivity extends AppCompatActivity {
    private static final String TAG = "ChatAIActivity";
    private static final String API_KEY = "AIzaSyADBcEjDyi4hvkIIq7wrBYkYKnzGmZ_8ZM"; // Replace with your actual key
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_ai);

        // Initialize UI elements
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        // Initialize chat messages and adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize Gemini model
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Send button click listener
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                Log.d(TAG, "Sending message to AI: " + message);
                addMessageToChat(message, true); // User message
                messageEditText.setText("");
                getAiResponse(message, model);
            } else {
                Log.w(TAG, "Empty message, not sending");
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMessageToChat(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private void getAiResponse(String userMessage, GenerativeModelFutures model) {
        Content content = new Content.Builder()
                .addText(userMessage)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiResponse = result.getText();
                Log.d(TAG, "AI response received: " + aiResponse);
                runOnUiThread(() -> addMessageToChat(aiResponse, false)); // AI message
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(TAG, "Failed to get AI response: " + t.getMessage(), t);
                runOnUiThread(() -> {
                    Toast.makeText(ChatAIActivity.this, "Failed to get AI response: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    addMessageToChat("Error: " + t.getMessage(), false);
                });
            }
        }, MoreExecutors.directExecutor());
    }
}