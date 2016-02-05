
package com.android.myapidemo.smartisan.clipboard;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;


public class FloatService extends Service {

    private WindowManager mWindowManager;
    private DisplayMetrics mDM;
    public Container mContainer;
    private WindowManager.LayoutParams mParams;
    private final static int BAR_HEIGHT = 101;

    public Handler handler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mParams.width = (int) (newConfig.screenWidthDp * mDM.density);
        mWindowManager.updateViewLayout(mContainer, mParams);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDM = new DisplayMetrics();

        mWindowManager.getDefaultDisplay().getMetrics(mDM);
        mContainer = new Container(this);

        mParams = new WindowManager.LayoutParams(
                mDM.widthPixels,
                (int) (BAR_HEIGHT * mDM.density),
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mParams.x = 0;
        mParams.y = 0;

        mWindowManager.addView(mContainer, mParams);
    }

    @Override
    public void onDestroy() {
        if (mContainer != null)
            mWindowManager.removeView(mContainer);

        super.onDestroy();
    }
}
