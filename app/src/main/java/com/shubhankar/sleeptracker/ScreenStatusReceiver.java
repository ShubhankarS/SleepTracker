package com.shubhankar.sleeptracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Shubhankar on 24/02/16.
 */
public class ScreenStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Test", "Test");
        Intent intent1 = new Intent(context, AccelService.class);
        context.startService(intent1);
    }
}
