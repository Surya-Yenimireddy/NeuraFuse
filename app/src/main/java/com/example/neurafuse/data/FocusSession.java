package com.example.neurafuse.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_sessions")
public class FocusSession {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long startTime;
    public long endTime;
    public long durationMillis;
    public boolean completed;
    public String sessionType; // "pomodoro", "deep_work", "study", "custom"

    public FocusSession() {}

    public FocusSession(long startTime, long endTime, long durationMillis, boolean completed, String sessionType) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMillis = durationMillis;
        this.completed = completed;
        this.sessionType = sessionType;
    }

    /** Duration in readable format */
    public String getReadableDuration() {
        long totalMinutes = durationMillis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0 && minutes > 0) return hours + "h " + minutes + "m";
        else if (hours > 0) return hours + "h";
        else return minutes + "m";
    }
}
