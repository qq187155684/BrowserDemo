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

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.UrlInputView.UrlInputListener;
import com.android.myapidemo.smartisan.browser.util.UrlUtils;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;


public abstract class NavigationBarBase extends LinearLayout implements
        OnClickListener, UrlInputListener, OnFocusChangeListener {

    protected BaseUi mBaseUi;
    protected TitleBar mTitleBar;
    protected UiController mUiController;
    protected UrlInputView mUrlInput;
    protected UrlInputView mSearchInput;
    private ImageView mLockIcon;

    public NavigationBarBase(Context context) {
        super(context);
    }

    public NavigationBarBase(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationBarBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLockIcon = (ImageView) findViewById(R.id.lock);
        mSearchInput = (UrlInputView) findViewById(R.id.search_input);
        mSearchInput.setOnClickListener(this);
        mSearchInput.setUrlInputListener(this);
        mSearchInput.setOnFocusChangeListener(this);
        mUrlInput = (UrlInputView) findViewById(R.id.url);
        mUrlInput.setOnClickListener(this);
        mUrlInput.setUrlInputListener(this);
        mUrlInput.setOnFocusChangeListener(this);
    }

    public void hideIME() {
        mUrlInput.hideIME();
    }

    public UrlInputView getUrlInputView() {
        return mUrlInput;
    }

    public UrlInputView getSearchInputView() {
        return mSearchInput;
    }


    public void setTitleBar(TitleBar titleBar) {
        mTitleBar = titleBar;
        mBaseUi = mTitleBar.getUi();
        mUiController = mTitleBar.getUiController();
        mUrlInput.setController(mUiController);
        mUrlInput.setBaseUi(mBaseUi);
        mSearchInput.setController(mUiController);
        mSearchInput.setBaseUi(mBaseUi);
    }

    public void setLock(Drawable d) {
        if (mLockIcon == null)
            return;
        if (d == null) {
            mLockIcon.setVisibility(View.GONE);
        } else {
            mLockIcon.setImageDrawable(d);
            mLockIcon.setVisibility(View.VISIBLE);
        }
    }

    public void setFavicon(Bitmap icon) {
        // no-op currently
    }

    public boolean isEditingUrl() {
        return mUrlInput.hasFocus();
    }

    void stopEditingUrl() {
        WebView currentTopWebView = mUiController.getCurrentTopWebView();
        if (currentTopWebView != null) {
            currentTopWebView.requestFocus();
        }
    }

    abstract void setDisplayTitle(String title);

    void clearCompletions() {
        mUrlInput.dismissDropDown();
    }

    /**
     * callback from suggestion dropdown user selected a suggestion
     */
    @Override
    public void onAction(String text, String extra, String source) {
        stopEditingUrl();
        text = text.replaceAll("(\\r|\\n)", "");
        if (UrlInputView.TYPED.equals(source)) {
            String url = UrlUtils.smartUrlFilter(text, false);
            Tab t = mBaseUi.getActiveTab();
            // Only shortcut javascript URIs for now, as there is special
            // logic in UrlHandler for other schemas
            if (url != null && t != null && url.startsWith("javascript:")) {
                mUiController.loadUrl(t, url);
                setDisplayTitle(text);
                return;
            }
            if (text != null && t != null && text.startsWith("wtai://")) {
                mUiController.loadUrl(t, text);
                setDisplayTitle(text);
                return;
            }

            if (text.toLowerCase().startsWith("rtsp://")) {// it will change rtsp:// to http:// in url
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(text));
                try {
                    intent.putExtra(BrowserActivity.EXTRA_DISABLE_URL_OVERRIDE, true);
                    if (mUiController.getActivity().startActivityIfNeeded(intent, -1)) {
                        setDisplayTitle(text);
                        return;
                    }
                } catch (ActivityNotFoundException ex) {
                }
            }
        }

        Intent i = new Intent();
        String action = Intent.ACTION_SEARCH;
        i.setAction(action);
        i.putExtra(SearchManager.QUERY, text);
        if (extra != null) {
            i.putExtra(SearchManager.EXTRA_DATA_KEY, extra);
        }
        if (source != null) {
            Bundle appData = new Bundle();
            appData.putString("source", source);
            i.putExtra(SearchManager.APP_DATA, appData);
        }
        Tab tab = mBaseUi.getActiveTab();
        if (tab != null) {
            tab.setShowHomePage(false);
        }
        mUiController.handleNewIntent(i);
        setDisplayTitle(text);
    }

    @Override
    public void onDismiss() {
        final Tab currentTab = mBaseUi.getActiveTab();
        post(new Runnable() {
            public void run() {
                clearFocus();
                if (currentTab != null) {
                    setDisplayTitle(currentTab.getUrl());
                }
            }
        });
    }

    /**
     * callback from the suggestion dropdown copy text to input field and stay
     * in edit mode
     */
    @Override
    public void onCopySuggestion(String text) {
        mUrlInput.setText(text.toLowerCase(), true);
        if (text != null) {
            mUrlInput.setSelection(text.length());
        }
    }

    public void setCurrentUrlIsBookmark(boolean isBookmark) {
        // no-op currently
    }
    public abstract void formatUrl();
    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.KEYCODE_BACK && !((PhoneUi)mBaseUi).showingNavScreen()) {
            // when in portraid mode, when key board showing, bottom bar will hide.
            // so we can hide key board at first. then exit edit mode when user press back key again.
            // not work for landscope mode
            if (!mBaseUi.isInputMethodShowing()) {
                stopEditingUrl();
                return true;
            }
        }
        return super.dispatchKeyEventPreIme(evt);
    }

    public abstract void updateBackground(boolean isPrivate);

    public abstract void onPause();
    /**
     * called from the Ui when the user wants to edit
     * 
     * @param clearInput clear the input field
     */
    void startEditingUrl(boolean clearInput, boolean forceIME) {
        // editing takes preference of progress
        setVisibility(View.VISIBLE);
        if (!mUrlInput.hasFocus()) {
            mUrlInput.requestFocus();
        }
        if (clearInput) {
            mUrlInput.setText("");
        }
        if (forceIME) {
            mUrlInput.showIME();
        }
    }

    // for sometimes BrowserActivity do onConfigurationChanged but not notice NavigationBarPhone
    // to do onConfigurationChanged then cause some params not change.
    protected abstract void onConfigurationChanged(Configuration config);

    public abstract void onProgressStarted();

    public abstract void onProgressStopped();

    public abstract void setBtnReadModeVisiblity(boolean isVisible);

    public abstract ImageView getPreBtn();

    public abstract ImageView getNextBtn();

    public void onVoiceResult(String s) {
        startEditingUrl(true, true);
        onCopySuggestion(s);
    }

}
