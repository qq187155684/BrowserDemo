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

package com.android.myapidemo;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebView;
import android.widget.ImageView;


import java.util.List;

import com.android.myapidemo.smartisan.browse.NavigationBarBase;
import com.android.myapidemo.smartisan.browse.Tab;
import com.android.myapidemo.smartisan.readmode.ReadModeHelper;

/**
 * UI interface definitions
 */
public interface UI {
    public static final int VIEW_TITLE_MASK = 0x01;
    public static final int VIEW_BOTTOM_MASK = 0x01 << 1;
    public static final int VIEW_ALL_MASK = VIEW_TITLE_MASK | VIEW_BOTTOM_MASK;

    public static enum ComboViews {
        History,
        Bookmarks,
        Snapshots,
    }

    public void onPause();

    public void onResume();

    public void onDestroy();

    public void onConfigurationChanged(Configuration config);

    public boolean onBackKey();

    public boolean onMenuKey();

    public boolean needsRestoreAllTabs();

    public void addTab(Tab tab);

    public void removeTab(Tab tab);

    public void setActiveTab(Tab tab);

    public void detachTab(Tab tab);

    public void attachTab(Tab tab, boolean setActive);

    public void onSetWebView(Tab tab, WebView view);

    public void createSubWindow(Tab tab, WebView subWebView);

    public void setReadViewHelper(ReadModeHelper readModeHelper);

    public boolean isReadModeWindowShowing();

    public boolean isReadModeWindowWillShowing();

    public void cancelshowReadModeWindow();

    public void dismissReadModeWindow();

    public void attachSubWindow(View subContainer);

    public void removeSubWindow(View subContainer);

    public void onTabDataChanged(Tab tab);

    public void onPageStopped(Tab tab);

    public void onProgressChanged(Tab tab);

    public void updateReadViewHelper(ReadModeHelper readModeHelper);

    public void showActiveTabsPage();

    public void removeActiveTabsPage();

    public void showComboView(ComboViews startingView, Bundle extra);

    public void showCustomView(View view, int requestedOrientation,
            CustomViewCallback callback);

    public void onHideCustomView();

    public boolean isCustomViewShowing();

    public boolean onPrepareOptionsMenu(Menu menu);

    public void updateMenuState(Tab tab, Menu menu);

    public void onOptionsMenuOpened();

    public void onExtendedMenuOpened();

    public boolean onOptionsItemSelected(MenuItem item);

    public void onOptionsMenuClosed(boolean inLoad);

    public void onExtendedMenuClosed(boolean inLoad);

    public void onContextMenuCreated(Menu menu);

    public void onContextMenuClosed(Menu menu, boolean inLoad);

    public void onActionModeStarted(ActionMode mode);

    public void onActionModeFinished(boolean inLoad);

    public void setShouldShowErrorConsole(Tab tab, boolean show);

    // returns if the web page is clear of any overlays (not including sub windows)
    public boolean isWebShowing();

    public void showWeb(boolean animate);

    Bitmap getDefaultVideoPoster();

    View getVideoLoadingProgressView();

    void bookmarkedStatusHasChanged(Tab tab);

    void editUrl(boolean clearInput, boolean forceIME);

    boolean isEditingUrl();

    boolean dispatchKey(int code, KeyEvent event);

    void showAutoLogin(Tab tab);

    void hideAutoLogin(Tab tab);

    void hideBar();

    public void showEditBarAnim();

    void setFullscreen(boolean enabled);
    void showFullscreen(boolean show);

    void translateTitleBar(float topControlsOffsetYPix);

    void translateReadModeTitleBar(float topControlsOffsetYPix);

    public boolean shouldCaptureThumbnails();

    boolean blockFocusAnimations();

    void onVoiceResult(String result);

    public Tab NewTabAnim(int tabTag, Bundle state);

    public void closeTheLeastUsedTab();

    public void OpenInBackGround(String url);

    public ImageView getPreBtn();

    public ImageView getForBtn();

    public void setBackForwardBtn();

    public void showTitleBottomBar(int mask);

    public void showTitleBottomBar(int mask, boolean isAnim);

    public void disMissTitleBottomBar(int mask);

    public void disMissTitleBottomBar(int mask, boolean isAnim);

    public boolean isBottomBarHide();

    public NavigationBarBase getBarBase();

    public void changeIncogMode(boolean isIncog);

    public void hideHomePage();

    public void showHomePage();

    public boolean isShowHomePage();

    public boolean isCmccFeature();

    public boolean isOpenHomePageFeature();

    public boolean isShowMaxTabsDialog(AlertDialog.OnClickListener okListener,
            AlertDialog.OnClickListener cancelListener);

    public void updateCheckPrompt();
}
