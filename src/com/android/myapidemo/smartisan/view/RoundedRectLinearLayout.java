package com.android.myapidemo.smartisan.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class RoundedRectLinearLayout extends LinearLayout {
    private static final float DEFAULT_RADIUS = 17;
    private float radius = DEFAULT_RADIUS;
    private Path mClip;
    public RoundedRectLinearLayout(Context context) {
        super(context);
    }

    public RoundedRectLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundedRectLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mClip = new Path();
        RectF rectRound = new RectF(0, 0, w, h);
        mClip.addRoundRect(rectRound, radius, radius, Direction.CW);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int saveCount = canvas.save();
        canvas.clipPath(mClip);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saveCount);
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}
