package com.avaa.balitidewidget.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Alan on 25 Jun 2016.
 */

public class MyPackageReplacedBR extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName name = new ComponentName(context.getApplicationContext(), TideWidget.class);
        int [] ids = AppWidgetManager.getInstance(context.getApplicationContext()).getAppWidgetIds(name);
        Log.i("BTW", "MyPackageReplacedBR " + ids.length);
        if (ids.length > 0) context.getApplicationContext().startService(new Intent(context.getApplicationContext(), ConfigurationChangedListener.class));
    }
}
