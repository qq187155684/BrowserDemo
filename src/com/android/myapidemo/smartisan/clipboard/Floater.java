
package com.android.myapidemo.smartisan.clipboard;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.Browser;
import com.android.myapidemo.smartisan.clipboard.Container.Callback;

public class Floater extends FloatService {
    private Browser mApp;
    private final static int DELAY = 5000;

    /***********************************************************
     * copied from smartisan os systemui. may change in future.*
     ***********************************************************/
    private static final String STATUS_BAR_EXPEND = "status_bar_expanded";
    private static final int STATUS_BAR_COLLAPSE = 0;
    //private static final int STATUS_BAR_EXPANDING = 1;
    //private static final int STATUS_BAR_EXPANDED = 2;

    private class StatusBarObserver extends ContentObserver {
        public StatusBarObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            int state = Settings.Global.getInt(
                    getContentResolver(), STATUS_BAR_EXPEND, STATUS_BAR_COLLAPSE);
            if (state != STATUS_BAR_COLLAPSE) {
                stopService(new Intent(mApp, Floater.class));
            }
        }
    }

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            stopService(new Intent(mApp, Floater.class));
        }
    };

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mRunnable);
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = (Browser) getApplication();

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.banner, mContainer);

        TextView tUrl = (TextView) mContainer.findViewById(R.id.url);
        tUrl.setText(mApp.mUrl);

        LinearLayout child = (LinearLayout) mContainer.findViewById(R.id.child);
        child.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mApp.mUrl == null)
                    return;

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse(mApp.mUrl));
                intent.setPackage(mApp.getPackageName());
                mApp.startActivity(intent);
                stopService(new Intent(mApp, Floater.class));
            }
        });

        mHandler.postDelayed(mRunnable, DELAY);

        getContentResolver().registerContentObserver(Settings.Global.getUriFor(STATUS_BAR_EXPEND),
                        true, new StatusBarObserver(new Handler()));

        mContainer.init(new Callback() {
            @Override
            public void stopService() {
                Floater.this.stopService(new Intent(mApp, Floater.class));
            }

            @Override
            public void stopTimer() {
                mHandler.removeCallbacks(mRunnable);
            }

            @Override
            public void startTimer() {
                mHandler.removeCallbacks(mRunnable);
                mHandler.postDelayed(mRunnable, DELAY);
            }
        });
    }

}
