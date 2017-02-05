package com.avaa.balitidewidget;

import android.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.avaa.balitidewidget.data.Port;
import com.avaa.balitidewidget.data.Ports;
import com.avaa.balitidewidget.views.ExtendedEditText;
import com.avaa.balitidewidget.views.PortsListViewAdapter;

import java.util.List;
import java.util.Map;


public class ChoosePortFragment extends Fragment {
    private static final int FL_CL_PERMISSIONS_REQUEST = 123;

    private static final Ports PORTS = new Ports(null);

    private ChoosePortFragmentListener listener;

    private PortsListViewAdapter lvSearchResultsAdapter = null;
    private ListView lvSearchResults;
    private ExtendedEditText etSearch;


    public ChoosePortFragment() { }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_port, container, false);

        etSearch = (ExtendedEditText)view.findViewById(R.id.etSearch);
        lvSearchResults = (ListView)view.findViewById(R.id.lvSearchResults);

        initSearch();

        view.findViewById(R.id.btnClearSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etSearch.setText("");
            }
        });
        view.findViewById(R.id.btnCloseSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onPortSelected(null);
            }
        });

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ChoosePortFragmentListener) {
            listener = (ChoosePortFragmentListener)context;
        }
        else {
            throw new RuntimeException(context.toString() + " must implement ChoosePortFragmentListener");
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Common.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        PORTS.load(sharedPreferences);
    }


    private void initSearch() {
        lvSearchResultsAdapter = new PortsListViewAdapter(getContext(), android.R.layout.simple_list_item_1);
        lvSearchResults.setAdapter(lvSearchResultsAdapter);
        lvSearchResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                etSearch.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listener.onPortSelected(lvSearchResultsAdapter.getItem(position));
                    }
                }, 100);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                updateSearchResults(s.toString());
                getView().findViewById(R.id.btnClearSearch).setVisibility(s.toString().isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) { //Handle search key click
                    updateSearchResults();
                    return true;
                }
                return false;
            }
        });
        etSearch.setBackKeyListener(new ExtendedEditText.BackKeyListener() {
            @Override
            public void onBackKey() {
                listener.onPortSelected(null);
            }
        });
        etSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (b) {
                    imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
                }
                else {
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
            }
        });
        etSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        lvSearchResults.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                lvSearchResults.requestFocus();
                return false;
            }
        });
    }


    public void activate() {
        updateMyLocation();
        updateSearchResults();
        etSearch.selectAll();
        etSearch.postDelayed(new Runnable() {
            @Override
            public void run() {
                etSearch.requestFocus();
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 0);
    }


    private void updateSearchResults() {
        updateSearchResults(etSearch.getText().toString());
    }
    private void updateSearchResults(String s) {
        lvSearchResultsAdapter.clear();
        lvSearchResultsAdapter.addAll(PORTS.search(s));
    }


    private Location updateMyLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    FL_CL_PERMISSIONS_REQUEST);
        }

        LocationManager locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);

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


    public Port getBestPort() {
        updateMyLocation();

        Port nearestFavorite = PORTS.searchNearestFavorite();
        if (nearestFavorite != null && nearestFavorite.distance < 50000) return nearestFavorite;
        else if (PORTS.portsAreSortedByDistance()) {
            Port nearestNotFavorite = PORTS.searchNearestNotFavorite();
            if (nearestNotFavorite != null && nearestNotFavorite.distance < 25000) return nearestNotFavorite;
        }

        return null;
    }


    public interface ChoosePortFragmentListener {
        void onPortSelected(Port port);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FL_CL_PERMISSIONS_REQUEST) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                updateMyLocation();
                updateSearchResults();
            }
        }
    }
}
