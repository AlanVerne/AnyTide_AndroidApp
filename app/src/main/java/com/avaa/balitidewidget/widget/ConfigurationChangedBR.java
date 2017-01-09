package com.avaa.balitidewidget.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * Created by Alan on 24 Jun 2016.
 */

public class ConfigurationChangedBR extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.i("BTW", "ConfigurationChangedBR onReceive " + this.toString());
        Intent intentUpdateAllWidgets = new Intent(context, TideWidget.class);
        intentUpdateAllWidgets.setAction(TideWidget.ACTION_UPDATE_ALL);
        context.sendBroadcast(intentUpdateAllWidgets);
    }
}
