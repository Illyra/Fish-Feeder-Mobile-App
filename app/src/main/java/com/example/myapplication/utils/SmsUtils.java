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
        if (TextUtils.isEmpty(phoneNumber)) return;

        // Check for SMS permission
        if (!checkSmsPermission(activity)) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("New feeding schedule for ").append(fishName).append(":\n");
        
        // Sort schedules by date and time
        Collections.sort(schedules, (s1, s2) -> {
            // First compare by date
            int dateCompare = Long.compare(s1.getStartDate(), s2.getStartDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
            
            // If dates are the same, compare by time
            return s1.getFeedingTime().compareTo(s2.getFeedingTime());
        });
        
        // Group schedules by date
        Map<Long, List<FeedingSchedule>> schedulesByDate = new TreeMap<>();
        for (FeedingSchedule schedule : schedules) {
            long date = schedule.getStartDate();
            if (!schedulesByDate.containsKey(date)) {
                schedulesByDate.put(date, new ArrayList<>());
            }
            schedulesByDate.get(date).add(schedule);
        }
        
        // Format the message with dates and times
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        for (Map.Entry<Long, List<FeedingSchedule>> entry : schedulesByDate.entrySet()) {
            long date = entry.getKey();
            List<FeedingSchedule> dateSchedules = entry.getValue();
            
            // Add date header
            message.append("\n").append(dateFormat.format(new Date(date))).append(":\n");
            
            // Sort schedules for this date by time
            Collections.sort(dateSchedules, (s1, s2) -> s1.getFeedingTime().compareTo(s2.getFeedingTime()));
            
            // Add feeding times for this date
            for (FeedingSchedule schedule : dateSchedules) {
                message.append("• ")
                      .append(schedule.getFeedingTime())
                      .append(" - ")
                      .append(String.format("%.1fg", schedule.getFeedQuantity()))
                      .append("\n");
            }
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message.toString());
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            
            Toast.makeText(activity, "Schedule notification sent", Toast.LENGTH_SHORT).show();
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