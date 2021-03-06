package com.shubhankar.sleeptracker;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
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

            //obtained stored data points
            ArrayList<SleepData> sleepData = gson.fromJson(data, listType);

            //remove data older than x days
            int x = 4;
            sleepData = removeOldData(sleepData, new DateTime().minusDays(x).getMillis());
            int rawCount = sleepData.size();

            //group data points
            ArrayList<SleepState> finalData = groupData(sleepData);
            Log.d("Size after", "preliminary grouping " + finalData.size());

            //group dataSets removing interrupts
            ArrayList<SleepState> cumulativeData = finalData;

            //cleaning of data as per required amount of smoothing
            for (int i = 1; i <= 1; i++) {
                cumulativeData = cleanData(cumulativeData, 2 * i, i);
                Log.d("Size after", i + " cleanups = " + cumulativeData.size());
            }

            //remove non sleep entries that are below a threshold
            cumulativeData = removeNonSleep(cumulativeData, 3 * 60);
            if (rawCount > x * 360) {
                Toast.makeText(getApplicationContext(), "You slept well for " + cumulativeData.size() + " days", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "We did not get good data for past " + x + " days", Toast.LENGTH_LONG).show();
            }
            Adapter adapter = new Adapter(cumulativeData, this);
            listView.setAdapter(adapter);
        }

    }

    private ArrayList<SleepData> removeOldData(ArrayList<SleepData> sleepData, long threshold) {
        ArrayList<SleepData> latest = new ArrayList<>();
        for (SleepData entry : sleepData) {
            if (entry.getTime() >= threshold) {
                latest.add(entry);
            }
        }
        return latest;
    }

    private ArrayList<SleepState> removeNonSleep(ArrayList<SleepState> cumulativeData, int thresholdInMin) {
        Iterator<SleepState> iterator = cumulativeData.iterator();
        while (iterator.hasNext()) {
            SleepState temp = iterator.next();
            if (!temp.isSleeping() || temp.getDuration() <= thresholdInMin) {
                iterator.remove();
            }
        }
        return cumulativeData;
    }

    private ArrayList<SleepState> groupData(ArrayList<SleepData> sleepData) {
        Log.d("Size of raw points", "" + sleepData.size());

        ArrayList<SleepState> finalData = new ArrayList<>();
        Boolean sleeping = null;
        long start = 0;

        ArrayList<Float> accelerometerReadings = new ArrayList<>();
        ArrayList<Float> lightReadings = new ArrayList<>();
        ArrayList<Boolean> screenStates = new ArrayList<>();

        for (SleepData currData : sleepData) {

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
        int size = sleepData.size();
        if (size > 0) {
            SleepState temp = new SleepState();

            temp.setIsSleeping(sleeping);
            temp.setStartTime(start);
            temp.setEndTime(sleepData.get(size > 0 ? (size - 1) : size).getTime());
            temp.setScreenStates(screenStates);
            temp.setAccelerometerReadings(accelerometerReadings);
            temp.setLightReadings(lightReadings);
            finalData.add(temp);
        }
        return finalData;
    }

    private ArrayList<SleepState> cleanData(ArrayList<SleepState> originalData, float trimInterval, int iteration) {
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
//        Gson gson1 = new Gson();
//        Log.d("Cleanup round", iteration + " interrupts found = " + interrupts.size() /*+
//                ", interrupts = " + gson1.toJson(interrupts).toString()*/);
        return processedData;
    }

    private void startMonitoringAccelerometer() {
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
        hasStoragePermission();
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

        String imageName = "captured@" + new DateTime().toString("YYYY-MM-dd hh:mm:ss") + ".jpeg";
        File imageFile = new File(dir, imageName);

        try {
            FileOutputStream fOut = new FileOutputStream(imageFile);

            viewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            Toast.makeText(getApplicationContext(), "Image saved to " + imageName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {

            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Could not save image :(", Toast.LENGTH_LONG).show();
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
    }

    private boolean hasStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("Write storagepermission", "Not granted, Requesting now");
                requestPermissions(new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_CONTACTS}, 69);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 69:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "You can now save graphs to storage", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Need storage permission to save graphs to storage", Toast.LENGTH_LONG).show();
                }
        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
