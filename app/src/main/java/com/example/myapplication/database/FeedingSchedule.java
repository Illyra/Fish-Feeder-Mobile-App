package com.example.myapplication.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feeding_schedules")
public class FeedingSchedule {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long fishId;
    private String scheduleName;
    private long startDate;
    private long endDate;
    private String feedingTime;
    private float feedQuantity;
    
    // Constructor
    public FeedingSchedule(long fishId, String scheduleName, long startDate, long endDate, 
                          String feedingTime, float feedQuantity) {
        this.fishId = fishId;
        this.scheduleName = scheduleName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.feedingTime = feedingTime;
        this.feedQuantity = feedQuantity;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getFishId() { return fishId; }
    public void setFishId(long fishId) { this.fishId = fishId; }
    
    public String getScheduleName() { return scheduleName; }
    public void setScheduleName(String scheduleName) { this.scheduleName = scheduleName; }
    
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    
    public String getFeedingTime() { return feedingTime; }
    public void setFeedingTime(String feedingTime) { this.feedingTime = feedingTime; }
    
    public float getFeedQuantity() { return feedQuantity; }
    public void setFeedQuantity(float feedQuantity) { this.feedQuantity = feedQuantity; }
} 