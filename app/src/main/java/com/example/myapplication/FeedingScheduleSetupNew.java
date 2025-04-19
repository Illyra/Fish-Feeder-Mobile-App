//package com.example.myapplication;
//
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.ImageButton;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.lifecycle.ViewModelProvider;
//
//import com.example.myapplication.database.FeedingSchedule;
//import com.example.myapplication.ui.schedule.FishSelectionListener;
//import com.example.myapplication.ui.schedule.FishSelectionManager;
//import com.example.myapplication.ui.schedule.ScheduleActionListener;
//import com.example.myapplication.ui.schedule.ScheduleDialogManager;
//import com.example.myapplication.ui.schedule.ScheduleDisplayManager;
//import com.example.myapplication.ui.schedule.ScheduleEditorManager;
//import com.example.myapplication.ui.schedule.ScheduleFeedingTimeManager;
//import com.example.myapplication.utils.SmsUtils;
//import com.example.myapplication.viewmodel.AquacultureViewModel;
//import com.google.android.material.button.MaterialButton;
//
//import java.util.List;
//
///**
// * Activity for setting up fish feeding schedules
// */
//public class FeedingScheduleSetupNew extends AppCompatActivity implements
//        ScheduleActionListener, FishSelectionListener {
//
//    private AquacultureViewModel viewModel;
//    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
//
//    // Manager instances
//    private ScheduleDisplayManager displayManager;
//    private FishSelectionManager fishManager;
//    private ScheduleEditorManager editorManager;
//    private ScheduleFeedingTimeManager feedingTimeManager;
//    private ScheduleDialogManager dialogManager;
//
//    // Current state
//    private long currentFishId = -1;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_feeding_schedule_setup);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Initialize ViewModel
//        viewModel = new ViewModelProvider(this).get(AquacultureViewModel.class);
//
//        // Get the selected fish from intent
//        String selectedFish = getIntent().getStringExtra("SELECTED_FISH");
//
//        // Initialize managers
//        feedingTimeManager = new ScheduleFeedingTimeManager(this);
//        dialogManager = new ScheduleDialogManager(this, viewModel);
//        displayManager = new ScheduleDisplayManager(this, viewModel);
//        fishManager = new FishSelectionManager(this, viewModel, selectedFish);
//        editorManager = new ScheduleEditorManager(this, viewModel, feedingTimeManager);
//
//        // Connect managers with listeners
//        displayManager.setListener(this);
//        fishManager.setListener(this);
//
//        // Set up buttons
//        setupButtons();
//
//        // Clean up any invalid schedules
//        cleanupConflictingSchedules();
//    }
//
//    /**
//     * Set up button click handlers
//     */
//    private void setupButtons() {
//        // Get reference to create schedule button
//        MaterialButton createScheduleButton = findViewById(R.id.createScheduleButton);
//        if (createScheduleButton != null) {
//            createScheduleButton.setEnabled(false);
//            createScheduleButton.setAlpha(0.5f);
//            createScheduleButton.setOnClickListener(v -> {
//                if (currentFishId > 0) {
//                    dialogManager.showDateRangePicker(currentFishId, (scheduleName, startDate, endDate) -> {
//                        editorManager.setScheduleParams(scheduleName, startDate, endDate, currentFishId);
//                        editorManager.showDailyScheduleEditor();
//                    });
//                } else {
//                    Toast.makeText(this, "Please select a fish first", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//
//        // Setup SMS button
//        Button sendSmsButton = findViewById(R.id.sendScheduleSmsButton);
//        if (sendSmsButton != null) {
//            sendSmsButton.setOnClickListener(v -> sendAllSchedulesViaSms());
//        }
//
//        // Setup close button
//        ImageButton closeButton = findViewById(R.id.closeDailyFeedingScheduleView);
//        if (closeButton != null) {
//            closeButton.setOnClickListener(v -> editorManager.hideDailyScheduleEditor());
//        }
//
//        // Setup save button
//        Button saveDailyScheduleButton = findViewById(R.id.saveDailyScheduleButton);
//        if (saveDailyScheduleButton != null) {
//            saveDailyScheduleButton.setOnClickListener(v ->
//                editorManager.validateAndSaveSchedule(fishManager.getSelectedFishItem()));
//        }
//
//        // Setup add feeding time button
//        Button addDailyFeedingTimeButton = findViewById(R.id.addDailyFeedingTimeButton);
//        if (addDailyFeedingTimeButton != null) {
//            addDailyFeedingTimeButton.setOnClickListener(v -> feedingTimeManager.addNewFeedingTime());
//        }
//    }
//
//    // ScheduleActionListener implementation
//    @Override
//    public void onScheduleEditRequested(List<FeedingSchedule> schedules) {
//        editorManager.editExistingSchedule(schedules);
//        editorManager.showDailyScheduleEditor();
//    }
//
//    @Override
//    public void onScheduleDeleteRequested(long fishId, long startDate, long endDate) {
//        dialogManager.showDeleteScheduleDialog(fishId, startDate, endDate, () -> {
//            // Refresh the schedule list after deletion
//            displayManager.loadExistingSchedules(currentFishId);
//        });
//    }
//
//    @Override
//    public void onScheduleSaved() {
//        // Refresh schedules after saving
//        displayManager.loadExistingSchedules(currentFishId);
//    }
//
//    // FishSelectionListener implementation
//    @Override
//    public void onFishSelected(long fishId, String fishName) {
//        currentFishId = fishId;
//        if (fishId != -1) {
//            displayManager.loadExistingSchedules(fishId);
//        }
//    }
//
//    /**
//     * Send all schedules for the selected fish via SMS
//     */
//    private void sendAllSchedulesViaSms() {
//        // Get the currently selected fish
//        FishSelectionManager.FishItem selectedFishItem = fishManager.getSelectedFishItem();
//        long fishId = selectedFishItem != null ? selectedFishItem.id : -1;
//
//        if (fishId == -1) {
//            Toast.makeText(this, "Please select a fish first", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Get the fish details including phone number
//        viewModel.getFishById(fishId).observe(this, fish -> {
//            if (fish != null) {
//                String phoneNumber = fish.getPhoneNumber();
//
//                if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
//                    Toast.makeText(this, "No phone number found for this fish. Please update fish details.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                // Get all schedules for this fish
//                viewModel.getAllSchedulesForFish(fishId).observe(this, schedules -> {
//                    if (schedules == null || schedules.isEmpty()) {
//                        Toast.makeText(this, "No schedules found for this fish", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    // Send SMS with all schedules
//                    SmsUtils.sendScheduleNotification(this, phoneNumber, fish.getName(), schedules);
//                });
//            }
//        });
//    }
//
//    /**
//     * Check and request SMS permissions
//     */
//    private void checkSmsPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(android.Manifest.permission.SEND_SMS)
//                != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(
//                    new String[]{android.Manifest.permission.SEND_SMS},
//                    SMS_PERMISSION_REQUEST_CODE
//                );
//            }
//        }
//    }
//
//    /**
//     * Remove invalid schedules from the database
//     */
//    private void cleanupConflictingSchedules() {
//        Log.d("Database", "Starting database cleanup");
//        viewModel.getAllSchedules().observe(this, schedules -> {
//            if (schedules != null) {
//                // Delete schedules with invalid fishId (-1)
//                for (FeedingSchedule schedule : schedules) {
//                    if (schedule.getFishId() == -1) {
//                        viewModel.deleteFeedingSchedule(schedule);
//                        Log.d("Database", "Deleted invalid schedule: " + schedule.getScheduleName());
//                    }
//                }
//            }
//        });
//    }
//
//    // Permission handling
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission granted, try sending SMS again
//                sendAllSchedulesViaSms();
//            } else {
//                Toast.makeText(this, "SMS permission denied. Cannot send schedule.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//}