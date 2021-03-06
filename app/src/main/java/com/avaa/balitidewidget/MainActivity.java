package com.avaa.balitidewidget;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.avaa.balitidewidget.data.AppStat;
import com.avaa.balitidewidget.data.DistanceUnits;
import com.avaa.balitidewidget.data.TideChartDrawer;
import com.avaa.balitidewidget.views.ExtendedEditText;
import com.avaa.balitidewidget.views.ObservableScrollView;
import com.avaa.balitidewidget.views.PortsListViewAdapter;
import com.avaa.balitidewidget.views.TideLoadingIndicator;
import com.avaa.balitidewidget.data.Port;
import com.avaa.balitidewidget.data.Ports;
import com.avaa.balitidewidget.data.TideData;
import com.avaa.balitidewidget.data.TideDataProvider;
import com.avaa.balitidewidget.widget.ConfigurationChangedListener;
import com.avaa.balitidewidget.widget.TideWidget;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.avaa.balitidewidget.data.Port.getTimeZoneString;

public class MainActivity extends AppCompatActivity {
    public static final String SPKEY_24H = "24h";
    public static final String SPKEY_LAST_PORT_ID = "LastPortID";
    public static final String SPKEY_EVER_FAVORITED = "EverFavorited";

    private static final String TAG = "MainAct";
    private static final String D_MMMM_EEEE = "d MMMM, EEEE";
    private static final String SPACE = "    ";
    private static final int N_DAYS = 5;
    private static Ports PORTS = new Ports(null);
    private static final float ZOOM_IN = 10f;
    private static final float ZOOM_OUT = 8.5f;
    private static final int FL_CL_PERMISSIONS_REQUEST = 123;

    private final String yesterdayStr;
    private final String todayStr;
    private final String tomorrowStr;

    private TextView tvNoData;

    private final ImageView[] imageViews = new ImageView[N_DAYS];
    private final boolean[] imageViewsHourly = new boolean[N_DAYS];
    private final TextView[] textViews = new TextView[N_DAYS];

    private String selectedPortID = null;
    private Port selectedPort = null;

    private SharedPreferences sharedPreferences = null;
    private TideDataProvider tideDataProvider = null;
    private TideChartDrawer drawer = null;

    private GoogleApiClient client;

    private Timer timerOnceIn5Minutes;

    private int shownDay = -1;
    private TideData tideData = null;

    private MapView mvMap;
    private View vSpace;
    private LinearLayout llScroll;
    private ObservableScrollView svScroll;
    private RelativeLayout rlMap;
    private LinearLayout llPortHeader;
    private ExtendedEditText etSearch;
    private ListView lvSearchResults;
    private LinearLayout llSearchScreen;
    private TextView tvPortTitle;
    private TideLoadingIndicator tideLoadingIndicator;

    private GoogleMap googleMap;

    private float density;

    private PortsListViewAdapter lvSearchResultsAdapter = null;

    private final OnMapReadyCallback onMapReadyCallback = googleMap1 -> {
        MainActivity.this.googleMap = googleMap1;
        googleMap1.getUiSettings().setRotateGesturesEnabled(false);
        googleMap1.getUiSettings().setCompassEnabled(false);
        googleMap1.getUiSettings().setMyLocationButtonEnabled(false);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap1.setMyLocationEnabled(true);
        }

//                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
//                    googleMap.setMyLocationEnabled(true);

        for (Map.Entry<String, Port> port : PORTS.entrySet()) {
            if (port.getValue().favorite) showMarker(port.getValue(), port.getKey());
        }

        googleMap1.setOnMarkerClickListener(marker -> {
            setSelectedPort(marker.getSnippet());
            return true;
        });
        googleMap1.setOnCameraMoveListener(() -> {
            if (googleMap1.getCameraPosition().zoom > 7) {
                findViewById(R.id.flHintZoomIn).setVisibility(View.INVISIBLE);

                LatLngBounds bounds = googleMap1.getProjection().getVisibleRegion().latLngBounds;

                int max = 50;
                for (Map.Entry<String, Port> entry : PORTS.entrySet()) {
                    if (bounds.contains(entry.getValue().position) || entry.getValue().favorite) {
                        if (max > 0 || entry.getValue().favorite) {
                            max--;
                            showMarker(entry.getValue(), entry.getKey());
                        }
                        else hideMarker(entry.getValue());
                    } else {
                        hideMarker(entry.getValue());
                    }
                }
            }
            else {
                findViewById(R.id.flHintZoomIn).setVisibility(View.VISIBLE);

                for (Map.Entry<String, Port> entry : PORTS.entrySet()) {
                    if (!entry.getValue().favorite) hideMarker(entry.getValue());
                }
            }
        });

        Location myLocation = updateMyLocation();

        PORTS.setMyLocation(myLocation);

        initSelected();

        if (selectedPort != null) showMarker(selectedPort, selectedPortID);
    };

    public boolean allowScrollToZero = false;
    private final ObservableScrollView.ScrollViewListener svScrollSVListener = new ObservableScrollView.ScrollViewListener() {
        @Override
        public void scroll(int l, int kt, int oldl, int oldt) {
            int scrollY = svScroll.getScrollY();

            Log.i(TAG, "scroll " + scrollY + " " + svScroll.isDown());

            if (!svScroll.isDown() && scrollY <= 0) setSelectedPort(null); //setPortViewsVisible(false);

            float hk = Math.min(1f, (float)scrollY / (textViews[0].getHeight() + chartHeight));

            llPortHeader.setY(-llPortHeader.getHeight() + hk * (llPortHeader.getHeight() + rlMap.getY() + rlMap.getPaddingTop()));

            int maxY = (rlMap.getHeight() - scrollY) + (int)(hk*llPortHeader.getHeight());
            maxY /= 2;
            int y = maxY - rlMap.getHeight() / 2;
            mvMap.setY(y);
            //mvMap.setCameraDistance(ZOOM_OUT + hk*(ZOOM_IN-ZOOM_OUT))
            if (selectedPort != null && googleMap != null) googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPort.position, ZOOM_OUT + hk * (ZOOM_IN - ZOOM_OUT)));

            hk = Math.max(0, Math.min(1, (scrollY - density*80) / (density*40)));
            findViewById(R.id.btnShowInMapsApp).setAlpha(hk);
            findViewById(R.id.btnReportMistake).setAlpha(hk);

            if (svScroll.isDown()) {
                allowScrollToZero = scrollY <= textViews[0].getHeight() + chartHeight;
            }
            else if (!allowScrollToZero && scrollY < textViews[0].getHeight() + chartHeight && oldt > kt && !svScroll.isDown()) {
                svScroll.smoothScrollTo(0, textViews[0].getHeight() + chartHeight);
            }
        }

        @Override
        public void interactionFinished() {
            int scrollY = svScroll.getScrollY();

            Log.i(TAG, "interactionFinished " + scrollY + " " + svScroll.isDown());

            if (scrollY == 0) {
                setSelectedPort(null);
                return;
            }

            int dy = textViews[0].getHeight() + chartHeight;
            if (scrollY <= dy - density*80) {
                svScroll.smoothScrollTo(0, 0);
            }
            else if (scrollY <= dy) {
                svScroll.smoothScrollTo(0, dy); //vSpace.getHeight() + findViewById(R.id.tidesTopShadow).getHeight() - llPortHeader.getHeight());
            }
        }

        @Override
        public void interactionFinishedWithSwing(int v) {
            int scrollY = svScroll.getScrollY();
            Log.i(TAG, "interactionFinishedWithSwing " + scrollY + " " + svScroll.isDown());
            int dy = textViews[0].getHeight() + chartHeight;
            if (scrollY <= dy) {
                if (v > 0) {
                    if (scrollY == 0) setSelectedPort(null);
                    svScroll.smoothScrollTo(0, 0);
                }
                else svScroll.smoothScrollTo(0, dy); //vSpace.getHeight() + findViewById(R.id.tidesTopShadow).getHeight() - llPortHeader.getHeight());
            }
        }
    };


    public MainActivity() {
        yesterdayStr = DateUtils.getRelativeTimeSpanString(
                0, 1000 * 60 * 60 * 24,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString();
        todayStr = DateUtils.getRelativeTimeSpanString(
                0, 0,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString();
        tomorrowStr = DateUtils.getRelativeTimeSpanString(
                1000 * 60 * 60 * 24, 0,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString();
    }


    private void initVariablesAndProviders() {
        sharedPreferences = getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        PORTS = new Ports(getApplicationContext());
        PORTS.loadFromSP(sharedPreferences);

        DistanceUnits.init();

        density = getResources().getDisplayMetrics().density;
        drawer = new TideChartDrawer(density, false);

        tideDataProvider = TideDataProvider.getInstance(sharedPreferences);
        tideDataProvider.addListener(new TideDataProvider.TideDataProviderListener() {
            @Override
            public void updated(final String portID) {
                if (portID.equals(selectedPortID)) updateCharts();
            }
            @Override
            public void loadingStateChanged(final String portID, final boolean loading) {
                tideLoadingIndicator.post(() -> {
                    if (portID.equals(selectedPortID)) setTideLoadingInProgress(loading);
                });
            }
        });

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    private void initViewsVariables() {
        mvMap = (MapView) findViewById(R.id.mvMap);

        textViews[0] = (TextView) findViewById(R.id.textView1);
        textViews[1] = (TextView) findViewById(R.id.textView2);
        textViews[2] = (TextView) findViewById(R.id.textView3);
        textViews[3] = (TextView) findViewById(R.id.textView4);
        textViews[4] = (TextView) findViewById(R.id.textView5);

        imageViews[0] = (ImageView) findViewById(R.id.iv1);
        imageViews[1] = (ImageView) findViewById(R.id.iv2);
        imageViews[2] = (ImageView) findViewById(R.id.iv3);
        imageViews[3] = (ImageView) findViewById(R.id.iv4);
        imageViews[4] = (ImageView) findViewById(R.id.iv5);

        llPortHeader = (LinearLayout) findViewById(R.id.llPortHeader);
        vSpace = findViewById(R.id.space);
        llScroll = (LinearLayout) findViewById(R.id.llScroll);
        svScroll = (ObservableScrollView) findViewById(R.id.svScroll);
        rlMap = (RelativeLayout) findViewById(R.id.rlMap);
        llSearchScreen = (LinearLayout) findViewById(R.id.llSearchScreen);
        lvSearchResults = (ListView) findViewById(R.id.lvSearchResults);
        etSearch = (ExtendedEditText) findViewById(R.id.etSearch);
        tvPortTitle = (TextView) findViewById(R.id.tvTitle);
        tideLoadingIndicator = (TideLoadingIndicator) findViewById(R.id.tideLoadingIndicator);
        tvNoData = (TextView) findViewById(R.id.tvNoData);
    }


    private void initViews(Bundle savedInstanceState) {
        initMap(savedInstanceState);

        for (int i = 0; i < imageViewsHourly.length; i++) imageViewsHourly[i] = true;

        vSpace.setOnClickListener(v -> btnClosePortClick(null));
        svScroll.setScrollViewListener(svScrollSVListener);
        svScroll.addOnLayoutChangeListener((view, l,t,r,b, ol,ot,or,ob) -> {
            Log.i(TAG, "svScroll.addOnLayoutChangeListener()");

            if (r-l==or-ol && b-t==ob-ot) return;

            chartWidth = svScroll.getWidth();
            chartHeight = svScroll.getHeight();

//            Log.d("DISPLAY", chartWidth + " x " + chartHeight);

            if (chartWidth > chartHeight) chartHeight = chartHeight - findViewById(R.id.textView1).getHeight();
            else chartHeight = (int)Math.round(chartWidth * 0.75);

            tideLoadingIndicator.setLayoutParams(new LinearLayout.LayoutParams(svScroll.getWidth(), chartHeight));
            findViewById(R.id.tvNoData).setLayoutParams(new LinearLayout.LayoutParams(svScroll.getWidth(), chartHeight - (int)(80 * density)));

            imageViews[0].setLayoutParams(new LinearLayout.LayoutParams(svScroll.getWidth(), chartHeight));
            imageViews[1].setLayoutParams(new LinearLayout.LayoutParams(svScroll.getWidth(), chartHeight));

            vSpace.setLayoutParams(new LinearLayout.LayoutParams(svScroll.getWidth(), svScroll.getHeight())); // - (int) (density * 3)));

            updateAll();
        });

        initSearch();
    }


    private void initSearch() {
        lvSearchResultsAdapter = new PortsListViewAdapter(this, android.R.layout.simple_list_item_1);
        lvSearchResults.setAdapter(lvSearchResultsAdapter);
        lvSearchResults.setOnItemClickListener((parent, view, position, id) -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            etSearch.postDelayed(() -> {
                llSearchScreen.setVisibility(View.GONE);
                Port item = lvSearchResultsAdapter.getItem(position); // lvSearchResults.getItemAtPosition(position);
                setSelectedPort(item.id);
            }, 100);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                updateSearchResults(s.toString());
                findViewById(R.id.btnClearSearch).setVisibility(s.toString().isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { //Handle search key click
                updateSearchResults();
                return true;
            }
            return false;
        });
        etSearch.setBackKeyListener(() -> btnCloseSearchClick(null));
        etSearch.setOnFocusChangeListener((view, b) -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (b) {
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
            else {
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
        });
        etSearch.setOnClickListener(view -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        });
        lvSearchResults.setOnTouchListener((view, motionEvent) -> {
            lvSearchResults.requestFocus();
            return false;
        });

        ViewGroup parentGroup = (ViewGroup)lvSearchResults.getParent();

        TextView tvNoResults = new TextView(parentGroup.getContext());
        tvNoResults.setText(R.string.no_results);
        tvNoResults.setGravity(Gravity.CENTER);
        tvNoResults.setTextSize(14);
        tvNoResults.setTextColor(0x66000000);
        tvNoResults.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(140*density)));

        parentGroup.addView(tvNoResults);

        lvSearchResults.setEmptyView(tvNoResults);
    }


    private void initMap(Bundle savedInstanceState) {
        //mvMap.requestFocus();
        mvMap.onCreate(savedInstanceState);
        //mvMap.onResume();
        mvMap.getMapAsync(onMapReadyCallback);
        MapsInitializer.initialize(this.getApplicationContext());
    }


    private void initWidgets() {
        ComponentName name = new ComponentName(getApplicationContext(), TideWidget.class);
        int[] ids = AppWidgetManager.getInstance(getApplicationContext()).getAppWidgetIds(name);
        if (ids.length > 0) getApplicationContext().startService(new Intent(getApplicationContext(), ConfigurationChangedListener.class));
    }


    private void initSelected() {
        String portID = null;

        Intent intent = getIntent();
        Bundle extras = intent != null ? intent.getExtras() : null;
        if (extras != null) portID = extras.getString("portID", null);

        if (portID == null) portID = sharedPreferences.getString(SPKEY_LAST_PORT_ID, null);

        if (portID == null) {
            Location myLocation = updateMyLocation();
            if (myLocation != null) {
                Port nearestFavorite = PORTS.searchNearestFavorite();
                if (nearestFavorite != null && nearestFavorite.distance < 50000) portID = nearestFavorite.id;
                else {
                    Port nearestNotFavorite = PORTS.searchNearestNotFavorite();
                    if (nearestNotFavorite != null && nearestNotFavorite.distance < 25000) {
                        portID = nearestNotFavorite.id;
                        if (nearestFavorite == null) {
                            boolean b = sharedPreferences.getBoolean(SPKEY_EVER_FAVORITED, false);
                            if (!b) {
                                nearestNotFavorite.favorite = true;
                                sharedPreferences.edit().putBoolean(SPKEY_EVER_FAVORITED, true).apply();
                            }
                        }
                    }
                }
            }
        }

        if (portID != null) {
            setSelectedPort(portID);
        }
        else {
            setPortViewsVisible(false);
            btnShowMeClick(null);
        }
    }


    // --


    private void updateSearchResults() {
        updateSearchResults(etSearch.getText().toString());
    }
    private void updateSearchResults(String s) {
        lvSearchResultsAdapter.clear();
        lvSearchResultsAdapter.addAll(PORTS.search(s));
    }


    // overrides ----------


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FL_CL_PERMISSIONS_REQUEST) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                if (llSearchScreen.getVisibility() == View.VISIBLE) {
                    updateMyLocation();
                    updateSearchResults();
                }
                else {
                    initSelected();
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate1");
        initVariablesAndProviders();
        Log.i(TAG, "onCreate3");
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate4");
        initViewsVariables();
        Log.i(TAG, "onCreate5");
        initViews(savedInstanceState);
        Log.i(TAG, "onCreate6");
        initSelected();
        Log.i(TAG, "onCreate7");
        initWidgets();
        Log.i(TAG, "onCreate8");

//        vSpace.setOnGenericMotionListener(new View.OnGenericMotionListener() {
//            @Override
//            public boolean onGenericMotion(View v, MotionEvent event) {
//                return rlMap.dispatchGenericMotionEvent(event);
//            }
//        });
//        vSpace.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                event.setLocation(event.getX(), event.getY()-svScroll.getScrollY());
//                rlMap.dispatchTouchEvent(event);
//                v.getParent().requestDisallowInterceptTouchEvent(true);
//                return false;
//            }
//        });
//        svScroll.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                vSpace.getParent().requestDisallowInterceptTouchEvent(false);
//                return false;
//            }
//        });
    }


    @Override
    public void onBackPressed() {
        if (llSearchScreen.getVisibility() == View.VISIBLE) {
            btnCloseSearchClick(null);
        } else if (llPortHeader.getVisibility() == View.VISIBLE) {
            btnClosePortClick(null);
        } else {
            super.onBackPressed();
        }
    }


    private void setSelectedPort(String portID) {
        Log.i(TAG, "setSelectedPort(" + portID + ")");

        if (portID == selectedPortID) return;

        Port port = null;
        if (portID != null) {
            port = PORTS.get(portID);
            if (port == null) portID = null;
        }
        if (portID == null) {
            setPortViewsVisible(false);
        }

        sharedPreferences.edit().putString(SPKEY_LAST_PORT_ID, portID).apply();
        String oldSelectedPortID = selectedPortID;

        selectedPort = port;
        selectedPortID = portID;

        if (oldSelectedPortID != null) updateMarker(PORTS.get(oldSelectedPortID), oldSelectedPortID);
        if (portID != null) updateMarker(port, portID);

        rlMap.postDelayed(this::updateAll, 10);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    private ScheduledExecutorService scheduler;


    @Override
    public void onResume() {
        super.onResume();

        mvMap.onResume();

        rlMap.postDelayed(() -> AppStat.appLaunched(this), 1000);

        String portID = null;
        Intent intent = getIntent();
        Bundle extras = intent != null ? intent.getExtras() : null;
        if (extras != null) portID = extras.getString(TideWidget.EXTRA_WIDGET_ID, null);
        setIntent(null);

        if (portID != null) {
            setSelectedPort(portID);
            svScroll.smoothScrollBy(0, 0);
            svScroll.scrollTo(0, vSpace.getHeight() + findViewById(R.id.tidesTopShadow).getHeight() - llPortHeader.getHeight());
        }

        //Log.i(TAG, "onResume() " + getIntent().getAction() + portID);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
                    Calendar calendar = new GregorianCalendar();
                    int today = calendar.get(Calendar.DAY_OF_YEAR);
                    if (shownDay != today) updateAll();
                    else updateToday();
                }, 0, 5, TimeUnit.MINUTES);
    }


    @Override
    public void onPause() {
        super.onPause();

        mvMap.onPause();

        scheduler.shutdown();

        PORTS.saveToSP(sharedPreferences);
    }


    @Override
    public void onStart() {
        super.onStart();
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, "Bali tides", null,
                Uri.parse("android-app://com.avaa.balitidewidget/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }


    @Override
    public void onStop() {
        super.onStop();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, "Bali tide", null,
                Uri.parse("android-app://com.avaa.balitidewidget/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }


    // ui clicks ------------------


    public void btnUpdateClick(View view) {
//        Log.i(TAG, "btnUpdateClick");
        if (selectedPort != null) tideDataProvider.fetch(selectedPort);
    }

    public void ivClick(View view) {
        for (int i = 0; i < imageViews.length; i++) {
            if (view == imageViews[i]) {
                imageViewsHourly[i] = !imageViewsHourly[i];
                update(i);
                return;
            }
        }
    }

    public void btnClosePortClick(View view) {
        if (selectedPort == null) return;

//        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(selectedPort.position, ZOOM_OUT);
//        googleMap.animateCamera(cameraUpdate);

//        setSelectedPort(null);
        allowScrollToZero = true;
        svScroll.smoothScrollTo(0, 0);
    }

    public void initFirstLaunchWithMe(Location location) {
        btnShowMeClick(null);
    }

    public void btnShowInMapsAppClick(View view) {
        if (selectedPort != null) {
            Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + selectedPort.position.latitude + "," + selectedPort.position.longitude + "?q=" + selectedPort.position.latitude + "," + selectedPort.position.longitude + "(" + selectedPort.getName() + ")"));
            startActivity(geoIntent);
        }
    }

    private Location updateMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    FL_CL_PERMISSIONS_REQUEST);
        }

        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        if (provider == null) {
            List<String> providers = locationManager.getProviders(true);
            if (!providers.isEmpty()) provider = providers.get(0);
        }
        if (provider == null) return null;

        Location location = locationManager.getLastKnownLocation(provider);

        PORTS.setMyLocation(location);

        return location;
    }

    public void btnSearchClick(View view) {
        updateMyLocation();
        updateSearchResults();
        llSearchScreen.setVisibility(View.VISIBLE);
        etSearch.selectAll();
        llSearchScreen.postDelayed(() -> {
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        }, 0);
    }

    public void btnShowMeClick(View view) {
        Location myLocation = updateMyLocation();
        if (myLocation == null) return;

        if (googleMap == null) return;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), ZOOM_OUT);
        googleMap.animateCamera(cameraUpdate);
    }

    public void btnCloseSearchClick(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        findViewById(R.id.btnCloseSearch).postDelayed(() -> llSearchScreen.setVisibility(View.GONE), 100);
    }

    public void btnStarClick(View view) {
        if (selectedPort == null) return;
        selectedPort.favorite = !selectedPort.favorite;
        updateMarker(selectedPort, selectedPortID);
        updateStar();
    }


    // helpers ------


    private void updatePortTitle() {
        tvPortTitle.setText(selectedPort.getName());
    }
    private void updateStar() {
        ((ImageView)findViewById(R.id.btnOkImage)).setImageResource(selectedPort.favorite ? R.drawable.ic_star_white_24dp : R.drawable.ic_star_border_white_24dp);
    }
    private void updateDifferentTimezone() {
        TextView tvDifferentTimeZoneLabel = (TextView)findViewById(R.id.tvDifferentTimeZoneLabel);
        LinearLayout llDifferentTimeZoneLabel = (LinearLayout)findViewById(R.id.llDifferentTimeZoneLabel);
        if (selectedPort.utc*60*60*1000 != TimeZone.getDefault().getOffset(System.currentTimeMillis())) {
            tvDifferentTimeZoneLabel.setText(getString(R.string.different_time_zone) + " " + getTimeZoneString(selectedPort.utc)); // + " your: " + getTimeZoneString(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (60*60*1000)));
            llDifferentTimeZoneLabel.setVisibility(View.VISIBLE);
        }
        else {
            llDifferentTimeZoneLabel.setVisibility(View.GONE);
        }
    }


    int chartWidth = 0;
    int chartHeight = 0;
    private Point getImageSize() {
        return chartWidth == 0 ? null : new Point(chartWidth, chartHeight);
    }


    public void updateToday() {
        if (selectedPort == null) return;
        if (daysRange != null && daysRange[0] != 0) { update(0); return; }
        Point size = getImageSize();
        if (size == null) return;
        imageViews[0].setImageBitmap(drawer.draw(size.x, size.y, tideData, 0, 0, imageViewsHourly[0], selectedPort));
    }
    public void update(int i) {
        if (selectedPort == null) return;
        Point size = getImageSize();
        if (size == null) return;
        if (i > 1) size.y = (int)(size.y * 0.66);
        int day = daysRange == null ? i : i+daysRange[0];
        imageViews[i].setImageBitmap(drawer.draw(size.x, size.y, tideData, day, day == 0 ? 0 : -1, imageViewsHourly[i], selectedPort));
    }


    public void updateAll() {
        Log.i(TAG, "updateAll()");

        if (selectedPortID == null) {
            svScroll.smoothScrollTo(0, 0);
            return;
        }

        updateCharts();
//        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(selectedPort.position, ZOOM_IN);
//        googleMap.animateCamera(cameraUpdate);

        setTideLoadingInProgress(tideDataProvider.loadingInProgress(selectedPortID));
        updatePortTitle();
        updateStar();
        updateDifferentTimezone();
        updateDates();

        svScroll.post(() -> {
            setPortViewsVisible(true);
            svScroll.smoothScrollTo(0, vSpace.getHeight() + findViewById(R.id.tidesTopShadow).getHeight() - llPortHeader.getHeight());
            //svScroll.smoothScrollTo(0, textViews[0].getHeight() + chartHeight);
        });
    }


    private String formatDate(int day, Calendar calendar) {
        if (day == -1) return yesterdayStr + SPACE + DateFormat.format(D_MMMM_EEEE, calendar);
        if (day ==  0) return todayStr + SPACE + DateFormat.format(D_MMMM_EEEE, calendar);
        if (day ==  1) return tomorrowStr + SPACE + DateFormat.format(D_MMMM_EEEE, calendar);
        return DateFormat.format(D_MMMM_EEEE, calendar).toString();
    }
    private void updateDates() {
        Calendar calendar = selectedPort == null ? new GregorianCalendar() : new GregorianCalendar(selectedPort.timeZone);

        int today = calendar.get(Calendar.DAY_OF_YEAR);
        shownDay = today;

        Calendar calendarMy = new GregorianCalendar(TimeZone.getDefault());

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendarMy.set(Calendar.HOUR_OF_DAY, 0);

        int day = 0;

        if (daysRange != null) {
            for (int i = 0; i < daysRange[0]; i++) {
                calendar.add(Calendar.DATE, 1);
                calendarMy.add(Calendar.DATE, 1);
                day++;
                shownDay++;
            }
        }

        if (calendar.get(Calendar.DATE) != calendarMy.get(Calendar.DATE)) {
            if (calendar.getTimeInMillis() < calendarMy.getTimeInMillis()) {
                day--;
            } else {
                day++;
            }
        }

        textViews[0].setText(formatDate(day++, calendar));
        for (int i = 1; i < N_DAYS; i++) {
            calendar.add(Calendar.DATE, 1);
            textViews[i].setText(formatDate(day++, calendar));
        }
    }



    int[] daysRange = null;
    TideChartsAsyncDrawer aDrawer = null;
    private void updateCharts() {
        Log.i(TAG, "updateCharts()");

        tideData = tideDataProvider.get(selectedPort);

        Point size = getImageSize();
        if (size == null) return;

        //setTideLoadingInProgress(true);
        //updateDates();

        int i = 0;
        if (tideData != null && !tideData.isEmpty()) {
            daysRange = tideData.hasDaysExact();
            updateDates();
            if (daysRange != null) {
                tvNoData.setVisibility(View.GONE);

                textViews[0].setVisibility(View.VISIBLE);
                imageViews[0].setVisibility(View.VISIBLE);
                imageViews[0].setImageBitmap(drawer.draw(size.x, size.y, tideData, daysRange[0], daysRange[0] == 0 ? 0 : -1, imageViewsHourly[0], selectedPort));
                i = 1;

                for (int j = 1; j < Math.min(5, daysRange[1]-daysRange[0]); j++) {
                    textViews[j].setVisibility(View.VISIBLE);
                    imageViews[j].setVisibility(View.VISIBLE);
                    i = j+1;
                }

                if (aDrawer != null && aDrawer.getStatus() != AsyncTask.Status.FINISHED) aDrawer.cancel(true);
                TideChartDrawer d = new TideChartDrawer(density, false);
                aDrawer = new TideChartsAsyncDrawer(size.x, size.y, d, tideData);
                aDrawer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
        for (; i < N_DAYS; i++) {
            if (i > 0) textViews[i].setVisibility(View.GONE);
            imageViews[i].setVisibility(View.GONE);
        }
    }


    private void setTideLoadingInProgress(boolean b) {
        boolean hasTodayData = tideData != null && tideData.hasDays() > 0;

        tideLoadingIndicator.setVisibility(b ? View.VISIBLE : View.GONE);

        if (b) {
            tideLoadingIndicator.setLayoutParams(new LinearLayout.LayoutParams(llScroll.getWidth(), hasTodayData ? (int)(80*density) : chartHeight));
            tvNoData.setVisibility(View.GONE);
        }
        else {
            tvNoData.setVisibility(hasTodayData ? View.GONE : View.VISIBLE);
        }

        findViewById(R.id.btnUpdate).setVisibility(b ? View.GONE : View.VISIBLE);
    }


    private void setPortViewsVisible(boolean b) {
        svScroll.setVisibility(b ? View.VISIBLE : View.GONE);
        llPortHeader.setVisibility(b ? View.VISIBLE : View.GONE);
    }


    public void btnClearSearchClick(View view) {
        etSearch.setText("");
    }


    public void btnReportMistakeClick(View view) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + "alan.dominik.verne@gmail.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Mistake report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Port '" + selectedPort.getName() + "' (id:" + selectedPortID + ") has mistake.\n\nCorrect location is: ");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email report"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }


    public void btnZoomInClick(View view) {
        if (googleMap == null) return;

        LatLng target = googleMap.getCameraPosition().target;
        Location location = new Location("");
        location.setLatitude(target.latitude);
        location.setLongitude(target.longitude);
        Port nearestPort = PORTS.getNearest(location);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(nearestPort == null ? target : nearestPort.position, ZOOM_OUT);
        googleMap.animateCamera(cameraUpdate);
    }


    private class TideChartsAsyncDrawer extends AsyncTask<Void, Void, Map<Integer, Bitmap>> {
        int w, h;
        TideChartDrawer drawer;
        TideData tideData;

        public TideChartsAsyncDrawer(int w, int h, TideChartDrawer drawer, TideData tideData) {
//            Log.i(TAG, "TideChartsAsyncDrawer");
            this.w = w;
            this.h = h;
            this.drawer = new TideChartDrawer(drawer.density); // drawer;
            this.tideData = tideData;
        }

        @Override
        protected Map<Integer, Bitmap> doInBackground(Void... params) {
//            Log.i(TAG, "doInBackground");
            Map<Integer, Bitmap> map = new HashMap<>();
            int[] daysRange = tideData.hasDaysExact();
            for (int j = daysRange[0]+1; j < daysRange[1]; j++) {
                if (j == daysRange[0]+2) h = (int)(h*0.66);
                map.put(j-daysRange[0], drawer.draw(w, h, tideData, j, -1, true, selectedPort));
            }
            return map;
        }

        @Override
        protected void onPostExecute(Map<Integer, Bitmap> bitmaps) {
//            Log.i(TAG, "onPostExecute");
            for (Map.Entry<Integer, Bitmap> b : bitmaps.entrySet()) {
                Integer day = b.getKey();
                if (day >= N_DAYS) continue;
                textViews[day].setVisibility(View.VISIBLE);
                imageViews[day].setVisibility(View.VISIBLE);
                imageViews[day].setImageBitmap(b.getValue());
            }
            tvNoData.setVisibility(View.GONE);
        }
    }


    // markers


    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), id);
        Bitmap bm = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bm);
    }
    public MarkerOptions getMarkerOptions(Port port, String portID) {
        return new MarkerOptions().position(port.position).snippet(portID)
                .icon(getBitmapDescriptor(
                        portID.equals(selectedPortID) ?
                        R.drawable.ic_room_black_24dp :
                        port.favorite ? R.drawable.ic_grade_black_24dp : R.drawable.ic_dot_black_24dp
                ))
                .anchor(0.5f, portID.equals(selectedPortID) ? 0.9f : 0.5f);
    }
    public void showMarker(Port port, String portID) {
        if (port.marker == null && googleMap != null) port.marker = googleMap.addMarker(getMarkerOptions(port, portID));
    }
    public void hideMarker(Port port) {
        if (port.marker == null) return;
        port.marker.remove();
        port.marker = null;
    }
    public void updateMarker(Port port, String portID) {
        if (portID.equals(selectedPortID) || port.favorite || port.marker != null) {
            hideMarker(port);
            showMarker(port, portID);
        }
    }
}
