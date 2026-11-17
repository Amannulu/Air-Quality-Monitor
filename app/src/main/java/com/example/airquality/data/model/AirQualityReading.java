package com.example.airquality.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "air_quality_readings")
public class AirQualityReading {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String dateFormatted;
    private int aqi;
    private String category;
    private String location;
    private double pm25;
    private double pm10;
    private double co;
    private long timestamp;

    public AirQualityReading(int id, String dateFormatted, int aqi, String category, String location, double pm25, double pm10, double co, long timestamp) {
        this.id = id;
        this.dateFormatted = dateFormatted;
        this.aqi = aqi;
        this.category = category;
        this.location = location;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.co = co;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDateFormatted() { return dateFormatted; }
    public void setDateFormatted(String dateFormatted) { this.dateFormatted = dateFormatted; }

    public int getAqi() { return aqi; }
    public void setAqi(int aqi) { this.aqi = aqi; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getPm25() { return pm25; }
    public void setPm25(double pm25) { this.pm25 = pm25; }

    public double getPm10() { return pm10; }
    public void setPm10(double pm10) { this.pm10 = pm10; }

    public double getCo() { return co; }
    public void setCo(double co) { this.co = co; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
