/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.myapidemo.smartisan.browser.bookmarks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.HistoryAdapter.HistoryEditListener;
import com.android.myapidemo.smartisan.browser.platformsupport.Browser;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract.Combined;

//import smartisanos.app.MenuDialog;
/**
 * Activity for displaying the browser's history, divided into days of viewing.
 */
public class HistoryPageFragment extends Fragment implements
        LoaderCallbacks<Cursor>, OnClickListener, OnChildClickListener,HistoryEditListener {

    static final int LOADER_HISTORY = 1;
    static final int LOADER_MOST_VISITED = 2;

    CombinedBookmarksCallbacks mCallback;
    HistoryAdapter mAdapter;
    HistoryChildWrapper mChildWrapper;
    boolean mDisableNewWindow;
    String mMostVisitsLimit;
    ListView mGroupList, mChildList;

    //private MenuDialog mDialog;

    private String mTitle;
    private String mUrl;

    private TextView mClearAll;
    private TextView mActionTitle;
    private LinearLayout mOptionBar;
    private LinearLayout mBookmarkView;
    private ViewGroup mPrefsContainer;
    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private HistoryListView mHistoryList;

    private LayoutInflater mInflater;
    private ViewGroup mContentContainer;
    private ViewGroup mEmptyContainer;
    private View mRoot;

    public static interface HistoryQuery {
        static final String[] PROJECTION = new String[] {
                Combined._ID, // 0
                Combined.DATE_LAST_VISITED, // 1
                Combined.TITLE, // 2
                Combined.URL, // 3
                Combined.FAVICON, // 4
                Combined.VISITS, // 5
        };

        static final int INDEX_ID = 0;
        static final int INDEX_DATE_LAST_VISITED = 1;
        public static final int INDEX_TITE = 2;
        public static final int INDEX_URL = 3;
        static final int INDEX_FAVICON = 4;
        static final int INDEX_VISITS = 5;
        static final int INDEX_IS_BOOKMARK = 6;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri.Builder historyBuilder = BrowserContract.History.CONTENT_URI.buildUpon();
        switch (id) {
            case LOADER_HISTORY: {
                String sort = Combined.DATE_LAST_VISITED + " DESC";
                String where = Combined.VISITS + " > 0";
                CursorLoader loader = new CursorLoader(getActivity(), historyBuilder.build(),
                        HistoryQuery.PROJECTION, where, null, sort);
                return loader;
            }

            case LOADER_MOST_VISITED: {
                Uri uri = historyBuilder
                        .appendQueryParameter(BrowserContract.PARAM_LIMIT, mMostVisitsLimit)
                        .build();
                String where = Combined.VISITS + " > 0";
                CursorLoader loader = new CursorLoader(getActivity(), uri,
                        HistoryQuery.PROJECTION, where, null, Combined.VISITS + " DESC");
                return loader;
            }

            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    void selectGroup(int position) {
        mGroupItemClickListener.onItemClick(null,
                mAdapter.getGroupView(position, false, null, null), position,
                position);
    }

    void checkIfEmpty() {
        if (mAdapter.mMostVisited != null && mAdapter.mHistoryCursor != null) {
            // Both cursors have loaded - check to see if we have data
            if (mAdapter.isEmpty()) {
                HistoryListView listView = (HistoryListView) mRoot
                        .findViewById(R.id.history_list);
                listView.hideGroupLayout();
                listView.setVisibility(View.GONE);
                mContentContainer.setVisibility(View.VISIBLE);
            } else {
                mRoot.findViewById(R.id.history_list).setVisibility(
                        View.VISIBLE);
                mContentContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mAdapter == null) {
            return;
        }
        switch (loader.getId()) {
            case LOADER_HISTORY: {
                mAdapter.changeCursor(data);
                if (!mAdapter.isEmpty()
                        && mGroupList != null
                        && mGroupList.getCheckedItemPosition() == ListView.INVALID_POSITION) {
                    selectGroup(0);
                }
                checkIfEmpty();
                break;
            }

            case LOADER_MOST_VISITED: {
                mAdapter.changeMostVisitedCursor(data);

                checkIfEmpty();
                break;
            }

            default: {

                throw new IllegalArgumentException();
            }

        }

        int groupCount = mAdapter.getGroupCount();
        for (int i = 0; i < groupCount; i++) {
            mHistoryList.expandGroup(i);
        }

        // disable header click
        mHistoryList.setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                    int groupPosition, long id) {
                return true;
            }

        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setHasOptionsMenu(true);
        Bundle args = getArguments();
        mDisableNewWindow = args.getBoolean(
                BookmarksPageFragment.EXTRA_DISABLE_WINDOW, false);
        int mvlimit = getResources().getInteger(R.integer.most_visits_limit);
        mMostVisitsLimit = Integer.toString(mvlimit);
        mCallback = (CombinedBookmarksCallbacks) getActivity();
        mInflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View actionBar = inflater.inflate(R.layout.browser_settings_actionbar,
                null);
        mRoot = inflater.inflate(R.layout.history, container, false);
        mContentContainer = (ViewGroup) mRoot.findViewById(R.id.empty_layout);
        mEmptyContainer = (ViewGroup) inflater.inflate(R.layout.history_empty,
                mContentContainer, false);
        mContentContainer.addView(mEmptyContainer);
        mAdapter = new HistoryAdapter(getActivity());
        mAdapter.setHistoryEditListener(this);
        mOptionBar = (LinearLayout) mRoot.findViewById(R.id.bottom_option_bar);
        mClearAll = (TextView) mRoot.findViewById(R.id.clear_all);
        mClearAll.setOnClickListener(this);
        boolean hasHistory = Browser.canClearHistory(getActivity()
                .getContentResolver());
        if (hasHistory) {
            mClearAll.setEnabled(true);
        } else {
            mClearAll.setEnabled(false);
        }
        mActionTitle = (TextView) actionBar
                .findViewById(R.id.action_new_event_text);
        mActionTitle.setText(R.string.tab_history);
        mBookmarkView = (LinearLayout) actionBar
                .findViewById(R.id.action_cancel);
        mBookmarkView.setVisibility(View.VISIBLE);

/*        ViewStub stub = (ViewStub) mRoot.findViewById(R.id.pref_stub);
        if (stub != null) {
            inflateTwoPane(stub);
        } else {
            inflateSinglePane();
        }*/

        // Start the loaders
        getLoaderManager().restartLoader(LOADER_HISTORY, null, this);
        getLoaderManager().restartLoader(LOADER_MOST_VISITED, null, this);

        return mRoot;
    }

    private void inflateSinglePane() {
        mHistoryList = (HistoryListView) mRoot
                .findViewById(R.id.history_list);
        mHistoryList.setAdapter(mAdapter);
        mAdapter.mHSListView = mHistoryList;
        mAdapter.setCallback(mCallback);
        mHistoryList.setGroupIndicator(null);
        mHistoryList.setDivider(null);
        mHistoryList.setOnChildClickListener(this);
    }

    private void inflateTwoPane(ViewStub stub) {
        stub.setLayoutResource(R.layout.preference_list_content);
        stub.inflate();
        mGroupList = (ListView) mRoot.findViewById(android.R.id.list);
        mPrefsContainer = (ViewGroup) mRoot.findViewById(R.id.prefs_frame);
        mFragmentBreadCrumbs = (FragmentBreadCrumbs) mRoot
                .findViewById(android.R.id.title);
        mFragmentBreadCrumbs.setMaxVisible(1);
        mFragmentBreadCrumbs.setActivity(getActivity());
        mPrefsContainer.setVisibility(View.VISIBLE);
        mGroupList.setAdapter(new HistoryGroupWrapper(mAdapter));
        mGroupList.setOnItemClickListener(mGroupItemClickListener);
        mGroupList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mChildWrapper = new HistoryChildWrapper(mAdapter);
        mChildList = new ListView(getActivity());
        mChildList.setAdapter(mChildWrapper);
        mChildList.setOnItemClickListener(mChildItemClickListener);
        ViewGroup prefs = (ViewGroup) mRoot.findViewById(R.id.prefs);
        prefs.addView(mChildList);
    }

    private OnItemClickListener mGroupItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            CharSequence title = ((TextView) view).getText();
            mFragmentBreadCrumbs.setTitle(title, title);
            mChildWrapper.setSelectedGroup(position);
            mGroupList.setItemChecked(position, true);
        }
    };

    private OnItemClickListener mChildItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            mCallback.openUrl(((HistoryItemCoverView) view).getUrl());
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(LOADER_HISTORY);
        getLoaderManager().destroyLoader(LOADER_MOST_VISITED);
    }

    static class ClearHistoryTask extends Thread {
        ContentResolver mResolver;

        public ClearHistoryTask(ContentResolver resolver) {
            mResolver = resolver;
        }

        @Override
        public void run() {
            Browser.clearHistory(mResolver);
        }
    }

    View getTargetView(ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterContextMenuInfo) {
            return ((AdapterContextMenuInfo) menuInfo).targetView;
        }
        if (menuInfo instanceof ExpandableListContextMenuInfo) {
            return ((ExpandableListContextMenuInfo) menuInfo).targetView;
        }
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mEmptyContainer.removeAllViews();
        mInflater.inflate(R.layout.history_empty, mEmptyContainer, true);
    }

    private static abstract class HistoryWrapper extends BaseAdapter {

        protected HistoryAdapter mAdapter;
        private DataSetObserver mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
                notifyDataSetInvalidated();
            }
        };

        public HistoryWrapper(HistoryAdapter adapter) {
            mAdapter = adapter;
            mAdapter.registerDataSetObserver(mObserver);
        }

    }

    private static class HistoryGroupWrapper extends HistoryWrapper {

        public HistoryGroupWrapper(HistoryAdapter adapter) {
            super(adapter);
        }

        @Override
        public int getCount() {
            return mAdapter.getGroupCount();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mAdapter.getGroupView(position, false, convertView, parent);
        }
    }

    private static class HistoryChildWrapper extends HistoryWrapper {

        private int mSelectedGroup;

        public HistoryChildWrapper(HistoryAdapter adapter) {
            super(adapter);
        }

        void setSelectedGroup(int groupPosition) {
            mSelectedGroup = groupPosition;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mAdapter.getChildrenCount(mSelectedGroup);
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mAdapter.getChildView(mSelectedGroup, position, false,
                    convertView, parent);
        }

    }

    private void deleteHistory() {
        /*mDialog = new MenuDialog(getActivity());
        mDialog.setTitle(R.string.delete_dialog_confirm);
        mDialog.setPositiveButton(R.string.history_clear_all,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ContentResolver resolver = getActivity().getContentResolver();
                        final ClearHistoryTask clear = new ClearHistoryTask(resolver);
                        clear.start();
                    }
                });
        mDialog.show();*/
    }

    public boolean hasItemOpen(){
        return mHistoryList.hasOpenAndCloseIt();
    }

    @Override
    public void onClick(View view) {
        if (mClearAll == view) {
            boolean hasHistory = Browser.canClearHistory(getActivity()
                    .getContentResolver());
            if (hasHistory) {
                deleteHistory();
            }
        }
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
            int childPosition, long id) {
        HistoryItemCoverView view = (HistoryItemCoverView)v.findViewById(R.id.item_history);
        mCallback.openUrl(view.getUrl());
        return false;
    }

    @Override
    public void disableClearBtn() {
        mClearAll.setEnabled(false);
    }
}
