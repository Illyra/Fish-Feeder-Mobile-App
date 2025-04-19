//package com.example.myapplication.ui.schedule;
//
//import android.app.AlertDialog;
//import android.app.TimePickerDialog;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.MaterialButton;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.core.util.Pair;
//
//import com.example.myapplication.FeedingScheduleSetup;
//import com.example.myapplication.R;
//import com.example.myapplication.database.FeedingSchedule;
//import com.example.myapplication.viewmodel.AquacultureViewModel;
//import com.google.android.material.datepicker.MaterialDatePicker;
//import com.google.android.material.textfield.TextInputEditText;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
///**
// * Manages all dialogs for schedule-related operations
// */
//public class ScheduleDialogManager {
//    private final FeedingScheduleSetup activity;
//    private final AquacultureViewModel viewModel;
//
//    // Callback interfaces
//    public interface DateRangeSelectedCallback {
//        void onDateRangeSelected(String scheduleName, Long startDate, Long endDate);
//    }
//
//    public interface DailyScheduleSavedCallback {
//        void onDailyScheduleSaved(List<FeedingSchedule> schedules);
//    }
//
//    public ScheduleDialogManager(FeedingScheduleSetup activity, AquacultureViewModel viewModel) {
//        this.activity = activity;
//        this.viewModel = viewModel;
//    }
//
//    /**
//     * Shows the date range picker dialog
//     */
//    public void showDateRangePicker(long fishId, DateRangeSelectedCallback callback) {
//        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
//            .setTitleText("Select Schedule Date Range");
//        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
//
//        picker.addOnPositiveButtonClickListener(selection -> {
//            Long startDate = selection.first;
//            Long endDate = selection.second;
//
//            // Check for overlapping schedules before proceeding
//            viewModel.getAllSchedulesForFish(fishId).observe(activity, existingSchedules -> {
//                if (existingSchedules != null && !existingSchedules.isEmpty()) {
//                    boolean hasOverlap = false;
//                    for (FeedingSchedule existing : existingSchedules) {
//                        Log.d("DatePicker", String.format("Checking overlap: new(%d-%d) vs existing(%d-%d)",
//                            startDate, endDate, existing.getStartDate(), existing.getEndDate()));
//
//                        // Check if date ranges overlap
//                        if (!(endDate < existing.getStartDate() || startDate > existing.getEndDate())) {
//                            hasOverlap = true;
//                            break;
//                        }
//                    }
//
//                    if (hasOverlap) {
//                        Toast.makeText(activity, "Selected date range overlaps with existing schedule", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                }
//
//                // No overlap found, proceed with schedule name dialog
//                showScheduleNameDialog(startDate, endDate, callback);
//            });
//        });
//
//        picker.show(activity.getSupportFragmentManager(), "DATE_PICKER");
//    }
//
//    /**
//     * Shows the schedule name dialog
//     */
//    private void showScheduleNameDialog(Long startDate, Long endDate, DateRangeSelectedCallback callback) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_schedule_name, null);
//        TextInputEditText nameInput = dialogView.findViewById(R.id.scheduleNameInput);
//
//        builder.setView(dialogView)
//               .setTitle("Name Your Schedule")
//               .setPositiveButton("Continue", (dialog, which) -> {
//                   String scheduleName = nameInput.getText().toString();
//                   if (!TextUtils.isEmpty(scheduleName)) {
//                       callback.onDateRangeSelected(scheduleName, startDate, endDate);
//                   }
//               })
//               .setNegativeButton("Cancel", null)
//               .show();
//    }
//
//    /**
//     * Shows the daily schedule editing dialog
//     */
//    public void showDailySchedulesDialog(List<FeedingSchedule> scheduleGroup, DailyScheduleSavedCallback callback) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//        View dialogView = activity.getLayoutInflater().inflate(R.layout.daily_schedules_dialog, null);
//        LinearLayout dailySchedulesContainer = dialogView.findViewById(R.id.dailySchedulesContainer);
//        com.google.android.material.button.MaterialButton addTimeButton = dialogView.findViewById(R.id.addTimeButton);
//        com.google.android.material.button.MaterialButton saveButton = dialogView.findViewById(R.id.saveButton);
//        com.google.android.material.button.MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
//
//        // Clear any existing views first
//        dailySchedulesContainer.removeAllViews();
//
//        // Sort schedules by time
//        Collections.sort(scheduleGroup, (s1, s2) -> {
//            try {
//                SimpleDateFormat parser = new SimpleDateFormat("hh:mm a", Locale.getDefault());
//                Date time1 = parser.parse(s1.getFeedingTime());
//                Date time2 = parser.parse(s2.getFeedingTime());
//                return time1.compareTo(time2);
//            } catch (ParseException e) {
//                Log.e("TimeSort", "Error parsing time", e);
//                return 0;
//            }
//        });
//
//        // Add existing feeding times
//        for (FeedingSchedule schedule : scheduleGroup) {
//            View timeScheduleItem = activity.getLayoutInflater().inflate(R.layout.daily_schedule_item, dailySchedulesContainer, false);
//            TextView timeText = timeScheduleItem.findViewById(R.id.timeText);
//            TextView quantityText = timeScheduleItem.findViewById(R.id.quantityText);
//            com.google.android.material.button.MaterialButton deleteButton = timeScheduleItem.findViewById(R.id.deleteTimeButton);
//
//            timeText.setText(schedule.getFeedingTime());
//            quantityText.setText(String.format("%.1fg", schedule.getFeedQuantity()));
//            quantityText.setTextColor(Color.parseColor("#FFFFFF"));
//
//            deleteButton.setOnClickListener(v -> {
//                dailySchedulesContainer.removeView(timeScheduleItem);
//            });
//
//            dailySchedulesContainer.addView(timeScheduleItem);
//        }
//
//        // Add new time button handler
//        addTimeButton.setOnClickListener(v -> {
//            showAddTimeDialog(dailySchedulesContainer);
//        });
//
//        AlertDialog dialog = builder.setView(dialogView).create();
//
//        // Set up save button
//        saveButton.setOnClickListener(v -> {
//            List<FeedingSchedule> updatedSchedules = collectSchedulesFromDialog(
//                scheduleGroup.get(0), dailySchedulesContainer);
//            if (!updatedSchedules.isEmpty()) {
//                callback.onDailyScheduleSaved(updatedSchedules);
//            }
//            dialog.dismiss();
//        });
//
//        // Set up cancel button
//        cancelButton.setOnClickListener(v -> {
//            dialog.dismiss();
//        });
//
//        dialog.show();
//    }
//
//    /**
//     * Collects schedule information from the dialog
//     */
//    private List<FeedingSchedule> collectSchedulesFromDialog(FeedingSchedule template, LinearLayout container) {
//        List<FeedingSchedule> updatedSchedules = new ArrayList<>();
//
//        for (int i = 0; i < container.getChildCount(); i++) {
//            View timeItem = container.getChildAt(i);
//            TextView timeText = timeItem.findViewById(R.id.timeText);
//            TextView quantityText = timeItem.findViewById(R.id.quantityText);
//
//            String time = timeText.getText().toString();
//            String quantityStr = quantityText.getText().toString()
//                .replace("g", "")
//                .trim();
//
//            if (TextUtils.isEmpty(quantityStr)) {
//                continue;
//            }
//
//            try {
//                float quantity = Float.parseFloat(quantityStr);
//                FeedingSchedule schedule = new FeedingSchedule(
//                    template.getFishId(),
//                    template.getScheduleName(),
//                    template.getStartDate(),
//                    template.getEndDate(),
//                    time,
//                    quantity
//                );
//                updatedSchedules.add(schedule);
//            } catch (NumberFormatException e) {
//                Log.e("SaveSchedule", "Error parsing quantity: '" + quantityStr + "'", e);
//                Toast.makeText(activity, "Error parsing quantity: " + quantityStr, Toast.LENGTH_SHORT).show();
//                return new ArrayList<>(); // Return an empty list if there's an error
//            }
//        }
//
//        if (updatedSchedules.isEmpty()) {
//            Toast.makeText(activity, "Please add at least one feeding time", Toast.LENGTH_SHORT).show();
//            return updatedSchedules;
//        }
//
//        // Sort before returning
//        Collections.sort(updatedSchedules, (s1, s2) -> {
//            try {
//                SimpleDateFormat parser = new SimpleDateFormat("hh:mm a", Locale.getDefault());
//                Date time1 = parser.parse(s1.getFeedingTime());
//                Date time2 = parser.parse(s2.getFeedingTime());
//                return time1.compareTo(time2);
//            } catch (ParseException e) {
//                Log.e("TimeSort", "Error parsing time", e);
//                return 0;
//            }
//        });
//
//        return updatedSchedules;
//    }
//
//    /**
//     * Shows dialog to add a new feeding time
//     */
//    private void showAddTimeDialog(LinearLayout container) {
//        View timePickerView = activity.getLayoutInflater().inflate(R.layout.feeding_time_card, null);
//        EditText timeInput = timePickerView.findViewById(R.id.weekNumTimeNum);
//        TextInputEditText quantityInput = timePickerView.findViewById(R.id.feedQuantityInput);
//
//        // Change card border color to white
//        if (timePickerView instanceof com.google.android.material.card.MaterialCardView) {
//            com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) timePickerView;
//            cardView.setStrokeColor(Color.WHITE);
//            cardView.setStrokeWidth(0); // Remove border completely
//        }
//
//        // Setup time picker
//        timeInput.setOnClickListener(v -> {
//            Calendar calendar = Calendar.getInstance();
//            int hour = calendar.get(Calendar.HOUR_OF_DAY);
//            int minute = calendar.get(Calendar.MINUTE);
//
//            TimePickerDialog timePickerDialog = new TimePickerDialog(activity,
//                (view, hourOfDay, selectedMinute) -> {
//                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
//                    calendar.set(Calendar.MINUTE, selectedMinute);
//                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
//                    timeInput.setText(sdf.format(calendar.getTime()));
//                }, hour, minute, false);
//            timePickerDialog.show();
//        });
//
//        // Create the dialog with white background
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//        AlertDialog dialog = builder
//            .setTitle("Add Feeding Time")
//            .setView(timePickerView)
//            .setPositiveButton("Add", null)  // Set to null initially to prevent auto-dismiss
//            .setNegativeButton("Cancel", null)
//            .create();
//
//        // Set background color
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#F5F5F5")));
//
//        // Set title text color to black
//        dialog.setOnShowListener(dialogInterface -> {
//            // Set title color to black
//            int titleId = activity.getResources().getIdentifier("alertTitle", "id", "android");
//            TextView titleView = dialog.findViewById(titleId);
//            if (titleView != null) {
//                titleView.setTextColor(Color.BLACK);
//            }
//
//            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
//            positiveButton.setOnClickListener(v -> {
//                String time = timeInput.getText().toString();
//                String quantityStr = quantityInput.getText().toString().trim();
//
//                if (!TextUtils.isEmpty(time) && !TextUtils.isEmpty(quantityStr)) {
//                    try {
//                        float quantity = Float.parseFloat(quantityStr);
//                        View timeScheduleItem = activity.getLayoutInflater().inflate(R.layout.daily_schedule_item, container, false);
//                        TextView timeText = timeScheduleItem.findViewById(R.id.timeText);
//                        TextView quantityText = timeScheduleItem.findViewById(R.id.quantityText);
//                        com.google.android.material.button.MaterialButton deleteButton = timeScheduleItem.findViewById(R.id.deleteTimeButton);
//
//                        timeText.setText(time);
//                        quantityText.setText(String.format("%.1fg", quantity));
//                        quantityText.setTextColor(Color.parseColor("#FFFFFF"));
//
//                        deleteButton.setOnClickListener(v2 -> {
//                            container.removeView(timeScheduleItem);
//                        });
//
//                        container.addView(timeScheduleItem);
//                        dialog.dismiss();
//                    } catch (NumberFormatException e) {
//                        Toast.makeText(activity, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    Toast.makeText(activity, "Please fill in all fields", Toast.LENGTH_SHORT).show();
//                }
//            });
//        });
//
//        dialog.show();
//    }
//
//    /**
//     * Shows the schedule deletion confirmation dialog
//     */
//    public void showDeleteScheduleDialog(long fishId, long startDate, long endDate, Runnable onDeleteConfirmed) {
//        new AlertDialog.Builder(activity)
//            .setTitle("Delete Schedule")
//            .setMessage("Are you sure you want to delete this schedule?")
//            .setPositiveButton("Delete", (dialog, which) -> {
//                viewModel.deleteSchedulesForDateRange(fishId, startDate, endDate)
//                    .observe(activity, success -> {
//                        if (success) {
//                            Toast.makeText(activity, "Schedule deleted", Toast.LENGTH_SHORT).show();
//                            // Execute callback if deletion was successful
//                            if (onDeleteConfirmed != null) {
//                                onDeleteConfirmed.run();
//                            }
//                        } else {
//                            Toast.makeText(activity, "Error deleting schedule", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//            })
//            .setNegativeButton("Cancel", null)
//            .show();
//    }
//}