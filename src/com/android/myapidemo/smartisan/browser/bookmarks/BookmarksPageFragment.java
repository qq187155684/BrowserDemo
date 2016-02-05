/*
 * Copyright (C) 2006 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

//import com.android.browser.AddBookmarkPage;
//import com.android.browser.BrowserSettings;
import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.BookmarksAdapter.FragmentEditListener;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract.Accounts;
import com.android.myapidemo.smartisan.browser.provider.BrowserProvider2;


//import smartisanos.app.MenuDialog;

interface BookmarksAccountPageCallbacks {
    // Return true if handled
    boolean onBookmarkSelected(Cursor c, boolean isFolder, boolean isEditstate);

    // Return true if handled
    boolean onOpenInNewWindow(String... urls);
}

/**
 * View showing the user's bookmarks in the browser.
 */
public class BookmarksPageFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnClickListener, FragmentEditListener,
        OnChangeFolderListener {

    interface onReloadFragmentListener {
        public void onReloadFragment(int type, long id, String title);

        public void setParentID(long parent);

        public long getParentID();

        public void setCurrentID(long parent);

        public void setAccountName(String name);

        public String getFolderName();

        public long getFolderID();

        public void changeActionBar(boolean isNormal);

    }

    private static final boolean DEBUG = false;
    private static final String LOGTAG = "BookmarksPageFragment";

    static final int LOADER_ACCOUNTS = 1;
    static final int LOADER_BOOKMARKS = 100;

    private static final int ANIM_TIME1 = 150;
    private static final int ANIM_TIME2 = 100;
    private static final int ANIM_TIME3 = 180;

    private static final int TEXT_START_POSITION = 286;

    public static final String EXTRA_DISABLE_WINDOW = "disable_new_window";
    static final String PREF_GROUP_STATE = "bbp_group_state";
    static final String CURRENT_FOLDER_NAME = "current_folder_name";

    public static final String ACCOUNT_TYPE = "account_type";
    public static final String ACCOUNT_NAME = "account_name";
    public static final String ACCOUNT = "account";
    public static final String NAME = "name";

    // public static final String FOLDER_TYPE = "folder";

    public static final int ACCOUNT_NAME_COLUMN_ID = 0;
    public static final int ACCOUNT_TYPE_COLUMN_ID = 1;
    public static final int ACCOUNT_ROOT_COLUMN_ID = 2;

    public static boolean mFirstDraw = true;
    public static boolean mRemoveState = true;

    public static String mWhereStr = null;

    //private MenuDialog mDialog;

    private String mTitle;
    private String mUrl;
    List<Integer> mList = new ArrayList<Integer>();

    BookmarksAccountPageCallbacks mCallbacks;
    onReloadFragmentListener mListener;
    View mRoot;
    BookmarkListView mListView;

    private BookmarksAdapter mLocalAdapter;
    BookmarksAdapter mAdapter;

    boolean mDisableNewWindow;
    boolean mEnableContextMenu = true;

    private boolean mIsEditState;
    private boolean mLastRemoveState;
    private Bundle mBundle;

    private LayoutInflater mInflater;
    private ViewGroup mContentContainer;
    private ViewGroup mEmptyContainer;

    View mEmptyView;
    View mHeader;

    private String mAccountName;

    private ImageView mSelectView;
    private TextView mEditBtn;
    private TextView mRemoveBtn;
    private TextView mCreatFolderBtn;
    private TextView mActionBarTitle;

    private TextView mLabel;

    private int mLoadID;
    private int mChildPosition;
    private long mRootID;
    private long mCurrentID;
    private long mParentID;

    private boolean mIsReload;

    private String mLoadName;

    HashMap<Integer, BookmarksAdapter> mBookmarkAdapters = new HashMap<Integer, BookmarksAdapter>();
    JSONObject mState;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // load account
        if (id == LOADER_ACCOUNTS) {
            setWhereStr();
            return new AccountsLoader(getActivity());
        } else if (id == LOADER_BOOKMARKS) {
            String accountType = args.getString(ACCOUNT_TYPE);
            String accountName = args.getString(ACCOUNT_NAME);
            BookmarksLoader bl = new BookmarksLoader(getActivity(),
                    accountType, accountName);
            long folderID = mBundle.getLong(ComboViewActivity.FRAGMENT_RELOAD_ID);
            if (folderID != 0) {
                Uri uri = ContentUris.withAppendedId(
                        BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, folderID);
                bl.setUri(uri);
            }
            return bl;
        } else {
            throw new UnsupportedOperationException("Unknown loader id " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            return;
        }
        if (loader.getId() == LOADER_ACCOUNTS) {
            LoaderManager lm = getLoaderManager();
            int id = LOADER_BOOKMARKS;
            // while (cursor.moveToNext()) {
            if (cursor.getCount() == 0) {
                Bundle args = new Bundle();
                args.putString(ACCOUNT_NAME, null);
                args.putString(ACCOUNT_TYPE, null);
                lm.restartLoader(id, args, this);
                BookmarksAdapter adapter = new BookmarksAdapter(
                        getActivity());
                mBookmarkAdapters.put(id, adapter);
                return;
            }
            cursor.moveToFirst();
            String accountName = cursor.getString(ACCOUNT_NAME_COLUMN_ID);
            String accountType = cursor.getString(ACCOUNT_TYPE_COLUMN_ID);
            mRootID = cursor.getInt(ACCOUNT_ROOT_COLUMN_ID);
            if (accountName == null) {
                mRootID = 0;
            }
            mCurrentID = mRootID;
            if (mAccountName == null) {
                mAccountName = accountName;
            }
            Bundle args = new Bundle();
            args.putString(ACCOUNT_NAME, accountName);
            args.putString(ACCOUNT_TYPE, accountType);
            BookmarksAdapter adapter = new BookmarksAdapter(
                    getActivity());
            mBookmarkAdapters.put(id, adapter);
            lm.restartLoader(id, args, this);
            // id++;//
            // }
            // TODO: Figure out what a reload of these means
            // Currently, a reload is triggered whenever bookmarks change
            // This is less than ideal
            // It also causes UI flickering as a new adapter is created
            // instead of re-using an existing one when the account_name is the
            // same.
            // For now, this is a one-shot load
            // setParentID();
            getLoaderManager().destroyLoader(LOADER_ACCOUNTS);
        } else if (loader.getId() == LOADER_BOOKMARKS) {
            if (mAdapter == null) {
                mAdapter = mBookmarkAdapters.get(loader.getId());
                if (mAdapter == null) {
                    return;
                }
                mListView.setAdapter(mAdapter);
                mAdapter.mHSListView = mListView;
                mAdapter.setStartFragmentListener(this);
            }
            setParentID();
            mAdapter.changeCursor(cursor);
            mLoadName = mAccountName;
            mLoadID = loader.getId();
            // if (mAdapter.getCount() == 0) {
            checkIfEmpty();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() >= LOADER_BOOKMARKS) {
            BookmarksAdapter adapter = mBookmarkAdapters.get(loader.getId());
            if (adapter != null) {
                adapter.changeCursor(null);
            }
        }
    }

    void checkIfEmpty() {
        // Both cursors have loaded - check to see if we have data
        if (mAdapter.isEmpty()) {
            mRoot.findViewById(R.id.grid).setVisibility(View.GONE);
            mContentContainer.setVisibility(View.VISIBLE);
        } else {
            mRoot.findViewById(R.id.grid).setVisibility(View.VISIBLE);
            mContentContainer.setVisibility(View.GONE);
        }
    }

    public void setParentID() {
        mListener.setAccountName(mAccountName);
        long folderID = mBundle.getLong(ComboViewActivity.FRAGMENT_RELOAD_ID);
        String where = BrowserContract.Bookmarks._ID +
                " = " + folderID;

        final Activity activity = getActivity();
        Cursor cursor1 = null;
        ContentResolver cr = activity.getContentResolver();
        cursor1 = cr.query(BrowserContract.Bookmarks.CONTENT_URI,
                new String[] {
                    BrowserContract.Bookmarks.PARENT,
                }, where, null, null);

        cursor1.moveToFirst();
        if (cursor1.getCount() != 0) {
            mParentID = cursor1.getLong(cursor1
                    .getColumnIndexOrThrow(BrowserContract.Bookmarks.PARENT));
            if (mParentID < mRootID) {
                mIsReload = true;
                mListener.setParentID(0);
            }
            else {
                mIsReload = true;
                mListener.setParentID(mParentID);
            }
        } else {
            mIsReload = false;
            mListener.setParentID(0);
        }
    }

    boolean canEdit(Cursor c) {
        int type = c.getInt(BookmarksLoader.COLUMN_INDEX_TYPE);
        return type == BrowserContract.Bookmarks.BOOKMARK_TYPE_BOOKMARK
                || type == BrowserContract.Bookmarks.BOOKMARK_TYPE_FOLDER;
    }

    /**
     * Create a new BrowserBookmarksPage.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
//        SharedPreferences prefs = BrowserSettings.getInstance().getPreferences();
//        try {
//            mState = new JSONObject(prefs.getString(PREF_GROUP_STATE, "{}"));
//        } catch (JSONException e) {
//            // Parse failed, clear preference and start with empty state
//            prefs.edit().remove(PREF_GROUP_STATE).apply();
//            mState = new JSONObject();
//        }
        Bundle args = getArguments();
        mDisableNewWindow = args == null ? false : args.getBoolean(EXTRA_DISABLE_WINDOW, false);
        setHasOptionsMenu(true);
        mBundle = args;
        // Listen folder data change

        ComboViewActivity activity = (ComboViewActivity) getActivity();
        activity.setFolderChanegListener(this);
        mInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (mCallbacks == null && getActivity() instanceof CombinedBookmarksCallbacks) {
            mCallbacks = new CombinedBookmarksCallbackWrapper(
                    (CombinedBookmarksCallbacks) getActivity());
        }
    }

    private static class CombinedBookmarksCallbackWrapper
            implements BookmarksAccountPageCallbacks {

        private CombinedBookmarksCallbacks mCombinedCallback;

        private CombinedBookmarksCallbackWrapper(CombinedBookmarksCallbacks cb) {
            mCombinedCallback = cb;
        }

        @Override
        public boolean onOpenInNewWindow(String... urls) {
            mCombinedCallback.openInNewTab(urls);
            return true;
        }

        @Override
        public boolean onBookmarkSelected(Cursor c, boolean isFolder, boolean isEditState) {
            if (isFolder) {
                return false;
            }
            if (!isEditState) {
                mCombinedCallback.openUrl(getUrl(c));
            }
            return true;
        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.bookmarks, container, false);
        mContentContainer = (ViewGroup) mRoot.findViewById(R.id.empty_layout);
        mEmptyContainer = (ViewGroup) mInflater.inflate(R.layout.bookmarks_empty,
                mContentContainer,
                false);
        mContentContainer.addView(mEmptyContainer);

        mEditBtn = (TextView) mRoot.findViewById(R.id.edit);
        mEditBtn.setText(R.string.bookmark_edit);
        mEditBtn.setOnClickListener(this);

        mRemoveBtn = (TextView) mRoot.findViewById(R.id.remove);
        mRemoveBtn.setText(R.string.remove);
        mRemoveBtn.setVisibility(View.GONE);
        mRemoveBtn.setOnClickListener(this);

        mCreatFolderBtn = (TextView) mRoot.findViewById(R.id.create_new_folder);
        mCreatFolderBtn.setText(R.string.new_folder);
        mCreatFolderBtn.setVisibility(View.GONE);
        mCreatFolderBtn.setOnClickListener(this);

        mListView = (BookmarkListView) mRoot.findViewById(R.id.grid);
        mListView.setSlideListener(mSlideListener);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                    itemClickEvent(position,id);
            }
        });
        // Start the loaders
        LoaderManager lm = getLoaderManager();
        lm.restartLoader(LOADER_ACCOUNTS, null, this);
        return mRoot;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LoaderManager lm = getLoaderManager();
        lm.destroyLoader(LOADER_ACCOUNTS);
        for (int id : mBookmarkAdapters.keySet()) {
            lm.destroyLoader(id);
        }
        mBookmarkAdapters.clear();
    }

    private void editChildBookmark(BookmarksAdapter adapter,int position) {
        if(mSlideListener == null){
            return;
        }
        boolean isCheck = mSlideListener.isChecked(position);
        if (isCheck) {
            mSlideListener.setChecked(position, false);
        } else {
            mSlideListener.setChecked(position, true);
        }
    }

    private void loadUrl(BookmarksAdapter adapter, int position) {
        if (mCallbacks != null && adapter != null) {
            mCallbacks.onBookmarkSelected((Cursor) adapter.getItem(position), false, mIsEditState);
        }
    }

    private void editBookmark(BookmarksAdapter adapter, int position,
            boolean isBottomAnimation) {
        if (adapter == null || position < 0) {
            return;
        }
        Cursor cursor = adapter.getCursor();
        if (!cursor.moveToFirst()) {
            return;
        }
        cursor = (Cursor) adapter.getItem(position);
        if (cursor.isAfterLast()) {
            Log.e(LOGTAG, "android.database.CursorIndexOutOfBoundsException: Index " + position
                    + " requested, with a size of " + cursor.getCount());
            return;
        }
        //Intent intent = new Intent(getActivity(), AddBookmarkPage.class);
        String folderName = mListener.getFolderName();
        Bundle item = new Bundle();
        item.putString(ACCOUNT_NAME, mLoadName);
        item.putString(CURRENT_FOLDER_NAME, folderName);
        item.putString(BrowserContract.Bookmarks.TITLE,
                cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE));
        item.putString(BrowserContract.Bookmarks.URL,
                cursor.getString(BookmarksLoader.COLUMN_INDEX_URL));
        byte[] data = cursor.getBlob(BookmarksLoader.COLUMN_INDEX_FAVICON);
        if (data != null) {
            item.putParcelable(BrowserContract.Bookmarks.FAVICON,
                    BitmapFactory.decodeByteArray(data, 0, data.length));
        }
        item.putLong(BrowserContract.Bookmarks._ID,
                cursor.getLong(BookmarksLoader.COLUMN_INDEX_ID));
        item.putLong(BrowserContract.Bookmarks.PARENT,
                cursor.getLong(BookmarksLoader.COLUMN_INDEX_PARENT));
        // folder data
//        item.putBoolean(AddBookmarkPage.EXTRA_IS_FOLDER,
//                cursor.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER) == 1);
//        item.putBoolean(AddBookmarkPage.EXTRA_IS_BOTTOM_ANIMATION, isBottomAnimation);
//        item.putBoolean(AddBookmarkPage.EXTRA_IS_EDIT_ING, true);
//        item.putBoolean(AddBookmarkPage.IS_BOOKMARK, true);
//        intent.putExtra(AddBookmarkPage.EXTRA_EDIT_BOOKMARK, item);
//        intent.putExtra(AddBookmarkPage.EXTRA_IS_FOLDER,
//                cursor.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER) == 1);
        //startActivity(intent);
    }

    private void displayRemoveBookmarkDialog(BookmarksAdapter adapter,
            int position) {
        // Put up a dialog asking if the user really wants to
        // delete the bookmark
        Cursor cursor = (Cursor) adapter.getItem(position);
        long id = cursor.getLong(BookmarksLoader.COLUMN_INDEX_ID);
        String title = cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE);
        Context context = getActivity();
        BookmarkUtils.displayRemoveBookmarkDialog(id, title, context, null);

    }

    private String getUrl(BookmarksAdapter adapter, int position) {
        return getUrl((Cursor) adapter.getItem(position));
    }

    /* package */static String getUrl(Cursor c) {
        return c.getString(BookmarksLoader.COLUMN_INDEX_URL);
    }

    private void copy(CharSequence text) {
        if (text == null) {
            return;
        }
        ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newRawUri(null, Uri.parse(text.toString())));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Resources res = getActivity().getResources();
        // mListView.setColumnWidthFromLayout(R.layout.bookmark_account_item);
        int paddingTop = (int) res.getDimension(R.dimen.combo_paddingTop);
        mRoot.setPadding(0, paddingTop, 0, 0);
        getActivity().invalidateOptionsMenu();
        mEmptyContainer.removeAllViews();
        mInflater.inflate(R.layout.bookmarks_empty, mEmptyContainer, true);
    }

    /**
     * @param uri
     */
    private void loadFolder(Uri uri) {
        LoaderManager manager = getLoaderManager();
        // This assumes groups are ordered the same as loaders
        BookmarksLoader loader = (BookmarksLoader) ((Loader<?>)
                manager.getLoader(mLoadID));
        loader.setAccountName(mLoadName);
        loader.setUri(uri);
        loader.forceLoad();
    }

    static class AccountsLoader extends CursorLoader {
        static String[] ACCOUNTS_PROJECTION = new String[] {
                Accounts.ACCOUNT_NAME,
                Accounts.ACCOUNT_TYPE,
                Accounts.ROOT_ID
        };

        public AccountsLoader(Context context) {
            super(context, Accounts.CONTENT_URI
                    .buildUpon()
                    .appendQueryParameter(BrowserProvider2.PARAM_ALLOW_EMPTY_ACCOUNTS, "false")
                    .build(),
                    ACCOUNTS_PROJECTION, mWhereStr, null, null);
        }
    }

    private void setWhereStr() {
        String name = mBundle.getString(ACCOUNT_NAME);
        if (name != null) {
            mWhereStr = BrowserContract.Accounts.ACCOUNT_NAME + " LIKE '" + name + "%'";
        } else {
            mWhereStr = BrowserContract.Accounts.ACCOUNT_NAME + " is NULL ";
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof onReloadFragmentListener)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mListener = (onReloadFragmentListener) activity;
    }

    @Override
    public void onClick(View view) {
        if (mAdapter == null) {
            return;
        }
        if (mEditBtn == view) {
            mFirstDraw = false;
            if (!mIsEditState) {
                openEditState();
            } else {
                cancelEditState();
            }
        } else if (mRemoveBtn == view) {
            mRemoveState = true;
            mAdapter.notifyDataSetChanged();
            mLastRemoveState = false;
            ArrayList<Long> tempList = mAdapter.getSelectItemList();
            for (int i = 0; i < tempList.size(); i++) {
                final long id = tempList.get(i);
                removeSelectBookmarks(id);
            }
            tempList.clear();
            hideButton(mCreatFolderBtn, ANIM_TIME1);
            hideButton(mRemoveBtn, ANIM_TIME1);
            mEditBtn.setText(R.string.bookmark_edit);
            mIsEditState = false;
            mListener.changeActionBar(false);
            cancelEditState();
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    mRemoveState = false;
                }
            };
            handler.postDelayed(runnable, 500);
        } else if (mCreatFolderBtn == view) {
            mFirstDraw = true;
            //Intent intent = new Intent(getActivity(), AddBookmarkPage.class);
            Bundle item = new Bundle();
            String folderName = mListener.getFolderName();
            long folderID = mListener.getParentID();
            if (folderID != 0) {
                item.putLong(BrowserContract.Bookmarks.PARENT,
                        folderID);
            }
            item.putString(ACCOUNT_NAME, mLoadName);
            item.putString(CURRENT_FOLDER_NAME, folderName);
//            item.putBoolean(AddBookmarkPage.EXTRA_IS_FOLDER, true);
//            item.putBoolean(AddBookmarkPage.EXTRA_IS_NEW_FOLDER, true);
//            intent.putExtra(AddBookmarkPage.EXTRA_EDIT_BOOKMARK, item);
            mAdapter.setEditCreateState(true);
            //startActivity(intent);
            getActivity().overridePendingTransition(R.anim.pop_up_in,
                    R.anim.activity_close_enter_in_call);
        }
    }

    @Override
    public void onChangAction(int position) {
        if (mAdapter == null) {
            return;
        }
        editBookmark(mAdapter, position, false);
        mAdapter.setEditCreateState(true);
    }

    public void removeSelectBookmarks(final long id) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Uri uri = ContentUris.withAppendedId(
                        BrowserContract.Bookmarks.CONTENT_URI,
                        id);
                getActivity().getContentResolver().delete(uri, null, null);
            }
        };
        new Thread(runnable).start();
    }

    @Override
    public void onChangeEditButton(boolean showRemoveBtn) {
        if (mLastRemoveState == showRemoveBtn) {
            return;
        }
        if (showRemoveBtn) {
            if (mRemoveBtn != null && mRemoveBtn.getVisibility() != View.VISIBLE) {
                showButton(mRemoveBtn, ANIM_TIME1);
                hideButton(mCreatFolderBtn, ANIM_TIME2);
            }
        } else {
            showButton(mCreatFolderBtn, ANIM_TIME1);
            hideButton(mRemoveBtn, ANIM_TIME2);
        }
        mLastRemoveState = showRemoveBtn;
    }

    @Override
    public boolean onChangeFolderAdapter() {
        if (mCurrentID != 0 && mRootID != mCurrentID) {
            Uri uri = ContentUris.withAppendedId(
                    BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, mRootID);
            loadFolder(uri);
            mCurrentID = mRootID;
            return false;
        }
        return true;
    }

    public int getCurrentType() {
        String fragmentName = mBundle.getString(ACCOUNT_NAME);
        if (fragmentName == null) {
            return ComboViewActivity.FRAGMENT_LOCAL_BOOKMARK;
        } else {
            return ComboViewActivity.FRAGMENT_ACCOUNT_BOOKMARK;
        }
    }

    @Override
    public boolean isReload() {
        return mIsReload;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRemoveState = false;
        mFirstDraw = true;
    }

    private void showButton(View v, int time) {
        final View view = v;
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0);
        view.animate().setDuration(time).setInterpolator(new DecelerateInterpolator())
                .setListener(null).alpha(1f);
    }

    private void hideButton(View v, int time) {
        final View view = v;
        view.animate().setDuration(time).setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }
                })
                .alpha(0);
    }

    public void openEditState() {
        if (mAdapter == null || mListener == null) {
            return;
        }
        mEditBtn.setText(R.string.cancel);
        showButton(mCreatFolderBtn, ANIM_TIME1);
        mIsEditState = true;
        mListener.changeActionBar(true);
        startEditMode();    
        doAnimation();
    }

    private void doAnimation() {
        List<AnimatorSet> animators = new ArrayList<AnimatorSet>();
        final int size = mListView.getChildCount();
        for (int i = 0; i < size; i++)
        {
            AnimatorSet animatorSet = new AnimatorSet();
            final BookmarkItemCoverView itemview = getBookmarkListItemView(mListView
                    .getChildAt(i));

            ObjectAnimator checkboxAnim = null;
            ObjectAnimator iconAnim = null;
            ObjectAnimator arrowAnim = null;
            ValueAnimator mTextAnim = null;

            if (mIsEditState) {
                checkboxAnim = ObjectAnimator.
                        ofFloat(itemview.mCheckBox, "translationX", -itemview.mCheckBox.getWidth(),
                                0);
                iconAnim = ObjectAnimator.
                        ofFloat(itemview.mIcon, "translationX", -itemview.mIcon.getWidth(), 0);
                arrowAnim = ObjectAnimator.
                        ofFloat(itemview.mArrow, "translationX", itemview.mArrow.getWidth(), 0);
                mTextAnim = ValueAnimator.ofInt(TEXT_START_POSITION - itemview.mIcon.getWidth(),
                        TEXT_START_POSITION);
                mTextAnim.addUpdateListener(new
                        ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator
                                    animation) {
                                itemview.mMoveX = (Integer) animation.getAnimatedValue();
                                itemview.invalidate();
                            }
                        });
            } else {
                checkboxAnim = ObjectAnimator.
                        ofFloat(itemview.mCheckBox, "translationX", 0,
                                -itemview.mCheckBox.getWidth());
                iconAnim = ObjectAnimator.
                        ofFloat(itemview.mIcon, "translationX", 0, -itemview.mIcon.getWidth());
                arrowAnim = ObjectAnimator.
                        ofFloat(itemview.mArrow, "translationX", 0, itemview.mArrow.getWidth());
                mTextAnim = ValueAnimator.ofInt(TEXT_START_POSITION, TEXT_START_POSITION
                        - itemview.mIcon.getWidth());
                mTextAnim.addUpdateListener(new
                        ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator
                                    animation) {
                                itemview.mMoveX = (Integer) animation.getAnimatedValue();
                                itemview.mX = TEXT_START_POSITION - itemview.mMoveX;
                                itemview.invalidate();
                            }
                        });
            }
            checkboxAnim.setInterpolator(new DecelerateInterpolator());
            iconAnim.setInterpolator(new DecelerateInterpolator());
            arrowAnim.setInterpolator(new DecelerateInterpolator());
            mTextAnim.setInterpolator(new DecelerateInterpolator());

            animatorSet.playTogether(checkboxAnim, iconAnim, arrowAnim, mTextAnim);
            animatorSet.setDuration(ANIM_TIME3);
            animatorSet.addListener(new AnimatorListener() {

                @Override
                public void onAnimationStart(Animator arg0) {
                    if (mAdapter != null) {
                        mAdapter.setEditCreateState(mIsEditState);
                    }
                    itemview.setDoingAnimatorState(true);
                }

                @Override
                public void onAnimationRepeat(Animator arg0) {

                }

                @Override
                public void onAnimationEnd(Animator arg0) {
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    itemview.setDoingAnimatorState(false);
                }

                @Override
                public void onAnimationCancel(Animator arg0) {
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetInvalidated();
                    }
                }
            });
            animators.add(animatorSet);
        }
        for (AnimatorSet as : animators) {
            as.start();
        }
    }

    @Override
    public void cancelEditState() {
        if (mAdapter == null || mListener == null) {
            return;
        }
        mAdapter.getSelectItemList().clear();
        //mEditView.setEditState(false);
        mLastRemoveState = false;
        mEditBtn.setText(R.string.bookmark_edit);
        hideButton(mCreatFolderBtn, ANIM_TIME1);
        hideButton(mRemoveBtn, ANIM_TIME1);
        mIsEditState = false;
        mListener.changeActionBar(false);
        endEditMode();
        doAnimation();
    }

    public void startEditMode() {
        if(mIsEditState){
            mListView.setSlideEnable(true);
            mListView.restoreScrollState(false);
            mListView.setScrollEnabled(false);
        }
        refreshUI();
    }

    public void endEditMode() {
        mIsEditState = false;
        mListView.setSlideEnable(false);
        mListView.setScrollEnabled(true);
        refreshUI();
    }

    private BookmarkListView.Listener mSlideListener = new BookmarkListView.Listener() {

        @Override
        public void setChecked(int position, boolean isChecked) {
            if (mAdapter.setChecked(position, isChecked)) {
                BookmarkItemCoverView item = getBookmarkListItemView(mListView
                        .getChildAt(position - mListView.getFirstVisiblePosition()));
                item.mCheckBox.setChecked(isChecked);
                refreshUI();
            }
        }

        @Override
        public boolean isChecked(int position) {
            return mAdapter.isChecked(position);
        }
    };

    private BookmarkItemCoverView getBookmarkListItemView(View root) {
        if (root instanceof BookmarkItemCoverView) {
            return (BookmarkItemCoverView) root;
        }
        return (BookmarkItemCoverView) root.findViewById(R.id.bookmark_cover);
    }

    @Override
    public void itemClickEvent(int position, long id) {
        if (mAdapter != null && mIsEditState) {
            editChildBookmark(mAdapter,position);
            return;
        }
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        boolean isFolder = cursor.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER) != 0;
        if (mCallbacks != null &&
                mCallbacks.onBookmarkSelected(cursor, isFolder, mIsEditState)) {
            return;
        }
        if (isFolder) {
            String title = cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE);
            Uri uri = ContentUris.withAppendedId(
                    BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER, id);
            mCurrentID = id;
            mListener.setCurrentID(mCurrentID);
            mParentID = cursor.getLong(cursor
                    .getColumnIndexOrThrow(BrowserContract.Bookmarks.PARENT));
            cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE);
            String fragmentName = mBundle.getString(ACCOUNT_NAME);
            if (fragmentName == null) {
                mListener
                        .onReloadFragment(ComboViewActivity.FRAGMENT_LOCAL_BOOKMARK, id, title);
            } else {
                mListener.onReloadFragment(ComboViewActivity.FRAGMENT_ACCOUNT_BOOKMARK, id,
                        title);
            }
            // stop load data
            loadFolder(uri);
        }
    }

    @Override
    public void editBookmark(int position) {
        editBookmark(mAdapter, position, true);
    }

    @Override
    public boolean getEditState() {
        return mIsEditState;
    }

    public boolean hasItemOpen(){
        return mListView.hasOpenAndCloseIt();
    }

    @Override
    public CombinedBookmarksCallbacks getActivityCallback() {
        return (CombinedBookmarksCallbacks) getActivity();
    }

    @Override
    public void setViewEnabled(boolean enable, float alpha) {
        if (mEditBtn != null) {
            mEditBtn.setEnabled(enable);
            mEditBtn.setAlpha(alpha);
        }
    }

    public void refreshUI() {
        if (mAdapter != null) {
            if (getEditState()) {
                boolean hasSelected = mAdapter.getSelectItemList().size() > 0;
                onChangeEditButton(hasSelected);
            } else {
                onChangeEditButton(false);
            }
        }
    }
}
