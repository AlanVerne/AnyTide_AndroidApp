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
    private static final String TAG = "TideWidget";

    public static final String  ACTION_UPDATE_ALL       = "com.avaa.balitidewidget.action.updateall";
    public static final String  ACTION_WIDGET_CLICKED   = "com.avaa.balitidewidget.action.widgetclicked";
    public static final String  EXTRA_WIDGET_ID         = "com.avaa.balitidewidget.extra.widgetid";

    public static final String  SPKEY_LAST_CLICK_TIME   = "LastClickTime";
    public static final String  SPKEY_SHOW_HOURLY       = "ShowHourly";

    public static final String  SPKEY_PORT_ID       = "PortID";
    public static final String  SPKEY_PORT_NAME     = "PortName";
    public static final String  SPKEY_PORT_POSITION = "PortPosition";
    public static final String  SPKEY_PORT_TIMEZONE = "PortTimezone";

    public static final String  SPKEY_CROP          = "Crop";
    public static final String  SPKEY_SHOW_NAME     = "ShowName";

    public static final int     DOUBLE_CLICK_MILLIS = 400;

    private SharedPreferences   sharedPreferences   = null;
    private TideChartDrawer     tideChartDrawer     = null;
    private TideDataProvider    tideDataProvider    = null;
    private Ports               ports = null;


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIDs) {
        updateWidgets(context, appWidgetManager, appWidgetIDs);
    }
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetID, Bundle newOptions) {
        updateWidget(context, appWidgetManager, appWidgetID, AppWidgetSizeUtils.getSizeInPixels(newOptions));
    }


    public void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIDs) {
        for (int appWidgetID : appWidgetIDs) {
            Port port = getWidgetPort(appWidgetID);

            if (port == null) return;

            TideData tideData = tideDataProvider.get(port, 3, new WidgetsUpdater(context));
            Point widgetSize = AppWidgetSizeUtils.getSizeInPixels(appWidgetManager, appWidgetID);

            updateWidget(context, appWidgetManager, widgetSize, appWidgetID,
                         port, tideData, showTomorrow(port), sharedPreferences.getBoolean(SPKEY_SHOW_HOURLY + appWidgetID, false));
        }
    }


    public void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetID, Point size) {
        updateWidget(context, appWidgetManager, appWidgetID, size, sharedPreferences.getBoolean(SPKEY_SHOW_HOURLY + appWidgetID, false));
    }
    public void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetID, boolean hourly) {
        updateWidget(context, appWidgetManager, appWidgetID, AppWidgetSizeUtils.getSizeInPixels(appWidgetManager, appWidgetID), hourly);
    }
    public void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetID, Point size, boolean hourly) {
        Port port = getWidgetPort(appWidgetID);

        if (port == null) return;

        TideData tideData = tideDataProvider.get(port, 3, new WidgetsUpdater(context));

        updateWidget(context, appWidgetManager, size, appWidgetID,
                     port, tideData, showTomorrow(port), hourly);
    }


    private void updateWidget(Context context, AppWidgetManager appWidgetManager, Point widgetSize, int appWidgetID,
                              Port port, TideData tideData, boolean showTomorrow, boolean hourly) {
        if (widgetSize.x <= 0 || widgetSize.y <= 0) return;

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bali_tide_widget);

        tideChartDrawer.set24h(!crop(appWidgetID));
        tideChartDrawer.name = showName(appWidgetID) ? port.getName() : null;
        Bitmap bitmap = tideChartDrawer.draw(widgetSize.x, widgetSize.y, tideData, showTomorrow ? 1 : 0, showTomorrow ? 1 : 0, hourly, port); //ports.get(getWidgetPortID(appWidgetID))

        Log.i(TAG, "updateWidget() | " + bitmap.getWidth());

        Intent intent = new Intent(context, getClass());
        intent.setAction(ACTION_WIDGET_CLICKED);
        intent.putExtra(EXTRA_WIDGET_ID, appWidgetID);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetID, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.imageView, pendingIntent);

        remoteViews.setImageViewBitmap(R.id.imageView, bitmap);

        appWidgetManager.updateAppWidget(appWidgetID, remoteViews);
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
        b.putString(EXTRA_WIDGET_ID, getWidgetPortID(widgetID));
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


    private String getWidgetPortID(int appWidgetID) {
        return sharedPreferences.getString(SPKEY_PORT_ID + appWidgetID, Common.BENOA_PORT_ID);
    }
    private Port getWidgetPort(int appWidgetID) {
        String stringAppWidgetID = String.valueOf(appWidgetID);
        String portID = sharedPreferences.getString(SPKEY_PORT_ID + stringAppWidgetID, Common.BENOA_PORT_ID);
        String portName = sharedPreferences.getString(SPKEY_PORT_NAME + stringAppWidgetID, null);
        String portPosition = sharedPreferences.getString(SPKEY_PORT_POSITION + stringAppWidgetID, null);
        String portTimeZoneID = sharedPreferences.getString(SPKEY_PORT_TIMEZONE + stringAppWidgetID, null);

        Log.i(TAG, "getWidgetPort("+appWidgetID+") | " + portID + " " + portName + " " + portPosition + " " + portTimeZoneID);
        if (portTimeZoneID == null || portPosition == null) return getPorts().get(portID); // backward compatibility

        return new Port(portID, portName, portPosition, portTimeZoneID);
    }
    private boolean crop(int appWidgetID) {
        return sharedPreferences.getBoolean(SPKEY_CROP + appWidgetID, false);
    }
    private boolean showName(int appWidgetID) {
        return sharedPreferences.getBoolean(SPKEY_SHOW_NAME + appWidgetID, false);
    }


    private boolean showTomorrow(Port port) {
        Calendar c = new GregorianCalendar(port.timeZone);
        return c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE) > SunTimesProvider.get(port.position, port.timeZone).sunset;
    }


    //


    private void checkVariables(Context context) {
        if (tideChartDrawer == null) {
            tideChartDrawer = new TideChartDrawer(context.getResources().getDisplayMetrics().density);
            tideChartDrawer.context = context;
        }
        if (sharedPreferences == null) sharedPreferences = context.getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (tideDataProvider == null) tideDataProvider = TideDataProvider.getInstance(sharedPreferences);
    }
    private Ports getPorts() { // backward compatibility
        if (ports == null) ports = new Ports(null);
        return ports;
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

