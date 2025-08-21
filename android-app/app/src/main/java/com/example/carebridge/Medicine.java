package com.example.carebridge;

public class Medicine {
    private String id;
    private String name;
    private String time;
    private boolean taken;
    private String dosage;
    private String dosageUnit;
    private String specialNotes;
    private long timestamp;

    public Medicine() {
        // Default constructor required for calls to DataSnapshot.getValue(Medicine.class)
    }

    public Medicine(String name, String time, boolean taken, String dosage, String dosageUnit, String specialNotes) {
        this.name = name;
        this.time = time;
        this.taken = taken;
        this.dosage = dosage;
        this.dosageUnit = dosageUnit;
        this.specialNotes = specialNotes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isTaken() {
        return taken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getDosageUnit() {
        return dosageUnit;
    }

    public void setDosageUnit(String dosageUnit) {
        this.dosageUnit = dosageUnit;
    }

    public String getSpecialNotes() {
        return specialNotes;
    }

    public void setSpecialNotes(String specialNotes) {
        this.specialNotes = specialNotes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
