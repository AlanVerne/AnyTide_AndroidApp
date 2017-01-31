package com.avaa.balitidewidget.data;

import android.graphics.Path;
import android.util.Log;

import com.avaa.balitidewidget.Common;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;


/**
 * Created by Alan on 17 Oct 2016.
 */


public class TideData {
    private static final String TAG = "TideData";

    public final TimeZone timeZone;

    public long  fetched;
    public long  fetchedSuccessfully;

    public final String preciseStr;
    public final String extremumsStr;

    private final SortedMap<Long, int[]> precise = new TreeMap<>();
    public final SortedMap<Long, Integer> extremums = new TreeMap<>();

    public  int   min, max;

    private int   hasDaysStartingFrom = -1;
    private int   hasDaysN = 0;


    public boolean equals(TideData o) {
        return o != null && precise.equals(o.precise) && extremums.equals(o.extremums);
    }


    public TideData(Long fetched) {
        this.timeZone = null;

        this.fetched = fetched;
        this.fetchedSuccessfully = 0L;

        this.preciseStr = null;
        this.extremumsStr = null;
    }
    public TideData(TimeZone timeZone, String preciseStr, String extremumsStr) {
        this(timeZone, preciseStr, extremumsStr, 0l, 0l);
    }
    public TideData(TimeZone timeZone, final String preciseStr, final String extremumsStr, Long fetched, Long fetchedSuccessfully) {
        this.timeZone = timeZone;

        this.fetched = fetched;
        this.fetchedSuccessfully = fetchedSuccessfully;

        this.preciseStr = preciseStr;
        this.extremumsStr = extremumsStr;

        if (preciseStr != null) {
            String[] daily = preciseStr.split("\n");

            if (daily.length % 2 == 1) {
                Log.i(TAG, "new TideData() | " + "failed to parce preciseStr");
                return;
            }

            for (int i = 0; i < daily.length; i += 2) {
                String[] strValues = daily[i + 1].split(" ");
                if (strValues.length < 24 * 6 + 1) continue;
                int[] values = new int[strValues.length];
                for (int j = 0; j < strValues.length; j++) {
                    values[j] = Integer.valueOf(strValues[j]);
                    min = Math.min(min, values[j]);
                    max = Math.max(max, values[j]);
                }
                this.precise.put(Long.valueOf(daily[i]), values);
            }
        }
        if (extremumsStr != null) {
            String[] split = extremumsStr.split("\n");

            if (split.length % 2 == 1) {
                Log.i(TAG, "new TideData() | " + "failed to parce extremumsStr");
                return;
            }

            for (int i = 0; i < split.length; i += 2) {
                this.extremums.put(Long.valueOf(split[i]), Integer.valueOf(split[i+1]));
            }
        }
    }


    public boolean isEmpty() {
        return precise.isEmpty() || extremums.isEmpty() || timeZone == null;
    }


    public int hasDays() { // 1 - only today, 7 - 7 days
        if (timeZone == null || isEmpty()) return 0;

        Calendar calendar = Common.getCalendarToday(timeZone);
        int day = calendar.get(Calendar.DAY_OF_YEAR);

        if (hasDaysStartingFrom == day) {
            Log.i(TAG, "hasDays() cached: startfrom = " + hasDaysStartingFrom + ", days = " + hasDaysN);
            return hasDaysN;
        }

        hasDaysStartingFrom = day;
        hasDaysN = 0;
        while (hasData(Common.getUnixTimeFromCalendar(calendar))) {
            calendar.add(Calendar.DATE, 1);
            hasDaysN++;
        }

        Log.i(TAG, "hasDays() calculated: startfrom = " + hasDaysStartingFrom + ", days = " + hasDaysN);

        return hasDaysN;
    }


    public boolean needUpdate() {
        return hasDays() < 7;
    }
    public boolean needAndCanUpdate(int needDays) {
        long currentTimeMillis = System.currentTimeMillis();
        return (fetched == 0 || fetched + 60*1000 < currentTimeMillis) &&
               (fetchedSuccessfully == 0 || fetchedSuccessfully + 60*60*1000 < currentTimeMillis) &&
               hasDays() < needDays;
    }
    public boolean needAndCanUpdate() {
        long currentTimeMillis = System.currentTimeMillis();

//        Log.i("TideData", "needAndCanUpdate: " + "hasDays " + hasDays() + ", " +
//                precise.lastKey() + " < " + (currentTimeMillis/1000 + TideDataProvider.ONE_DAY*5/1000) + " = " +
//                (precise.isEmpty() || precise.lastKey() < currentTimeMillis/1000 + TideDataProvider.ONE_DAY*5/1000) + ", " +
//                (fetched == 0 || fetched + 60*1000 < currentTimeMillis) + ", " +
//                (fetchedSuccessfully == 0 || fetchedSuccessfully + 60*60*1000 < currentTimeMillis));

        return (fetched == 0 || fetched + 60*1000 < currentTimeMillis) &&
               (fetchedSuccessfully == 0 || fetchedSuccessfully + 60*60*1000 < currentTimeMillis) &&
               needUpdate();
    }


    public SortedMap<Integer, Integer> getHourly(long day, int start, int end) {
        int[] values = precise.get(day);
        if (values == null) return null;

        SortedMap<Integer, Integer> r = new TreeMap<>();
        for (int h = start; h <= end; h++) {
            r.put(h, Math.round((float)values[h*6] / 10.0f));
        }
        return r;
    }


    public Integer getTide(long time) {
        float now = time / 60;

        int[] values = null;
        for (Map.Entry<Long, int[]> entry : precise.entrySet()) {
            if (time >= entry.getKey()) {
                values = entry.getValue();
                now = (time - entry.getKey()) / 60f;
            }
        }

        if (values == null) return null;
        if (now > 24*60) return null;

        now /= 10;

        int i1 = (int)Math.floor(now);
        int i2 = (int)Math.ceil(now);
        //Log.i(TAG, i1 + " " + i2 + " | " + values.length);
//        if (true) return null;
        if (i2 >= values.length) return values[i1];
        int h1 = values[i1];
        int h2 = values[i2];

        int h = Math.round((h1 + (h2-h1) * (now-i1)) / 10.0f);

        return h;
    }
    public Integer getNow(long day) {
        int[] values = precise.get(day);
        if (values == null || timeZone == null) return null;

        Calendar calendar = GregorianCalendar.getInstance(timeZone);
        float now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) + calendar.get(Calendar.SECOND) / 60.0f;
        now /= 10;

        int i1 = (int)Math.floor(now);
        int i2 = (int)Math.ceil(now);
        int h1 = values[i1];
        int h2 = values[i2];

        int h = Math.round((h1 + (h2-h1) * (now-i1)) / 10.0f);

        return h;
    }


//    public String preciseToString() {
//        return preciseStr;
////        if (precise == null || precise.isEmpty()) return null;
////        String r = "";
////        for (Map.Entry<Long, int[]> e : precise.entrySet()) {
////            r += e.getKey() + "\n";
////            for (int i : e.getValue()) {
////                r += i + " ";
////            }
////            r = r.substring(0, r.length()-1) + "\n";
////        }
////        r = r.substring(0, r.length()-1);
////        return r;
//    }
//
//
//    public String extremumsToString() {
//        return extremumsStr;
////        if (extremums == null || extremums.isEmpty()) return null;
////        String r = "";
////        for (Map.Entry<Long, Integer> e : extremums.entrySet()) {
////            r += e.getKey() + "\n" + e.getValue() + "\n";
////        }
////        r = r.substring(0, r.length()-1);
////        return r;
//    }


    public Map<Integer, Integer> getExtremums(long day) {
        Map<Integer, Integer> result = new TreeMap<>();
        for (Map.Entry<Long, Integer> entry : extremums.subMap(day, day + 60L * 60 * 24).entrySet()) {
            Integer tide = getTide(entry.getKey());
            if (tide == null) tide = entry.getValue() / 10;
            else tide = Math.round(tide/10f)*10;
            result.put((int)((entry.getKey() - day) / 60), tide);
        }
        return result;
    }


    public boolean
    hasData(long day) {
        return precise.containsKey(day);
    }


    public int[] getPrecise(long day) {
        return precise.get(day);
    }


    @Override
    public String toString() {
        return preciseStr;
    }
}
