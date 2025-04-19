package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FishTemplatesActivity extends AppCompatActivity {

    private RecyclerView templatesRecyclerView;
    private Button createNewTemplateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fish_templates);

        // Initialize views
        templatesRecyclerView = findViewById(R.id.templatesRecyclerView);
        createNewTemplateButton = findViewById(R.id.createNewTemplateButton);

        // Set up RecyclerView
        templatesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Load sample templates (in a real app, these would come from a database)
        loadSampleTemplates();
        
        // Set up create new template button
        createNewTemplateButton.setOnClickListener(v -> {
            // Open template creation screen
            Toast.makeText(this, "Create new template feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        // Set up back button
        findViewById(R.id.backButton).setOnClickListener(v -> {
            finish(); // Return to previous screen
        });
    }
    
    private void loadSampleTemplates() {
        // Sample data - in a real app, this would come from a database
        String[] templateNames = {
            "Tilapia - Standard Growth",
            "Tilapia - Accelerated Growth",
            "Catfish - Standard",
            "Trout - Cold Water",
            "Carp - Warm Water"
        };
        
        String[] templateDescriptions = {
            "Standard feeding schedule for normal tilapia growth",
            "Intensive feeding for accelerated tilapia growth",
            "Balanced feeding for catfish",
            "Specialized feeding for cold water trout",
            "Optimal feeding for warm water carp species"
        };
        
        // Create and set adapter (you would need to create this adapter class)
        TemplateAdapter adapter = new TemplateAdapter(this, templateNames, templateDescriptions);
        templatesRecyclerView.setAdapter(adapter);
    }
} 