package com.example.myapplication.ui.schedule;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.constraintlayout.widget.Group;

import com.example.myapplication.FeedingScheduleSetup;
import com.example.myapplication.R;
import com.example.myapplication.database.FeedingSchedule;
import com.example.myapplication.database.Fish;
import com.example.myapplication.utils.SmsUtils;
import com.example.myapplication.viewmodel.AquacultureViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manages the schedule editor functionality
 */
public class ScheduleEditorManager {
    private final FeedingScheduleSetup activity;
    private final AquacultureViewModel viewModel;
    private final ScheduleFeedingTimeManager feedingTimeManager;
    
    private LinearLayout dailySchedulesContainer;
    private Group dailySchedContainerGroup;
    private Group mainContainerContentGroup;
    
    private String currentScheduleName = "";
    private Long currentStartDate = null;
    private Long currentEndDate = null;
    private long currentFishId = -1;
    private boolean isEditingExistingSchedule = false;

    public ScheduleEditorManager(FeedingScheduleSetup activity, AquacultureViewModel viewModel, ScheduleFeedingTimeManager feedingTimeManager) {
        this.activity = activity;
        this.viewModel = viewModel;
        this.feedingTimeManager = feedingTimeManager;
        
        // Initialize views
        this.dailySchedulesContainer = activity.findViewById(R.id.dailySchedulesContainer);
        this.dailySchedContainerGroup = activity.findViewById(R.id.dailySchedContainerGroup);
        this.mainContainerContentGroup = activity.findViewById(R.id.mainContainerContentGroup);
    }

    /**
     * Shows the daily schedule editor
     */
    public void showDailyScheduleEditor() {
        // Clear any previous schedule name
        EditText scheduleNameInput = activity.findViewById(R.id.scheduleNameInput);
        if (scheduleNameInput != null) {
            scheduleNameInput.setText("");
        }
        
        // Hide the main container and show the editor
        if (dailySchedContainerGroup != null) {
            dailySchedContainerGroup.setVisibility(View.VISIBLE);
        }
        if (mainContainerContentGroup != null) {
            mainContainerContentGroup.setVisibility(View.GONE);
        }
        
        // Clear any existing feeding times
        if (dailySchedulesContainer != null) {
            dailySchedulesContainer.removeAllViews();
            feedingTimeManager.addNewFeedingTime();
        }
    }

    /**
     * Hides the daily schedule editor
     */
    public void hideDailyScheduleEditor() {
        if (dailySchedContainerGroup != null) {
            dailySchedContainerGroup.setVisibility(View.GONE);
        }
        if (mainContainerContentGroup != null) {
            mainContainerContentGroup.setVisibility(View.VISIBLE);
        }
        if (dailySchedulesContainer != null) {
            dailySchedulesContainer.removeAllViews();
        }
        
        // Reset current schedule data
        currentScheduleName = null;
        currentStartDate = null;
        currentEndDate = null;
        isEditingExistingSchedule = false; // Reset the editing flag
    }

    /**
     * Validates and saves the schedule
     */
    public void validateAndSaveSchedule(FishSelectionManager.FishItem selectedFish) {
        if (selectedFish == null || selectedFish.name.equals("Select Fish")) {
            Toast.makeText(activity, "Please select a fish type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentScheduleName)) {
            Toast.makeText(activity, "Please enter a schedule name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentStartDate == null || currentEndDate == null) {
            Toast.makeText(activity, "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }

        // If we're editing an existing schedule, skip the date range validation
        if (isEditingExistingSchedule) {
            if (validateRemainingFields()) {
                saveSchedule(selectedFish);
            }
            return;
        }

        // Only check for existing schedules if creating a new schedule
        viewModel.getSchedulesForDateRange(selectedFish.id, currentStartDate, currentEndDate)
            .observe(activity, existingSchedules -> {
                if (existingSchedules != null && !existingSchedules.isEmpty()) {
                    Toast.makeText(activity, "A schedule already exists for this date range", Toast.LENGTH_SHORT).show();
                } else {
                    // Continue with validation and saving
                    if (validateRemainingFields()) {
                        saveSchedule(selectedFish);
                    }
                }
            });
    }

    /**
     * Validates the remaining schedule fields
     */
    private boolean validateRemainingFields() {
        // Check if there are any feeding times
        if (dailySchedulesContainer.getChildCount() == 0) {
            Toast.makeText(activity, "Please add at least one feeding time", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate each feeding time entry
        for (int i = 0; i < dailySchedulesContainer.getChildCount(); i++) {
            View card = dailySchedulesContainer.getChildAt(i);
            
            EditText timeInput = card.findViewById(R.id.weekNumTimeNum);
            TextInputEditText quantityInput = card.findViewById(R.id.feedQuantityInput);
            
            String time = timeInput.getText().toString();
            String quantityText = quantityInput.getText() != null ? quantityInput.getText().toString() : "";
            
            if (TextUtils.isEmpty(time)) {
                Toast.makeText(activity, "Please select time for all entries", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (TextUtils.isEmpty(quantityText)) {
                Toast.makeText(activity, "Please enter feed quantity for all entries", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            try {
                float quantity = Float.parseFloat(quantityText);
                if (quantity <= 0) {
                    Toast.makeText(activity, "Feed quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(activity, "Please enter valid feed quantity", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        return true;
    }

    /**
     * Collects feeding schedules from the UI
     */
    public List<FeedingSchedule> collectFeedingSchedules() {
        List<FeedingSchedule> schedules = new ArrayList<>();
        
        for (int i = 0; i < dailySchedulesContainer.getChildCount(); i++) {
            View card = dailySchedulesContainer.getChildAt(i);
            
            EditText timeInput = card.findViewById(R.id.weekNumTimeNum);
            TextInputEditText quantityInput = card.findViewById(R.id.feedQuantityInput);
            
            String time = timeInput.getText().toString();
            String quantityText = quantityInput.getText() != null ? quantityInput.getText().toString() : "";
            
            if (!TextUtils.isEmpty(time) && !TextUtils.isEmpty(quantityText)) {
                try {
                    float quantity = Float.parseFloat(quantityText);
                    
                    // Use the constructor with all required parameters
                    FeedingSchedule schedule = new FeedingSchedule(
                        currentFishId,
                        currentScheduleName,
                        currentStartDate,
                        currentEndDate,
                        time,
                        quantity
                    );
                    
                    schedules.add(schedule);
                } catch (NumberFormatException e) {
                    Log.e("FeedingScheduleSetup", "Error parsing quantity", e);
                }
            }
        }
        
        // Sort schedules by time
        Collections.sort(schedules, (s1, s2) -> {
            try {
                SimpleDateFormat parser = new SimpleDateFormat("h:mm a", Locale.getDefault());
                Date time1 = parser.parse(s1.getFeedingTime());
                Date time2 = parser.parse(s2.getFeedingTime());
                return time1.compareTo(time2);
            } catch (ParseException e) {
                Log.e("TimeSort", "Error parsing time", e);
                return 0;
            }
        });
        
        return schedules;
    }

    /**
     * Saves the schedule to the database
     */
    private void saveSchedule(FishSelectionManager.FishItem selectedFish) {
        List<FeedingSchedule> schedules = collectFeedingSchedules();
        
        // Save new schedules with correct fishId
        for (FeedingSchedule schedule : schedules) {
            schedule.setFishId(selectedFish.id);
            Log.d("SaveSchedule", "Creating schedule with fishId: " + schedule.getFishId());
        }
        
        // If editing existing schedule, use updateSchedulesForDateRange instead of saveFeedingSchedules
        if (isEditingExistingSchedule) {
            viewModel.updateSchedulesForDateRange(
                selectedFish.id,
                currentStartDate,
                currentEndDate,
                schedules
            );
        } else {
            viewModel.saveFeedingSchedules(schedules);
        }
        
        // Get fish info to check for phone number
        viewModel.getFishById(selectedFish.id).observe(activity, fish -> {
            if (fish != null && !TextUtils.isEmpty(fish.getPhoneNumber())) {
                // Send SMS notification for the schedule (both new and edited)
                SmsUtils.sendScheduleNotification(activity, fish.getPhoneNumber(), fish.getName(), schedules);
            }
            
            hideDailyScheduleEditor();
            Toast.makeText(activity, isEditingExistingSchedule ? 
                "Schedule updated successfully!" : 
                "Schedule saved successfully!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Prepares for editing an existing schedule
     */
    public void editExistingSchedule(List<FeedingSchedule> scheduleGroup) {
        if (scheduleGroup == null || scheduleGroup.isEmpty()) return;
        
        // Store the schedule information for editing
        FeedingSchedule template = scheduleGroup.get(0);
        currentScheduleName = template.getScheduleName();
        currentStartDate = template.getStartDate();
        currentEndDate = template.getEndDate();
        currentFishId = template.getFishId();
        isEditingExistingSchedule = true; // Flag to indicate we're editing an existing schedule
        
        // Show existing schedule times for editing
        for (FeedingSchedule schedule : scheduleGroup) {
            feedingTimeManager.addExistingFeedingTime(schedule.getFeedingTime(), schedule.getFeedQuantity());
        }
    }

    /**
     * Sets the schedule parameters
     */
    public void setScheduleParams(String scheduleName, Long startDate, Long endDate, long fishId) {
        this.currentScheduleName = scheduleName;
        this.currentStartDate = startDate;
        this.currentEndDate = endDate;
        this.currentFishId = fishId;
    }
} 