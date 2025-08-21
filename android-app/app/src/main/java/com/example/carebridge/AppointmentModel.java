package com.example.carebridge;

public class AppointmentModel {
    private String appointmentId;
    private String date;
    private String doctorUid;
    private String patientUid;
    private String reason;
    private String time;
    private long timestamp;
    private String doctor;

    public AppointmentModel() {
    }

    public AppointmentModel(String appointmentId, String date, String time, String reason, String patientUid, String doctor) {
        this.appointmentId = appointmentId;
        this.date = date;
        this.time = time;
        this.reason = reason;
        this.patientUid = patientUid;
        this.doctor = doctor;
        this.timestamp = System.currentTimeMillis();
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDoctorUid() {
        return doctorUid;
    }

    public void setDoctorUid(String doctorUid) {
        this.doctorUid = doctorUid;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }
}