package com.avaa.balitidewidget.views;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.avaa.balitidewidget.data.Port;

import java.util.Map;

/**
 * Created by Alan on 6 Nov 2016.
 */

public class PortsListViewAdapter extends ArrayAdapter<Port> {
    public PortsListViewAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PortListViewItem view = (PortListViewItem)convertView;
        if (view == null) view = new PortListViewItem(getContext(), null);
        view.setPort(getItem(position));
        return view;
    }
}
