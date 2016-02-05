
package com.android.myapidemo.smartisan.clipboard;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Service;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.util.Patterns;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.myapidemo.smartisan.browse.Browser;

/**
 * Starts a background thread to monitor the states of clipboard
 */
public class ClipboardMonitor extends Service {

    Browser mApp;
    OnPrimaryClipChangedListener mListener;
    ClipboardManager mCM;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mCM.removePrimaryClipChangedListener(mListener);

        super.onDestroy();
    }

    Handler mHandler = new Handler();
    Runnable mTranslateRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                String text = mCM.getPrimaryClip().getItemAt(0).getText().toString();
                // don't double copy
                if (!TextUtils.isEmpty(text)) {
                    ArrayList<LinkSpec> links = new ArrayList<LinkSpec>();
                    gatherLinks(links, text, Patterns.WEB_URL,
                            new String[] { "http://", "https://", "rtsp://" },
                            Linkify.sUrlMatchFilter, null);
                    if (links.isEmpty())
                        return;
                    mApp.mUrl = links.get(0).url;
                    stopService(new Intent(ClipboardMonitor.this, Floater.class));
                    startService(new Intent(ClipboardMonitor.this, Floater.class));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private static final void gatherLinks(ArrayList<LinkSpec> links,
            String s, Pattern pattern, String[] schemes,
            MatchFilter matchFilter, TransformFilter transformFilter) {
        Matcher m = pattern.matcher(s);

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (matchFilter == null || matchFilter.acceptMatch(s, start, end)) {
                LinkSpec spec = new LinkSpec();
                String url = makeUrl(m.group(0), schemes, m, transformFilter);

                spec.url = url;
                spec.start = start;
                spec.end = end;

                links.add(spec);
            }
        }
    }

    private static final String makeUrl(String url, String[] prefixes,
            Matcher m, TransformFilter filter) {
        if (filter != null) {
            url = filter.transformUrl(m, url);
        }

        boolean hasPrefix = false;

        for (int i = 0; i < prefixes.length; i++) {
            if (url.regionMatches(true, 0, prefixes[i], 0,
                                  prefixes[i].length())) {
                hasPrefix = true;

                // Fix capitalization if necessary
                if (!url.regionMatches(false, 0, prefixes[i], 0,
                                       prefixes[i].length())) {
                    url = prefixes[i] + url.substring(prefixes[i].length());
                }

                break;
            }
        }

        if (!hasPrefix) {
            url = prefixes[0] + url;
        }

        return url;
    }

    private static boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    //Log.d("===========Background", appProcess.processName);
                    return true;
                } else {
                    //Log.d("===========Foreground", appProcess.processName);
                    return false;
                }
            }
        }
        return true;
    }

    private final static boolean isScreenLocked(Context c) {
        android.app.KeyguardManager mKeyguardManager = (KeyguardManager) c.getSystemService(c.KEYGUARD_SERVICE);
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    @Override
    public void onCreate() {
        mApp = (Browser) getApplication();
        mCM = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        mListener = new OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                if (isBackground(mApp) && !mApp.mPowerSave && !isScreenLocked(mApp) && mCM.hasPrimaryClip()) {
                    if (mCM.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                            mCM.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                        mHandler.removeCallbacks(mTranslateRunnable);
                        mHandler.postDelayed(mTranslateRunnable, 150);
                    }
                }
            }
        };

        mCM.addPrimaryClipChangedListener(mListener);
        startForeground(0, new Notification());
    }
}

class LinkSpec {
    String url;
    int start;
    int end;
}