
package com.android.myapidemo.smartisan.navigation;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class GlobalSearchBar extends RelativeLayout implements OnClickListener, AdapterView.OnItemClickListener {

    private Context mContext;
    private RelativeLayout mSearchBarView;

    private TextView mBtnClearSearch;

    private LinearLayout mSearchView;
    private EditText mSearchEdit;
    private TextView mSearchViewRight;
    private TextView mSearchViewLeft;
    private boolean mIsSearchMode = false;
    private boolean mIsPlaySearchModeAnimation = false;

    private int mDuration = 200;
    public static final int SORT_BY_NAME = (1 << 0);
    public static final int SORT_BY_TIME = (1 << 1);
    public static final int SORT_BY_SIZE = (1 << 2);
    private int mSortMode = SORT_BY_TIME;
    private DecelerateInterpolator mDecelerateInterpolator;

    private Listener mListener;

    public interface Listener {

        /**
         * Change sort mode.
         *
         * @param SortMode to wanted set mode.
         * @return false change mode failure.
         */
        boolean onModeChange(int sortMode);

        void setSoftKeyboardHide();

        void startSearchBarAnimation();

        void endSearchBarAnimation();

        void onQueryTextChange(String newString);

        void hideQuickContactView();

        void endSearchBarAnimationWithoutAnimation();

        void recoverQuickBar();
    }

    public GlobalSearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSearchBarView = (RelativeLayout) inflater.inflate(R.layout.global_searchbar,
                this);
        mDecelerateInterpolator = new DecelerateInterpolator(1.5F);
        setViews();
    }

    private void setViews() {
        mSearchView = (LinearLayout) mSearchBarView.findViewById(R.id.search_view);
        mSearchView.setOnClickListener(this);
        mBtnCancelSearch = (TextView) findViewById(R.id.btn_cancel);
        mSearchViewRight = (TextView) mSearchBarView
                .findViewById(R.id.searchbarright);
        mSearchViewRight.setOnClickListener(this);
        mSearchViewLeft = (TextView) mSearchBarView
                .findViewById(R.id.searchbarleft);
        mSearchViewLeft.setOnClickListener(this);

        mSearchEdit = (EditText) mSearchBarView.findViewById(R.id.search_edit_text);
        mSearchEdit.setOnClickListener(this);
        mSearchEdit.addTextChangedListener(mSearchWatcher);
        mSearchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE && mListener != null) {
                    mListener.setSoftKeyboardHide();
                }
                return false;
            }
        });
        mBtnCancelSearch.setOnClickListener(this);
        mBtnClearSearch = (TextView) mSearchBarView.findViewById(R.id.btn_clear_search);
        mBtnClearSearch.setOnClickListener(this);
    }

    /* package */void clearSearchText() {
        if (mSearchEdit != null) {
            mSearchEdit.setText("");
        }
    }

    private TextWatcher mSearchWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mIsSearchMode && mListener != null) {
                mListener.onQueryTextChange(String.valueOf(s));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            mBtnClearSearch.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
        }
    };

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void clearState(){
        TextKeyListener.clear(mSearchEdit.getText());
    }
    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.searchbarleft:
            case R.id.searchbarright:
                if (mIsSearchMode) {
                    break;
                }
            case R.id.search_edit_text: {
                if (mListener != null) {
                    mListener.hideQuickContactView();
                }
                startSearch();
                break;
            }
            case R.id.btn_cancel:
                cancelSearch();
                break;
            case R.id.btn_clear_search:
                mSearchEdit.setText("");
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                switchSortMode(SORT_BY_NAME);
                break;
            case 1:
                switchSortMode(SORT_BY_TIME);
                break;
            case 2: {
                switchSortMode(SORT_BY_SIZE);
                break;
            }
        }

    }

    private PopupWindow mPopupWindow;

    public static final String KEY_ICON_RESOURCE = "resource";
    public static final String KEY_SORT_TYPE = "sort_type";
    public static final String KEY_IS_SELECTED = "selected";

    private void switchSortMode(int sortMode) {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
        if (mSortMode == sortMode) {
            return;
        }
        if (mListener != null && mListener.onModeChange(sortMode)) {
            mSortMode = sortMode;
        }
    }

    public void requestSeachViewFocus() {
        mSearchEdit.requestFocus();
    }
    private TextView mBtnCancelSearch;
    public void startSearch() {
        if (!mIsPlaySearchModeAnimation && !mIsSearchMode) {
            mSearchEdit.setCursorVisible(true);
            mSearchEdit.setFocusable(true);
            mSearchEdit.setFocusableInTouchMode(true);
            mSearchEdit.requestFocus();
            postDelayed(new Runnable() {
                public void run() {
                    InputMethodManager imm = (InputMethodManager) mContext
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(mSearchEdit, 0);
                }
            }, 200);
            startAnimation(mBtnCancelSearch);
            mIsSearchMode = true;
        }
    }

    public void cancelSearch() {
        if (!mIsPlaySearchModeAnimation && mIsSearchMode) {
            if (mListener != null) {
                mListener.setSoftKeyboardHide();
            }
            TextKeyListener.clear(mSearchEdit.getText());
            cancelAnimation(mBtnCancelSearch);
            mIsSearchMode = false;
            mSearchEdit.setFocusable(false);
            mSearchEdit.setFocusableInTouchMode(false);
        }
    }

    public void cancelSearchWithoutAnimation() {
        if (!mIsPlaySearchModeAnimation && mIsSearchMode) {
            post(new Runnable() {
                @Override
                public void run() {
                    mListener.setSoftKeyboardHide();
                }
            });
            mSearchViewRight.setTranslationX(0);
            mBtnCancelSearch.setVisibility(View.GONE);
            TextKeyListener.clear(mSearchEdit.getText());
            mIsSearchMode = false;
            mSearchEdit.setFocusable(false);
            mSearchEdit.setFocusableInTouchMode(false);
            if (mListener != null) {
                mListener.endSearchBarAnimationWithoutAnimation();
            }
        }
    }

    /**
     * Perform the start search or cancel search animation.
     */
    private void startAnimation(final View visibleView) {
        mIsPlaySearchModeAnimation = true;
        final float visibleXDelta = visibleView.getWidth();
        final Animator rightViewAnimator = ObjectAnimator.ofFloat(mSearchViewRight, "translationX", 0, -visibleXDelta);
        rightViewAnimator.setDuration(mDuration);
        rightViewAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) mContext
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mSearchEdit, 0);
                    }
                });
            }
        });

        final TranslateAnimation visibleAnimation = new TranslateAnimation(visibleXDelta, 0, 0, 0);
//        visibleAnimation.setInterpolator(mDecelerateInterpolator);
        visibleAnimation.setDuration(mDuration);
        visibleAnimation.setAnimationListener(new AnimationListenerWrapper() {

            @Override
            public void onAnimationEnd(Animation animation) {
                visibleView.setVisibility(View.VISIBLE);
                mIsPlaySearchModeAnimation = false;
            }
        });
        resetSearchViewParam((int) visibleXDelta + CommonUtil.dip2px(mContext, 6));
        if (mListener != null) {
            mListener.startSearchBarAnimation();
        }
        rightViewAnimator.start();
        visibleView.startAnimation(visibleAnimation);
    }

    private void cancelAnimation(final View goneView) {
        final float goneXDelta = goneView.getWidth();

        final Animator rightViewAnimator = ObjectAnimator.ofFloat(mSearchViewRight, "translationX", -goneXDelta, 0);
        rightViewAnimator.setDuration(mDuration);

        TranslateAnimation goneAnimation = new TranslateAnimation(0, goneXDelta, 0, 0);
//        goneAnimation.setInterpolator(mDecelerateInterpolator);
        goneAnimation.setDuration(mDuration);
        goneAnimation.setAnimationListener(new AnimationListenerWrapper() {

            @Override
            public void onAnimationStart(Animation animation) {
                mIsPlaySearchModeAnimation = true;
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mIsPlaySearchModeAnimation = false;
                resetSearchViewParam(0);
                goneView.setVisibility(View.INVISIBLE);
            }
        });
        if (mListener != null) {
            mListener.endSearchBarAnimation();
        }
        goneView.startAnimation(goneAnimation);
        rightViewAnimator.start();
    }

    private void resetSearchViewParam(int marginRight) {
        RelativeLayout.LayoutParams params = (LayoutParams) mSearchView.getLayoutParams();
        params.rightMargin = marginRight;
        mSearchView.setLayoutParams(params);
    }

    public void setCursorVisible(boolean isVisible) {
        mSearchEdit.setCursorVisible(isVisible);
    }

    public boolean isPlayingSearchAnimation() {
        return mIsPlaySearchModeAnimation;
    }

    public boolean isInSearchMode() {
        return mIsSearchMode;
    }

    public void freezeSearchView(boolean freeze) {
        mSearchEdit.setEnabled(!freeze);
    }

    /**
     * used to wrap {@link AnimationListener}, mostly time we don't need override all method in
     * {@link AnimationListener}. So we can use this class to override the method we need but all.
     */
    private class AnimationListenerWrapper implements AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
