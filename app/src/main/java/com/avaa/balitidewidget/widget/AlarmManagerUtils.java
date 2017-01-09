package com.avaa.balitidewidget.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Alan on 20 Jun 2016.
 */

public class AlarmManagerUtils {
    public static void scheduleUpdate(Context context) {
        String interval = "1";

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        long intervalMillis = 5*60*1000;

        PendingIntent pi = getAlarmIntent(context);
        am.cancel(pi);
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), intervalMillis, pi);
    }

    private static PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, TideWidget.class);
        intent.setAction(TideWidget.ACTION_UPDATE_ALL);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        return pi;
    }

    public static void clearUpdate(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getAlarmIntent(context));
    }
}
