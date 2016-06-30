package com.shubhankar.sleeptracker;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import java.util.ArrayList;

/**
 * Created by Shubhankar on 25/02/16.
 */
public class SleepState {
    @Expose
    @SerializedName("start_time")
    long startTime;

    @Expose
    @SerializedName("end_time")
    long endTime;

    @Expose
    @SerializedName("is_sleeping")
    boolean isSleeping;

    ArrayList<Float> lightReadings, accelerometerReadings;
    ArrayList<Boolean> screenStates;

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isSleeping() {
        return isSleeping;
    }

    public void setIsSleeping(boolean isSleeping) {
        this.isSleeping = isSleeping;
    }

    public String getInterval() {
        String interval = new DateTime(getStartTime()).toString("dd/MM/yy hh:mm:ss");
        interval = interval + " - " + new DateTime(getEndTime()).toString("dd/MM/yy hh:mm:ss");

        int duration = (int) getDuration();
        if (duration >= 60) {
            interval = interval + "\nTotal time : " + (int) duration / 60 + "hr " + duration % 60 + "min";

        } else {
            interval = interval + "\nTotal time : " + duration + "min";
        }

        return interval;
    }

    public float getDuration() {
        return (float) (getEndTime() - getStartTime()) / (float) (60 * 1000);

    }

    public ArrayList<Float> getLightReadings() {
        return lightReadings;
    }

    public ArrayList<Float> getAccelerometerReadings() {
        return accelerometerReadings;
    }

    public void setLightReadings(ArrayList<Float> lightReadings) {
        this.lightReadings = lightReadings;
    }

    public void setAccelerometerReadings(ArrayList<Float> accelerometerReadings) {
        this.accelerometerReadings = accelerometerReadings;
    }

    public void setScreenStates(ArrayList<Boolean> screenStates) {
        this.screenStates = screenStates;
    }

    public ArrayList<Boolean> getScreenStates() {
        return screenStates;
    }

    public void addAccelerometerData(ArrayList<Float> newData) {
        this.accelerometerReadings.addAll(newData);
    }

    public void addLightData(ArrayList<Float> newData) {
        this.lightReadings.addAll(newData);
    }

    public void addScreenData(ArrayList<Boolean> newData) {
        this.screenStates.addAll(newData);
    }
}
