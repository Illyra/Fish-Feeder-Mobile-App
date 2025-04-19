package com.example.myapplication.ui.schedule;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.FeedingScheduleSetup;
import com.example.myapplication.R;
import com.example.myapplication.database.Fish;
import com.example.myapplication.viewmodel.AquacultureViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages fish selection functionality
 */
public class FishSelectionManager {
    private final FeedingScheduleSetup activity;
    private final AquacultureViewModel viewModel;
    private final Spinner fishSpinner;
    private List<FishItem> fishList = new ArrayList<>();
    private FishSelectionListener listener;
    private String initialSelectedFish;

    public FishSelectionManager(FeedingScheduleSetup activity, AquacultureViewModel viewModel, String initialSelectedFish) {
        this.activity = activity;
        this.viewModel = viewModel;
        this.initialSelectedFish = initialSelectedFish;
        this.fishSpinner = activity.findViewById(R.id.fish_dropdown);
        
        // Initialize the spinner
        setupFishSpinner();
    }

    /**
     * Set the listener for fish selection events
     */
    public void setListener(FishSelectionListener listener) {
        this.listener = listener;
    }

    /**
     * Setup the fish spinner
     */
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
            activity,
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
        
        MaterialButton createScheduleButton = activity.findViewById(R.id.createScheduleButton);
        createScheduleButton.setEnabled(false);
        
        // First, load fish data
        viewModel.getAllFish().observe(activity, fishEntities -> {
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
                if (initialSelectedFish != null && !initialSelectedFish.isEmpty()) {
                    for (int i = 0; i < fishList.size(); i++) {
                        if (fishList.get(i).name.equals(initialSelectedFish)) {
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
                        
                        if (selectedFish.name.equals("Select Fish")) {
                            // Disable create button for default selection
                            createScheduleButton.setEnabled(false);
                            createScheduleButton.setAlpha(0.5f);
                            
                            // Notify listener of deselection
                            if (listener != null) {
                                listener.onFishSelected(-1, "");
                            }
                            return;
                        }
                        
                        // Enable create button and notify listener
                        createScheduleButton.setEnabled(true);
                        createScheduleButton.setAlpha(1.0f);
                        
                        if (listener != null) {
                            listener.onFishSelected(selectedFish.id, selectedFish.name);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        createScheduleButton.setEnabled(false);
                        createScheduleButton.setAlpha(0.5f);
                        
                        // Notify listener
                        if (listener != null) {
                            listener.onFishSelected(-1, "");
                        }
                    }
                });
            }
        });
    }

    /**
     * Resets the fish selection
     */
    public void resetFishSelection() {
        fishSpinner.setSelection(0); // Select the "Select Fish" item
        Toast.makeText(activity, "Please select a fish type", Toast.LENGTH_SHORT).show();
    }

    /**
     * Get the currently selected fish ID
     */
    public long getSelectedFishId() {
        FishItem selectedFish = (FishItem) fishSpinner.getSelectedItem();
        return selectedFish != null ? selectedFish.id : -1;
    }

    /**
     * Get the currently selected fish item
     */
    public FishItem getSelectedFishItem() {
        return (FishItem) fishSpinner.getSelectedItem();
    }

    /**
     * Fish item class for spinner
     */
    public static class FishItem {
        public final String name;
        public final long id;

        public FishItem(String name, long id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return name;
        }
    }
} 