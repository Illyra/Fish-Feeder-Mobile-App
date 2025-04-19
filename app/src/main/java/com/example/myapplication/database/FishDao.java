package com.example.myapplication.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FishDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Fish fish);
    
    @Update
    void update(Fish fish);
    
    @Delete
    void delete(Fish fish);
    
    @Query("SELECT * FROM fish WHERE id = :id")
    LiveData<Fish> getFishById(long id);
    
    @Query("SELECT * FROM fish")
    LiveData<List<Fish>> getAllFish();
    
    @Query("SELECT * FROM fish WHERE name = :name LIMIT 1")
    LiveData<Fish> getFishByName(String name);
    
    @Query("DELETE FROM fish")
    void deleteAll();
    
    @Query("SELECT id FROM fish WHERE name = :fishName LIMIT 1")
    LiveData<Long> getFishIdByName(String fishName);
} 