package com.avaa.balitidewidget.data;

import android.graphics.Path;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Created by Alan on 9 Jan 2017.
 */


public class TideDataToPath {
    public static Path getPath(int[] values, float w, float h, int min, int max, int hStart, int hEnd) {
        if (values == null) return null;

        Path p = new Path();

        p.moveTo(0, h*2);

        hStart *= 6; hEnd *= 6;
        int hWidth = hEnd - hStart;
        int minMaxH = max - min;
        for (int i = hStart; i <= hEnd; i++) {
            p.lineTo(w * (i-hStart) / hWidth, h * (1f - ((float)values[i]/10.0f - min) / minMaxH));
        }

        p.lineTo(w, h*2);
        p.close();

        return p;
    }
}
