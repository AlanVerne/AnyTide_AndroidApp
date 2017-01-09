package com.avaa.balitidewidget.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Created by Alan on 28 Oct 2016.
 */

public class TideLoadingIndicator extends View {
    private float w = 0.4f;
    private float x = 0;
    private final Paint paint = new Paint() {{setColor(0xff0091ce); setStyle(Paint.Style.STROKE); setStrokeCap(Paint.Cap.ROUND); setAntiAlias(true);}};
    private float fd1, fa1, fd2, fp2, fa2;

    public TideLoadingIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStrokeWidth(3 * context.getResources().getDisplayMetrics().density);
        newPath();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (getVisibility() == VISIBLE) {
            restart();
            postInvalidate();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        float x = this.x;
        float y = f(x/getWidth());

        Path p = new Path();
        p.moveTo(x, y);

        float l = w * getWidth();

        float dx2 = getWidth() * w / 100f;
        dx2 = dx2*dx2;
        while (l > w*getWidth()*19f/20f) {
            x += getWidth() * w / 100f;
            float newy = f(x / getWidth());
            l -= Math.sqrt(dx2+(y-newy)*(y-newy));
            y = newy;
            p.lineTo(x, y);
        }
        this.x = x;
        while (l > 0) {
            x += getWidth() * w / 100f;
            float newy = f(x / getWidth());
            l -= Math.sqrt(dx2+(y-newy)*(y-newy));
            y = newy;
            p.lineTo(x, y);
        }

        //0xffffdd33
        canvas.drawPath(p, paint);

        if (this.x >= getWidth()) restart();

        if (getVisibility() == VISIBLE) postInvalidate();
    }

    public void restart() {
        x = -getWidth()*w*1.2f;
        newPath();
    }

    private void newPath() {
        Random r = new Random();
        fd1 = r.nextFloat()*10000;
        fa1 = 0.1f + r.nextFloat()*0.9f;
        fd2 = r.nextFloat()*10000;
        fp2 = 1f + r.nextFloat()*5f;
        fa2 = 0.0f + r.nextFloat()*1.0f;
    }

    private float f(float state) {
        return getHeight() * ((float)(fa1*Math.cos(state*12 + fd1) * (1+fa2*Math.cos(state*fp2 + fd2))/2f) + 1f)/2f;
    }
}
