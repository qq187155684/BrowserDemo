
package com.android.myapidemo.smartisan.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.android.myapidemo.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class AudioFocusView extends View {

    private boolean mCurrentAudioFocus = false;
    private long mCountTime = 0;
    private WeakReference<Bitmap> mBitmapRef;
    private ArrayList<Ripple> mRippleList = new ArrayList<Ripple>();
    private ArrayList<Ripple> mStaleList = new ArrayList<Ripple>();

    public AudioFocusView(Context context) {
        this(context, null);
    }

    public AudioFocusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioFocusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setAudioFocus(boolean focus) {
        if (mCurrentAudioFocus == focus) {
            return;
        }
        mCurrentAudioFocus = focus;
        setVisibility(mCurrentAudioFocus ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        mStaleList.clear();
        final long now = SystemClock.elapsedRealtime();
        if (now - mCountTime >= 400) {
            Ripple r = new Ripple();
            mRippleList.add(r);
            mCountTime = now;
        }

        for (int i = 0; i < mRippleList.size(); i++) {
            Ripple r = mRippleList.get(i);
            if (r.mTime >= Ripple.TOTALTIME) {
                mStaleList.add(r);
                continue;
            }
            r.update();
            r.draw(canvas);
        }

        for (int i = 0; i < mStaleList.size(); i++) {
            Ripple r = mStaleList.get(i);
            mRippleList.remove(r);
        }
        invalidate();
    }

    private Bitmap getCacheBitmap() {
        WeakReference<Bitmap> ref = mBitmapRef;
        Bitmap bitmap = null;

        if (ref != null)
            bitmap = ref.get();

        if (bitmap == null) {
            bitmap = getBitmap();
            mBitmapRef = new WeakReference<Bitmap>(bitmap);
        }
        return bitmap;
    }

    private Bitmap getBitmap() {
        return BitmapFactory.decodeResource(getResources(),
                R.drawable.recent_panel_sound_ring);
    }

    private int getBitmapWidth() {
        Bitmap bitmap = getCacheBitmap();
        if (bitmap != null) {
            return bitmap.getWidth();
        }
        return 0;
    }

    private int getBitmapHeight() {
        Bitmap bitmap = getCacheBitmap();
        if (bitmap != null) {
            return bitmap.getHeight();
        }
        return 0;
    }

    private class Ripple {
        public static final int TOTALTIME = 1000;

        private int mPWidth = getWidth();
        private int mPHeight = getHeight();
        private int mWidth = getBitmapWidth();
        private int mHeight = getBitmapHeight();
        private long mStartTime = 0;
        private float mAlpha = 0;
        private float mScale = 1;
        private float mFromAlpha = 255f;
        private float mToAlpha = 0f;
        private float mFromScale = 0.05f;
        private float mToScale = 1.3f;
        private Paint mPaint = new Paint();
        private LinearInterpolator mInterpolator = new LinearInterpolator();
        public long mTime = 0;

        public void draw(Canvas canvas) {
            float left = mPWidth / 2 - mWidth * mScale / 2;
            float top = mPHeight / 2 - mHeight * mScale / 2;
            float right = mPWidth / 2 + mWidth * mScale / 2;
            float bottom = mPHeight / 2 + mHeight * mScale / 2;
            RectF dstR = new RectF(left, top, right, bottom);
            mPaint.setAlpha((int) mAlpha);
            canvas.drawBitmap(getCacheBitmap(), null, dstR, mPaint);
        }

        public void update() {
            long now = SystemClock.elapsedRealtime();
            if (mStartTime == 0) {
                mStartTime = SystemClock.elapsedRealtime();
            }
            mTime = now - mStartTime;
            float r = mTime / (float) TOTALTIME;
            float t = mInterpolator.getInterpolation((r <= 1) ? r : 1);
            mAlpha = mFromAlpha + t * (mToAlpha - mFromAlpha);
            mScale = mFromScale + t * (mToScale - mFromScale);
        }
    }
}
