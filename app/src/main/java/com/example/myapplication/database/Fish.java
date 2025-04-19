package com.example.myapplication.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;
import android.widget.ArrayAdapter;
import android.view.View;
import android.widget.TextView;

@Entity(tableName = "fish")
public class Fish {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String name;
    private int totalCount;
    private int aliveCount;
    private int deadCount;
    private float averageLength;
    private float averageWidth;
    private float averageWeight;
    private float feedPerFish;
    private Date dateAdded;
    private Date lastUpdated;
    private String notes;
    private String phoneNumber;
    
    // Constructor
    public Fish(String name, int totalCount, int aliveCount, int deadCount, 
                float averageLength, float averageWidth, float averageWeight, 
                float feedPerFish, Date dateAdded, Date lastUpdated, String notes,
                String phoneNumber) {
        this.name = name;
        this.totalCount = totalCount;
        this.aliveCount = aliveCount;
        this.deadCount = deadCount;
        this.averageLength = averageLength;
        this.averageWidth = averageWidth;
        this.averageWeight = averageWeight;
        this.feedPerFish = feedPerFish;
        this.dateAdded = dateAdded;
        this.lastUpdated = lastUpdated;
        this.notes = notes;
        this.phoneNumber = phoneNumber;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    
    public int getAliveCount() { return aliveCount; }
    public void setAliveCount(int aliveCount) { this.aliveCount = aliveCount; }
    
    public int getDeadCount() { return deadCount; }
    public void setDeadCount(int deadCount) { this.deadCount = deadCount; }
    
    public float getAverageLength() { return averageLength; }
    public void setAverageLength(float averageLength) { this.averageLength = averageLength; }
    
    public float getAverageWidth() { return averageWidth; }
    public void setAverageWidth(float averageWidth) { this.averageWidth = averageWidth; }
    
    public float getAverageWeight() { return averageWeight; }
    public void setAverageWeight(float averageWeight) { this.averageWeight = averageWeight; }
    
    public float getFeedPerFish() { return feedPerFish; }
    public void setFeedPerFish(float feedPerFish) { this.feedPerFish = feedPerFish; }
    
    public Date getDateAdded() { return dateAdded; }
    public void setDateAdded(Date dateAdded) { this.dateAdded = dateAdded; }
    
    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @Override
    public String toString() {
        return name;
    }
} 