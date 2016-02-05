package com.android.myapidemo.smartisan.browse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.util.AttributeSet;
import android.view.View;

public class NavThumbnailView extends View{
    private boolean mIncogState;
    private Bitmap mBitmap;
    private Bitmap mBitmapTemp;
    private Canvas mCanvas;
    public NavThumbnailView(Context context) {
        super(context);
    }
    public NavThumbnailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public NavThumbnailView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setIncogState(boolean isIncogState) {
        mIncogState = isIncogState;
    }
    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        invalidate();
    }
    public Bitmap getDrawable() {
        return mBitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap == null || mBitmap.isRecycled()) {
            return;
        }
        Bitmap roundCornerBitmap = toRoundCorner(mBitmap, 8);
        canvas.drawBitmap(roundCornerBitmap, 0, 0, null);
        roundCornerBitmap.recycle();
    }

    private Bitmap toRoundCorner(Bitmap bitmap, int pixels) {
        if (mBitmapTemp == null || mBitmapTemp.isRecycled()) {
            try {
                mBitmapTemp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
            } catch (Exception e) {
                return null;
            }
        }
        mCanvas = new Canvas(mBitmapTemp);
        int color = 0xffB2B5B9;
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        mCanvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        Rect rectS = new Rect(0, 0, bitmap.getWidth(), 10);
        mCanvas.drawRect(rectS, paint);
        mCanvas.drawRoundRect(rectF, pixels, pixels, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        mCanvas.drawBitmap(bitmap, rect, rect, paint);
        if (mIncogState) {
            return toGrayscale(mBitmapTemp);
        } else {
            return mBitmapTemp;
        }
    }

    // 黑白色
    private Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}
