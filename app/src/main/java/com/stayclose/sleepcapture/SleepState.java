package com.stayclose.sleepcapture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Shubhankar on 25/02/16.
 */
public class SleepState {

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
        String interval = "";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm:ss");
        Date date = new Date();

        date.setTime(getStartTime());

        interval = sdf.format(date);

        date.setTime(getEndTime());

        interval = interval + " - " + sdf.format(date);

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

    long startTime, endTime;
    boolean isSleeping;
    ArrayList<Float> lightReadings, accelerometerReadings;
    ArrayList<Boolean> screenStates;
}
