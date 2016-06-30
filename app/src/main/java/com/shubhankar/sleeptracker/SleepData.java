package com.shubhankar.sleeptracker;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Shubhankar on 24/02/16.
 */
public class SleepData {
    @Expose
    @SerializedName("screen_state")
    boolean screenState;

    @Expose
    @SerializedName("accelerometer")
    float accelerometer;

    @Expose
    @SerializedName("light")
    float light;

    @Expose
    @SerializedName("time")
    long time;

    @Expose
    @SerializedName("end_time")
    long endTime;

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
        if (getAccelerometer() >= 1.1 || getAccelerometer() <= 0.8) {
            move = true;
        }
        if (!getScreenState() && !move && getLight() < 15.0f) {
            return true;
        } else {
            return false;
        }
    }


}
