package com.shubhankar.sleeptracker;

import android.hardware.SensorManager;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Shubhankar on 24/02/16.
 */
public class SleepData {
    @Expose
    @SerializedName("screen_state")
    private boolean screenState;

    @Expose
    @SerializedName("accelerometer")
    private float accelerometer;

    @Expose
    @SerializedName("light")
    private float light;

    @Expose
    @SerializedName("time")
    private long time;

    @Expose
    @SerializedName("end_time")
    private long endTime;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean getScreenState() {
        return screenState;
    }

    public void setScreenState(boolean screenState) {
        this.screenState = screenState;
    }

    public float getAccelerometer() {
        return accelerometer;
    }

    public void setAccelerometer(float accelerometer) {
        this.accelerometer = accelerometer;
    }

    public float getLight() {
        return light;
    }

    public void setLight(float light) {
        this.light = light;
    }

    Boolean getCase() {
        boolean move = false;
        if (getAccelerometer() >= 1.2 || getAccelerometer() <= 0.8) {
            move = true;
        }
        return !getScreenState() && !move;
    }


}
