package com.example.myapplication.ui.schedule;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.myapplication.FeedingScheduleSetup;
import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Manages feeding time entries in the schedule editor
 */
public class ScheduleFeedingTimeManager {
    private final FeedingScheduleSetup activity;
    private final LinearLayout dailySchedulesContainer;

    public ScheduleFeedingTimeManager(FeedingScheduleSetup activity) {
        this.activity = activity;
        this.dailySchedulesContainer = activity.findViewById(R.id.dailySchedulesContainer);
    }

    /**
     * Adds a new feeding time UI component
     */
    public void addNewFeedingTime() {
        View feedingTimeCard = activity.getLayoutInflater().inflate(R.layout.feeding_time_card, dailySchedulesContainer, false);
        
        // Setup the time picker and delete functionality
        setupTimePicker(feedingTimeCard);
        setupDeleteButton(feedingTimeCard);
        
        dailySchedulesContainer.addView(feedingTimeCard);
    }

    /**
     * Adds UI for an existing feeding time
     */
    public void addExistingFeedingTime(String time, float quantity) {
        View feedingTimeCard = activity.getLayoutInflater().inflate(R.layout.feeding_time_card, dailySchedulesContainer, false);
        
        EditText timeInput = feedingTimeCard.findViewById(R.id.weekNumTimeNum);
        TextInputEditText quantityInput = feedingTimeCard.findViewById(R.id.feedQuantityInput);
        
        timeInput.setText(time);
        quantityInput.setText(String.valueOf(quantity));
        
        // Setup the time picker and delete functionality
        setupTimePicker(feedingTimeCard);
        setupDeleteButton(feedingTimeCard);
        
        dailySchedulesContainer.addView(feedingTimeCard);
    }

    /**
     * Sets up the time picker for a feeding time card
     */
    private void setupTimePicker(View feedingTimeCard) {
        EditText timeInput = feedingTimeCard.findViewById(R.id.weekNumTimeNum);
        ImageView timePickerButton = feedingTimeCard.findViewById(R.id.timePickerButton);
        
        // Make the EditText non-editable
        timeInput.setFocusable(false);
        timeInput.setClickable(true);
        
        View.OnClickListener showTimePickerListener = v -> {
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Feeding Time")
                .build();
    
            picker.addOnPositiveButtonClickListener(dialog -> {
                // Format the time
                String period = picker.getHour() >= 12 ? "PM" : "AM";
                int hour = picker.getHour() % 12;
                if (hour == 0) hour = 12;
                String time = String.format(Locale.getDefault(), 
                    "%d:%02d %s", 
                    hour, 
                    picker.getMinute(), 
                    period);
                
                timeInput.setText(time);
            });
    
            picker.show(activity.getSupportFragmentManager(), "TIME_PICKER");
        };
    
        // Set click listeners for both the EditText and the clock icon
        timeInput.setOnClickListener(showTimePickerListener);
        if (timePickerButton != null) {
            timePickerButton.setOnClickListener(showTimePickerListener);
        }
    }
    
    /**
     * Sets up the delete button for a feeding time card
     */
    private void setupDeleteButton(View feedingTimeCard) {
        MaterialButton deleteButton = feedingTimeCard.findViewById(R.id.deleteFeedingTimeButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                // If this is the last feeding time, show warning
                if (dailySchedulesContainer.getChildCount() <= 1) {
                    new AlertDialog.Builder(activity)
                        .setTitle("Warning")
                        .setMessage("Deleting the last feeding time will delete the entire schedule. Continue?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dailySchedulesContainer.removeView(feedingTimeCard);
                            
                            // If we've removed all times and this was the last one, add a new empty one
                            if (dailySchedulesContainer.getChildCount() == 0) {
                                addNewFeedingTime();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    // Just remove this feeding time
                    dailySchedulesContainer.removeView(feedingTimeCard);
                }
            });
        } else {
            Log.e("DeleteButton", "Delete button not found in feeding time card");
        }
    }
} 