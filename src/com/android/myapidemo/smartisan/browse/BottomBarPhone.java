/**
 *
 */

package com.android.myapidemo.smartisan.browse;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.adapter.MenuPopupAdapter;
import com.android.myapidemo.smartisan.widget.BasePopupWindow;
import com.android.myapidemo.smartisan.widget.BrowserMenu;
import com.android.myapidemo.smartisan.widget.MenuPopupWindow;
import com.android.myapidemo.smartisan.widget.BasePopupWindow.ListItem;

/**
 * @author qijin
 */
public class BottomBarPhone extends BottomBarBase {

    private UiController mUiController;
    private BaseUi mBaseUi;

    private BasePopupWindow mListPopup;
    private Context mContext;

    private ImageView mPreBtn;
    private ImageView mNextBtn;
    private ImageView mSwitchTabBtn;
    private ImageView mAddTabBtn;
    private ImageView mMenuBtn;

    public static final int NEW_TAB_ING_TAG = 0;
    public static final int NEW_TAB_TAG = 1;
    public static final int NEW_TAB_INNER_TAG = 2;

    private ArrayList<ListItem> mMenuItems = new ArrayList<ListItem>();
    private ArrayList<ListItem> mTabItems = new ArrayList<ListItem>();
    private int portraitOffY = (int) getContext().getResources().getDimensionPixelSize(
            R.dimen.portrait_off_y);

    /**
     * @param context
     * @param controller
     * @param ui
     * @param contentView
     */
    public BottomBarPhone(Context context, UiController controller, BaseUi ui,
            FrameLayout contentView) {
        super(context, controller, ui, contentView);
        mUiController = controller;
        mBaseUi = ui;
        mContext = context;
        initLayout(context);
    }

    public ImageView getPreBtn() {
        return mPreBtn;
    }

    public ImageView getNextBtn() {
        return mNextBtn;
    }

    public ImageView getSwitchTabBtn() {
        return mSwitchTabBtn;
    }

    private void initLayout(Context context) {
        setBackgroundResource(R.drawable.bottom_bar);
        setWeightSum(5);
        setOrientation(LinearLayout.HORIZONTAL);
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.bottom_bar, this);
        mPreBtn = (ImageView) findViewById(R.id.previous_btn);
        mPreBtn.setOnClickListener(this);
        mNextBtn = (ImageView) findViewById(R.id.next_btn);
        mNextBtn.setOnClickListener(this);
        mSwitchTabBtn = (ImageView) findViewById(R.id.switch_btn);
        mSwitchTabBtn.setOnClickListener(this);
        mAddTabBtn = (ImageView) findViewById(R.id.newtab_btn);
        mAddTabBtn.setOnClickListener(this);
        mBaseUi.setBackForwardBtn();
        mMenuBtn = (ImageView) findViewById(R.id.menu_btn);
        mMenuBtn.setOnClickListener(this);
        changeVisible(mContext.getResources().getConfiguration());
    }

    private void changeVisible(Configuration config){
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        changeVisible(config);
        InputMethodManager mInput = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mInput.isActive()) {
            disMissPopup();
        }
        //Activity not call onConfigurationChanged if click home button when orientation is changed
        PhoneUi phoneUi = ((PhoneUi) mBaseUi);
        NavScreen navScreen = phoneUi.getNavScreen();
        if(navScreen != null && navScreen.getParent() == null){
            navScreen.onConfigurationChanged(config);
        }
    }

    @Override
    public void onClick(View v) {
        PhoneUi phoneUi = ((PhoneUi) mBaseUi);
        // not take action if showing nav screen
        if (phoneUi.showingNavScreen())
            return;

        if (mPreBtn == v) {
            Tab tab = mUiController.getTabControl().getCurrentTab();
            if (tab != null) {
                tab.goBack();
            }
        } else if (mNextBtn == v) {
            Tab tab = mUiController.getTabControl().getCurrentTab();
            if (tab != null) {
                tab.goForward();
            }
        } else if (mSwitchTabBtn == v) {
            phoneUi.toggleNavScreen();
            phoneUi.setPrivateAnim(false);
            mSwitchTabBtn.setClickable(false);
        } else if (mAddTabBtn == v) {
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
        } else if (mMenuBtn == v) {
            if (((NavigationBarPhone)(mBaseUi.getBarBase())).isPending())
                return; // not show menu in pending state

            if (mMenuItems != null) {
                mMenuItems.clear();
            }
            BrowserMenu menu = new BrowserMenu(mContext, mUiController, mBaseUi);
            menu.initMenuPopupItem();
            int xOff = getResources().getInteger(R.integer.popup_window_x_off);
            int yOff = getResources().getInteger(R.integer.popup_window_y_off);
            menu.initMenuPopupWindow(mMenuBtn, xOff, yOff);
            mListPopup = menu.getPopupWindow();
            MenuPopupWindow menupop = (MenuPopupWindow)mListPopup;
            menupop.setLandState(false);
        }
    }

    private void startNewTabAnim(){
        if (mUiController.getTabControl().getIncogMode()) {
            mBaseUi.NewTabAnim(NEW_TAB_ING_TAG, null);
        } else {
            mBaseUi.NewTabAnim(NEW_TAB_TAG, null);
        }
    }

    private boolean isBlankPage(WebView webView){
        if (webView != null) {
            String url = webView.getUrl();
            if (TextUtils.isEmpty(url)|| url.matches(UrlInputView.MATCHES)) {
                return true;
            }
        }
        return false;
    }

    public BasePopupWindow getListPopup(){
        return mListPopup;
    }

    public void changeIncog(boolean isIncog){
        if (isIncog) {
            setBackgroundResource(R.drawable.bottom_bar_private);
            mPreBtn.setImageResource(R.drawable.bottombar_previous_page_incog_selector);
            mNextBtn.setImageResource(R.drawable.bottombar_next_page_incog_selector);
            mSwitchTabBtn.setImageResource(R.drawable.bottombar_switch_tab_incog_selector);
            mAddTabBtn.setImageResource(R.drawable.bottombar_add_tab_incog_selector);
            mMenuBtn.setImageResource(R.drawable.bottombar_menu_incog_selector);
        } else {
            setBackgroundResource(R.drawable.bottom_bar);
            mPreBtn.setImageResource(R.drawable.bottombar_previous_page_selector);
            mNextBtn.setImageResource(R.drawable.bottombar_next_page_selector);
            mSwitchTabBtn.setImageResource(R.drawable.bottombar_switch_tab_selector);
            mAddTabBtn.setImageResource(R.drawable.bottombar_add_tab_selector);
            mMenuBtn.setImageResource(R.drawable.bottombar_menu_selector);
        }
    }

    public void initTabPopupItem() {
        Resources res = mContext.getResources();
        Drawable drawable = null;
        String text = res.getString(R.string.new_tab);
        mTabItems.add(new ListItem(text, drawable));
        text = res.getString(R.string.new_incognito_tab);
        mTabItems.add(new ListItem(text, drawable));
    }

    @Override
    public void onPause() {
        super.onPause();
        disMissPopup();
    }

    public void disMissPopup() {
        if (mListPopup != null) {
            mListPopup.dismiss();
        }
    }
}
