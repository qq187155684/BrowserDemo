
package com.android.myapidemo.smartisan.widget;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.AdapterView.OnItemClickListener;

import com.android.myapidemo.R;
import com.android.myapidemo.UI.ComboViews;
import com.android.myapidemo.smartisan.adapter.MenuPopupAdapter;
import com.android.myapidemo.smartisan.browse.BaseUi;
import com.android.myapidemo.smartisan.browse.BottomBarPhone;
import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.browse.DataController;
import com.android.myapidemo.smartisan.browse.Tab;
import com.android.myapidemo.smartisan.browse.UiController;
import com.android.myapidemo.smartisan.widget.BasePopupWindow.ListItem;

public class BrowserMenu {

    private Context mContext;
    private UiController mUiController;
    private BasePopupWindow mListPopup;
    private BaseUi mBaseUi;
    private ArrayList<ListItem> mMenuItems = new ArrayList<ListItem>();
    private int portraitOffY;

    public BrowserMenu(Context context, UiController controller, BaseUi ui) {
        mUiController = controller;
        mBaseUi = ui;
        mContext = context;
        portraitOffY = (int) mContext.getResources().getDimensionPixelSize(
                R.dimen.portrait_off_y);
        if (mMenuItems != null) {
            mMenuItems.clear();
        }
    }

    public void initMenuPopupWindow(View view, int x, int y) {
        boolean isShowHomePage = false;
        Tab currentTab = mUiController.getCurrentTab();
        if (currentTab != null && currentTab.isShowHomePage()) {
            isShowHomePage = true;
        }
        mListPopup = new MenuPopupWindow(mContext, view);
        MenuPopupAdapter menuPopupAdapter = new MenuPopupAdapter(mContext, mMenuItems);
        menuPopupAdapter.setBlankPage(isShowHomePage);
                //|| isBlankPage(mUiController.getCurrentWebView()));
        menuPopupAdapter.setIncogState(mUiController.getTabControl().getIncogMode());
        menuPopupAdapter.setRemusePageState(DataController.getInstance(mContext)
                .queryClosedUrlCount() > 0);
        boolean isBookmarkedSite = false;
        try {
            isBookmarkedSite = mUiController.getTabControl().getCurrentTab().isBookmarkedSite();
        } catch (Exception e) {
            e.printStackTrace();
        }
        menuPopupAdapter.setCanSaveBookmark(!isBookmarkedSite && !isShowHomePage);
        mListPopup.setAdapter(menuPopupAdapter);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mListPopup.setListViewParams(params);
            mListPopup.showAsDropDown(view, x, y);
        } else {
            mListPopup.setListViewParams(params);
            mListPopup.showAsDropDown(view, x, y + portraitOffY);
        }
        mListPopup.setOnItemClickListener(clickListener);
    }

    public void initMenuPopupItem() {
        Resources res = mContext.getResources();
        Drawable drawable = res.getDrawable(R.drawable.revert_icon);
        String text = res.getString(R.string.resume_closed_tab);
        mMenuItems.add(new ListItem(text, drawable));
        drawable = res.getDrawable(R.drawable.in_page_search_icon);
        text = res.getString(R.string.find_on_page);
        mMenuItems.add(new ListItem(text, drawable));
        if (mUiController.getTabControl().getIncogMode()) {
            drawable = res.getDrawable(R.drawable.quit_privacy_icon);
            text = res.getString(R.string.menu_exit_incog);
        } else {
            drawable = res.getDrawable(R.drawable.privacy_icon);
            text = res.getString(R.string.menu_to_incog);
        }
        mMenuItems.add(new ListItem(text, drawable));
        Tab tab = mUiController.getTabControl().getCurrentTab();
//        boolean b = BrowserSettings.getInstance().hasDesktopUseragent(
//                tab != null ? tab.getWebView() : null);
//        if (!b) {
//            drawable = res.getDrawable(R.drawable.desktop_icon);
//            text = res.getString(R.string.ua_switcher_desktop);
//        } else {
//            drawable = res.getDrawable(R.drawable.mobile);
//            text = res.getString(R.string.ua_switcher_mobile);
//        }
        mMenuItems.add(new ListItem(text, drawable));
        drawable = res.getDrawable(R.drawable.copy_share_icon);
        text = res.getString(R.string.copylink_and_share);
        mMenuItems.add(new ListItem(text, drawable));
        // if (mUiController.getUi().isCmccFeature()) {
            drawable = res.getDrawable(R.drawable.save_page_icon);
            text = res.getString(R.string.save_offline);
            mMenuItems.add(new ListItem(text, drawable));
            MenuPopupWindow.iTEM_SAVE_BOOKMARK = MenuPopupWindow.ITEM_SAVE_BOOKMARK + 1;
            MenuPopupWindow.iTEM_BOOKMARKS = MenuPopupWindow.ITEM_BOOKMARKS + 1;
            MenuPopupWindow.iTEM_HISTORY = MenuPopupWindow.ITEM_HISTORY + 1;
            MenuPopupWindow.iTEM_SETTING = MenuPopupWindow.ITEM_SETTING + 1;
        //}
        drawable = res.getDrawable(R.drawable.add_to_bookmarks);
        text = res.getString(R.string.save_to_bookmarks);
        mMenuItems.add(new ListItem(text, drawable));
        drawable = res.getDrawable(R.drawable.open_book_marks_icon);
        text = res.getString(R.string.bookmarks);
        mMenuItems.add(new ListItem(text, drawable));
        drawable = res.getDrawable(R.drawable.history_icon);
        text = res.getString(R.string.history);
        mMenuItems.add(new ListItem(text, drawable));
        drawable = res.getDrawable(R.drawable.setting_icon);
        text = res.getString(R.string.menu_preferences);
        mMenuItems.add(new ListItem(text, drawable));
    }

    private boolean isBlankPage(WebView webView) {
        if (webView != null) {
            String url = webView.getUrl();
            if (TextUtils.isEmpty(url)) {
                return true;
            }
        }
        return false;
    }

    public MenuPopupWindow getPopupWindow() {
        if (mListPopup == null) {
            return null;
        }
        return (MenuPopupWindow) mListPopup;
    }

    public void disMissPopup() {
        if (mListPopup != null) {
            mListPopup.dismiss();
        }
    }

    OnItemClickListener clickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            System.out.println("=================== onItemClick =====================");
            if (position == MenuPopupWindow.ITEM_REOPEN_CLOSED_TAB) {
                if (!mBaseUi.isNewTabAnimating()) {
                    Bundle state = DataController.getInstance(mContext).getLastestState();
                    if (state != null) {
                        Tab tab = mUiController.getTabControl().findTabWithUrl("", false);
                        if (tab != null) {
                            mUiController.switchToTab(tab);
                            tab.restoreBackForwardList(state);
                        }
                        else {
                            mBaseUi.NewTabAnim(BottomBarPhone.NEW_TAB_INNER_TAG, state); //
                        }
                        DataController.getInstance(mContext).clearState(state.getString("url", ""));
                    }
                }
            } else if (position == MenuPopupWindow.ITEM_FIND_IN_PAGE) {
                mUiController.findOnPage();
            } else if (position == MenuPopupWindow.ITEM_SWITCH_INCOG) {
                // If private tab's count is 0 then new a private tab to keep
                // the count
                // is 1.
                if (mUiController.getTabControl().getList(true).size() == 0) {
                    Tab tab = mUiController.getTabControl().createNewTab(true);
                    tab.setShowHomePage(true);
                    if (mUiController.getUi().isCmccFeature()) {
                        tab.loadUrl(mContext.getResources().getString(R.string.cmcc_homepage_url), null);
                    }
                }
                mBaseUi.hideSearchBar();
//                ((PhoneUi) mBaseUi).toggleNavScreen();
//                ((PhoneUi) mBaseUi).setPrivateAnim(true);
            } else if (position == MenuPopupWindow.ITEM_SWITCH_UA) {
                mUiController.toggleUserAgent();
            } else if (position == MenuPopupWindow.iTEM_SAVE_BOOKMARK) {
                mUiController.bookmarkCurrentPage();
            } else if (position == MenuPopupWindow.iTEM_BOOKMARKS) {
                mUiController.bookmarksOrHistoryPicker(ComboViews.Bookmarks);
            } else if (position == MenuPopupWindow.iTEM_HISTORY) {
                mUiController.bookmarksOrHistoryPicker(ComboViews.History);
            } else if (position == MenuPopupWindow.iTEM_SETTING) {
                mUiController.openPreferences();
            } else if (position == MenuPopupWindow.ITEM_COPY_AND_SHARE) {
                mUiController.shareCurrentPage();
            } else if (position == MenuPopupWindow.ITEM_SAVE_OFFLINE) {
                mBaseUi.saveOffline();
            }
            disMissPopup();
        }
    };
}
