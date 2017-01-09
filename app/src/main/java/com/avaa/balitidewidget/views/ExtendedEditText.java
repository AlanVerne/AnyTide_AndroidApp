package com.avaa.balitidewidget.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * Created by Alan on 26 Oct 2016.
 */

public class ExtendedEditText extends EditText {
    private BackKeyListener backKeyListener = null;

    public interface BackKeyListener {
        void onBackKey();
    }

    public ExtendedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setBackKeyListener(BackKeyListener backKeyListener) {
        this.backKeyListener = backKeyListener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backKeyListener != null) backKeyListener.onBackKey();
            // User has pressed Back key. So hide the keyboard
            //InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            //mgr.hideSoftInputFromWindow(this.getWindowToken(), 0);
            return true;
        }
        else {
            return super.onKeyPreIme(keyCode, event);
        }
    }
}
