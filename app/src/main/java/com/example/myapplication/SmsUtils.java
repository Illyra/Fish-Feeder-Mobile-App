package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.database.FeedingSchedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsUtils {

    private static final String TAG = "SmsUtils";

    /**
     * Sends an SMS notification about a feeding schedule
     */
    public static void sendScheduleNotification(Context context, String phoneNumber, String fishName, List<FeedingSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            Log.e(TAG, "Cannot send SMS: Schedule list is empty");
            return;
        }

        if (!hasSmsSendPermission(context)) {
            Log.e(TAG, "Cannot send SMS: Permission not granted");
            Toast.makeText(context, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        FeedingSchedule firstSchedule = schedules.get(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String startDate = dateFormat.format(new Date(firstSchedule.getStartDate()));
        String endDate = dateFormat.format(new Date(firstSchedule.getEndDate()));

        // Build schedule message
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("FISH FEEDER: Schedule for ").append(fishName).append("\n");
        messageBuilder.append("Schedule: ").append(firstSchedule.getScheduleName()).append("\n");
        messageBuilder.append("Dates: ").append(startDate).append(" - ").append(endDate).append("\n\n");
        messageBuilder.append("Feeding times:\n");

        for (FeedingSchedule schedule : schedules) {
            messageBuilder.append("• ").append(schedule.getFeedingTime())
                    .append(" - ").append(schedule.getFeedQuantity()).append("g\n");
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, messageBuilder.toString(), null, null);
            Log.d(TAG, "SMS sent to " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS: " + e.getMessage());
            Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Sends an SMS with a command to feed fish immediately
     */
    public static void sendFeedNowCommand(Context context, String phoneNumber, String fishName, float feedAmount) {
        if (!hasSmsSendPermission(context)) {
            Log.e(TAG, "Cannot send SMS: Permission not granted");
            Toast.makeText(context, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Build feed now command message
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("FEED NOW COMMAND\n");
        messageBuilder.append("Fish: ").append(fishName).append("\n");
        messageBuilder.append("Amount: ").append(String.format("%.1f", feedAmount)).append("g per fish\n");
        messageBuilder.append("Time: ").append(new SimpleDateFormat("hh:mm a, MMM dd", Locale.getDefault()).format(new Date()));
        
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, messageBuilder.toString(), null, null);
            Log.d(TAG, "Feed now SMS sent to " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send feed now SMS: " + e.getMessage());
            Toast.makeText(context, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Checks if the app has permission to send SMS
     */
    private static boolean hasSmsSendPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }
} 