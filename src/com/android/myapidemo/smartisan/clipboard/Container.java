package com.android.myapidemo.smartisan.clipboard;

import com.android.myapidemo.R;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

public class Container
        extends LinearLayout
        implements SwipeHelper.Callback {

    private SwipeHelper mSwipeHelper;
    private Callback mCallback;

    public Container(Context context) {
        super(context);

        float densityScale = getResources().getDisplayMetrics().density;
        float pagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
        mSwipeHelper = new SwipeHelper(SwipeHelper.X, this, densityScale, pagingTouchSlop);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mSwipeHelper.onInterceptTouchEvent(ev) ||
                super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mSwipeHelper.onTouchEvent(ev) ||
                super.onTouchEvent(ev);
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        return findViewById(R.id.child);
    }

    @Override
    public View getChildContentView(View v) {
        return v;
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        return true;
    }

    @Override
    public void onBeginDrag(View v) {
        requestDisallowInterceptTouchEvent(true);
        mCallback.stopTimer();
    }

    @Override
    public void onChildDismissed(View v) {
        mCallback.stopService();
    }

    @Override
    public void onDragCancelled(View v) {
        mCallback.startTimer();
    }

    public void init(Callback callback) {
        mCallback = callback;
    }

    public interface Callback {
        void stopService();

        void stopTimer();

        void startTimer();
    }
}
