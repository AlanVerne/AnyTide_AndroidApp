package com.avaa.balitidewidget.views;

/**
 * Created by Alan on 26 Oct 2016.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * Created by Alan on 9 Jul 2016.
 */

public class ObservableScrollView extends ScrollView {
    private static final int MAX_SCROLL_CHANGE_INTERVAL = 100;
    private static final int SCROLL_CHANGE_LISTENER_INTERVAL = 100;

    public interface ScrollViewListener {
        void scroll(int l, int kt, int oldl, int oldt);
        void interactionFinished();
        void interactionFinishedWithSwing(int v);
    }

    private class ScrollStateHandler implements Runnable {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScrollUpdate > MAX_SCROLL_CHANGE_INTERVAL) {
                lastScrollUpdate = -1;
                onScrollEnd();
            } else {
                postDelayed(this, SCROLL_CHANGE_LISTENER_INTERVAL);
            }
        }
    }

    private ScrollViewListener scrollViewListener = null;

    private long lastScrollUpdate = -1;

    private boolean down = false;


    // --


    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }


    protected void onScrollStart() {}
    protected void onScrollEnd()   {}


    protected boolean isScrolling() {
        return lastScrollUpdate != -1;
    }

    @Override
    protected void onScrollChanged(int l, int kt, int oldl, int oldt) {
        super.onScrollChanged(l, kt, oldl, oldt);

        if (lastScrollUpdate == -1) {
            onScrollStart();
            postDelayed(new ScrollStateHandler(), SCROLL_CHANGE_LISTENER_INTERVAL);
        }

        lastScrollUpdate = System.currentTimeMillis();

        if (scrollViewListener != null) scrollViewListener.scroll(l, kt, oldl, oldt);
    }

    public boolean isDown() {
        return down;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_MOVE) {
            if (down == false) {
                down = true;
                //scrollViewListener.interactionS();
            }
        }

        boolean b = super.onTouchEvent(ev);

        if (mGestureDetector.onTouchEvent(ev)) { }
        else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            down = false;
            scrollViewListener.interactionFinished();
        }
        return b;
    }


    private static final int SWIPE_THRESHOLD_VELOCITY = 300;
    private final GestureDetector.OnGestureListener ogl = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                    down = false;
                    scrollViewListener.interactionFinishedWithSwing((int)velocityY);
                    return true;
                }
            }
            catch (Exception e) {
                Log.e("Fling", "There was an error processing the Fling event:" + e.getMessage());
            }
            return false;
        }
    };
    GestureDetector mGestureDetector = new GestureDetector(this.getContext(), ogl);
}

