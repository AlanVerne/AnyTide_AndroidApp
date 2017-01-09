package com.avaa.balitidewidget.data;

import android.graphics.PointF;
import android.location.Location;
import android.text.TextUtils;

import com.avaa.balitidewidget.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.sql.Time;
import java.util.TimeZone;


/**
 * Created by Alan on 25 Oct 2016.
 */


public class Port {
    public String id = null;

    private String name = null;

    public final String[] nameWords;
    public final String[] altNames;
    public final String[] countyAndArea;
    public final LatLng position;
    public int min;
    public int max;
    public MarkerOptions markerOptions;
    public Marker marker = null;
    public boolean favorite = false;
    public int utc = +8;
    public float distance = -1;

//    private TideData tideData = null;


    public Port(String name, LatLng position) {
        this.name = name;
        this.nameWords = name.split(" ");
        this.position = position;
        this.altNames = null;
        this.countyAndArea = null;
    }
    public Port(String id, String name, String[] altNames, String[] countyAndArea, LatLng position) {
        this.id = id;
        this.name = name;
        this.nameWords = name.split(" ");
        this.altNames = altNames;
        this.countyAndArea = countyAndArea;
        this.position = position;
    }
    public Port(String name, String[] altNames, String[] countyAndArea, LatLng position, int utc) {
        this.name = name;
        this.nameWords = name.split(" ");
        this.altNames = altNames;
        this.countyAndArea = countyAndArea;
        this.position = position;
        this.utc = utc;
    }


//    public TideData getTideData() {
//        tideData = TideDataProvider.getInstance().getTideData();
//    }


    public String getName() {
        if (name == null) name = TextUtils.join(" ", nameWords);
        return name;
    }


    public String getSubname() {
        return countyAndArea == null ? "" : TextUtils.join(", ", countyAndArea);
    }


    public int check(String s) {
        for (String name : nameWords) if (name.startsWith(s)) return favorite ? 6 : 3;
        if (altNames != null) for (String name : altNames) if (name.startsWith(s)) return favorite ? 5 : 2;
        if (countyAndArea != null) for (String name : countyAndArea) if (name.startsWith(s)) return favorite ? 4 : 1;
        return 0;
    }


    public void updateDistance(Location location) {
        Location portLocation = new Location("");
        portLocation.setLatitude(this.position.latitude);
        portLocation.setLongitude(this.position.longitude);
        distance = location.distanceTo(portLocation);
    }


    public String getDistanceString() {
        return distance < 0 ? "" : (distance > 10000 ? String.valueOf((int)(distance/1000)) : String.valueOf((int)(distance/100) / 10f)) + "km";
    }


    public TimeZone getTimeZone() {
        return TimeZone.getTimeZone("GMT" + (utc >= 0 ? "+" : "-") + String.valueOf(utc));
    }


    @Override
    public String toString() {
        return getName() + " (" + id + ")";
    }
}
