package com.avaa.balitidewidget.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.util.SparseArray;

import com.avaa.balitidewidget.R;
import com.google.android.gms.maps.model.LatLng;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by Alan on 25 Oct 2016.
 */

public class Ports extends LinkedHashMap<String, Port> {
    private static final String TAG = "Ports";

    private static final String SPKEY_FAVORITE_PORTS = "FavoritePorts";
    private static final int    SEARCH_RESULT_MAX_N = 50;

    private static final Comparator<Port> portsComparatorByDistance = new Comparator<Port>() {
        @Override
        public int compare(Port lhs, Port rhs) {
            return (int)(lhs.distance - rhs.distance);
        }
    };

    private boolean portsAreSortedByDistance = false;
    private final List<Port> portsSortedByDistance;

    private Location myLocation = null;


    private void loadPorts(Context context) {
        String prevCountry = null;
        Map<String, TimeZone> timeZones = new HashMap<>(10);

        if (context != null) {
            CsvParserSettings csvParserSettings = new CsvParserSettings();
            csvParserSettings.setHeaderExtractionEnabled(false);
            csvParserSettings.getFormat().setLineSeparator("\n");
            CsvParser parser = new CsvParser(csvParserSettings);

            parser.beginParsing(context.getResources().openRawResource(R.raw.ports_indo_and_time_zones));

            String[] row;
            while ((row = parser.parseNext()) != null) {
                //if (row.length < 9) continue;
                if (row[4] != null && !row[4].isEmpty() && row[5] != null && !row[5].isEmpty()) {
                    LatLng latLng = new LatLng(Double.valueOf(row[4]), Double.valueOf(row[5]));

                    String country = row[7];
                    if (country.equals(prevCountry)) country = prevCountry;
                    else prevCountry = country;

                    String strTimeZone = row[9];
                    TimeZone timeZone = timeZones.get(strTimeZone);
                    if (timeZone == null) {
                        timeZone = TimeZone.getTimeZone(strTimeZone);
                        timeZones.put(strTimeZone, timeZone);
                    }

                    put(new Port(row[0], row[1], null, new String[]{row[2], country}, latLng, timeZone));
                }
            }

            parser.stopParsing();
        }
    }


    private void loadPorts() { // for backward compatibility
        String[] portugal = {"Portugal"};
        put(new Port("1740", "Cascais", null, portugal, new LatLng(38.691915, -9.419067), TimeZone.getTimeZone("Portugal").getOffset(System.currentTimeMillis())));

        String indonesia = "Indonesia";

        String[] indonesiaJava = {"Java", indonesia};
        put(new Port("5358", "Banyuwangi", null, indonesiaJava, new LatLng(-8.128243, 114.399974), +7));
        put(new Port("5359", "Pulau Tabuan", null, indonesiaJava, new LatLng(-8.037211, 114.461060), +7));
        put(new Port("5360", "Gosong Karangmas", null, indonesiaJava, new LatLng(-7.676389, 114.433333), +7));

        String[] indonesiaBali = {"Bali", indonesia};
        put(new Port("5382", "Benoa", new String[]{"Denpasar", "Old", "Batu"}, indonesiaBali, new LatLng(-8.746247, 115.211678), +8));
        put(new Port("5379", "Buleleng", new String[]{"Lowina", "Singaraja"}, indonesiaBali, new LatLng(-8.164220, 115.019817), +8));
        put(new Port("5381", "Sanur", new String[]{"Denpasar"}, indonesiaBali, new LatLng(-8.691562, 115.266637), +8));
        put(new Port("5379A", "Labuan Amuk", new String[]{"Labuhan"}, indonesiaBali, new LatLng(-8.519105, 115.507468), +8));

        String[] indonesiaLombok = {"Lombok", indonesia};
        put(new Port("5386", "Tanjung Pandanan", null, indonesiaLombok, new LatLng(-8.726043, 115.858193), +8));
        put(new Port("5385", "Teluk Labuhantereng", null, indonesiaLombok, new LatLng(-8.742473, 116.054388), +8));
        put(new Port("5384", "Ampenan", null, indonesiaLombok, new LatLng(-8.565419, 116.072186), +8));

        String[] indonesiaSumbawa = {"Sumbawa", indonesia};
        put(new Port("5395", "Bima", null, indonesiaSumbawa, new LatLng(-8.447375, 118.712837), +8));
        put(new Port("5397", "Teluk Waworada", null, indonesiaSumbawa, new LatLng(-8.706588, 118.800877), +8));
        put(new Port("5396", "Teluk Sape", null, indonesiaSumbawa, new LatLng(-8.571800, 119.014635), +8));
        put(new Port("5399", "Teluk Slawi", null, indonesiaSumbawa, new LatLng(-8.601699, 119.517401), +8));
    }


    public Ports(Context context) {
        if (context != null) loadPorts(context);
        else loadPorts();

        portsSortedByDistance = new ArrayList<>(values());
    }


    private void put(Port p) {
        if (!containsKey(p.id)) put(p.id, p);
    }


    public void loadFromSP(SharedPreferences sp) {
        Set<String> favoritePorts = sp.getStringSet(SPKEY_FAVORITE_PORTS, null);
        if (favoritePorts == null) return;
        for (String favoritePort : favoritePorts) {
            Port port = get(favoritePort);
            if (port != null) port.favorite = true;
        }
    }
    public void saveToSP(SharedPreferences sp) {
        Set<String> favoritePorts = new HashSet<>();
        for (Entry<String, Port> portEntry : this.entrySet()) {
            if (portEntry.getValue().favorite) favoritePorts.add(portEntry.getKey());
        }
        sp.edit().putStringSet(SPKEY_FAVORITE_PORTS, favoritePorts).apply();
    }


    public boolean isPortsSortedByDistance() {
        return portsAreSortedByDistance;
    }


    public Port getNearest(Location location) {
        float minD = Float.MAX_VALUE;
        Port minDPort = null;
        for (Port port : values()) {
            float d = port.getDistance(location);
            if (minD > d) {
                minD = d;
                minDPort = port;
            }
        }
        return minDPort;
    }


    public Port searchNearestFavorite() {
        for (Port entry : portsSortedByDistance) {
            if (entry.favorite) return entry;
        }
        return null;
    }
    public Port searchNearestNotFavorite() {
        for (Port entry : portsSortedByDistance) {
            if (!entry.favorite) return entry;
        }
        return null;
    }
    public List<Port> search() {
        List<Port> res = new ArrayList<>(SEARCH_RESULT_MAX_N);

        for (Port entry : portsSortedByDistance) {
            if (entry.favorite) {
                res.add(entry);
                if (res.size() > SEARCH_RESULT_MAX_N) return res;
            }
        }
        for (Port entry : portsSortedByDistance) {
            if (!entry.favorite) {
                res.add(entry);
                if (res.size() > SEARCH_RESULT_MAX_N) return res;
            }
        }

        return res;
    }
    public List<Port> search(String s) {
        if (s == null || s.isEmpty()) return search();

        if (s.indexOf(' ') != -1 || s.indexOf(',') != -1 || s.indexOf(';') != -1) {
            String[] split = s.split("[ ,;]");
            return search(split);
        }

        Map<Integer, List<Port>> r = new HashMap<>();
        for (int i = 1; i <= 6; ++i) r.put(i, new ArrayList<Port>());

        s = s.length() > 1 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() : s.toUpperCase();

        for (Port entry : portsSortedByDistance) {
            int check = entry.check(s);
            if (check > 0) r.get(check).add(entry);
        }

        List<Port> lr = new ArrayList<>(SEARCH_RESULT_MAX_N);
        for (int i = 6; i > 0; --i) {
            List<Port> entries = r.get(i);
            if (lr.size() + entries.size() > SEARCH_RESULT_MAX_N) {
                for (int j = 0; j < entries.size(); j++) {
                    lr.add(entries.get(j));
                }
            }
            else lr.addAll(entries);
        }
        return lr;
    }
    public List<Port> search(final String[] s) {
        if (s == null) return search();

//        Log.i(TAG, "search(l="+s.length+", s[0]="+s[0]+")");

        for (int i = 0; i < s.length; i++) {
            s[i] = s[i].length() > 1 ? s[i].substring(0, 1).toUpperCase() + s[i].substring(1).toLowerCase() : s[i].toUpperCase();
        }

//        Map<Integer, List<Port>> r = new HashMap<>();
//        for (int j = 0; j < s.length; j++) for (int i = 1; i <= 6; ++i) r.put(i+5*(j+1), new ArrayList<Port>());
        int ss = (s.length)*11+1;
        ArrayList<Port>[] r = (ArrayList<Port>[])new ArrayList[ss];
        for (int i = 0; i < ss; i++) {
            r[i] = new ArrayList<>();
        }

        int check = 0, c;
        for (Port port : portsSortedByDistance) {
            for (String si : s) {
                c = port.check(si);
                if (c > 0) check += 5 + c;
                else {
                    check = 0;
                    break;
                }
            }
            if (check > 0) {
                //if (!r.containsKey(check)) r.put(check, new ArrayList<Port>());
                r[check].add(port);
                //Log.i(TAG, "    " + check + " - " + port.getName());
                check = 0;
            }
        }

        List<Port> lr = new ArrayList<>(SEARCH_RESULT_MAX_N);
        for (int k = s.length; k > 0; --k)
        for (int i = 6; i > 0; --i) {
            List<Port> entries = r[i+k*5];
            if (entries == null) continue;
            if (lr.size() + entries.size() > SEARCH_RESULT_MAX_N) {
                for (int j = 0; j < entries.size(); j++) {
                    lr.add(entries.get(j));
                }
            }
            else lr.addAll(entries);
        }
        return lr;
    }


    public void setMyLocation(Location location) {
        if (location == null) return;
        if (myLocation == null || myLocation.distanceTo(location) > 100) {
            myLocation = location;
            for (Port port : values()) {
                port.updateDistance(myLocation);
            }
            Collections.sort(portsSortedByDistance, portsComparatorByDistance);
            portsAreSortedByDistance = true;
        }
    }


//    public List<PortEntry> getInBounds(LatLngBounds bounds) {
//        List<PortEntry> r = new ArrayList<>();
//        for (Entry<String, Port> entry : entrySet()) {
//            if (bounds.contains(entry.getValue().position)) {
//                r.add(new PortEntry(entry));
//            }
//        }
//        return r;
//    }


//    public List<Entry<String, Port>> getAllFavorite() {
//        List<Entry<String, Port>> r = new ArrayList<>();
//        for (Entry<String, Port> entry : entrySet()) {
//            if (entry.getValue().favorite) {
//                r.add(entry);
//            }
//        }
//        return r;
//    }
}
