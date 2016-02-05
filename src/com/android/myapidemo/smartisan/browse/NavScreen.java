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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.TabControl.OnThumbnailUpdatedListener;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.myapidemo.smartisan.browse.Browser;

@SuppressLint("NewApi")
public class NavScreen extends RelativeLayout implements OnClickListener,
        OnMenuItemClickListener, OnThumbnailUpdatedListener {

    UiController mUiController;
    PhoneUi mUi;
    TabControl mTc;
    Tab mTab;
    Activity mActivity;
    ImageButton mRefresh;
    ImageButton mForward;
    // ImageButton mMore;
    ImageButton mNewTab;
    ImageButton mClearAllTab;
    ImageButton mFullScreen;
    FrameLayout mHolder;

    TextView mTitle;
    ImageView mFavicon;
    ImageButton mCloseTab;

    Browser mApplication = null;
    // NavTabScroller mScroller;
    // TabAdapter mAdapter;
    boolean mNeedsMenu;
    boolean mPrivateState;
    HashMap<Tab, View> mTabViews;
    NavTabViewPagerBuilder mBuilder;
    //public MenuDialog mDialog;
    private ScheduledExecutorService scheduledExecutorService;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    int orientation = getResources().getConfiguration().orientation;
                    Drawable drawable;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        drawable = getResources().getDrawable(R.drawable.navsreen_land_bg);
                    } else {
                        drawable = getResources().getDrawable(R.drawable.navscreen_bg);
                    }
                    setBackground(drawable);
                    break;
            }
        }
    };

    public NavScreen(Activity activity, UiController ctl, PhoneUi ui) {
        super(activity);
        mActivity = activity;
        mUiController = ctl;
        mUi = ui;
        init();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mUiController.onOptionsItemSelected(item);
    }

    protected float getToolbarHeight() {
        return mActivity.getResources().getDimension(R.dimen.toolbar_height);
    }

    @Override
    protected void onConfigurationChanged(Configuration newconfig) {
        //mApplication.setmThumbnailPosition(mBuilder.mCurrentConfigPosition);
        removeAllViews();
        int visibility = View.VISIBLE;
        if(mBuilder.mList.size() == 1){//mark the last tab's close btn state Ticket:0029253
            visibility = mBuilder.mList.get(0).mImageViewLeft.getVisibility();
        }
        init();
        if(mBuilder.mList.size() > 0){
            mBuilder.mList.get(0).mImageViewLeft.setVisibility(visibility);
            for (ThumbnailItems thumbnailItems : mBuilder.mList) {
                thumbnailItems.showTabViewTitle();
            }
        }
        mBuilder.setCurrentItemView(mApplication.getmThumbnailPosition());
        updateBackground(mPrivateState);
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.nav_screen, this);
        setContentDescription(getContext().getResources().getString(
                R.string.accessibility_transition_navscreen));
        System.out.println("================= NavScreen init 11111111 ====================");
        //mApplication = (Browser) mActivity.getApplication();
        mNewTab = (ImageButton) findViewById(R.id.newtab);
        mClearAllTab = (ImageButton) findViewById(R.id.clearall);
        mFullScreen = (ImageButton) findViewById(R.id.fullscreen);
        mClearAllTab.setOnClickListener(this);
        mFullScreen.setOnClickListener(this);
        mNewTab.setOnClickListener(this);
        mTc = mUiController.getTabControl();
        mTabViews = new HashMap<Tab, View>(mTc.getTabCount());
        mBuilder = (NavTabViewPagerBuilder) findViewById(R.id.viewPagerBuilder);
        mBuilder.makeViewPager(mUi, mTc);
        mNeedsMenu = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
        changeIncogStyle(mTc.getIncogMode());
    }

    public void changeIncogStyle(boolean isPrivate) {
        mPrivateState = isPrivate;
        updateBackground(mPrivateState);
        if (mPrivateState) {
            mNewTab.setImageResource(R.drawable.add_new_page_incog_selector);
            mClearAllTab.setImageResource(R.drawable.close_all_thumbnails_incog_selector);
            mFullScreen.setImageResource(R.drawable.full_screen_incog_selector);
        } else {
            mNewTab.setImageResource(R.drawable.add_new_page_light_selector);
            mClearAllTab.setImageResource(R.drawable.close_all_thumbnails_selector);
            mFullScreen.setImageResource(R.drawable.full_screen_selector);
        }
    }

    private void updateBackground(boolean isPrivate) {
        if (isPrivate) {
            Drawable drawable = getResources().getDrawable(R.drawable.navscreen_incog_background);
            setBackground(drawable);
        } else {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    mHandler.obtainMessage(0).sendToTarget();
                }
            };
            Thread t = new Thread(runnable);
            t.start();
        }
    }

    @Override
    public void onClick(View v) {
        if (!mBuilder.mIsShowing) {
            return;
        }

        if (mNewTab == v) {
            if (mUiController.getTabControl().canCreateNewTab()) {
                callBackNewTabAnim();
            }else{
                boolean isShow = mUi.isShowMaxTabsDialog(new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        mUi.closeTheLeastUsedTab();
                        mUi.updateCheckPrompt();
                        callBackNewTabAnim();
                        mBuilder.mIsShowing = false;
                    }
                }, new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        mBuilder.mIsShowing = true;
                    }
                });
                if (!isShow) {
                    mUi.closeTheLeastUsedTab();
                    callBackNewTabAnim();
                    mBuilder.mIsShowing = false;
                    return;
                }
                mBuilder.mIsShowing = true;
                return;
            }
        } else if (mClearAllTab == v) {
            clearAllTabs();
        } else if (mFullScreen == v) {
            if (!mUi.mNavScreenShowing) {
                callBackFullScreenAnim();
            }else {
                mBuilder.mIsShowing = true;
                return;
            }
        }
        mBuilder.mIsShowing = false;
    }

    static private int DELAYTIME = 50;
    public void callBackNewTabAnim(){
        //mApplication.setmThumbnailPosition(mBuilder.mCurrentConfigPosition);
        int endPosition = mBuilder.getList().size() - 1;
        Browser application = (Browser) mActivity.getApplication();
        int startPosition = application.getmThumbnailPosition();
        if (startPosition < 0 || startPosition >= mBuilder.getList().size())
            startPosition = 0;
        AnimatorSet moveAnim = null;
        int width = getWidth();
        int animTime = getResources().getInteger(R.integer.config_shortAnimTime);
        if (endPosition == startPosition) {
            mUi.openNewTab();
        } else {
            ObjectAnimator oTran;
            ArrayList<Animator> animList = new ArrayList<Animator>();
            for (int i = startPosition; i <= endPosition; i++) {
                if (i != endPosition) {
                    oTran = ObjectAnimator.ofFloat(mBuilder.getList()
                            .get(i), "translationX", 0,
                            -(i - startPosition + 1) * width);
                    oTran.setStartDelay(DELAYTIME * (i - startPosition));
                    oTran.setDuration(animTime * (i - startPosition + 1));
                } else {
                    oTran = ObjectAnimator.ofFloat(mBuilder.getList()
                            .get(i), "translationX", 0,
                            -(endPosition - startPosition) * width);
                    oTran.setStartDelay(DELAYTIME * (endPosition - startPosition));
                    oTran.setDuration(animTime * (endPosition - startPosition));
                }
                animList.add(oTran);
            }
            moveAnim = new AnimatorSet();
            moveAnim.playTogether(animList);
            moveAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mUi.openNewTab();
                    reverse(animation,0);
                    super.onAnimationEnd(animation);
                }
            });
            moveAnim.start();
        }
    }

    public void reverse(Animator animation, long time) {
        if(animation instanceof ObjectAnimator){
            animation.removeAllListeners();
            animation.setDuration(time);
            ((ObjectAnimator)animation).reverse();
        }else if(animation instanceof AnimatorSet){
            ArrayList<Animator> animations = ((AnimatorSet)animation).getChildAnimations();
            for(Animator animator: animations){
                reverse(animator, time);
            }
        }
    }

    public void callBackFullScreenAnim(){
        //mApplication.setmThumbnailPosition(mBuilder.mCurrentConfigPosition);
        int endPosition = mUiController.getTabControl()
                .getCurrentPosition() / 4;
        Browser application = (Browser) mActivity.getApplication();
        int startPosition = application.getmThumbnailPosition();
        AnimatorSet moveAnim = null;
        int animTime = getResources().getInteger(R.integer.config_shortAnimTime);
        if (endPosition >= mBuilder.getList().size())
            endPosition = mBuilder.getList().size() - 1;
        if (endPosition < 0)
            endPosition = 0;
        int width = getWidth();
        if (endPosition == startPosition) {
            mUi.hideNavScreen(
                    mUiController.getTabControl().getTabPosition(
                            mUi.getActiveTab()), true);
        } else if (endPosition > startPosition) {
            ObjectAnimator oTran;
            ArrayList<Animator> animList = new ArrayList<Animator>();
            for (int i = startPosition; i <= endPosition; i++) {
                if (i != endPosition) {
                    oTran = ObjectAnimator.ofFloat(mBuilder.getList()
                            .get(i), "translationX", 0,
                            -(i - startPosition + 1) * width);
                    oTran.setStartDelay(DELAYTIME * (i - startPosition));
                    oTran.setDuration(animTime * (i - startPosition + 1));
                } else {
                    oTran = ObjectAnimator.ofFloat(mBuilder.getList()
                            .get(i), "translationX", 0,
                            -(endPosition - startPosition) * width);
                    oTran.setStartDelay(DELAYTIME * (endPosition - startPosition));
                    oTran.setDuration(animTime * (endPosition - startPosition));
                }
                animList.add(oTran);
            }
            moveAnim = new AnimatorSet();
            moveAnim.playTogether(animList);
        } else {
            ObjectAnimator oTran;
            ArrayList<Animator> animList = new ArrayList<Animator>();
            for (int i = startPosition; i >= endPosition; i--) {
                if (i != endPosition) {
                    oTran = ObjectAnimator.ofFloat(mBuilder.getList()
                            .get(i), "translationX", 0,
                            (startPosition - i + 1) * width);
                    oTran.setStartDelay(DELAYTIME * (startPosition - i));
                    oTran.setDuration(animTime * (startPosition - i + 1));
                } else {
                    oTran = ObjectAnimator.ofFloat(mBuilder.getList()
                            .get(i), "translationX", 0,
                            (startPosition - endPosition) * width);
                    oTran.setStartDelay(DELAYTIME * (startPosition - endPosition));
                    oTran.setDuration(animTime * (startPosition - endPosition));
                }
                animList.add(oTran);
            }
            moveAnim = new AnimatorSet();
            moveAnim.playTogether(animList);
        }
        if (moveAnim != null) {
            moveAnim.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    // TODO Auto-generated method stub
                    mUi.hideNavScreen(
                            mUiController.getTabControl().getTabPosition(
                                    mUi.getActiveTab()), true, animation);
                    super.onAnimationEnd(animation);
                }

            });
            moveAnim.start();
        }
    }
    private void clearAllTabs() {
//        mDialog = new MenuDialog(mActivity);
//        mDialog.setTitle(R.string.is_close_all_tabs);
//        mDialog.setPositiveButton(R.string.clear_all_tabs,
//                new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mUi.closeAllTab();
//                    }
//                }
//        );
//        mDialog.setOnDismissListener(new OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface arg0) {
//                if (!mUi.isCloseTabAnimating()) {
//                    mBuilder.mIsShowing = true;
//                }
//            }
//        });
//        mDialog.show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mBuilder.mIsShowing) {
            try {
                return super.dispatchTouchEvent(ev);
            } catch(IllegalArgumentException e) {
                return false;
            }
        } else {
            return true;
        }
    }

    private void onCloseTab(Tab tab) {
        if (tab != null) {
            if (tab == mUiController.getCurrentTab()) {
                mUiController.closeCurrentTab();
            } else {
                mUiController.closeTab(tab);
            }
        }
    }

    protected void close(int position) {
        close(position, true);
    }

    protected void close(int position, boolean animate) {
        mUi.hideNavScreen(position, animate);
    }
    protected ThumbnailItems getThumView(int pos) {
        return mBuilder.getTabView(pos);
    }

    private void switchToTab(Tab tab) {
        if (tab != mUi.getActiveTab()) {
            mUiController.setActiveTab(tab);
        }
    }

    @Override
    public void onThumbnailUpdated(Tab t) {
        View v = mTabViews.get(t);
        if (v != null) {
            v.invalidate();
        }
    }

    public void finishBuilder(NavTabViewPagerBuilder builder) {
        this.removeView(builder);
    }
}
