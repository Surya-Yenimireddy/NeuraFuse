package com.example.neurafuse.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {FocusSession.class}, version = 1, exportSchema = false)
public abstract class NeuraFuseDatabase extends RoomDatabase {

    private static volatile NeuraFuseDatabase INSTANCE;

    public abstract FocusSessionDao focusSessionDao();

    public static NeuraFuseDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NeuraFuseDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            NeuraFuseDatabase.class,
                            "neurafuse_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
