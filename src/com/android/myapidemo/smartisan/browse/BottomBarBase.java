/**
 *
 */
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

import android.content.Context;
import android.content.res.Configuration;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.FrameLayout;


import android.widget.LinearLayout;

/**
 * Base class for a bottom bar used by the browser.
 * 
 * @author qijin
 */
public class BottomBarBase extends LinearLayout implements OnClickListener {
    /**
     * @param context
     */

    private UiController mUiController;
    private BaseUi mBaseUi;
    private FrameLayout mContentView;

    // state
    private boolean mShowing;
    // private boolean mSkipTitleBarAnimations;
    private boolean mIsFixedTitleBar;

    public BottomBarBase(Context context, UiController controller, BaseUi ui,
            FrameLayout contentView) {
        super(context, null);
        mUiController = controller;
        mBaseUi = ui;
        mContentView = contentView;
        setFixedBottomBar();
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

//        if (mIsFixedTitleBar) {
//            // int margin = getMeasuredHeight() - calculateEmbeddedHeight();
//            mBaseUi.setContentViewMarginBottom(-6);
//        } else {
//            mBaseUi.setContentViewMarginBottom(0);
//        }
    }

    private void setFixedBottomBar() {
        // If getParent() returns null, we are initializing
        ViewGroup parent = (ViewGroup) getParent();
        if (mIsFixedTitleBar && parent != null)
            return;
        mIsFixedTitleBar = true;
        // setSkipTitleBarAnimations(true);
        // show();
        // setSkipTitleBarAnimations(false);
        if (parent != null) {
            parent.removeView(this);
        }
        if (mIsFixedTitleBar) {
            mBaseUi.addFixedBottomBar(this);
        } else {
            mBaseUi.addFixedBottomBar(this);
            mContentView.addView(this, makeLayoutParams());
            //mBaseUi.setContentViewMarginBottom(0);
        }
    }

    public BaseUi getUi() {
        return mBaseUi;
    }

    public UiController getUiController() {
        return mUiController;
    }

    boolean isShowing() {
        return mShowing;
    }

    public WebView getCurrentWebView() {
        Tab t = mBaseUi.getActiveTab();
        if (t != null) {
            return t.getWebView();
        } else {
            return null;
        }
    }

    private ViewGroup.LayoutParams makeLayoutParams() {
        return new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
    }

    @Override
    public View focusSearch(View focused, int dir) {
        WebView web = getCurrentWebView();
        if (FOCUS_DOWN == dir && hasFocus() && web != null && web.hasFocusable()
                && web.getParent() != null) {
            return web;
        }
        return super.focusSearch(focused, dir);
    }

    public void onResume() {
//        setFixedBottomBar();
    }

    public void onPause() {
    }

    @Override
    public void onClick(View v) {
    }

}
