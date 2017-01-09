package com.avaa.balitidewidget.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.avaa.balitidewidget.R;
import com.avaa.balitidewidget.data.Port;

import java.util.Map;

/**
 * Created by Alan on 3 Nov 2016.
 */

public class PortListViewItem extends LinearLayout {
    TextView tvName;
    TextView tvSubname;
    TextView tvDistance;
    ImageView ivStar;

    public PortListViewItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        float density = context.getResources().getDisplayMetrics().density;

        setMinimumHeight((int)(60*density));
        setPadding((int)(20*density), 0, (int)(20*density), 0);
        setGravity(Gravity.CENTER_VERTICAL);

        tvName = new TextView(context);
        tvSubname = new TextView(context);
        tvDistance = new TextView(context);
        ivStar = new ImageView(context);

        tvName.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0));
        tvName.setTextColor(0xff000000);
        tvName.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tvName.setPadding(0, 0, (int)(10*density), 0);
        tvName.setTextSize(14);
        tvName.setSingleLine(true);
        tvSubname.setTextColor(0x88000000);
        tvSubname.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        tvSubname.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tvSubname.setPadding(0, 0, (int)(10*density), 0);
        tvSubname.setTextSize(14);
        tvSubname.setSingleLine(true);
        tvDistance.setTextColor(0x88000000);
        tvDistance.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0));
        tvDistance.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        tvDistance.setPadding(0, 0, (int)(10*density), 0);
        tvDistance.setTextSize(14);
        tvDistance.setSingleLine(true);
        ivStar.setImageResource(R.drawable.ic_grade_gray_24dp);
        ivStar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        //ivStar.setMinimumWidth((int)(40*density));

        addView(tvName);
        addView(tvSubname);
        addView(tvDistance);
        addView(ivStar);
    }

    public void setPort(Port port) {
        tvName.setText(port.getName());
        tvSubname.setText(port.getSubname());
        tvDistance.setText(port.getDistanceString());
        ivStar.setVisibility(port.favorite ? VISIBLE : GONE);
    }
}
