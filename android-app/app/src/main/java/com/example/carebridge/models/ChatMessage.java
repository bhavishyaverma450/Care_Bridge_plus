package com.example.carebridge.models;

public class ChatMessage {
    private String senderUid;
    private String text;
    private long timestamp;

    public ChatMessage() {
        // Default constructor required for Firebase
    }

    public ChatMessage(String senderUid, String text, long timestamp) {
        this.senderUid = senderUid;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderUid() { return senderUid; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}