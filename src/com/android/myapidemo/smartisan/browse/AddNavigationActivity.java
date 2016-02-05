
package com.android.myapidemo.smartisan.browse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ExpandableListView.OnGroupClickListener;

import java.util.ArrayList;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.adapter.AddNavAdapter;
import com.android.myapidemo.smartisan.adapter.AddNavSearchAdapter;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.navigation.AddNavigationInfo;
import com.android.myapidemo.smartisan.navigation.AnimatedExpandableListView;
import com.android.myapidemo.smartisan.navigation.GlobalSearchBar;
import com.android.myapidemo.smartisan.navigation.GlobalSearchBar.Listener;
import com.android.myapidemo.smartisan.navigation.NavigationInfo;

public class AddNavigationActivity extends Activity implements Listener, OnClickListener {
    private AnimatedExpandableListView mAnimatedExpandableListView;
    private GlobalSearchBar mSearchBar;
    private View mShadeBackground;
    private AddNavAdapter mAdapter;
    private Point mOutSize = new Point();
    private ArrayList<NavigationInfo> mInfos = new ArrayList<NavigationInfo>();
    private ListView mSearchListView;
    public static final float ROTATION_DEGEE = -180;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_navigation);
        initTitle();
        initSearchBar();
        initExpandableListView();
    }

    private View mTitlebar;

    private void initTitle() {
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM);
        mTitlebar = LayoutInflater.from(this).inflate(
                        R.layout.add_navigation_header, null);
        getActionBar().setCustomView(
                mTitlebar,
                new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        mTitlebar.setVisibility(View.VISIBLE);
        findViewById(R.id.header_left).setOnClickListener(this);
        findViewById(R.id.header_right).setOnClickListener(this);
    }

    private View mFrame;

    private void initSearchBar() {
        mSearchBar = (GlobalSearchBar) findViewById(R.id.search_bar);
        mFrame = findViewById(R.id.frame);
        mShadeBackground = findViewById(R.id.sv_background);
        mShadeBackground.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mSearchBar.cancelSearch();
                return true;
            }
        });
        getWindowManager().getDefaultDisplay().getSize(mOutSize);
        mSearchBar.setListener(this);
    }

    private void initExpandableListView() {
        NavigationInfoParser parser = NavigationInfoParser.getInstance(this);
        ArrayList<AddNavigationInfo> addNavigationInfos = parser.parseAddNavigationInfos();
        mAnimatedExpandableListView = (AnimatedExpandableListView) findViewById(R.id.listview);
        mAdapter = new AddNavAdapter(this);
        mAdapter.setNavigationInfos(addNavigationInfos);
        mAnimatedExpandableListView.setAdapter(mAdapter);
        mAnimatedExpandableListView.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition,
                    long id) {
                boolean isExpanded = mAnimatedExpandableListView.isGroupExpanded(groupPosition);
                ImageView imgArrow = (ImageView) v.findViewById(R.id.arrow);
                if (isExpanded) {
                    mAnimatedExpandableListView.collapseGroupWithAnimation(groupPosition);
                } else {
                    boolean needScroll = false;
                    for (int i = 0; i < mAdapter.getGroupCount(); i++) {
                        if (mAnimatedExpandableListView.isGroupExpanded(i)) {
                            mAnimatedExpandableListView.collapseGroupWithAnimation(i);
                            needScroll = (groupPosition == mAdapter.getGroupCount() - 1);
                            break;
                        }
                    }
                    for (int i = 0; i < mAnimatedExpandableListView.getChildCount(); i++) {
                        View child = mAnimatedExpandableListView.getChildAt(i);
                        View arrow = child.findViewById(R.id.arrow);
                        if(arrow != null && arrow != imgArrow){
                            ObjectAnimator rotationAnimation = ObjectAnimator.ofFloat(arrow, View.ROTATION, arrow.getRotation(), 0);
                            rotationAnimation.setDuration(250);
                            rotationAnimation.setInterpolator(new DecelerateInterpolator(1.0f));
                            rotationAnimation.start();
                        }
                    }
                    mAnimatedExpandableListView.expandGroupWithAnimation(groupPosition);
                    if (needScroll) {
                        mAnimatedExpandableListView.smoothScrollToPositionFromTop(groupPosition, 0, 10);
                    }
                }
                float start = imgArrow.getRotation();
                float end = isExpanded ? 0 : ROTATION_DEGEE;
                ObjectAnimator rotationAnimation = ObjectAnimator.ofFloat(imgArrow, View.ROTATION, start, end);
                rotationAnimation.setDuration(250);
                rotationAnimation.setInterpolator(new DecelerateInterpolator(1.0f));
                rotationAnimation.start();
                return true;
            }
        });
        if (addNavigationInfos.size() > 0) {
            mAnimatedExpandableListView.expandGroup(0);
            mAnimatedExpandableListView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                        int oldRight, int oldBottom) {
                    if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                        mAnimatedExpandableListView.removeOnLayoutChangeListener(this);
                        View child = mAnimatedExpandableListView.getChildAt(0);
                        View arrow = child.findViewById(R.id.arrow);
                        if(arrow != null){
                            arrow.setRotation(ROTATION_DEGEE);
                        }
                    }
                }
            });
        }
        mSearchListView = (ListView) findViewById(R.id.search_listview);
        mSearchListView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    setSoftKeyboardHide();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    @Override
    public boolean onModeChange(int sortMode) {
        return false;
    }

    @Override
    public void setSoftKeyboardHide() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //don't restore anything
    };

    AnimatorSet mStartSearchAnimatorSet;
    int mTitleBarHeight;

    @Override
    public void startSearchBarAnimation() {
        getActionBar().hide();
        mShadeBackground.postDelayed(new Runnable() {
            @Override
            public void run() {
                mShadeBackground.setAlpha(0);
                mShadeBackground.setVisibility(View.VISIBLE);
                PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofFloat("alpha", 0, 1);
                Animator alphaAnimator = ObjectAnimator.ofPropertyValuesHolder(mShadeBackground, pvhAlpha);
                mStartSearchAnimatorSet = new AnimatorSet();
                mStartSearchAnimatorSet.setStartDelay(10);
                mStartSearchAnimatorSet.setDuration(200);
                mStartSearchAnimatorSet.playTogether(alphaAnimator);
                mStartSearchAnimatorSet.start();
            }
        }, 300);
    }

    private AnimatorSet mEndSearchAnimatorSet;

    @Override
    public void endSearchBarAnimation() {
        PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat("alpha", 1, 0);
        Animator alpha = ObjectAnimator.ofPropertyValuesHolder(mShadeBackground, pvhA);
        mEndSearchAnimatorSet = new AnimatorSet();
        mEndSearchAnimatorSet.setDuration(200);
        mEndSearchAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                searchAnimationEnd();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                searchAnimationEnd();
            }
        });
        mEndSearchAnimatorSet.playTogether(alpha);
        mEndSearchAnimatorSet.start();
    }

    protected void searchAnimationEnd() {
        mShadeBackground.setVisibility(View.GONE);
        mSearchBar.setCursorVisible(false);
        getActionBar().show();
    }

    @Override
    public void onBackPressed() {
        if (mSearchBar != null && mSearchBar.isInSearchMode()) {
            mSearchBar.cancelSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onQueryTextChange(String key) {
        if (TextUtils.isEmpty(key)) {
            if (mSearchListView != null && mSearchListView.getEmptyView() != null) {
                mSearchListView.getEmptyView().setVisibility(View.GONE);
                mSearchListView.setVisibility(View.GONE);
            }
            mShadeBackground.setVisibility(View.VISIBLE);
        } else {
            NavigationInfoParser infoParser = NavigationInfoParser.getInstance(this);
            ArrayList<NavigationInfo> infos = infoParser.searchNavigationInfos(key);
            mShadeBackground.setVisibility(View.GONE);
            putDataInListview(infos);
        }
    }
    private AddNavSearchAdapter mAddNavSearchAdapter;
    private void putDataInListview(ArrayList<NavigationInfo> infos) {
        mInfos.clear();
        mInfos.addAll(infos);
        mSearchListView.setVisibility(View.VISIBLE);
        if (mAddNavSearchAdapter == null) {
            mAddNavSearchAdapter = new AddNavSearchAdapter(this, mInfos);
            View emptyView = findViewById(R.id.search_empty_view);
            mSearchListView.setEmptyView(emptyView);
            mSearchListView.setAdapter(mAddNavSearchAdapter);
        } else {
            mAddNavSearchAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void hideQuickContactView() {

    }

    @Override
    public void endSearchBarAnimationWithoutAnimation() {

    }

    @Override
    public void recoverQuickBar() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.header_left:
                finish();
                break;
            case R.id.header_right:
                Intent intent = new Intent(this, EditNavActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.pop_up_in, R.anim.activity_close_enter_in_call);
                break;
            default:
                break;
        }
    }
    public void requestFullScreen() {
        Window win = getWindow();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestFullScreen();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        requestFullScreen();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_slide_do_nothing,R.anim.slide_down_out);
    }
}
