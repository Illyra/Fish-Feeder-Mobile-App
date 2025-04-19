package com.example.myapplication.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Fish.class, FeedingSchedule.class}, version = 6, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    public abstract FishDao fishDao();
    public abstract FeedingScheduleDao feedingScheduleDao();
    
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
        Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "aquaculture_database")
                            .addCallback(sRoomDatabaseCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    
    // Callback to prepopulate the database with default fish
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            
            databaseWriteExecutor.execute(() -> {
                FishDao dao = INSTANCE.fishDao();
                
                // Update default fish creation with phone number
                Date now = new Date();
                Fish bangus = new Fish("Bangus", 0, 0, 0, 0, 0, 0, 0, now, now, "", "");
                Fish tilapia = new Fish("Tilapia", 0, 0, 0, 0, 0, 0, 0, now, now, "", "");
                
                dao.insert(bangus);
                dao.insert(tilapia);
            });
        }
    };
} 