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
import android.graphics.Color;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.lang.reflect.Field;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.SuggestionsAdapter.CompletionListener;
import com.android.myapidemo.smartisan.browse.SuggestionsAdapter.SuggestItem;
import com.android.myapidemo.smartisan.browser.platformsupport.WebAddress;
import com.android.myapidemo.smartisan.browser.util.AgentUtil;
import com.android.myapidemo.smartisan.browser.util.UrlUtils;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;

/**
 * url/search input view
 * handling suggestions
 */
public class UrlInputView extends AutoCompleteTextView
        implements OnEditorActionListener,
        CompletionListener, OnItemClickListener, TextWatcher {

    static final String TYPED = "browser-type";
    static final String SUGGESTED = "browser-suggest";
    static final String MATCHES = "^file:///android_asset/incognito_mode_start_page.*";
    static final int ADDRESS_INPUT = 0;
    static final int SEARCH_INPUT = 1;
    private static int color = Color.argb((int) 120, Color.red(Color.WHITE), Color.green(Color.WHITE),
            Color.blue(Color.WHITE));

    private UrlInputListener mListener;
    private InputMethodManager mInputManager;
    private SuggestionsAdapter mAdapter;
    private BaseUi mBaseUi;
    private View mContainer;
    private boolean mLandscape;
    private boolean mIncognitoMode;
    private boolean mNotShowDropState;
    private int mType = ADDRESS_INPUT;
    private Rect mPopupPadding;
    private static String mText;
    private Context mContext;

    private static final String TAG = "UrlInputView";

    public UrlInputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPopupPadding = new Rect();
        init(context);
    }

    public UrlInputView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public UrlInputView(Context context) {
        this(context, null);
    }

    public void setBaseUi(BaseUi ui) {
        mBaseUi = ui;
    }

    private void init(Context ctx) {
        mContext = ctx;
        mInputManager = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        setOnEditorActionListener(this);
        mAdapter = new SuggestionsAdapter(ctx, this);
        setAdapter(mAdapter);
        int offset = ctx.getResources().getInteger(R.integer.auto_complete_textview_offset);
        setDropDownVerticalOffset(offset);
        setDropDownBackgroundResource(R.drawable.suggestion_bg);
        onConfigurationChanged(ctx.getResources().getConfiguration());
        setThreshold(1);
        setOnItemClickListener(this);
        mText = ctx.getResources().getString(R.string.search_incognito_hint);

        ReflectHelper.invokeProxyMethod("android.widget.TextView", "setAsSmartisanUrlInputView",
                this, new Class[] { boolean.class }, new Object[] {true});
        try {
            Field MENU_NO_DICT = ActionMode.class.getDeclaredField("MENU_NO_DICT");
            Field MENU_NO_SEARCH = ActionMode.class.getDeclaredField("MENU_NO_SEARCH");
            ReflectHelper.invokeProxyMethod("android.widget.TextView", "setHiddenContextMenuItem",
                    this, new Class[] { int.class },
                    new Object[] { MENU_NO_DICT.getInt(this) | MENU_NO_SEARCH.getInt(this) });
        } catch(Exception e) {}
    }

    void setController(UiController controller) {
//        UrlSelectionActionMode urlSelectionMode
//                = new UrlSelectionActionMode(controller);
//        setCustomSelectionActionModeCallback(urlSelectionMode);
    }

    void setContainer(View container) {
        mContainer = container;
    }

    public void setUrlInputListener(UrlInputListener listener) {
        mListener = listener;
    }

    public void setType(int type) {
        mType = type;
        mAdapter.setType(type);
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mLandscape = (config.orientation &
                Configuration.ORIENTATION_LANDSCAPE) != 0;
        mAdapter.setLandscapeMode(mLandscape);
        if (isPopupShowing() && (getVisibility() == View.VISIBLE)) {
            setupDropDown();
            performFiltering(getText(), 0);
        }
    }

    @Override
    public void showDropDown() {
        if (getSelectionEnd() - getSelectionStart() != this.length() && !mNotShowDropState) {
            setupDropDown();
            super.showDropDown();
        }
    }

    @Override
    public void dismissDropDown() {
        super.dismissDropDown();
        mAdapter.clearCache();
    }

    public void setNotNeedShowDropState(boolean isNotShowDropState) {
        mNotShowDropState = isNotShowDropState;
    }

    private void setupDropDown() {
        int width = mContainer != null ? mContainer.getWidth() : getWidth();
        width += mPopupPadding.left + mPopupPadding.right;
        if (width != getDropDownWidth()) {
            setDropDownWidth(width);
        }
        int left = getLeft();
        left += mPopupPadding.left;
        if (left != -getDropDownHorizontalOffset()) {
            setDropDownHorizontalOffset(-left);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        finishInput(getText().toString(), null, TYPED);
        return true;
    }

    void hideIME() {
        mInputManager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    void showIME() {
        mInputManager.showSoftInput(this, 0);
    }

    private void finishInput(String url, String extra, String source) {
        dismissDropDown();
        hideIME();
        if (TextUtils.isEmpty(url)) {
            mListener.onDismiss();
        } else {
            if (isSearch(url)) {
//                if (mType == 0) {
//                    AgentUtil.mTrackerAgent.onAttach(TAG, "ADDRESS_BAR");
//                    AgentUtil.mTrackerAgent.onDetach(TAG, "ADDRESS_BAR");
//                } else {
//                    AgentUtil.mTrackerAgent.onAttach(TAG, "SEARCH_BAR");
//                    AgentUtil.mTrackerAgent.onDetach(TAG, "SEARCH_BAR");
//                }
//                AgentUtil.mTrackerAgent.onAttach(TAG, "SEARCH_TOTAL");
//                AgentUtil.mTrackerAgent.onDetach(TAG, "SEARCH_TOTAL");
            }
            setTitle(url);
            mListener.onAction(url, extra, source);
        }
    }
    public void setTitle(String title) {
        try {
            setText(convertSpecialString(title, mBaseUi.getActiveTab()
                .isPrivateBrowsingEnabled()));
        } catch(Exception e) {}
    }

    boolean isSearch(String inUrl) {
        String url = UrlUtils.fixUrl(inUrl).trim();
        if (TextUtils.isEmpty(url)) return false;

        if (Patterns.WEB_URL.matcher(url).matches()
                || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url).matches()) {
            return false;
        }
        return true;
    }

    // Completion Listener

    @Override
    public void onSearch(String search) {
        mListener.onCopySuggestion(search);
    }

    @Override
    public void onSelect(String url, int type, String extra) {
        finishInput(url, extra, SUGGESTED);
    }

    @Override
    public void onItemClick(
            AdapterView<?> parent, View view, int position, long id) {
        SuggestItem item = mAdapter.getItem(position);
        onSelect(SuggestionsAdapter.getSuggestionUrl(item), item.type, item.extra);
    }

    interface UrlInputListener {

        public void onDismiss();

        public void onAction(String text, String extra, String source);

        public void onCopySuggestion(String text);

    }

    public void setIncognitoMode(boolean incognito) {
        mIncognitoMode = incognito;
        mAdapter.setIncognitoMode(mIncognitoMode);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent evt) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && !isInTouchMode()) {
            finishInput(null, null, null);
            return true;
        }
        return super.onKeyDown(keyCode, evt);
    }

    /*
     * no-op to prevent scrolling of webview when embedded titlebar
     * gets edited
     */
    @Override
    public boolean requestRectangleOnScreen(Rect rect, boolean immediate) {
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(mContainer == null ){
            return;
        }

        if (before == 0 && count > 1)
            showIME(); // force show ime if paste some text in the view

        String inputText = getText().toString().trim();

        if (mType == ADDRESS_INPUT) {
            if (TextUtils.isEmpty(inputText)) {
                ((NavigationBarPhone)mContainer).setStopButtonVisibility(View.GONE);
            }else {
                ((NavigationBarPhone)mContainer).setStopButtonVisibility(View.VISIBLE);
            }
        } else {
            if (TextUtils.isEmpty(inputText)) {
                ((NavigationBarPhone) mContainer).setSearchClearButtonVisibility(View.GONE);
            } else {
                if (inputText.length() != 0) {
                    ((NavigationBarPhone) mContainer).setSearchClearButtonVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) { }

    /**
     * convert url host is default color,other is gray
     * @param url
     * @return
     */
    public static SpannableString convertSpecialString(String text, boolean isPrivate) {
        SpannableString spannableString = new SpannableString(text);
        try {
            WebAddress webAddress = new WebAddress(text);
            String host = webAddress.getHost();
            if (!TextUtils.isEmpty(host)) {//some text is not url,like file:///xxxx
                int indexOf = text.indexOf(host);
                if (isPrivate) {
                    if (text.equals(mText)) {
                        spannableString.setSpan(new ForegroundColorSpan(color),
                                0, text.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        spannableString.setSpan(new ForegroundColorSpan(color),
                                indexOf + host.length(), text.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    spannableString.setSpan(new ForegroundColorSpan(Color.GRAY),
                            indexOf + host.length(), text.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //ingore it, if throw exception,it means text is not a url,we just return itself.
        }
        return spannableString;
    }
}
