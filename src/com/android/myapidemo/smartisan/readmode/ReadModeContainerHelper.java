package com.android.myapidemo.smartisan.readmode;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.BaseUi;
import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.browse.TabControl;
import com.android.myapidemo.smartisan.browser.util.Constants;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.WebView;
import android.widget.*;

/**
 * Created by jude on 14-11-23.
 */
public class ReadModeContainerHelper {
    private BaseUi mBaseUi;
    private ReadModeHelper mReadModeHelper;
    private View mReadViewContainer;
    private FrameLayout mReadModeFrame;
    private WebView mReadView;
    private PopupWindow mReadModeSettingPopup;
    private int mReadModeFontSize = Constants.READMODE_MIDDLE_SIZE;
    private int mReadModeStyle = Constants.READMODE_STYLE_DAY;
    private LinearLayout mBgTitleBar;
    private ImageButton mBtnSetting;
    private TextView mTitle;
    private TextView mBtnDone;
    private TextView mBtnStyleDay;
    private TextView mBtnStyleNight;
    private TextView btnZoomOut;
    private TextView btnZoomIn;
    private Context mContext;
    private Resources mResources;
    private Animator mShowReadWindowAnimator;
    private Runnable mShowReadWindowRunnable;
    private boolean mIsReadModeWindowShowing = false;
    private boolean mIsReadModeWillShowing = false;//show readview runnable is delay 50 ms to run. in the 50 ms, mIsReadModeWillShowing is true, else false.
    private Handler mHandler = new Handler();

    public ReadModeContainerHelper(BaseUi baseUi) {
        mBaseUi = baseUi;
        mContext = mBaseUi.getActivity();
        mResources = mContext.getResources();
        init(mContext);
    }

    private void init(Context context) {
        mReadModeFrame = (FrameLayout) mBaseUi.mActivity.getWindow().getDecorView().findViewById(R.id.readmode_frame);
        LayoutInflater inflater = LayoutInflater.from(context);
        mReadViewContainer = inflater.inflate(R.layout.readmode_header, null);
        mReadModeFrame.addView(mReadViewContainer, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
        mBgTitleBar = (LinearLayout) mReadViewContainer
          .findViewById(R.id.titlebar);
        mTitle = (TextView) mReadViewContainer
          .findViewById(R.id.title);
        mBtnDone = (TextView) mReadViewContainer
                .findViewById(R.id.btn_done);
        mBtnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBaseUi.dismissReadModeWindow();
            }
        });
        mBtnSetting = (ImageButton) mReadViewContainer
                .findViewById(R.id.btn_setting);
        mBtnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReadModeSettingPopup == null) {
                    initSettingPopwindow(mBtnSetting);
                } else {
                    if (mReadModeSettingPopup.isShowing())
                        mReadModeSettingPopup.dismiss();
                    else
                        mReadModeSettingPopup
                                .showAsDropDown(mBtnSetting,
                                        Constants.READMODE_SETTING_POPUP_OFFSET_X,
                                        Constants.READMODE_SETTING_POPUP_OFFSET_Y);
                }
            }
        });

        mReadModeFontSize = BrowserSettings.getInstance().getReadFontSize();
        mReadModeStyle = BrowserSettings.getInstance().getReadStyle();
        updateBtns();
    }

    public void onPause() {
        if (mReadModeSettingPopup != null && mReadModeSettingPopup.isShowing()) {
            mReadModeSettingPopup.dismiss();
        }
        mBaseUi.translateReadModeTitleBar(0);
    }

    public View getTitleBar() {
        return mReadViewContainer;
    }

    private void initSettingPopwindow(View view) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View conentView = inflater.inflate(R.layout.readmode_setting_window, null);

        mBtnStyleDay = (TextView) conentView.findViewById(R.id.btn_style_day);
        mBtnStyleDay.setOnClickListener(new onReadModeSettingClickListener());
        mBtnStyleDay.setBackgroundResource(R.drawable.btn_rm_style_day_checked_selector);
        mBtnStyleNight = (TextView) conentView.findViewById(R.id.btn_style_night);
        mBtnStyleNight.setOnClickListener(new onReadModeSettingClickListener());
        btnZoomOut = (TextView) conentView.findViewById(R.id.btn_zoomout);
        btnZoomOut.setOnClickListener(new onReadModeSettingClickListener());
        btnZoomIn = (TextView) conentView.findViewById(R.id.btn_zoomin);
        btnZoomIn.setOnClickListener(new onReadModeSettingClickListener());

        updateZoomButton();
        updateBtns();

        mReadModeSettingPopup = new PopupWindow(conentView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mReadModeSettingPopup.setBackgroundDrawable(new BitmapDrawable());
        mReadModeSettingPopup.setFocusable(true);
        mReadModeSettingPopup.setOutsideTouchable(true);
        mReadModeSettingPopup.showAsDropDown(view, Constants.READMODE_SETTING_POPUP_OFFSET_X,
                Constants.READMODE_SETTING_POPUP_OFFSET_Y);
    }

    private void updateZoomButton() {
        if (mReadModeFontSize == Constants.READMODE_MAX_SIZE) {
            btnZoomOut.setBackgroundResource(R.drawable.btn_rm_font_zoomout_selector);
            btnZoomOut.setClickable(true);
            btnZoomIn.setBackgroundResource(R.drawable.btn_readmode_diszoomin);
            btnZoomIn.setClickable(false);
        } else if (mReadModeFontSize == Constants.READMODE_MIN_SIZE) {
            btnZoomOut.setBackgroundResource(R.drawable.btn_readmode_diszoomout);
            btnZoomOut.setClickable(false);
            btnZoomIn.setBackgroundResource(R.drawable.btn_rm_font_zoomin_selector);
            btnZoomIn.setClickable(true);
        } else {
            btnZoomOut.setBackgroundResource(R.drawable.btn_rm_font_zoomout_selector);
            btnZoomOut.setClickable(true);
            btnZoomIn.setBackgroundResource(R.drawable.btn_rm_font_zoomin_selector);
            btnZoomIn.setClickable(true);
        }
    }

    private class onReadModeSettingClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_style_day:
                    if (mReadModeStyle != Constants.READMODE_STYLE_DAY) {
                        mReadModeStyle = Constants.READMODE_STYLE_DAY;
                        updateBtns();
                    }
                    break;
                case R.id.btn_style_night:
                    if (mReadModeStyle != Constants.READMODE_STYLE_NIGHT) {
                        mReadModeStyle = Constants.READMODE_STYLE_NIGHT;
                        updateBtns();
                    }
                    break;
                case R.id.btn_zoomout:
                    if (mReadModeFontSize > Constants.READMODE_MIN_SIZE) {
                        mReadModeFontSize -= 1;
                    }
                    break;
                case R.id.btn_zoomin:
                    if (mReadModeFontSize < Constants.READMODE_MAX_SIZE) {
                        mReadModeFontSize += 1;
                    }
                    break;
            }
            BrowserSettings.getInstance().setReadFontSize(mReadModeFontSize);
            BrowserSettings.getInstance().setReadStyle(mReadModeStyle);
            updateZoomButton();
            mReadModeHelper.updateStyle();
        }
    }

    public void updateReadViewHelper(ReadModeHelper readModeHelper) {
//        if (readModeHelper != null && readModeHelper.getReadView() != null) {
//            mReadModeHelper = readModeHelper;
//            mReadView = readModeHelper.getReadView();
//            mReadModeHelper.updateStyle();
//        }
    }

    private void updateBtns() {
        if (mReadModeStyle == Constants.READMODE_STYLE_DAY) {
            mBgTitleBar.setBackgroundResource(R.drawable.title_bg);
            mTitle.setTextColor(mResources.getColor(R.color.browser_actionbar_title));
            mBtnDone.setBackgroundResource(R.drawable.btn_title_selector);
            mBtnDone.setTextColor(mResources.getColor(R.color.browser_history_item_back_color));
            mBtnSetting.setBackgroundResource(R.drawable.btn_title_selector);
            mBtnSetting.setImageResource(R.drawable.icon_readmode_setting);
            if (mBtnStyleNight != null) {
                mBtnStyleDay.setBackgroundResource(R.drawable.btn_rm_style_day_checked_selector);
                mBtnStyleNight.setBackgroundResource(R.drawable.btn_rm_style_night_selector);
            }
        } else {
            mBgTitleBar.setBackgroundResource(R.drawable.title_bg_dark);
            mTitle.setTextColor(mResources.getColor(R.color.readmode_title_text_light_color));
            mBtnDone.setBackgroundResource(R.drawable.btn_title_dark_selector);
            mBtnDone.setTextColor(mResources.getColor(R.color.readmode_title_text_light_color));
            mBtnSetting.setBackgroundResource(R.drawable.btn_title_dark_selector);
            mBtnSetting.setImageResource(R.drawable.btn_setting_dark);
            if (mBtnStyleNight != null) {
                mBtnStyleNight.setBackgroundResource(R.drawable.btn_rm_style_night_checked_selector);
                mBtnStyleDay.setBackgroundResource(R.drawable.btn_rm_style_day_selector);
            }
        }
    }

    public boolean isReadModeWindowShowing() {
        return mIsReadModeWindowShowing;
    }

    public boolean isReadModeWindowWillShowing() {
        return mIsReadModeWillShowing;
    }

    public void showReadModeWindow() {
        mReadModeFrame.setVisibility(View.VISIBLE);
        if (mReadView.getParent() == null) {
            mReadModeFrame.addView(mReadView, 0);
        }
        mBaseUi.getUiController().getCurrentTopWebView().requestFocus();
        mReadView.setVisibility(View.VISIBLE);
        mReadView.requestFocus();
        mReadView.onPause();
        mReadView.onResume();
        mIsReadModeWillShowing = true;
        mShowReadWindowRunnable = new Runnable() {
            @Override
            public void run() {
                PropertyValuesHolder pvhTranslateY = PropertyValuesHolder
                        .ofFloat("y", mReadModeFrame.getHeight(), 0);
                mShowReadWindowAnimator = ObjectAnimator
                        .ofPropertyValuesHolder(
                                mReadModeFrame,
                                pvhTranslateY);
                mShowReadWindowAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mBaseUi.detachActiveTab();
                    }
                });
                mShowReadWindowAnimator.setDuration(Constants.FLING_DURATION);
                mShowReadWindowAnimator
                        .setInterpolator(new AccelerateDecelerateInterpolator());
                mShowReadWindowAnimator.start();
                mIsReadModeWindowShowing = true;
                mIsReadModeWillShowing = false;
            }
        };
        mHandler.postDelayed(mShowReadWindowRunnable, 50);
    }

    public void cancelshowReadModeWindow() {
        mHandler.removeCallbacks(mShowReadWindowRunnable);
        dismissReadModeWindowWithoutAnimator();
    }

    public void dismissReadModeWindow() {
        if (mReadModeSettingPopup != null)
            mReadModeSettingPopup.dismiss();

        mIsReadModeWindowShowing = false;
        if (mReadView == null)
            return;
        PropertyValuesHolder pvhTranslateY = PropertyValuesHolder
                .ofFloat("y", mReadModeFrame.getY(), Constants.SCREEN_HEIGHT);
        Animator translateAnimator = ObjectAnimator
                .ofPropertyValuesHolder(mReadModeFrame, pvhTranslateY);
        translateAnimator.setDuration(Constants.FLING_DURATION);
        translateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mBaseUi.translateReadModeTitleBar(0);
                mReadView.onPause();
                mReadView.setVisibility(View.GONE);
                mReadModeFrame.removeView(mReadView);
            }
        });
        if (mShowReadWindowAnimator != null && mShowReadWindowAnimator.isRunning())
            mShowReadWindowAnimator.cancel();
        translateAnimator.start();
    }

    private void dismissReadModeWindowWithoutAnimator() {
        if (mReadModeSettingPopup != null)
            mReadModeSettingPopup.dismiss();

        mIsReadModeWindowShowing = false;
        mBaseUi.translateReadModeTitleBar(0);
        mReadModeFrame.setY(Constants.SCREEN_HEIGHT);
        mReadView.onPause();
        mReadView.setVisibility(View.GONE);
        mReadModeFrame.removeView(mReadView);
    }

    public void onConfigurationChanged(final TabControl tabControl) {
        if (mReadModeSettingPopup != null)
            mReadModeSettingPopup.dismiss();
    }
}
