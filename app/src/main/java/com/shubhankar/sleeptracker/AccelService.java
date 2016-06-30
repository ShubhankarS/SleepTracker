package com.shubhankar.sleeptracker;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Shubhankar on 28-10-2015.
 */
public class AccelService extends Service implements SensorEventListener {
    private SensorManager mSensMan;
    private Sensor accelerometer, light;
    private boolean moving;
    private boolean walk;
    private long startService;
    private SharedPreferences preference;
    private float relativeGravity;
    //some notification parameters
    private NotificationManager notificationManager;

    private SleepData sleepData;
    private List<SleepData> sleepDataList;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sleepData = new SleepData();
        String data = preference.getString(MainActivity.DATA_LIST, "");
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<SleepData>>() {
        }.getType();
        sleepDataList = gson.fromJson(data, listType);

        screenState();

        Log.v("Accel servicestarted", "AccelserviceStarted");
        //initialize sensorManager
        mSensMan = (SensorManager) getSystemService(SENSOR_SERVICE);

        //initialize accelerometer
        accelerometer = mSensMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        light = mSensMan.getDefaultSensor(Sensor.TYPE_LIGHT);

        //get current time
        Calendar cal = Calendar.getInstance();
        startService = cal.getTimeInMillis();
        Log.v("Current time", startService + "");

        //register accelerometer listener
        mSensMan.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensMan.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context ctx = getApplicationContext();
        preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        walk = false;
        moving = false;
        //initialize notifications
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {
            //monitor only for accelerometer changes
            int type = sensorEvent.sensor.getType();
            if (type == android.hardware.Sensor.TYPE_ACCELEROMETER) {

                //get times of when sensor is being changed
                Calendar cal = Calendar.getInstance();
                long now = cal.getTimeInMillis();
//                Log.d("NOW", "" + now);

                //monitor raw data for first 2 seconds, can be skipped as soon as a movement is detected
                if ((!(now - startService >= 2 * 1000) || relativeGravity == 0.0f) && !moving) {
                    //initialize values for sensor event
                    float[] value = sensorEvent.values;
                    float x = value[0];
                    float y = value[1];
                    float z = value[2];
                    //check the current accelerometer val relative to earths gravity
                    relativeGravity = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
                    //user is moving if rg more than 1.1 or less than 0.9
//                    Log.d("RelativeGravity", "" + relativeGravity);
                    if (relativeGravity >= 1.1 || relativeGravity <= 0.8) {
                        //movement is happening
//                        Log.d("Moving", "Some movement is present" + relativeGravity);
                        moving = true;
                    } else {
                        //user is not moving
//                        Log.d("Not moving", "No movement" + relativeGravity);
                    }

                } else {
                    stopService();
                }
            }
            if (type == android.hardware.Sensor.TYPE_LIGHT) {
                sleepData.setLight(sensorEvent.values[0]);
            }
        }
    }

    private void stopService() {
        mSensMan.unregisterListener(this, accelerometer);
        mSensMan.unregisterListener(this, light);
        stopSelf();

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void screenState() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean screenOn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            screenOn = pm.isInteractive();
        } else {
            screenOn = pm.isScreenOn();
        }
        if (screenOn) {
            sleepData.setScreenState(true);
        } else {
            sleepData.setScreenState(false);
        }
    }


    @Override
    public void onDestroy() {
        if (sleepDataList == null)
            sleepDataList = new ArrayList<>();
        sleepData.setTime(System.currentTimeMillis());
        sleepData.setAccelerometer(relativeGravity);
        Gson gsonq = new Gson();
        String json12 = gsonq.toJson(sleepData);
        Log.d("finaly_data", json12);
        sleepDataList.add(sleepData);
        Gson gson = new Gson();
        String json = gson.toJson(sleepDataList);
        preference.edit().putString(MainActivity.DATA_LIST, json).apply();
        super.onDestroy();
        Log.v("AccelService", "AccelService stopped");
    }


}
