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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.Path.Direction;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.TypedValue;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.provider.Settings;
import android.webkit.ValueCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.android.myapidemo.R;
import com.android.myapidemo.UI;
import com.android.myapidemo.smartisan.animation.Rotate3dAnimation;
/**
 * Ui for regular phone screen sizes
 */
@SuppressLint("NewApi")
public class PhoneUi extends BaseUi {

    private static final String LOGTAG = "PhoneUi";
    private static final int MSG_INIT_NAVSCREEN = 100;

    private NavScreen mNavScreen;
    private NavigationBarPhone mNavigationBar;

    private InputMethodManager mInput;
    boolean mAnimating;
    boolean mShowNav = false;

    private EditText mEditText;
    private Activity mActivity;
    private OnEditBarChangeListener mListener;
    private static int ANIMATION_TIME = 200;
    private static int NAV_FULL_SCREEN_DURATION = 400;
    private static int NAV_SCREEN_DURATION = 400;
    private static int NAV_TRANST_SCREEN_DURATION = 700;
    private boolean mIsDuringCloseAnim = false;
    private ImageView mNavCover;
    boolean mNavScreenShowing = false;
    public static String ABOUT_BLANK = "about:blank";

    private float mCenterX = 0;
    private float mCenterY = 0;
    /**
     * @param browser
     * @param controller
     */
    public PhoneUi(Activity browser, UiController controller) {
        super(browser, controller);
        mActivity = browser;
        System.out.println("================ PhoneUi mActivity ======================"+mActivity);
        mNavigationBar = (NavigationBarPhone) mTitleBar.getNavigationBar();
        TypedValue heightValue = new TypedValue();
        browser.getTheme().resolveAttribute(
                Resources.getSystem().getIdentifier("actionBarSize", "attr", "android"), heightValue, true);
        mInput = (InputMethodManager) browser
                .getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onDestroy() {
        // hideTitleBar();
        mNavigationLeftView.onDestory();
    }

    public void setBtnReadModeVisibility(Tab tab, String url) {
        if (tab != null && tab.inForeground() && !TextUtils.isEmpty(url) && url.equals(tab.getWebView().getUrl())) {
            mTitleBar.setBtnReadModeVisiblity(tab.isBtnReadModeShowing());
        }
    }

    public int getBtnReadModeVisibility(){
        return mNavigationBar.getBtnReadModeVisibility();
    }

    public NavScreen getNavScreen(){
        return mNavScreen;
    }
    public void closeDialog() {
        if (!showingNavScreen()) {
            //if (mNavScreen != null && mNavScreen.mDialog != null) {
                //mNavScreen.mDialog.dismiss();
            //}
        }
    }

    public boolean isCloseTabAnimating() {
        return mIsDuringCloseAnim;
    }

    public boolean isShowNavAnimating() {
        return mNavScreenShowing;
    }

    @Override
    public void editUrl(boolean clearInput, boolean forceIME) {
        if (mUseQuickControls) {
            mTitleBar.setShowProgressOnly(false);
        }
        // Do nothing while at Nav show screen.
        if (mShowNav)
            return;
        super.editUrl(clearInput, forceIME);
    }

    @Override
    public boolean onBackKey() {
        boolean isShowing = true;
        if (mAnimationState) {
            return false;
        }
        if (mNavScreen != null) {
            isShowing = mNavScreen.mBuilder.mIsShowing;
        }
        if (!isShowing) {
            // if hide anim is doing,return back key event.
            return true;
        } else {
            if (showingNavScreen() && mNavScreen != null) {
                mNavScreen.mBuilder.mIsShowing = false;
                mNavScreen.callBackFullScreenAnim();
                return true;
            }
        }
        return super.onBackKey();
    }

    public boolean showingNavScreen() {
        return mShowNav;
    }

    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        return false;
    }

    @Override
    public void onProgressChanged(Tab tab) {
        super.onProgressChanged(tab);
        if (mNavScreen == null && getTitleBar().getHeight() > 0) {
            mHandler.sendEmptyMessage(MSG_INIT_NAVSCREEN);
        }
    }

    @Override
    protected void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.what == MSG_INIT_NAVSCREEN) {
            if (mNavScreen == null) {
                mNavScreen = new NavScreen(mActivity, mUiController, this);
            }
        }
    }

    @Override
    public void setActiveTab(final Tab tab) {
        if (tab == null) {
            return;
        }
        super.setActiveTab(tab);
        // if at Nav screen show, detach tab like what showNavScreen() do.
        if (mShowNav) {
            detachTab(mActiveTab);
        }
        tab.updateBookmarkedStatus();
        updateLockIconToLatest(tab);
    }

    // menu handling callbacks

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuState(mActiveTab, menu);
        return true;
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
/*        MenuItem bm = menu.findItem(R.id.bookmarks_menu_id);
        if (bm != null) {
            bm.setVisible(!showingNavScreen());
        }
        MenuItem abm = menu.findItem(R.id.add_bookmark_menu_id);
        if (abm != null) {
            abm.setVisible((tab != null) && !tab.isSnapshot()
                    && !showingNavScreen());
        }
        MenuItem info = menu.findItem(R.id.page_info_menu_id);
        if (info != null) {
            info.setVisible(false);
        }
        MenuItem newtab = menu.findItem(R.id.new_tab_menu_id);
        if (newtab != null && !mUseQuickControls) {
            newtab.setVisible(false);
        }
        MenuItem incognito = menu.findItem(R.id.incognito_menu_id);
        if (incognito != null) {
            incognito.setVisible(showingNavScreen() || mUseQuickControls);
        }
        MenuItem closeOthers = menu.findItem(R.id.close_other_tabs_id);
        if (closeOthers != null) {
            boolean isLastTab = true;
            if (tab != null) {
                isLastTab = (mTabControl.getTabCount() <= 1);
            }
            closeOthers.setEnabled(!isLastTab);
        }
        if (showingNavScreen()) {
            menu.setGroupVisible(R.id.LIVE_MENU, false);
            menu.setGroupVisible(R.id.SNAPSHOT_MENU, false);
            menu.setGroupVisible(R.id.NAV_MENU, false);
            menu.setGroupVisible(R.id.COMBO_MENU, true);
        }*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*if (showingNavScreen() && (item.getItemId() != R.id.history_menu_id)
                && (item.getItemId() != R.id.snapshots_menu_id)) {
            hideNavScreen(mUiController.getTabControl().getCurrentPosition(),
                    false);
        }*/
        return false;
    }

    @Override
    public boolean isWebShowing() {
        return super.isWebShowing() && (!showingNavScreen() || mAnimationState) ;
    }

    @Override
    public void showWeb(boolean animate) {
        super.showWeb(animate);
        hideNavScreen(mUiController.getTabControl().getCurrentPosition(),
                animate);
    }

    int mTabPosition = 0;
    void showNavScreen() {
        System.out.println("================ ShowNavScreen =========================");
        mOldIncogState = mUiController.getTabControl().getIncogMode();
        mTabPosition = mUiController.getTabControl().getTabPosition(mActiveTab);
        if (mNavScreen == null) {
            System.out.println("================ ShowNavScreen new NavScreen =========================");
            mNavScreen = new NavScreen(mActivity, mUiController, this);
        } else {
            System.out.println("================ ShowNavScreen makeViewPager =========================");
            mNavScreen.mBuilder.makeViewPager(this,
                    mUiController.getTabControl());
        };

        ThumbnailItems thumbnailItems = mNavScreen.getThumView(mTabPosition / 4);
        if (thumbnailItems != null) {
            thumbnailItems.mImageViewLeft.setVisibility(View.GONE);
            thumbnailItems.mImageViewRight.setVisibility(View.GONE);
            thumbnailItems.mImageViewDownLeft.setVisibility(View.GONE);
            thumbnailItems.mImageViewDownRight.setVisibility(View.GONE);
        }

        if (mActiveTab == null && mTabControl.getTabCount() > 0) {
            if (mTabControl.getCurrentPosition() >= 0 && mTabControl.getCurrentPosition() < mTabControl.getTabCount())
                mActiveTab = mTabControl.getTab(mTabControl.getCurrentPosition());
            else
                mActiveTab = mTabControl.getTab(0);
        }
        if (mActiveTab == null) {
            return;
        }

        mHandler.postDelayed(mShowNavRunnable, 500); // avoid ui hang if capture not return
        mActiveTab.getWebViewCapture(new ValueCallback<Bitmap>() {
            @Override
            public void onReceiveValue(Bitmap webCapture) {
                if (!mShowNav) {
                    mWebCapture = webCapture;
                    mHandler.removeCallbacks(mShowNavRunnable);
                    mHandler.post(mShowNavRunnable);
                }
            }
        });
    }

    Runnable mShowNavRunnable = new Runnable() {
        public void run() {
            mShowNav = true;
            mNavScreenShowing = true;
            mUiController.setBlockEvents(true);
            if (isLand()) {
                if (mWebCapture == null) {
                    mWebCapture = Bitmap.createBitmap(1920, 769, Config.ARGB_8888);
                }
                captureBmp(mWebCapture);
                mActiveTab.setBackBitmap(mWebCapture);
            } else {
                mActiveTab.capture(mWebCapture);
            }
            hideSearchBar();
            mNavScreenContainer.addView(mNavScreen, COVER_SCREEN_PARAMS);
            mNavScreenContainer.bringToFront();
            mRoundCorner.bringToFront();
            switch (mTabPosition % 4) {
                case 0:// the first thumbnial's pivot point
                    setNavAnimValues(0.087f, 0.1025f, 0.0869565f, 0.079635f);
                    break;
                case 1:// second
                    setNavAnimValues(0.913f, 0.1025f, 0.9125435f, 0.079635f);
                    break;
                case 2:// third
                    setNavAnimValues(0.087f, 0.84306f, 0.0869565f, 0.825516669f);
                    break;
                case 3:// forth
                    setNavAnimValues(0.913f, 0.84306f, 0.9125435f, 0.825516669f);
                    break;
                default:
                    mUiController.setBlockEvents(false);
                    break;
            }
            finishAnimationIn();
        }
    };

    private void captureBmp(Bitmap bmp) {
        if (bmp == null) {
            return;
        }
        Bitmap pic = Bitmap.createBitmap(bmp, 0, 0, 1920, 769);//800
        drawTabInBitmap(pic);
    }

    private void drawTabInBitmap(Bitmap content) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(1920, 927, Config.ARGB_8888);//936
            Canvas c = new Canvas(bitmap);
            c.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG
                    | Paint.FILTER_BITMAP_FLAG));
            int left = 0;
            int top = 0;
            int state = c.save();
            c.translate(-left, -top);
            float scale = bitmap.getWidth() / content.getWidth();
            c.scale(1.0f, scale, left, top);
            c.drawBitmap(content, 0, 0, null);
            c.restoreToCount(state);
            mActiveTab.capture(bitmap);
            c.setBitmap(null);
        } catch (OutOfMemoryError err){
        }
    }

    private boolean isLand(){
        int orientation = mActivity.getResources().getConfiguration().orientation;
        return (orientation == Configuration.ORIENTATION_LANDSCAPE ? true
                        : false);
    }

    void setNavAnimValues(float pivotlX, float pivotlY, float pivotpX, float pivotpY){
        int orientation = mActivity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            setNavAnim(mNavScreenContainer, pivotpX, pivotpY);
        }else {
            setNavAnim(mNavScreenContainer, pivotlX, pivotlY);
        }
    }
    private void finishAnimationIn() {
        if (showingNavScreen()) {
            // notify accessibility manager about the screen change
            mNavScreen
                    .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            mTabControl.setOnThumbnailUpdatedListener(mNavScreen);
        }
    }

    void closeAllTab() {
        if (mNavScreen == null || mNavScreen.mBuilder == null)
            return;
        ThumbnailItems currentThumbnailItems = mNavScreen.mBuilder
                .getcurrentTabVIew();
        if(currentThumbnailItems == null){
            return;
        }
        mIsDuringCloseAnim = true;
        final FrameLayout frameLayoutLeft = currentThumbnailItems.frameLayoutLeft;
        final FrameLayout frameLayoutRight = currentThumbnailItems.frameLayoutRight;
        final FrameLayout frameLayoutDleft = currentThumbnailItems.frameLayoutDownLeft;
        final FrameLayout frameLayoutDright = currentThumbnailItems.frameLayoutDownRight;

        PropertyValuesHolder pvhAlphal = PropertyValuesHolder.ofFloat("alpha",
                1.0f, 0f);
        PropertyValuesHolder pvhScaleXl = PropertyValuesHolder.ofFloat(
                "scaleX", frameLayoutLeft.getScaleX(),
                frameLayoutLeft.getScaleX() / 5);
        PropertyValuesHolder pvhScaleYl = PropertyValuesHolder.ofFloat(
                "scaleY", frameLayoutLeft.getScaleY(),
                frameLayoutLeft.getScaleY() / 5);
        Animator disAniml = ObjectAnimator.ofPropertyValuesHolder(
                frameLayoutLeft, pvhAlphal, pvhScaleXl, pvhScaleYl);
        disAniml.setDuration(200);

        PropertyValuesHolder pvhAlphar = PropertyValuesHolder.ofFloat("alpha",
                1.0f, 0f);
        PropertyValuesHolder pvhScaleXr = PropertyValuesHolder.ofFloat(
                "scaleX", frameLayoutRight.getScaleX(),
                frameLayoutRight.getScaleX() / 5);
        PropertyValuesHolder pvhScaleYr = PropertyValuesHolder.ofFloat(
                "scaleY", frameLayoutRight.getScaleY(),
                frameLayoutRight.getScaleY() / 5);
        Animator disAnimr = ObjectAnimator.ofPropertyValuesHolder(
                frameLayoutRight, pvhAlphar, pvhScaleXr, pvhScaleYr);
        disAnimr.setDuration(200);

        PropertyValuesHolder pvhAlphadl = PropertyValuesHolder.ofFloat("alpha",
                1.0f, 0f);
        PropertyValuesHolder pvhScaleXdl = PropertyValuesHolder.ofFloat(
                "scaleX", frameLayoutDleft.getScaleX(),
                frameLayoutDleft.getScaleX() / 5);
        PropertyValuesHolder pvhScaleYdl = PropertyValuesHolder.ofFloat(
                "scaleY", frameLayoutDleft.getScaleY(),
                frameLayoutDleft.getScaleY() / 5);
        Animator disAnimdl = ObjectAnimator.ofPropertyValuesHolder(
                frameLayoutDleft, pvhAlphadl, pvhScaleXdl, pvhScaleYdl);
        disAnimdl.setDuration(200);

        PropertyValuesHolder pvhAlphadr = PropertyValuesHolder.ofFloat("alpha",
                1.0f, 0f);
        PropertyValuesHolder pvhScaleXdr = PropertyValuesHolder.ofFloat(
                "scaleX", frameLayoutDright.getScaleX(),
                frameLayoutDright.getScaleX() / 5);
        PropertyValuesHolder pvhScaleYdr = PropertyValuesHolder.ofFloat(
                "scaleY", frameLayoutDright.getScaleY(),
                frameLayoutDright.getScaleY() / 5);
        Animator disAnimdr = ObjectAnimator.ofPropertyValuesHolder(
                frameLayoutDright, pvhAlphadr, pvhScaleXdr, pvhScaleYdr);
        disAnimdr.setDuration(200);
        AnimatorSet asDimiss = new AnimatorSet();
        asDimiss.setDuration(300);
        asDimiss.playTogether(disAniml, disAnimr, disAnimdl, disAnimdr);
        asDimiss.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                frameLayoutLeft.setScaleX(1f);
                frameLayoutLeft.setScaleY(1f);
                frameLayoutRight.setScaleX(1f);
                frameLayoutRight.setScaleY(1f);
                frameLayoutDleft.setScaleX(1f);
                frameLayoutDleft.setScaleY(1f);
                frameLayoutDright.setScaleX(1f);
                frameLayoutDright.setScaleY(1f);
                mUiController.closeAllTabs();
                openNewTab();
                mNavScreen.reverse(animation, animation.getDuration());
                List<ThumbnailItems> list = mNavScreen.mBuilder.getList();
                for (ThumbnailItems thumbnailItems : list) {
                    thumbnailItems.hideTabViewTitle();
                }
            }
        });
        asDimiss.start();
    }

    public void openNewTab() {// open new tab in nav screen
        setNewTabAnimStatus(true);
        // need to call openTab explicitely with setactive false
        final Tab tab;
        if (mTabControl.getIncogMode()) {
            tab = mUiController.openIncognitoTab();
        } else {
            tab = mUiController.openTabToHomePage();
        }
        // .openTab(BrowserSettings.getInstance().getHomePage(),
        // false, false, false);
        if (tab != null) {
            mUiController.setBlockEvents(true);
            final int tix = mTabControl.getTabPosition(tab);
            openNewTab(tix);
            switchToTab(tab);
            mUiController.setBlockEvents(false);
        }
    }

    private void switchToTab(Tab tab) {
        if (tab != getActiveTab()) {
            mUiController.setActiveTab(tab);
        }
    }

    void openNewTab(final int position) {
        FrameLayout frameLayout = null;
        mNavScreen.mBuilder.makeViewPager(this, mUiController.getTabControl());
        final ThumbnailItems thumbnailItems = mNavScreen.getThumView(position / 4);
        switch (position % 4) {
        case 0:
            frameLayout = thumbnailItems.frameLayoutLeft;
            break;
        case 1:
            frameLayout = thumbnailItems.frameLayoutRight;
            break;
        case 2:
            frameLayout = thumbnailItems.frameLayoutDownLeft;
            break;
        case 3:
            frameLayout = thumbnailItems.frameLayoutDownRight;
            break;
        default:
            break;
        }
        mNavScreen.mBuilder.setCurrentView(position);
        PropertyValuesHolder pvhAlphal = PropertyValuesHolder.ofFloat("alpha",
                0f, 1.0f);
        PropertyValuesHolder pvhScaleXl = PropertyValuesHolder.ofFloat(
                "scaleX", frameLayout.getScaleX() / 5, frameLayout.getScaleX());
        PropertyValuesHolder pvhScaleYl = PropertyValuesHolder.ofFloat(
                "scaleY", frameLayout.getScaleY() / 5, frameLayout.getScaleY());
        Animator showAnim = ObjectAnimator.ofPropertyValuesHolder(frameLayout,
                pvhAlphal, pvhScaleXl, pvhScaleYl);
        showAnim.setDuration(300);
        showAnim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                mContentView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideNavScreen(position, true);
                    }
                }, 180);
                super.onAnimationStart(animation);
            }
        });
        showAnim.start();
    }

    void hideNavScreen(int position, boolean animate) {
        hideNavScreen(position, animate,null);
    }

    void hideNavScreen(int position, boolean animate,final Animator reverseAnimator) {
        if (!showingNavScreen()){
            finishAnimateOut();
            return;
        }
        mShowNav = false;
        final Tab tab = mUiController.getTabControl().getTab(position);
        Browser browser = (Browser) mActivity.getApplication();
        browser.setmIsFromThumbnial(true);
        if ((tab == null) || !animate) {
            if (tab != null) {
                mUiController.setActiveTab(tab);
            } else if (mTabControl.getTabCount() > 0) {
                // use a fallback tab
                mUiController.setActiveTab(mTabControl.getCurrentTab());
            }
            mContentView.setVisibility(View.VISIBLE);
            finishAnimateOut();
            return;
        }
        ThumbnailItems thumbnailItems = mNavScreen.getThumView(position / 4);
        if (thumbnailItems == null) {
            if (mTabControl.getTabCount() > 0) {
                // use a fallback tab
                mUiController.setActiveTab(mTabControl.getCurrentTab());
            }
            mContentView.setVisibility(View.VISIBLE);
            finishAnimateOut();
            return;
        }
        mUiController.setActiveTab(tab);
        mContentView.setVisibility(View.VISIBLE);
        int itemPosition = position % 4;
        thumbnailItems.mImageViewLeft.setVisibility(View.GONE);
        thumbnailItems.mImageViewRight.setVisibility(View.GONE);
        thumbnailItems.mImageViewDownLeft.setVisibility(View.GONE);
        thumbnailItems.mImageViewDownRight.setVisibility(View.GONE);
        switch (itemPosition) {
        case 0:
            setFullAnimValues(0.087f, 0.09f, 0.0869565f, 0.079635f,reverseAnimator);
            break;
        case 1:
            setFullAnimValues(0.913f, 0.09f, 0.9125435f, 0.079635f,reverseAnimator);
            break;
        case 2:
            setFullAnimValues(0.087f, 0.736f, 0.0869565f, 0.825516669f,reverseAnimator);
            break;
        case 3:
            setFullAnimValues(0.913f, 0.736f, 0.9125435f, 0.825516669f,reverseAnimator);
            break;
        default:
            break;
        }
    }

    void setFullAnimValues(float pivotlX,float pivotlY,float pivotpX,float pivotpY,Animator reverseAnimator){
        int orientation = mActivity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            setFullAnim(mNavScreen, pivotpX, pivotpY,reverseAnimator);
        }else {
            setFullAnim(mNavScreen, pivotlX, pivotlY,reverseAnimator);
        }
    }
    // anim for enter full screen
    public void setFullAnim(final NavScreen navScreen, float pivotx,
            float pivoty,final Animator reverseAnimator) {
        navScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        final ImageView tempScreen = new ImageView(mActivity);
        int orientation = mActivity.getResources().getConfiguration().orientation;
        final Bitmap bitmap = safeCreateBitmap(mBottomBarBase.getWidth(),
                mBottomBarBase.getHeight());
        if(orientation == Configuration.ORIENTATION_PORTRAIT){
            tempScreen.measure(MeasureSpec.makeMeasureSpec(
                    mBottomBarBase.getWidth(), MeasureSpec.EXACTLY), MeasureSpec
                    .makeMeasureSpec(0,
                            MeasureSpec.EXACTLY));
            tempScreen.layout(0, 0, mBottomBarBase.getWidth(),
                    mBottomBarBase.getHeight());
            if (bitmap != null) {
                Canvas c = new Canvas(bitmap);
                mBottomBarBase.draw(c);
                c.setBitmap(null);
            }
            tempScreen.setImageBitmap(bitmap);
          mNavScreenContainer.addView(tempScreen,
          COVER_SCREENANIM_SIZE);
        }
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat("scaleX",
                navScreen.getScaleX(), (float) (navScreen.getScaleX() * 2.353));
        PropertyValuesHolder pvhScaleY;
        if (isLand()) {
            pvhScaleY = PropertyValuesHolder.ofFloat("scaleY",
                    navScreen.getScaleY(), (float) (navScreen.getScaleY() * 2.82));
        } else {
            pvhScaleY = PropertyValuesHolder.ofFloat("scaleY",
                    navScreen.getScaleX(), (float) (navScreen.getScaleX() * 2.353));
        }
        Animator zoomAnim = ObjectAnimator.ofPropertyValuesHolder(navScreen,
                pvhScaleX, pvhScaleY);
        navScreen.setPivotX(pivotx * navScreen.getWidth());
        navScreen.setPivotY(pivoty * navScreen.getHeight());
        int value = (int) mActivity.getResources().getDimension(
                R.dimen.thumbnail_zoom_value);

        AnimatorSet together = new AnimatorSet();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ObjectAnimator oTran = ObjectAnimator.ofFloat(tempScreen,
                    "translationY", mBottomBarBase.getHeight() + value, value);
            together.playTogether(zoomAnim, oTran);
        } else {
            together.playTogether(zoomAnim);
        }
        together.setDuration(NAV_FULL_SCREEN_DURATION);
        together.setInterpolator(new DecelerateInterpolator(1.5f));
        together.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                List<ThumbnailItems> list = mNavScreen.mBuilder.getList();
                for (ThumbnailItems thumbnailItems : list) {
                    thumbnailItems.hideTabViewTitle();
                    if (isLand()) {
                        thumbnailItems.changeBitmap();
                    }
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setNewTabAnimStatus(false);
                finishAnimateOut();
                navScreen.setScaleX(1f);
                navScreen.setScaleY(1f);
                if (tempScreen != null) {
                    mNavScreenContainer.removeView(tempScreen);
                    tempScreen.setImageBitmap(null);
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                }
                if (reverseAnimator != null) {
                    mNavScreen.reverse(reverseAnimator, 0);
                }
                navScreen.setLayerType(View.LAYER_TYPE_NONE, null);
                if (mAnimationState) {
                    //switchOnConfigchange();
                    mAnimationState = false;
                }

                AnimatorSet aSet = new AnimatorSet();
                aSet.setDuration(150);
                addAnimationToList(mNavigationBar.getSearchInputView(), "alpha", 0.0f, 1.0f);
                addAnimationToList(mNavigationBar.getUrlInputView(), "alpha", 0.0f, 1.0f);
                aSet.playTogether(mAnimList);
                aSet.start();
            }
        });
        together.start();
    }
    private ArrayList<Animator> mAnimList = new ArrayList<Animator>();
    private void addAnimationToList(Object target,String propertyName,float value1,float value2){
        if(propertyName.equals("right") || propertyName.equals("left")){
            ObjectAnimator objAnimator = ObjectAnimator.ofInt(target,
                    propertyName, (int)value1, (int)value2);
            mAnimList.add(objAnimator);
        }else {
            ObjectAnimator objAnimator = ObjectAnimator.ofFloat(target,
                    propertyName, value1, value2);
            mAnimList.add(objAnimator);
        }
    }

    // anim for enter nav screen
    public void setNavAnim(final FrameLayout frameLayout, float pivotx,
            float pivoty) {
        frameLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        int orientation = mActivity.getResources().getConfiguration().orientation;
        final ImageView tempScreen = new ImageView(mActivity);
        final Bitmap bitmap = safeCreateBitmap(mBottomBarBase.getWidth(),
                mBottomBarBase.getHeight());
        if(orientation == Configuration.ORIENTATION_PORTRAIT){
            tempScreen.measure(MeasureSpec.makeMeasureSpec(
                    mBottomBarBase.getWidth(), MeasureSpec.EXACTLY), MeasureSpec
                    .makeMeasureSpec(0,
                            MeasureSpec.EXACTLY));
            tempScreen.layout(0, 0, mBottomBarBase.getWidth(),
                    mBottomBarBase.getHeight());
            if (bitmap != null) {
                Canvas c = new Canvas(bitmap);
                mBottomBarBase.draw(c);
                c.setBitmap(null);
            }
            tempScreen.setImageBitmap(bitmap);
          mNavScreenContainer.addView(tempScreen,
          COVER_SCREENANIM_SIZE);
        }
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat("scaleX",
                (float) (frameLayout.getScaleX() * 2.353),
                frameLayout.getScaleX());
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat("scaleY",
                (float) (frameLayout.getScaleY() * 2.353),
                frameLayout.getScaleY());
        Animator zoomAnim = ObjectAnimator.ofPropertyValuesHolder(frameLayout,
                pvhScaleX, pvhScaleY);
        frameLayout.setPivotX(pivotx * frameLayout.getWidth());
        frameLayout.setPivotY(pivoty * frameLayout.getHeight());
        int value = mCustomScreen.getHeight();
        ObjectAnimator oTran = ObjectAnimator.ofFloat(tempScreen,
                "translationY", value, mBottomBarBase.getHeight() + value);
        AnimatorSet together = new AnimatorSet();
        if (isDoPrivateAnim) {
            zoomAnim.setDuration(NAV_TRANST_SCREEN_DURATION);
            zoomAnim.setInterpolator(new AccelerateInterpolator(1.5f));
        } else {
            zoomAnim.setDuration(NAV_SCREEN_DURATION);
            zoomAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        }
        oTran.setDuration(NAV_SCREEN_DURATION);
        oTran.setInterpolator(new AccelerateInterpolator(1.5f));
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            together.playTogether(zoomAnim, oTran);
        } else {
            together.playTogether(zoomAnim);
        }
        together.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                List<ThumbnailItems> list = mNavScreen.mBuilder.getList();
                for (ThumbnailItems thumbnailItems : list) {
                    thumbnailItems.showTabViewTitle();
                }
                if (isDoPrivateAnim) {
                    mAnimationState = true;
                    boolean isIncog = mUiController.getTabControl().getIncogMode();
                    changeIncogMode(!isIncog);
                    frameLayout.setLayerType(View.LAYER_TYPE_NONE, null);
                    //switchOnConfigchange();
                    trans3DAnimation(mOldIncogState, false);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (tempScreen != null) {
                    mNavScreenContainer.removeView(tempScreen);
                    tempScreen.setImageBitmap(null);
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                }
                if (mNavScreen.mBuilder.mIsShowing) {//if the navscreen is show,the close btn just show.
                    List<ThumbnailItems> list = mNavScreen.mBuilder.getList();
                    for (ThumbnailItems thumbnailItems : list) {
                        thumbnailItems.mImageViewLeft.setVisibility(View.VISIBLE);
                        thumbnailItems.mImageViewRight.setVisibility(View.VISIBLE);
                        thumbnailItems.mImageViewDownLeft.setVisibility(View.VISIBLE);
                        thumbnailItems.mImageViewDownRight.setVisibility(View.VISIBLE);
                    }
                }

                mBottomBarBase.getSwitchTabBtn().setClickable(true);
                frameLayout.setScaleX(1f);
                frameLayout.setScaleY(1f);
                frameLayout.setLayerType(View.LAYER_TYPE_NONE, null);
                mNavScreenShowing = false;
//                mActiveTab.putInBackground();
//                detachTab(mActiveTab);
                mUiController.setBlockEvents(false);
            }
        });
        together.start();
    }

    boolean mSwicthState = false;

    private void switchOnConfigchange() {
        int flag = Settings.System.getInt(mActivity.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0);
        if (flag == 1) {
            Settings.System.putInt(mActivity.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, flag == 1 ? 0 : 1);
            mSwicthState = true;
        } else {
            if (mSwicthState) {
                Settings.System.putInt(mActivity.getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, flag == 1 ? 0 : 1);
                mSwicthState = false;
            }
        }
    }

    private void makeViewPager(){
        if (mNavScreen == null) {
            mNavScreen = new NavScreen(mActivity, mUiController, this);
        } else {
            mNavScreen.mBuilder.makeViewPager(this,
                    mUiController.getTabControl());
        }
    }

    private boolean mOldIncogState;
    private boolean mReverse = false;

    private boolean mAnimationState;
    private void trans3DAnimation(boolean isIncog, boolean reverse) {
        Rotate3dAnimation rotation;
        mReverse = reverse;
        mCenterX = 540;
        mCenterY = 540;
        mNavScreen.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (isIncog) {
            if (mReverse) {
                rotation = new Rotate3dAnimation(90, 0, mCenterX, mCenterY,
                        310.0f, mReverse, isIncog);
                rotation.setInterpolator(new DecelerateInterpolator(1.5f));
            } else {
                rotation = new Rotate3dAnimation(360, 270, mCenterX, mCenterY,
                        310.0f, mReverse, isIncog);
                rotation.setInterpolator(new AccelerateInterpolator(1.5f));
            }
        } else {
            if (mReverse) {
                rotation = new Rotate3dAnimation(270, 360, mCenterX, mCenterY,
                        310.0f, mReverse, isIncog);
                rotation.setInterpolator(new DecelerateInterpolator(1.5f));
            } else {
                rotation = new Rotate3dAnimation(0, 90, mCenterX, mCenterY,
                        310.0f, mReverse, isIncog);// 310f
                rotation.setInterpolator(new AccelerateInterpolator(1.5f));
            }
        }
        rotation.setDuration(700);
        rotation.setAnimationListener(new TurnToImageView());
        mNavScreen.startAnimation(rotation);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            makeViewPager();
        }
    };

    class TurnToImageView implements AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {
            if (!mReverse) {
                mUiController.setBlockEvents(true);
                mNavScreen.mBuilder.mIsShowing = false;
                mNavScreenContainer.setBackgroundColor(Color.BLACK);
            }
        }
        @Override
        public void onAnimationEnd(Animation animation) {
            if (mReverse) {
                mNavScreenContainer.setBackground(null);
                int position = mUiController.getTabControl().getCurrentPosition();
                if (position == -1) {
                    position = 0;
                }
                hideNavScreen(position, true);
                mNavScreen.mBuilder.mIsShowing = true;
                mUiController.setBlockEvents(false);
                //remove navscreen shadow
            } else {
                mNavScreen.changeIncogStyle(!mOldIncogState);
                mUiController.getTabControl().setIncogMode(!mOldIncogState);
                mHandler.post(mRunnable);
                /*if(!mOldIncogState){
                    if (mNavCover == null) {
                        mNavCover = new ImageView(mActivity);
                    }
                    mNavCover.setBackgroundResource(R.drawable.navscreen_incog_cover);
                    // add navscreen shadow
                    //mCustomViewContainer.addView(mNavCover, COVER_SCREEN_PARAMS);
                }*/
                trans3DAnimation(mOldIncogState,true);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }

    private void finishAnimateOut() {
        getTitleBar().setVisibility(View.VISIBLE);
        mNavScreenContainer.removeView(mNavScreen);
//        mTabControl.setOnThumbnailUpdatedListener(null);
        mBottomBarBase.getSwitchTabBtn().setClickable(true);
        mIsDuringCloseAnim = false;
        mNavScreen.mBuilder.mIsShowing = true;
        mUiController.setBlockEvents(false);
        /**
         * it is need not to request,only Set whether this view can receive focus while in touch mode
         */
//        urlInputView.requestFocus();
    }

    @Override
    public boolean needsRestoreAllTabs() {
        return false;
    }

    public void toggleNavScreen() {
        System.out.println("======================= toggleNavScreen 111111 =======================");
        if (!showingNavScreen()) {
            showNavScreen();
        } else {
            hideNavScreen(mUiController.getTabControl().getCurrentPosition(),
                    false);
        }
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNavigationBar != null) {
            mNavigationBar.changeSearchIcon();
        }
        showTitleBottomBar(UI.VIEW_ALL_MASK, false);
    }

    public class OnEditBarChangeListener implements OnLayoutChangeListener {
        @Override
        public void onLayoutChange(View view, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

            LinearLayout l1 = (LinearLayout) view.findViewById(R.id.find_edit);
            mEditText = (EditText) view.findViewById(R.id.edit);
            int end = mActivity.getResources().getInteger(
                    R.integer.find_on_page_end_point);
            l1.setRight(end);
            TextView doneBtn = (TextView) view.findViewById(R.id.finish);
            RelativeLayout findBtn = (RelativeLayout) view
                    .findViewById(R.id.find_button);
            ObjectAnimator doneBtnAnim1 = ObjectAnimator.ofFloat(doneBtn,
                    "translationX", -(float) doneBtn.getWidth(), 0f);
            float btnRight = (float) doneBtn.getWidth();
            int editLeft = (int) btnRight;
            int editRight = mActivity.getResources().getInteger(
                    R.integer.find_on_page_end_point)
                    - findBtn.getWidth();
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(l1,
                    "alpha", 0f, 1f);
            ObjectAnimator findBtnAnim1 = ObjectAnimator.ofFloat(findBtn,
                    "translationX", btnRight, 0f);

            PropertyValuesHolder searchLeft = PropertyValuesHolder.ofInt(
                    "left", l1.getLeft(), editLeft);

            PropertyValuesHolder searchRight = PropertyValuesHolder.ofInt(
                    "right", l1.getRight(), editRight);
            Animator inputAnim = ObjectAnimator.ofPropertyValuesHolder(l1,
                    searchLeft, searchRight);
            AnimatorSet asAnimatorSet = new AnimatorSet();
            asAnimatorSet.playTogether(inputAnim, doneBtnAnim1, findBtnAnim1,
                    alphaAnim);
            asAnimatorSet.setDuration(ANIMATION_TIME);
            asAnimatorSet.start();
            asAnimatorSet.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mEditText.requestFocus();
                    getSearchBar().setSelectionState(mEditText.hasSelection());
                    mInput.showSoftInput(mEditText,
                            InputMethodManager.SHOW_IMPLICIT);
                    getSearchBar().removeOnLayoutChangeListener(mListener);
                }

                @Override
                public void onAnimationCancel(Animator arg0) {

                }
            });
        }
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
    public void showEditBarAnim() {
        getTitleBar().setVisibility(View.INVISIBLE);
        getSearchBar().setVisibility(View.VISIBLE);
        translateTitleBar(0);
        mListener = new OnEditBarChangeListener();
        getSearchBar().addOnLayoutChangeListener(mListener);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    @Override
    public void onPause() {
        if (mAnimationState) {
            mUiController.getTabControl().setIncogMode(!mOldIncogState);
        }
        super.onPause();
    }

    private boolean isDoPrivateAnim = false;

    public void setPrivateAnim(boolean isChangePrivate) {
        isDoPrivateAnim = isChangePrivate;
    }

    @Override
    public void showAutoLogin(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void hideAutoLogin(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void hideBar() {
        // TODO Auto-generated method stub
        
    }
}
