package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.database.FeedingSchedule;
import com.example.myapplication.database.Fish;
import com.example.myapplication.viewmodel.AquacultureViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.text.ParseException;

public class MainActivity extends AppCompatActivity {

    private Spinner fishSelectionSpinner;
    private TextView fishCountOutput;
    private TextView aliveCountText;
    private TextView deadCountText;
    private AquacultureViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AquacultureViewModel.class);

        // Initialize views
        fishSelectionSpinner = findViewById(R.id.fishTypeSpinner);
        fishCountOutput = findViewById(R.id.fishCountOutput);
        aliveCountText = findViewById(R.id.textView14);
        deadCountText = findViewById(R.id.totalFeed);

        // Setup navigation buttons
        setupNavigationButtons();

        // Load fish data from database
        loadFishData();
        updateTotalFeedAmount();

        // Update feed amount when fish is selected
        Spinner fishTypeSpinner = findViewById(R.id.fishTypeSpinner);
        fishTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTotalFeedAmount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Reset the total feed amount display
                TextView totalFeedAmountView = findViewById(R.id.totalFeedAmount);
                totalFeedAmountView.setText("Total Feed Amount: 0g");
            }
        });
    }

    private void setupNavigationButtons() {
        // Dashboard button (refresh current view)
        View dashboardButton = findViewById(R.id.dashboardButton);
        dashboardButton.setOnClickListener(view -> {
            refreshDashboardData();
        });

        // Feed Now button
        Button feedNowButton = findViewById(R.id.buttonFeedNowShowDialog);
        if (feedNowButton != null) {
            feedNowButton.setOnClickListener(view -> {
                showFeedNowDialog();
            });
        }

        // Cleanup button
        View cleanupButton = findViewById(R.id.cleanupDatabaseButton);
        if (cleanupButton != null) {
            cleanupButton.setOnClickListener(view -> {
                cleanupFishDatabase();
            });
        }

        // Fish Templates button
        Button fishTemplatesButton = findViewById(R.id.fishTemplatesButton);
        if (fishTemplatesButton != null) {
            fishTemplatesButton.setOnClickListener(view -> {
                // Navigate to FishTemplatesActivity
                Intent intent = new Intent(MainActivity.this, FishTemplatesActivity.class);
                startActivity(intent);
            });
        }

        // Add Fish Type button - check for both the image and its parent layout
        View addFishButtonLayout = findViewById(R.id.addFishButtonLayout);
        if (addFishButtonLayout != null) {
            addFishButtonLayout.setOnClickListener(view -> {
                showAddFishTypeDialog();
            });
        }
        
        // Also keep the original ImageView click handler as a fallback
        ImageView addFishTypeButton = findViewById(R.id.addFishTypeButton);
        if (addFishTypeButton != null) {
            addFishTypeButton.setOnClickListener(view -> {
                showAddFishTypeDialog();
            });
        }

        // Edit Fish Count button
        Button editFishCountButton = findViewById(R.id.editFishCount);
        if (editFishCountButton != null) {
            editFishCountButton.setOnClickListener(view -> {
                // Navigate to FeedingSetup activity
                Intent intent = new Intent(MainActivity.this, FeedingSetupActivity.class);
                
                // Pass the selected fish name as an extra
                if (fishSelectionSpinner != null && fishSelectionSpinner.getSelectedItem() != null) {
                    String selectedFish = fishSelectionSpinner.getSelectedItem().toString();
                    intent.putExtra("SELECTED_FISH", selectedFish);
                    Log.d("MainActivity", "Sending selected fish: " + selectedFish);
                }
                
                startActivity(intent);
            });
        }

        // Schedule Edit button
        Button scheduleEditButton = findViewById(R.id.fishScheduleEditButton);
        if (scheduleEditButton != null) {
            scheduleEditButton.setOnClickListener(view -> {
                // Navigate to FeedingScheduleSetup activity
                Intent intent = new Intent(MainActivity.this, FeedingScheduleSetup.class);
                
                // Pass the selected fish from fishTypeSpinner
                Spinner fishTypeSpinner = findViewById(R.id.fishTypeSpinner);
                if (fishTypeSpinner != null && fishTypeSpinner.getSelectedItem() != null) {
                    String selectedFish = fishTypeSpinner.getSelectedItem().toString();
                    intent.putExtra("SELECTED_FISH", selectedFish);
                    Log.d("MainActivity", "Navigating to schedule with fish: " + selectedFish);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Please select a fish first", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadFishData() {
        viewModel.getAllFish().observe(this, fishList -> {
            if (fishList != null && !fishList.isEmpty()) {
                // Use a Set to eliminate duplicate fish names
                List<String> uniqueFishNames = new ArrayList<>();
                List<Long> fishIds = new ArrayList<>();
                
                // Track fish IDs with their names to get the most recent entry of each fish type
                java.util.Map<String, Fish> latestFishByName = new java.util.HashMap<>();
                
                // Keep only the latest entry for each fish name
                for (Fish fish : fishList) {
                    // If we haven't seen this fish name yet, or if this fish is more recent than the one we have
                    if (!latestFishByName.containsKey(fish.getName()) || 
                        (fish.getLastUpdated() != null && latestFishByName.get(fish.getName()).getLastUpdated() != null &&
                         fish.getLastUpdated().after(latestFishByName.get(fish.getName()).getLastUpdated()))) {
                        latestFishByName.put(fish.getName(), fish);
                    }
                }
                
                // Convert the map values to a list
                for (Fish fish : latestFishByName.values()) {
                    uniqueFishNames.add(fish.getName());
                    fishIds.add(fish.getId());
                }
                
                // Create a custom adapter that stores both name and ID
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        this, android.R.layout.simple_spinner_item, uniqueFishNames) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text = (TextView) view.findViewById(android.R.id.text1);
                        text.setTextColor(Color.BLACK);
                        return view;
                    }
                    
                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView text = (TextView) view.findViewById(android.R.id.text1);
                        text.setTextColor(Color.BLACK);
                        return view;
                    }
                };
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                fishSelectionSpinner.setAdapter(adapter);
                
                // Set listener for selection changes
                fishSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedFishName = parent.getItemAtPosition(position).toString();
                        updateDashboardWithFishData(selectedFishName);
                        loadFeedingScheduleForFish(latestFishByName.get(selectedFishName).getId());
                        updateTotalFeedAmount();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
                
                // Update dashboard with first fish if available
                if (!uniqueFishNames.isEmpty()) {
                    String firstName = uniqueFishNames.get(0);
                    updateDashboardWithFishData(firstName);
                    loadFeedingScheduleForFish(latestFishByName.get(firstName).getId());
                }
            } else {
                // Database is empty - show a message or empty state
                Toast.makeText(this, "No fish data available", Toast.LENGTH_SHORT).show();
                
                // Set up an empty adapter
                List<String> emptyList = new ArrayList<>();
                emptyList.add("No fish available");
                ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, emptyList);
                emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                fishSelectionSpinner.setAdapter(emptyAdapter);
                
                // Clear dashboard displays
                clearDashboard();
            }
        });
    }

    private void updateDashboardWithFishData(String fishName) {
        viewModel.getFishByName(fishName).observe(this, fish -> {
            if (fish != null) {
                fishCountOutput.setText(String.valueOf(fish.getTotalCount()));
                aliveCountText.setText("Alive: " + fish.getAliveCount());
                deadCountText.setText("Dead: " + fish.getDeadCount());
            }
        });
    }

    private void loadFeedingScheduleForFish(long fishId) {
        LinearLayout scheduleContainer = findViewById(R.id.schedulesContainer);
        TextView scheduleTitle = findViewById(R.id.editTextText2);
        
        if (scheduleContainer != null) {
            scheduleContainer.setVisibility(View.VISIBLE);
            
            viewModel.getAllSchedulesForFish(fishId).observe(this, schedules -> {
                if (schedules != null && !schedules.isEmpty()) {
                    // Group schedules by name and date range
                    Map<String, List<FeedingSchedule>> groupedSchedules = new HashMap<>();
                    
                    for (FeedingSchedule schedule : schedules) {
                        // Create a unique key using name, start date, and end date
                        String key = String.format("%s_%d_%d", 
                            schedule.getScheduleName(),
                            schedule.getStartDate(),
                            schedule.getEndDate());
                        
                        if (!groupedSchedules.containsKey(key)) {
                            groupedSchedules.put(key, new ArrayList<>());
                        }
                        groupedSchedules.get(key).add(schedule);
                    }
                    
                    // Create a list for sorting
                    List<Map.Entry<String, List<FeedingSchedule>>> sortedGroups = 
                        new ArrayList<>(groupedSchedules.entrySet());
                    
                    // Sort by start date (chronological order)
                    Collections.sort(sortedGroups, (entry1, entry2) -> {
                        long date1 = entry1.getValue().get(0).getStartDate();
                        long date2 = entry2.getValue().get(0).getStartDate();
                        return Long.compare(date1, date2); // Ascending order (oldest first)
                    });
                    
                    LinearLayout scheduleList = findViewById(R.id.schedulesContainer);
                    
                    if (scheduleList != null) {
                        scheduleList.removeAllViews();
                        
                        // Display schedules in sorted order
                        for (Map.Entry<String, List<FeedingSchedule>> entry : sortedGroups) {
                            List<FeedingSchedule> scheduleGroup = entry.getValue();
                            FeedingSchedule firstSchedule = scheduleGroup.get(0);
                            String scheduleName = firstSchedule.getScheduleName();
                            
                            // Create schedule header
                            TextView scheduleHeader = new TextView(this);
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            
                            String dateRange = sdf.format(new Date(firstSchedule.getStartDate())) + 
                                             " - " + 
                                             sdf.format(new Date(firstSchedule.getEndDate()));
                            
                            scheduleHeader.setText(scheduleName + "\n" + dateRange);
                            scheduleHeader.setTextSize(16);
                            scheduleHeader.setPadding(0, 16, 0, 8);
                            scheduleHeader.setTypeface(null, Typeface.BOLD);
                            scheduleHeader.setTextColor(Color.WHITE);
                            scheduleList.addView(scheduleHeader);
                            
                            // Deduplicate feeding times using a Map with time as key
                            Map<String, FeedingSchedule> uniqueSchedules = new HashMap<>();
                            for (FeedingSchedule schedule : scheduleGroup) {
                                uniqueSchedules.put(schedule.getFeedingTime(), schedule);
                            }
                            
                            // Convert back to list for sorting
                            List<FeedingSchedule> deduplicatedSchedules = new ArrayList<>(uniqueSchedules.values());
                            
                            // Sort feeding times using proper time comparison
                            Collections.sort(deduplicatedSchedules, (s1, s2) -> {
                                try {
                                    SimpleDateFormat parser = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                    Date time1 = parser.parse(s1.getFeedingTime());
                                    Date time2 = parser.parse(s2.getFeedingTime());
                                    return time1.compareTo(time2);
                                } catch (ParseException e) {
                                    Log.e("TimeSort", "Error parsing time", e);
                                    // Fallback to string comparison if parsing fails
                                    return s1.getFeedingTime().compareTo(s2.getFeedingTime());
                                }
                            });
                            
                            // Add each feeding time (now deduplicated and properly sorted)
                            for (FeedingSchedule schedule : deduplicatedSchedules) {
                                TextView scheduleItem = new TextView(this);
                                scheduleItem.setText("• " + schedule.getFeedingTime() + 
                                    " - " + schedule.getFeedQuantity() + "g");
                                scheduleItem.setPadding(16, 4, 0, 4);
                                scheduleItem.setTextColor(Color.WHITE);
                                scheduleList.addView(scheduleItem);
                            }
                        }
                        
                        if (scheduleTitle != null) {
                            scheduleTitle.setText("Feeding Schedules (" + sortedGroups.size() + " schedules)");
                        }
                    }
                } else {
                    // No schedules found
                    LinearLayout scheduleList = findViewById(R.id.schedulesContainer);
                    if (scheduleList != null) {
                        scheduleList.removeAllViews();
                        TextView noScheduleText = new TextView(this);
                        noScheduleText.setText("No feeding schedules found");
                        noScheduleText.setPadding(16, 16, 16, 16);
                        noScheduleText.setTextColor(Color.WHITE);
                        scheduleList.addView(noScheduleText);
                    }
                    
                    if (scheduleTitle != null) {
                        scheduleTitle.setText("Feeding Schedules");
                    }
                }
            });
        }
    }

    private void refreshDashboardData() {
        if (fishSelectionSpinner.getSelectedItem() != null) {
            String selectedFish = fishSelectionSpinner.getSelectedItem().toString();
            updateDashboardWithFishData(selectedFish);
            
            // Also refresh the feeding schedule
            viewModel.getFishByName(selectedFish).observe(this, fish -> {
                if (fish != null) {
                    loadFeedingScheduleForFish(fish.getId());
                }
            });
        }
    }
    
    private void cleanupFishDatabase() {
        viewModel.getAllFish().observe(this, fishList -> {
            if (fishList != null && fishList.size() > 0) {
                // First observation, remove observer after processing
                viewModel.getAllFish().removeObservers(this);
                
                // Map to store the most recent fish entry for each fish name
                java.util.Map<String, Fish> latestFishByName = new java.util.HashMap<>();
                
                // Track how many records we'll delete
                final int[] totalRecords = {fishList.size()};
                int duplicateCount = 0;
                
                // Find the most recent entry for each fish name
                for (Fish fish : fishList) {
                    String name = fish.getName();
                    if (!latestFishByName.containsKey(name) || 
                        (fish.getLastUpdated() != null && 
                         latestFishByName.get(name).getLastUpdated() != null &&
                         fish.getLastUpdated().after(latestFishByName.get(name).getLastUpdated()))) {
                        latestFishByName.put(name, fish);
                    }
                    else {
                        duplicateCount++;
                    }
                }
                
                // The number to keep is the unique fish names
                final int keepCount = latestFishByName.size();
                final int deleteCount = totalRecords[0] - keepCount;
                
                // Show confirmation dialog before proceeding
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Clean up database?")
                    .setMessage("Found " + totalRecords[0] + " total fish records with " + 
                               deleteCount + " duplicates. After cleanup, only " + 
                               keepCount + " records will remain (1 per fish type).\n\n" +
                               "Do you want to proceed with cleanup?")
                    .setPositiveButton("Yes, Clean Up", (dialog, which) -> {
                        // Delete all fish first
                        viewModel.deleteAllFish();
                        
                        // Then re-insert only the latest records
                        for (Fish fish : latestFishByName.values()) {
                            viewModel.insert(fish);
                        }
                        
                        // Show success message
                        Toast.makeText(this, "Database cleaned up! Removed " + 
                                      deleteCount + " duplicate records.", 
                                      Toast.LENGTH_LONG).show();
                        
                        // Refresh the dashboard data
                        loadFishData();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });
    }

    private void showAddFishTypeDialog() {
        // Create an EditText for the dialog
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter new fish type name");
        
        // Configure layout parameters for the EditText
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        
        // Add padding to the input field
        int paddingDp = 16;
        float density = getResources().getDisplayMetrics().density;
        int paddingPixel = (int)(paddingDp * density);
        input.setPadding(paddingPixel, paddingPixel, paddingPixel, paddingPixel);
        
        // Create alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Fish Type");
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton("Add", (dialog, which) -> {
            String fishName = input.getText().toString().trim();
            
            // Validate input
            if (fishName.isEmpty()) {
                Toast.makeText(this, "Fish name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if fish type already exists
            viewModel.getAllFish().observe(this, fishList -> {
                // Remove the observer after first use
                viewModel.getAllFish().removeObservers(this);
                
                boolean fishExists = false;
                for (Fish fish : fishList) {
                    if (fish.getName().equalsIgnoreCase(fishName)) {
                        fishExists = true;
                        break;
                    }
                }
                
                if (fishExists) {
                    Toast.makeText(this, "Fish type '" + fishName + "' already exists", Toast.LENGTH_SHORT).show();
                } else {
                    // Create a new fish entry with default values
                    Date now = new Date();
                    Fish newFish = new Fish(
                        fishName,
                        0,  // totalCount
                        0,  // aliveCount
                        0,  // deadCount
                        0,  // averageLength
                        0,  // averageWidth
                        0,  // averageWeight
                        0,  // feedPerFish
                        now,
                        now,
                        "",
                        ""
                    );
                    
                    // Save to database
                    viewModel.insert(newFish);
                    
                    // Show confirmation
                    Toast.makeText(this, "Added new fish type: " + fishName, Toast.LENGTH_SHORT).show();
                    
                    // Refresh fish data to update spinner
                    loadFishData();
                }
            });
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Add a method to handle empty state
    private void clearDashboard() {
        // Reset all dashboard UI elements to show no data
        fishCountOutput.setText("0");
        aliveCountText.setText("0");
        deadCountText.setText("0");
        // ... clear other UI elements as needed
    }

    private void updateTotalFeedAmount() {
        TextView totalFeedAmountView = findViewById(R.id.totalFeedAmount);
        
        // Get selected fish from spinner
        Spinner fishTypeSpinner = findViewById(R.id.fishTypeSpinner);
        if (fishTypeSpinner != null && fishTypeSpinner.getSelectedItem() != null) {
            String selectedFish = fishTypeSpinner.getSelectedItem().toString();
            
            // Get current date as timestamp
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long currentDate = calendar.getTimeInMillis();
            
            // Get schedules for today
            viewModel.getFishIdByName(selectedFish).observe(this, fishId -> {
                if (fishId != null) {
                    viewModel.getSchedulesForDate(fishId, currentDate).observe(this, schedules -> {
                        if (schedules != null && !schedules.isEmpty()) {
                            // Get fish count
                            viewModel.getFishById(fishId).observe(this, fish -> {
                                if (fish != null) {
                                    int fishCount = fish.getAliveCount(); // Using getAliveCount instead of getCount
                                    
                                    // Calculate total feed amount for the day
                                    float totalFeedAmount = 0;
                                    for (FeedingSchedule schedule : schedules) {
                                        totalFeedAmount += schedule.getFeedQuantity();
                                    }
                                    
                                    // Multiply by number of fish
                                    float totalDailyFeed = totalFeedAmount * fishCount;
                                    
                                    // Update the TextView
                                    String displayText = String.format("Total Feed Amount: %.1fg", totalDailyFeed);
                                    totalFeedAmountView.setText(displayText);
                                    
                                    Log.d("FeedCalculation", "Fish: " + selectedFish + 
                                        ", Count: " + fishCount + 
                                        ", Per fish: " + totalFeedAmount + 
                                        ", Total: " + totalDailyFeed);
                                }
                            });
                        } else {
                            totalFeedAmountView.setText("Total Feed Amount: 0g");
                        }
                    });
                }
            });
        } else {
            totalFeedAmountView.setText("Total Feed Amount: 0g");
        }
    }

    /**
     * Shows a dialog to input feed amount and send feed now command via SMS
     */
    private void showFeedNowDialog() {
        // Check if a fish is selected
        if (fishSelectionSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a fish first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedFishName = fishSelectionSpinner.getSelectedItem().toString();
        
        // Create dialog view with input field
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_feed_now, null);
        EditText feedAmountInput = dialogView.findViewById(R.id.feedAmountInput);
        
        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Feed Now");
        builder.setView(dialogView);
        
        // Set up confirm button
        builder.setPositiveButton("CONFIRM", null); // We'll override this below
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Override the positive button to prevent auto-dismiss on validation error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String amountStr = feedAmountInput.getText().toString().trim();
            
            // Validate input
            if (amountStr.isEmpty()) {
                feedAmountInput.setError("Please enter feed amount");
                return;
            }
            
            try {
                float feedAmount = Float.parseFloat(amountStr);
                if (feedAmount <= 0) {
                    feedAmountInput.setError("Amount must be greater than zero");
                    return;
                }
                
                // Get fish information to send SMS
                viewModel.getFishByName(selectedFishName).observe(this, fish -> {
                    if (fish != null && !TextUtils.isEmpty(fish.getPhoneNumber())) {
                        // Send SMS command for feed now
                        sendFeedNowSms(fish.getPhoneNumber(), fish.getName(), feedAmount);
                        Toast.makeText(this, "Feed command sent via SMS", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        // No phone number found
                        Toast.makeText(this, "No phone number associated with this fish", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                    
                    // Remove observer to avoid multiple callbacks
                    viewModel.getFishByName(selectedFishName).removeObservers(this);
                });
            } catch (NumberFormatException e) {
                feedAmountInput.setError("Please enter a valid number");
            }
        });
    }
    
    /**
     * Sends an SMS with the feed now command
     */
    private void sendFeedNowSms(String phoneNumber, String fishName, float feedAmount) {
        // Create the SMS message for feed now command
        String message = "FEED NOW: " + fishName + "\n" +
                         "Amount: " + feedAmount + "g per fish";
        
        // Use SmsUtils to send the message
        SmsUtils.sendFeedNowCommand(this, phoneNumber, fishName, feedAmount);
    }
}