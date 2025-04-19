package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.database.Fish;
import com.example.myapplication.viewmodel.AquacultureViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FeedingSetupActivity extends AppCompatActivity {

    private TextInputEditText originalFishCountInput;
    private TextInputEditText deadFishCountInput;
    private TextInputEditText fishLengthInput;
    private TextInputEditText fishWidthInput;
    private TextInputEditText fishWeightInput;
    private TextInputEditText feedPerFishInput;
    private TextInputEditText phoneNumberInput;
    private TextView aliveFishCountOutput;
    private TextView totalFeedAmountOutput;
    private Button calculateFeedButton;
    private Button saveFeedingSetupButton;
    private Spinner fishTypeSpinner;
    private AquacultureViewModel viewModel;
    private long currentFishId = -1;
    private boolean isDataLoaded = false;
    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feeding_setup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AquacultureViewModel.class);

        // Get the selected fish from intent extras
        String selectedFishName = getIntent().getStringExtra("SELECTED_FISH");
        
        // Initialize views
        initializeViews();
        
        // Set up navigation buttons
        setupNavigationButtons();
        
        // Set up fish type spinner with database data
        setupFishTypeSpinner(selectedFishName);
        
        // Set up calculation button
        setupCalculationButton();
        
        // Set up save button
        setupSaveButton();
        
        // Set up text change listeners for auto-calculation
        setupTextChangeListeners();
    }
    
    private void clearDatabaseCompletely() {
        // Use an executor to run database operations on a background thread
        executor.execute(() -> {
            // Delete all fish from the database directly
            viewModel.deleteAllFish();
            
            // Update UI on the main thread
            runOnUiThread(() -> {
                Toast.makeText(this, "Database cleared completely", Toast.LENGTH_SHORT).show();
                isDataLoaded = true;
                
                // Set up default spinner with "Add a new fish type"
                List<String> defaultList = new ArrayList<>();
                defaultList.add("Add a new fish type");
                ArrayAdapter<String> defaultAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item, defaultList);
                defaultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                fishTypeSpinner.setAdapter(defaultAdapter);
            });
        });
    }
    
    private void initializeViews() {
        originalFishCountInput = findViewById(R.id.originalFishCountInput);
        deadFishCountInput = findViewById(R.id.deadFishCount);
        fishLengthInput = findViewById(R.id.fishLengthInput);
        fishWidthInput = findViewById(R.id.fishWidthInput);
        fishWeightInput = findViewById(R.id.fishWeightInput);
        feedPerFishInput = findViewById(R.id.feedPerFishInput);
        aliveFishCountOutput = findViewById(R.id.aliveFishCountResult);
        totalFeedAmountOutput = findViewById(R.id.total_feed_amount_output);
        calculateFeedButton = findViewById(R.id.calculateFeedButton);
        saveFeedingSetupButton = findViewById(R.id.saveFeedingSetupButton);
        fishTypeSpinner = findViewById(R.id.fish_dropdown);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
    }
    
    private void setupNavigationButtons() {
        // Home/Dashboard button
        View homeButton = findViewById(R.id.dashboardButton);
        if (homeButton != null) {
            homeButton.setOnClickListener(view -> {
                Intent intent = new Intent(FeedingSetupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
        
        // Fish Template button
        View fishTemplateButton = findViewById(R.id.fishTemplateButton);
        if (fishTemplateButton != null) {
            fishTemplateButton.setOnClickListener(view -> {
                Intent intent = new Intent(FeedingSetupActivity.this, FishTemplatesActivity.class);
                startActivity(intent);
            });
        }
        
        // Edit Schedules button
        View editSchedulesButton = findViewById(R.id.editSchedulesButton);
        if (editSchedulesButton != null) {
            editSchedulesButton.setOnClickListener(view -> {
                Intent intent = new Intent(FeedingSetupActivity.this, FeedingScheduleSetup.class);
                startActivity(intent);
            });
        }
    }
    
    private void setupFishTypeSpinner(String selectedFishName) {
        // Create a list with a placeholder item
        List<String> fishNames = new ArrayList<>();
        fishNames.add("Select Fish Type"); // Add placeholder as first item
        
        // Create initial adapter with just the placeholder
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, fishNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fishTypeSpinner.setAdapter(adapter);
        
        // Initialize fields to disabled state until a fish is selected
        setInputFieldsEnabled(false);
        
        // Load fish data from database and add to the adapter
        viewModel.getAllFish().observe(this, fishList -> {
            if (fishList != null && !fishList.isEmpty()) {
                // Use a Set to eliminate duplicates
                Set<String> uniqueFishNames = new HashSet<>();
                for (Fish fish : fishList) {
                    uniqueFishNames.add(fish.getName());
                }
                
                // Keep the placeholder at position 0, add fish names after
                fishNames.clear();
                fishNames.add("Select Fish Type"); // Keep placeholder
                fishNames.addAll(uniqueFishNames); // Add all fish names
                
                // Notify adapter of data change
                adapter.notifyDataSetChanged();
                
                // Set listener for selection changes
                fishTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedFishName = parent.getItemAtPosition(position).toString();
                        
                        // Skip loading data for the placeholder item
                        if (position == 0 || selectedFishName.equals("Select Fish Type")) {
                            clearForm(); // Clear the form when placeholder is selected
                            currentFishId = -1;
                            setInputFieldsEnabled(false); // Disable input fields
                            return;
                        }
                        
                        // Enable input fields for a valid fish selection
                        setInputFieldsEnabled(true);
                        
                        // Find the fish with the selected name
                        for (Fish fish : fishList) {
                            if (fish.getName().equals(selectedFishName)) {
                                currentFishId = fish.getId();
                                loadFishData(fish);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Disable inputs when nothing is selected
                        setInputFieldsEnabled(false);
                    }
                });
                
                // If we have a selected fish from the intent, set the spinner to that fish
                if (selectedFishName != null && !selectedFishName.isEmpty()) {
                    int position = fishNames.indexOf(selectedFishName);
                    if (position >= 0) {
                        fishTypeSpinner.setSelection(position);
                        setInputFieldsEnabled(true); // Enable fields for the selected fish
                    }
                }
            } else {
                // No fish in database, keep fields disabled
                setInputFieldsEnabled(false);
            }
        });
    }
    
    private void clearForm() {
        originalFishCountInput.setText("");
        deadFishCountInput.setText("0");
        fishLengthInput.setText("");
        fishWidthInput.setText("");
        fishWeightInput.setText("");
        feedPerFishInput.setText("");
        aliveFishCountOutput.setText("Alive Fish Count: 0");
        totalFeedAmountOutput.setText("Total feed amount: 0g");
        phoneNumberInput.setText("");
    }
    
    private void loadFishData(Fish fish) {
        // Load fish data into the form
        originalFishCountInput.setText(String.valueOf(fish.getTotalCount()));
        deadFishCountInput.setText(String.valueOf(fish.getDeadCount()));
        fishLengthInput.setText(String.valueOf(fish.getAverageLength()));
        fishWidthInput.setText(String.valueOf(fish.getAverageWidth()));
        fishWeightInput.setText(String.valueOf(fish.getAverageWeight()));
        feedPerFishInput.setText(String.valueOf(fish.getFeedPerFish()));
        
        // Calculate alive fish count
        int aliveCount = fish.getTotalCount() - fish.getDeadCount();
        aliveFishCountOutput.setText("Alive Fish Count: " + aliveCount);
        
        // Calculate total feed amount if feed per fish is set
        if (fish.getFeedPerFish() > 0) {
            float totalFeed = aliveCount * fish.getFeedPerFish();
            totalFeedAmountOutput.setText("Total feed amount: " + totalFeed + "g");
        }
        
        if (fish.getPhoneNumber() != null) {
            phoneNumberInput.setText(fish.getPhoneNumber());
        }
    }
    
    private void setupCalculationButton() {
        calculateFeedButton.setOnClickListener(v -> calculateFeedAmount());
    }
    
    private void setupSaveButton() {
        saveFeedingSetupButton.setOnClickListener(v -> {
            // Validate inputs before saving
            if (validateInputs()) {
                saveFeedingSetup();
            }
        });
    }
    
    private void setupTextChangeListeners() {
        // Add text change listeners to automatically update alive fish count
        TextWatcher fishCountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateAliveFishCount();
            }
        };
        
        originalFishCountInput.addTextChangedListener(fishCountWatcher);
        deadFishCountInput.addTextChangedListener(fishCountWatcher);
    }
    
    private void updateAliveFishCount() {
        try {
            int originalCount = originalFishCountInput.getText().toString().isEmpty() ? 
                    0 : Integer.parseInt(originalFishCountInput.getText().toString());
            int deadCount = deadFishCountInput.getText().toString().isEmpty() ? 
                    0 : Integer.parseInt(deadFishCountInput.getText().toString());
            
            // Ensure dead count doesn't exceed original count
            if (deadCount > originalCount) {
                deadFishCountInput.setError("Dead fish cannot exceed original count");
                aliveFishCountOutput.setText("Alive Fish Count: 0");
                return;
            }
            
            int aliveCount = originalCount - deadCount;
            aliveFishCountOutput.setText("Alive Fish Count: " + aliveCount);
        } catch (NumberFormatException e) {
            aliveFishCountOutput.setText("Alive Fish Count: 0");
        }
    }
    
    private void calculateFeedAmount() {
        if (!validateInputs()) {
            return;
        }
        
        try {
            int originalCount = Integer.parseInt(originalFishCountInput.getText().toString());
            int deadCount = Integer.parseInt(deadFishCountInput.getText().toString());
            float feedPerFish = Float.parseFloat(feedPerFishInput.getText().toString());
            
            int aliveCount = originalCount - deadCount;
            float totalFeedAmount = aliveCount * feedPerFish;
            
            // Update the UI
            aliveFishCountOutput.setText("Alive Fish Count: " + aliveCount);
            totalFeedAmountOutput.setText("Total feed amount: " + totalFeedAmount + "g");
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Check if original fish count is entered
        if (originalFishCountInput.getText().toString().isEmpty()) {
            originalFishCountInput.setError("Required");
            isValid = false;
        }
        
        // Check if dead fish count is entered
        if (deadFishCountInput.getText().toString().isEmpty()) {
            deadFishCountInput.setText("0"); // Default to 0 if empty
        }
        
        // Check if feed per fish is entered
        if (feedPerFishInput.getText().toString().isEmpty()) {
            feedPerFishInput.setError("Required");
            isValid = false;
        }
        
        // Validate that dead fish count doesn't exceed original count
        try {
            int originalCount = Integer.parseInt(originalFishCountInput.getText().toString());
            int deadCount = Integer.parseInt(deadFishCountInput.getText().toString());
            
            if (deadCount > originalCount) {
                deadFishCountInput.setError("Dead fish cannot exceed original count");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            // This will be caught by the empty checks above
        }
        
        return isValid;
    }
    
    private void saveFeedingSetup() {
        try {
            // Get values from form
            int totalCount = Integer.parseInt(originalFishCountInput.getText().toString());
            int deadCount = Integer.parseInt(deadFishCountInput.getText().toString());
            float length = fishLengthInput.getText().toString().isEmpty() ? 0 : 
                    Float.parseFloat(fishLengthInput.getText().toString());
            float width = fishWidthInput.getText().toString().isEmpty() ? 0 : 
                    Float.parseFloat(fishWidthInput.getText().toString());
            float weight = fishWeightInput.getText().toString().isEmpty() ? 0 : 
                    Float.parseFloat(fishWeightInput.getText().toString());
            float feedPerFish = feedPerFishInput.getText().toString().isEmpty() ? 0 :
                    Float.parseFloat(feedPerFishInput.getText().toString());
            
            // Get selected fish name
            String fishName = fishTypeSpinner.getSelectedItem().toString();
            
            // Get phone number
            String phoneNumber = phoneNumberInput.getText().toString();
            
            // Create or update fish record
            Date now = new Date();
            
            if (currentFishId != -1) {
                // Update existing fish
                Fish updatedFish = new Fish(
                    fishName,
                    totalCount,
                    totalCount - deadCount,
                    deadCount,
                    length,
                    width,
                    weight,
                    feedPerFish,
                    now,
                    now,
                    "",
                    phoneNumber
                );
                updatedFish.setId(currentFishId);
                viewModel.update(updatedFish);
                Toast.makeText(this, "Fish information updated", Toast.LENGTH_SHORT).show();
                clearForm();
            } else {
                // Create new fish
                Fish newFish = new Fish(
                    fishName,
                    totalCount,
                    totalCount - deadCount,
                    deadCount,
                    length,
                    width,
                    weight,
                    feedPerFish,
                    now,
                    now,
                    "",
                    phoneNumber
                );
                
                viewModel.insert(newFish);
                Toast.makeText(this, "New fish added", Toast.LENGTH_SHORT).show();
                clearForm();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    // Add this new method to enable/disable all input fields
    private void setInputFieldsEnabled(boolean enabled) {
        // Disable/enable all input fields
        originalFishCountInput.setEnabled(enabled);
        deadFishCountInput.setEnabled(enabled);
        fishLengthInput.setEnabled(enabled);
        fishWidthInput.setEnabled(enabled);
        fishWeightInput.setEnabled(enabled);
        feedPerFishInput.setEnabled(enabled);
        
        // Disable/enable buttons
        calculateFeedButton.setEnabled(enabled);
        saveFeedingSetupButton.setEnabled(enabled);
        
        // Optional: Change the alpha to visually indicate disabled state
        float alpha = enabled ? 1.0f : 0.5f;
        originalFishCountInput.setAlpha(alpha);
        deadFishCountInput.setAlpha(alpha);
        fishLengthInput.setAlpha(alpha);
        fishWidthInput.setAlpha(alpha);
        fishWeightInput.setAlpha(alpha);
        feedPerFishInput.setAlpha(alpha);
        calculateFeedButton.setAlpha(alpha);
        saveFeedingSetupButton.setAlpha(alpha);
    }
}