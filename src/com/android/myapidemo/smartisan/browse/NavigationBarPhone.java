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

import android.animation.*;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.webkit.WebView;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.adapter.AddTabPopupAdapter;
import com.android.myapidemo.smartisan.adapter.SearchPopupWindowAdapter;
import com.android.myapidemo.smartisan.adapter.SearchPopupWindowAdapter.ListItem;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;
import com.android.myapidemo.smartisan.browser.util.Constants;
import com.android.myapidemo.smartisan.browser.util.UrlUtils;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;
import com.android.myapidemo.smartisan.search.SearchEngine;
import com.android.myapidemo.smartisan.widget.BasePopupWindow;
import com.android.myapidemo.smartisan.widget.BrowserMenu;
import com.android.myapidemo.smartisan.widget.BrowserPopupWindow;
import com.android.myapidemo.smartisan.widget.MenuPopupWindow;

import java.util.ArrayList;

public class NavigationBarPhone extends NavigationBarBase implements
        OnMenuItemClickListener {
    private static final String TAG = "NavigationBarPhone";
    private ImageView mStopButton;
    private ImageView mMagnify;
    private ImageView mWebIcon;
    private ImageView mBtnReadMode;
    private ImageView mProgressView;
    private ImageView mSearchClear;

    private LinearLayout mToolsContiner1;
    private LinearLayout mToolsContiner2;
    private ImageView mPreBtn;
    private ImageView mNextBtn;
    private ImageView mSwitchBtn;
    private ImageView mAddTabBtn;
    private ImageView mMenuBtn;
    private ImageView mAddressMask;

    private TextView mCancelButton;
    private Drawable mStopDrawable;
    private Drawable mClearDrawable;
    private Drawable mRefreshDrawable;
    private Drawable mCmccDrawable;
    private Drawable mGoogleDrawable;
    private Drawable mBaiduDrawable;
    private Drawable mBingDrawable;
    private Drawable mYahooUsDrawable, mYahooJpDrawable;
    private Drawable mWebiconDrawable;
    private Drawable mReadModeDrawable;
    private Drawable mAddressMarkDrawable;
    private String mStopDescription;
    private String mRefreshDescription;
    private View mTitleContainer;
    private View mSearchContainer;
    private FrameLayout mTitleBarContainer;
    private int mProgressViewWidth;
    private int mBtnReadModeWidth = 66;
    private boolean mIncognito = false;

    private int mAuto = 0;
    private BasePopupWindow mListPopup;
    private MenuPopupWindow mMenuPopup;
    private SearchPopupWindowAdapter mAdapter;
    private Context mContext;
    private AnimatorSet mShowBtnReadModeAS = new AnimatorSet();
    private boolean mIsBtnReadModeShowing = false;

    public static final int ITEM_SEARCH_ENGINE_139 = -1;
    public static final int ITEM_SEARCH_ENGINE_GOOGLE = 0;
    public static final int ITEM_SEARCH_ENGINE_BING = 1;
    public static final int ITEM_SEARCH_ENGINE_BAIDU = 2;

    public static int iTEM_SEARCH_ENGINE_139 = ITEM_SEARCH_ENGINE_139;
    public static int iTEM_SEARCH_ENGINE_GOOGLE = ITEM_SEARCH_ENGINE_GOOGLE;
    public static int iTEM_SEARCH_ENGINE_BING = ITEM_SEARCH_ENGINE_BING;
    public static int iTEM_SEARCH_ENGINE_BAIDU = ITEM_SEARCH_ENGINE_BAIDU;

    private static final String SEARCH_LABEL_139 = "CMCC";
    private static final String SEARCH_LABEL_GOOGLE = "Google";
    private static final String SEARCH_LABEL_BING = "Bing";
    private static final String SEARCH_LABEL_YAHOO = "Yahoo!";
    private static final String SEARCH_LABEL_YAHOO_JP = "Yahoo! JAPAN";

    private static final String SEARCH_DATA_139 = "cmcc";
    private static final String SEARCH_DATA_GOOGLE = "google";
    private static final String SEARCH_DATA_BING = "bing_zh_CN";
    private static final String SEARCH_DATA_BAIDU = "baidu";
    private static final String SEARCH_DATA_YAHOO = "yahoo";
    private static final String SEARCH_DATA_YAHOO_JP = "yahoo_jp";
    private static final int NAVIGATION_ANIM_DURATION = 200;
    private static final long PROGRESS_ANIMATION_POST_DELAYED = 50L;
    private static final long ADJUST_UI_POST_DELAYED = 100L;

    private int mCurrentState = STATE_NORMAL;
    private static final int STATE_NORMAL = 0;
    private static final int STATE_CHANGING_TO_EDIT = 1;
    private static final int STATE_EDIT = 2;
    private static final int STATE_CHANGING_TO_NORMAL = 3;

    private ArrayList<ListItem> mItems = new ArrayList<ListItem>();
    /**
     * mPostingProgressAnimation means the progress view is going to start , but it is
     * in postDelay , the value of true should last only very short time.
     */
    private boolean mPostingProgressAnimation = false;
    /**
     * mProgressAnimating means the progress view is in animating of EndProgressAnim (from right to left)
     * or startProgressAnim (from left to right) , if this value is true , the next animating should wait
     * until the current animation finished.
     */
    private boolean mProgressAnimating = false;

    private boolean mSearchState = false;

    private boolean mIsAnimShowing = false;
    private int mAnimState = Constants.NAVIGATION_ANIM_NORMAL;

    private Handler mHandler = new Handler();

    public NavigationBarPhone(Context context) {
        super(context);
        mContext = context;
    }

    public NavigationBarPhone(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public NavigationBarPhone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public void setStopButtonVisibility(int visibility) {
         mStopButton.setVisibility(visibility);
    }

    public void setSearchClearButtonVisibility(int visibility) {
         mSearchClear.setVisibility(visibility);
    }

    public int getBtnReadModeVisibility() {
        return mBtnReadMode.getVisibility();
    }

    public boolean isPending() {
        return mCurrentState == STATE_CHANGING_TO_EDIT || mCurrentState == STATE_CHANGING_TO_NORMAL || mIsAnimShowing;
    }

    public boolean isEditing() {
        return mCurrentState == STATE_EDIT;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSearchClear = (ImageView) findViewById(R.id.search_clear);
        mSearchClear.setOnClickListener(this);
        mStopButton = (ImageView) findViewById(R.id.stop);
        mStopButton.setOnClickListener(this);
        mCancelButton = (TextView) findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);
        mCancelButton.setTranslationX(50); //has some promble
        int px = CommonUtil.dip2px(getContext(), 12.5);
        ReflectHelper.invokeMethod(mCancelButton, "setMaxTextSize", new Class[]{float.class}, new Object[]{px});
        mMagnify = (ImageView) findViewById(R.id.magnify);
        mMagnify.setOnClickListener(this);
        mMagnify.setVisibility(View.INVISIBLE);
        mWebIcon = (ImageView) findViewById(R.id.web_icon);

        mTitleBarContainer = (FrameLayout)findViewById(R.id.title_layout);
        mToolsContiner1 = (LinearLayout) findViewById(R.id.tool1);
        mToolsContiner2 = (LinearLayout) findViewById(R.id.tool2);

        mPreBtn = (ImageView) findViewById(R.id.previous_btn);
        mPreBtn.setOnClickListener(this);
        mNextBtn = (ImageView) findViewById(R.id.next_btn);
        mNextBtn.setOnClickListener(this);
        mAddTabBtn = (ImageView) findViewById(R.id.newtab_btn);
        mAddTabBtn.setOnClickListener(this);
        mSwitchBtn = (ImageView) findViewById(R.id.switch_btn);
        mSwitchBtn.setOnClickListener(this);
        mMenuBtn = (ImageView) findViewById(R.id.menu_btn);
        mMenuBtn.setOnClickListener(this);
        mAddressMask = (ImageView) findViewById(R.id.mask);

        mTitleContainer = findViewById(R.id.title_bg);
        mSearchContainer = findViewById(R.id.search_bg);
        Resources res = getContext().getResources();
        mStopDrawable = res.getDrawable(R.drawable.stop_loadurl_selector);
        mClearDrawable = res.getDrawable(R.drawable.clear_btn_selector);
        mRefreshDrawable = res.getDrawable(R.drawable.refresh_url_selector);
        mCmccDrawable = res.getDrawable(R.drawable.cmcc_search_selector);
        mGoogleDrawable = res.getDrawable(R.drawable.google_search_selector);
        mBaiduDrawable = res.getDrawable(R.drawable.baidu_search_selector);
        mBingDrawable = res.getDrawable(R.drawable.bing_search_selector);
        mYahooUsDrawable = res.getDrawable(R.drawable.yahoo_search_us_selector);
        mYahooJpDrawable = res.getDrawable(R.drawable.yahoo_search_jp_selector);
        mStopDescription = res.getString(R.string.accessibility_button_stop);
        mRefreshDescription = res.getString(R.string.accessibility_button_refresh);
        mBtnReadMode =  (ImageView) findViewById(R.id.btn_readmode);
        mBtnReadMode.setOnClickListener(this);
        mProgressView = (ImageView)findViewById(R.id.progress_icon);
        mProgressViewWidth = (int)res.getDimension(R.dimen.nav_progress_view_width);
        if(mUrlInput.getTranslationX() == 0) {
            mProgressView.setTranslationX(-mProgressViewWidth);
        }
        //pivot of the progress view
        mProgressView.setPivotX(71);
        mProgressView.setPivotY(48);
        mUrlInput.setContainer(this);
        mUrlInput.setType(UrlInputView.ADDRESS_INPUT);
        mSearchInput.setContainer(this);
        mSearchInput.setType(UrlInputView.SEARCH_INPUT);
        mListPopup = new BrowserPopupWindow(getContext(), this);
        changeToolBtnVisible();
    }

    private void changeToolBtnVisible() {
        final int addressBoxMargin = (int) getContext().getResources()
                .getDimensionPixelSize(R.dimen.addres_bar_margin_left);
        final int searchBoxMargin = (int) getContext().getResources()
                .getDimensionPixelSize(R.dimen.search_bar_margin_left);
        FrameLayout.LayoutParams addressParams = (FrameLayout.LayoutParams) mTitleContainer
                .getLayoutParams();
        FrameLayout.LayoutParams searchParams = (FrameLayout.LayoutParams) mSearchContainer
                .getLayoutParams();
        updateBackground(mIncognito);
        if (!isLand()) {
            setToolBtnVisible(View.GONE);
            mAddressMask.setVisibility(View.GONE);
            if (mCurrentState == STATE_EDIT) {
                if (mSearchState) {
                    searchParams.leftMargin = 0;
                } else {
                    mTitleContainer.setTranslationX(addressBoxMargin);
                    searchParams.leftMargin = searchBoxMargin;
                }
            } else {
                searchParams.leftMargin = searchBoxMargin;
            }
            addressParams.leftMargin = 0;
            mTitleContainer.setLayoutParams(addressParams);
            mSearchContainer.setLayoutParams(searchParams);
        } else {
            setToolBtnVisible(View.VISIBLE);
            if (mCurrentState == STATE_EDIT) {
                if (mSearchState) {
                    mSearchContainer.setTranslationX(0);
                } else {
                    mSearchContainer.setX(searchBoxMargin);
                    // set address box left margin,the margin value is 243px.
                    addressParams.leftMargin = addressBoxMargin;
                    // set search box left margin,the margin value is 1055px.
                    searchParams.leftMargin = searchBoxMargin;
                }
                mTitleContainer.setTranslationX(-addressBoxMargin);
                mToolsContiner1.setTranslationX(-addressBoxMargin);
                mToolsContiner2.setTranslationX(searchBoxMargin);
            } else {
                mAddressMask.setVisibility(View.VISIBLE);
                mToolsContiner1.setTranslationX(0);
                mToolsContiner2.setTranslationX(0);
                searchParams.leftMargin = searchBoxMargin;// now
                                                          // 81dp+280dp+3.6dp-13dp
                addressParams.leftMargin = addressBoxMargin;
            }
            mSearchContainer.setLayoutParams(searchParams);
            mTitleContainer.setLayoutParams(addressParams);
        }
    }

    private void setToolBtnVisible(int visible) {
        mPreBtn.setVisibility(visible);
        mNextBtn.setVisibility(visible);
        mSwitchBtn.setVisibility(visible);
        mAddTabBtn.setVisibility(visible);
        mMenuBtn.setVisibility(visible);
    }

    private boolean isLand() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        if (mListPopup != null) {
            mListPopup.dismiss();
        }
        if (mMenuPopup != null) {
            mMenuPopup.dismiss();
        }
        if (mIsAnimShowing) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mIsAnimShowing) {
                        mHandler.postDelayed(this, ADJUST_UI_POST_DELAYED);
                    } else {
                        adjustUI();
                        changeToolBtnVisible();
                    }
                }
            }, ADJUST_UI_POST_DELAYED);
        } else {
            adjustUI();
            changeToolBtnVisible();
        }
    }

    private void adjustUI() {
        adjustInputWidth();
        mStopButton.setTranslationX(0);
        //adjust width
        LinearLayout.LayoutParams paramsTitlebar = (LinearLayout.LayoutParams) mTitleBarContainer
                .getLayoutParams();
        paramsTitlebar.width = getContext().getResources()
                .getDimensionPixelSize(R.dimen.title_bar_width);
        paramsTitlebar.height = getContext().getResources()
                .getDimensionPixelSize(R.dimen.toolbar_height);
        mTitleBarContainer.setLayoutParams(paramsTitlebar);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this
                .getLayoutParams();
        params.height = getContext().getResources()
                .getDimensionPixelSize(R.dimen.toolbar_height);
        setLayoutParams(params);

        if (mCurrentState == STATE_EDIT) {
            if (!mSearchState) {
                final int addressEditWidth = (int) getContext().getResources()
                        .getDimensionPixelSize(R.dimen.addres_bar_edit_width);
                FrameLayout.LayoutParams paramsTitle = (FrameLayout.LayoutParams) mTitleContainer
                        .getLayoutParams();
                paramsTitle.width = addressEditWidth;
                final int searchWidth = (int) getContext().getResources()
                        .getDimensionPixelSize(R.dimen.search_bar_normal_width);
                FrameLayout.LayoutParams paramsSearch = (FrameLayout.LayoutParams) mSearchContainer
                        .getLayoutParams();
                paramsSearch.width = searchWidth;
                mSearchContainer.setLayoutParams(paramsSearch);
            } else {
                final int addressWidth = (int) getContext().getResources()
                        .getDimensionPixelSize(R.dimen.addres_bar_normal_width);
                FrameLayout.LayoutParams paramsTitle = (FrameLayout.LayoutParams) mTitleContainer
                        .getLayoutParams();
                paramsTitle.width = addressWidth;
                mTitleContainer.setLayoutParams(paramsTitle);
                mTitleContainer.setX(-addressWidth);
                final int searchEditWidth = (int) getContext().getResources()
                        .getDimensionPixelSize(R.dimen.search_bar_edit_width);
                FrameLayout.LayoutParams paramsSearch = (FrameLayout.LayoutParams) mSearchContainer
                        .getLayoutParams();
                paramsSearch.width = searchEditWidth;
                mSearchContainer.setLayoutParams(paramsSearch);
            }
            mCancelButton.setTranslationX(-mCancelButton.getWidth()+mAuto);
        } else {
            // all normal states
            final int addressWidth = (int) getContext().getResources()
                    .getDimensionPixelSize(R.dimen.addres_bar_normal_width);
            final int searchWidth = (int) getContext().getResources()
                    .getDimensionPixelSize(R.dimen.search_bar_normal_width);
            FrameLayout.LayoutParams paramsTitle = (FrameLayout.LayoutParams) mTitleContainer
                    .getLayoutParams();
            paramsTitle.width = addressWidth;
            mTitleContainer.setLayoutParams(paramsTitle);
            FrameLayout.LayoutParams paramsSearch = (FrameLayout.LayoutParams) mSearchContainer
                    .getLayoutParams();
            paramsSearch.width = searchWidth;
            mSearchContainer.setLayoutParams(paramsSearch);
        }
    }

    private void adjustInputWidth(){
        if (mCurrentState == STATE_NORMAL) {
            FrameLayout.LayoutParams paramsUrl = (FrameLayout.LayoutParams) mUrlInput
                    .getLayoutParams();
            paramsUrl.width = (int) getResources().getDimension(R.dimen.urlinput_normal_width);
            mUrlInput.setLayoutParams(paramsUrl);
            FrameLayout.LayoutParams paramsSearch = (FrameLayout.LayoutParams) mSearchInput
                    .getLayoutParams();
            paramsSearch.width = (int) getResources().getDimension(R.dimen.search_input_normal_width);
            mSearchInput.setLayoutParams(paramsSearch);
        } else if (mCurrentState == STATE_EDIT) {
            if (!mSearchState) {
                FrameLayout.LayoutParams paramsUrl = (FrameLayout.LayoutParams) mUrlInput
                        .getLayoutParams();
                paramsUrl.width = (int) getResources().getDimension(R.dimen.urlinput_edit_width);
                mUrlInput.setLayoutParams(paramsUrl);
            } else {
                FrameLayout.LayoutParams paramsSearch = (FrameLayout.LayoutParams) mSearchInput
                        .getLayoutParams();
                paramsSearch.width = (int) getResources().getDimension(
                        R.dimen.search_input_edit_width);
                mSearchInput.setLayoutParams(paramsSearch);
            }
        }
    }

    public void onProgressStarted() {
        // tab init
        if (TextUtils.isEmpty(mBaseUi.getActiveTab().getUrl())) {
            return;
        }
        if (mCurrentState == STATE_CHANGING_TO_EDIT) {
            return;
        }
        if (mStopButton.getDrawable() != mStopDrawable) {
            mStopButton.setImageDrawable(mStopDrawable);
            mStopButton.setContentDescription(mStopDescription);
            if (mStopButton.getVisibility() != View.VISIBLE) {
                mStopButton.setVisibility(View.VISIBLE);
            }
        }
        if (mCurrentState != STATE_NORMAL)
            stopEditingUrl();
        else
            progressStartAnim();

        if (mIsBtnReadModeShowing) {
            if (mShowBtnReadModeAS != null && mShowBtnReadModeAS.isRunning())
                mShowBtnReadModeAS.cancel();
            mUrlInput.setTranslationX(0);
            hideBtnReadMode();
        }
    }

    private void hideBtnReadMode() {
        mBtnReadMode.setX(-150);
        mProgressAnimating = false;
        mIsBtnReadModeShowing = false;
    }

    public void onProgressStopped() { // this function also update the url in address bar when switch tab
        if (mCurrentState != STATE_EDIT || mSearchState) {
            mStopButton.setImageDrawable(mRefreshDrawable);
            mStopButton.setContentDescription(mRefreshDescription);
            formatUrl();
        }
        if (mIsProgressViewShowing) {
            progressEndAnim();
        } else if (mPostingProgressAnimation) { // if the progress animation is in the post , just delay some time then check again
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mIsProgressViewShowing) {
                        progressEndAnim();
                    }
                }
            }, PROGRESS_ANIMATION_POST_DELAYED);
        }
    }

    /*
     * when mainWebView show the html code, and readmode view injected js
     * success of load the code,then set button readmode visible, else gone
     */
    public void setBtnReadModeVisiblity(boolean isVisible) {
        if (isVisible == mIsBtnReadModeShowing) {
            return;
        }
        mIsBtnReadModeShowing = isVisible;
        if (mBaseUi.isReadModeWindowShowing()) {
            mBaseUi.dismissReadModeWindow();
        }
        mBtnReadMode.setEnabled(isVisible);
        if (mIsProgressViewShowing || mAnimState == Constants.NAVIGATION_ANIM_URLINPUT_HIDE) {
            return;
        }
        if (isVisible) {
            if (mCurrentState == STATE_CHANGING_TO_EDIT || mCurrentState == STATE_CHANGING_TO_NORMAL ||
              (mCurrentState == STATE_EDIT && !mSearchState)) {
                mBtnReadMode.setX(-150);
                mBtnReadMode.setVisibility(View.VISIBLE);
            } else if (mCurrentState == STATE_EDIT && mSearchState) {
                mBtnReadMode.setX(0);
                mBtnReadMode.setVisibility(View.VISIBLE);
                mUrlInput.setTranslationX(mBtnReadModeWidth);
            } else if (mCurrentState == STATE_NORMAL) {
                boolean isNotDoAnimation = false;
                if (mUrlInput.getTranslationX() == 66 && mBtnReadMode.getTranslationX() == 0) {
                    isNotDoAnimation = true;
                }

                if (!mIsProgressViewShowing && !isNotDoAnimation) {
                    showBtnReadModeAnimation();
                }
            }
        } else if (!isVisible) {
            if (mCurrentState == STATE_CHANGING_TO_EDIT || mCurrentState == STATE_CHANGING_TO_NORMAL ||
              (mCurrentState == STATE_EDIT && !mSearchState)) {
                mBtnReadMode.setX(-150);
                mBtnReadMode.setVisibility(View.GONE);
            } else if (mCurrentState == STATE_NORMAL || mSearchState) {
                if (!mIsProgressViewShowing && !mIsAnimShowing) {
                    hideReadModeAnimation();
                }
            } else if (mCurrentState == STATE_EDIT || mCurrentState == STATE_CHANGING_TO_EDIT) {
                mBtnReadMode.setX(-150);
                mBtnReadMode.setVisibility(View.GONE);
            }
        }
    }

    private void showBtnReadModeAnimation() {
        mBtnReadMode.setVisibility(View.VISIBLE);
        mBtnReadMode.setX(-150);
        ObjectAnimator btnReadAnim = ObjectAnimator.ofFloat(mBtnReadMode,
          "translationX", -150, 0);
        PropertyValuesHolder pvhTranUrlInput = PropertyValuesHolder.ofFloat(
          "translationX", 0, mBtnReadModeWidth);
        ObjectAnimator urlInputAnim = ObjectAnimator.ofPropertyValuesHolder(
          mUrlInput, pvhTranUrlInput);
        mShowBtnReadModeAS.playTogether(btnReadAnim, urlInputAnim);
        mShowBtnReadModeAS.setDuration(NAVIGATION_ANIM_DURATION);
        mShowBtnReadModeAS.start();
    }

    private void hideReadModeAnimation() {
        ObjectAnimator btnReadAnim = ObjectAnimator.ofFloat(mBtnReadMode,
          "translationX", mBtnReadMode.getTranslationX(), -150);
        ObjectAnimator urlInputAnim = ObjectAnimator.ofFloat(
          mUrlInput, "translationX", mUrlInput.getTranslationX(), 0);
        AnimatorSet aSet = new AnimatorSet();
        aSet.playTogether(btnReadAnim, urlInputAnim);
        aSet.setDuration((long) (NAVIGATION_ANIM_DURATION * ((150 + mBtnReadMode.getTranslationX()) / 150)));
        aSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                hideBtnReadMode();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                hideBtnReadMode();
            }
        });
        if (mShowBtnReadModeAS != null && mShowBtnReadModeAS.isRunning()) {
            mShowBtnReadModeAS.cancel();
        }
        aSet.start();
    }
    /**
     * Update the text displayed in the title bar.
     *
     * @param title String to display. If null, the new tab string will be
     *            shown.
     */
    @Override
    void setDisplayTitle(String title) {
        if (mBaseUi != null && mBaseUi.getActiveTab() != null
                && mBaseUi.getActiveTab().isPrivateBrowsingEnabled()) {
            if (TextUtils.isEmpty(title) || title.matches(UrlInputView.MATCHES)) {
                title = "";
                mStopButton.setVisibility(View.GONE);
            }
        }
        if (title == null) {
            mUrlInput.setText("");
        } else {
            String current = mUrlInput.getText().toString();
            String alteredUrl = UrlUtils.alterUrl(title);
            if (!current.equals(alteredUrl) && mCurrentState != STATE_EDIT) {
                mUrlInput.setTitle(alteredUrl);
            }
        }
    }

    @Override
    public void onClick(View view) {
        Tab tab = mUiController.getTabControl().getCurrentTab();
        switch (view.getId()) {
            case R.id.stop:
                if (mStopButton.getDrawable() == mStopDrawable) {
                    mStopButton.setImageDrawable(mRefreshDrawable);
                    if (mTitleBar.isInLoad()) {
                        mUiController.stopLoading();
                    }
                } else if (mStopButton.getDrawable() == mRefreshDrawable) {
                    if(mBaseUi.isReadModeWindowWillShowing()){
                        mBaseUi.cancelshowReadModeWindow();
                    }
                    if (mBaseUi.isReadModeWindowShowing()) {
                        mBaseUi.dismissReadModeWindow();
                    }
                    if (mIsBtnReadModeShowing && mBtnReadMode != null) {
                        mBtnReadMode.setEnabled(false);
                    }
                    WebView web = mBaseUi.getWebView();
                    if (web != null) {
                        stopEditingUrl();
                        web.reload();
                    }
                } else {
                    mUrlInput.setText("");
                }
                break;
            case R.id.magnify:
                if (mItems != null) {
                    mItems.clear();
                }
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mUrlInput.hideIME();
                }
                initPopupItem();
                initPopupWindow(mMagnify, AddTabPopupAdapter.TOP_ITEM);
                break;

            case R.id.cancel:
                String title = null;
                if (mBaseUi.getWebView() != null)
                    title = mBaseUi.getWebView().getUrl();
                if (title != null && !mUrlInput.getText().toString().equals(title))
                    mUrlInput.dismissDropDown();
                stopEditingUrl();
                break;
            case R.id.btn_readmode:
                if (mCurrentState == STATE_NORMAL) {
                    mUrlInput.hideIME();
                    mBaseUi.showReadWindow();
                }
                break;
            case R.id.previous_btn:
                if (tab != null) {
                    tab.goBack();
                }
                break;
            case R.id.next_btn:
                if (tab != null) {
                    tab.goForward();
                }
                break;
            case R.id.newtab_btn:
                if (!mBaseUi.isNewTabAnimating()) {
                    if (mUiController.getTabControl().canCreateNewTab()) {
                        startNewTabAnim();
                    }else{
                        boolean isShow = mBaseUi.isShowMaxTabsDialog(new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface d, int which) {
                                startNewTabAnim();
                                mBaseUi.updateCheckPrompt();
                            }
                        }, null);
                        if (!isShow) {
                            startNewTabAnim();
                        }
                    }
                }
                break;
            case R.id.switch_btn:
                PhoneUi phoneUi = ((PhoneUi) mBaseUi);
                phoneUi.toggleNavScreen();
                phoneUi.setPrivateAnim(false);
                //mSwitchTabBtn.setClickable(false);
                break;
            case R.id.menu_btn:
                if (((NavigationBarPhone) (mBaseUi.getBarBase())).isPending())
                    return; // not show menu in pending state
                BrowserMenu menu = new BrowserMenu(mContext, mUiController, mBaseUi);
                menu.initMenuPopupItem();
                int xOff = getResources().getInteger(R.integer.popup_window_x_off);
                int yOff = getResources().getInteger(R.integer.popup_window_y_off);
                menu.initMenuPopupWindow(mMenuBtn, xOff, yOff);
                mMenuPopup = menu.getPopupWindow();
                MenuPopupWindow menupop = (MenuPopupWindow)mMenuPopup;
                menupop.setLandState(true);
                break;
            default:
                break;
        }
    }

    private void startNewTabAnim() {
        if (mUiController.getTabControl().getIncogMode()) {
            mBaseUi.NewTabAnim(BottomBarPhone.NEW_TAB_ING_TAG, null);
        } else {
            mBaseUi.NewTabAnim(BottomBarPhone.NEW_TAB_TAG, null);
        }
    }

    public ImageView getPreBtn() {
        return mPreBtn;
    }

    public ImageView getNextBtn() {
        return mNextBtn;
    }

    /**
     * mIsProgressViewShowing means wheather the progress view is shown
     */
    private boolean mIsProgressViewShowing = false;
    private long mLastProgressStartAnimationTime = 0L;

    private void progressStartAnim() {
        // Log.d("NavigationBarPhone" , "progressStartAnim :" +
        // !mIsProgressViewShowing + " " + !mPostingProgressAnimation + " load:"
        // + mTitleBar.isInLoad());
        if (!mIsAnimShowing) {
            if (!mIsProgressViewShowing && !mPostingProgressAnimation && mTitleBar.isInLoad()) {
                mPostingProgressAnimation = true;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressAnimating) {
                            // Log.d("NavigationBarPhone" ,
                            // "progressStartAnim  mProgressAnimating post a delay");
                            // the end animation is drawing
                            mPostingProgressAnimation = false;
                            progressStartAnim();
                            return;
                        }
                        if (mIsProgressViewShowing || !mTitleBar.isInLoad()) {
                            mPostingProgressAnimation = false;
                            // Log.d("NavigationBarPhone" ,
                            // "progressStartAnim is not inload return");
                            return;
                        }
                        mPostingProgressAnimation = false;
                        mProgressAnimating = true;
                        mIsProgressViewShowing = true;
                        progresStart();
                        ObjectAnimator urlInputMoveToRight;
                        ObjectAnimator progressIn;
                        ObjectAnimator btnReadModeOut;
                        mProgressView.setVisibility(View.VISIBLE);
                        mProgressView.setTranslationX(-mProgressViewWidth);
                        btnReadModeOut = ObjectAnimator.ofFloat(mBtnReadMode,
                          "translationX", 0, -150);
                        urlInputMoveToRight = ObjectAnimator.ofFloat(mUrlInput,
                          "translationX", 0, 66);
                        progressIn = ObjectAnimator.ofFloat(mProgressView,
                          "translationX", -mProgressViewWidth, 0);
                        AnimatorSet asAnimatorSetStart = new AnimatorSet();
                        asAnimatorSetStart.setDuration(NAVIGATION_ANIM_DURATION);
                        if (mIsBtnReadModeShowing && mBtnReadMode.getVisibility() == View.VISIBLE
                          && mBtnReadMode.getTranslationX() == 0) {
                            asAnimatorSetStart.playSequentially(btnReadModeOut, progressIn);
                            asAnimatorSetStart.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mBtnReadMode.setTranslationX(-150);
                                    mBtnReadMode.setVisibility(View.GONE);
                                    mIsBtnReadModeShowing = false;
                                    mProgressAnimating = false;
                                }
                            });
                        } else {
                            mBtnReadMode.setTranslationX(-150);
                            mBtnReadMode.setVisibility(View.GONE);
                            asAnimatorSetStart.playTogether(urlInputMoveToRight, progressIn);
                            asAnimatorSetStart.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mProgressAnimating = false;
                                }
                            });
                        }
                        asAnimatorSetStart.start();
                        mLastProgressStartAnimationTime = System.currentTimeMillis();
                    }
                }, PROGRESS_ANIMATION_POST_DELAYED);
            }
        }
    }

    ObjectAnimator mProgressAnimator = null;

    private void progresStart() {
        mProgressAnimator = ObjectAnimator.ofFloat(mProgressView,
          "rotation", 0, 360);
        mProgressAnimator.setDuration(1500);
        mProgressAnimator.setRepeatCount(-1);
        LinearInterpolator lin = new LinearInterpolator();
        mProgressAnimator.setInterpolator(lin);
        mProgressAnimator.start();
    }

    private void progresEnd() {
        Browser browser = (Browser) mBaseUi.mActivity.getApplication();
        browser.setmIsFromThumbnial(false);
        if (mIsBtnReadModeShowing) {
            mProgressView.setVisibility(View.GONE);
            mIsProgressViewShowing = false;
        }
        if (mProgressAnimator != null) {
            mProgressAnimator.cancel();
            mProgressAnimator = null;
        }
    }

    AnimatorSet asAnimatorSetEnd = null;

    private void progressEndAnim() {
        mPostingProgressAnimation = true;
        mHandler.removeCallbacks(mRunnable);
        mHandler.post(mRunnable);
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mPostingProgressAnimation = false;
            if (!mTitleBar.isInLoad() && mCurrentState == STATE_NORMAL) {
                if (!mProgressAnimating) {
                    mProgressAnimating = true;
                    // test position
                    // progressview.getwidth - urlinput's left margin
                    ObjectAnimator urlInputMoveToLeft = ObjectAnimator.ofFloat(mUrlInput,
                      "translationX", 66, 0);
                    ObjectAnimator urlInputMoveToRight = ObjectAnimator.ofFloat(mUrlInput,
                      "translationX", mUrlInput.getTranslationX(), 66);
                    ObjectAnimator progressOut = ObjectAnimator.ofFloat(mProgressView,
                      "translationX", mProgressView.getTranslationX(), -mProgressViewWidth);
                    ObjectAnimator btnReadModeIn = ObjectAnimator.ofFloat(mBtnReadMode,
                      "translationX", -150, 0);
                    asAnimatorSetEnd = new AnimatorSet();
                    asAnimatorSetEnd.setDuration(NAVIGATION_ANIM_DURATION);
                    if (!mIsBtnReadModeShowing) {
                        if (mUrlInput.getTranslationX() == 66)
                            asAnimatorSetEnd.playTogether(urlInputMoveToLeft, progressOut);
                    } else if (mIsBtnReadModeShowing && mShowBtnReadModeAS != null && mShowBtnReadModeAS.isRunning()) {
                        asAnimatorSetEnd.playTogether(progressOut, urlInputMoveToRight);
                    } else {
                        mBtnReadMode.setTranslationX(-150);
                        mBtnReadMode.setVisibility(View.VISIBLE);
                        asAnimatorSetEnd.playSequentially(progressOut, btnReadModeIn, urlInputMoveToRight);
                    }
                    asAnimatorSetEnd.setDuration(300);
                    asAnimatorSetEnd.addListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            super.onAnimationCancel(animation);
                            progresEnd();
                            mProgressAnimating = false;
                            if (mIsBtnReadModeShowing && mBtnReadMode.getTranslationX() != 0) {
                                showBtnReadModeAnimation();
                            } else if (!mIsBtnReadModeShowing && mBtnReadMode.getTranslationX() == 0) {
                                hideReadModeAnimation();
                            }
                            if (mProgressView.getVisibility() != View.GONE)
                                mProgressView.setVisibility(View.GONE);
                            mIsProgressViewShowing = false;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            progresEnd();
                            mProgressAnimating = false;
                            if (mIsBtnReadModeShowing && mBtnReadMode.getTranslationX() != 0) {
                                showBtnReadModeAnimation();
                            } else if (!mIsBtnReadModeShowing && mBtnReadMode.getTranslationX() == 0) {
                                hideReadModeAnimation();
                            }

                            if (mProgressView.getVisibility() != View.GONE)
                                mProgressView.setVisibility(View.GONE);
                            mIsProgressViewShowing = false;
                        }
                    });
                    final long timeGap = System.currentTimeMillis()
                      - mLastProgressStartAnimationTime;
                    if (timeGap < 300L && timeGap > 0) {
                        // wait some time to let start animation finished
                        asAnimatorSetEnd.setStartDelay(300L);
                    }
                    asAnimatorSetEnd.start();
                } else if (mProgressAnimating) {
                    mPostingProgressAnimation = true;
                    mHandler.removeCallbacks(mRunnable);
                    mHandler.postDelayed(mRunnable, 100L);
                }
            }
        }
    };

    public void setButtonsPosition(boolean showReadButton) {
        mIsBtnReadModeShowing = showReadButton;
        mBtnReadMode.setVisibility(mIsBtnReadModeShowing ? View.VISIBLE : View.GONE);
        mBtnReadMode.setEnabled(mIsBtnReadModeShowing);
        if (mTitleBar.isInLoad()) {
            mProgressView.setX(0);
            mProgressView.setVisibility(View.VISIBLE);
            //mIsProgressViewShowing = true;
            mUrlInput.setX(mBtnReadModeWidth);
        } else {
            mProgressView.setX(-mProgressView.getWidth());
            mProgressView.setVisibility(View.GONE);
            mIsProgressViewShowing = false;
            if (showReadButton) {
                mBtnReadMode.setX(0);
                mUrlInput.setX(mBtnReadModeWidth);
            } else {
                mBtnReadMode.setX(-mBtnReadModeWidth);
                mUrlInput.setX(0);
            }
        }
    }

    Runnable mEditRunnable = new Runnable() {
        public void run() {
            onStateChanged(STATE_EDIT);
        }
    };
    Runnable mNormalRunnable = new Runnable() {
        public void run() {
            onStateChanged(STATE_NORMAL);
        }
    };

    static final int POST_DELAY = 300;

    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus)
            mUrlInput.hideIME();

        if (mIsAnimShowing) {// to avoid state get messed, not change state when animating.
            return;
        }

        if (hasFocus) {
            if (!mBaseUi.isNewTabAnimating() && mCurrentState == STATE_NORMAL && (view == mUrlInput || view == mSearchInput)) {
                mSearchState = (view == mSearchInput ? true : false);
                onStateChanged(STATE_CHANGING_TO_EDIT);// pending state
                mHandler.removeCallbacks(mEditRunnable);
                mHandler.removeCallbacks(mNormalRunnable);
                mHandler.postDelayed(mEditRunnable, POST_DELAY);
            }
        }
        else {
            if (mCurrentState == STATE_EDIT) {
                // let the ime goes down first , then changed the state (animation will be start soon)
                //otherwise , it will cause the animation shaking slightly
                onStateChanged(STATE_CHANGING_TO_NORMAL);// pending state
                mHandler.removeCallbacks(mEditRunnable);
                mHandler.removeCallbacks(mNormalRunnable);
                mHandler.postDelayed(mNormalRunnable, POST_DELAY);
            }
        }
    }

    private void onStateChanged(int state) {
        mCurrentState = state;
        switch (state) {
            case STATE_NORMAL:
                hideAnimation();
                break;
            case STATE_EDIT:
                showAnimation();
                break;
        }
    }

    private void changeEmptyUrlState() {
        if (TextUtils.isEmpty(mUrlInput.getText().toString())) {
            mStopButton.setVisibility(View.GONE);
        }else {
            mStopButton.setVisibility(View.VISIBLE);
        }
    }

    public void initPopupWindow(View view, int type) {
        mAdapter = new SearchPopupWindowAdapter(mContext, mItems, type);
        initSelecteItem(mAdapter);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginLeft = getResources().getInteger(
                R.integer.pop_window_margin_left);
        int xOff = getResources().getInteger(R.integer.title_bar_x_off);
        int yOff = getResources().getInteger(R.integer.title_bar_y_off);
        params.setMargins(marginLeft, 0, 0, 0);
        mListPopup.setAdapter(mAdapter);
        mListPopup.setOnItemClickListener(clickListener);
        mListPopup.showAsDropDown(view, xOff, yOff);
        mListPopup.setListViewParams(params);
    }

    public void initPopupItem() {
        Resources res = mContext.getResources();
        Drawable drawable;
        String text;
        if (mBaseUi.isCmccFeature()) {
            iTEM_SEARCH_ENGINE_139 = ITEM_SEARCH_ENGINE_139 + 1;
            iTEM_SEARCH_ENGINE_GOOGLE = ITEM_SEARCH_ENGINE_GOOGLE + 1;
            iTEM_SEARCH_ENGINE_BING = ITEM_SEARCH_ENGINE_BING + 1;
            iTEM_SEARCH_ENGINE_BAIDU = ITEM_SEARCH_ENGINE_BAIDU + 1;
            text = res.getString(R.string.search_139);
            drawable = res.getDrawable(R.drawable.cmcc);
            mItems.add(new ListItem(text, drawable));
        }
        text = res.getString(R.string.search_google);
        drawable = res.getDrawable(R.drawable.google);
        mItems.add(new ListItem(text, drawable));
        text = res.getString(R.string.search_bing);
        drawable = res.getDrawable(R.drawable.bing);
        mItems.add(new ListItem(text, drawable));
        if (mBaseUi.isFeatureUSEnabled()) {
            text = res.getString(R.string.search_yahoo);
            drawable = res.getDrawable(R.drawable.yahoo);
        } else if (mBaseUi.isFeatureJPEnabled()) {
            text = res.getString(R.string.search_yahoo);
            drawable = res.getDrawable(R.drawable.yahoo_jp);
        } else {
            text = res.getString(R.string.search_baidu);
            drawable = res.getDrawable(R.drawable.baidu);
        }
        mItems.add(new ListItem(text, drawable));
    }

    public void initSelecteItem(SearchPopupWindowAdapter adapter) {
        if (adapter == null) {
            return;
        }
        SearchEngine searchEngine = BrowserSettings.getInstance()
                .getSearchEngine();
        if (searchEngine.getLabel().equals(SEARCH_LABEL_139)) {
            adapter.setSelectedItem(iTEM_SEARCH_ENGINE_139);
        } else if (searchEngine.getLabel().equals(SEARCH_LABEL_GOOGLE)) {
            adapter.setSelectedItem(iTEM_SEARCH_ENGINE_GOOGLE);
        } else if (searchEngine.getLabel().equals(SEARCH_LABEL_BING)) {
            adapter.setSelectedItem(iTEM_SEARCH_ENGINE_BING);
        } else {
            adapter.setSelectedItem(iTEM_SEARCH_ENGINE_BAIDU);
        }
    }

    OnItemClickListener clickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            if (mAdapter != null) {
                mAdapter.setSelectedItem(position);
            }
            if (mListPopup != null) {
                if (position == iTEM_SEARCH_ENGINE_139) {
                    setSearchEngine(SEARCH_DATA_139);
                    changeSearchIcon();
                } else if (position == iTEM_SEARCH_ENGINE_GOOGLE) {
                    setSearchEngine(SEARCH_DATA_GOOGLE);
                    changeSearchIcon();
                } else if (position == iTEM_SEARCH_ENGINE_BING) {
                    setSearchEngine(SEARCH_DATA_BING);
                    changeSearchIcon();
                } else {
                    if (mBaseUi.isFeatureUSEnabled())
                        setSearchEngine(SEARCH_DATA_YAHOO);
                    else if (mBaseUi.isFeatureJPEnabled())
                        setSearchEngine(SEARCH_DATA_YAHOO_JP);
                    else
                        setSearchEngine(SEARCH_DATA_BAIDU);
                    changeSearchIcon();
                }
                mListPopup.dismiss();
            }
        }
    };

    private void showAnimation(){
        mIsAnimShowing = true;
        mMagnify.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mCancelButton.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mTitleContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mSearchContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        final int marginRight = (int) getResources().getDimension(
                R.dimen.search_bar_margin_right);
        final int addressEditWidth = (int) getContext().getResources()
                .getDimensionPixelSize(R.dimen.addres_bar_edit_width);
        final int urlEditWidth = (int) getResources().getDimension(R.dimen.urlinput_edit_width);
        final int searchEditWidth = (int) getContext().getResources()
                .getDimensionPixelSize(R.dimen.search_bar_edit_width);
        final int searchEditRight = (int) getContext().getResources()
                .getDimensionPixelSize(R.dimen.search_bar_edit_right);
        final int searchInputEditWidth = (int) getResources().getDimension(
                R.dimen.search_input_edit_width);
        mAuto = mCancelButton.getWidth() - 174;
        if( mProgressAnimating && asAnimatorSetEnd != null ){
            asAnimatorSetEnd.cancel();
        }
        //land screen do the aimation
        if (isLand()) {
            mAddressMask.setVisibility(View.GONE);
            setToolBtnVisible(View.VISIBLE);
            addAnimationToList(mToolsContiner1, "translationX", 0, -mToolsContiner1.getWidth());
            addAnimationToList(mToolsContiner2, "translationX", 0, mToolsContiner2.getWidth());
        } else {
            setToolBtnVisible(View.GONE);
        }
        if (!mSearchState) {
            mAnimState = Constants.NAVIGATION_ANIM_URLINPUT_SHOW;
            formatUrl();
            mStopButton.setImageDrawable(mClearDrawable);
            mStopButton.setVisibility(View.GONE);
            addAnimationToList(mTitleContainer, "right", mTitleContainer.getRight(),
                    addressEditWidth);
            addAnimationToList(mTitleContainer, "left", mTitleContainer.getLeft(),
                    0);
            addAnimationToList(mWebIcon, "translationX", 0, mWebIcon.getWidth());
            addAnimationToList(mCancelButton, "translationX", mAuto, -mCancelButton.getWidth()+mAuto);
            addAnimationToList(mSearchContainer, "alpha", 1.0f, 0.0f);
            if (mIsProgressViewShowing) {
                addAnimationToList(mProgressView, "x", 0, -150);
                addAnimationToList(mUrlInput, "translationX", mWebIcon.getWidth(), mWebIcon.getWidth());
            } else if (mIsBtnReadModeShowing) {
                if (mShowBtnReadModeAS != null && mShowBtnReadModeAS.isRunning()) {
                    mShowBtnReadModeAS.cancel();
                }
                addAnimationToList(mBtnReadMode, "x", mBtnReadMode.getTranslationX(), -150);
                addAnimationToList(mUrlInput, "translationX", mWebIcon.getWidth(), mWebIcon.getWidth());
            } else {
                addAnimationToList(mUrlInput, "translationX", 0, mWebIcon.getWidth());
            }
            addAnimationToList(mUrlInput, "right", mUrlInput.getRight(), addressEditWidth);
        } else {
            mAnimState = Constants.NAVIGATION_ANIM_SEARCHINPUT_SHOW;
            formatSearch();
            mTitleBar.setMaskVisiblity(false);
            mSearchInput.setAlpha(0.0f);
            addAnimationToList(mSearchContainer, "right", mSearchContainer.getRight(), searchEditWidth);
            addAnimationToList(mSearchContainer, "left", mSearchContainer.getLeft(), 0);
            addAnimationToList(mMagnify, "translationX", 0, -mMagnify.getWidth());
            addAnimationToList(mTitleContainer, "translationX", 0, -mTitleContainer.getWidth());
            addAnimationToList(mTitleContainer, "alpha", 1.0f, 0.0f);
            addAnimationToList(mCancelButton, "translationX", 0, -mCancelButton.getWidth() + mAuto);
        }
        AnimatorSet aSet = new AnimatorSet();
        aSet.setDuration(NAVIGATION_ANIM_DURATION);
        aSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mCancelButton.setLayerType(View.LAYER_TYPE_NONE, null);
                mSearchContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                mTitleContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                mMagnify.setLayerType(View.LAYER_TYPE_NONE, null);
                mAnimList.clear();
                mIsAnimShowing = false;
                mAnimState = Constants.NAVIGATION_ANIM_NORMAL;
                if (mProgressView.getVisibility() != View.GONE) {
                    mProgressView.setVisibility(View.GONE);
                }
                mIsProgressViewShowing = false;
                if (!mSearchState) {
                    FrameLayout.LayoutParams paramsTitle = (FrameLayout.LayoutParams) mTitleContainer
                      .getLayoutParams();
                    paramsTitle.width = addressEditWidth;
                    paramsTitle.rightMargin = marginRight;
                    mTitleContainer.setLayoutParams(paramsTitle);
                    FrameLayout.LayoutParams paramsUrl = (FrameLayout.LayoutParams) mUrlInput
                      .getLayoutParams();
                    paramsUrl.width = urlEditWidth;
                    mUrlInput.setLayoutParams(paramsUrl);
                    mCancelButton.setTranslationX(-mCancelButton.getWidth() + mAuto);
                    changeEmptyUrlState();
                    mStopButton.setTranslationX(0);
                    mUrlInput.requestFocus(); // make sure it has focus when animation end. important
                    mUrlInput.selectAll();
                    ReflectHelper.invokeProxyMethod("android.widget.TextView", "showSelectionController", mUrlInput, null, null);
                    if(isLand()){
                        mTitleContainer.setTranslationX(-mToolsContiner1.getWidth());
                    }
                } else {
                    mTitleContainer.setVisibility(View.INVISIBLE);
                    FrameLayout.LayoutParams paramsSearch = (FrameLayout.LayoutParams) mSearchContainer
                      .getLayoutParams();
                    paramsSearch.width = searchEditWidth;
                    paramsSearch.leftMargin = 0;
                    mSearchContainer.setLayoutParams(paramsSearch);
                    mMagnify.setVisibility(View.VISIBLE);
                    mMagnify.setX(1);
                    FrameLayout.LayoutParams paramsInput = (FrameLayout.LayoutParams) mSearchInput
                      .getLayoutParams();
                    paramsInput.width = searchInputEditWidth;
                    mSearchInput.setLayoutParams(paramsInput);
                    mSearchInput.setTranslationX(155);
                    mSearchInput.setAlpha(1.0f);
                    mSearchInput.requestFocus(); // make sure it has focus when animation end. important
                    mSearchInput.setHint(R.string.title_search_edit_hint);
                    if(isLand()){
                        mSearchContainer.setTranslationX(0);
                    }
                }
            }
        });
        aSet.playTogether(mAnimList);
        aSet.start();
    }

    private void hideAnimation(){
        mIsAnimShowing = true;
        mMagnify.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mCancelButton.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mSearchContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mTitleContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        final int marginRight = (int) getResources().getDimension(
                R.dimen.search_bar_margin_right);
        final int addressWidth = (int) getContext().getResources()
                .getDimensionPixelSize(R.dimen.addres_bar_normal_width);
        final int searchWidth = (int) getContext().getResources()
                .getDimensionPixelSize(R.dimen.search_bar_normal_width);
        final int marginLeft = (int) getResources().getDimension(
                R.dimen.title_input_marginleft);
        int searchRight;
        int searchLeft = (mToolsContiner1.getWidth()+addressWidth);
        mTitleContainer.setVisibility(View.VISIBLE);
        //land screen do the animation
        if(isLand()){
            setToolBtnVisible(View.VISIBLE);
            searchRight = getWidth()-mToolsContiner2.getWidth();
            searchLeft = searchLeft+11;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mTitleContainer
                    .getLayoutParams();
            params = (FrameLayout.LayoutParams) mTitleContainer
                    .getLayoutParams();
            params.leftMargin = mToolsContiner1.getWidth();
            mTitleContainer.setLayoutParams(params);
            mTitleContainer.setTranslationX(-mToolsContiner1.getWidth());
            mToolsContiner1.setTranslationX(-mToolsContiner1.getWidth());
            mToolsContiner2.setTranslationX(mToolsContiner2.getWidth());
            addAnimationToList(mToolsContiner1, "translationX", -mToolsContiner1.getWidth(),0);
            addAnimationToList(mToolsContiner2, "translationX", mToolsContiner2.getWidth(),0);
        }else{
            searchRight = getWidth()-18;
            setToolBtnVisible(View.GONE);
        }
        if (!mSearchState) {
            //hide address
            mAnimState = Constants.NAVIGATION_ANIM_URLINPUT_HIDE;
            mUrlInput.setNotNeedShowDropState(true);
            mUrlInput.clearFocus();
            formatUrl();
            addAnimationToList(mCancelButton, "translationX", -mCancelButton.getWidth(), mAuto);
            addAnimationToList(mWebIcon, "translationX", mWebIcon.getWidth(), 0);
            addAnimationToList(mTitleContainer, "right", mTitleContainer.getRight(),searchLeft);
            addAnimationToList(mTitleContainer, "translationX", -mToolsContiner1.getWidth(), 0);
            addAnimationToList(mSearchContainer, "alpha", 0.0f, 1.0f);
        } else {
            //hide search
            mAnimState = Constants.NAVIGATION_ANIM_SEARCHINPUT_HIDE;
            formatSearch();
            mMagnify.setVisibility(View.INVISIBLE);
            mSearchInput.clearFocus();
            mSearchInput.setText("");
            mSearchInput.setAlpha(0.0f);

            addAnimationToList(mSearchContainer, "left", mSearchContainer.getLeft(), searchLeft);
            addAnimationToList(mSearchContainer, "right", mSearchContainer.getRight(), searchRight);
            addAnimationToList(mMagnify, "translationX", -mMagnify.getWidth(), 0);
            addAnimationToList(mTitleContainer, "translationX", -(mTitleContainer.getWidth()+mToolsContiner1.getWidth()), 0);
            addAnimationToList(mTitleContainer, "alpha", 0.0f,1.0f);
            addAnimationToList(mCancelButton, "translationX", mCancelButton.getTranslationX(), mCancelButton.getWidth());
            addAnimationToList(mSearchInput, "translationX", mSearchInput.getTranslationX(), 0);
        }

        if (mIsBtnReadModeShowing) {
            mUrlInput.setTranslationX(mBtnReadModeWidth);
            mBtnReadMode.setVisibility(View.VISIBLE);
            addAnimationToList(mBtnReadMode,"translationX", -150, 0);
        } else {
            mBtnReadMode.setVisibility(View.GONE);
            addAnimationToList(mUrlInput, "translationX", 66, 0);
        }
        mUrlInput.setEnabled(false);
        mSearchInput.setEnabled(false);
        AnimatorSet aSet = new AnimatorSet();
        aSet.setDuration(NAVIGATION_ANIM_DURATION);
        aSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationCancel(Animator animation) {
                mUrlInput.setEnabled(true);
                mSearchInput.setEnabled(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!mIsBtnReadModeShowing && mBtnReadMode.getTranslationX() == 66) {
                    hideReadModeAnimation();
                } else if (mIsBtnReadModeShowing && (mBtnReadMode.getTranslationX() != 0 ||
                  mBtnReadMode.getVisibility() != View.VISIBLE ||
                  mUrlInput.getTranslationX() != mBtnReadModeWidth)) {
                    showBtnReadModeAnimation();
                }

                mIsAnimShowing = false;
                mAnimState = Constants.NAVIGATION_ANIM_NORMAL;
                mMagnify.setLayerType(View.LAYER_TYPE_NONE, null);
                mCancelButton.setLayerType(View.LAYER_TYPE_NONE, null);
                mTitleContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                mSearchContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                // title edit bar width
                FrameLayout.LayoutParams paramsTitle = (FrameLayout.LayoutParams) mTitleContainer
                  .getLayoutParams();
                paramsTitle.width = addressWidth;
                // search edit bar width
                FrameLayout.LayoutParams paramsSearch = (FrameLayout.LayoutParams) mSearchContainer
                  .getLayoutParams();
                paramsSearch.width = searchWidth;
                mAnimList.clear();
                mUrlInput.setEnabled(true);
                mSearchInput.setEnabled(true);
                if (isLand()) {
                    mAddressMask.setVisibility(View.VISIBLE);
                }else{
                    mTitleBar.setMaskVisiblity(true);
                }
                if (!mSearchState) {
                    setNormalState();
                    paramsTitle.rightMargin = marginRight;
                    paramsSearch.rightMargin = marginRight;
                    mSearchContainer.setLayoutParams(paramsSearch);
                    mUrlInput.setNotNeedShowDropState(false);
                } else {
                    paramsSearch.leftMargin = mToolsContiner1.getWidth()+ mTitleContainer.getWidth()+11;
                    paramsSearch.rightMargin = 18;
                    mSearchInput.setHint(R.string.title_search_normal_hint);
                    mSearchInput.setAlpha(1.0f);
                    formatUrl();
                    mSearchClear.setVisibility(View.GONE);
                    mSearchContainer.setTranslationX(0);
                }
                mTitleContainer.setLayoutParams(paramsTitle);
                mSearchContainer.setLayoutParams(paramsSearch);
                if (!mIsProgressViewShowing && mTitleBar.isInLoad() && !mIsBtnReadModeShowing) {
                    progressStartAnim();
                }
            }
        });
        aSet.playTogether(mAnimList);
        aSet.start();
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

    public void formatUrl() {
        if (mBaseUi == null)
            return;
        Tab activeTab = mBaseUi.getActiveTab();
        if (activeTab != null) {
            String url = activeTab.isShowHomePage() ? "" : activeTab.getUrl();
            if (mCurrentState == STATE_NORMAL) {
                setDisplayTitle(url);
                if (url != null && url.startsWith("https://")) {
                    mUrlInput.setCompoundDrawablesWithIntrinsicBounds(R.drawable.https_icon,0,0,0);
                } else {
                    mUrlInput.setCompoundDrawablesWithIntrinsicBounds(R.drawable.null_icon, 0, 0, 0);
                }
            } else if (mCurrentState == STATE_EDIT) {
                mUrlInput.setText(url);
                mUrlInput.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
            }

            if (activeTab.isShowHomePage()) {
                mUrlInput.setCompoundDrawablesWithIntrinsicBounds(mCurrentState == STATE_EDIT ? 0: R.drawable.null_icon, 0, 0, 0);
            }
        } else {
            mUrlInput.setCompoundDrawablesWithIntrinsicBounds(R.drawable.null_icon, 0, 0, 0);
        }
    }

    private void formatSearch() { // this is for adjust wrong padding of a background png. can delete below code if UI designer can give correct png
        if (mCurrentState == STATE_NORMAL) {
            mSearchInput.setCompoundDrawablesWithIntrinsicBounds(R.drawable.null_icon, 0, 0, 0);
        } else if (mCurrentState == STATE_EDIT) {
            mSearchInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private void setIncognitoDrawable() {
        Resources res = getContext().getResources();
        mStopDrawable = res.getDrawable(R.drawable.stop_loadurl_incognito_selector);
        mClearDrawable = res.getDrawable(R.drawable.clear_btn_incognito_selector);
        mRefreshDrawable = res.getDrawable(R.drawable.refresh_url_incognito_selector);
        mProgressView.setBackgroundResource(R.drawable.progress_icon_incognito);
        mCmccDrawable = res.getDrawable(R.drawable.cmcc_search_incognito_selector);
        mGoogleDrawable = res.getDrawable(R.drawable.google_search_incognito_selector);
        mBaiduDrawable = res.getDrawable(R.drawable.baidu_search_incognito_selector);
        mBingDrawable = res.getDrawable(R.drawable.bing_search_incognito_selector);
        mYahooUsDrawable = res.getDrawable(R.drawable.yahoo_search_us_incognito_selector);
        mYahooJpDrawable = res.getDrawable(R.drawable.yahoo_search_jp_incognito_selector);
        mWebiconDrawable = res.getDrawable(R.drawable.web_icon_private);
        mReadModeDrawable = res.getDrawable(R.drawable.btn_readmode_private_icon);
        mAddressMarkDrawable = res.getDrawable(R.drawable.addressbar_mask_private_land);
        mAddressMask.setImageDrawable(mAddressMarkDrawable);
        mWebIcon.setImageDrawable(mWebiconDrawable);
        mBtnReadMode.setImageDrawable(mReadModeDrawable);
        mSearchClear.setImageDrawable(mClearDrawable);
        if (mTitleBar.isInLoad()) {
            mStopButton.setImageDrawable(mStopDrawable);
        } else {
            mStopButton.setImageDrawable(mRefreshDrawable);
        }
        changeSearchIcon();
        changeLandBtnPrivate(true);
        int colorHint = res.getColor(R.color.input_hint_color);
        mCancelButton.setBackgroundResource(R.drawable.btn_title_cancel_incog_selector);
        mTitleContainer.setBackgroundResource(R.drawable.address_box_private);
        mUrlInput.setTextColor(Color.WHITE);
        mCancelButton.setTextColor(Color.WHITE);
        setHintColor(mUrlInput, colorHint);

        mSearchContainer.setBackgroundResource(R.drawable.search_box_private);
        mSearchInput.setTextColor(Color.WHITE);
        setHintColor(mSearchInput, colorHint);
    }

    private void setNormalDrawable() {
        Resources res = getContext().getResources();
        mStopDrawable = res.getDrawable(R.drawable.stop_loadurl_selector);
        mClearDrawable = res.getDrawable(R.drawable.clear_btn_selector);
        mRefreshDrawable = res.getDrawable(R.drawable.refresh_url_selector);
        mProgressView.setBackgroundResource(R.drawable.progress_icon);
        mCmccDrawable = res.getDrawable(R.drawable.cmcc_search_selector);
        mGoogleDrawable = res.getDrawable(R.drawable.google_search_selector);
        mBaiduDrawable = res.getDrawable(R.drawable.baidu_search_selector);
        mBingDrawable = res.getDrawable(R.drawable.bing_search_selector);
        mYahooUsDrawable = res.getDrawable(R.drawable.yahoo_search_us_selector);
        mYahooJpDrawable = res.getDrawable(R.drawable.yahoo_search_jp_selector);
        mWebiconDrawable = res.getDrawable(R.drawable.web_icon);
        mReadModeDrawable = res.getDrawable(R.drawable.btn_readmode_icon);
        mAddressMarkDrawable = res.getDrawable(R.drawable.addressbar_mask_land);
        mAddressMask.setImageDrawable(mAddressMarkDrawable);
        mWebIcon.setImageDrawable(mWebiconDrawable);
        mBtnReadMode.setImageDrawable(mReadModeDrawable);
        mSearchClear.setImageDrawable(mClearDrawable);
        if (mTitleBar.isInLoad()) {
            mStopButton.setImageDrawable(mStopDrawable);
        } else {
            mStopButton.setImageDrawable(mRefreshDrawable);
        }
        changeSearchIcon();
        changeLandBtnPrivate(false);
        int colorGrayHint = res.getColor(R.color.webview_find_edit_hint_color);
        mCancelButton.setBackgroundResource(R.drawable.btn_title_cancel_selector);
        mTitleContainer.setBackgroundResource(R.drawable.address_box);
        mUrlInput.setTextColor(Color.BLACK);
        int colorCancel = res.getColor(R.color.find_on_page_button);
        mCancelButton.setTextColor(colorCancel);
        setHintColor(mUrlInput, colorGrayHint);

        mSearchContainer.setBackgroundResource(R.drawable.search_box);
        mSearchInput.setTextColor(Color.BLACK);
        setHintColor(mSearchInput, colorGrayHint);
    }
    private void setHintColor(EditText inputView, int color) {
        // Seems that EditText apply the hintTextColor only if the text is empty. http://stackoverflow.com/questions/6438478/sethinttextcolor-in-edittext
        android.text.Editable text = inputView.getText();
        inputView.setText(null);
        inputView.setHintTextColor(color);
        inputView.setText(text);
    }

    public void setIncognitoMode(boolean incognito) {
        if(!mIsBtnReadModeShowing || mIsProgressViewShowing){
            mBtnReadMode.setTranslationX(-150);
        }
        if (mIncognito == incognito)
            return;
        mIncognito = incognito;

        if (incognito) {
            setIncognitoDrawable();
        } else {
            setNormalDrawable();
        }
        updateBackground(mIncognito);
        mUrlInput.setIncognitoMode(incognito);
    }

    private void setNormalState() {
        if (mBaseUi != null && mBaseUi.getWebView() != null
                && !TextUtils.isEmpty(mBaseUi.getWebView().getUrl())) {
            if (mTitleBar.isInLoad()) {
                mStopButton.setImageDrawable(mStopDrawable);
                mIsProgressViewShowing = false;
            } else {
                mStopButton.setImageDrawable(mRefreshDrawable);
            }
        } else {
            mStopButton.setVisibility(View.GONE);
        }
        LinearLayout.LayoutParams paramsStop = (LinearLayout.LayoutParams) mStopButton
                .getLayoutParams();
        paramsStop.rightMargin = 0;
        mStopButton.setLayoutParams(paramsStop);
    }

    public void setSearchEngine(String string) {
        Editor ed = BrowserSettings.getInstance().getPreferences().edit();
        ed.putString(BrowserSettings.PREF_SEARCH_ENGINE, string);
        ed.apply();
    }

    private String getSearchName() {
        SearchEngine searchEngine = BrowserSettings.getInstance()
                .getSearchEngine();
        return (searchEngine == null ? null : (String) searchEngine.getLabel());
    }

    public void changeSearchIcon() {
        if (mMagnify == null) {
            return;
        }
        if (getSearchName().equals(SEARCH_LABEL_139)) {
            mMagnify.setImageDrawable(mCmccDrawable);
        } else if (getSearchName().equals(SEARCH_LABEL_GOOGLE)) {
            mMagnify.setImageDrawable(mGoogleDrawable);
        } else if (getSearchName().equals(SEARCH_LABEL_BING)) {
            mMagnify.setImageDrawable(mBingDrawable);
        } else if (getSearchName().equals(SEARCH_LABEL_YAHOO)) {
            mMagnify.setImageDrawable(mYahooUsDrawable);
        } else if (getSearchName().equals(SEARCH_LABEL_YAHOO_JP)) {
            mMagnify.setImageDrawable(mYahooJpDrawable);
        } else {
            mMagnify.setImageDrawable(mBaiduDrawable);
        }
    }

    private void changeLandBtnPrivate(boolean isPrivate) {
        Resources res = getContext().getResources();
        Drawable preBtnDrawable;
        Drawable nextBtnDrawable;
        Drawable addBtnDrawable;
        Drawable switchBtnDrawable;
        Drawable menuBtnDrawable;
        if (isPrivate) {
            preBtnDrawable = res
                    .getDrawable(R.drawable.bottombar_previous_page_land_incog_selector);
            nextBtnDrawable = res.getDrawable(R.drawable.bottombar_next_page_land_incog_selector);
            addBtnDrawable = res.getDrawable(R.drawable.bottombar_add_tab_land_incog_selector);
            switchBtnDrawable = res
                    .getDrawable(R.drawable.bottombar_switch_tab_land_incog_selector);
            menuBtnDrawable = res.getDrawable(R.drawable.bottombar_menu_land_incog_selector);
        } else {
            preBtnDrawable = res.getDrawable(R.drawable.bottombar_previous_page_land_selector);
            nextBtnDrawable = res.getDrawable(R.drawable.bottombar_next_page_land_selector);
            addBtnDrawable = res.getDrawable(R.drawable.bottombar_add_tab_land_selector);
            switchBtnDrawable = res.getDrawable(R.drawable.bottombar_switch_tab_land_selector);
            menuBtnDrawable = res.getDrawable(R.drawable.bottombar_menu_land_selector);
        }
        mPreBtn.setImageDrawable(preBtnDrawable);
        mNextBtn.setImageDrawable(nextBtnDrawable);
        mSwitchBtn.setImageDrawable(switchBtnDrawable);
        mAddTabBtn.setImageDrawable(addBtnDrawable);
        mMenuBtn.setImageDrawable(menuBtnDrawable);
    }

    public void onPause() {
        disMissPopup();
    }

    private void disMissPopup() {
        if (mListPopup != null) {
            mListPopup.dismiss();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mUiController.onOptionsItemSelected(item);
    }

    @Override
    public void updateBackground(boolean isPrivate) {
        Resources res = mContext.getResources();
        Drawable titleBg;
        Drawable addressBg;
        Drawable searchBg;
        Drawable maskBg;
        if (isLand()) {
            if (isPrivate) {
                titleBg = res.getDrawable(R.drawable.title_bg_private);
                addressBg = res.getDrawable(R.drawable.address_box_private_land);
                searchBg = res.getDrawable(R.drawable.search_box_private_land);
                maskBg = res.getDrawable(R.drawable.addressbar_mask_private_land);
            } else {
                titleBg = res.getDrawable(R.drawable.title_bg);
                addressBg = res.getDrawable(R.drawable.address_box_land);
                searchBg = res.getDrawable(R.drawable.search_box_land);
                maskBg = res.getDrawable(R.drawable.addressbar_mask_land);
            }
            mAddressMask.setImageDrawable(maskBg);
        } else {
            if (isPrivate) {
                titleBg = res.getDrawable(R.drawable.title_bg_private);
                addressBg = res.getDrawable(R.drawable.address_box_private);
                searchBg = res.getDrawable(R.drawable.search_box_private);
            } else {
                titleBg = res.getDrawable(R.drawable.title_bg);
                addressBg = res.getDrawable(R.drawable.address_box);
                searchBg = res.getDrawable(R.drawable.search_box);
            }
        }
        setBackground(titleBg);
        mTitleContainer.setBackground(addressBg);
        mSearchContainer.setBackground(searchBg);
    }
}
