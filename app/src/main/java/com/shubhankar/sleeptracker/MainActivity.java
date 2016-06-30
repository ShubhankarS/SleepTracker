package com.shubhankar.sleeptracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements Adapter.ViewLongClickListener {

    public static final String DATA_LIST = "SLEEP_DATA";

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startMonitoringAccelerometer();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences.contains(DATA_LIST)) {
            List<SleepData> sleepData = new ArrayList<>();
            Gson gson = new Gson();
            String dataList = gson.toJson(sleepData);
            sharedPreferences.edit().putString(DATA_LIST, dataList).apply();
        } else {
            String data = sharedPreferences.getString(DATA_LIST, "nothing inside");

            ListView listView = (ListView) findViewById(R.id.listview);

            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<SleepData>>() {
            }.getType();

            //obtained stored datapoints
            ArrayList<SleepData> sleepDatas = gson.fromJson(data, listType);

            //group data points
            ArrayList<SleepState> finaldata = groupData(sleepDatas);
            Log.d("Size after", "preliminary grouping " + finaldata.size());

            //group datasets removing interrupts
            ArrayList<SleepState> cumulativeData = finaldata;

            for (int i = 1; i <= 6; i++) {
                cumulativeData = cleanData(cumulativeData, 2 * i, i);
                Log.d("Size after", i + " cleanups = " + cumulativeData.size());
            }

//            cumulativeData = removeNonSleep(cumulativeData, 5 * 60);
            Adapter adapter = new Adapter(cumulativeData, this);
            listView.setAdapter(adapter);
        }

    }

    private ArrayList<SleepState> removeNonSleep(ArrayList<SleepState> cumilativeData, int minSleep) {
        Iterator<SleepState> iterator = cumilativeData.iterator();
        while (iterator.hasNext()) {
            SleepState tempp = iterator.next();
            if (!tempp.isSleeping() || tempp.getDuration() <= minSleep) {
                iterator.remove();
            }
        }
        return cumilativeData;
    }

    private ArrayList<SleepState> groupData(ArrayList<SleepData> sleepDatas) {
        Log.d("Size of raw points", "" + sleepDatas.size());

        ArrayList<SleepState> finalData = new ArrayList<>();
        Boolean sleeping = null;
        long start = 0;

        ArrayList<Float> accelerometerReadings = new ArrayList<>();
        ArrayList<Float> lightReadings = new ArrayList<>();
        ArrayList<Boolean> screenStates = new ArrayList<>();

        for (SleepData currData : sleepDatas) {

            if (sleeping == null) {
                sleeping = currData.getCase();
            }
            if (sleeping == currData.getCase()) {
                if (start == 0) {
                    start = currData.getTime();
                }
                accelerometerReadings.add(currData.getAccelerometer());
                lightReadings.add(currData.getLight());
                screenStates.add(currData.getScreenState());
            } else {

                SleepState temp = new SleepState();
                temp.setEndTime(currData.getTime());
                temp.setIsSleeping(sleeping);
                temp.setStartTime(start);
                temp.setLightReadings(lightReadings);
                temp.setAccelerometerReadings(accelerometerReadings);
                temp.setScreenStates(screenStates);
                finalData.add(temp);

                start = currData.getTime();
                sleeping = currData.getCase();

                //start new ArrayLists
                accelerometerReadings = new ArrayList<>();
                lightReadings = new ArrayList<>();
                screenStates = new ArrayList<>();

                //Add first value to new ArrayLists
                accelerometerReadings.add(currData.getAccelerometer());
                lightReadings.add(currData.getLight());
                screenStates.add(currData.getScreenState());

            }
        }
        int size = sleepDatas.size();
        if (size > 0) {
            SleepState temp = new SleepState();

            temp.setIsSleeping(sleeping);
            temp.setStartTime(start);
            temp.setEndTime(sleepDatas.get(size > 0 ? (size - 1) : size).getTime());
            temp.setScreenStates(screenStates);
            temp.setAccelerometerReadings(accelerometerReadings);
            temp.setLightReadings(lightReadings);
            finalData.add(temp);
        }
        return finalData;
    }

    public ArrayList<SleepState> cleanData(ArrayList<SleepState> originalData, float trimInterval, int iteration) {
        Log.d("Size received", "for cleanup " + originalData.size());
        ArrayList<SleepState> processedData = new ArrayList<>();
        ArrayList<SleepState> interrupts = new ArrayList<>();
        for (int i = 0; i < originalData.size(); i++) {
            if (i == 0) {
                processedData.add(originalData.get(i));
            } else {
                int lastPos = processedData.size() - 1;
                if (originalData.get(i).isSleeping() == processedData.get(lastPos).isSleeping()) {
                    processedData.get(lastPos).setEndTime(originalData.get(i).getEndTime());
                    processedData.get(lastPos).addAccelerometerData(originalData.get(i).getAccelerometerReadings());
                    processedData.get(lastPos).addLightData(originalData.get(i).getLightReadings());
                    processedData.get(lastPos).addScreenData(originalData.get(i).getScreenStates());
                } else {
                    if (originalData.get(i).getDuration() > trimInterval) {
                        processedData.add(originalData.get(i));
                    } else {
                        interrupts.add(originalData.get(i));
                    }
                }
            }
        }
        Gson gson1 = new Gson();
        Log.d("Cleanup round", iteration + " interrupts found = " + interrupts.size() /*+
                ", interrupts = " + gson1.toJson(interrupts).toString()*/);
        return processedData;
    }

    public void startMonitoringAccelerometer() {
        Calendar cal = Calendar.getInstance();
        Intent intent = new Intent(this, AccelService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 3, intent, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        // Start service every 2 minutes
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 2 * 60 * 1000, pendingIntent);
    }


    @Override
    public void saveViewAsBitmap(View v) {
        Log.d("Long clicked view", "now");
        v.setDrawingCacheEnabled(true);
        v.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        v.buildDrawingCache(true);
        Bitmap viewBitmap = v.getDrawingCache();

        String file_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()
                + "/SleepGraphs";
        File dir = new File(file_path);
        if (!dir.exists()) {
            boolean mkdirs = dir.mkdirs();
        }

        String imageName = "graph" + new DateTime().toString("YYYY-MM-DD(") + new Random().nextInt(10) + ").jpeg";
        File imageFile = new File(dir, imageName);

        try {
            FileOutputStream fOut = new FileOutputStream(imageFile);

            viewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //inform that media is mounted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(imageFile);
            mediaScanIntent.setData(contentUri);
            getApplicationContext().sendBroadcast(mediaScanIntent);
        } else {
            sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://"
                            + Environment.getExternalStorageDirectory())));
        }

        v.setDrawingCacheEnabled(false);
        Toast.makeText(getApplicationContext(), "Image saved to " + imageName, Toast.LENGTH_LONG).show();

    }
}
