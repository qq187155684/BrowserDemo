
package com.android.myapidemo.smartisan.view;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

public class ResizeRelativeLayout extends RelativeLayout {
    private InputMethodListener mInputMethodListener;
    public ResizeRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ResizeRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResizeRelativeLayout(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // this not work if in fullscreen,so we not excute in landscape mode
        if (mInputMethodListener == null || Configuration.ORIENTATION_LANDSCAPE == getResources().getConfiguration().orientation) {
            return;
        }

        int height = getMeasuredHeight();
        int rootViewHeight = getRootView().getHeight();
        if (height > 0 && height < rootViewHeight * 2 / 3) {
            mInputMethodListener.inputMethodStateChange(true);
        } else {
            mInputMethodListener.inputMethodStateChange(false);
        }
    }

    public void setInputMethodListener(InputMethodListener listener) {
        mInputMethodListener = listener;
    }

    public interface InputMethodListener {
        public void inputMethodStateChange(boolean isShow);
    }

}
