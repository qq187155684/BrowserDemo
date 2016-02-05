/*
 * Copyright (C) 2011 The Android Open Source Project
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

//import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebStorage;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserConfig;
import com.android.myapidemo.smartisan.browser.provider.BrowserProvider;
import com.android.myapidemo.smartisan.browser.util.Constants;
import com.android.myapidemo.smartisan.preferences.PreferenceKeys;
import com.android.myapidemo.smartisan.search.SearchEngine;
import com.android.myapidemo.smartisan.search.SearchEngines;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Class for managing settings
 */
public class BrowserSettings implements OnSharedPreferenceChangeListener,
        PreferenceKeys {

    private static final String TAG = "BrowserSettings";
    // The minimum min font size
    // Aka, the lower bounds for the min font size range
    // which is 1:5..24
    private static final int MIN_FONT_SIZE_OFFSET = 5;
    // The initial value in the text zoom range
    // This is what represents 100% in the SeekBarPreference range
    private static final int TEXT_ZOOM_START_VAL = 10;
    // The size of a single step in the text zoom range, in percent
    private static final int TEXT_ZOOM_STEP = 5;
    // The initial value in the double tap zoom range
    // This is what represents 100% in the SeekBarPreference range
    private static final int DOUBLE_TAP_ZOOM_START_VAL = 5;
    // The size of a single step in the double tap zoom range, in percent
    private static final int DOUBLE_TAP_ZOOM_STEP = 5;

    private static final int CACHE_INDEX_COUNT = 2;

    private static BrowserSettings sInstance;

    private Context mContext;
    private SharedPreferences mPrefs;
    private static final String CACHE_PATH = "/app_swe_webview/Cache";
    private LinkedList<WeakReference<WebSettings>> mManagedSettings;
    private Controller mController;
    private WebStorageSizeManager mWebStorageSizeManager;
//    private AutofillHandler mAutofillHandler;
    private static boolean sInitialized = false;
    private boolean mNeedsSharedSync = true;
    private float mFontSizeMult = 1.0f;

    // Current state of network-dependent settings
    private boolean mLinkPrefetchAllowed = true;

    // Cached values
    private int mPageCacheCapacity = 1;
    private String mAppCachePath;
    private String mPath;

    // Cached settings
    private SearchEngine mSearchEngine;

    private static String sFactoryResetUrl;

    private boolean mEngineInitialized = false;
    private boolean mSyncManagedSettings = false;

    public static synchronized void initialize(final Context context) {
        System.out.println("================== BrowserSettings initialize =====================");
        if (sInstance == null)
            sInstance = new BrowserSettings(context);
    }

    public static BrowserSettings getInstance() {
        System.out.println("================ BrowserSettings sInstance ===================="+sInstance);
        return sInstance;
    }

    private BrowserSettings(Context context) {
        mContext = context.getApplicationContext();
        mPath = mContext.getApplicationContext().getApplicationInfo().dataDir+CACHE_PATH;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mManagedSettings = new LinkedList<WeakReference<WebSettings>>();
        BackgroundHandler.execute(mSetup);
    }

    public void setController(Controller controller) {
        mController = controller;
        mNeedsSharedSync = true;
    }

    public void onEngineInitializationComplete() {
        mEngineInitialized = true;
//        mAutofillHandler = new AutofillHandler(mContext);
        if (mSyncManagedSettings) {
            syncManagedSettings();
        }
        if (mNeedsSharedSync) {
            syncSharedSettings();
        }
    }

    public void startManagingSettings(final WebSettings settings) {

        if (mNeedsSharedSync) {
            syncSharedSettings();
        }

        synchronized (mManagedSettings) {
            syncStaticSettings(settings);
            syncSetting(settings);
            mManagedSettings.add(new WeakReference<WebSettings>(settings));
        }
    }

    public void stopManagingSettings(WebSettings settings) {
        Iterator<WeakReference<WebSettings>> iter = mManagedSettings.iterator();
        while (iter.hasNext()) {
            WeakReference<WebSettings> ref = iter.next();
            if (ref.get() == settings) {
                iter.remove();
                return;
            }
        }
    }

    private Runnable mSetup = new Runnable() {

        @Override
        public void run() {
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            mFontSizeMult = metrics.scaledDensity / metrics.density;
            // the cost of one cached page is ~3M (measured using nytimes.com). For
            // low end devices, we only cache one page. For high end devices, we try
            // to cache more pages, currently choose 5.
            // TODO: assume a high-memory device
            //if (ActivityManager.staticGetMemoryClass() > 16) {
                mPageCacheCapacity = 5;
            //}
            mWebStorageSizeManager = new WebStorageSizeManager(mContext,
                    new WebStorageSizeManager.StatFsDiskInfo(getAppCachePath()),
                    new WebStorageSizeManager.WebKitAppCacheInfo(getAppCachePath()));
            // Workaround b/5254577
            mPrefs.registerOnSharedPreferenceChangeListener(BrowserSettings.this);
            if (Build.VERSION.CODENAME.equals("REL")) {
                // This is a release build, always startup with debug disabled
                setDebugEnabled(false);
            }
            if (mPrefs.contains(PREF_TEXT_SIZE)) {
                /*
                 * Update from TextSize enum to zoom percent
                 * SMALLEST is 50%
                 * SMALLER is 75%
                 * NORMAL is 100%
                 * LARGER is 150%
                 * LARGEST is 200%
                 */
//                switch (getTextSize()) {
//                    case SMALLEST:
//                        setTextZoom(50);
//                        break;
//                    case SMALLER:
//                        setTextZoom(75);
//                        break;
//                    case LARGER:
//                        setTextZoom(150);
//                        break;
//                    case LARGEST:
//                        setTextZoom(200);
//                        break;
//                }
                mPrefs.edit().remove(PREF_TEXT_SIZE).apply();
            }

            // sFactoryResetUrl = mContext.getResources().getString(R.string.search_hint);
            sFactoryResetUrl = "";

            if (!mPrefs.contains(PREF_DEFAULT_TEXT_ENCODING)) {
                mPrefs.edit().putString(PREF_DEFAULT_TEXT_ENCODING, "auto").apply();
            }

            if (sFactoryResetUrl.indexOf("{CID}") != -1) {
                sFactoryResetUrl = sFactoryResetUrl.replace("{CID}",
                        BrowserProvider.getClientId(mContext.getContentResolver()));
            }

            synchronized (BrowserSettings.class) {
                sInitialized = true;
                BrowserSettings.class.notifyAll();
            }
        }
    };

    private static void requireInitialization() {
        synchronized (BrowserSettings.class) {
            while (!sInitialized) {
                try {
                    BrowserSettings.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Syncs all the settings that have a Preference UI
     */
    private void syncSetting(WebSettings settings) {
        settings.setGeolocationEnabled(enableGeolocation());
        settings.setJavaScriptEnabled(enableJavascript());
        settings.setLightTouchEnabled(enableLightTouch());
        //settings.setNavDump(enableNavDump());
        settings.setDefaultTextEncodingName(getDefaultTextEncoding());
        settings.setMinimumFontSize(getMinimumFontSize());
        settings.setMinimumLogicalFontSize(getMinimumFontSize());
        settings.setTextZoom(getTextZoom());
        //settings.setLayoutAlgorithm(getLayoutAlgorithm());
        settings.setJavaScriptCanOpenWindowsAutomatically(!blockPopupWindows());
        settings.setLoadsImagesAutomatically(loadImages());
        settings.setLoadWithOverviewMode(loadPageInOverviewMode());
        settings.setSavePassword(rememberPasswords());
        settings.setSaveFormData(saveFormdata());
        settings.setUseWideViewPort(isWideViewport());
        //settings.setDoNotTrack(doNotTrack());
        settings.setMediaPlaybackRequiresUserGesture(false);
        //settings.setAllowMediaDownloads(allowMediaDownloads());
        setExtraHTTPRequestHeaders(settings);

        WebSettings settingsClassic = (WebSettings) settings;
//        settingsClassic.setHardwareAccelSkiaEnabled(isSkiaHardwareAccelerated());
//        settingsClassic.setShowVisualIndicator(enableVisualIndicator());
//        settingsClassic.setForceUserScalable(forceEnableUserScalable());
//        settingsClassic.setDoubleTapZoom(getDoubleTapZoom());
//        settingsClassic.setAutoFillEnabled(isAutofillEnabled());

        boolean useInverted = useInvertedRendering();
//        settingsClassic.setProperty(WebViewProperties.gfxInvertedScreen,
//                useInverted ? "true" : "false");
        if (useInverted) {
//          settingsClassic.setProperty(WebViewProperties.gfxInvertedScreenContrast,
//                    Float.toString(getInvertedContrast()));
        }

        if (isDebugEnabled()) {
//          settingsClassic.setProperty(WebViewProperties.gfxEnableCpuUploadPath,
//                    enableCpuUploadPath() ? "true" : "false");
        }

        //settingsClassic.setLinkPrefetchEnabled(mLinkPrefetchAllowed);
    }

    private void setExtraHTTPRequestHeaders(WebSettings settings){
        String headers = mContext.getResources().getString(R.string.def_extra_http_headers);
        if (!TextUtils.isEmpty(headers)){
            //settings.setHTTPRequestHeaders(headers);
        }
    }

    /**
     * Syncs all the settings that have no UI These cannot change, so we only
     * need to set them once per WebSettings
     */
    private void syncStaticSettings(WebSettings settings) {
        settings.setDefaultFontSize(16);
        settings.setDefaultFixedFontSize(13);

        // WebView inside Browser doesn't want initial focus to be set.
        settings.setNeedInitialFocus(false);
        // Browser supports multiple windows
        settings.setSupportMultipleWindows(true);
        // enable smooth transition for better performance during panning or
        // zooming
        settings.setEnableSmoothTransition(true);
        // enable content url access
        settings.setAllowContentAccess(true);

        // HTML5 API flags
        settings.setAppCacheEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);

        // HTML5 configuration parametersettings.
        settings.setAppCacheMaxSize(getWebStorageSizeManager().getAppCacheMaxSize());
        settings.setAppCachePath(getAppCachePath());
        settings.setDatabasePath(mContext.getDir("databases", 0).getPath());
        settings.setGeolocationDatabasePath(mContext.getDir("geolocation", 0).getPath());
        // origin policy for file access
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setAllowFileAccessFromFileURLs(false);
        //settings.setFullscreenSupported(true);

        //if (!(settings instanceof WebSettingsClassic)) return;
        /*

        WebSettingsClassic settingsClassic = (WebSettingsClassic) settings;
        settingsClassic.setPageCacheCapacity(getPageCacheCapacity());
        // WebView should be preserving the memory as much as possible.
        // However, apps like browser wish to turn on the performance mode which
        // would require more memory.
        // TODO: We need to dynamically allocate/deallocate temporary memory for
        // apps which are trying to use minimal memory. Currently, double
        // buffering is always turned on, which is unnecessary.
        settingsClassic.setProperty(WebViewProperties.gfxUseMinimalMemory, "false");
        settingsClassic.setWorkersEnabled(true);  // This only affects V8.
        */
    }

    private void syncSharedSettings() {
        mNeedsSharedSync = false;
        CookieManager.getInstance().setAcceptCookie(acceptCookies());
        if (mController != null) {
            mController.setShouldShowErrorConsole(enableJavascriptConsole());
        }
    }

    private void syncManagedSettings() {
        if (!mEngineInitialized) {
            mSyncManagedSettings = true;
            return;
        }
        mSyncManagedSettings = false;
        syncSharedSettings();
        synchronized (mManagedSettings) {
            Iterator<WeakReference<WebSettings>> iter = mManagedSettings.iterator();
            while (iter.hasNext()) {
                WeakReference<WebSettings> ref = iter.next();
                WebSettings settings = (WebSettings)ref.get();
                if (settings == null) {
                    iter.remove();
                    continue;
                }
                syncSetting(settings);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        syncManagedSettings();
        if (PREF_SEARCH_ENGINE.equals(key)) {
            updateSearchEngine(false);
        } else if (PREF_FULLSCREEN.equals(key)) {
            if (mController != null && mController.getUi() != null) {
                mController.getUi().setFullscreen(useFullscreen());
            }
        } else if (PREF_ENABLE_QUICK_CONTROLS.equals(key)) {
//            if (mController != null && mController.getUi() != null) {
//                mController.getUi().setUseQuickControls(sharedPreferences.getBoolean(key, false));
//            }
        } else if (PREF_LINK_PREFETCH.equals(key)) {
            updateConnectionType();
        }
    }

    public static String getFactoryResetHomeUrl(Context context) {
        requireInitialization();
        return sFactoryResetUrl;
    }

    public LayoutAlgorithm getLayoutAlgorithm() {
        LayoutAlgorithm layoutAlgorithm = LayoutAlgorithm.NORMAL;
        if (autofitPages()) {
            layoutAlgorithm = LayoutAlgorithm.NARROW_COLUMNS;
        }
        if (isDebugEnabled()) {
            if (isSmallScreen()) {
                layoutAlgorithm = LayoutAlgorithm.SINGLE_COLUMN;
            } else {
                if (isNormalLayout()) {
                    layoutAlgorithm = LayoutAlgorithm.NORMAL;
                } else {
                    layoutAlgorithm = LayoutAlgorithm.NARROW_COLUMNS;
                }
            }
        }
        return layoutAlgorithm;
    }

    public int getPageCacheCapacity() {
        requireInitialization();
        return mPageCacheCapacity;
    }

    public WebStorageSizeManager getWebStorageSizeManager() {
        requireInitialization();
        return mWebStorageSizeManager;
    }

    private String getAppCachePath() {
        if (mAppCachePath == null) {
            mAppCachePath = mContext.getDir("appcache", 0).getPath();
        }
        return mAppCachePath;
    }

    private void updateSearchEngine(boolean force) {
        String searchEngineName = getSearchEngineName();
        if (force || mSearchEngine == null ||
                !mSearchEngine.getName().equals(searchEngineName)) {
            mSearchEngine = SearchEngines.get(mContext, searchEngineName);
        }
    }

    public SearchEngine getSearchEngine() {
        if (mSearchEngine == null) {
            updateSearchEngine(false);
        }
        return mSearchEngine;
    }

    public boolean isDebugEnabled() {
        requireInitialization();
        return mPrefs.getBoolean(PREF_DEBUG_MENU, false);
    }

    public void setDebugEnabled(boolean value) {
        Editor edit = mPrefs.edit();
        edit.putBoolean(PREF_DEBUG_MENU, value);
        if (!value) {
            // Reset to "safe" value
            edit.putBoolean(PREF_ENABLE_HARDWARE_ACCEL_SKIA, false);
        }
        edit.apply();
    }

    public boolean isMaxTabsPrompt() {
        return mPrefs.getBoolean(PREF_MAX_TABS_IS_PROMPT, false);
    }

    public void setMaxTabsPrompt(boolean value) {
        Editor edit = mPrefs.edit();
        edit.putBoolean(PREF_MAX_TABS_IS_PROMPT, value);
        edit.apply();
    }

    public void clearCache() {
        if (mController != null) {
//            WebView current = mController.getCurrentWebView();
//            if (current != null) {
//                current.clearCache(true);
//            }
        }
    }

    public void clearCookies() {
        CookieManager.getInstance().removeAllCookie();
    }

    public void clearHistory() {
        ContentResolver resolver = mContext.getContentResolver();
//        Browser.clearHistory(resolver);
//        Browser.clearSearches(resolver);
        DataController.getInstance(mContext).clearState(null);
        if (mController == null)
            return;
        List<Tab> tabs = mController.getTabs();
        for (Tab tab : tabs) {
            WebView wv = tab.getWebView();
            if (wv != null)
                wv.clearHistory();
        }
        mController.getUi().setBackForwardBtn();
    }

    public void clearFormData() {
        WebViewDatabase.getInstance(mContext).clearFormData();
        if (mController != null) {
            //WebView currentTopView = mController.getCurrentTopWebView();
//            if (currentTopView != null) {
//                currentTopView.clearFormData();
//            }
        }
    }

    public WebView getTopWebView(){
        if (mController!= null)
            return mController.getCurrentTopWebView();

        return null;
    }

    public void clearPasswords() {
        // Clear password store maintained by SWE engine
        WebSettings settings = null;
        // find a valid settings object
        Iterator<WeakReference<WebSettings>> iter = mManagedSettings.iterator();
        while (iter.hasNext()) {
            WeakReference<WebSettings> ref = iter.next();
            settings = (WebSettings)ref.get();
            if (settings != null) {
                break;
            }
        }
//        if (settings != null) {
//            settings.clearPasswords();
//        }

        // Clear passwords in WebView database
        WebViewDatabase db = WebViewDatabase.getInstance(mContext);
        //db.clearUsernamePassword();
        db.clearHttpAuthUsernamePassword();
    }

    public void clearDatabases() {
        WebStorage.getInstance().deleteAllData();
    }

    public void clearLocationAccess() {
        GeolocationPermissions.getInstance().clearAll();
        /*if (GeolocationPermissions.isIncognitoCreated()) {
            GeolocationPermissions.getIncognitoInstance().clearAll();
        }*/
    }

    public void resetDefaultPreferences() {
        // Preserve autologin setting
        /*long gal = mPrefs.getLong(GoogleAccountLogin.PREF_AUTOLOGIN_TIME, -1);
        mPrefs.edit()
                .clear()
                .putLong(GoogleAccountLogin.PREF_AUTOLOGIN_TIME, gal)
                .apply();*/
        resetCachedValues();
        syncManagedSettings();
    }

    private void resetCachedValues() {
        updateSearchEngine(false);
    }
/*
    public AutoFillProfile getAutoFillProfile() {
         // query the profile from components autofill database 524
        if (mAutofillHandler.mAutoFillProfile == null &&
               !mAutofillHandler.mAutoFillActiveProfileId.equals("")) {
            WebSettings settings = null;
            // find a valid settings object
            Iterator<WeakReference<WebSettings>> iter = mManagedSettings.iterator();
            while (iter.hasNext()) {
                WeakReference<WebSettings> ref = iter.next();
                settings = (WebSettings)ref.get();
                if (settings != null) {
                    break;
                }
            }
            if (settings != null) {
                AutoFillProfile profile =
                    settings.getAutoFillProfile(mAutofillHandler.mAutoFillActiveProfileId);
                mAutofillHandler.setAutoFillProfile(profile);
            }
        }
        return mAutofillHandler.getAutoFillProfile();
    }

    public String getAutoFillProfileId() {
        return mAutofillHandler.getAutoFillProfileId();
    }

    public void updateAutoFillProfile(AutoFillProfile profile) {
         syncAutoFillProfile(profile);
    }

    private void syncAutoFillProfile(AutoFillProfile profile) {
       synchronized (mManagedSettings) {
            Iterator<WeakReference<WebSettings>> iter = mManagedSettings.iterator();
            while (iter.hasNext()) {
                WeakReference<WebSettings> ref = iter.next();
                WebSettings settings = (WebSettings)ref.get();
                if (settings == null) {
                    iter.remove();
                    continue;
                }
                // update the profile only once.
                settings.setAutoFillProfile(profile);
                // Now we should have the guid
                mAutofillHandler.setAutoFillProfile(profile);
                break;
            }
        }
    }
*/
    public void toggleDebugSettings() {
        setDebugEnabled(!isDebugEnabled());
    }

    public boolean hasDesktopUseragent(WebView view) {
        return view != null ;//&& view.getUseDesktopUserAgent();
    }

    public void toggleDesktopUseragent(WebView view) {
        if (view == null) {
            return;
        }
        /*if (hasDesktopUseragent(view))
            view.setUseDesktopUserAgent(false, true);
        else
            view.setUseDesktopUserAgent(true, true);*/
    }

    public static int getAdjustedMinimumFontSize(int rawValue) {
        rawValue++; // Preference starts at 0, min font at 1
        if (rawValue > 1) {
            rawValue += (MIN_FONT_SIZE_OFFSET - 2);
        }
        return rawValue;
    }

    public int getAdjustedTextZoom(int rawValue) {
        rawValue = (rawValue - TEXT_ZOOM_START_VAL) * TEXT_ZOOM_STEP;
        return (int) ((rawValue + 100) * mFontSizeMult);
    }

    static int getRawTextZoom(int percent) {
        return (percent - 100) / TEXT_ZOOM_STEP + TEXT_ZOOM_START_VAL;
    }

    public int getAdjustedDoubleTapZoom(int rawValue) {
        rawValue = (rawValue - DOUBLE_TAP_ZOOM_START_VAL) * DOUBLE_TAP_ZOOM_STEP;
        return (int) ((rawValue + 100) * mFontSizeMult);
    }

    static int getRawDoubleTapZoom(int percent) {
        return (percent - 100) / DOUBLE_TAP_ZOOM_STEP + DOUBLE_TAP_ZOOM_START_VAL;
    }

    public SharedPreferences getPreferences() {
        return mPrefs;
    }

    // update connectivity-dependent options
    public void updateConnectionType() {
        ConnectivityManager cm = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        String linkPrefetchPreference = getLinkPrefetchEnabled();
        boolean linkPrefetchAllowed = linkPrefetchPreference.
                equals(getLinkPrefetchAlwaysPreferenceString(mContext));
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            switch (ni.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_ETHERNET:
                case ConnectivityManager.TYPE_BLUETOOTH:
                    linkPrefetchAllowed |= linkPrefetchPreference.
                            equals(getLinkPrefetchOnWifiOnlyPreferenceString(mContext));
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_MOBILE_DUN:
                case ConnectivityManager.TYPE_MOBILE_MMS:
                case ConnectivityManager.TYPE_MOBILE_SUPL:
                case ConnectivityManager.TYPE_WIMAX:
                default:
                    break;
            }
        }
        if (mLinkPrefetchAllowed != linkPrefetchAllowed) {
            mLinkPrefetchAllowed = linkPrefetchAllowed;
            syncManagedSettings();
        }
    }

    // -----------------------------
    // getter/setters for accessibility_preferences.xml
    // -----------------------------

    @Deprecated
    private TextSize getTextSize() {
        String textSize = mPrefs.getString(PREF_TEXT_SIZE, "NORMAL");
        return TextSize.valueOf(textSize);
    }

    public int getMinimumFontSize() {
        int minFont = mPrefs.getInt(PREF_MIN_FONT_SIZE, 0);
        return getAdjustedMinimumFontSize(minFont);
    }

    public boolean forceEnableUserScalable() {
        return mPrefs.getBoolean(PREF_FORCE_USERSCALABLE, false);
    }

    public int getTextZoom() {
        requireInitialization();
        int textZoom = mPrefs.getInt(PREF_TEXT_ZOOM, 10);
        return getAdjustedTextZoom(textZoom);
    }

    public void setTextZoom(int percent) {
        mPrefs.edit().putInt(PREF_TEXT_ZOOM, getRawTextZoom(percent)).apply();
    }

    public int getDoubleTapZoom() {
        requireInitialization();
        int doubleTapZoom = mPrefs.getInt(PREF_DOUBLE_TAP_ZOOM, 5);
        return getAdjustedDoubleTapZoom(doubleTapZoom);
    }

    public void setDoubleTapZoom(int percent) {
        mPrefs.edit().putInt(PREF_DOUBLE_TAP_ZOOM, getRawDoubleTapZoom(percent)).apply();
    }

    // -----------------------------
    // getter/setters for advanced_preferences.xml
    // -----------------------------

    public String getSearchEngineName() {
        if (mController == null)
            return mPrefs.getString(PREF_SEARCH_ENGINE, SearchEngine.BAIDU);

        if ("CN".equals(((BaseUi)mController.getUi()).mFeatureRegon))
            return mPrefs.getString(PREF_SEARCH_ENGINE, SearchEngine.BAIDU);
        else
            return mPrefs.getString(PREF_SEARCH_ENGINE, SearchEngine.GOOGLE);
    }

    public boolean allowAppTabs() {
        return mPrefs.getBoolean(PREF_ALLOW_APP_TABS, false);
    }

    public boolean openInBackground() {
        return mPrefs.getBoolean(PREF_OPEN_IN_BACKGROUND, false);
    }

    public boolean enableJavascript() {
        return mPrefs.getBoolean(PREF_ENABLE_JAVASCRIPT, true);
    }

    public boolean enableMemoryMonitor() {
        return mPrefs.getBoolean(PREF_ENABLE_MEMORY_MONITOR, true);
    }

    public boolean allowMediaDownloads() {
        // Return false if preference is not exposed to user
/*        if (!BrowserConfig.getInstance(mContext)
                .hasFeature(BrowserConfig.Feature.ALLOW_MEDIA_DOWNLOADS))
            return false;*/

        // Otherwise, look at default value
        boolean defaultAllowMediaDownloadsValue = mController.getContext()
                .getResources().getBoolean(R.bool.def_allow_media_downloads);

        // If preference is not saved, save default value
        if (!mPrefs.contains(PREF_ALLOW_MEDIA_DOWNLOADS)){
            Editor edit = mPrefs.edit();
            edit.putBoolean(PREF_ALLOW_MEDIA_DOWNLOADS, defaultAllowMediaDownloadsValue);
            edit.apply();
        }

        return mPrefs.getBoolean(PREF_ALLOW_MEDIA_DOWNLOADS, defaultAllowMediaDownloadsValue);
    }

    public boolean loadPageInOverviewMode() {
        return mPrefs.getBoolean(PREF_LOAD_PAGE, true);
    }

    public boolean autofitPages() {
        return mPrefs.getBoolean(PREF_AUTOFIT_PAGES, true);
    }

    public boolean blockPopupWindows() {
        return mPrefs.getBoolean(PREF_BLOCK_POPUP_WINDOWS, false);
    }

    public String getDefaultTextEncoding() {
        //return mPrefs.getString(PREF_DEFAULT_TEXT_ENCODING, "GBK");
        String autoDetect = mPrefs.getString(PREF_DEFAULT_TEXT_ENCODING, "auto");
        if(autoDetect.equalsIgnoreCase("auto")) {
            return mContext.getResources().getString(R.string.pref_default_text_encoding_default);
        }
        return autoDetect;
    }

    // -----------------------------
    // getter/setters for general_preferences.xml
    // -----------------------------

    public String getHomePage() {
        return mPrefs.getString(PREF_HOMEPAGE, getFactoryResetHomeUrl(mContext));
    }

    public void setHomePage(String value) {
        mPrefs.edit().putString(PREF_HOMEPAGE, value).apply();
    }

    public boolean isAutofillEnabled() {
        return mPrefs.getBoolean(PREF_AUTOFILL_ENABLED, true);
    }

    public void setAutofillEnabled(boolean value) {
        mPrefs.edit().putBoolean(PREF_AUTOFILL_ENABLED, value).apply();
    }

    // -----------------------------
    // getter/setters for debug_preferences.xml
    // -----------------------------

    public boolean isHardwareAccelerated() {
        if (!isDebugEnabled()) {
            return true;
        }
        return mPrefs.getBoolean(PREF_ENABLE_HARDWARE_ACCEL, true);
    }

    public boolean isSkiaHardwareAccelerated() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_HARDWARE_ACCEL_SKIA, false);
    }

    // -----------------------------
    // getter/setters for hidden_debug_preferences.xml
    // -----------------------------

    public boolean enableVisualIndicator() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_VISUAL_INDICATOR, false);
    }

    public boolean enableCpuUploadPath() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_CPU_UPLOAD_PATH, false);
    }

    public boolean enableJavascriptConsole() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_JAVASCRIPT_CONSOLE, true);
    }

    public boolean isSmallScreen() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_SMALL_SCREEN, false);
    }

    public boolean isWideViewport() {
        if (!isDebugEnabled()) {
            return true;
        }
        return mPrefs.getBoolean(PREF_WIDE_VIEWPORT, true);
    }

    public boolean isNormalLayout() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_NORMAL_LAYOUT, false);
    }

    public boolean isTracing() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_TRACING, false);
    }

    public boolean enableLightTouch() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_LIGHT_TOUCH, false);
    }

    public boolean enableNavDump() {
        if (!isDebugEnabled()) {
            return false;
        }
        return mPrefs.getBoolean(PREF_ENABLE_NAV_DUMP, false);
    }

    public String getJsEngineFlags() {
        if (!isDebugEnabled()) {
            return "";
        }
        return mPrefs.getString(PREF_JS_ENGINE_FLAGS, "");
    }

    public long getLastUpdateTime() {
        return mPrefs.getLong(LAST_UPDATE_TIME, 0);
    }

    public void saveLastUpdateTime(long updateTime) {
        mPrefs.edit().putLong(LAST_UPDATE_TIME, updateTime).apply();
    }

    // -----------------------------
    // getter/setters for lab_preferences.xml
    // -----------------------------

    public boolean useMostVisitedHomepage() {
        //return HomeProvider.MOST_VISITED.equals(getHomePage());
        return false;
    }

    public boolean useFullscreen() {
        return mPrefs.getBoolean(PREF_FULLSCREEN, false);
    }

    public boolean useInvertedRendering() {
        return mPrefs.getBoolean(PREF_INVERTED, false);
    }

    public float getInvertedContrast() {
        return 1 + (mPrefs.getInt(PREF_INVERTED_CONTRAST, 0) / 10f);
    }

    // -----------------------------
    // getter/setters for privacy_security_preferences.xml
    // -----------------------------

    public boolean showSecurityWarnings() {
        return mPrefs.getBoolean(PREF_SHOW_SECURITY_WARNINGS, true);
    }

    public boolean doNotTrack() {
        return mPrefs.getBoolean(PREF_DO_NOT_TRACK, true);
    }

    public boolean acceptCookies() {
        return mPrefs.getBoolean(PREF_ACCEPT_COOKIES, true);
    }

    public boolean saveFormdata() {
        return mPrefs.getBoolean(PREF_SAVE_FORMDATA, true);
    }

    public boolean enableGeolocation() {
        return mPrefs.getBoolean(PREF_ENABLE_GEOLOCATION, true);
    }

    public boolean rememberPasswords() {
        return mPrefs.getBoolean(PREF_REMEMBER_PASSWORDS, true);
    }

    public boolean hasCache() {
        File file = new File(mPath);
        String[] filePaths = null;
        if (file.exists() && file.isDirectory()) {
            filePaths = file.list();
        }
        if (filePaths == null) {
            return false;
        } else {
            return filePaths.length > CACHE_INDEX_COUNT ? true : false;
        }
    }

    public boolean hasCookie() {
        return CookieManager.getInstance().hasCookies();
    }

    public boolean hasFormData() {
        WebViewDatabase db = WebViewDatabase.getInstance(mContext);
        if (db == null) {
            return false;
        } else {
            return db.hasFormData();
        }
    }

    public boolean hasHistory() {
        ContentResolver resolver = mContext.getContentResolver();
        //return Browser.canClearHistory(resolver);
        return false;
    }

    public boolean hasLocationAccess() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("webview",
                Context.MODE_PRIVATE);
        if (sharedPreferences != null && sharedPreferences.getAll() != null) {
            return !sharedPreferences.getAll().isEmpty();
        } else {
            return false;
        }
    }

    public boolean hasUsernamePassword() {
        WebSettings settings = null;
        // find a valid settings object
        Iterator<WeakReference<WebSettings>> iter = mManagedSettings.iterator();
        while (iter.hasNext()) {
            WeakReference<WebSettings> ref = iter.next();
            settings = (WebSettings)ref.get();
            if (settings != null) {
                break;
            }
        }
/*        if (settings != null)
            return settings.hasUsernamePasswords();*/
        return false;
    }

    // -----------------------------
    // getter/setters for bandwidth_preferences.xml
    // -----------------------------

    public boolean loadImages() {
        // return true for we hide this option in settings
        return true;
        //return mPrefs.getBoolean(PREF_LOAD_IMAGES, true);
    }

    public static String getPreloadOnWifiOnlyPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_data_preload_value_wifi_only);
    }

    public static String getPreloadAlwaysPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_data_preload_value_always);
    }

    private static final String DEAULT_PRELOAD_SECURE_SETTING_KEY =
            "browser_default_preload_setting";

    public String getDefaultPreloadSetting() {
        String preload = Settings.Secure.getString(mContext.getContentResolver(),
                DEAULT_PRELOAD_SECURE_SETTING_KEY);
        if (preload == null) {
            preload = mContext.getResources().getString(R.string.pref_data_preload_default_value);
        }
        return preload;
    }

    public String getPreloadEnabled() {
        // return "WIFI_ONLY" for we hide this option in settings
        return mContext.getResources().getString(R.string.pref_data_preload_default_value);
        //return mPrefs.getString(PREF_DATA_PRELOAD, getDefaultPreloadSetting());
    }

    private String getLinkPrefetchOnWifiOnlyPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_link_prefetch_value_wifi_only);
    }

    private String getLinkPrefetchAlwaysPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_link_prefetch_value_always);
    }

    private static final String DEFAULT_LINK_PREFETCH_SECURE_SETTING_KEY =
            "browser_default_link_prefetch_setting";

    public String getDefaultLinkPrefetchSetting() {
        String preload = Settings.Secure.getString(mContext.getContentResolver(),
                DEFAULT_LINK_PREFETCH_SECURE_SETTING_KEY);
        if (preload == null) {
            preload = mContext.getResources().getString(R.string.pref_link_prefetch_default_value);
        }
        return preload;
    }

    public String getLinkPrefetchEnabled() {
        // return "WIFI_ONLY" for we hide this option in settings
        return mContext.getResources().getString(R.string.pref_link_prefetch_default_value);
       // return mPrefs.getString(PREF_LINK_PREFETCH, getDefaultLinkPrefetchSetting());
    }

    // -----------------------------
    // getter/setters for readmode
    // -----------------------------

    public int getReadStyle() {
        return mPrefs.getInt(PREF_READ_MODE_STYLE, Constants.READMODE_STYLE_DAY);
    }

    public void setReadStyle(int style) {
        mPrefs.edit().putInt(PREF_READ_MODE_STYLE, style).apply();
    }

    public int getReadFontSize() {
        return mPrefs.getInt(PREF_READ_MODE_FONT_SIZE, Constants.READMODE_MIDDLE_SIZE);
    }

    public void setReadFontSize(int value) {
        mPrefs.edit().putInt(PREF_READ_MODE_FONT_SIZE, value).apply();
    }

    // -----------------------------
    // getter/setters for operation_preferences.xml
    // -----------------------------

    public boolean canShakeRestore() {
        return mPrefs.getBoolean(PREF_SHAKE_RESTORE, true);
    }

    public void setShakeRestore(boolean value) {
        mPrefs.edit().putBoolean(PREF_SHAKE_RESTORE, value).apply();
    }

    public boolean isFirstLaunch() {
        return mPrefs.getBoolean(PREF_FIRST_LAUNCH, true);
    }

    public void setNotFirstLaunch() {
        mPrefs.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();
    }

    // -----------------------------
    // getter/setters for preference clickboard monitor
    // -----------------------------

    public boolean isMonitorClipBoard() {
        return mPrefs.getBoolean(PREF_MONITOR_CLICKBOARD, true);
    }

    // -----------------------------
    // getter/setters for browser recovery
    // -----------------------------
    /**
     * The last time browser was started.
     * 
     * @return The last browser start time as System.currentTimeMillis. This can
     *         be 0 if this is the first time or the last tab was closed.
     */
    public long getLastRecovered() {
        return mPrefs.getLong(KEY_LAST_RECOVERED, 0);
    }

    /**
     * Sets the last browser start time.
     * 
     * @param time The last time as System.currentTimeMillis that the browser
     *            was started. This should be set to 0 if the last tab is
     *            closed.
     */
    public void setLastRecovered(long time) {
        mPrefs.edit()
                .putLong(KEY_LAST_RECOVERED, time)
                .apply();
    }

    /**
     * Used to determine whether or not the previous browser run crashed. Once
     * the previous state has been determined, the value will be set to false
     * until a pause is received.
     * 
     * @return true if the last browser run was paused or false if it crashed.
     */
    public boolean wasLastRunPaused() {
        return mPrefs.getBoolean(KEY_LAST_RUN_PAUSED, false);
    }

    /**
     * Sets whether or not the last run was a pause or crash.
     * 
     * @param isPaused Set to true When a pause is received or false after
     *            resuming.
     */
    public void setLastRunPaused(boolean isPaused) {
        mPrefs.edit()
                .putBoolean(KEY_LAST_RUN_PAUSED, isPaused)
                .apply();
    }
}
