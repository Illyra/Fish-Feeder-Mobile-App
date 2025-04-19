package com.example.myapplication.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.Fish;
import com.example.myapplication.database.FishDao;
import com.example.myapplication.database.FeedingSchedule;
import com.example.myapplication.database.FeedingScheduleDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AquacultureViewModel extends AndroidViewModel {
    private FishDao fishDao;
    private FeedingScheduleDao feedingScheduleDao;
    private LiveData<List<Fish>> allFish;
    private ExecutorService executor;
    
    public AquacultureViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        fishDao = db.fishDao();
        feedingScheduleDao = db.feedingScheduleDao();
        allFish = fishDao.getAllFish();
        executor = Executors.newSingleThreadExecutor();
    }
    
    // Fish operations
    public LiveData<List<Fish>> getAllFish() {
        return allFish;
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
    
    // Updated Feeding Schedule operations
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
    
    public LiveData<Boolean> deleteSchedulesForDateRange(long fishId, long startDate, long endDate) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                feedingScheduleDao.deleteSchedulesForDateRange(fishId, startDate, endDate);
                result.postValue(true);
            } catch (Exception e) {
                result.postValue(false);
            }
        });
        return result;
    }
    
    public LiveData<List<FeedingSchedule>> getAllSchedules() {
        return feedingScheduleDao.getAllSchedules();
    }
    
    public void deleteFeedingSchedule(FeedingSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            feedingScheduleDao.delete(schedule);
        });
    }
    
    public void updateFeedingSchedule(FeedingSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            feedingScheduleDao.update(schedule);
        });
    }
    
    public void updateSchedulesForDateRange(long fishId, long startDate, long endDate, List<FeedingSchedule> newSchedules) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Log deletion parameters for debugging
                Log.d("AquacultureViewModel", "Updating schedules - fishId: " + fishId + 
                      ", startDate: " + startDate + ", endDate: " + endDate);
                
                if (newSchedules.isEmpty()) {
                    Log.d("AquacultureViewModel", "No new schedules to add, aborting update");
                    return;
                }
                
                String scheduleName = newSchedules.get(0).getScheduleName();
                
                // Use more specific deletion query
                int deletedCount = feedingScheduleDao.deleteSchedulesByNameAndDateRange(
                    fishId, scheduleName, startDate, endDate);
                Log.d("AquacultureViewModel", "Deleted " + deletedCount + " schedules");
                
                // Then insert new schedules
                Log.d("AquacultureViewModel", "Inserting " + newSchedules.size() + " new schedules");
                feedingScheduleDao.insertAll(newSchedules);
                
                // Clean up any duplicates that might have been created
                int removedDuplicates = feedingScheduleDao.removeDuplicateSchedulesForFish(fishId);
                Log.d("AquacultureViewModel", "Removed " + removedDuplicates + " duplicate schedules");
                
            } catch (Exception e) {
                Log.e("AquacultureViewModel", "Error updating schedules: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Cleans up duplicate schedules for a specific fish
     */
    public LiveData<Integer> cleanupDuplicateSchedulesForFish(long fishId) {
        MutableLiveData<Integer> result = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                int count = feedingScheduleDao.removeDuplicateSchedulesForFish(fishId);
                result.postValue(count);
            } catch (Exception e) {
                Log.e("AquacultureViewModel", "Error cleaning up duplicates: " + e.getMessage(), e);
                result.postValue(0);
            }
        });
        return result;
    }
    
    /**
     * Cleans up all duplicate schedules in the database
     */
    public LiveData<Integer> cleanupAllDuplicateSchedules() {
        MutableLiveData<Integer> result = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                int count = feedingScheduleDao.removeDuplicateSchedules();
                result.postValue(count);
            } catch (Exception e) {
                Log.e("AquacultureViewModel", "Error cleaning up all duplicates: " + e.getMessage(), e);
                result.postValue(0);
            }
        });
        return result;
    }
    
    public LiveData<Long> getFishIdByName(String name) {
        return fishDao.getFishIdByName(name);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
} 