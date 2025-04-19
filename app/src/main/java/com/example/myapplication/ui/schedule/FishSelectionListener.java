package com.example.myapplication.ui.schedule;

/**
 * Interface for handling fish selection events
 */
public interface FishSelectionListener {
    /**
     * Called when a fish is selected
     * 
     * @param fishId The ID of the selected fish
     * @param fishName The name of the selected fish
     */
    void onFishSelected(long fishId, String fishName);
} 