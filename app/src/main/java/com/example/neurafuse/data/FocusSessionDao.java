package com.example.neurafuse.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FocusSessionDao {

    @Insert
    void insert(FocusSession session);

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    LiveData<List<FocusSession>> getAllSessions();

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC LIMIT 5")
    List<FocusSession> getRecentSessions();

    @Query("SELECT * FROM focus_sessions WHERE startTime >= :startOfDay ORDER BY startTime DESC")
    List<FocusSession> getSessionsToday(long startOfDay);

    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM focus_sessions WHERE startTime >= :startOfDay")
    long getTotalFocusTimeToday(long startOfDay);

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE startTime >= :startOfDay AND completed = 1")
    int getCompletedSessionsToday(long startOfDay);

    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM focus_sessions WHERE startTime >= :weekStart")
    long getTotalFocusTimeThisWeek(long weekStart);

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE startTime >= :weekStart AND completed = 1")
    int getCompletedSessionsThisWeek(long weekStart);

    /** Get distinct days with completed sessions (for streak calculation) */
    @Query("SELECT DISTINCT (startTime / 86400000) as dayKey FROM focus_sessions WHERE completed = 1 ORDER BY dayKey DESC")
    List<Long> getCompletedSessionDays();

    @Query("DELETE FROM focus_sessions")
    void deleteAll();
}
