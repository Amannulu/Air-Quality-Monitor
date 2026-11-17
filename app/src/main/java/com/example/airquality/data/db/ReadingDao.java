package com.example.airquality.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.airquality.data.model.AirQualityReading;

import java.util.List;

@Dao
public interface ReadingDao {

    @Query("SELECT * FROM air_quality_readings ORDER BY id DESC")
    LiveData<List<AirQualityReading>> getAllReadings();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReading(AirQualityReading reading);

    @Delete
    void deleteReading(AirQualityReading reading);

    @Query("DELETE FROM air_quality_readings WHERE id = :id")
    void deleteById(int id);
}
