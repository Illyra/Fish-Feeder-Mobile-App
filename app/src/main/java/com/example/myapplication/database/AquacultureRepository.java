package com.example.myapplication.database;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class AquacultureRepository {
    private FishDao fishDao;
    private FeedingScheduleDao feedingScheduleDao;
    private LiveData<List<Fish>> allFish;
    
    public AquacultureRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        fishDao = db.fishDao();
        feedingScheduleDao = db.feedingScheduleDao();
        allFish = fishDao.getAllFish();
    }
    
    // Fish methods
    public LiveData<List<Fish>> getAllFish() {
        return fishDao.getAllFish();
    }
    
    public LiveData<Fish> getFishById(long id) {
        return fishDao.getFishById(id);
    }
    
    public LiveData<Fish> getFishByName(String name) {
        return fishDao.getFishByName(name);
    }
    
    public void insert(Fish fish) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            fishDao.insert(fish);
        });
    }
    
    public void update(Fish fish) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            fishDao.update(fish);
        });
    }
    
    public void delete(Fish fish) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            fishDao.delete(fish);
        });
    }

    public void deleteAllFish() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            fishDao.deleteAll();
        });
    }
    
    // Updated Feeding Schedule methods
    public LiveData<List<FeedingSchedule>> getAllSchedulesForFish(long fishId) {
        return feedingScheduleDao.getAllSchedulesForFish(fishId);
    }
    
    public LiveData<List<FeedingSchedule>> getSchedulesForDateRange(long fishId, long startDate, long endDate) {
        return feedingScheduleDao.getSchedulesForDateRange(fishId, startDate, endDate);
    }
    
    public LiveData<List<FeedingSchedule>> getSchedulesForDate(long fishId, long date) {
        return feedingScheduleDao.getSchedulesForDate(fishId, date);
    }
    
    public LiveData<List<FeedingSchedule>> getSchedulesByName(long fishId, String scheduleName) {
        return feedingScheduleDao.getSchedulesByName(fishId, scheduleName);
    }
    
    public void saveFeedingSchedule(FeedingSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            feedingScheduleDao.insert(schedule);
        });
    }
    
    public void saveFeedingSchedules(List<FeedingSchedule> schedules) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            feedingScheduleDao.insertAll(schedules);
        });
    }
    
    public void deleteSchedulesForDateRange(long fishId, long startDate, long endDate) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            feedingScheduleDao.deleteSchedulesForDateRange(fishId, startDate, endDate);
        });
    }
} 