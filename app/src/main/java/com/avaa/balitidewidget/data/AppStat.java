package com.avaa.balitidewidget.data;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import com.avaa.balitidewidget.R;

/**
 * Created by Alan on 9 Mar 2017.
 */

public class AppStat {
    private final static int DAYS_UNTIL_PROMPT = 14;        //Min number of days
    private final static int DAYS_REMIND_LATER = 3;         //Min number of days
    private final static int LAUNCHES_UNTIL_PROMPT = 50;    //Min number of launches

    public static void appLaunched(Context mContext) {
        if (mContext == null) return;
        SharedPreferences sp = mContext.getSharedPreferences("apprater", 0);
        if (sp == null) return;
        if (sp.getBoolean("dontshowagain", false)) { return; }

        SharedPreferences.Editor editor = sp.edit();

        long launch_count = sp.getLong("launch_count", 0) + 1;      // Increment launch counter
        editor.putLong("launch_count", launch_count);

        Long date_firstLaunch = sp.getLong("date_firstlaunch", 0);  // Get date of first launch
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }

        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {                // Wait at least n days before opening
            if (System.currentTimeMillis() >= date_firstLaunch + DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000) {
                showRateDialog(mContext, editor);
            }
        }

        editor.commit();
    }

    public static void showRateDialog(final Context context, final SharedPreferences.Editor editor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.rate_title)
                .setMessage(R.string.rate_msg)
                .setPositiveButton(R.string.rate_rate, (dialog, which) -> {
                    editor.putBoolean("dontshowagain", true).commit();
                    Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    try {
                        context.startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
                    }
                })
                .setNeutralButton(R.string.rate_no, (dialogInterface, i) -> editor.putBoolean("dontshowagain", true).commit())
                .setNegativeButton(R.string.rate_later, (dialogInterface, i) -> {
                    Long date_firstLaunch = System.currentTimeMillis() - (DAYS_UNTIL_PROMPT - DAYS_REMIND_LATER) * 24 * 60 * 60 * 1000;
                    editor.putLong("date_firstlaunch", date_firstLaunch).commit();
                });
        AlertDialog dialog1 = builder.create();
        dialog1.show();
        dialog1.setOnCancelListener(dialogInterface -> {
                Long date_firstLaunch = System.currentTimeMillis() - (DAYS_UNTIL_PROMPT - DAYS_REMIND_LATER/2) * 24 * 60 * 60 * 1000;
                editor.putLong("date_firstlaunch", date_firstLaunch).commit();
        });
        dialog1.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0x55000000);
        dialog1.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(0x55000000);
    }
}
