package com.avaa.balitidewidget.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;

/**
 * Created by Alan on 7 Jun 2016.
 */

public class AppWidgetSizeUtils {
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Point getSizeInDIP(AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        return getSizeInDIP(options);
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static Point getSizeInDIP(Bundle options) {
        return isPortrait() ?
                new Point(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                          options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)) :
                new Point(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                          options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
    }

    public static Point getSizeInPixels(AppWidgetManager appWidgetManager, int appWidgetId) {
        Point widgetSize = getSizeInDIP(appWidgetManager, appWidgetId);
        return dipToPixel(widgetSize);
    }
    public static Point getSizeInPixels(Bundle options) {
        Point widgetSize = getSizeInDIP(options);
        return dipToPixel(widgetSize);
    }


    public static void handleTouchWiz(Context context, Intent intent, AppWidgetProvider widget) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        int appWidgetId = intent.getIntExtra("widgetId", 0);
        int widgetSpanX = intent.getIntExtra("widgetspanx", 0);
        int widgetSpanY = intent.getIntExtra("widgetspany", 0);

        if(appWidgetId > 0 && widgetSpanX > 0 && widgetSpanY > 0) {
            Bundle newOptions = new Bundle();
            // We have to convert these numbers for future use
            newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widgetSpanY * 74);
            newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widgetSpanX * 74);
            newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widgetSpanY * 74);
            newOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widgetSpanX * 74);

            widget.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        }
    }


    private static boolean isPortrait() {
        return Resources.getSystem().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }
    private static Point dipToPixel(Point point) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return new Point((int)(point.x * density + 0.5f), (int)(point.y * density + 0.5f));
    }
}
