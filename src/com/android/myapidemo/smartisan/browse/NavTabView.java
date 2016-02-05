/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.util.UrlUtils;
import com.android.myapidemo.smartisan.view.AudioFocusView;

public class NavTabView extends LinearLayout  {

    private TextView mTabTitle;
    private TextView mTabPrivateTitle;
    private View mTitleContainer;
    private View mTitlePrivateContainer;
    private View mAddressContainer;
    private View mAddressPrivateContainer;
    private Tab mTab;
    private NavThumbnailView mImage;
    private AudioFocusView mAudioFocusView;
    private Context mContext;
    public NavTabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public NavTabView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NavTabView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setOrientation(LinearLayout.VERTICAL);
        setFocusable(false);
        LayoutInflater.from(mContext).inflate(R.layout.nav_tab_view, this, true);
        mTitleContainer = findViewById(R.id.titlebar_title);
        mTitlePrivateContainer = findViewById(R.id.titlebar_title_private);
        mAddressContainer = findViewById(R.id.titlebar_input);
        mAddressPrivateContainer = findViewById(R.id.titlebar_input_private);
        mImage = (NavThumbnailView) findViewById(R.id.tab_view);
        mAudioFocusView = (AudioFocusView) findViewById(R.id.audio_focus);
    }

    private void setTitle(final CharSequence title) {
        if (mContext == null || title == null || mTab == null){
            return;
        }
        showTabTitle();
        if (mTab.isPrivateBrowsingEnabled()) {
            mTabPrivateTitle = (TextView) findViewById(R.id.title_private);
            mTabPrivateTitle.setText(mTab.getTitle());
            if (isCurrentTab()) {
                setTextBold(mTabPrivateTitle, true);
                mTabPrivateTitle.getCurrentTextColor();
                mTabPrivateTitle.setTextColor(Color.WHITE);
            } else {
                setTextBold(mTabPrivateTitle, false);
                mTabPrivateTitle.setTextColor(mContext.getResources().getColor(
                        R.color.input_hint_color));
            }
        } else {
            mTabTitle = (TextView) findViewById(R.id.title);
            mTabTitle.setText(mTab.getTitle());
            if (isCurrentTab()) {
                setTextBold(mTabTitle, true);
                mTabTitle.setTextColor(mContext.getResources().getColor(
                        R.color.nav_tab_view_current_title));
            } else {
                setTextBold(mTabTitle, false);
                mTabTitle.setTextColor(mContext.getResources().getColor(R.color.nav_tabview_title));
            }
        }
    }

    private void setTextBold(TextView tv, boolean bold) {
        if (bold) {
            tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        } else {
            tv.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }
    }

    public void hideTabTitle(){
        mTitleContainer.setVisibility(GONE);
        mTitlePrivateContainer.setVisibility(GONE);
        if (mTab != null && mTab.isPrivateBrowsingEnabled()) {
            mAddressPrivateContainer.setVisibility(VISIBLE);
            mAddressContainer.setVisibility(GONE);
        }else{
            mAddressPrivateContainer.setVisibility(GONE);
            mAddressContainer.setVisibility(VISIBLE);
        }
        hideAudioFocusView();
    }

    public void showTabTitle(){
        mAddressPrivateContainer.setVisibility(GONE);
        mAddressContainer.setVisibility(GONE);
        if (mTab != null && mTab.isPrivateBrowsingEnabled()) {
            mTitlePrivateContainer.setVisibility(VISIBLE);
            mTitleContainer.setVisibility(GONE);
        }else{
            mTitleContainer.setVisibility(VISIBLE);
            mTitlePrivateContainer.setVisibility(GONE);
        }
    }

    protected void setWebView(Tab tab) {
        final CharSequence title = UrlUtils.alterUrl(tab.getUrl());
        mTab = tab;
        setTitle(title);
        Bitmap image;
        //get the portrait,land screenshot
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            image = tab.getScreenshot();
        }else {
            image = tab.getHScreenshot();
        }
        if (image != null) {
            mImage.setBitmap(image);
            mImage.setIncogState(mTab.isPrivateBrowsingEnabled());
            if (tab != null) {
                mImage.setContentDescription(tab.getTitle());
            }
        }
        mAudioFocusView.setAudioFocus(tab.isMediaPlaying());
    }

    public void changeBitmap() {
        int orientation = getResources().getConfiguration().orientation;
        if (mTab == null || orientation == Configuration.ORIENTATION_PORTRAIT) {
            return;
        }
        Bitmap image = null;
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            image = mTab.getBScreenshot();
        }
        if (image != null) {
            mImage.setBitmap(image);
        }
        if (mTc.getIncogMode()) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mAddressPrivateContainer
                    .getLayoutParams();
            params.height = 61;
            mAddressPrivateContainer.setLayoutParams(params);
        } else {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mAddressContainer
                    .getLayoutParams();
            params.height = 61;
            mAddressContainer.setLayoutParams(params);
        }
    }

    public void hideAudioFocusView() {
        mAudioFocusView.setAudioFocus(false);
    }

    private TabControl mTc;

    public void setTabControl(TabControl tc) {
        mTc = tc;
    }

    private boolean isCurrentTab(){
        return mTab.equals(mTc.getCurrentTab()) ? true:false;
    }

    public Tab getTab(){
        return mTab;
    }
    /**some time not refesh,we force to do that.*/
    public void refeshLayout() {
        mImage.requestLayout();
    }
}
