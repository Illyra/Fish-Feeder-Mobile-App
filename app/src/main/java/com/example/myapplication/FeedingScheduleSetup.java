package com.example.myapplication;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.TreeMap;
import java.util.Date;
import java.text.ParseException;

import com.example.myapplication.database.FeedingSchedule;
import com.example.myapplication.database.Fish;
import com.example.myapplication.viewmodel.AquacultureViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.myapplication.utils.SmsUtils;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialDatePicker.Builder;
import androidx.core.util.Pair;
import com.example.myapplication.R;  // Import R from your app package
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import android.telephony.SmsManager;

public class FeedingScheduleSetup extends AppCompatActivity {
    private View dailyScheduleMainContainer;
    private LinearLayout dailySchedulesContainer;
    private Group dailySchedContainerGroup;
    private Group mainContainerContentGroup;
    private Button saveDailyScheduleButton;
    private Button addDailyFeedingTimeButton;
    private AquacultureViewModel viewModel;
    private Spinner fishSpinner;
    private long currentFishId = -1;
    private List<FishItem> fishList = new ArrayList<>();
    private String currentFishName = "";
    private Button confirmFishButton;
    private Observer<Fish> fishObserver;
    private String currentScheduleName = "";
    private Long currentStartDate = null;
    private Long currentEndDate = null;
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    private boolean isEditingExistingSchedule = false;
    private long lastSaveClickTime = 0; // Add this to track last save button click time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feeding_schedule_setup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AquacultureViewModel.class);

        // Clean up any existing duplicate schedules
        viewModel.cleanupAllDuplicateSchedules().observe(this, count -> {
            if (count > 0) {
                Log.d("FeedingScheduleSetup", "Cleaned up " + count + " duplicate schedules on startup");
            }
        });

        // Get the selected fish from intent
        String selectedFish = getIntent().getStringExtra("SELECTED_FISH");

        // Initialize views with correct ID
        fishSpinner = findViewById(R.id.fish_dropdown);  // Changed to match layout ID

        // Initialize other views
        dailyScheduleMainContainer = findViewById(R.id.dailyScheduleMainContainer);
        dailySchedulesContainer = findViewById(R.id.dailySchedulesContainer);
        dailySchedContainerGroup = findViewById(R.id.dailySchedContainerGroup);
        mainContainerContentGroup = findViewById(R.id.mainContainerContentGroup);
        saveDailyScheduleButton = findViewById(R.id.saveDailyScheduleButton);
        addDailyFeedingTimeButton = findViewById(R.id.addDailyFeedingTimeButton);
        confirmFishButton = findViewById(R.id.confirmFishButton);

        // Get reference to create schedule button and disable it initially
        MaterialButton createScheduleButton = findViewById(R.id.createScheduleButton);
        if (createScheduleButton != null) {
            createScheduleButton.setEnabled(false);
            createScheduleButton.setAlpha(0.5f);
            createScheduleButton.setOnClickListener(v -> showDateRangePicker());
        }

        // Setup SMS button
        Button sendSmsButton = findViewById(R.id.sendScheduleSmsButton);
        if (sendSmsButton != null) {
            sendSmsButton.setOnClickListener(v -> sendAllSchedulesViaSms());
        }

        // Setup fish spinner
        setupFishSpinner();

        // Initially hide daily schedule editor
        if (dailySchedContainerGroup != null) {
            dailySchedContainerGroup.setVisibility(View.GONE);
        }

        // Setup close button
        ImageButton closeButton = findViewById(R.id.closeDailyFeedingScheduleView);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> hideDailyScheduleEditor());
        }

        // Setup other buttons
        if (saveDailyScheduleButton != null) {
            saveDailyScheduleButton.setOnClickListener(v -> {
                // Add debounce mechanism to prevent rapid clicks
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSaveClickTime < 1000) {
                    // Less than 1 second since last click, ignore this click
                    Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
                    return;
                }
                lastSaveClickTime = currentTime;
                validateAndSaveSchedule();
            });
        }
        if (addDailyFeedingTimeButton != null) {
            addDailyFeedingTimeButton.setOnClickListener(v -> addNewFeedingTime());
        }

        // Call this in onCreate and when fish selection changes
        setupScheduleDisplay();

        // Clean up any invalid schedules
        cleanupConflictingSchedules();
    }

    private void setupFishSpinner() {
        Log.d("FishSpinner", "Starting spinner setup");

        // Add null check and logging
        if (fishSpinner == null) {
            Log.e("FishSpinner", "Spinner not found in layout. Make sure R.id.fish_dropdown exists in activity_feeding_schedule_setup.xml");
            return;
        }

        fishList = new ArrayList<>();
        fishList.add(new FishItem("Select Fish", -1));

        // Create simple adapter with custom styling
        ArrayAdapter<FishItem> adapter = new ArrayAdapter<FishItem>(
                this,
                android.R.layout.simple_spinner_item,
                fishList
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                text.setBackgroundColor(Color.parseColor("#F4FEFD"));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                text.setBackgroundColor(Color.parseColor("#F4FEFD"));
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fishSpinner.setAdapter(adapter);

        MaterialButton createScheduleButton = findViewById(R.id.createScheduleButton);
        createScheduleButton.setEnabled(false);

        // Get the selected fish from intent
        String selectedFish = getIntent().getStringExtra("SELECTED_FISH");

        // First, load fish data
        viewModel.getAllFish().observe(this, fishEntities -> {
            Log.d("FishSpinner", "Fish data received: " + (fishEntities != null ? fishEntities.size() : 0));
            if (fishEntities != null && !fishEntities.isEmpty()) {
                fishList.clear();
                fishList.add(new FishItem("Select Fish", -1));
                for (Fish fish : fishEntities) {
                    Log.d("FishSpinner", "Adding fish: " + fish.getName() + " with ID: " + fish.getId());
                    fishList.add(new FishItem(fish.getName(), fish.getId()));
                }
                adapter.notifyDataSetChanged();
                Log.d("FishSpinner", "Adapter updated with " + fishList.size() + " items");

                // Set the selected fish if passed from MainActivity
                if (selectedFish != null && !selectedFish.isEmpty()) {
                    for (int i = 0; i < fishList.size(); i++) {
                        if (fishList.get(i).name.equals(selectedFish)) {
                            fishSpinner.setSelection(i);
                            break;
                        }
                    }
                }

                // After fish data is loaded, setup spinner listener
                fishSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        FishItem selectedFish = (FishItem) parent.getItemAtPosition(position);
                        Log.d("FishSpinner", "Selected fish: " + selectedFish.name + " with ID: " + selectedFish.id);

                        LinearLayout schedulesContainer = findViewById(R.id.schedulesContainer);

                        if (selectedFish.name.equals("Select Fish")) {
                            // Clear and hide schedules for default selection
                            if (schedulesContainer != null) {
                                schedulesContainer.removeAllViews();
                            }
                            createScheduleButton.setEnabled(false);
                            createScheduleButton.setAlpha(0.5f);
                            return;
                        }

                        // Enable create button and show schedules for selected fish
                        createScheduleButton.setEnabled(true);
                        createScheduleButton.setAlpha(1.0f);

                        viewModel.getAllSchedulesForFish(selectedFish.id).observe(FeedingScheduleSetup.this, schedules -> {
                            if (schedulesContainer != null) {
                                schedulesContainer.removeAllViews();
                                if (schedules != null) {
                                    // Group schedules by name and date range
                                    Map<String, List<FeedingSchedule>> groupedSchedules = new HashMap<>();
                                    for (FeedingSchedule schedule : schedules) {
                                        String key = schedule.getScheduleName() + "_" + schedule.getStartDate() + "_" + schedule.getEndDate();
                                        if (!groupedSchedules.containsKey(key)) {
                                            groupedSchedules.put(key, new ArrayList<>());
                                        }
                                        groupedSchedules.get(key).add(schedule);
                                    }

                                    // Convert to list for sorting
                                    List<Map.Entry<String, List<FeedingSchedule>>> sortedGroups = new ArrayList<>(groupedSchedules.entrySet());

                                    // Sort by start date
                                    Collections.sort(sortedGroups, (entry1, entry2) -> {
                                        long date1 = entry1.getValue().get(0).getStartDate();
                                        long date2 = entry2.getValue().get(0).getStartDate();
                                        return Long.compare(date1, date2);
                                    });

                                    // Display each group in sorted order
                                    for (Map.Entry<String, List<FeedingSchedule>> entry : sortedGroups) {
                                        List<FeedingSchedule> group = entry.getValue();
                                        if (!group.isEmpty()) {
                                            addScheduleGroupToContainer(group, schedulesContainer);
                                        }
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        createScheduleButton.setEnabled(false);
                        createScheduleButton.setAlpha(0.5f);
                    }
                });
            }
        });

        // Debug query for all schedules
        viewModel.getAllSchedules().observe(this, allSchedules -> {
            Log.d("Database", "DEBUG: All schedules in database");
            if (allSchedules != null) {
                for (FeedingSchedule schedule : allSchedules) {
                    Log.d("Database", String.format("Schedule found: id=%d, fishId=%d, name=%s",
                            schedule.getId(), schedule.getFishId(), schedule.getScheduleName()));
                }
            }
        });
    }

    private void addScheduleGroupToContainer(List<FeedingSchedule> scheduleGroup, LinearLayout container) {
        if (scheduleGroup.isEmpty()) return;

        FeedingSchedule firstSchedule = scheduleGroup.get(0);
        String scheduleName = firstSchedule.getScheduleName();
        long startDate = firstSchedule.getStartDate();
        long endDate = firstSchedule.getEndDate();

        // Create the schedule card view
        View scheduleCard = getLayoutInflater().inflate(R.layout.schedule_group_item, container, false);

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
        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                editExistingSchedule(scheduleGroup);
            });
        }

        // Setup delete button
        Button deleteButton = scheduleCard.findViewById(R.id.deleteScheduleButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                deleteSchedule(startDate, endDate);
            });
        }

        // Add the card to the container
        container.addView(scheduleCard);
    }

    private void showDailySchedulesDialog(List<FeedingSchedule> scheduleGroup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.daily_schedules_dialog, null);
        LinearLayout dailySchedulesContainer = dialogView.findViewById(R.id.dailySchedulesContainer);
        MaterialButton addTimeButton = dialogView.findViewById(R.id.addTimeButton);
        MaterialButton saveButton = dialogView.findViewById(R.id.saveButton);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Clear any existing views first
        dailySchedulesContainer.removeAllViews();

        // Sort schedules by time
        Collections.sort(scheduleGroup, (s1, s2) -> {
            try {
                SimpleDateFormat parser = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Date time1 = parser.parse(s1.getFeedingTime());
                Date time2 = parser.parse(s2.getFeedingTime());
                return time1.compareTo(time2);
            } catch (ParseException e) {
                Log.e("TimeSort", "Error parsing time", e);
                return 0;
            }
        });

        // Add existing feeding times
        for (FeedingSchedule schedule : scheduleGroup) {
            View timeScheduleItem = getLayoutInflater().inflate(R.layout.daily_schedule_item, dailySchedulesContainer, false);
            TextView timeText = timeScheduleItem.findViewById(R.id.timeText);
            TextView quantityText = timeScheduleItem.findViewById(R.id.quantityText);
            MaterialButton deleteButton = timeScheduleItem.findViewById(R.id.deleteTimeButton);

            timeText.setText(schedule.getFeedingTime());
            quantityText.setText(String.format("%.1fg", schedule.getFeedQuantity()));
            quantityText.setTextColor(Color.parseColor("#FFFFFF"));

            deleteButton.setOnClickListener(v -> {
                dailySchedulesContainer.removeView(timeScheduleItem);
            });

            dailySchedulesContainer.addView(timeScheduleItem);
        }

        // Add new time button handler
        addTimeButton.setOnClickListener(v -> {
            showAddTimeDialog(dailySchedulesContainer);
        });

        AlertDialog dialog = builder.setView(dialogView).create();

        // Set up save button
        saveButton.setOnClickListener(v -> {
            saveDailySchedules(scheduleGroup.get(0), dailySchedulesContainer);
            dialog.dismiss();
        });

        // Set up cancel button
        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showAddTimeDialog(LinearLayout container) {
        View timePickerView = getLayoutInflater().inflate(R.layout.feeding_time_card, null);
        EditText timeInput = timePickerView.findViewById(R.id.weekNumTimeNum);
        TextInputEditText quantityInput = timePickerView.findViewById(R.id.feedQuantityInput);

        // Change card border color to white
        if (timePickerView instanceof MaterialCardView) {
            MaterialCardView cardView = (MaterialCardView) timePickerView;
            cardView.setStrokeColor(Color.WHITE);
            cardView.setStrokeWidth(0); // Remove border completely
        }

        // Setup time picker
        timeInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, selectedMinute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, selectedMinute);
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        timeInput.setText(sdf.format(calendar.getTime()));
                    }, hour, minute, false);
            timePickerDialog.show();
        });

        // Create the dialog with white background
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder
                .setTitle("Add Feeding Time")
                .setView(timePickerView)
                .setPositiveButton("Add", null)  // Set to null initially to prevent auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        // Set background color
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F5F5F5")));

        // Set title text color to black
        dialog.setOnShowListener(dialogInterface -> {
            // Set title color to black
            int titleId = getResources().getIdentifier("alertTitle", "id", "android");
            TextView titleView = dialog.findViewById(titleId);
            if (titleView != null) {
                titleView.setTextColor(Color.BLACK);
            }

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String time = timeInput.getText().toString();
                String quantityStr = quantityInput.getText().toString().trim();

                if (!TextUtils.isEmpty(time) && !TextUtils.isEmpty(quantityStr)) {
                    try {
                        float quantity = Float.parseFloat(quantityStr);
                        boolean isDuplicate = false;
                        for (int i = 0; i < container.getChildCount(); i++) {
                            View existingItem = container.getChildAt(i);
                            TextView existingTimeText = existingItem.findViewById(R.id.timeText);
                            if (existingTimeText != null && time.equals(existingTimeText.getText().toString())) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (isDuplicate) {
                            Toast.makeText(FeedingScheduleSetup.this, 
                                "A feeding time for " + time + " already exists", 
                                Toast.LENGTH_SHORT).show();
                            return;
                        }

                        View timeScheduleItem = getLayoutInflater().inflate(R.layout.daily_schedule_item, container, false);
                        TextView timeText = timeScheduleItem.findViewById(R.id.timeText);
                        TextView quantityText = timeScheduleItem.findViewById(R.id.quantityText);
                        MaterialButton deleteButton = timeScheduleItem.findViewById(R.id.deleteTimeButton);

                        timeText.setText(time);
                        quantityText.setText(String.format("%.1fg", quantity));
                        quantityText.setTextColor(Color.parseColor("#FFFFFF"));

                        deleteButton.setOnClickListener(v2 -> {
                            container.removeView(timeScheduleItem);
                        });

                        container.addView(timeScheduleItem);
                        dialog.dismiss();
                    } catch (NumberFormatException e) {
                        Toast.makeText(FeedingScheduleSetup.this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FeedingScheduleSetup.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void saveDailySchedules(FeedingSchedule template, LinearLayout container) {
        List<FeedingSchedule> updatedSchedules = new ArrayList<>();

        for (int i = 0; i < container.getChildCount(); i++) {
            View timeItem = container.getChildAt(i);
            TextView timeText = timeItem.findViewById(R.id.timeText);
            TextView quantityText = timeItem.findViewById(R.id.quantityText);

            String time = timeText.getText().toString();
            String quantityStr = quantityText.getText().toString()
                    .replace("g", "")
                    .trim();

            if (TextUtils.isEmpty(quantityStr)) {
                continue;
            }

            try {
                float quantity = Float.parseFloat(quantityStr);
                FeedingSchedule schedule = new FeedingSchedule(
                        template.getFishId(),
                        template.getScheduleName(),
                        template.getStartDate(),
                        template.getEndDate(),
                        time,
                        quantity
                );
                updatedSchedules.add(schedule);
            } catch (NumberFormatException e) {
                Log.e("SaveSchedule", "Error parsing quantity: '" + quantityStr + "'", e);
                return; // Just return without showing error toast
            }
        }

        if (!updatedSchedules.isEmpty()) {
            // Sort before saving
            Collections.sort(updatedSchedules, (s1, s2) -> {
                try {
                    SimpleDateFormat parser = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    Date time1 = parser.parse(s1.getFeedingTime());
                    Date time2 = parser.parse(s2.getFeedingTime());
                    return time1.compareTo(time2);
                } catch (ParseException e) {
                    Log.e("TimeSort", "Error parsing time", e);
                    return 0;
                }
            });

            viewModel.updateSchedulesForDateRange(
                    template.getFishId(),
                    template.getStartDate(),
                    template.getEndDate(),
                    updatedSchedules
            );
            
            // Get fish info to send SMS notification
            viewModel.getFishById(template.getFishId()).observe(this, fish -> {
                if (fish != null && !TextUtils.isEmpty(fish.getPhoneNumber())) {
                    // Send SMS notification for the updated schedule
                    SmsUtils.sendScheduleNotification(this, fish.getPhoneNumber(), fish.getName(), updatedSchedules);
                    Toast.makeText(this, "SMS notification sent", Toast.LENGTH_SHORT).show();
                }
            });
            
            Toast.makeText(this, "Schedule updated successfully", Toast.LENGTH_SHORT).show();
            hideDailyScheduleEditor();
            loadExistingSchedules();
        }
    }

    private void updateScheduleDisplay(Map<String, List<FeedingSchedule>> schedulesByName) {
        dailySchedulesContainer.removeAllViews();

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
                addScheduleGroupToContainer(scheduleGroup, dailySchedulesContainer);
            }
        }
    }

    private void resetFishSelection() {
        currentFishName = "";
        currentFishId = -1;
        dailySchedulesContainer.removeAllViews();
        Toast.makeText(this, "Please select a fish type", Toast.LENGTH_SHORT).show();
    }

    // Add this method to collect all feeding schedules from the UI
    private List<FeedingSchedule> collectFeedingSchedules() {
        List<FeedingSchedule> schedules = new ArrayList<>();

        for (int i = 0; i < dailySchedulesContainer.getChildCount(); i++) {
            View card = dailySchedulesContainer.getChildAt(i);

            EditText timeInput = card.findViewById(R.id.weekNumTimeNum);
            TextInputEditText quantityInput = card.findViewById(R.id.feedQuantityInput);

            String timeText = timeInput.getText().toString();
            float quantity = Float.parseFloat(quantityInput.getText().toString());

            FeedingSchedule schedule = new FeedingSchedule(
                    currentFishId,
                    currentScheduleName,
                    currentStartDate,
                    currentEndDate,
                    timeText,
                    quantity
            );

            schedules.add(schedule);
        }

        return schedules;
    }

    // Also add cleanup in onDestroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up observers
        if (fishObserver != null) {
            viewModel.getFishByName(currentFishName).removeObserver(fishObserver);
        }
    }

    // Add permission result handling
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try sending SMS again
                sendAllSchedulesViaSms();
            } else {
                Toast.makeText(this, "SMS permission denied. Cannot send schedule.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Add this method to check and request SMS permissions
    private void checkSmsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    private void showDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Schedule Date Range");
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Long startDate = selection.first;
            Long endDate = selection.second;

            FishItem selectedFish = (FishItem) fishSpinner.getSelectedItem();

            // Check for overlapping schedules before proceeding
            viewModel.getAllSchedulesForFish(selectedFish.id).observe(this, existingSchedules -> {
                if (existingSchedules != null && !existingSchedules.isEmpty()) {
                    boolean hasOverlap = false;
                    for (FeedingSchedule existing : existingSchedules) {
                        Log.d("DatePicker", String.format("Checking overlap: new(%d-%d) vs existing(%d-%d)",
                                startDate, endDate, existing.getStartDate(), existing.getEndDate()));

                        // Check if date ranges overlap
                        if (!(endDate < existing.getStartDate() || startDate > existing.getEndDate())) {
                            hasOverlap = true;
                            break;
                        }
                    }

                    if (hasOverlap) {
                        Toast.makeText(this, "Selected date range overlaps with existing schedule", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // No overlap found, proceed with schedule name dialog
                showScheduleNameDialog(startDate, endDate);
            });
        });

        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showScheduleNameDialog(Long startDate, Long endDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_schedule_name, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.scheduleNameInput);

        builder.setView(dialogView)
                .setTitle("Name Your Schedule")
                .setPositiveButton("Continue", (dialog, which) -> {
                    String scheduleName = nameInput.getText().toString();
                    if (!TextUtils.isEmpty(scheduleName)) {
                        currentScheduleName = scheduleName;
                        currentStartDate = startDate;
                        currentEndDate = endDate;
                        showDailyScheduleEditor();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDailyScheduleEditor() {
        // Clear any previous schedule name
        EditText scheduleNameInput = findViewById(R.id.scheduleNameInput);
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
            addNewFeedingTime();
        }
    }

    private void hideDailyScheduleEditor() {
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
    }

    private void addNewFeedingTime() {
        View feedingTimeCard = getLayoutInflater().inflate(R.layout.feeding_time_card, dailySchedulesContainer, false);

        // Setup the time picker and delete functionality
        setupTimePicker(feedingTimeCard);
        setupDeleteButton(feedingTimeCard);

        dailySchedulesContainer.addView(feedingTimeCard);
    }

    private void validateAndSaveSchedule() {
        FishItem selectedFish = (FishItem) fishSpinner.getSelectedItem();

        if (selectedFish == null || selectedFish.name.equals("Select Fish")) {
            Toast.makeText(this, "Please select a fish type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(currentScheduleName)) {
            Toast.makeText(this, "Please enter a schedule name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentStartDate == null || currentEndDate == null) {
            Toast.makeText(this, "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate remaining fields and collect schedules
        if (!validateRemainingFields()) {
            return;
        }
        
        List<FeedingSchedule> schedules = collectFeedingSchedules();
        if (schedules.isEmpty()) {
            Toast.makeText(this, "Please add at least one feeding time", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Remove any duplicate times that might have been added
        Map<String, FeedingSchedule> uniqueSchedules = new HashMap<>();
        for (FeedingSchedule schedule : schedules) {
            uniqueSchedules.put(schedule.getFeedingTime(), schedule);
        }
        
        // Convert back to list
        schedules = new ArrayList<>(uniqueSchedules.values());
        
        // Sort schedules by time
        Collections.sort(schedules, (s1, s2) -> {
            try {
                SimpleDateFormat parser = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Date time1 = parser.parse(s1.getFeedingTime());
                Date time2 = parser.parse(s2.getFeedingTime());
                return time1.compareTo(time2);
            } catch (ParseException e) {
                Log.e("TimeSort", "Error parsing time", e);
                return s1.getFeedingTime().compareTo(s2.getFeedingTime());
            }
        });
        
        // If we're editing an existing schedule, use updateSchedulesForDateRange
        if (isEditingExistingSchedule) {
            // Set fish ID for all schedules
            for (FeedingSchedule schedule : schedules) {
                schedule.setFishId(selectedFish.id);
            }
            
            // Update existing schedules (deletes old ones and adds new ones)
            viewModel.updateSchedulesForDateRange(
                selectedFish.id,
                currentStartDate,
                currentEndDate,
                schedules
            );
            
            // Get fish info to check for phone number
            List<FeedingSchedule> finalSchedules = schedules;
            viewModel.getFishById(selectedFish.id).observe(this, fish -> {
                if (fish != null && !TextUtils.isEmpty(fish.getPhoneNumber())) {
                    // Send SMS notification
                    SmsUtils.sendScheduleNotification(this, fish.getPhoneNumber(), fish.getName(), finalSchedules);
                    Toast.makeText(this, "SMS notification sent", Toast.LENGTH_SHORT).show();
                }
                
                hideDailyScheduleEditor();
                Toast.makeText(this, "Schedule updated successfully", Toast.LENGTH_SHORT).show();
                loadExistingSchedules();
            });
        } else {
            // For new schedules, use the regular save method
            saveSchedule(selectedFish);
        }
    }

    private boolean validateRemainingFields() {
        // Check if there are any feeding times
        if (dailySchedulesContainer.getChildCount() == 0) {
            Toast.makeText(this, "Please add at least one feeding time", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Please select time for all entries", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (TextUtils.isEmpty(quantityText)) {
                Toast.makeText(this, "Please enter feed quantity for all entries", Toast.LENGTH_SHORT).show();
                return false;
            }

            try {
                float quantity = Float.parseFloat(quantityText);
                if (quantity <= 0) {
                    Toast.makeText(this, "Feed quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid feed quantity", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void saveSchedule(FishItem selectedFish) {
        List<FeedingSchedule> schedules = collectFeedingSchedules();

        // Set fish ID for all schedules
        for (FeedingSchedule schedule : schedules) {
            schedule.setFishId(selectedFish.id);
            Log.d("SaveSchedule", "Creating schedule with fishId: " + schedule.getFishId());
        }
        
        // Use updateSchedulesForDateRange instead of saveFeedingSchedules
        // This will first delete any existing schedules with this date range and name 
        // before inserting new ones, preventing duplication
        viewModel.updateSchedulesForDateRange(
            selectedFish.id,
            currentStartDate,
            currentEndDate,
            schedules
        );

        // Get fish info to check for phone number
        viewModel.getFishById(selectedFish.id).observe(this, new Observer<Fish>() {
            @Override
            public void onChanged(Fish fish) {
                if (fish != null && !TextUtils.isEmpty(fish.getPhoneNumber())) {
                    // Send SMS notification using the utility method
                    SmsUtils.sendScheduleNotification(FeedingScheduleSetup.this, fish.getPhoneNumber(), fish.getName(), schedules);
                    Toast.makeText(FeedingScheduleSetup.this, "SMS notification sent", Toast.LENGTH_SHORT).show();
                }

                hideDailyScheduleEditor();
                Toast.makeText(FeedingScheduleSetup.this, "Schedule saved successfully!", Toast.LENGTH_SHORT).show();
                
                // Important: Remove the observer to prevent multiple callbacks
                viewModel.getFishById(selectedFish.id).removeObserver(this);
                
                // Refresh the schedule list
                loadExistingSchedules();
            }
        });
    }

    private void sendAllSchedulesViaSms() {
        // Get the currently selected fish from the spinner
        if (fishSpinner == null || fishSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a fish first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the fish ID from the selected item
        FishItem selectedFishItem = (FishItem) fishSpinner.getSelectedItem();
        long fishId = selectedFishItem.id;

        if (fishId == -1) {
            Toast.makeText(this, "Invalid fish selection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the fish details including phone number
        viewModel.getFishById(fishId).observe(this, fish -> {
            if (fish != null) {
                String phoneNumber = fish.getPhoneNumber();

                if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                    Toast.makeText(this, "No phone number found for this fish. Please update fish details.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get all schedules for this fish
                viewModel.getAllSchedulesForFish(fishId).observe(this, schedules -> {
                    if (schedules == null || schedules.isEmpty()) {
                        Toast.makeText(this, "No schedules found for this fish", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Confirm before sending SMS
                    new AlertDialog.Builder(this)
                            .setTitle("Send All Schedules via SMS")
                            .setMessage("Send all feeding schedules for " + fish.getName() + " to " + phoneNumber + "?")
                            .setPositiveButton("Send", (dialog, which) -> {
                                // Format the message for Arduino
                                SmsUtils.sendScheduleNotification(this, phoneNumber, fish.getName(), schedules);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
        });
    }

    // Add this method to handle time picker
    private void showTimePicker(View feedingTimeCard) {
        EditText timeInput = feedingTimeCard.findViewById(R.id.weekNumTimeNum);
        ImageView timePickerButton = feedingTimeCard.findViewById(R.id.timePickerButton);

        // Make the EditText non-editable
        timeInput.setFocusable(false);
        timeInput.setClickable(true);

        // Set click listeners for both the EditText and the clock icon
        View.OnClickListener showTimePickerListener = v -> showTimePicker(timeInput);
        timeInput.setOnClickListener(showTimePickerListener);
        timePickerButton.setOnClickListener(showTimePickerListener);
    }

    // Add this inner class at the bottom of FeedingScheduleSetup.java
    private static class FishItem {
        final String name;
        final long id;

        FishItem(String name, long id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void loadExistingSchedules() {
        if (currentFishId == -1) return;

        Log.d("FeedingScheduleSetup", "Loading schedules for fish ID: " + currentFishId);

        viewModel.getAllSchedulesForFish(currentFishId).observe(this, schedules -> {
            if (schedules != null) {
                Log.d("FeedingScheduleSetup", "Loaded schedules: " + schedules.size());

                // Group schedules by name and date range (not just name)
                Map<String, List<FeedingSchedule>> groupedSchedules = new HashMap<>();

                // First deduplicate schedules by time within each group
                Map<String, Map<String, FeedingSchedule>> deduplicatedGroups = new HashMap<>();

                for (FeedingSchedule schedule : schedules) {
                    // Create unique key using name AND date range
                    String key = String.format("%s_%d_%d",
                            schedule.getScheduleName(),
                            schedule.getStartDate(),
                            schedule.getEndDate());
                    
                    // Initialize inner map if needed
                    if (!deduplicatedGroups.containsKey(key)) {
                        deduplicatedGroups.put(key, new HashMap<>());
                    }
                    
                    // Use feeding time as key to ensure uniqueness
                    deduplicatedGroups.get(key).put(schedule.getFeedingTime(), schedule);
                }
                
                // Convert deduplicated maps to lists
                for (Map.Entry<String, Map<String, FeedingSchedule>> entry : deduplicatedGroups.entrySet()) {
                    groupedSchedules.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
                }

                // Clear existing views
                LinearLayout schedulesContainer = findViewById(R.id.schedulesContainer);
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
                    
                    // Sort feeding times within each group
                    for (List<FeedingSchedule> group : scheduleGroups) {
                        Collections.sort(group, (s1, s2) -> {
                            try {
                                SimpleDateFormat parser = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                Date time1 = parser.parse(s1.getFeedingTime());
                                Date time2 = parser.parse(s2.getFeedingTime());
                                return time1.compareTo(time2);
                            } catch (ParseException e) {
                                Log.e("TimeSort", "Error parsing time", e);
                                return s1.getFeedingTime().compareTo(s2.getFeedingTime());
                            }
                        });
                    }

                    // Add schedule groups to the container in sorted order
                    for (List<FeedingSchedule> group : scheduleGroups) {
                        if (!group.isEmpty()) {
                            addScheduleGroupToContainer(group, schedulesContainer);
                        }
                    }
                } else {
                    Log.e("FeedingScheduleSetup", "schedulesContainer is null");
                }
            } else {
                Log.e("FeedingScheduleSetup", "Loaded schedules is null");
            }
        });
    }

    private void editExistingSchedule(List<FeedingSchedule> schedules) {
        if (schedules.isEmpty()) return;

        FeedingSchedule firstSchedule = schedules.get(0);
        currentScheduleName = firstSchedule.getScheduleName();
        currentStartDate = firstSchedule.getStartDate();
        currentEndDate = firstSchedule.getEndDate();

        // Show the daily schedule editor
        showDailyScheduleEditor();

        // Clear existing feeding times
        dailySchedulesContainer.removeAllViews();

        // Add each feeding time
        for (FeedingSchedule schedule : schedules) {
            addExistingFeedingTime(schedule.getFeedingTime(), schedule.getFeedQuantity());
        }
    }

    private void addExistingFeedingTime(String time, float quantity) {
        View feedingTimeCard = getLayoutInflater().inflate(R.layout.feeding_time_card, dailySchedulesContainer, false);

        EditText timeInput = feedingTimeCard.findViewById(R.id.weekNumTimeNum);
        TextInputEditText quantityInput = feedingTimeCard.findViewById(R.id.feedQuantityInput);

        timeInput.setText(time);
        quantityInput.setText(String.valueOf(quantity));

        // Setup the time picker and delete functionality
        setupTimePicker(feedingTimeCard);
        setupDeleteButton(feedingTimeCard);

        dailySchedulesContainer.addView(feedingTimeCard);
    }

    private void deleteSchedule(long startDate, long endDate) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete this schedule?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteSchedulesForDateRange(currentFishId, startDate, endDate)
                            .observe(this, success -> {
                                if (success) {
                                    Toast.makeText(this, "Schedule deleted", Toast.LENGTH_SHORT).show();
                                    // Make sure we're showing the main view, not the editor
                                    hideDailyScheduleEditor();
                                    // Refresh the schedule list
                                    loadExistingSchedules();
                                } else {
                                    Toast.makeText(this, "Error deleting schedule", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Call this in onCreate and when fish selection changes
    private void setupScheduleDisplay() {
        fishSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                FishItem selectedFish = (FishItem) parent.getItemAtPosition(position);
                currentFishId = selectedFish.id;
                if (currentFishId != -1) {
                    loadExistingSchedules();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentFishId = -1;
            }
        });
    }

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

            picker.show(getSupportFragmentManager(), "TIME_PICKER");
        };

        // Set click listeners for both the EditText and the clock icon
        timeInput.setOnClickListener(showTimePickerListener);
        if (timePickerButton != null) {
            timePickerButton.setOnClickListener(showTimePickerListener);
        }
    }

    private void setupDeleteButton(View feedingTimeCard) {
        MaterialButton deleteButton = feedingTimeCard.findViewById(R.id.deleteFeedingTimeButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                // If this is the last feeding time, show warning
                if (dailySchedulesContainer.getChildCount() <= 1) {
                    new AlertDialog.Builder(this)
                            .setTitle("Warning")
                            .setMessage("Deleting the last feeding time will delete the entire schedule. Continue?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                dailySchedulesContainer.removeView(feedingTimeCard);
                                hideDailyScheduleEditor();
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

    private void cleanupConflictingSchedules() {
        Log.d("Database", "Starting database cleanup");
        viewModel.getAllSchedules().observe(this, schedules -> {
            if (schedules != null) {
                // Delete schedules with invalid fishId (-1)
                for (FeedingSchedule schedule : schedules) {
                    if (schedule.getFishId() == -1) {
                        viewModel.deleteFeedingSchedule(schedule);
                        Log.d("Database", "Deleted invalid schedule: " + schedule.getScheduleName());
                    }
                }
            }
        });
    }

    private void editDailySchedule(FeedingSchedule schedule) {
        // Create a dialog for editing daily schedule
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.feeding_time_card, null);

        // Initialize views
        EditText timeInput = dialogView.findViewById(R.id.weekNumTimeNum);
        TextInputEditText quantityInput = dialogView.findViewById(R.id.feedQuantityInput);

        // Set existing values
        timeInput.setText(schedule.getFeedingTime());
        quantityInput.setText(String.valueOf(schedule.getFeedQuantity()));

        // Setup time picker for the dialog
        setupTimePicker(dialogView);

        builder.setTitle("Edit Feeding Schedule")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTime = timeInput.getText().toString();
                    String newQuantity = quantityInput.getText().toString();

                    if (TextUtils.isEmpty(newTime)) {
                        Toast.makeText(this, "Please select time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(newQuantity)) {
                        Toast.makeText(this, "Please enter feed quantity", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        float quantity = Float.parseFloat(newQuantity);
                        if (quantity <= 0) {
                            Toast.makeText(this, "Feed quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Update schedule with new values
                        schedule.setFeedingTime(newTime);
                        schedule.setFeedQuantity(quantity);

                        // Save to database
                        viewModel.updateFeedingSchedule(schedule);

                        // Refresh the schedule display
                        //refreshScheduleDisplay();

                        Toast.makeText(this, "Schedule updated successfully", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter valid feed quantity", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void refreshScheduleDisplay() {
        // Implementation of refreshScheduleDisplay method
    }
}