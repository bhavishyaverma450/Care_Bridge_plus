package com.example.carebridge.models;

public class Patient {
    private String uid;
    private String name;
    private String disease;
    private String age;

    public Patient() {
        // Default constructor required for Firebase
    }

    public Patient(String uid, String name, String disease, String age) {
        this.uid = uid;
        this.name = name;
        this.disease = disease;
        this.age = age;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getDisease() { return disease; }
    public String getAge() { return age; }
}