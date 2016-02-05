
package com.android.myapidemo.smartisan.browse;

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

import com.android.myapidemo.R;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebView.FindListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import android.widget.LinearLayout;

public class ActionModeTitleBar extends LinearLayout implements OnClickListener, TextWatcher,
        FindListener {
    /**
     * @param context
     */

    private UiController mUiController;
    private BaseUi mBaseUi;
    private FrameLayout mContentView;

    private static int ANIMATION_TIME = 200;
    private static int LAYOUT_LEFT_POINT = 0;

    // state
    private boolean mSelectionState;
    private boolean isStart;
    private boolean isFinal;

    private View mCustomView;
    private EditText mEditText;
    private TextView mMatches;
    private TextView mDoneButton;
    private ImageView mPreBtn;
    private ImageView mNextBtn;
    private WebView mWebView;
    private RelativeLayout mFindLayout;
    private LinearLayout mFindBar;
    private LinearLayout mFindEidt;

    private boolean mMatchesFound;
    private boolean mFocusState;
    private boolean mIncogState;
    private int mNumberOfMatches;
    private int mActiveMatchIndex;
    private InputMethodManager mInput;
    private Resources mResources;

    public ActionModeTitleBar(Context context, UiController controller, BaseUi ui,
            FrameLayout contentView) {
        super(context, null);
        mUiController = controller;
        mBaseUi = ui;
        mContentView = contentView;
        initLayout(context);
        attach();
    }

    private void initLayout(Context context) {
        mCustomView = LayoutInflater.from(context).inflate(
                R.layout.webview_find, this);
        mFindBar = (LinearLayout) mCustomView.findViewById(R.id.find_bar);
        mDoneButton = (TextView) mCustomView.findViewById(R.id.finish);
        mDoneButton.setOnClickListener(this);
        mEditText = (EditText) mCustomView.findViewById(R.id.edit);
        mEditText.addTextChangedListener(this);
        mEditText.setOnClickListener(this);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    int endIndex = mEditText.getText().toString().length();
                    mEditText.setSelection(endIndex, endIndex);

                }
                return false;
            }
        });
        mEditText.setSelectAllOnFocus(true);
        mEditText.requestFocus();
        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mInput.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                }
            }
        });
        mMatches = (TextView) mCustomView.findViewById(R.id.matches);
        mPreBtn = (ImageView) mCustomView.findViewById(R.id.pre_button);
        mPreBtn.setOnClickListener(this);
        mNextBtn = (ImageView) mCustomView.findViewById(R.id.next_button);
        mNextBtn.setOnClickListener(this);
        mFindLayout = (RelativeLayout) mCustomView.findViewById(R.id.find_button);
        mFindEidt = (LinearLayout) mCustomView.findViewById(R.id.find_edit);
        mMatches.setText("");
        mInput = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mResources = context.getResources();
        mIncogState = mUiController.getTabControl().getIncogMode();
        if (mIncogState) {
            setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled_private);
            setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled_private);
        } else {
            setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled);
            setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled);
        }
        int width = getResources().getInteger(R.integer.find_edit_width);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                width, LinearLayout.LayoutParams.WRAP_CONTENT);
        mFindEidt.setLayoutParams(params);
        if (mUiController.getTabControl() != null && mUiController.getTabControl().getIncogMode()) {
            changeIncogMode(true);
        }else{
            changeIncogMode(false);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        int width = getResources().getInteger(R.integer.find_edit_width);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                width, LinearLayout.LayoutParams.WRAP_CONTENT);
        mFindEidt.setLayoutParams(params);
        attach();
    }

    private void attach() {
        // If getParent() returns null, we are initializing
        if (getParent() != null)
            return;
        mBaseUi.addFixedTitleBar(this);
    }

    public void setSelectionState(boolean isSelection) {
        mSelectionState = isSelection;
    }

    /*
     * Set the WebView to search. Must be non null, and set before calling
     * startActionMode.
     */
    public void updateWebView() {
        WebView webView = getCurrentWebView();
        if (null == webView) {
            throw new AssertionError("WebView supplied to "
                    + "FindActionModeCallback cannot be null");
        }
        mWebView = webView;
        mWebView.setFindListener(this);
        mWebView.setOnClickListener(this);
    }

    private WebView getCurrentWebView() {
        Tab t = mBaseUi.getActiveTab();
        if (t != null) {
            return t.getWebView();
        } else {
            return null;
        }
    }

    public void updateMatchCount(int matchIndex, int matchCount, boolean isEmptyFind) {
        if (!isEmptyFind) {
            mNumberOfMatches = matchCount;
            mActiveMatchIndex = matchIndex;
            if (mIncogState) {
                if (mActiveMatchIndex == 0) {
                    isStart = true;
                    setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled_private);
                } else {
                    isStart = false;
                    setFindBtnEnable(mPreBtn, true, R.drawable.previous_btn_incog_selector);
                }
                if (mActiveMatchIndex == matchCount - 1) {
                    setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled_private);
                    isFinal = true;
                } else {
                    isFinal = false;
                    setFindBtnEnable(mNextBtn, true, R.drawable.next_btn_incog_selector);
                }
            } else {
                if (mActiveMatchIndex == 0) {
                    isStart = true;
                    setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled);
                } else {
                    isStart = false;
                    setFindBtnEnable(mPreBtn, true, R.drawable.previous_btn_selector);
                }
                if (mActiveMatchIndex == matchCount - 1) {
                    setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled);
                    isFinal = true;
                } else {
                    isFinal = false;
                    setFindBtnEnable(mNextBtn, true, R.drawable.next_btn_selector);
                }
            }
            updateMatchesString();
        } else {
            mMatches.setVisibility(View.INVISIBLE);
            mNumberOfMatches = 0;
        }
    }

    /*
     * Update the string which tells the user how many matches were found, and
     * which match is currently highlighted.
     */
    private void updateMatchesString() {
        boolean shouldShowMatchesView = true;
        if (mNumberOfMatches == 0 || mNumberOfMatches == 1) {
            if (mIncogState) {
                setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled_private);
                setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled_private);
            } else {
                setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled);
                setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled);
            }
            if (ActionModeTitleBar.this.getVisibility() == View.GONE) {
                mMatches.setText("");
            } else if (mNumberOfMatches == 1) {
                mMatches.setText((mActiveMatchIndex + 1) + "/" + mNumberOfMatches);
            } else {
                mMatches.setText(R.string.no_matches);
            }
            if (mEditText != null && mEditText.getText() != null
                    && mEditText.getText().length() == 0) {
                shouldShowMatchesView = false;
            }
        } else {
            if (mIncogState) {
                if (isStart) {
                    setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled_private);
                    setFindBtnEnable(mNextBtn, true, R.drawable.next_btn_incog_selector);
                } else if (isFinal) {
                    setFindBtnEnable(mPreBtn, true, R.drawable.previous_btn_incog_selector);
                    setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled_private);
                } else {
                    setFindBtnEnable(mPreBtn, true, R.drawable.previous_btn_incog_selector);
                    setFindBtnEnable(mNextBtn, true, R.drawable.next_btn_incog_selector);
                }
            } else {
                if (isStart) {
                    setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled);
                    setFindBtnEnable(mNextBtn, true, R.drawable.next_btn_selector);
                } else if (isFinal) {
                    setFindBtnEnable(mPreBtn, true, R.drawable.previous_btn_selector);
                    setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled);
                } else {
                    setFindBtnEnable(mPreBtn, true, R.drawable.previous_btn_selector);
                    setFindBtnEnable(mNextBtn, true, R.drawable.next_btn_selector);
                }
            }
            mMatches.setText((mActiveMatchIndex + 1) + "/" + mNumberOfMatches);
        }
        if (shouldShowMatchesView) {
            mMatches.setVisibility(View.VISIBLE);
        } else {
            mMatches.setVisibility(View.GONE);
        }
    }


    /*
     * Move the highlight to the next match.
     * @param next If true, find the next match further down in the document. If
     * false, find the previous match, up in the document.
     */
    private void findNext(boolean next) {
        if (mWebView == null) {
            throw new AssertionError(
                    "No WebView for FindActionModeCallback::findNext");
        }
        if (!mMatchesFound) {
            findAll();
            return;
        }
        if (0 == mNumberOfMatches) {
            // There are no matches, so moving to the next match will not do
            // anything.
            return;
        }
        mWebView.findNext(next);
        updateMatchesString();
    }

    /*
     * Highlight all the instances of the string from mEditText in mWebView.
     */
    void findAll() {
        if (mWebView == null) {
            return;
        }
        CharSequence find = mEditText.getText();
        if (0 == find.length()) {
            if (mIncogState) {
                setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled_private);
                setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled_private);
            } else {
                setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled);
                setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled);
            }
            try {
                mWebView.clearMatches();
            } catch(NullPointerException e) {}
            mMatches.setVisibility(View.GONE);
            mMatchesFound = false;
            mWebView.findAll(null);
        } else {
            mMatchesFound = true;
            mNumberOfMatches = 0;
            mWebView.findAllAsync(find.toString());
        }
    }

    private void setFindBtnEnable(ImageView imgBtn, boolean enable, int resourcesID) {
        imgBtn.setEnabled(enable);
        Drawable drawable = mResources.getDrawable(resourcesID);
        imgBtn.setImageDrawable(drawable);
    }

    private ViewGroup.LayoutParams makeLayoutParams() {
        return new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
    }

    public void changeIncogMode(boolean isIncog) {
        mIncogState = isIncog;
        if (mIncogState) {
            mFindBar.setBackgroundResource(R.drawable.title_bg_private);
            mFindEidt.setBackgroundResource(R.drawable.address_box_private);
            mEditText.setTextColor(Color.WHITE);
            int color = mResources.getColor(R.color.input_hint_color);
            mEditText.setHintTextColor(color);
            mMatches.setTextColor(color);
            mDoneButton.setTextColor(Color.WHITE);
            mDoneButton.setBackgroundResource(R.drawable.btn_title_incog_selector);
            setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled_private);
            setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled_private);
        } else {
            mFindBar.setBackgroundResource(R.drawable.title_bg);
            mFindEidt.setBackgroundResource(R.drawable.address_box);
            mEditText.setTextColor(Color.BLACK);
            int color = mResources.getColor(R.color.webview_find_edit_hint_color);
            mEditText.setHintTextColor(color);
            mMatches.setTextColor(color);
            mDoneButton.setTextColor(mResources.getColor(R.color.find_on_page_button));
            mDoneButton.setBackgroundResource(R.drawable.btn_title_selector);
            setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled);
            setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled);
        }
    }

    public void onResume() {
        if (mEditText == null) {
            return;
        }
        if (mFocusState) {
            mEditText.requestFocus();
        } else {
            mEditText.clearFocus();
        }
    }

    public void onPause() {
        if (mEditText == null) {
            return;
        }
        mFocusState = mEditText.hasFocus();
    }

    private void hideInputMethod() {
        if (mInput == null || mBaseUi == null || mBaseUi.getActivity() == null
                || mBaseUi.getActivity().getCurrentFocus() == null) {
            return;
        }
        mInput.hideSoftInputFromWindow(mBaseUi.getActivity().getCurrentFocus().getWindowToken(),
                0);
    }

    @Override
    public void onClick(View v) {
        if (mDoneButton == v && mWebView != null) {
            mMatches.setVisibility(View.GONE);
            hide();
        } else if (mWebView == v) {
// qinjin said it is a requirement: click the webpage will hide search bar.
// but below implement will cause issue. so comment it for now.
//            this.setVisibility(View.GONE);
//            mBaseUi.getTitleBar().setVisibility(View.VISIBLE);
        } else if (mPreBtn == v) {
            findNext(false);
            hideInputMethod();
        } else if (mNextBtn == v) {
            findNext(true);
            hideInputMethod();
        } else if (mEditText == v) {
            if (mSelectionState) {
                mSelectionState = false;
                findAll();
            }
        }
    }

    public void clearMatches(){
        if (mWebView != null) {
            try {
                mWebView.clearMatches();
                mWebView.findAllAsync(null);
            } catch(NullPointerException e) {}
            mEditText.setText("");
        }
    }

    public void hide() {
        if (mWebView != null)
            mWebView.getSettings().setJavaScriptEnabled(
                    BrowserSettings.getInstance().enableJavascript());
        if (mIncogState) {
            setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled_private);
            setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled_private);
        } else {
            setFindBtnEnable(mPreBtn, false, R.drawable.previous_btn_disabled);
            setFindBtnEnable(mNextBtn, false, R.drawable.next_btn_disabled);
        }

        float width = (float) mDoneButton.getWidth();
        ObjectAnimator doneBtnAnim = ObjectAnimator.ofFloat(mDoneButton,
                "translationX", 0f, -width);
        ObjectAnimator findBtnAnim = ObjectAnimator
                .ofFloat(mFindLayout,
                        "translationX", 0f,
                        (float) mFindLayout.getRight() + (float) mFindLayout.getWidth());

        ObjectAnimator alphaAnim = ObjectAnimator
                .ofFloat(mEditText,
                        "alpha", 1f, 0f);

        int end = getResources().getInteger(R.integer.find_on_page_close_end_point);

        PropertyValuesHolder searchLeft = PropertyValuesHolder.ofInt("left",
                mFindEidt.getLeft(), LAYOUT_LEFT_POINT);
        PropertyValuesHolder searchRight = PropertyValuesHolder.ofInt("right",
                mFindEidt.getRight(), end);
        Animator inputAnim = ObjectAnimator.ofPropertyValuesHolder(mFindEidt,
                searchLeft, searchRight);

        AnimatorSet asAnimatorSet = new AnimatorSet();
        asAnimatorSet.playTogether(inputAnim, doneBtnAnim, findBtnAnim, alphaAnim);
        asAnimatorSet.setDuration(ANIMATION_TIME);
        asAnimatorSet.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                clearMatches();
                mEditText.setAlpha(1f);
                mCustomView.setVisibility(View.GONE);
                mBaseUi.getTitleBar().setVisibility(View.VISIBLE);
                hideInputMethod();
            }

            @Override
            public void onAnimationCancel(Animator arg0) {

            }
        });
        asAnimatorSet.start();
    }

    @Override
    public void afterTextChanged(Editable arg0) {

    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        findAll();
    }

    @Override
    public void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches,
            boolean isDoneCounting) {
        updateMatchCount(activeMatchOrdinal, numberOfMatches, !isDoneCounting);

    }

}
