package com.avaa.balitidewidget.data;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

    private final SortedMap<String, TideData> portIDToTideData = new TreeMap<>();
    private final Map<String, FetchTideDataAsyncTask> asyncTasks = new HashMap<>();

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


    public void fetch(@NonNull Port port) {
        fetch(port, null);
    }
    public void fetch(@NonNull Port port, @Nullable final Runnable widgetsRunnable) {
        Log.i(TAG, "Fetch " + port);

        TideData tideData = portIDToTideData.get(port.id);
        if (tideData != null) {
            Log.i(TAG, "Current tidedata " + tideData.toString());
            tideData.fetched = System.currentTimeMillis();
        }
        else {
            Log.i(TAG, "Current tidedata null");
            portIDToTideData.put(port.id, new TideData(System.currentTimeMillis()));
        }

        FetchTideDataAsyncTask asyncTask = asyncTasks.get(port.id);
        if (asyncTask == null || asyncTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            fireLoadingStateChanged(port.id, true);
            asyncTask = new FetchTideDataAsyncTask(getInstance(), port, widgetsRunnable);
            asyncTasks.put(port.id, asyncTask);
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    public boolean loadingInProgress(String portID) {
        FetchTideDataAsyncTask asyncTask = asyncTasks.get(portID);
        return asyncTask != null && !asyncTask.getStatus().equals(AsyncTask.Status.FINISHED);
    }


    private void fireLoadingStateChanged(String portID, boolean loading) {
        for (TideDataProviderListener listener : listeners) {
            listener.loadingStateChanged(portID, loading);
        }
    }
    private void fireUpdated(String portID) {
        for (TideDataProviderListener listener : listeners) {
            listener.updated(portID);
        }
    }


    private TideDataProvider(Ports ports, SharedPreferences sharedPreferences) {
        this.ports = ports;
        this.sharedPreferences = sharedPreferences;
        loadTides();
    }


    public TideData getTideData(Port port, int needDays, Runnable afterFetch) {
        TideData tideData = portIDToTideData.get(port.id);
        if (tideData == null || tideData.needAndCanUpdate(needDays)) fetch(port, afterFetch);
        if (tideData != null && tideData.isEmpty()) tideData = null;
        return tideData;
    }
    public TideData getTideData(Port port) {
        TideData tideData = portIDToTideData.get(port.id);
        if (tideData == null || tideData.needAndCanUpdate()) fetch(port);
        if (tideData != null && tideData.isEmpty()) tideData = null;
        return tideData;
    }


    private static class FetchTideDataAsyncTask extends AsyncTask<String, Void, TideData> {
        private final Runnable runnable;
        private final TideDataProvider tideDataProvider;
        private final Port port;

        public FetchTideDataAsyncTask(TideDataProvider tideDataProvider, Port port, Runnable runAfter) {
            Log.i(TAG, "new FetchTideDataAsyncTask for " + port);

            this.tideDataProvider = tideDataProvider;
            this.port = port;
            this.runnable = runAfter;
        }

        protected TideData doInBackground(String... addr) {
            URL url;
            BufferedReader reader = null;

            try {
                url = new URL("http://128.199.252.5/ports/" + port.id + "/predictions");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");

                connection.setReadTimeout(15 * 1000);
                connection.connect();

                InputStream is = connection.getInputStream();

                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }

                String[] split = result.toString("UTF-8").split("\n--\n");

//                Log.i("BTW", split.length+" ");
//                Log.i("BTW", split[0]);
//                Log.i("BTW", split[1]);

                long currentTimeMillis = System.currentTimeMillis();
                return new TideData(port.getTimeZone(), split[0], split[1], currentTimeMillis, currentTimeMillis);
            } catch (Exception e) {
                Log.i(TAG, "Fetch failed");
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
            return null;
        }

        protected void onPostExecute(TideData tideData) {
            Log.i(TAG, "onPostExecute for " + port + ", " + (tideData == null ? "null" : "hasDays=" + tideData.hasDays()));

            tideDataProvider.fireLoadingStateChanged(port.id, false);

            if (tideData == null) return;
            if (tideData.equals(tideDataProvider.portIDToTideData.get(port.id))) return;

            tideDataProvider.portIDToTideData.put(port.id, tideData);
            tideDataProvider.saveTides();

            tideDataProvider.fireUpdated(port.id);

            if (runnable != null) runnable.run();
        }
    }


    private void loadTides() {
        Set<String> portIDs = sharedPreferences.getStringSet(SPKEY_SAVED_PORT_IDS, null);
        if (portIDs == null) return;

        for (String portID : portIDs) {
            portIDToTideData.put(portID, new TideData(ports.get(portID).getTimeZone(), sharedPreferences.getString(SPKEY_TIDE_DATA_PRECISE + portID, null), sharedPreferences.getString(SPKEY_TIDE_DATA_EXTREMUMS + portID, null)));
        }

        //Log.i(TAG, portIDToTideData.toString());
    }


    private void saveTides() {
        SharedPreferences.Editor edit = sharedPreferences.edit();

        Map<String, TideData> toSave = new HashMap<>();
        for (Map.Entry<String, TideData> entry : portIDToTideData.entrySet()) {
            if (!entry.getValue().isEmpty()) toSave.put(entry.getKey(), entry.getValue());
        }

        edit.putStringSet(SPKEY_SAVED_PORT_IDS, toSave.keySet());

        for (Map.Entry<String, TideData> entry : toSave.entrySet()) {
            edit.putString(SPKEY_TIDE_DATA_PRECISE  + entry.getKey(),  entry.getValue().preciseToString());
            edit.putString(SPKEY_TIDE_DATA_EXTREMUMS + entry.getKey(), entry.getValue().extremumsToString());
        }

        edit.apply();
    }
}
