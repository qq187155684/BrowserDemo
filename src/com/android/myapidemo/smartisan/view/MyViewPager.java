
package com.android.myapidemo.smartisan.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class MyViewPager extends ViewPager {
    private boolean isEditMode;
    private int scaledTouchSlop;
    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyViewPager(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas arg0) {
        super.onDraw(arg0);
    }

    private void init() {
        scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }
    private float preX;
    private float preY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isEditMode) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            preX = event.getX();
            preY = event.getY();
        } else {
            float curX = event.getX();
            float curY = event.getY();
            float distanceX = Math.abs(curX - preX);
            float distanceY = Math.abs(curY - preY);
            if (distanceX > scaledTouchSlop && (distanceY - distanceX) < 0) {
                return true;
            } else {
                preX = event.getX();
                preY = event.getY();
            }
        }
        return super.onInterceptTouchEvent(event);
    }
    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        if (isEditMode) {
            return false;
        }
        try {
            return super.onTouchEvent(arg0);
        } catch (IllegalArgumentException e) {
        }
        return false;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void setEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;
    }

}
