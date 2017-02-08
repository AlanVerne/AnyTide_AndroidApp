package com.avaa.balitidewidget.data;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Alan on 10 Jan 2017.
 */

public class TideDataRetriever extends AsyncTask<String, Void, TideData> {
    private String TAG = "TideDataRetriever";

    private final Runnable runnable;
    private final TideDataProvider tideDataProvider;
    private final Port port;

    TideDataRetriever(TideDataProvider tideDataProvider, Port port, Runnable runAfter) {
//        Log.i(TAG, "new TideDataRetriever() | for " + port);

        this.tideDataProvider = tideDataProvider;
        this.port = port;
        this.runnable = runAfter;
    }

    protected TideData doInBackground(String... addr) {
//        Log.i(TAG, "doInBackground()");

        URL url;

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

            String[] split = result.toString("ASCII").split("\n--\n"); //"UTF-8").split("\n--\n");

            long currentTimeMillis = System.currentTimeMillis();
            return new TideData(port.timeZone, split[0].trim(), split[1].trim(), currentTimeMillis, currentTimeMillis);
        } catch (Exception e) {
//            Log.i(TAG, "doInBackground() | fetch failed");
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(TideData tideData) {
//        Log.i(TAG, "onPostExecute() | for " + port + ", " + (tideData == null ? "tideData = null" : "hasDays = " + tideData.hasDays()));

        tideDataProvider.fireLoadingStateChanged(port.id, false);

        if (tideData == null) return;

        tideDataProvider.newDataFetched(port.id, tideData);

        if (runnable != null) runnable.run();
    }
}
