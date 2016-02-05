/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;

//import smartisanos.app.MenuDialog;

import android.animation.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Path.Direction;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.*;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.content.res.TypedArray;

import com.android.myapidemo.R;
import com.android.myapidemo.UI;
import com.android.myapidemo.smartisan.browse.QuickBar.OnTextClickListener;
import com.android.myapidemo.smartisan.browse.Tab.SecurityState;
import com.android.myapidemo.smartisan.browser.bookmarks.ComboViewActivity;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;
import com.android.myapidemo.smartisan.browser.util.Constants;
import com.android.myapidemo.smartisan.navigation.DynamicGridView;
import com.android.myapidemo.smartisan.navigation.NavigationLeftView;
import com.android.myapidemo.smartisan.navigation.NavigationRightView;
import com.android.myapidemo.smartisan.navigation.DynamicGridView.OnEditModeChangeListener;
import com.android.myapidemo.smartisan.readmode.ReadModeContainerHelper;
import com.android.myapidemo.smartisan.readmode.ReadModeHelper;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;
import com.android.myapidemo.smartisan.view.PagerAdapter;
import com.android.myapidemo.smartisan.view.ViewPager;

/**
 * UI interface definitions
 */
public abstract class BaseUi implements UI, SensorEventListener, OnEditModeChangeListener {

    private static final String LOGTAG = "BaseUi";
    private static final boolean LOGD_ENABLED = Browser.LOGD_ENABLED;

    // webview parameters
    protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);

    protected static final LinearLayout.LayoutParams COVER_SCREEN_SIZE = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);

    protected static final LinearLayout.LayoutParams COVER_SCREENANIM_SIZE = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);

    protected static final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
    private static final int MSG_HIDE_TITLEBAR = 1;
    public static final int HIDE_TITLEBAR_DELAY = 1500; // in ms

    public static final int CREATE_NEW_TAB_ANIM_DURATION = 300;

    public Activity mActivity;
    UiController mUiController;
    TabControl mTabControl;
    protected Tab mActiveTab;

    private Drawable mLockIconSecure;
    private Drawable mLockIconMixed;
    protected Drawable mGenericFavicon;

    protected FrameLayout mContentView;
    protected FrameLayout mNavScreenContainer;
    protected ImageView mRoundCorner;
    protected FrameLayout mFullscreenContainer;
//    private ImageView mAddressbarShadow;
    private FrameLayout mFixedTitlebarContainer;
    private LinearLayout mTitleBarAndShadow;
    private LinearLayout mFixedBottombarContainer;
    protected CustomScreen mCustomScreen;
    private ImageView mShadowBottomView;
    private ImageView mShadowTitleView;

    private QuickBar mQuickBar;

    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    private int mOriginalOrientation;
    private FrameLayout mFrameLayout;

    private UrlBarAutoShowManager mUrlBarAutoShowManager;

    private LinearLayout mErrorConsoleContainer = null;
    private RelativeLayout mMainLayout = null;
    private boolean mNewTabAnimShowing = false;

    private Toast mStopToast;

    // the video progress view
    private View mVideoProgressView;

    private boolean mActivityPaused;
    protected boolean mUseQuickControls;
    protected TitleBar mTitleBar;
    // add BottomBar
    protected BottomBarPhone mBottomBarBase;
    private NavigationBarBase mNavigationBar;
    private ReadModeContainerHelper mReadModeContainerHelper;

    private ActionModeTitleBar mSearchBar;

    private boolean mBlockFocusAnimations;
    private static int DURATION = 150;
    private boolean isInputMethodActive;
    private static final int SENSOR_ACCERELATE = 23;
    private SensorManager mSensorManager;
    private int mLastSansKeyboardHeight;
    private RelativeLayout mHomePage;
    private RadioGroup mDotGroup;
    private NavigationRightView mNavigationRightView;
    private View mNavigationHeader;
    private TextView mComplete, mHeaderTitle;
    private View mHeaderMask;
    private View mIndicator;
    boolean isCmccFeature;
    public String mFeatureRegon;
    private ScrollView rightView;
    protected NavigationLeftView mNavigationLeftView;
    public BaseUi(Activity browser, UiController controller) {
        mActivity = browser;
        mUiController = controller;
        mTabControl = controller.getTabControl();
        Resources res = mActivity.getResources();
        mLockIconSecure = res.getDrawable(R.drawable.https_icon);
        mLockIconMixed = res.getDrawable(R.drawable.https_icon);

        mFrameLayout = (FrameLayout) mActivity.getWindow()
                .getDecorView().findViewById(android.R.id.content);
        LayoutInflater.from(mActivity).inflate(R.layout.custom_screen,
                mFrameLayout);
        mFixedTitlebarContainer = (FrameLayout) mFrameLayout
                .findViewById(R.id.fixed_titlebar_container);
        mTitleBarAndShadow = (LinearLayout) mFrameLayout
                .findViewById(R.id.titlebar_container_and_shadow);
        mFixedBottombarContainer = (LinearLayout) mFrameLayout
                .findViewById(R.id.fixed_bottombar_container);
        mContentView = (FrameLayout) mFrameLayout
                .findViewById(R.id.main_content);
        mNavScreenContainer = (FrameLayout) mFrameLayout
                .findViewById(R.id.navscreen_container);
        mRoundCorner = (ImageView) mFrameLayout
                .findViewById(R.id.round_corner);
        mErrorConsoleContainer = (LinearLayout) mFrameLayout
                .findViewById(R.id.error_console);
        mMainLayout= (RelativeLayout) mFrameLayout
                .findViewById(R.id.main_layout);
        mCustomScreen = (CustomScreen) mFrameLayout
                .findViewById(R.id.vertical_layout);
        mCustomScreen.setUi(this);
        mShadowTitleView = (ImageView) mFrameLayout
                .findViewById(R.id.shadow);
        mShadowBottomView = (ImageView) mFrameLayout
                .findViewById(R.id.shadowbottom);
        setFullscreen(BrowserSettings.getInstance().useFullscreen());
        Boolean FEATURE_CMCC = (Boolean) ReflectHelper.invokeProxyMethod(
                "smartisanos.util.config.Features", "isFeatureCMCCEnabled",
                this, new Class[] { Context.class }, new Object[] {mActivity});
        isCmccFeature = FEATURE_CMCC != null && FEATURE_CMCC;
        mFeatureRegon = (String) ReflectHelper.invokeProxyMethod(
                "smartisanos.util.config.Features", "getFeatureRegion",
                this, new Class[] { Context.class }, new Object[] {mActivity});
        mTitleBar = new TitleBar(mActivity, mUiController, this, mFixedTitlebarContainer);
        mBottomBarBase = new BottomBarPhone(mActivity, mUiController, this,
                mContentView);
        mNavigationBar = mTitleBar.getNavigationBar();
        mQuickBar = (QuickBar) mFrameLayout.findViewById(R.id.quick_input_bar);
        mQuickBar.setOnTextClickListener(new OnTextClickListener() {
            @Override
            public void onTextClick(String text) {
                int start = Math.max(mNavigationBar.mUrlInput.getSelectionStart(), 0);
                int end = Math.max(mNavigationBar.mUrlInput.getSelectionEnd(), 0);
                mNavigationBar.mUrlInput.getText().replace(Math.min(start, end), Math.max(start, end), text,
                        0, text.length());
            }
        });
        mMainLayout.removeView(mQuickBar); //remove here to simplify the layout
        //mReadModeContainerHelper = new ReadModeContainerHelper(this);
        mUrlBarAutoShowManager = new UrlBarAutoShowManager(this);
        mRoundCorner.bringToFront();
        mCustomScreen.setBackgroundColor(isCmccFeature()? Color.WHITE: mActivity.getResources().getColor(R.color.home_page_background_color));
        initHomePage();
    }

    private class ViewPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(position == 0 ? rightView : mNavigationLeftView);
            return position == 0 ? rightView : mNavigationLeftView;
        }
    }

    private ViewPager mPager;

    private void initHomePage() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        mHomePage = (RelativeLayout)inflater.inflate(R.layout.home_page, null);
        mIndicator = mHomePage.findViewById(R.id.nav_indicator);
        mPager = (ViewPager) mHomePage.findViewById(R.id.pager);
        mNavigationLeftView = (NavigationLeftView) inflater.inflate(R.layout.navigation_left_view, null);
        rightView = (ScrollView) inflater.inflate(R.layout.navigation_right_view, null);
        mNavigationRightView = (NavigationRightView) rightView.findViewById(R.id.nav_right_view);
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter();
        mPager.setAdapter(pagerAdapter);
        mPager.setOffscreenPageLimit(2);
        // /init dot
        mDotGroup = (RadioGroup) mHomePage.findViewById(R.id.dot);
        mDotGroup.check(R.id.right_dot);
        for (int i = 0; i < mDotGroup.getChildCount(); i++) {
            mDotGroup.getChildAt(i).setClickable(false);
        }
//        mPager.setOnPageChangeListener(new OnPageChangeListener() {
//            @Override
//            public void onPageSelected(int position) {
//                mDotGroup.check(position == 0 ? R.id.left_dot : R.id.right_dot);
//            }
//
//            @Override
//            public void onPageScrolled(int arg0, float arg1, int arg2) {
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int arg0) {
//            }
//        });
        mDotGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mPager.setCurrentItem(checkedId == R.id.left_dot ? 0 : 1, true);
            }
        });
        mPager.setCurrentItem(1);

        mNavigationLeftView.setBaseUi(this);
        mNavigationRightView.setBaseUi(this);
        mNavigationLeftView.setOnEditModeListener(this);
        // init header bar
        mNavigationHeader = LayoutInflater.from(mActivity).inflate(R.layout.add_navigation_header, null);
        mNavigationHeader.findViewById(R.id.header_left).setVisibility(View.INVISIBLE);
        mHeaderTitle = (TextView) mNavigationHeader.findViewById(R.id.header_title);
        mHeaderTitle.setText(R.string.edit_nav_title);
        mNavigationHeader.findViewById(R.id.header_right).setVisibility(View.GONE);
        mComplete = (TextView) mNavigationHeader.findViewById(R.id.header_right_2);
        mComplete.setVisibility(View.VISIBLE);
        mComplete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mNavigationLeftView.stopEditMode();
            }
        });
        mComplete.setText(R.string.import_bookmarks_wizard_done);

        mHeaderMask = new View(mActivity);
        int color = mActivity.getResources().getColor(R.color.header_mask_color);
        mHeaderMask.setBackgroundColor(color);
        //mHeaderMask.setAlpha(0);
    }

    @Override
    public void onEditModeChanged(boolean inEditMode) {
        if (inEditMode) {
            mUiController.stopLoading();
        }
        if (mHeaderMask.getParent() == null && inEditMode) {
            mHeaderMask.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, mFixedTitlebarContainer.getHeight()));
            mFixedTitlebarContainer.addView(mHeaderMask);
            mFixedTitlebarContainer.addView(mNavigationHeader);
        }
        for (int i = 0; i < mDotGroup.getChildCount(); i++) {
            mDotGroup.getChildAt(i).setEnabled(!inEditMode);
        }
        playEditModeAnimation(inEditMode);
        //mPager.setEditMode(inEditMode);
        mNavigationBar.mUrlInput.setText("");
        setBottomBarEnable(!inEditMode);
    }

    public void playEditModeAnimation(boolean inEditMode) {
        float startAlpha = inEditMode ? 0 : 1;
        float endAlpha = inEditMode ? 1 : 0;
        float startTranslationY = inEditMode ? -mFixedTitlebarContainer.getMeasuredHeight() : 0;
        float endTranslationY = inEditMode ? 0 : -mFixedTitlebarContainer.getMeasuredHeight();
        ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(mNavigationHeader, View.TRANSLATION_Y, startTranslationY, endTranslationY);
        translationYAnimator.setDuration(200);
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mHeaderMask, View.ALPHA, startAlpha, endAlpha);
        alphaAnimator.setDuration(100);
        ArrayList<Animator> amimators = new ArrayList<Animator>();
        DynamicGridView dynamicGridView = mNavigationLeftView.getDynamicGridView();
        final ArrayList<View> texts = new ArrayList<View>();
        if (!inEditMode) {
            ArrayList<View> deletes = new ArrayList<View>();
            for (int i = 0; i < dynamicGridView.getChildCount(); i++) {
                View child = dynamicGridView.getChildAt(i);
                View delete = child.findViewById(R.id.nav_delete);
                View deleteMask = child.findViewById(R.id.nav_delete_mask);
                deletes.add(delete);
                deletes.add(deleteMask);
                View text = child.findViewById(R.id.item_title);
                texts.add(text);
            }
            Animator deleteAlphaAnimation = CommonUtil.createAlphaAnimation(deletes, 1, 0);
            deleteAlphaAnimation.setDuration(100);
            Animator textAlphaAnimation = CommonUtil.createAlphaAnimation(texts, 0, 1);
            textAlphaAnimation.setDuration(100);
            amimators.add(deleteAlphaAnimation);
            amimators.add(textAlphaAnimation);
        }
        AnimatorSet set = new AnimatorSet();
        amimators.add(translationYAnimator);
        amimators.add(alphaAnimator);
        set.playTogether(amimators);
        set.start();
    }

    public void updateQuickBar(boolean inputMethodActive) {
        if (inputMethodActive && mNavigationBar.mUrlInput.hasFocus()) {
            if (mQuickBar.getParent() == null)
                mMainLayout.addView(mQuickBar);
            mQuickBar.setVisibility(View.VISIBLE);
        } else {
            mQuickBar.setVisibility(View.GONE);
        }

        if (inputMethodActive) {
            mBottomBarBase.setVisibility(View.GONE);
            mShadowBottomView.setVisibility(View.GONE);
        } else {
            mBottomBarBase.setVisibility(View.VISIBLE);
            mShadowBottomView.setVisibility(View.VISIBLE);
        }
    }

    private void cancelStopToast() {
        if (mStopToast != null) {
            mStopToast.cancel();
            mStopToast = null;
        }
    }

    public Resources getActivityResources(){
        if(mActivity != null){
            return mActivity.getResources();
        }
        return null;
    }

    protected void setNewTabAnimStatus(boolean status){
        mNewTabAnimShowing = status;
    }

    public boolean isNewTabAnimating() {
        return mNewTabAnimShowing;
    }

    // lifecycle
    public void onPause() {
        if (LOGD_ENABLED)
            Log.d("LOGTAG", "onPause");
        if (BrowserSettings.getInstance().isFirstLaunch() || (BrowserSettings.getInstance().canShakeRestore())) {
            if (mSensorManager != null) mSensorManager.unregisterListener(this);
        }
        if (isCustomViewShowing()) {
            onHideCustomView();
        }
        if (mTabControl != null && mTabControl.getCurrentTab() != null) {
            mTabControl.getCurrentTab().closeGeolocationDialog();
        }
        cancelStopToast();
        mActivityPaused = true;
        if (mSearchBar != null && mSearchBar.getVisibility() == View.VISIBLE) {
            mSearchBar.onPause();
        } else {
            mTitleBar.onPause();
        }
        mBottomBarBase.onPause();
        mReadModeContainerHelper.onPause();

        if (mShakeDialog != null)
            mShakeDialog.cancel();
        if (mFirstShakeDialog != null)
            mFirstShakeDialog.cancel();
        if (mConfirmShakeDialog != null)
            mConfirmShakeDialog.cancel();
    }

    public void onResume() {
        if (LOGD_ENABLED)
            Log.d("LOGTAG", "onResume");
        if (BrowserSettings.getInstance().isFirstLaunch() || (BrowserSettings.getInstance().canShakeRestore())) {
            mSensorManager = (SensorManager) mActivity.getSystemService(mActivity.SENSOR_SERVICE);
            /**
             * Improve the receiving frequency,up to SENSOR_DELAY_GAME
             * when forbidden the Automatic rotary screen.SytemUI will reduce Sensor's receiving frequency.
             */
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
        }
        mActivityPaused = false;
        // check if we exited without setting active tab
        // b: 5188145
        final Tab ct = mTabControl.getCurrentTab();
        if (ct != null) {
            //setActiveTab(ct); // why setActiveTab onResume? it cause bug 37139
        }
        ((PhoneUi) this).closeDialog();
        if (mSearchBar != null && mSearchBar.getVisibility() == View.VISIBLE) {
            mSearchBar.onResume();
        } else {
            mTitleBar.onResume();
        }
        mBottomBarBase.onResume();
    }

    public boolean isActivityPaused() {
        return mActivityPaused;
    }

    public void onConfigurationChanged(Configuration config) {
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE){
            updateQuickBar(false);
            mBottomBarBase.setVisibility(View.GONE);
            //mShadowTitleView.setTranslationY(-9);
        }else{
            //mShadowTitleView.setTranslationY(0);
        }
        mIndicator.setVisibility(config.orientation == Configuration.ORIENTATION_PORTRAIT ? View.VISIBLE
                : View.GONE);
        if(mTitleBar != null){
            mTitleBar.onConfigurationChanged(config);
        }
        //mReadModeContainerHelper.onConfigurationChanged(mTabControl);
        if(isCustomViewShowing()){
            Window win = mActivity.getWindow();
            if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }

    }

    public Activity getActivity() {
        return mActivity;
    }

    public UiController getUiController(){
        return mUiController;
    }

    // key handling

    @Override
    public boolean onBackKey() {
        if (getSearchBar() != null && getSearchBar().getVisibility() == View.VISIBLE) {
            hideSearchBar();
            return true;
        }
        if (mCustomView != null) {
            mUiController.hideCustomView();
            return true;
        }
        if(mNavigationLeftView != null && mNavigationLeftView.isEditMode()){
            mNavigationLeftView.stopEditMode();
            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuKey() {
        return false;
    }

    // Tab callbacks
    @Override
    public void onTabDataChanged(Tab tab) {
        setUrlTitle(tab);
        setFavicon(tab);
        updateLockIconToLatest(tab);
        updateNavigationState(tab);
        mTitleBar.onTabDataChanged(tab);
        onProgressChanged(tab);
    }

    @Override
    public void onProgressChanged(Tab tab) {
        if (tab.inForeground()) {
            mTitleBar.setProgress(tab.getLoadProgress());
        }
    }

    @Override
    public void updateReadViewHelper(ReadModeHelper readModeHelper){
        mReadModeContainerHelper.updateReadViewHelper(readModeHelper);
    }

    @Override
    public void bookmarkedStatusHasChanged(Tab tab) {
        if (tab.inForeground()) {
            boolean isBookmark = tab.isBookmarkedSite();
            mNavigationBar.setCurrentUrlIsBookmark(isBookmark);
        }
    }

    @Override
    public void onPageStopped(Tab tab) {
        cancelStopToast();
    }

    @Override
    public boolean needsRestoreAllTabs() {
        return true;
    }

    @Override
    public void addTab(Tab tab) {
    }

    @Override
    public void setActiveTab(final Tab tab) {
        if (tab == null)
            return;
        // block unnecessary focus change animations during tab switch
        mBlockFocusAnimations = true;
        if ((tab != mActiveTab) && (mActiveTab != null)) {
            removeTabFromContentView(mActiveTab);
            BrowserWebView web = (BrowserWebView)mActiveTab.getWebView();
            if (web != null) {
                web.setOnTouchListener(null);
            }
            mActiveTab.putInBackground(); // sometime mActiveTab not put to back ground, so make sure here.
        }
        mActiveTab = tab;
        BrowserWebView web = (BrowserWebView) mActiveTab.getWebView();
        updateUrlBarAutoShowManagerTarget();
        attachTabToContentView(tab, true);
        if (web != null) {
            web.setVisibility(View.VISIBLE);
            tab.putInForeground();
            //web.onShow();
            // Request focus on the top window.
            if (mUseQuickControls) {
                web.setTitleBar(null);
                web.setBottomBar(null);
            } else {
                web.setTitleBar(mTitleBar);
                web.setBottomBar(mBottomBarBase);
            }
        }
//        mTitleBar.bringToFront();
        translateTitleBar(tab.mTopControlsOffsetYPix);
        if (tab.getTopWindow() != null)
            tab.getTopWindow().requestFocus();
        setShouldShowErrorConsole(tab, mUiController.shouldShowErrorConsole());
        onTabDataChanged(tab);
        setBackForwardBtn();
        updateReadViewHelper(tab.getReadModeHelper());
        ((NavigationBarPhone)mNavigationBar).setButtonsPosition(tab.isBtnReadModeShowing());
        ((NavigationBarPhone)mNavigationBar).setIncognitoMode(tab.isPrivateBrowsingEnabled());
        mBlockFocusAnimations = false;
    }

    protected void updateUrlBarAutoShowManagerTarget() {
        WebView web = mActiveTab != null ? mActiveTab.getWebView() : null;
        if (web instanceof BrowserWebView) {
            mUrlBarAutoShowManager.setTarget((BrowserWebView) web);
        }
    }
    Tab getActiveTab() {
        return mActiveTab;
    }

    @Override
    public void removeTab(Tab tab) {
        if (mActiveTab == tab) {
            removeTabFromContentView(tab);
            mActiveTab = null;
        }
    }

    @Override
    public void detachTab(Tab tab) {
        removeTabFromContentView(tab);
    }

    @Override
    public void attachTab(Tab tab, boolean setActive) {
        attachTabToContentView(tab, setActive);
    }

    protected void attachTabToContentView(Tab tab, boolean setActive) {
        if ((tab == null) || (tab.getWebView() == null)) {
            return;
        }
        View container = tab.getViewContainer();
        WebView mainView = tab.getWebView();
        // Attach the WebView to the container and then attach the
        // container to the content view.
        FrameLayout wrapper = (FrameLayout) container
                .findViewById(R.id.webview_wrapper);
        ViewGroup parent = (ViewGroup) mainView.getParent();
        if (parent != wrapper) {
            if (parent != null) {
                parent.removeView(mainView);
            }
            wrapper.addView(mainView);
        }
        parent = (ViewGroup) container.getParent();
        if (parent != mContentView) {
            if (parent != null) {
                parent.removeView(container);
            }
            if (setActive)
                mContentView.addView(container, COVER_SCREEN_PARAMS);
            else // if not active tab, then add it to content view, but put to the bottom, covered by other tabs
                mContentView.addView(container, mContentView.getChildCount()-1, COVER_SCREEN_PARAMS);
        }
        mUiController.attachSubWindow(tab);
    }

    private void removeTabFromContentView(Tab tab) {
//        hideTitleBar();
        // Remove the container that contains the main WebView.
        WebView mainView = tab.getWebView();
        View container = tab.getViewContainer();
        if (mainView == null) {
            return;
        }
        // Remove the container from the content and then remove the
        // WebView from the container. This will trigger a focus change
        // needed by WebView.
        FrameLayout wrapper = (FrameLayout) container
                .findViewById(R.id.webview_wrapper);
        wrapper.removeView(mainView);
        mContentView.removeView(container);
        mUiController.endActionMode();
        mUiController.removeSubWindow(tab);
        ErrorConsoleView errorConsole = tab.getErrorConsole(false);
        if (errorConsole != null) {
            mErrorConsoleContainer.removeView(errorConsole);
            mErrorConsoleContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSetWebView(Tab tab, WebView webView) {
        View container = tab.getViewContainer();
        if (container == null) {
            // The tab consists of a container view, which contains the main
            // WebView, as well as any other UI elements associated with the
            // tab.
            container = mActivity.getLayoutInflater().inflate(R.layout.tab,
                    mContentView, false);
            tab.setViewContainer(container);
        }
        if (tab.getWebView() != webView) {
            // Just remove the old one.
            FrameLayout wrapper = (FrameLayout) container
                    .findViewById(R.id.webview_wrapper);
            wrapper.removeView(tab.getWebView());
        }
    }

    /**
     * create a sub window container and webview for the tab Note: this methods
     * operates through side-effects for now it sets both the subView and
     * subViewContainer for the given tab
     *
     * @param tab tab to create the sub window for
     * @param subView webview to be set as a subwindow for the tab
     */
    @Override
    public void createSubWindow(Tab tab, WebView subView) {
        View subViewContainer = mActivity.getLayoutInflater().inflate(
                R.layout.browser_subwindow, null);
        ViewGroup inner = (ViewGroup) subViewContainer
                .findViewById(R.id.inner_container);
        inner.addView(subView, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        final ImageButton cancel = (ImageButton) subViewContainer
                .findViewById(R.id.subwindow_close);
        final WebView cancelSubView = subView;
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BrowserWebView) cancelSubView).getWebChromeClient()
                        .onCloseWindow(cancelSubView);
            }
        });
        tab.setSubWebView(subView);
        tab.setSubViewContainer(subViewContainer);
    }

    @Override
    public void setReadViewHelper(ReadModeHelper readModeHelper) {
        mReadModeContainerHelper.updateReadViewHelper(readModeHelper);
    }

    @Override
    public boolean isReadModeWindowShowing() {
        return mReadModeContainerHelper.isReadModeWindowShowing();
    }

    @Override
    public boolean isReadModeWindowWillShowing() {
        return mReadModeContainerHelper.isReadModeWindowWillShowing();
    }

    @Override
    public void cancelshowReadModeWindow() {
        cancelReadMode();
        mReadModeContainerHelper.cancelshowReadModeWindow();
    }

    @Override
    public void dismissReadModeWindow() {
        mReadModeContainerHelper.dismissReadModeWindow();
        cancelReadMode();
    }

    private void cancelReadMode() {
        attachTab(mActiveTab, true);
        mActiveTab.putInForeground();
    }

    /**
     * Remove the sub window from the content view.
     */
    @Override
    public void removeSubWindow(View subviewContainer) {
        mContentView.removeView(subviewContainer);
        mUiController.endActionMode();
    }

    /**
     * Attach the sub window to the content view.
     */
    @Override
    public void attachSubWindow(View container) {
        if (container.getParent() != null) {
            // already attached, remove first
            ((ViewGroup) container.getParent()).removeView(container);
        }
        mContentView.addView(container, 0);
    }

    protected void refreshWebView() {
        WebView web = getWebView();
        if (web != null) {
            web.invalidate();
        }
    }

    public void editUrl(boolean clearInput, boolean forceIME) {
        if (mUiController.isInCustomActionMode()) {
            mUiController.endActionMode();
        }
        if ((getActiveTab() != null) && !getActiveTab().isSnapshot()) {
            mNavigationBar.startEditingUrl(clearInput, forceIME);
        }
    }

    boolean canShowTitleBar() {
        return /*!isTitleBarShowing()
                && */!isActivityPaused()
                && (getActiveTab() != null)
                && (getWebView() != null)
                && !mUiController.isInCustomActionMode();
    }

    protected void showTitleBar() {
        mHandler.removeMessages(MSG_HIDE_TITLEBAR);
        if (canShowTitleBar()) {
            mTitleBar.show();
        }
    }

    protected void hideTitleBar() {
        if (mTitleBar.isShowing()) {
            mTitleBar.hide();
        }
    }

    protected boolean isTitleBarShowing() {
        return mTitleBar.isShowing();
    }

    public boolean isEditingUrl() {
        return mTitleBar.isEditingUrl();
    }

    public void stopEditingUrl() {
        mTitleBar.getNavigationBar().stopEditingUrl();
    }

    public TitleBar getTitleBar() {
        return mTitleBar;
    }

    public View getNavigationEditView() {
        return mNavigationHeader;
    }

    public View getNavigationEditViewMask() {
        return mHeaderMask;
    }

    @Override
    public void showComboView(ComboViews startingView, Bundle extras) {
        System.out.println("================= BaseUi showComboView =====================");
        Intent intent = new Intent(mActivity, ComboViewActivity.class);
        intent.putExtra(ComboViewActivity.EXTRA_INITIAL_VIEW,
                startingView.name());
        intent.putExtra(ComboViewActivity.EXTRA_COMBO_ARGS, extras);
        Tab t = getActiveTab();
        if (t != null) {
            intent.putExtra(ComboViewActivity.EXTRA_CURRENT_URL, t.getUrl());
        }
        mActivity.startActivityForResult(intent, Controller.COMBO_VIEW);
        mActivity.overridePendingTransition(
                R.anim.pop_up_in,
                R.anim.activity_close_enter_in_call);
    }

    @Override
    public void showCustomView(View view, int requestedOrientation,
            CustomViewCallback callback) {
        // if a view already exists then immediately terminate the new one
        if (mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }

        mOriginalOrientation = mActivity.getRequestedOrientation();
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        mFullscreenContainer = new FullscreenHolder(mActivity);
        mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        mCustomView = view;
        setFullscreen(true);
        ((BrowserWebView) getWebView()).setVisibility(View.INVISIBLE);
        mCustomViewCallback = callback;
        //mActivity.setRequestedOrientation(requestedOrientation);
    }

    @Override
    public void onHideCustomView() {
        ((BrowserWebView) getWebView()).setVisibility(View.VISIBLE);
        if (mCustomView == null)
            return;
        setFullscreen(false);
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        decor.removeView(mFullscreenContainer);
        mFullscreenContainer = null;
        mCustomView = null;
        mCustomViewCallback.onCustomViewHidden();
        // Show the content view.
        mActivity.setRequestedOrientation(mOriginalOrientation);
    }

    @Override
    public boolean isCustomViewShowing() {
        return mCustomView != null;
    }

    @Override
    public boolean isWebShowing() {
        return mCustomView == null;
    }

    // -------------------------------------------------------------------------

    protected void updateNavigationState(Tab tab) {
    }

    /**
     * Update the lock icon to correspond to our latest state.
     */
    protected void updateLockIconToLatest(Tab t) {
        if (t != null && t.inForeground()) {
            updateLockIconImage(t.getSecurityState());
        }
    }

    /**
     * Updates the lock-icon image in the title-bar.
     */
    private void updateLockIconImage(SecurityState securityState) {
        Drawable d = null;
        if (securityState == SecurityState.SECURITY_STATE_SECURE) {
            d = mLockIconSecure;
        } else if (securityState == SecurityState.SECURITY_STATE_MIXED
                || securityState == SecurityState.SECURITY_STATE_BAD_CERTIFICATE) {
            d = mLockIconMixed;
        }
        mNavigationBar.setLock(d);
    }

    protected void setUrlTitle(Tab tab) {
        if (tab.inForeground()) {
            mNavigationBar.setDisplayTitle(tab.getUrl());
        }
    }

    // Set the favicon in the title bar.
    protected void setFavicon(Tab tab) {
        if (tab.inForeground()) {
            Bitmap icon = tab.getFavicon();
            mNavigationBar.setFavicon(icon);
        }
    }


    public void showReadWindow() {
        mReadModeContainerHelper.showReadModeWindow();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBottomBarBase.disMissPopup();
            }
        }, Constants.FLING_DURATION);
    }

    public void detachActiveTab() {
        detachTab(mActiveTab);
        mActiveTab.putInBackground();
    }

    // active tabs page
    public void showActiveTabsPage() {
    }

    /**
     * Remove the active tabs page.
     */
    public void removeActiveTabsPage() {
    }

    // menu handling callbacks
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
    }

    @Override
    public void onOptionsMenuOpened() {
    }

    @Override
    public void onExtendedMenuOpened() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onOptionsMenuClosed(boolean inLoad) {
    }

    @Override
    public void onExtendedMenuClosed(boolean inLoad) {
    }

    @Override
    public void onContextMenuCreated(Menu menu) {
    }

    @Override
    public void onContextMenuClosed(Menu menu, boolean inLoad) {
    }

    // error console

    @Override
    public void setShouldShowErrorConsole(Tab tab, boolean flag) {
        if (tab == null)
            return;
        ErrorConsoleView errorConsole = tab.getErrorConsole(true);
        if (flag) {
            // Setting the show state of the console will cause it's the layout
            // to be inflated.
            if (errorConsole.numberOfErrors() > 0) {
                errorConsole.showConsole(ErrorConsoleView.SHOW_MINIMIZED);
            } else {
                errorConsole.showConsole(ErrorConsoleView.SHOW_NONE);
            }
            if (errorConsole.getParent() != null) {
                mErrorConsoleContainer.removeView(errorConsole);
                mErrorConsoleContainer.setVisibility(View.GONE);
            }
            // Now we can add it to the main view.
            mErrorConsoleContainer.addView(errorConsole,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
            mErrorConsoleContainer.setVisibility(View.VISIBLE);
        } else {
            mErrorConsoleContainer.removeView(errorConsole);
            mErrorConsoleContainer.setVisibility(View.GONE);
        }
    }

    // -------------------------------------------------------------------------
    // Helper function for WebChromeClient
    // -------------------------------------------------------------------------

    private Bitmap mDefaultVideoPoster;
    @Override
    public Bitmap getDefaultVideoPoster() {
        if (mDefaultVideoPoster == null) {
            mDefaultVideoPoster = BitmapFactory.decodeResource(
              mActivity.getResources(), R.drawable.default_video_poster);
        }
        return mDefaultVideoPoster;
    }

    @Override
    public View getVideoLoadingProgressView() {
        if (mVideoProgressView == null) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            mVideoProgressView = inflater.inflate(
                    R.layout.video_loading_progress, null);
        }
        return mVideoProgressView;
    }

    protected WebView getWebView() {
        if (mActiveTab != null) {
            return mActiveTab.getWebView();
        } else {
            return null;
        }
    }

    public void setFullscreen(boolean enabled) {
        Window win = mActivity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (enabled) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
            if (mCustomView != null) {
                mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
        win.setAttributes(winParams);
    }

    //make full screen by showing/hiding topbar and system status bar
    public void showFullscreen(boolean fullScreen) {
        //Hide/show system ui bar as needed
        if (!BrowserSettings.getInstance().useFullscreen())
            setFullscreen(fullScreen);

        //Hide/show topbar as needed
        if (getWebView() != null) {
//            if (fullScreen) {
//                //hide topbar
//                getWebView().updateTopControls(true, false, false);
//            } else {
//                //show the topbar
//                getWebView().updateTopControls(false, true, false);
//                //enable for auto-hide
//                if (!mTitleBar.isFixed())
//                    getWebView().updateTopControls(true, true, false);
//            }
        }
    }

    public void translateReadModeTitleBar(float topControlsOffsetYPix) {
        mReadModeContainerHelper.getTitleBar().setTranslationY(topControlsOffsetYPix);
    }

    public void translateTitleBar(float topControlsOffsetYPix) {
        if (mTitleBar != null && !mInActionMode) {
            if (topControlsOffsetYPix != 0.0) {
                mTitleBar.setEnabled(false);
            } else {
                mTitleBar.setEnabled(true);
            }
            if (!mTitleBar.isFixed()) {
                float currentY = mTitleBar.getTranslationY();
                float height = mTitleBar.getHeight();
                if ((height + currentY) <= 0 && (height + topControlsOffsetYPix) > 0) {
                    mTitleBar.requestLayout();
                } else if ((height + topControlsOffsetYPix) <= 0) {
                    topControlsOffsetYPix -= 1;
                    mTitleBar.getParent().requestTransparentRegion(mTitleBar);
                }
            }
            // This was done to get HTML5 fullscreen API to work with fixed mode since
            // topcontrols are used to implement HTML5 fullscreen
            mTitleBarAndShadow.setTranslationY(topControlsOffsetYPix);
            mTitleBarAndShadow.bringToFront();
        }
    }

    public Drawable getFaviconDrawable(Bitmap icon) {
        Drawable[] array = new Drawable[3];
        array[0] = new PaintDrawable(Color.BLACK);
        PaintDrawable p = new PaintDrawable(Color.WHITE);
        array[1] = p;
        if (icon == null) {
            array[2] = mGenericFavicon;
        } else {
            array[2] = new BitmapDrawable(icon);
        }
        LayerDrawable d = new LayerDrawable(array);
        d.setLayerInset(1, 1, 1, 1, 1);
        d.setLayerInset(2, 2, 2, 2, 2);
        return d;
    }

    private boolean isLoading() {
        return mActiveTab != null ? mActiveTab.inPageLoad() : false;
    }

    /**
     * Suggest to the UI that the title bar can be hidden. The UI will then
     * decide whether or not to hide based off a number of factors, such
     * as if the user is editing the URL bar or if the page is loading
     */
    public void suggestHideTitleBar() {
        if (!isLoading() && !isEditingUrl() && !mTitleBar.wantsToBeVisible()) {
            hideTitleBar();
        }
    }

    protected final void showTitleBarForDuration() {
        showTitleBarForDuration(HIDE_TITLEBAR_DELAY);
    }

    protected final void showTitleBarForDuration(long duration) {
        showTitleBar();
        Message msg = Message.obtain(mHandler, MSG_HIDE_TITLEBAR);
        mHandler.sendMessageDelayed(msg, duration);
    }

    protected void setMenuItemVisibility(Menu menu, int id,
                                         boolean visibility) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setVisible(visibility);
        }
    }

    protected Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            BaseUi.this.handleMessage(msg);
        }
    };

    protected void handleMessage(Message msg) {
    }

    @Override
    public void showWeb(boolean animate) {
        mUiController.hideCustomView();
    }

    static class FullscreenHolder extends FrameLayout {

        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(R.color.black));
        }

        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }

    }

    public void addFixedTitleBar(View view) {
        mFixedTitlebarContainer.addView(view);
    }

    public void addFixedBottomBar(View view) {
        mFixedBottombarContainer.addView(view);
    }

    public void removeFixedBottomBar(View view) {
        mFixedBottombarContainer.removeView(view);
    }

    public void setContentViewMarginTop(int margin) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mContentView
                .getLayoutParams();
        if (params.topMargin != margin) {
//            params.topMargin = margin;
        }
    }

    /*public void setContentViewMarginBottom(int margin) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mCustomScreen
                .getLayoutParams();
        if (params.bottomMargin != margin) {
            params.bottomMargin = margin;
            mCustomScreen.requestLayout();
        }
    }*/
 /*   public void setTitleBarTranslationY(int deltaY) {
        if(isInputMethodShowing()){
            return;
        }
        float oldTranslationY = mFixedTitlebarContainer.getTranslationY();
        int titleHeight = mFixedTitlebarContainer.getHeight();
        float newTranslationY = oldTranslationY + deltaY;
        //fix size
        if(newTranslationY > 0){
            newTranslationY = 0;
        } else if (newTranslationY < -titleHeight) {
            newTranslationY = -titleHeight;
        }
        int oldMarginTop = getCurrentMarginTop();
        int newMarginTop = oldMarginTop + deltaY;
        if(newMarginTop < 0){
            newMarginTop = 0;
        }else if(newMarginTop > titleHeight){
            newMarginTop = titleHeight;
        }
        setContentViewMarginTop(newMarginTop);
        mFixedTitlebarContainer.setTranslationY(newTranslationY);
        mshadowTitleView.setTranslationY(newTranslationY);
    }

    public void setBottomBarTranslationY(int y) {
        if(isInputMethodShowing()){
            return;
        }
        float oldTranslationY = mFixedBottombarContainer.getTranslationY();
        int height = mBottomBarBase.getHeight();
        float newTranslationY = oldTranslationY - y;
        if (newTranslationY > height) {
            newTranslationY = height;
        }else if(newTranslationY < 0){
            newTranslationY = 0;
        }
        float margin = mBottomBarBase.getHeight() - newTranslationY;
        setContentViewMarginBottom((int)margin);
        mFixedBottombarContainer.setTranslationY(newTranslationY);
    }*/
    @Override
    public boolean blockFocusAnimations() {
        return mBlockFocusAnimations;
    }

    @Override
    public void onVoiceResult(String result) {
        mNavigationBar.onVoiceResult(result);
    }

    protected ActionModeTitleBar getSearchBar() {// should only use in BaseUi.java and PhoneUi.java
        return mSearchBar;
    }

    public void showSearchBar() {// should only called when click findOnPage menu. so bring search bar to front
        if (mSearchBar == null)
            mSearchBar = new ActionModeTitleBar(mActivity, mUiController, this, mContentView);
        mSearchBar.updateWebView();
        mSearchBar.bringToFront();
    }

    public void hideSearchBar() {
        if (mSearchBar != null && mSearchBar.getVisibility() == View.VISIBLE) {
            mSearchBar.hide();
            mSearchBar.clearMatches();
            mSearchBar.setVisibility(View.GONE);
        }
    }

    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
    }

    boolean mInActionMode = false;
    private float getActionModeHeight() {
        TypedArray actionBarSizeTypedArray = mActivity.obtainStyledAttributes(
                    new int[] { android.R.attr.actionBarSize });
        float size = actionBarSizeTypedArray.getDimension(0, 0f);
        actionBarSizeTypedArray.recycle();
        return size;
    }


    @Override
    public void onActionModeStarted(ActionMode mode) {
        mInActionMode = true;

        if (mTitleBar.isFixed()) {
            int fixedTbarHeight = mTitleBar.calculateEmbeddedHeight();
            setContentViewMarginTop(fixedTbarHeight);
        } else {
            mTitleBar.setTranslationY(getActionModeHeight());
        }
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
        mInActionMode = false;
        if (mTitleBar.isFixed()) {
            setContentViewMarginTop(0);
        } else {
            mTitleBar.setTranslationY(0);
        }
    }

    @Override
    public void closeTheLeastUsedTab() {
        Tab leastUsedTab = mTabControl.getLeastUsedTab(getActiveTab());
        if (leastUsedTab != null) {
            mUiController.closeTab(leastUsedTab);
        }
    }

    @Override
    public void OpenInBackGround(String url) {
        Tab tab = null;
        if (mTabControl.canCreateNewTab()) {
            tab = mUiController.openTab(url, mTabControl.getCurrentTab(), false, false, true);
        } else {
            tab = mTabControl.findTabWithUrl("", mTabControl.getCurrentTab().isPrivateBrowsingEnabled());
            if (tab != null) {
                tab.loadUrl(url, null);
            } else {
                closeTheLeastUsedTab();
                tab = mUiController.openTab(url, mTabControl.getCurrentTab(), false, false, true);
            }
        }
        if (tab != null) {
            tab.pause();
        }
    }

    public Tab NewTab(final int tabTag, final Bundle state) {
        Tab tab = null;
        mState = state;
        final String url = state == null ? null : state.getString("url");
        if (!mTabControl.canCreateNewTab()) {
            closeTheLeastUsedTab();
        }
        if (mTabControl.getIncogMode()) {
            mMainLayout.setBackgroundResource(R.drawable.navscreen_incog_background);
        } else {
            mMainLayout.setBackgroundResource(R.drawable.main_bg_shape);
        }
        switch (tabTag) {
            case BottomBarPhone.NEW_TAB_ING_TAG: {
                if (TextUtils.isEmpty(url)) {
                    tab = mUiController.openIncognitoTab();
                } else {
                    tab = mUiController.openIncognitoTab(url, mTabControl.getCurrentTab(),
                            !mUiController.getSettings().openInBackground(), true);
                }
                ((NavigationBarPhone)mNavigationBar).setIncognitoMode(true);
                break;
            }
            case BottomBarPhone.NEW_TAB_TAG: {
                tab = mUiController.openTabToHomePage();
                ((NavigationBarPhone)mNavigationBar).setIncognitoMode(false);
                break;
            }
            case BottomBarPhone.NEW_TAB_INNER_TAG: {
                tab = mUiController.openTab("", mTabControl.getCurrentTab(), false, true);
                break;
            }
            default:
                // assert not reached
                break;
        }

        return tab;
    }

    public void newAnim(Tab tab, Message msg) {
        mOriginTab = getActiveTab();
        if (mOriginTab == null) // avoid crash when run monkey
            return;

        mNewTabAnimShowing = true;
        if (mSearchBar != null && mSearchBar.getVisibility() == View.VISIBLE) {
            getSearchBar().clearMatches();
            getSearchBar().setVisibility(View.GONE);
            getTitleBar().setVisibility(View.VISIBLE);
        }

        mTargetTab = tab;
        mMsg = msg;
        mCaptured = false;
        mHandler.postDelayed(mNewTabRunnable, 500); // assume the capture will not take more than 160ms on T1, but U1 will take longer.
        mOriginTab.getWebViewCapture(new ValueCallback<Bitmap>() {
            @Override
            public void onReceiveValue(Bitmap webCapture) {
                if (!mCaptured) {
                    mWebCapture = webCapture;
                    mHandler.removeCallbacks(mNewTabRunnable);
                    mHandler.post(mNewTabRunnable);
                }
            }
        });
    }

    private Tab mTargetTab, mOriginTab;
    private Message mMsg;
    protected Bitmap mWebCapture = null;
    private boolean mCaptured = false;
    private Bundle mState;
    Runnable mNewTabRunnable = new Runnable() {
        public void run() {
            mCaptured = true;
            if (mMsg != null && mTargetTab != null) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) mMsg.obj;
                transport.setWebView(mTargetTab.getWebView());
                mMsg.getTarget().sendMessage(mMsg);
            }
            mOriginTab.capture(mWebCapture);
            mOriginTab.setBackBitmap(mWebCapture);
            int customScreenWidth = mCustomScreen.getWidth();
            int customScreenHeight = mCustomScreen.getHeight();
            final ImageView customScreen = new ImageView(mActivity);
            Bitmap bitmap = safeCreateBitmap(customScreenWidth, customScreenHeight);
            customScreen.measure(
                    MeasureSpec.makeMeasureSpec(customScreenWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(customScreenHeight, MeasureSpec.EXACTLY));
            customScreen.layout(0, 0, customScreenWidth, customScreenHeight);
            if (bitmap != null) {
                Path clip = new Path();
                RectF rectRound = new RectF(0, 0, customScreenWidth, customScreenHeight);
                float radius = CommonUtil.dip2px(mActivity, 3);
                Canvas c = new Canvas(bitmap);
                clip.addRoundRect(rectRound, new float[] {radius, radius, radius, radius, 0, 0, 0, 0}, Direction.CW);
                c.drawColor(mActivity.getResources().getColor(R.color.main_page_bg));
                c.clipPath(clip);
                c.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
                mCustomScreen.draw(c);
                if (!mOriginTab.isShowHomePage() && mWebCapture != null && !mWebCapture.isRecycled()) {
                    int offsetY = customScreen.getHeight() - mWebCapture.getHeight();
                    c.drawBitmap(mWebCapture, null, new Rect(0, offsetY, mWebCapture.getWidth(), mWebCapture.getHeight() + offsetY) , null);
                } else if (!mOriginTab.isShowHomePage()) {
                    Paint paint = new Paint();
                    paint.setColor(Color.WHITE);
                    c.drawRect(0, mTitleBar.getHeight() + mTitleBar.getTranslationY(), customScreenWidth, customScreenHeight, paint);
                }
                customScreen.setImageBitmap(bitmap);
                c.setBitmap(null);
            }
            final WeakReference<Bitmap> bitmapPtr = new WeakReference<Bitmap>(bitmap);
            COVER_SCREENANIM_SIZE.bottomMargin = (int) mActivity.getResources()
                    .getDimension(R.dimen.custom_screen_bottom_bar_heigth);
            ((ViewGroup) mCustomScreen.getParent()).addView(
                    customScreen, COVER_SCREENANIM_SIZE);
            ObjectAnimator oTran = ObjectAnimator.ofFloat(customScreen,
                    "translationX", 0, -customScreen.getWidth());
            PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat(
                    "scaleX", customScreen.getScaleX(),
                    (float) (customScreen.getScaleX() * 0.85));
            PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat(
                    "scaleY", customScreen.getScaleY(),
                    (float) (customScreen.getScaleY() * 0.85));
            Animator scaleAnim = ObjectAnimator.ofPropertyValuesHolder(
                    customScreen, pvhScaleX, pvhScaleY);

            mCustomScreen.setScaleX(0.85f);
            mCustomScreen.setScaleY(0.85f);
            mCustomScreen.setVisibility(View.VISIBLE);
            PropertyValuesHolder pvhScaleX1 = PropertyValuesHolder.ofFloat(
                    "scaleX", mCustomScreen.getScaleX(),
                    (float) (mCustomScreen.getScaleX() / 0.85f));
            PropertyValuesHolder pvhScaleY1 = PropertyValuesHolder.ofFloat(
                    "scaleY", mCustomScreen.getScaleY(),
                    (float) (mCustomScreen.getScaleY() / 0.85f));
            Animator scaleAnim1 = ObjectAnimator.ofPropertyValuesHolder(
                    mCustomScreen, pvhScaleX1, pvhScaleY1);
            ObjectAnimator oTranLeft = ObjectAnimator.ofFloat(
                    mCustomScreen, "translationX",
                    mCustomScreen.getWidth(), 0);
            ArrayList<Animator> animList = new ArrayList<Animator>();
            animList.add(oTran);
            animList.add(oTranLeft);
            AnimatorSet asTran = new AnimatorSet();
            asTran.playTogether(animList);
            AnimatorSet asAll = new AnimatorSet();
            asAll.setDuration(CREATE_NEW_TAB_ANIM_DURATION);
            scaleAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mTargetTab != null) {
                        mUiController.setActiveTab(mTargetTab);
                    }
                }
            });
            asAll.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (mTargetTab != null) {
                        byte[] latestState = mState == null ? null : mState
                                .getByteArray(DataController.BACKFORWARD_LIST);
                        if (latestState != null) {
                            mTargetTab.restoreBackForwardList(mState);
                        } else {
                            String url = mState == null ? null : mState.getString("url");
                            if (url != null && mMsg == null) {
                                mTargetTab.getWebView().loadUrl(url);
                            }
                        }
                    }
                    mState = null;
                    mMsg = null;
                    if(!isOpenHomePageFeature()){//set urlinputTextview empty
                        mNavigationBar.mUrlInput.setText("");
                    }
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (customScreen != null) {
                        customScreen.setImageBitmap(null); // to ensure no FC
                        ((ViewGroup) mCustomScreen.getParent()).removeView(customScreen);
                    }
                    if (bitmapPtr != null) {
                        Bitmap bm = bitmapPtr.get();
                        if (bm != null) {
                            bm.recycle();
                        }
                    }
                    mContentView.setVisibility(View.VISIBLE);
                    mContentView.requestFocus();
                    mNewTabAnimShowing = false;
                    super.onAnimationEnd(animation);
                }
            });
            if(!mTargetTab.isShowHomePage()) {
                mContentView.setVisibility(View.INVISIBLE);
            }
            asAll.playSequentially(scaleAnim, asTran, scaleAnim1);
            asAll.start();
       }
    };

    @Override
    public Tab NewTabAnim(final int tabTag, final Bundle state) {
        System.out.println("====================== NewTabAnim ======================");
        Tab tab = NewTab(tabTag, state);

        newAnim(tab, null);

        return tab;
    }

    private Bitmap safeCreateBitmap(int width, int height) {
        if (width <= 0 || height <= 0) {
            Log.w(LOGTAG, "safeCreateBitmap failed! width: " + width
                    + ", height: " + height);
            return null;
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    }

    @Override
    public ImageView getPreBtn() {
        return mBottomBarBase.getPreBtn();
    }

    @Override
    public ImageView getForBtn() {
        return mBottomBarBase.getNextBtn();
    }
    public void setBottomBarEnable(boolean enable){
        for (int i = 0; i < mBottomBarBase.getChildCount(); i++) {
            mBottomBarBase.getChildAt(i).setEnabled(enable);
        }
        if (enable) {
            setBackForwardBtn();
        }
    }
    @Override
    public void setBackForwardBtn() {
        Tab tab = mUiController.getCurrentTab();
        if (tab == null) {
            return;
        }
        if (tab.isPrivateBrowsingEnabled()) {
            mBottomBarBase.getNextBtn().setImageResource(R.drawable.bottombar_next_page_incog_selector);
            mBottomBarBase.getPreBtn().setImageResource(R.drawable.bottombar_previous_page_incog_selector);
        } else {
            mBottomBarBase.getNextBtn().setImageResource(R.drawable.bottombar_next_page_selector);
            mBottomBarBase.getPreBtn().setImageResource(R.drawable.bottombar_previous_page_selector);
        }
        mBottomBarBase.getNextBtn().setEnabled(tab.canGoForward());
        mBottomBarBase.getPreBtn().setEnabled(tab.canGoBack());
        mNavigationBar.getNextBtn().setEnabled(tab.canGoForward());
        mNavigationBar.getPreBtn().setEnabled(tab.canGoBack());
    }

    @Override
    public void showTitleBottomBar(int mask) {
        doShow((mask & UI.VIEW_TITLE_MASK) == UI.VIEW_TITLE_MASK,
                (mask & UI.VIEW_BOTTOM_MASK) == UI.VIEW_BOTTOM_MASK, true);
    }
    @Override
    public void showTitleBottomBar(int mask,boolean isAnim) {
        doShow((mask & UI.VIEW_TITLE_MASK) == UI.VIEW_TITLE_MASK  ,
                (mask & UI.VIEW_BOTTOM_MASK) == UI.VIEW_BOTTOM_MASK,isAnim);
    }
    @Override
    public boolean isBottomBarHide() {
        if(mFixedBottombarContainer == null){
            return false;
        }
        return (int)mFixedBottombarContainer.getTranslationY() > 0 || isInputMethodShowing();
    }
    public boolean isBottomBarNotShowAll() {
        return mFixedBottombarContainer.getTranslationY() != 0;
    }
    public boolean isBottomBarShowLittle() {
        if(mFixedBottombarContainer == null){
            return false;
        }
        float translationY = mFixedBottombarContainer.getTranslationY();
        return translationY != 0 && translationY != mBottomBarBase.getHeight();
    }
    public boolean isInputMethodShowing() {
        return isInputMethodActive;
    }

    @Override
    public void disMissTitleBottomBar(int mask) {
      /*  doDismiss((mask & UI.VIEW_TITLE_MASK) == UI.VIEW_TITLE_MASK ,
                (mask & UI.VIEW_BOTTOM_MASK) == UI.VIEW_BOTTOM_MASK, true);*/
    }
    @Override
    public void disMissTitleBottomBar(int mask, boolean isAnim) {
       /* doDismiss((mask & UI.VIEW_TITLE_MASK) == UI.VIEW_TITLE_MASK ,
                (mask & UI.VIEW_BOTTOM_MASK) == UI.VIEW_BOTTOM_MASK, isAnim);*/
    }

    private void doShow(boolean title , boolean bottom, boolean isAnim) {
        if (isInputMethodShowing()) {
            return;
        }
        //titleShowAnim(mFixedTitlebarContainer, isAnim);
        //bottomShowAnim(mBottomBarBase, isAnim);
    }

    public int getCurrentMarginTop(){
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mContentView
                .getLayoutParams();
        return params.topMargin;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        float[] values = event.values;
        NavScreen navScreen = ((PhoneUi)this).getNavScreen();
//        MenuDialog dialog = null;
//        if (navScreen != null) {
//            dialog = navScreen.mDialog;
//        }
//        if (mNewTabAnimShowing || (dialog != null && dialog.isShowing()) || mTabControl.getIncogMode() || values == null) {
//            return;
//        }
        //x,y,z accelerte
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            if ((Math.abs(values[0]) > SENSOR_ACCERELATE || Math.abs(values[1]) > SENSOR_ACCERELATE || Math
                    .abs(values[2]) > SENSOR_ACCERELATE)) {
                if ((mShakeDialog != null && mShakeDialog.isShowing()) ||
                        (mFirstShakeDialog != null && mFirstShakeDialog.isShowing()) ||
                        (mConfirmShakeDialog != null && mConfirmShakeDialog.isShowing()) ||
                        isReadModeWindowShowing())
                {
                    return;
                } else if (BrowserSettings.getInstance().canShakeRestore()) {
                    if (BrowserSettings.getInstance().isFirstLaunch())
                        showFirstShakeDialog(DataController.getInstance(mActivity).getLastestState());
                    else
                        showShakeDialog(DataController.getInstance(mActivity).getLastestState());
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private AlertDialog mFirstShakeDialog = null;
    private void showFirstShakeDialog (final Bundle state) {
        if (state == null) return;

        final SensorEventListener self = this;
        mFirstShakeDialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.just_shake_the_phone)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.description_of_shake_restore)
                .setNegativeButton(R.string.disable_the_feature, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        BrowserSettings.getInstance().setNotFirstLaunch();
                        BrowserSettings.getInstance().setShakeRestore(false);
                        mSensorManager.unregisterListener(self);
                        Toast.makeText(mActivity, R.string.toast_to_disable_shake_restore, Toast.LENGTH_LONG).show();
                    }
                }).setPositiveButton(R.string.enable_the_feature, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        BrowserSettings.getInstance().setNotFirstLaunch();
                        BrowserSettings.getInstance().setShakeRestore(true);
                        showConfirmShakeDialog(state);
                    }
                }).show();
    }

    private AlertDialog mConfirmShakeDialog = null;
    private void showConfirmShakeDialog (final Bundle state) {
        String url = state.getString(BrowserContract.History.URL);
        String webTitle = state.getString(BrowserContract.History.TITLE, "");
        if (webTitle == null || "".equals(webTitle)) webTitle = url;
        LinearLayout messageView = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.shake_dialog, null);
        TextView titleView = (TextView) messageView.findViewById(R.id.restore_title);
        TextView urlView = (TextView) messageView.findViewById(R.id.restore_url);
        titleView.setText(webTitle);
        urlView.setText(url);

        mConfirmShakeDialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.now_resume_closed_tab)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setView(messageView)
                .setPositiveButton(R.string.shake_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final PhoneUi phoneUi = (PhoneUi)BaseUi.this;
                        int delay = 0;
                        /**
                         * Hide the IME before do the anim
                         */
                        if (phoneUi.isBottomBarHide()) {
                            delay = 200;
                            phoneUi.getBarBase().hideIME();
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // if find any page empty and not incognito, then use it. otherwise open new page
                                Tab tab = mTabControl.findTabWithUrl("", false);
                                if (phoneUi.showingNavScreen()) {
                                    if (tab == null)
                                        phoneUi.openNewTab();
                                    else phoneUi.hideNavScreen(mUiController.getTabControl().
                                            getTabPosition(tab), true);
                                    getActiveTab().restoreBackForwardList(state);
                                } else {
                                    if (tab == null){
                                        NewTabAnim(BottomBarPhone.NEW_TAB_INNER_TAG, state);
                                    }
                                    else {
                                        mUiController.switchToTab(tab);
                                        getActiveTab().restoreBackForwardList(state);
                                    }
                                }
                                DataController.getInstance(mActivity).clearState(state.getString("url", ""));
                            }
                        }, delay);
                    }
                }).show();
    }

    private AlertDialog mShakeDialog = null;
    private void showShakeDialog (final Bundle state) {
        if (state == null) return;
        String url = state.getString(BrowserContract.History.URL);
        String webTitle = state.getString(BrowserContract.History.TITLE, "");
        if (webTitle == null || "".equals(webTitle)) webTitle = url;
        LinearLayout messageView = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.shake_dialog, null);
        TextView titleView = (TextView) messageView.findViewById(R.id.restore_title);
        TextView urlView = (TextView) messageView.findViewById(R.id.restore_url);
        titleView.setText(webTitle);
        urlView.setText(url);
        mShakeDialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.is_resume_closed_tab)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setView(messageView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final PhoneUi phoneUi = (PhoneUi)BaseUi.this;
                        int delay = 0;
                        /**
                         * Hide the IME before do the anim
                         */
                        if (phoneUi.isBottomBarHide()) {
                            delay = 200;
                            phoneUi.getBarBase().hideIME();
                        }
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // if find any page empty and not incognito, then use it. otherwise open new page
                                Tab tab = mTabControl.findTabWithUrl("", false);
                                if (phoneUi.showingNavScreen()) {
                                    if (tab == null)
                                        phoneUi.openNewTab();
                                    else phoneUi.hideNavScreen(mUiController.getTabControl().
                                            getTabPosition(tab), true);
                                    getActiveTab().restoreBackForwardList(state);
                                } else {
                                    if (tab == null){
                                        NewTabAnim(BottomBarPhone.NEW_TAB_INNER_TAG, state);
                                    }
                                    else {
                                        mUiController.switchToTab(tab);
                                        getActiveTab().restoreBackForwardList(state);
                                    }
                                }
                                DataController.getInstance(mActivity).clearState(state.getString("url", ""));
                            }
                        }, delay);
                    }
                }).show();
        WindowManager.LayoutParams lp = mShakeDialog.getWindow().getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        mShakeDialog.getWindow().setAttributes(lp);
    }

    @Override
    public NavigationBarBase getBarBase() {
        return mNavigationBar;
    }

    @Override
    public void changeIncogMode(boolean isIncog){
        mNavigationBar.updateBackground(isIncog);
        if (isIncog) {
            mShadowBottomView.setBackgroundResource(R.drawable.bottom_bar_shadow_private);
            mNavigationHeader.setBackgroundResource(R.drawable.title_bg_private);
            mComplete.setBackgroundResource(R.drawable.btn_title_dark_selector);
            mHeaderTitle.setTextColor(mActivity.getResources().getColor(R.color.white));
        } else {
            mShadowBottomView.setBackgroundResource(R.drawable.bottom_bar_shadow);
            mNavigationHeader.setBackgroundResource(R.drawable.title_bg);
            mComplete.setBackgroundResource(R.drawable.btn_title_blue_selector);
            mHeaderTitle.setTextColor(mActivity.getResources().getColor(R.color.browser_actionbar_title));
        }
        mTitleBar.setMaskRes(isIncog);
        mBottomBarBase.changeIncog(isIncog);
        if (mSearchBar != null)
            mSearchBar.changeIncogMode(isIncog);
    }

    public void saveOffline() {
        String title = getActiveTab().getTitle() + ".mht";
        String path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).toString();
        final String filename = path + "/" + title;
        getActiveTab().getWebView().saveWebArchive(filename);
    }

    @Override
    public void showHomePage() {
        if(!isOpenHomePageFeature()){
            return;
        }
        if (mHomePage.getParent() == null) {
            mContentView.addView(mHomePage);
            FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) mHomePage.getLayoutParams();
            params.topMargin = mTitleBar.getHeight();
            Configuration configuration = mActivity.getResources().getConfiguration();
            mNavigationRightView.dispatchConfigurationChanged(configuration);
            mNavigationLeftView.dispatchConfigurationChanged(configuration);
        }
        mHomePage.setVisibility(View.VISIBLE);
        mHomePage.bringToFront();
        Tab tab = getActiveTab();
        if (tab != null) {
            tab.setShowHomePage(true);
        }
        //mUiController.stopLoading();
        mTitleBar.setProgress(Tab.PROGRESS_MAX);//stop the progress load anim
        //mNavigationBar.formatUrl();
        setBackForwardBtn();
        getTitleBar().getNavigationBar().setDisplayTitle(null);
        translateTitleBar(0);
        getTitleBar().setBtnReadModeVisiblity(false);
    }

    public boolean isShowHomePage() {
        return mHomePage.getVisibility() == View.VISIBLE && isOpenHomePageFeature();
    }

    public void hideHomePage() {
        if(!isOpenHomePageFeature()){
            return;
        }
        if (mHomePage.getParent() != null && mHomePage.getVisibility() == View.VISIBLE) {
            getNavigationEditView().setTranslationY(getNavigationEditView().getTranslationY());
            getNavigationEditViewMask().setAlpha(0);
            mHomePage.setVisibility(View.INVISIBLE);
            Tab tab = getActiveTab();
            if (tab != null) {
                tab.setShowHomePage(false);
            }
            setBackForwardBtn();
            onTabDataChanged(tab);
        }
    }

    public View getHomePage() {
        return mHomePage;
    }
    @Override
    public boolean isCmccFeature() {
        return isCmccFeature;
    }
    @Override
    public boolean isOpenHomePageFeature() {
        return !(isCmccFeature || isFeatureJPEnabled() || isFeatureUSEnabled());
    }

    private static final String US = "US";
    private static final String CN = "CN";
    private static final String JP = "JP";

    public boolean isFeatureJPEnabled() {
        return JP.equals(mFeatureRegon);
    }

    public boolean isFeatureUSEnabled() {
        return US.equals(mFeatureRegon);

    }

    private CheckBox mCheckBox;

    public boolean  isShowMaxTabsDialog(AlertDialog.OnClickListener okListener,
            AlertDialog.OnClickListener cancleListener) {
        if (!isCmccFeature || BrowserSettings.getInstance().isMaxTabsPrompt()) {
            return false;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.tabs_limit_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setPositiveButton(R.string.ok, okListener);
        builder.setNegativeButton(R.string.cancel,cancleListener);
        View dialogView = LayoutInflater.from(mActivity).inflate(
                R.layout.js_block_prompt, null);
        mCheckBox = (CheckBox) dialogView.findViewById(R.id.block);
        mCheckBox.setText(R.string.tabs_limit_not_prompt);
        ((TextView) dialogView.findViewById(R.id.message))
                .setText(R.string.tabs_limit_warning);
        builder.setView(dialogView);
        builder.show();
        return true;
    }

    public void updateCheckPrompt() {
        if (mCheckBox == null) {
            return;
        }
        BrowserSettings.getInstance().setMaxTabsPrompt(mCheckBox.isChecked());
    }
}
