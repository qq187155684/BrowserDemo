/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.myapidemo.smartisan.browse;

import java.lang.reflect.Field;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.clipboard.ClipboardMonitor;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;
import com.android.myapidemo.smartisan.stub.NullController;

import java.util.Locale;


public class BrowserActivity extends Activity implements ViewTreeObserver.OnPreDrawListener {

    public static final String ACTION_SHOW_BOOKMARKS = "show_bookmarks";
    public static final String ACTION_SHOW_BROWSER = "show_browser";
    public static final String ACTION_RESTART = "--restart--";
    private static final String EXTRA_STATE = "state";
    public static final String EXTRA_DISABLE_URL_OVERRIDE = "disable_url_override";

    private final static String LOGTAG = "browser";

    private final static boolean LOGV_ENABLED = Browser.LOGV_ENABLED;

    private ActivityController mController = NullController.INSTANCE;
    private static final String SMARTISAN_SEARCH = "smartisan_search";
    private PhoneUi mUi;

    private Handler mHandler = new Handler();
    private final Locale mCurrentLocale = Locale.getDefault();

    private UiController mUiController;
    private Handler mHandlerEx = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
           if (mUiController != null) {
               WebView current = mUiController.getCurrentWebView();
               if (current != null) {
                   current.postInvalidate();
               }
           }
        }
    };

    private Bundle mSavedInstanceState;
    private EngineInitializer mEngineInitializer;

    @Override
    public void onCreate(Bundle icicle) {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, this + " onCreate, has state: "
                    + (icicle == null ? "false" : "true"));
        }
        super.onCreate(icicle);
        //Settings init
        BrowserSettings.initialize((Context) this);
        
        if (shouldIgnoreIntents()) {
            finish();
            return;
        }

        mEngineInitializer = EngineInitializer.getInstance();
        mEngineInitializer.onActivityCreate(BrowserActivity.this);

        // If this was a web search request, pass it on to the default web
        // search provider and finish this activity.
        boolean isQuickSearch = getIntent().getBooleanExtra(SMARTISAN_SEARCH, false);
        if (IntentHandler.handleWebSearchIntent(this, null, getIntent())
                && !isQuickSearch) {
            finish();
            return;
        }

        //Thread.setDefaultUncaughtExceptionHandler(new CrashLogExceptionHandler(this));

        mSavedInstanceState = icicle;
        // Create the initial UI views
        mController = createController();

        // Workaround for the black screen flicker on SurfaceView creation
        ViewGroup topLayout = (ViewGroup) findViewById(R.id.main_content);
        topLayout.requestTransparentRegion(topLayout);

        // Add pre-draw listener to start the controller after engine initialization.
        final ViewTreeObserver observer = getWindow().getDecorView().getViewTreeObserver();
        observer.addOnPreDrawListener(this);
        requestFullScreen(getResources().getConfiguration());
        mEngineInitializer.initializeResourceExtractor(this);
        onPreDraw();
    }

    @Override
    public boolean onPreDraw()
    {
        System.out.println("================ onPreDraw 111111 ================");
        final ViewTreeObserver observer = getWindow().getDecorView().getViewTreeObserver();
        observer.removeOnPreDrawListener(this);
        mEngineInitializer.onPreDraw();
        return true;
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    private Controller createController() {
        Controller controller = new Controller(this);
        System.out.println("================== createController ===================");
        mUi = new PhoneUi(this, controller);
        mUiController = mUi.getUiController();
        controller.setUi(mUi);
        return controller;
    }

    public void onEngineInitializationComplete() {
        Intent intent = (mSavedInstanceState == null) ? getIntent() : null;
        mController.start(intent);
    }

    //@VisibleForTesting
    //public to facilitate testing
    public Controller getController() {
        return (Controller) mController;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (shouldIgnoreIntents()) return;
        if (mEngineInitializer != null) {
            mEngineInitializer.onNewIntent(intent);
        }
        // Note: Do not add any more application logic in this method.
        //       Move any additional app logic into handleOnNewIntent().
    }

    protected void handleOnNewIntent(Intent intent) {
        if (ACTION_RESTART.equals(intent.getAction())) {
            Bundle outState = new Bundle();
            mController.onSaveInstanceState(outState);
            finish();
            getApplicationContext().startActivity(
                    new Intent(getApplicationContext(), BrowserActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_STATE, outState));
            return;
        }
        mController.handleNewIntent(intent);
    }

    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;

    private boolean shouldIgnoreIntents() {
        // Only process intents if the screen is on and the device is unlocked
        // aka, if we will be user-visible
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
        boolean ignore = !mPowerManager.isScreenOn();
        // If keyguard screen is showing or in restricted key input mode,
        // browser still can be triggered
        // ignore |= mKeyguardManager.inKeyguardRestrictedInputMode();
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "ignore intents: " + ignore);
        }
        return ignore;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mEngineInitializer.onActivityStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onResume: this=" + this);
        }
        //mEngineInitializer.onActivityResume();
        // Note: Do not add any more application logic in this method.
        //       Move any additional app logic into handleOnResume().
    }

    protected void handleOnResume() {
        // Note: Intentionally left blank.
        mController.onResume();
    }

    protected void handleOnStart() {
        Object isPowerSaveMode = ReflectHelper.invokeMethod(mPowerManager, "isPowerSaveMode", null, null);
        if (isPowerSaveMode != null)
            ((Browser) getApplication()).mPowerSave = (Boolean)isPowerSaveMode;
        if (BrowserSettings.getInstance().isMonitorClipBoard())
            startService(new Intent(this, ClipboardMonitor.class));
        try {
            mUi.getActiveTab().getWebView().setVisibility(View.VISIBLE);
        } catch (Exception e) {}
    }

    @Override
    protected void onStop() {
        mEngineInitializer.onActivityStop();
        super.onStop();
        // Note: Do not add any more application logic in this method.
        //       Move any additional app logic into handleOnStop().
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (Window.FEATURE_OPTIONS_PANEL == featureId) {
            mController.onMenuOpened(featureId, menu);
        }
        return true;
    }

    public void requestFullScreen(Configuration configuration) {
        if(configuration == null){
            configuration = getResources().getConfiguration();
        }
        Window win = getWindow();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * onSaveInstanceState(Bundle map)
     * onSaveInstanceState is called right before onStop(). The map contains
     * the saved state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onSaveInstanceState: this=" + this);
        }
        mController.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        mEngineInitializer.onActivityPause();
        super.onPause();
        // Note: Do not add any more application logic in this method.
        //       Move any additional app logic into handleOnPause().
    }

    protected void handleOnPause() {
        mController.pauseVideo();
        mController.onPause();
    }

    protected void handleOnStop() {
        //CookieManager.getInstance().flushCookieStore();
        try {
            mUi.getActiveTab().pause();
            mUi.getActiveTab().getWebView().setVisibility(View.GONE);
        } catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onDestroy: this=" + this);
        }
        super.onDestroy();
        // mEngineInitializer can be null if onCreate is not called before onDestroy
        // it happens when starting the activity with an intent while the screen is locked.
        if (mEngineInitializer != null)
            mEngineInitializer.onActivityDestroy();
        mController.onDestroy();
        mController = NullController.INSTANCE;
        if (!Locale.getDefault().equals(mCurrentLocale)) {
            Log.e(LOGTAG,"Force Killing Browser on locale change");
            Process.killProcess(Process.myPid());
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        requestFullScreen(newConfig);
        mController.onConfgurationChanged(newConfig);

        //For avoiding bug CR520353 temporarily, delay 300ms to refresh WebView.
        mHandlerEx.postDelayed(runnable, 300);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mController.onLowMemory();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mController.onKeyDown(keyCode, event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mController.onKeyLongPress(keyCode, event)
                || super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mController.onKeyUp(keyCode, event)
                || super.onKeyUp(keyCode, event);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        mController.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        mController.onActionModeFinished(mode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        mEngineInitializer.onActivityResult(requestCode, resultCode, intent);
    }

    protected void handleOnActivityResult (int requestCode, int resultCode, Intent intent) {
        mController.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onSearchRequested() {
        return mController.onSearchRequested();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mController.dispatchKeyEvent(event)
                || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return mController.dispatchKeyShortcutEvent(event)
                || super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if ((ev.getAction() == MotionEvent.ACTION_MOVE || ev.getAction() ==
                MotionEvent.ACTION_UP)
                && mController.isDialogShowing()) {
            return false;
        }
        return mController.dispatchTouchEvent(ev)
                || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return mController.dispatchTrackballEvent(ev)
                || super.dispatchTrackballEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return mController.dispatchGenericMotionEvent(ev)
                || super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        mController.onOptionsMenuClosed(menu);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        mController.onContextMenuClosed(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        mController.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return mController.onContextItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        if (getIntent() != null) {
            try {
                Field EXTRA_SMARTISAN_ANIM_RESOURCE_ID = Intent.class.getDeclaredField("EXTRA_SMARTISAN_ANIM_RESOURCE_ID");
                int[] anims = getIntent().getIntArrayExtra((String) EXTRA_SMARTISAN_ANIM_RESOURCE_ID.get(this));
                if (anims != null) {
                    overridePendingTransition(anims[0], anims[1]);
                }
            } catch(Exception e) {}
        }
    }
}
