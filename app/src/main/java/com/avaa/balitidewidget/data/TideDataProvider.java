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
    private static final String SPKEY_SAVED_PORT_IDS = "SavedPortIDs";
    private static final String SPKEY_TIDE_DATA_PRECISE = "TideDataPrecise";
    private static final String SPKEY_TIDE_DATA_EXTREMUMS = "TideDataExtremums";

    public static final Long ONE_DAY = 1000*60*60*24*1L;

    protected final SortedMap<String, TideData> portIDToTideData = new TreeMap<>();
    private final Map<String, TideDataRetriever> asyncTasks = new HashMap<>();

    private final Ports ports;
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
    public static TideDataProvider getInstance(Ports ports, SharedPreferences sharedPreferences) {
        if (instance == null) {
            instance = new TideDataProvider(ports, sharedPreferences);
        }
        return instance;
    }


    public TideData get(Port port, int needDays, Runnable afterFetch) {
        TideData tideData = portIDToTideData.get(port.id);
        if (tideData == null || tideData.needAndCanUpdate(needDays)) fetch(port, afterFetch);
        if (tideData != null && tideData.isEmpty()) tideData = null;
        return tideData;
    }
    public TideData get(Port port) {
        TideData tideData = portIDToTideData.get(port.id);
        if (tideData == null || tideData.needAndCanUpdate()) fetch(port);
        if (tideData != null && tideData.isEmpty()) tideData = null;
        return tideData;
    }


    public void fetch(@NonNull Port port) {
        fetch(port, null);
    }
    public void fetch(@NonNull Port port, @Nullable final Runnable widgetsRunnable) {
        Log.i(TAG, "fetch() | " + port);

        TideData tideData = portIDToTideData.get(port.id);
        if (tideData != null) {
            tideData.fetched = System.currentTimeMillis();
        }
        else {
            Log.i(TAG, "fetch() | current tidedata null");
            portIDToTideData.put(port.id, new TideData(System.currentTimeMillis()));
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


    private TideDataProvider(Ports ports, SharedPreferences sharedPreferences) {
        this.ports = ports;
        this.sharedPreferences = sharedPreferences;
        load();
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


    private void load() {
        Set<String> portIDs = sharedPreferences.getStringSet(SPKEY_SAVED_PORT_IDS, null);
        if (portIDs == null) return;

        for (String portID : portIDs) {
            String precise   = sharedPreferences.getString(SPKEY_TIDE_DATA_PRECISE   + portID, null);
            String extremums = sharedPreferences.getString(SPKEY_TIDE_DATA_EXTREMUMS + portID, null);
            if (precise != null && extremums != null) {
                TideData tideData = new TideData(ports.get(portID).getTimeZone(), precise, extremums);
                if (tideData.hasDays() > 0) portIDToTideData.put(portID, tideData);
            }
        }
    }


    private void save(String id) {
        SharedPreferences.Editor edit = sharedPreferences.edit();

        edit.putStringSet(SPKEY_SAVED_PORT_IDS, portIDToTideData.keySet());

        TideData tideData = portIDToTideData.get(id);
        edit.putString(SPKEY_TIDE_DATA_PRECISE   + id, tideData.preciseStr);
        edit.putString(SPKEY_TIDE_DATA_EXTREMUMS + id, tideData.extremumsStr);

        edit.apply();
    }


//    private void saveAll() {
//        SharedPreferences.Editor edit = sharedPreferences.edit();
//
//        Map<String, TideData> toSave = new HashMap<>();
//        for (Map.Entry<String, TideData> entry : portIDToTideData.entrySet()) {
//            if (!entry.getValue().isEmpty()) toSave.put(entry.getKey(), entry.getValue());
//        }
//
//        edit.putStringSet(SPKEY_SAVED_PORT_IDS, toSave.keySet());
//
//        for (Map.Entry<String, TideData> entry : toSave.entrySet()) {
//            edit.putString(SPKEY_TIDE_DATA_PRECISE  + entry.getKey(),  entry.getValue().preciseToString());
//            edit.putString(SPKEY_TIDE_DATA_EXTREMUMS + entry.getKey(), entry.getValue().extremumsToString());
//        }
//
//        edit.apply();
//    }
}
