package com.avaa.balitidewidget.data;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Alan on 20 May 2016.
 */

public class TideDataProvider {
    private static final String TAG = "TideDataProv";
    private static final String SPKEY_TIDE_DATA_PRECISE = "TideDataPrecise";
    private static final String SPKEY_TIDE_DATA_EXTREMUMS = "TideDataExtremums";

    public static final Long ONE_DAY = 1000*60*60*24*1L;

    protected final SortedMap<String, TideData> portIDToTideData = new TreeMap<>();
    private final Map<String, TideDataRetriever> asyncTasks = new HashMap<>();

    private final SharedPreferences sharedPreferences;

    public interface TideDataProviderListener {
        void updated(String portID);
        void loadingStateChanged(String portID, boolean loading);
    }
    private final List<TideDataProviderListener> listeners = new ArrayList<>();
    public void addListener(TideDataProviderListener l) { listeners.add(l); }


    public static TideDataProvider instance = null;
    public static TideDataProvider getInstance() {
        return instance;
    }
    public static TideDataProvider getInstance(SharedPreferences sharedPreferences) {
        if (instance == null) instance = new TideDataProvider(sharedPreferences);
        return instance;
    }


    public TideData get(Port port, int needDays, Runnable afterFetch) {
        TideData tideData = portIDToTideData.get(port.id);
        if (tideData == null && !portIDToTideData.containsKey(port.id)) tideData = load(port);
        if (tideData == null || tideData.needAndCanUpdate(needDays)) fetch(port, afterFetch);
        if (tideData != null && tideData.isEmpty()) tideData = null;
        return tideData;
    }
    public TideData get(Port port) {
        TideData tideData = portIDToTideData.get(port.id);
        if (tideData == null && !portIDToTideData.containsKey(port.id)) tideData = load(port);
        if (tideData == null || tideData.needAndCanUpdate()) fetch(port);
        if (tideData != null && tideData.isEmpty()) tideData = null;
        return tideData;
    }


    public void fetch(@NonNull Port port) {
        fetch(port, null);
    }
    public void fetch(@NonNull Port port, @Nullable final Runnable widgetsRunnable) {
//        Log.i(TAG, "fetch() | " + port);

        TideData tideData = portIDToTideData.get(port.id);
        if (tideData != null) {
            tideData.fetched = System.currentTimeMillis();
        }
        else {
//            Log.i(TAG, "fetch() | current tidedata null");
            portIDToTideData.put(port.id, new TideData(port.timeZone, System.currentTimeMillis()));
        }

        TideDataRetriever asyncTask = asyncTasks.get(port.id);
        if (asyncTask == null || asyncTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            fireLoadingStateChanged(port.id, true);
            asyncTask = new TideDataRetriever(getInstance(), port, widgetsRunnable);
            asyncTasks.put(port.id, asyncTask);
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    public boolean loadingInProgress(String portID) {
        TideDataRetriever asyncTask = asyncTasks.get(portID);
        return asyncTask != null && !asyncTask.getStatus().equals(AsyncTask.Status.FINISHED);
    }


    // --


    private TideDataProvider(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }


    void fireLoadingStateChanged(String portID, boolean loading) {
        for (TideDataProviderListener listener : listeners) {
            listener.loadingStateChanged(portID, loading);
        }
    }
    private void fireUpdated(String portID) {
        for (TideDataProviderListener listener : listeners) {
            listener.updated(portID);
        }
    }


    void newDataFetched(String portID, TideData tideData) {
        if (tideData.equals(portIDToTideData.get(portID))) return;

        portIDToTideData.put(portID, tideData);
        save(portID);

        fireUpdated(portID);
    }


    // --


    private TideData load(Port port) {
        TideData tideData = null;

        String portID = port.id;

        String precise = sharedPreferences.getString(SPKEY_TIDE_DATA_PRECISE + portID, null);
        String extremums = sharedPreferences.getString(SPKEY_TIDE_DATA_EXTREMUMS + portID, null);

        if (precise != null && extremums != null) {
            tideData = new TideData(port.timeZone, precise, extremums);
            if (tideData.hasDays() <= 0) tideData = null;
        }

        portIDToTideData.put(portID, tideData);

        return tideData;
    }


    private void save(String id) {
        SharedPreferences.Editor edit = sharedPreferences.edit();

        TideData tideData = portIDToTideData.get(id);
        edit.putString(SPKEY_TIDE_DATA_PRECISE   + id, tideData.preciseStr);
        edit.putString(SPKEY_TIDE_DATA_EXTREMUMS + id, tideData.extremumsStr);

        edit.apply();
    }
}
