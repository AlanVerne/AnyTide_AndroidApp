package com.avaa.balitidewidget.data;

import com.avaa.balitidewidget.Common;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Alan on 5 Nov 2016.
 */

public class SunTimesProviderByWiki {
    public static class SunTimes {
        public long sunrise, sunset;

        public SunTimes(long sunrise, long sunset) {
            this.sunrise = sunrise/60;
            this.sunset = sunset/60;
        }

        @Override
        public String toString() {
            return sunrise/60 + ":" + sunrise%60 + " " + sunset/60 + ":" + sunset%60;
        }
    }

    public static SunTimes get(LatLng position, int gmt) {
        double n = getJulianFromUnix((new Date().getTime())/1000) - 2451545 + 0.0008;
        double jStar = n - position.longitude/360;
        double m = 357.5291 + 0.98560028 * jStar; // mod 360
        double mRad = m * Math.PI/180; // mod 360
        double c = 1.9148 * Math.sin(mRad) + 0.02*Math.sin(2*mRad) + 0.0003*Math.sin(3*mRad);
        double lambdaRad = (m+c+180+102.9372) * Math.PI/180; // mod 360
        double jTransit = 2451545.5 + jStar + 0.0053*Math.sin(m) - 0.0069*Math.sin(2*lambdaRad);
        double sinDelta = Math.sin(lambdaRad) + Math.sin(23.44*Math.PI/180);
        double cosDelta = Math.cos(Math.asin(sinDelta));
        double cosOmega0 = (Math.sin(-0.83 * Math.PI/180) - Math.sin(position.latitude * Math.PI/180) * sinDelta) / (Math.cos(position.latitude * Math.PI/180) * cosDelta);

        double omega0 = Math.acos(cosOmega0)/Math.PI/2;

        long day = Common.getDay(0, TimeZone.getDefault());
        return new SunTimes(julianToUnix(jTransit - omega0) - day, julianToUnix(jTransit + omega0) - day);
    }

    private static double getJulianFromUnix(long unixSecs) {
        return (int)(unixSecs / 86400.0f) + 2440587.5f;
    }
    private static long julianToUnix(double unixSecs) {
        return (long)((unixSecs - 2440587.5) * 86400.0);
    }
}
