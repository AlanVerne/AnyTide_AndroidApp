package com.avaa.balitidewidget.widget;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by Alan on 24 Jun 2016.
 */

public class ConfigurationChangedListener extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.i("BTW", "ConfigurationChangedListener onStartCommand");
//        registerReceiver(new ConfigurationChangedBR(), new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
//        return super.onStartCommand(intent, flags, startId);
//    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Log.i("BTW", "ConfigurationChangedListener onConfigurationChanged");
        Intent intentUpdateAllWidgets = new Intent(this, TideWidget.class);
        intentUpdateAllWidgets.setAction(TideWidget.ACTION_UPDATE_ALL);
        sendBroadcast(intentUpdateAllWidgets);
    }
}
