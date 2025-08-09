package com.example.myapplication.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import com.example.myapplication.database.FeedingSchedule;
import com.example.myapplication.database.Fish;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class SmsUtils {
    private static final int SMS_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "SmsUtils";

    /**
     * Sends a schedule notification via SMS
     * 
     * @param phoneNumber The phone number to send the SMS to
     * @param fishName The name of the fish
     * @param schedules The schedules to send
     */
    public static void sendScheduleNotification(Activity activity, String phoneNumber, String fishName, List<FeedingSchedule> schedules) {
        if (TextUtils.isEmpty(phoneNumber) || schedules == null || schedules.isEmpty()) return;

        // Check for SMS permission
        if (!checkSmsPermission(activity)) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("New feeding schedule for ").append(fishName).append("\n\n");
        
        // Sort schedules by date and time
        Collections.sort(schedules, (s1, s2) -> {
            // First compare by time
            int timeCompare = s1.getFeedingTime().compareTo(s2.getFeedingTime());
            if (timeCompare != 0) {
                return timeCompare;
            }
            // If times are the same, compare by date
            return Long.compare(s1.getStartDate(), s2.getStartDate());
        });
        
        // Group schedules by time and quantity
        Map<String, List<FeedingSchedule>> schedulesByTime = new TreeMap<>();
        for (FeedingSchedule schedule : schedules) {
            String timeKey = schedule.getFeedingTime() + "_" + schedule.getFeedQuantity();
            if (!schedulesByTime.containsKey(timeKey)) {
                schedulesByTime.put(timeKey, new ArrayList<>());
            }
            schedulesByTime.get(timeKey).add(schedule);
        }
        
        // Format the message with date ranges and times
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        
        // Add date range header if we have schedules
        if (!schedules.isEmpty()) {
            // Find min startDate and max endDate
            long minStartDate = Long.MAX_VALUE;
            long maxEndDate = Long.MIN_VALUE;
            for (FeedingSchedule s : schedules) {
                if (s.getStartDate() < minStartDate) minStartDate = s.getStartDate();
                if (s.getEndDate() > maxEndDate) maxEndDate = s.getEndDate();
            }
            
            // Add date range
            message.append(dateFormat.format(new Date(minStartDate)))
                  .append(" - ")
                  .append(dateFormat.format(new Date(maxEndDate)))
                  .append(":\n");
        }
        
        // Add feeding times with quantities
        for (Map.Entry<String, List<FeedingSchedule>> entry : schedulesByTime.entrySet()) {
            String[] timeParts = entry.getKey().split("_");
            String time = timeParts[0];
            String quantity = timeParts[1];
            
            message.append("- ")
                  .append(time)
                  .append(" - ")
                  .append(quantity)
                  .append("g\n");
        }
        // Always ensure the message ends with a newline for Arduino parsing
        if (message.length() > 0 && message.charAt(message.length() - 1) != '\n') {
            message.append('\n');
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            String finalMessage = message.toString();
            
            // Force ASCII/GSM 7-bit encoding by removing any non-ASCII characters
            finalMessage = finalMessage.replaceAll("[^\\x00-\\x7F]", "");
            
            ArrayList<String> parts = smsManager.divideMessage(finalMessage);
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            
            Toast.makeText(activity, "Schedule notification sent", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "SMS sent to " + phoneNumber + " with " + parts.size() + " parts");
        } catch (Exception e) {
            Toast.makeText(activity, "Failed to send SMS notification", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "SMS send error", e);
        }
    }

    /**
     * Sends a schedule SMS for a specific week
     */
    public static void sendScheduleSms(Activity activity, String phoneNumber, String fishName, int weekNumber, List<FeedingSchedule> schedules) {
        if (checkSmsPermission(activity)) {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Feeding Schedule for ").append(fishName)
                        .append(" - Week ").append(weekNumber).append("\n\n");

            float totalDailyFeed = 0;
            for (FeedingSchedule schedule : schedules) {
                messageBuilder.append("Time: ").append(schedule.getFeedingTime())
                            .append(" - ").append(schedule.getFeedQuantity())
                            .append("g\n");
                totalDailyFeed += schedule.getFeedQuantity();
            }
            
            messageBuilder.append("\nTotal Daily Feed: ").append(totalDailyFeed).append("g");

            try {
                SmsManager smsManager = SmsManager.getDefault();
                ArrayList<String> parts = smsManager.divideMessage(messageBuilder.toString());
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
                Toast.makeText(activity, "Schedule SMS sent successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(activity, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Checks if the app has SMS permission and requests it if not
     * 
     * @return true if the permission is already granted, false otherwise
     */
    public static boolean checkSmsPermission(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.SEND_SMS},
                SMS_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }
} 