package com.avaa.balitidewidget;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by Alan on 19 Jun 2016.
 */

public class Common {
    public static final String SHARED_PREFERENCES_NAME = "com.avaa.balitidewidget";

    public static final String BENOA_PORT_ID = "5382";


    public static long getToday(TimeZone timeZone) {
        return getDay(0, timeZone);
    }
    public static long getDay(int plusDays, TimeZone timeZone) {
        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, plusDays);
        return calendar.getTime().getTime() / 1000;
    }


    public static Calendar getCalendarToday(TimeZone timeZone) {
        Calendar calendar = new GregorianCalendar(timeZone);
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        //calendar.add(Calendar.DATE, plusDays);
        return calendar;
    }


    public static long getUnixTimeFromCalendar(Calendar calendar) {
        return calendar.getTime().getTime() / 1000;
    }
}
