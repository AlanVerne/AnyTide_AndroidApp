package com.avaa.balitidewidget.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.avaa.balitidewidget.Common;
import com.avaa.balitidewidget.MainActivity;
import com.avaa.balitidewidget.R;
import com.avaa.balitidewidget.data.TideChartDrawer;
import com.avaa.balitidewidget.data.Port;
import com.avaa.balitidewidget.data.Ports;
import com.avaa.balitidewidget.data.SunTimesProvider;
import com.avaa.balitidewidget.data.TideData;
import com.avaa.balitidewidget.data.TideDataProvider;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TideWidget extends AppWidgetProvider {
    public static final String ACTION_UPDATE_ALL     = "com.avaa.balitidewidget.action.updateall";
    public static final String ACTION_WIDGET_CLICKED = "com.avaa.balitidewidget.action.widgetclicked";
    public static final String EXTRA_WIDGET_ID       = "com.avaa.balitidewidget.extra.widgetid";

    public static final String SPKEY_LAST_CLICK_TIME = "LastClickTime";
    public static final String SPKEY_SHOW_HOURLY     = "ShowHourly";
    public static final String SPKEY_PORT_ID         = "PortID";
    public static final String SPKEY_CROP            = "Crop";
    public static final String SPKEY_SHOW_NAME       = "ShowName";

    public static final int    DOUBLE_CLICK_MILLIS   = 400;

    private SharedPreferences sharedPreferences = null;
    private TideChartDrawer   tideChartDrawer = null;
    private TideDataProvider  tideDataProvider = null;
    private Ports ports = null;


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidgets(context, appWidgetManager, appWidgetIds);
    }
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateWidget(context, appWidgetManager, appWidgetId, AppWidgetSizeUtils.getSizeInPixels(newOptions));
    }


    public void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            String portID = portID(appWidgetId);
            Port port = ports.get(portID);

            TideData tideData = tideDataProvider.getTideData(port, 3, new WidgetsUpdater(context));
            Point widgetSize = AppWidgetSizeUtils.getSizeInPixels(appWidgetManager, appWidgetId);

            updateWidget(context, appWidgetManager, widgetSize, appWidgetId,
                         portID, port, tideData, showTomorrow(port), sharedPreferences.getBoolean(SPKEY_SHOW_HOURLY + appWidgetId, false));
        }
    }


    public void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Point size) {
        updateWidget(context, appWidgetManager, appWidgetId, size, sharedPreferences.getBoolean(SPKEY_SHOW_HOURLY + appWidgetId, false));
    }
    public void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean hourly) {
        updateWidget(context, appWidgetManager, appWidgetId, AppWidgetSizeUtils.getSizeInPixels(appWidgetManager, appWidgetId), hourly);
    }
    public void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Point size, boolean hourly) {
        String portID = portID(appWidgetId);
        Port port = ports.get(portID);

        TideData tideData = tideDataProvider.getTideData(port, 3, new WidgetsUpdater(context));

        updateWidget(context, appWidgetManager, size, appWidgetId,
                     portID, port, tideData, showTomorrow(port), hourly);
    }


    private void updateWidget(Context context, AppWidgetManager appWidgetManager, Point widgetSize, int appWidgetId,
                              String portID, Port port, TideData tideData, boolean showTomorrow, boolean hourly) {
        if (widgetSize.x <= 0 || widgetSize.y <= 0) return;

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bali_tide_widget);

        tideChartDrawer.set24h(!crop(appWidgetId));
        tideChartDrawer.name = showName(appWidgetId) ? port.getName() : null;
        Bitmap bitmap = tideChartDrawer.draw(widgetSize.x, widgetSize.y, tideData, showTomorrow?1:0, showTomorrow ? 1 : 0, hourly, ports.get(portID(appWidgetId)));

        Intent intent = new Intent(context, getClass());
        intent.setAction(ACTION_WIDGET_CLICKED);
        intent.putExtra(EXTRA_WIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.imageView, pendingIntent);

        remoteViews.setImageViewBitmap(R.id.imageView, bitmap);

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onReceive(final Context context, Intent intent) {
        checkVariables(context);

//        Log.i("BTW", "orn" + Resources.getSystem().getConfiguration().orientation);
//        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//        int rotation = display.getRotation();
//        int orientation = display.getOrientation();
//        Log.i("BTW", "rot" + rotation + "orie" + orientation);
//        Point s = new Point();
//        display.getSize(s);
//        Log.i("BTW", "s" + s + "wh" + display.getWidth() + display.getHeight());
//        display.getRealSize(s);
//        Log.i("BTW", "s" + s);
//        Point s2 = new Point();
//        display.getCurrentSizeRange(s, s2);
//        Log.i("BTW", "s" + s + "   " + s2);

        Log.i("BTW", "ON RECIEVE" + intent.getAction());

        if (ACTION_UPDATE_ALL.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), getClass().getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
        else if (ACTION_WIDGET_CLICKED.equals(intent.getAction())) {
            int id = intent.getIntExtra(EXTRA_WIDGET_ID, 0);
            boolean hourly = !sharedPreferences.getBoolean(SPKEY_SHOW_HOURLY + id, false);
            sharedPreferences.edit().putBoolean(SPKEY_SHOW_HOURLY + id, hourly).apply();
            updateWidget(context, AppWidgetManager.getInstance(context), id, hourly);

            long clickTime = sharedPreferences.getLong(SPKEY_LAST_CLICK_TIME + id, 0);
            long nowTime = new Date().getTime();

            if (nowTime - clickTime < DOUBLE_CLICK_MILLIS) {
                startMainActivity(context, id);
                sharedPreferences.edit().putLong(SPKEY_LAST_CLICK_TIME + id, nowTime).apply();
            }
            else sharedPreferences.edit().putLong(SPKEY_LAST_CLICK_TIME + id, nowTime).commit();
        }
        else super.onReceive(context, intent);
    }


    private void startMainActivity(Context context, int widgetID) {
        Intent intentStartActivity = new Intent(context, MainActivity.class);
        intentStartActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle b = new Bundle();
        b.putString(EXTRA_WIDGET_ID, portID(widgetID));
        intentStartActivity.putExtras(b);
        context.startActivity(intentStartActivity);
    }


    @Override
    public void onEnabled(Context context) {
        Log.i("BTW", "on enabled");
        AlarmManagerUtils.scheduleUpdate(context);
        context.getApplicationContext().startService(new Intent(context.getApplicationContext(), ConfigurationChangedListener.class));
    }
    @Override
    public void onDisabled(Context context) {
        Log.i("BTW", "on disabled");
        AlarmManagerUtils.clearUpdate(context);
        context.getApplicationContext().stopService(new Intent(context.getApplicationContext(), ConfigurationChangedListener.class));
    }


    //


    private String portID(int appWidgetId) {
        return sharedPreferences.getString(SPKEY_PORT_ID + appWidgetId, Common.BENOA_PORT_ID);
    }
    private boolean crop(int appWidgetId) {
        return sharedPreferences.getBoolean(SPKEY_CROP + appWidgetId, false);
    }
    private boolean showName(int appWidgetId) {
        return sharedPreferences.getBoolean(SPKEY_SHOW_NAME + appWidgetId, false);
    }
    private boolean showTomorrow(Port port) {
        Calendar c = new GregorianCalendar(port.getTimeZone());
        return c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE) > SunTimesProvider.get(port.position, port.getTimeZone()).sunset;
    }


    //


    private void checkVariables(Context context) {
        if (tideChartDrawer == null) {
            tideChartDrawer = new TideChartDrawer(context.getResources().getDisplayMetrics().density);
            tideChartDrawer.context = context;
        }
        if (sharedPreferences == null) sharedPreferences = context.getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (ports == null) ports = new Ports();
        if (tideDataProvider == null) tideDataProvider = TideDataProvider.getInstance(ports, sharedPreferences);
    }


    //


    public static class WidgetsUpdater implements Runnable {
        private final Context c;

        public WidgetsUpdater(Context c) {
            this.c = c;
        }

        public void run() {
            Intent intent = new Intent(c, TideWidget.class);
            intent.setAction(ACTION_UPDATE_ALL);
            c.sendBroadcast(intent);
        }
    }
}

