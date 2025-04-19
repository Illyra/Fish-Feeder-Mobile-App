package com.example.myapplication.ui.schedule;

import com.example.myapplication.database.FeedingSchedule;
import java.util.List;

/**
 * Interface for handling schedule-related actions
 */
public interface ScheduleActionListener {
    /**
     * Called when a schedule edit is requested
     */
    void onScheduleEditRequested(List<FeedingSchedule> schedules);
    
    /**
     * Called when a schedule deletion is requested
     */
    void onScheduleDeleteRequested(long fishId, long startDate, long endDate);
    
    /**
     * Called after a schedule has been successfully saved
     */
    void onScheduleSaved();
} 