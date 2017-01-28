package com.avaa.balitidewidget.data;

import android.content.SharedPreferences;
import android.location.Location;import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
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
    private static final String SPKEY_FAVORITE_PORTS = "FavoritePorts";
    private static final int SEARCH_RESULT_MAX_N = 50;

    private static final Comparator<Entry<String, Port>> portsComparatorByDistance = new Comparator<Entry<String, Port>>() {
        @Override
        public int compare(Entry<String, Port> lhs, Entry<String, Port> rhs) {
            return (int)(lhs.getValue().distance - rhs.getValue().distance);
        }
    };

    private static class PortEntry extends SimpleEntry<String, Port> {
        public PortEntry(Entry<? extends String, ? extends Port> copyFrom) {
            super(copyFrom);
        }
        @Override
        public String toString() {
            return getValue().getName() + "    " + getValue().getDistanceString();
        }
    }

    private boolean portsAreSortedByDistance = false;
    private final List<Entry<String, Port>> portsSortedByDistance;

    private Location myLocation = null;


    public Ports() {
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

        portsSortedByDistance = new ArrayList<>(entrySet());
    }


    private void put(Port p) {
        put(p.id, p);
    }


    public void load(SharedPreferences sp) {
        Set<String> favoritePorts = sp.getStringSet(SPKEY_FAVORITE_PORTS, null);
        if (favoritePorts == null) return;
        for (String favoritePort : favoritePorts) {
            Port port = get(favoritePort);
            if (port != null) port.favorite = true;
        }
    }
    public void save(SharedPreferences sp) {
        Set<String> favoritePorts = new HashSet<>();
        for (Entry<String, Port> portEntry : entrySet()) {
            if (portEntry.getValue().favorite) favoritePorts.add(portEntry.getKey());
        }
        sp.edit().putStringSet(SPKEY_FAVORITE_PORTS, favoritePorts).apply();
    }


    public boolean portsAreSortedByDistance() {
        return portsAreSortedByDistance;
    }

    public Entry<String, Port> searchNearestFavorite() {
        for (Entry<String, Port> entry : portsSortedByDistance) {
            if (entry.getValue().favorite) return entry;
        }
        return null;
    }
    public Entry<String, Port> searchNearestNotFavorite() {
        for (Entry<String, Port> entry : portsSortedByDistance) {
            if (!entry.getValue().favorite) return entry;
        }
        return null;
    }
    public List<Entry<String, Port>> search() {
        List<Entry<String, Port>> res = new ArrayList<>(SEARCH_RESULT_MAX_N);

        for (Entry<String, Port> entry : portsSortedByDistance) {
            if (entry.getValue().favorite) {
                res.add(new PortEntry(entry));
                if (res.size() > SEARCH_RESULT_MAX_N) return res;
            }
        }
        for (Entry<String, Port> entry : portsSortedByDistance) {
            if (!entry.getValue().favorite) {
                res.add(new PortEntry(entry));
                if (res.size() > SEARCH_RESULT_MAX_N) return res;
            }
        }

        return res;
    }
    public List<Entry<String, Port>> search(String s) {
        if (s == null || s.isEmpty()) return search();

        Map<Integer, List<Entry<String, Port>>> r = new HashMap<>();
        for (int i = 1; i <= 6; ++i) r.put(i, new ArrayList<Entry<String, Port>>());

        s = s.length() > 1 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() : s.toUpperCase();

        for (Entry<String, Port> entry : portsSortedByDistance) {
            int check = entry.getValue().check(s);
            if (check > 0) r.get(check).add(new PortEntry(entry));
        }

        List<Entry<String, Port>> lr = new ArrayList<>(SEARCH_RESULT_MAX_N);
        for (int i = 6; i > 0; --i) {
            List<Entry<String, Port>> entries = r.get(i);
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
