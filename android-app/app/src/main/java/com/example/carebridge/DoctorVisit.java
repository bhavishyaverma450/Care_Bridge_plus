package com.example.carebridge;

public class DoctorVisit {
    private String id;
    private String date;
    private String doctorName;
    private String time;
    private long timestamp;

    public DoctorVisit() {
    }

    public DoctorVisit(String id, String date, String doctorName, String time, long timestamp) {
        this.id = id;
        this.date = date;
        this.doctorName = doctorName;
        this.time = time;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
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
}