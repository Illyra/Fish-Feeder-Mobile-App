package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {

    private final Context context;
    private final String[] templateNames;
    private final String[] templateDescriptions;

    public TemplateAdapter(Context context, String[] templateNames, String[] templateDescriptions) {
        this.context = context;
        this.templateNames = templateNames;
        this.templateDescriptions = templateDescriptions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fish_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.templateNameTextView.setText(templateNames[position]);
        holder.templateDescriptionTextView.setText(templateDescriptions[position]);
        
        // Set up button click listeners
        holder.applyTemplateButton.setOnClickListener(v -> {
            Toast.makeText(context, "Applied template: " + templateNames[position], Toast.LENGTH_SHORT).show();
            // In a real app, you would apply the template to the current fish setup
        });
        
        holder.editTemplateButton.setOnClickListener(v -> {
            Toast.makeText(context, "Editing template: " + templateNames[position], Toast.LENGTH_SHORT).show();
            // In a real app, you would open an editor for this template
        });
    }

    @Override
    public int getItemCount() {
        return templateNames.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView templateNameTextView;
        TextView templateDescriptionTextView;
        Button applyTemplateButton;
        Button editTemplateButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            templateNameTextView = itemView.findViewById(R.id.templateNameTextView);
            templateDescriptionTextView = itemView.findViewById(R.id.templateDescriptionTextView);
            applyTemplateButton = itemView.findViewById(R.id.applyTemplateButton);
            editTemplateButton = itemView.findViewById(R.id.editTemplateButton);
        }
    }
} 