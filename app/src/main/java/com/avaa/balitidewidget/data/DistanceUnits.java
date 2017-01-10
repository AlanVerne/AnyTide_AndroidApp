package com.avaa.balitidewidget.data;

import java.util.Locale;

/**
 * Created by Alan on 10 Jan 2017.
 */

public class DistanceUnits {
    private static final String US = "US";
    private static final String LR = "LR";
    private static final String MM = "MM";

    private static final String MIL = "mil";
    private static final String KM  = "km";

    private static final float  MILES_IN_KM = 0.621371f;

    private static boolean imperial = false; // 0 - km, 1 - miles
    private static String units = KM;


    public static float fix(float inMeters) {
        return imperial ? inMeters * MILES_IN_KM : inMeters;
    }

    public static String getUnit() {
        return units;
    }


    public static void init() {
        String countryCode = Locale.getDefault().getCountry();
        if (US.equals(countryCode) || LR.equals(countryCode) || MM.equals(countryCode)) {
            imperial = true;
            units = MIL;
        }
    }
}
