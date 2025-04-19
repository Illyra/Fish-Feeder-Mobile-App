package com.example.myapplication.ui.schedule;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.Observer;

import com.example.myapplication.FeedingScheduleSetup;
import com.example.myapplication.R;
import com.example.myapplication.database.FeedingSchedule;
import com.example.myapplication.viewmodel.AquacultureViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages the display of feeding schedules in the UI
 */
public class ScheduleDisplayManager {
    private final FeedingScheduleSetup activity;
    private final AquacultureViewModel viewModel;
    private LinearLayout schedulesContainer;
    private ScheduleActionListener listener;

    public ScheduleDisplayManager(FeedingScheduleSetup activity, AquacultureViewModel viewModel) {
        this.activity = activity;
        this.viewModel = viewModel;
        this.schedulesContainer = activity.findViewById(R.id.schedulesContainer);
    }

    /**
     * Set the listener for schedule actions
     */
    public void setListener(ScheduleActionListener listener) {
        this.listener = listener;
    }

    /**
     * Loads and displays schedules for the selected fish
     */
    public void loadExistingSchedules(long fishId) {
        if (fishId == -1) return;

        Log.d("ScheduleDisplayManager", "Loading schedules for fish ID: " + fishId);

        viewModel.getAllSchedulesForFish(fishId).observe(activity, schedules -> {
            if (schedules != null) {
                Log.d("ScheduleDisplayManager", "Loaded schedules: " + schedules.size());
                
                // Group schedules by name and date range (not just name)
                Map<String, List<FeedingSchedule>> groupedSchedules = new HashMap<>();
                
                for (FeedingSchedule schedule : schedules) {
                    // Create unique key using name AND date range
                    String key = String.format("%s_%d_%d", 
                        schedule.getScheduleName(),
                        schedule.getStartDate(),
                        schedule.getEndDate());
                    if (!groupedSchedules.containsKey(key)) {
                        groupedSchedules.put(key, new ArrayList<>());
                    }
                    groupedSchedules.get(key).add(schedule);
                }

                // Clear existing views
                if (schedulesContainer != null) {
                    schedulesContainer.removeAllViews();

                    // Create a list of schedule groups for sorting
                    List<List<FeedingSchedule>> scheduleGroups = new ArrayList<>(groupedSchedules.values());
                    
                    // Sort by start date (chronological order)
                    Collections.sort(scheduleGroups, (group1, group2) -> {
                        if (group1.isEmpty() || group2.isEmpty()) {
                            return 0;
                        }
                        long date1 = group1.get(0).getStartDate();
                        long date2 = group2.get(0).getStartDate();
                        Log.d("SortingDebug", "Comparing dates: " + date1 + " vs " + date2);
                        return Long.compare(date1, date2); // Ascending order (oldest first)
                    });

                    // Debug the sorting
                    for (List<FeedingSchedule> group : scheduleGroups) {
                        if (!group.isEmpty()) {
                            FeedingSchedule schedule = group.get(0);
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            String dateStr = sdf.format(new Date(schedule.getStartDate()));
                            Log.d("SortedSchedules", "Schedule: " + schedule.getScheduleName() + 
                                  " Date: " + dateStr + " Timestamp: " + schedule.getStartDate());
                        }
                    }

                    // Add schedule groups to the container in sorted order
                    for (List<FeedingSchedule> group : scheduleGroups) {
                        if (!group.isEmpty()) {
                            addScheduleGroupToContainer(group, schedulesContainer);
                        }
                    }
                } else {
                    Log.e("ScheduleDisplayManager", "schedulesContainer is null");
                }
            } else {
                Log.e("ScheduleDisplayManager", "Loaded schedules is null");
            }
        });
    }

    /**
     * Adds a schedule group card to the container
     */
    private void addScheduleGroupToContainer(List<FeedingSchedule> scheduleGroup, LinearLayout container) {
        if (scheduleGroup.isEmpty()) return;
        
        FeedingSchedule firstSchedule = scheduleGroup.get(0);
        String scheduleName = firstSchedule.getScheduleName();
        long startDate = firstSchedule.getStartDate();
        long endDate = firstSchedule.getEndDate();
        
        // Create the schedule card view
        View scheduleCard = activity.getLayoutInflater().inflate(R.layout.schedule_group_item, container, false);
        
        // Set the schedule name
        TextView scheduleNameText = scheduleCard.findViewById(R.id.scheduleNameText);
        if (scheduleNameText != null) {
            scheduleNameText.setText(scheduleName);
        }
        
        // Format and set the date range
        TextView dateRangeText = scheduleCard.findViewById(R.id.dateRangeText);
        if (dateRangeText != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateRange = sdf.format(new Date(startDate)) + " - " + sdf.format(new Date(endDate));
            dateRangeText.setText(dateRange);
            
            // Log the date for debugging
            Log.d("ScheduleCard", "Adding card with date range: " + dateRange + 
                  " (Start timestamp: " + startDate + ")");
        }
        
        // Setup edit button
        Button editButton = scheduleCard.findViewById(R.id.editScheduleButton);
        if (editButton != null && listener != null) {
            editButton.setOnClickListener(v -> {
                listener.onScheduleEditRequested(scheduleGroup);
            });
        }
        
        // Setup delete button
        Button deleteButton = scheduleCard.findViewById(R.id.deleteScheduleButton);
        if (deleteButton != null && listener != null) {
            deleteButton.setOnClickListener(v -> {
                listener.onScheduleDeleteRequested(firstSchedule.getFishId(), startDate, endDate);
            });
        }
        
        // Add the card to the container
        container.addView(scheduleCard);
    }

    /**
     * Updates the schedule display with latest data
     */
    public void updateScheduleDisplay(Map<String, List<FeedingSchedule>> schedulesByName) {
        if (schedulesContainer == null) return;
        
        schedulesContainer.removeAllViews();
        
        // Convert the map values to a list for sorting
        List<List<FeedingSchedule>> scheduleGroups = new ArrayList<>(schedulesByName.values());
        
        // Sort the groups by start date (chronological order)
        Collections.sort(scheduleGroups, (group1, group2) -> {
            if (group1.isEmpty() || group2.isEmpty()) {
                return 0;
            }
            long date1 = group1.get(0).getStartDate();
            long date2 = group2.get(0).getStartDate();
            return Long.compare(date1, date2); // Ascending order (oldest first)
        });
        
        // Add the sorted groups to the container
        for (List<FeedingSchedule> scheduleGroup : scheduleGroups) {
            if (!scheduleGroup.isEmpty()) {
                addScheduleGroupToContainer(scheduleGroup, schedulesContainer);
            }
        }
    }
} 