package com.avaa.balitidewidget.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.Log;

import com.avaa.balitidewidget.Common;
import com.avaa.balitidewidget.R;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TimeZone;

/**
 * Created by Alan on 25 May 2016.
 */

public class TideChartDrawer {
    private static final int WHITE = 0xffffffff;
    private static final int BLACK = 0xff000000;
    private static final int BLUE = 0xff0fabec; //55ccff;
    private static final int AVE_BLUE = 0xff0d93cb; //40b0e0;
    private static final int DARK_BLUE = 0xff0a77a5; //337799;
    private static final int RED = 0xffbb1111;

    private static final int GRAY_LEVEL_SCALE_LINE = 0x88f2f2f2;
    private static final int GRAY_LEVEL_SCALE = 0xfff2f2f2;
    private static final int GRAY_LEVEL_SCALE_TEXT = 0xFFBBBBBB;

    private static final int GRAY_TIDE_MINOR = 0x77000000;

    private static final int WHITE_TRANSPARENT_ABOVE_200_REGION = 0x44ffffff;
    private static final int WHITE_TRANSPARENT_SCALE_HOUR_9_12_15 = 0x66ffffff;

    private static final int MINUTES_IN_DAY = 24*60;

    private static final float BEZIER_CIRCLE_K = 1 - 0.5522f;

    private static final float EXTREMUMS_FONT_MAX_SIZE = 16; //dp
    private static final float HOURLY_FONT_MAX_SIZE = 14;


    private final String tomorrowStr;


    int border = 0;

    int rounding = 3;

    int hourScaleWidth = 1;
    int hourScaleHeight = 6;
    int hourScaleOffset = 0; //hourScaleHeight;

    int levelMin = -50;
    int levelMax = 400;

    int bottomPadding = 20;

    int smallestTextSize = 8;
    int smallTextSize = 12;
    int smallTextHeight;
    int smallTextPadding;


    public float density;
    int w, h;
    Canvas c;


    boolean drawLevelScaleLabels;
    boolean drawExtremumsText;


    int levelScaleWidthLeft  = 0;
    int levelScaleWidthRight = 0;


    Paint paint = new Paint() {{
        setColor(WHITE);
        setStyle(Style.STROKE);
        setStrokeCap(Cap.BUTT);
        setStyle(Style.FILL);
        setAntiAlias(true);
    }};
    Paint paintExtremumsText = new Paint() {{
        setTextSize(smallTextSize);
        setStyle(Style.FILL);
        setTextAlign(Align.CENTER);
        setAntiAlias(true);
    }};
    Paint paintLevelScaleText = new Paint(paintExtremumsText) {{
        setColor(GRAY_LEVEL_SCALE_TEXT); //setColor(0xffe0e0e0);
        setTextSize(smallestTextSize);
        setTextAlign(Align.LEFT);
    }};
    public String name = null;


    public TideChartDrawer(float density, boolean roundEdges) {
        this(density);
        if (!roundEdges) rounding = 0;
    }
    public TideChartDrawer(float density) {
        this.density = density;

        rounding *= density;
        border *= density;

        hourScaleWidth *= density;
        hourScaleHeight *= density;
        hourScaleOffset *= density;

        bottomPadding *= density;

        smallTextSize *= density;
        smallestTextSize *= density;
        paintExtremumsText.setTextSize(smallTextSize);
        paintLevelScaleText.setTextSize(smallestTextSize);

        tomorrowStr = DateUtils.getRelativeTimeSpanString(
                1000*60*60*24, 0,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString();
    }


    public void set24h(boolean b) {
        if (b) {
            sH = 0;
            eH = 24;
        }
        else {
            sH = 3;
            eH = 21;
        }
    }
    int sH = 0;//3;
    int eH = 24;//21;


    private int getY(int tide) {
        return (int)((h - bottomPadding) * (1 - (double)(tide - levelMin) / (levelMax - levelMin)));
    }

    public Context context = null;

    public Bitmap draw(int width, int height, TideData tideData, int plusDays, int tomorrow, boolean drawHourly, Port port) {
        Log.i("TCD", "draw " + width + " " + height);

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        c = new Canvas(b);

        w = width  - border*2;
        h = height - border*2;

        long day = Common.getDay(plusDays, port.getTimeZone());

        if (tideData == null || !tideData.hasData(day)) {
            DrawNoData();
            return b;
        }

        c.translate(border, border);

        int maxTide = -100;

        maxTide = 250;

        int tideMax = tideData.max;
        int d = getD(tideMax);
        tideMax  = (tideMax / d/10) * d;
        levelMax = tideMax;
        levelMin = Math.min(-50, (tideData.min / d/10) * d);

//        if (tomorrow == 0) {
//            for (Map.Entry<Integer, Integer> tide : tides.entrySet()) {
//                if (tide.getKey() >= 3 * 60 && tide.getKey() <= 21 * 60) {
//                    if (tide.getValue() > maxTide) maxTide = tide.getValue();
//                }
//            }
//        }

        boolean bigExtremumsFont = false;
        int nowTextSize = 0;

        bottomPadding = hourScaleHeight;
        if (width/density < 240 || height/density < 100) {
            drawExtremumsText = false;
            smallTextSize = (int)(12 * density) * 24/(24 - sH);
            paintExtremumsText.setTextSize(smallTextSize);
        }
        else {
            if (!drawHourly && width/height >= 1 && width/height < 2.3) bigExtremumsFont = true;

            drawExtremumsText = true;
            smallTextSize = Math.min((int)(EXTREMUMS_FONT_MAX_SIZE*density*24/(24 - sH)), Math.min(height, width/2) / 14 * 24/(24 - sH));
            nowTextSize = (int)Math.min(18*density*1.5, smallTextSize*2);
            if (bigExtremumsFont) smallTextSize = (int)Math.min(15*density, width/10/2);
            paintExtremumsText.setTextSize(smallTextSize);

            Rect bounds = new Rect();
            paintExtremumsText.getTextBounds("0", 0, 1, bounds);
            smallTextHeight  = bounds.height();

            smallTextPadding = hourScaleHeight + Math.max(Math.max(hourScaleHeight, smallTextHeight)/2, height * smallTextHeight / width);
            if (bigExtremumsFont) smallTextPadding *= 1.5;

            bottomPadding += smallTextPadding + smallTextHeight;
        }

        paint.setTextSize(nowTextSize);

        hourScaleWidth = Math.min(density >= 3 ? 4 : 3, Math.max(density >= 3 ? 2 : 1, width / 300));
        hourScaleWidth = (int)(hourScaleWidth * 24f/(eH - sH)); // * 9f/8f
        levelMax += drawExtremumsText ? (drawHourly || width*0.75 <= height || (maxTide > 240 && tomorrow == 0)) ? 300 : 200 : 150;

        drawLevelScaleLabels = width/density > 120 && height/density > 100;
        if (drawLevelScaleLabels) {
            smallestTextSize = Math.min(Math.min(height / 24, (int)(9 * density)), (int)(smallTextSize-density));
            paintLevelScaleText.setTextSize(smallestTextSize);
        }

        if (!drawExtremumsText) drawHourly = false;

        int maxFontSize = Math.max((int)(width*0.9/(eH - sH)), (int)(HOURLY_FONT_MAX_SIZE*density*24/(24 - sH)));
        final int hourlyTextSize = Math.min(maxFontSize, Math.min(height, width / 2) / 15 *24/(24-sH));
        Paint paintHourlyTides = new Paint(paintLevelScaleText) {{
            setColor(GRAY_TIDE_MINOR);
            setTextAlign(Align.LEFT);
            setTextSize(hourlyTextSize);
        }};
        Paint paintHourly3Tides = new Paint(paintHourlyTides) {{
            setColor(BLACK);
        }};
        Paint paintHourly3Hour = new Paint(paintHourlyTides) {{
            setColor(WHITE);
        }};
        Rect bounds = new Rect();
        paintHourly3Hour.getTextBounds("10:00", 0, 5, bounds);
        int hourlyHoursWidth = bounds.width();
        int hourlyHoursHeight = bounds.height();

        if (drawHourly || width*0.75 <= height) bottomPadding = hourScaleHeight * 2 + hourlyHoursWidth;

        DrawBG();

        final Path path = TideDataToPath.getPath(tideData.getPrecise(day), w, h - bottomPadding, levelMin, levelMax, (sH==0 ? 0 : 3), (sH==0 ? 24 : 21));
        SortedMap<Integer, Integer> hourlyTides = tideData.getHourly(day, (sH==0 ? 4 : 5), (sH==0 ? 20 : 19));

        paintForTomorrowText.setTextSize(paintExtremumsText.getTextSize());
        DrawLabels(tomorrow == 1);
        if (tomorrow == 0) {
            Integer nowTide = tideData.getNow(day);
            if (nowTide == null) DrawNowLines(port.getTimeZone(), paint);
            else DrawNowLines(nowTide, port.getTimeZone(), paint);
        }

        int widthInMinutes = (eH-sH)*60;
        int sM = sH*60;

        SunTimes sunTimes = SunTimesProvider.get(port.position, day, port.getTimeZone());
        int firstlight = (sunTimes.cSunrise-sM) * w / widthInMinutes;
        int lastlight  = (sunTimes.cSunset -sM) * w / widthInMinutes;
        int sunrise    = (sunTimes.sunrise -sM) * w / widthInMinutes;
        int sunset     = (sunTimes.sunset  -sM) * w / widthInMinutes;

        Region sunReg = new Region(sunrise, 0, sunset, h);
        Region firstLightReg = new Region(firstlight, 0, sunrise, h);
        firstLightReg.op(sunset, 0, lastlight, h, Region.Op.UNION);
        Region nightReg = new Region(0, 0, firstlight, h);
        nightReg.op(lastlight, 0, w, h, Region.Op.UNION);

        Path bottomRounding = null;
        if (rounding > 0) {
            bottomRounding = new Path();
            bottomRounding.moveTo(0, 0);
            bottomRounding.lineTo(0, h - rounding);
            bottomRounding.cubicTo(0, h - rounding * BEZIER_CIRCLE_K, rounding * BEZIER_CIRCLE_K, h, rounding, h);
            bottomRounding.lineTo(w - rounding, h);
            bottomRounding.cubicTo(w - rounding * BEZIER_CIRCLE_K, h, w, h - rounding * BEZIER_CIRCLE_K, w, h - rounding);
            bottomRounding.lineTo(w, 0);
            bottomRounding.close();
        }

        Region above4DReg = new Region(0, tideMax-getY(d*2), w, tideMax-getY(d));
        //Region above6DReg = new Region(0, getY(getD()*7), w, getY(getD()*6));

        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Path p = new Path(path);
            p.op(nightReg.getBoundaryPath(), Path.Op.INTERSECT);
            if (bottomRounding != null) p.op(bottomRounding, Path.Op.INTERSECT);
            paint.setColor(DARK_BLUE);
            c.drawPath(p, paint);

            p = new Path(path);
            p.op(firstLightReg.getBoundaryPath(), Path.Op.INTERSECT);
            paint.setColor(AVE_BLUE);
            c.drawPath(p, paint);

            p = new Path(path);
            p.op(sunReg.getBoundaryPath(), Path.Op.INTERSECT);
            paint.setColor(BLUE);
            c.drawPath(p, paint);

            p = new Path(path);
            p.op(above4DReg.getBoundaryPath(), Path.Op.INTERSECT);
            paint.setColor(WHITE_TRANSPARENT_ABOVE_200_REGION);
            c.drawPath(p, paint);

//            p = new Path(path);
//            p.op(above6DReg.getBoundaryPath(), Path.Op.INTERSECT);
//            paint.setColor(WHITE_TRANSPARENT_ABOVE_200_REGION);
//            c.drawPath(p, paint);
        }
        else {
            c.clipPath(nightReg.getBoundaryPath());
            paint.setColor(DARK_BLUE);
            c.drawPath(path, paint);

            c.clipPath(firstLightReg.getBoundaryPath(), Region.Op.REPLACE);
            paint.setColor(AVE_BLUE);
            c.drawPath(path, paint);

            c.clipPath(sunReg.getBoundaryPath(), Region.Op.REPLACE);
            paint.setColor(BLUE);
            c.drawPath(path, paint);

            c.clipPath(above4DReg.getBoundaryPath(), Region.Op.REPLACE);
            paint.setColor(WHITE_TRANSPARENT_ABOVE_200_REGION);
            c.drawPath(path, paint);

//            c.clipPath(above6DReg.getBoundaryPath(), Region.Op.REPLACE);
//            paint.setColor(WHITE_TRANSPARENT_ABOVE_200_REGION);
//            c.drawPath(path, paint);

            c.clipRect(0, 0, width, height, Region.Op.REPLACE);
        }

        c.save();
        try {
            Calendar cal = new GregorianCalendar(port.getTimeZone());
            int dayofyear = cal.get(Calendar.DAY_OF_YEAR);
            if (density >= 1 && context != null && dayofyear > 355 || dayofyear <= 7) {
                //Drawable drawable = context.getResources().getDrawable(context.getResources().getDrawable(R.drawable.snowflakes));
                Bitmap sf = BitmapFactory.decodeResource(context.getResources(), R.drawable.snowflakes);

                c.clipPath(path, Region.Op.REPLACE);

                int h = getY(270);

                Paint paint = new Paint() {{
                    setAlpha(density >= 3 ? 80 : 70);
                }};

                Random r = new Random();

                int sfx;
                int sfy;

                int s = r.nextInt(8+2);
                if (s > 7) s = 7 + r.nextInt(5);
                switch (s) {
                    case 0: sfx = 0; sfy = 0; break;
                    case 1: sfx = 1; sfy = 0; break;
                    case 2: sfx = 2; sfy = 0; break;
                    case 3: sfx = 2; sfy = 1; break;
                    case 4: sfx = 1; sfy = 2; break;
                    case 5: sfx = 2; sfy = 2; break;
                    case 6: sfx = 3; sfy = 2; break;
                    case 7: sfx = 1; sfy = 3; break;

                    case 8: sfx = 3; sfy = 0; break;
                    case 9: sfx = 1; sfy = 1; break;
                    case 10: sfx = 0; sfy = 2; break;
                    case 11: sfx = 0; sfy = 3; break;
                    default: sfx = 3; sfy = 3; break;
                }
                sfx*=300; sfy*=300;

                int nsf = (int)((width/density)*(height/density)/2500);
                for (int i = 0; i < nsf; i++) {
                    int sfsize = density >= 3 ? 60 : (int)(density * 24);
                    int sfpx = r.nextInt(width + sfsize) - sfsize;
                    int sfpy = h + r.nextInt(height - h + sfsize) - sfsize;
                    c.drawBitmap(sf, new Rect(sfx, sfy, sfx + 300, sfy + 300), new Rect(sfpx, sfpy, sfpx + sfsize, sfpy + sfsize), paint);
                }

                c.clipRect(0, 0, width, height, Region.Op.REPLACE);
            }
        }
        catch (Exception ignored) { }
        c.restore();

        Region shiftedPath = new Region();
        shiftedPath.setPath(path, new Region(0, 0, width, height));
        shiftedPath.translate(0, hourScaleHeight);

        Region shiftedPath2 = new Region();
        shiftedPath2.setPath(path, new Region(0, 0, width, height));
        shiftedPath2.translate(0, hourScaleHeight+hourlyHoursWidth+hourlyHoursHeight*2);

        Region region = new Region();

        int ticksWidth = hourScaleWidth;
        if (bigExtremumsFont) ticksWidth = hourScaleWidth * 4 / 3;

        for (int hour = (sH==0 ? 4 : 5); hour <= (sH==0 ? 20 : 19); hour++) {
            //int x = hour * w / 24  -  hourScaleWidth / 2;

            int x = w * (hour-sH) / (eH-sH)  -  ticksWidth / 2;
            Region r = new Region(x, 0, x + ticksWidth, h);

            region.setPath(path, r);

            if (hour % 3 == 0) {
                if (hour > 6 && hour < 18) {
                    paint.setColor(WHITE_TRANSPARENT_SCALE_HOUR_9_12_15);
                    if (drawHourly || bigExtremumsFont) {
                        Region r22 = new Region(region);
                        if (bigExtremumsFont) {
                            Region shiftedPath3 = new Region();
                            shiftedPath3.setPath(path, new Region(0, 0, width, height));
                            shiftedPath3.translate(0, smallTextHeight + smallTextPadding*2 - (int)(hourScaleHeight));
                            r22.op(shiftedPath3, Region.Op.INTERSECT);
                        }
                        else {
                            r22.op(shiftedPath2, Region.Op.INTERSECT);
                        }
                        c.drawPath(r22.getBoundaryPath(), paint);
                    }
                    else c.drawPath(region.getBoundaryPath(), paint);
                }

                paint.setColor(WHITE);
                region.op(shiftedPath, Region.Op.DIFFERENCE);
                c.drawPath(region.getBoundaryPath(), paint);
            }
            else {
                paint.setColor(WHITE_TRANSPARENT_SCALE_HOUR_9_12_15);
                region.op(shiftedPath, Region.Op.DIFFERENCE);
                c.drawPath(region.getBoundaryPath(), paint);
            }
        }

        if (drawHourly) {
            int save = c.save();
            c.rotate(-90);
            for (Map.Entry<Integer, Integer> entry : hourlyTides.entrySet()) {
                Integer h = entry.getKey();
                boolean h3 = h % 3 == 0;
                Point point = new Point(w * (h-sH) / (eH-sH) + hourlyHoursHeight / 2, getY(entry.getValue()));
                c.drawText(String.valueOf(Math.round(entry.getValue() / 10) / 10.0), -point.y + hourScaleHeight + hourlyHoursHeight, point.x, h3 ? paintHourly3Tides : paintHourlyTides);

                String strH = (h > 10 ? String.valueOf(h) : "0" + String.valueOf(h))  + ":00";
                if (h3) c.drawText(strH, -point.y - hourScaleHeight - hourlyHoursHeight - hourlyHoursWidth, point.x, paintHourly3Hour);
            }
            c.restoreToCount(save);
        }

        DrawLevelScale();
        if (!drawHourly && drawExtremumsText) DrawExtremumsText(tideData.getExtremums(day));

        hourScaleWidth = ticksWidth;
        DrawHoursScaleBottom(this.paint);

        return b;
    }


    private void DrawNoData() {
        paint.setColor(0xffeeeeee);
        c.drawRect(0,0,w,h,paint);
        paintExtremumsText.setColor(0xff000000);
        c.drawText("No data", w/2, h/2 - 40, paintExtremumsText);
        c.drawText("Internet required", w/2, h/2 + 40, paintExtremumsText);
    }

    private int getD(int levelMax) {
        int d = 50;
        if (levelMax > 500) d = 100;
        return d;
    }

    private void DrawBG() {
        int prevY = getY(-50);
        int d = getD(levelMax);
        for (int tide = 0; tide <= levelMax; tide += d) {
            if (tide <= 0) paint.setColor(0xffda7f5b);
            else if (tide <= d*2) {
                if (tide % (d*2) != 0) paint.setColor(0xff82dbaf);
                else paint.setColor(0xffc9eddc);
            }
            else {
                if (tide % (d*2) != 0) paint.setColor(WHITE);
                else paint.setColor(GRAY_LEVEL_SCALE);
            }

            int y = getY(tide);

            if (tide == levelMax && rounding > 0) {
                Path p = new Path();
                p.moveTo(0, prevY);
                p.lineTo(0, y+rounding);
                p.cubicTo(0f,y+rounding*BEZIER_CIRCLE_K, rounding*BEZIER_CIRCLE_K,y, rounding,y);
                p.lineTo(w-rounding, y);
                p.cubicTo(w-rounding*BEZIER_CIRCLE_K,y, w, y+rounding*BEZIER_CIRCLE_K, w,y+rounding);
                p.lineTo(w, prevY);
                p.close();

                c.drawPath(p, paint);
            }
            else c.drawRect(0, y, w, prevY, paint);

            prevY = y;
        }
    }


    Paint paintForTomorrowText = new Paint(paintExtremumsText) {{
        setAntiAlias(true);
        setColor(GRAY_LEVEL_SCALE_TEXT);
        setTextAlign(Paint.Align.LEFT);
    }};
    private void DrawLabels(boolean drawTomorrow) {
        if (name == null && !drawTomorrow) return;

        Rect b = new Rect();
        paintForTomorrowText.getTextBounds("W", 0, 1, b);
        int fh = b.height();

        int h = getY(350) - getY(400);
        int d = (h + fh) / 2;
        if (fh * 1.3 > h) {
            h = getY(200) - getY(300);
            d = (h + fh) / 2;
        }

        String tomorrowStr = this.tomorrowStr;
        int tomorrowStrW = 0;
        if (drawTomorrow) {
            paintForTomorrowText.getTextBounds(tomorrowStr, 0, tomorrowStr.length(), b);
            tomorrowStrW = b.width();
            if (tomorrowStrW + d * 2 > w / 2) {
                tomorrowStr = "Tmrw";
                paintForTomorrowText.getTextBounds(tomorrowStr, 0, tomorrowStr.length(), b);
                tomorrowStrW = b.width();
            }
        }

        if (name == null && drawTomorrow) {
            if (tomorrowStrW + d * 4 > w) {
                c.drawText(tomorrowStr, w / 2 - tomorrowStrW / 2, d, paintForTomorrowText);
            }
            else {
                int x = w - tomorrowStrW - d;
                c.drawText(tomorrowStr, x, d, paintForTomorrowText);
            }
        }
        else {
            paintForTomorrowText.getTextBounds(name, 0, name.length(), b);
            int nameW = b.width();

            if (nameW + d * 4 > w) {
                c.drawText(name, w/2-nameW/2, d, paintForTomorrowText);
            }
            else {
                c.drawText(name, d, d, paintForTomorrowText);
            }

            if (drawTomorrow) {
                if (w - nameW - tomorrowStrW - h*3 > 0) {
                    c.drawText(tomorrowStr, w - tomorrowStrW - d, d, paintForTomorrowText);
                }
                else {
                    if (nameW + d * 4 > w) {
                        c.drawText(tomorrowStr, w/2-tomorrowStrW/2, d+h, paintForTomorrowText);
                    }
                    else {
                        c.drawText(tomorrowStr, d, d+h, paintForTomorrowText);
                    }
                }
            }
        }
    }


    private void DrawExtremumsText(Map<Integer, Integer> extremums) {
        if (extremums == null) return;

        paintExtremumsText.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();

        paintExtremumsText.getTextBounds("0.00m", 0, 5, bounds);
        int labelW2 = bounds.width()/2;

        for (Map.Entry<Integer, Integer> tide : extremums.entrySet()) {
            Point p = new Point(w * (tide.getKey()-sH*60) / (eH-sH)/60, getY(tide.getValue()));

            if (p.x < labelW2+levelScaleWidthLeft || p.x > w-labelW2-levelScaleWidthRight) continue;

            paintExtremumsText.setColor(BLACK);
            paintExtremumsText.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            c.drawText(String.valueOf(tide.getValue() / 100.0f) + "m", p.x, p.y - smallTextPadding, paintExtremumsText);

            Integer time = tide.getKey();
            int hour = time / 60;
            int min  = time - hour * 60;
            String strTime = hour + (min < 10 ? ":0" : ":") + min;

            paintExtremumsText.setColor(WHITE);
            paintExtremumsText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            c.drawText(strTime, p.x, p.y + smallTextPadding + smallTextHeight, paintExtremumsText);
        }
    }


    private void DrawHoursScaleBottom(Paint paint) {
        paint.setColor(WHITE);
        //paint.setStrokeWidth(hourScaleWidth);

        int hourH = h - hourScaleOffset;

        for (int hour = sH+1; hour < eH; hour++) {
            int x = w * (hour-sH) / (eH-sH) - hourScaleWidth/2;
            if (hour % 3 == 0) c.drawRect(x, hourH - hourScaleHeight, x+hourScaleWidth, hourH, paint);
            else c.drawRect(x, hourH - hourScaleWidth*2, x+hourScaleWidth, hourH, paint);
        }
    }


    private void DrawNowLines(int tide, TimeZone timeZone, Paint paint) {
        Calendar calendar = new GregorianCalendar(timeZone);
        int now  = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int nowX = w * (now-sH*60) / (eH-sH)/60; // w * now / MINUTES_IN_DAY;

        paint.setStrokeWidth(0);
        paint.setColor(RED);

        Rect r = new Rect();
        paint.getTextBounds("0.00", 0, 4, r);
        int d = w / 24;
        if (nowX - r.width() - d*3 < 0) {
            paint.setTextAlign(Paint.Align.LEFT);
            c.drawText(String.valueOf(tide / 100.0), nowX + d, r.height() + d, paint);
            DrawNowLines(timeZone, paint, r.height()+d*2);
        }
        else {
            paint.setTextAlign(Paint.Align.RIGHT);
            c.drawText(String.valueOf(tide / 100.0), nowX - d, r.height() + d, paint);
            DrawNowLines(timeZone, paint, 0);
        }
    }
    private void DrawNowLines(TimeZone timeZone, Paint paint) {
        DrawNowLines(timeZone, paint, 0);
    }
    private void DrawNowLines(TimeZone timeZone, Paint paint, int y) {
        Calendar calendar = new GregorianCalendar(timeZone);
        int now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int nowX = w * (now-sH*60) / (eH-sH) / 60; // w * now / MINUTES_IN_DAY;
        if (nowX < 0 || nowX > w) return;

        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(RED);

        int w = smallestTextSize/8;
        if (hourScaleWidth*300/24 <= w/(eH-sH)) c.drawRect(nowX-w, 0, nowX+w*2, h, paint);
        else {
            c.drawRect(nowX-w, 0, nowX+w*2, h, paint);
            return;
        }

//        int halfW = hourScaleWidth/2;
//        nowX = w * (now+60-sH*60) / (eH-sH)/60; //(now+60)  / MINUTES_IN_DAY;
//        c.drawRect(nowX-halfW, y, nowX+hourScaleWidth, h, paint);
//        nowX = w * (now+120-sH*60) / (eH-sH)/60; //w * (now+120) / MINUTES_IN_DAY;
//        c.drawRect(nowX-halfW, y, nowX+hourScaleWidth, h, paint);
    }


    private void DrawLevelScale() {
        paint.setColor(GRAY_LEVEL_SCALE_LINE);

        int spacings = 5;

        Rect r = new Rect();
        paintLevelScaleText.getTextBounds("0m", 0, 2, r);
        levelScaleWidthLeft  = r.width() + spacings*4;
        paintLevelScaleText.getTextBounds("0", 0, 1, r);
        levelScaleWidthRight = r.width() + spacings*4;

        int linesWidth = Math.max(1, (Math.min(hourScaleWidth, Math.round(paintLevelScaleText.getTextSize() / 12))));

        int d = getD(levelMax)*2;

        for (int tide = 0; tide < levelMax; tide += d) {
            int y = getY(tide);

            if (drawLevelScaleLabels) {
                c.drawRect(0, y, levelScaleWidthLeft,    y+linesWidth, paint);
                c.drawRect(w, y, w-levelScaleWidthRight, y+linesWidth, paint);

                paintLevelScaleText.setTextAlign(Paint.Align.LEFT);
                c.drawText(String.valueOf(tide / 100) + "m", spacings*2, y - spacings, paintLevelScaleText);
                paintLevelScaleText.setTextAlign(Paint.Align.RIGHT);
                c.drawText(String.valueOf(tide / 100) + "", w-spacings*2, y - spacings, paintLevelScaleText);
            }
            else {
                c.drawRect(0, y, hourScaleHeight,   y+linesWidth, paint);
                c.drawRect(w, y, w-hourScaleHeight, y+linesWidth, paint);
            }
        }
    }
}
