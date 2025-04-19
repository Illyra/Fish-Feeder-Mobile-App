package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FeedingScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FeedingSchedule schedule);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<FeedingSchedule> schedules);
    
    @Update
    void update(FeedingSchedule schedule);
    
    @Delete
    void delete(FeedingSchedule schedule);
    
    @Query("SELECT * FROM feeding_schedules WHERE fishId = :fishId")
    LiveData<List<FeedingSchedule>> getAllSchedulesForFish(long fishId);
    
    @Query("SELECT * FROM feeding_schedules WHERE fishId = :fishId AND ((startDate <= :endDate AND endDate >= :startDate))")
    LiveData<List<FeedingSchedule>> getSchedulesForDateRange(long fishId, long startDate, long endDate);
    
    @Query("SELECT * FROM feeding_schedules WHERE fishId = :fishId AND ((startDate <= :endDate AND endDate >= :startDate))")
    List<FeedingSchedule> getSchedulesForDateRangeSync(long fishId, long startDate, long endDate);

    @Query("DELETE FROM feeding_schedules WHERE fishId = :fishId AND startDate = :startDate AND endDate = :endDate")
    int deleteSchedulesForDateRange(long fishId, long startDate, long endDate);
    
    @Query("DELETE FROM feeding_schedules WHERE fishId = :fishId AND scheduleName = :scheduleName AND startDate = :startDate AND endDate = :endDate")
    int deleteSchedulesByNameAndDateRange(long fishId, String scheduleName, long startDate, long endDate);
    
    @Query("DELETE FROM feeding_schedules WHERE id NOT IN (SELECT MIN(id) FROM feeding_schedules GROUP BY fishId, scheduleName, startDate, endDate, feedingTime)")
    int removeDuplicateSchedules();
    
    @Query("DELETE FROM feeding_schedules WHERE fishId = :fishId AND id NOT IN (SELECT MIN(id) FROM feeding_schedules WHERE fishId = :fishId GROUP BY scheduleName, startDate, endDate, feedingTime)")
    int removeDuplicateSchedulesForFish(long fishId);
    
    @Query("SELECT * FROM feeding_schedules WHERE fishId = :fishId AND :date BETWEEN startDate AND endDate")
    LiveData<List<FeedingSchedule>> getSchedulesForDate(long fishId, long date);
    
    @Query("SELECT * FROM feeding_schedules WHERE fishId = :fishId AND scheduleName = :scheduleName")
    LiveData<List<FeedingSchedule>> getSchedulesByName(long fishId, String scheduleName);
    
    @Query("SELECT * FROM feeding_schedules")
    LiveData<List<FeedingSchedule>> getAllSchedules();
} 