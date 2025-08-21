package com.example.carebridge;

public class AlertModel {
    private String alertId;
    private String message;
    private String timestamp;
    private String patientUid;

    public AlertModel() {
        // Required empty constructor for Firebase
    }

    public AlertModel(String alertId, String message, String timestamp, String patientUid) {
        this.alertId = alertId;
        this.message = message;
        this.timestamp = timestamp;
        this.patientUid = patientUid;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
    }
}
