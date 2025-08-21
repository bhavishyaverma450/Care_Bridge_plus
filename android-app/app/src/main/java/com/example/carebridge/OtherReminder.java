package com.example.carebridge;

public class OtherReminder {
    private String title;
    private String reason;
    private String time;
    private String notes;
    private String id;

    public OtherReminder() {
        // Default constructor required for calls to DataSnapshot.getValue(OtherReminder.class)
    }

    public OtherReminder(String title, String reason, String time, String notes) {
        this.title = title;
        this.reason = reason;
        this.time = time;
        this.notes = notes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

