package com.example.myapplication.database;

import androidx.room.TypeConverter;
import java.util.Date;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    // Keep these methods if you plan to use Lists in other parts of your app
    // Otherwise, they can be safely removed
    /*
    @TypeConverter
    public static List<String> fromString(String value) {
        return new Gson().fromJson(value, new TypeToken<List<String>>(){}.getType());
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        return new Gson().toJson(list);
    }
    */
} 