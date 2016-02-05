/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.myapidemo.smartisan.browse;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import java.util.Map;

import com.android.myapidemo.UI;

/**
 * Manage WebView scroll events
 */
public class BrowserWebView extends WebView {
    public interface OnScrollChangedListener {
        void onScrollChanged(int l, int t, int oldl, int oldt);
    }

    private boolean mBackgroundRemoved = false;
    private TitleBar mTitleBar;
    private OnScrollChangedListener mOnScrollChangedListener;
    private BottomBarPhone mBottomBarArea;
    private WebChromeClient mWebChromeClient;
    private WebViewClient mWebViewClient;
    private Tab mTab;

    /**
     * @param context
     * @param attrs
     * @param defStyle
     * @param javascriptInterfaces
     * for 4.4 webview not support private mode ,so set false directly.
     */
    public BrowserWebView(Context context, AttributeSet attrs, int defStyle,
            Map<String, Object> javascriptInterfaces, boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        //this.setJavascriptInterfaces(javascriptInterfaces);
        init(context);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     * @param privateBrowsing
     */
    public BrowserWebView(Context context, AttributeSet attrs, int defStyle,
            boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        init(context);
    }

  /*  *//**
     *  SWE-feature-backgroundtab
     * @param context
     * @param attrs
     * @param defStyle
     * @param privateBrowsing
     * @param backgroundTab
     *//*
    public BrowserWebView(
            Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing, boolean backgroundTab) {
        super(context, attrs, defStyle, privateBrowsing, backgroundTab);
        init(context);
    }
*/
    /**
     * @param context
     * @param attrs
     */
    public BrowserWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * @param context
     */
    public BrowserWebView(Context context) {
        super(context);
        init(context);
    }

    private Method IsWindowInthumbMode = null;

    public void init(Context context) {
        //setOverScrollMode(View.OVER_SCROLL_NEVER);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);

        try {
            IsWindowInthumbMode = WindowManager.class.getMethod("isWindowInthumbMode", new Class[] {});
        } catch (NoSuchMethodException e) {
        }
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        mWebChromeClient = client;
        super.setWebChromeClient(client);
    }

    public WebChromeClient getWebChromeClient() {
        return mWebChromeClient;
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        mWebViewClient = client;
        super.setWebViewClient(client);
    }

    public WebViewClient getWebViewClient() {
        return mWebViewClient;
    }

    public void setTitleBar(TitleBar title) {
        mTitleBar = title;
    }

    public void setBottomBar(BottomBarPhone bottomBarArea) {
        mBottomBarArea = bottomBarArea;
    }

    public void setTab(Tab tab) {
        mTab = tab;
    }

//    // From TitleBarDelegate
//    @Override
//    public int getTitleHeight() {
//        return (mTitleBar != null) ? mTitleBar.getEmbeddedHeight() : 0;
//    }

//    // From TitleBarDelegate
//    @Override
//    public void onSetEmbeddedTitleBar(final View title) {
//        // TODO: Remove this method; it is never invoked.
//    }

    public boolean hasTitleBar() {
        return (mTitleBar != null);
    }

    public void drawContent(Canvas c) {
        //super.drawContent(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mTab != null && mTab.isShowHomePage()) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mBottomBarArea != null){
            BaseUi baseUI = mBottomBarArea.getUi();
            baseUI.showTitleBottomBar(UI.VIEW_ALL_MASK);
        }
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        // NOTE: this function seems to not be called when the WebView is scrolled (it may be fine)
        super.onScrollChanged(l, t, oldl, oldt);
        if (mTitleBar != null) {
            mTitleBar.onScrollChanged();
        }
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(l, t, oldl, oldt);
        }
    }

    public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        mOnScrollChangedListener = listener;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        return false;
    }

    @Override
    public void destroy() {
        //BrowserSettings.getInstance().stopManagingSettings(getSettings());
        super.destroy();
    }

}
