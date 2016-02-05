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

import com.android.myapidemo.R;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;

import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Base class for a title bar used by the browser.
 */
public class TitleBar extends RelativeLayout implements ViewTreeObserver.OnPreDrawListener {

    private static final float ANIM_TITLEBAR_DECELERATE = 2.5f;

    private UiController mUiController;
    private BaseUi mBaseUi;
    private FrameLayout mContentView;
    private AccessibilityManager mAccessibilityManager;

    private NavigationBarBase mNavBar;
    private ImageView mMask;

    //state
    private boolean mShowing;
    private boolean mInLoad;
    private boolean mSkipTitleBarAnimations;
    private Animator mTitleBarAnimator;
    private boolean mIsFixedTitleBar;
    private float mCurrentTranslationY;
    private boolean mUpdateTranslationY = false;

    public TitleBar(Context context, UiController controller, BaseUi ui,
            FrameLayout contentView) {
        super(context, null);
        mUiController = controller;
        mBaseUi = ui;
        mContentView = contentView;
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        initLayout(context);
        setFixedTitleBar();
    }

    private void initLayout(Context context) {
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.title_bar, this);
        mNavBar = (NavigationBarBase) findViewById(R.id.taburlbar);
        mNavBar.setTitleBar(this);
        mMask = (ImageView) findViewById(R.id.addressmask);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setMaskVisiblity(false);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        setFixedTitleBar();
        if(mNavBar != null){
            mNavBar.onConfigurationChanged(config);
        }
        if(config.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setMaskVisiblity(false);
        }else{
            setMaskVisiblity(true);
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mBaseUi.setContentViewMarginTop(0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mCurrentTranslationY = this.getTranslationY();
        if (mCurrentTranslationY < 0) {
            mUpdateTranslationY = true;
            this.setTranslationY(0);

            final ViewTreeObserver observer = this.getViewTreeObserver();
            observer.addOnPreDrawListener(this);
        }
    }

    @Override
    public boolean onPreDraw() {
        if (mUpdateTranslationY) {
            this.setTranslationY(mCurrentTranslationY);
            mUpdateTranslationY = false;
        }
        final ViewTreeObserver observer = this.getViewTreeObserver();
        observer.removeOnPreDrawListener(this);
        return true;
    }

    private void setFixedTitleBar() {
        boolean isFixed = false;

        isFixed |= mAccessibilityManager.isEnabled() &&
            mAccessibilityManager.isTouchExplorationEnabled();
        // If getParent() returns null, we are initializing
        ViewGroup parent = (ViewGroup)getParent();
        if (mIsFixedTitleBar == isFixed && parent != null) return;
        mIsFixedTitleBar = isFixed;
        setSkipTitleBarAnimations(true);
        show();
        setSkipTitleBarAnimations(false);
        if (parent != null) {
            parent.removeView(this);
        }
        mContentView.addView(this, makeLayoutParams());
        mBaseUi.setContentViewMarginTop(0);
    }

    public BaseUi getUi() {
        return mBaseUi;
    }

    public UiController getUiController() {
        return mUiController;
    }

    void setShowProgressOnly(boolean progress) {
        if (progress) {
            mNavBar.setVisibility(View.GONE);
        } else {
            mNavBar.setVisibility(View.VISIBLE);
        }
    }

    public void setBtnReadModeVisiblity(boolean isVisibility){
        mNavBar.setBtnReadModeVisiblity(isVisibility);
    }

    public void setMaskVisiblity(boolean isVisibility) {
        if (isVisibility) {
            mMask.setVisibility(View.VISIBLE);
        } else {
            mMask.setVisibility(View.GONE);
        }
    }

    public void setMaskRes(boolean isIncog) {
        if (isIncog) {
            mMask.setBackgroundResource(R.drawable.addressbar_mask_private);
        } else {
            mMask.setBackgroundResource(R.drawable.addressbar_mask);
        }
    }

    void setSkipTitleBarAnimations(boolean skip) {
        mSkipTitleBarAnimations = skip;
    }

    void setupTitleBarAnimator(Animator animator) {
        Resources res = getContext().getResources();
        int duration = res.getInteger(R.integer.titlebar_animation_duration);
        animator.setInterpolator(new DecelerateInterpolator(
                ANIM_TITLEBAR_DECELERATE));
        animator.setDuration(duration);
    }

    //Disable stock autohide behavior in favor of top controls
    private static final  boolean bOldStyleAutoHideDisabled = true;
    void show() {
        cancelTitleBarAnimation(false);
        if (mSkipTitleBarAnimations) {
            this.setVisibility(View.VISIBLE);
            this.setTranslationY(0);
            // reaffirm top-controls
            if (isFixed() || isInLoad())
                showTopControls();
            else
                enableTopControls();
        } else if (!bOldStyleAutoHideDisabled) {
            int visibleHeight = getVisibleTitleHeight();
            float startPos = (-getEmbeddedHeight() + visibleHeight);
            if (getTranslationY() != 0) {
                startPos = Math.max(startPos, getTranslationY());
            }
            mTitleBarAnimator = ObjectAnimator.ofFloat(this,
                    "translationY",
                    startPos, 0);
            setupTitleBarAnimator(mTitleBarAnimator);
            mTitleBarAnimator.start();
        }

        mShowing = true;
    }

    void hide() {
        if (mIsFixedTitleBar || bOldStyleAutoHideDisabled) return;
        if (!mSkipTitleBarAnimations) {
            cancelTitleBarAnimation(false);
            int visibleHeight = getVisibleTitleHeight();
            mTitleBarAnimator = ObjectAnimator.ofFloat(this,
                    "translationY", getTranslationY(),
                    (-getEmbeddedHeight() + visibleHeight));
            mTitleBarAnimator.addListener(mHideTileBarAnimatorListener);
            setupTitleBarAnimator(mTitleBarAnimator);
            mTitleBarAnimator.start();
        } else {
            onScrollChanged();
        }
        mShowing = false;
    }

    boolean isShowing() {
        return mShowing;
    }

    void cancelTitleBarAnimation(boolean reset) {
        if (mTitleBarAnimator != null) {
            mTitleBarAnimator.cancel();
            mTitleBarAnimator = null;
        }
        if (reset) {
            setTranslationY(0);
        }
    }

    private AnimatorListener mHideTileBarAnimatorListener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // update position
            onScrollChanged();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    private int getVisibleTitleHeight() {
       /* Tab tab = mBaseUi.getActiveTab();
        WebView webview = tab != null ? tab.getWebView() : null;
        return webview != null ? webview.getVisibleTitleHeight() : 0;*/
        return 0;
    }

    private void hideTopControls() {
        Tab tab = mBaseUi.getActiveTab();
        WebView view = tab != null ? tab.getWebView() : null;
/*        if (view != null)
            view.updateTopControls(true, false, true);*/
    }

    private void showTopControls() {
        Tab tab = mBaseUi.getActiveTab();
        WebView view = tab != null ? tab.getWebView() : null;
        //if (view != null)
            //view.updateTopControls(false, true, true);
    }

    private void enableTopControls() {
        Tab tab = mBaseUi.getActiveTab();
        WebView view = tab != null ? tab.getWebView() : null;
        //if (view != null)
            //view.updateTopControls(true, true, true);
    }


    /**
     * Update the progress, from 0 to 100.
     */
    public void setProgress(int newProgress) {
        if (newProgress >= Tab.PROGRESS_MAX) {
            mInLoad = false;
            mNavBar.onProgressStopped();
        } else if (newProgress < Tab.INITIAL_PROGRESS) {
            ;// wrong progress. should do nothing here
        } else {
            if (!mInLoad) {
                mInLoad = true;
                mNavBar.onProgressStarted();
            }
        }
    }

    public int getEmbeddedHeight() {
        if (mIsFixedTitleBar) return 0;
        return calculateEmbeddedHeight();
    }

     public boolean isFixed() {
        return mIsFixedTitleBar;
    }

    int calculateEmbeddedHeight() {
        int height = mNavBar.getHeight();
        return height;
    }

    public boolean wantsToBeVisible() {
        return true;
    }

    public boolean isEditingUrl() {
        return mNavBar.isEditingUrl();
    }

    public WebView getCurrentWebView() {
        Tab t = mBaseUi.getActiveTab();
        if (t != null) {
            return t.getWebView();
        } else {
            return null;
        }
    }

    public NavigationBarBase getNavigationBar() {
        return mNavBar;
    }

    public boolean isInLoad() {
        return mInLoad;
    }

    private ViewGroup.LayoutParams makeLayoutParams() {
        return new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return mBaseUi.isCustomViewShowing() ? false :
            super.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public View focusSearch(View focused, int dir) {
        WebView web = getCurrentWebView();
        if (FOCUS_DOWN == dir && hasFocus() && web != null
                && web.hasFocusable() && web.getParent() != null) {
            return web;
        }
        return super.focusSearch(focused, dir);
    }

    public void onTabDataChanged(Tab tab) {
        mNavBar.setVisibility(VISIBLE);
    }

    public void onScrollChanged() {
        if (!mShowing && !mIsFixedTitleBar) {
            setTranslationY(getVisibleTitleHeight() - getEmbeddedHeight());
        }
    }

    public void onResume() {
        setFixedTitleBar();
    }

    public void onPause() {
        if (mNavBar != null) {
            mNavBar.onPause();
        }
    }
}
