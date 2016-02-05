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

package com.android.myapidemo.smartisan.browse;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract.Accounts;
import com.android.myapidemo.smartisan.browser.platformsupport.WebAddress;
import com.android.myapidemo.smartisan.browser.provider.BrowserProvider2;
import com.android.myapidemo.smartisan.browser.util.UrlUtils;
import com.android.myapidemo.smartisan.browser.bookmarks.BookmarkUtils;
import com.android.myapidemo.smartisan.browser.bookmarks.Bookmarks;

public class AddBookmarkPage extends Activity
        implements View.OnClickListener, TextView.OnEditorActionListener,
        EditBookmark {
    public static final long DEFAULT_FOLDER_ID = -1;
    public static final String TOUCH_ICON_URL = "touch_icon_url";

    // Place on an edited bookmark to remove the saved thumbnail
    public static final String REMOVE_THUMBNAIL = "remove_thumbnail";
    public static final String USER_AGENT = "user_agent";
    public static final String CHECK_FOR_DUPE = "check_for_dupe";

    // Fragment tag
    public static final String FRAGMENT_EDIT_TAG = "edit";
    public static final String FRAGMENT_FOLDER_TAG = "folder";

    public static final String EXTRA_EDIT_BOOKMARK = "bookmark";
    public static final String EXTRA_IS_FOLDER = "is_folder";
    public static final String EXTRA_IS_NEW_FOLDER = "is_new_folder";
    public static final String EXTRA_IS_EDIT_ING = "is_edit";
    public static final String EXTRA_IS_BOTTOM_ANIMATION = "is_bottom_animation";
    public static final String IS_BOOKMARK = "is_bookmark";

    private final String LOGTAG = "Bookmarks";

    // fragment type
    public static final int FRAGMENT_EDIT_PAGE = 0;
    public static final int FRAGMENT_FOLDER_PAGE = 1;

    private boolean mEditingExisting;
    private boolean mEditingFolder;
    private boolean mInsertData;
    private boolean mIsBookmark;
    private boolean mIsBottomAnimation;

    private Bundle mMap;
    private String mTouchIconUrl;
    private String mOriginalUrl;
    private String mLastActionBarName;
    private String mLastBookmarkTitle;
    private String mLastBookmakrUrl;
    private String mSelectFolderName;

    private EditText mTitleEdit;
    private EditText mUrlEdit;
    private EditText mFolderNamer;

    private View mCancelActionView;
    private View mDoneActionView;
    private long mCurrentFolder;

    // private BreadCrumbView mCrumbs;
    private boolean mSaveToHomeScreen;

    // action bar relate
    private TextView mActionBarTitle;
    private TextView mCancelBtn;
    private TextView mDoneBtn;

    private boolean mCrateOrEditFolder;
    private boolean isOnCreate;
    private boolean isNewFolder;

    private long mFolderID = -1;
    private int mCurrentFragment = -1;

    public interface OnSaveListener
    {
        public void saveError();
    }

    // Message IDs
    private static final int SAVE_BOOKMARK = 100;
    private static final int SAVE_FOLDER = 101;
    private static final int TOUCH_ICON_DOWNLOADED = 102;
    private static final int BOOKMARK_DELETED = 103;

    private Handler mHandler;

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mFolderNamer) {
            if (v.getText().length() > 0) {
                if (actionId == EditorInfo.IME_NULL) {
                    // Only want to do this once.
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        completeOrCancelFolderNaming(false);
                    }
                }
            }
            // Steal the key press; otherwise a newline will be added
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (mCancelActionView == v) { // back button
            if (mCurrentFragment == FRAGMENT_FOLDER_PAGE) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                AddBookmarkFolder fregment = (AddBookmarkFolder) getFragmentManager()
                        .findFragmentByTag(FRAGMENT_FOLDER_TAG);
                ft.hide(fregment);
                isOnCreate = false;
                startFragment(FRAGMENT_EDIT_PAGE);
            } else {
                onBackPressed();
            }
        } else if (v == mDoneActionView) {
            if (mEditingFolder) {
                completeOrCancelFolderNaming(false);
            } else if (save()) {
                finish();
                if (mIsBottomAnimation) {
                    overridePendingTransition(R.anim.activity_slide_do_nothing,
                            R.anim.slide_down_out);
                }
            }

        }
    }

    /**
     * Finish naming a folder, and close the IME
     * 
     * @param cancel If true, the new folder is not created. If false, the new
     *            folder is created and the user is taken inside it.
     */
    private void completeOrCancelFolderNaming(boolean cancel) {
        String title = mTitleEdit.getText().toString().trim();
        if (!cancel && !TextUtils.isEmpty(title)) {
            if (mCrateOrEditFolder) {
                // id = addFolderToCurrent(title, mFolderID);
                // if (id != 0) {
                addFolderToCurrent(title, mFolderID);
                finish();
                overridePendingTransition(R.anim.activity_slide_do_nothing,
                        R.anim.slide_down_out);

            } else {
                saveFolderToTarget(title);
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.folder_name_not_null,
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private long addFolderToCurrent(String title, long folderID) {
        // Add the folder to the database
        mInsertData = true;
        // ContentValues values = new ContentValues();
        // values.put(BrowserContract.Bookmarks.TITLE,
        // name);
        // values.put(BrowserContract.Bookmarks.IS_FOLDER, 1);
        // values.put(BrowserContract.Bookmarks.PARENT, currentFolder);
        long currentFolder;
        if (folderID != -1) {
            currentFolder = folderID;
        } else {
            long id = mMap.getLong(BrowserContract.Bookmarks.PARENT, 1);
            currentFolder = id;
        }

        createHandler();
        Bundle bundle = new Bundle();
        bundle.putInt(BrowserContract.Bookmarks.IS_FOLDER, 1);
        bundle.putLong(BrowserContract.Bookmarks.PARENT, currentFolder);
        bundle.putString(BrowserContract.Bookmarks.TITLE, title);

        // bundle.putString(BrowserContract.Bookmarks.URL, url);
        // bundle.putParcelable(BrowserContract.Bookmarks.FAVICON, favicon);

        // Uri uri = getContentResolver().insert(
        // BrowserContract.Bookmarks.CONTENT_URI, values);
        Message msg = Message.obtain(mHandler, SAVE_FOLDER);
        msg.setData(bundle);
        // Start a new thread so as to not slow down the UI
        Thread t = new Thread(new SaveFolderRunnable(getApplicationContext(), msg));
        t.start();

        /*
         * if (uri != null) { mInsertData = true; return
         * ContentUris.parseId(uri); } else {
         */
        return -1;
        // }
    }

    private void saveFolderToTarget(String title) {
        mInsertData = true;
        ContentValues values = new ContentValues();
        long id = mMap.getLong(BrowserContract.Bookmarks._ID);
        values.put(BrowserContract.Bookmarks.PARENT, mFolderID);
        values.put(BrowserContract.Bookmarks.TITLE, title);
        if (values.size() > 0) {
            new UpdateBookmarkTask(getApplicationContext(), id).execute(values);
        }
        finish();
        if (!mEditingExisting) {
            overridePendingTransition(R.anim.activity_slide_do_nothing,
                    R.anim.slide_down_out);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // dot display action bar
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        mMap = getIntent().getExtras();
        setContentView(R.layout.browser_add_bookmark);
        Window window = getWindow();

        if (mMap != null) {
            Bundle b = mMap.getBundle(EXTRA_EDIT_BOOKMARK);
            if (b != null) {
                mMap = b;
                mEditingExisting = true;
            } else {
                int gravity = mMap.getInt("gravity", -1);
                if (gravity != -1) {
                    WindowManager.LayoutParams l = window.getAttributes();
                    l.gravity = gravity;
                    window.setAttributes(l);
                }
            }
            mEditingExisting = mMap.getBoolean(EXTRA_IS_EDIT_ING, false);
            mIsBottomAnimation = mMap.getBoolean(EXTRA_IS_BOTTOM_ANIMATION, true);
            isNewFolder = mMap.getBoolean(EXTRA_IS_NEW_FOLDER, false);
            mEditingFolder = mMap.getBoolean(EXTRA_IS_FOLDER, false);
            mIsBookmark = mMap.getBoolean(IS_BOOKMARK, false);
            isOnCreate = true;
            startFragment(FRAGMENT_EDIT_PAGE);
            mTouchIconUrl = mMap.getString(TOUCH_ICON_URL);
            mCurrentFolder = mMap.getLong(BrowserContract.Bookmarks.PARENT, DEFAULT_FOLDER_ID);
        }
        initActionBar();

    }

    private void initActionBar() {
        LayoutInflater inflater = (LayoutInflater) getActionBar()
                .getThemedContext().getSystemService(this.LAYOUT_INFLATER_SERVICE);
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM);
        View actionBar = LayoutInflater.from(this).inflate(
                R.layout.browser_settings_actionbar, null);
        getActionBar().setCustomView(actionBar,
                new ActionBar.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        mActionBarTitle = (TextView)
                actionBar.findViewById(R.id.action_new_event_text);
        if (isNewFolder) {
            mActionBarTitle.setText(R.string.new_folder);
        } else if (mEditingExisting) {
            if (mEditingFolder) {
                mActionBarTitle.setText(R.string.edit_folder);
            } else {
                mActionBarTitle.setText(R.string.edit_bookmark);
            }
        } else {
            if (mIsBookmark) {
                mActionBarTitle.setText(R.string.edit_bookmark);
            } else {
                mActionBarTitle.setText(R.string.save_to_bookmarks);
            }
        }

        mCancelActionView = actionBar.findViewById(R.id.action_cancel);
        mCancelActionView.setOnClickListener(this);
        if (mEditingExisting) {
            mCancelActionView.setBackgroundResource(R.drawable.btn_title_back_selector);
            mCancelBtn = (TextView) actionBar.findViewById(R.id.action_back_textview);
            mCancelBtn.setVisibility(View.VISIBLE);
        } else {
            mCancelActionView.setBackgroundResource(R.drawable.btn_title_selector);
            mCancelBtn = (TextView) actionBar.findViewById(R.id.action_cancel_textview);
            mCancelBtn.setVisibility(View.VISIBLE);
        }

        mDoneActionView = actionBar.findViewById(R.id.action_done);
        mDoneActionView.setOnClickListener(this);
        mDoneBtn = (TextView) actionBar.findViewById(R.id.edit_event);
        mDoneBtn.setText(R.string.autofill_profile_editor_save_profile);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        requestFullScreen();
    }

    public void requestFullScreen() {
        Window win = getWindow();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void startFragment(int type) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        mCurrentFragment = type;
        if (mFolderID != -1) {
            mMap.putLong(BrowserContract.Bookmarks.PARENT, mFolderID);
        }
        switch (type) {
            case 0:
                if (mLastActionBarName != null) {
                    mActionBarTitle.setText(mLastActionBarName);
                }
                if (!mEditingExisting && mCancelActionView != null) {
                    mCancelActionView.setBackgroundResource(R.drawable.btn_title_selector);
                    mCancelBtn.setText(R.string.cancel);
                }
                EditBookmarkOrFolderPage editFragment = new EditBookmarkOrFolderPage();
                editFragment.setArguments(mMap);
                if (!isOnCreate) {
                    ft.setCustomAnimations(R.anim.in_from_left, R.anim.out_to_right);
                }
                ft.replace(R.id.contianer, editFragment, FRAGMENT_EDIT_TAG);
                ft.commit();
                break;
            case 1:
                mCancelActionView.setBackgroundResource(R.drawable.btn_title_back_selector);
                mCancelBtn.setText(R.string.back);
                AddBookmarkFolder folderFragment = new AddBookmarkFolder();
                if (mActionBarTitle != null && mActionBarTitle.getText() != null) {
                    mLastActionBarName = mActionBarTitle.getText().toString();
                    mActionBarTitle.setText(R.string.bookmarks_save_target);
                }
                folderFragment.setArguments(mMap);
                ft.setCustomAnimations(R.anim.in_from_right, R.anim.out_to_left);
                ft.replace(R.id.contianer, folderFragment, FRAGMENT_FOLDER_TAG);
                ft.commit();
                break;
        }
    }

    /**
     * Runnable to save a bookmark, so it can be performed in its own thread.
     */
    private class SaveBookmarkRunnable implements Runnable {
        // FIXME: This should be an async task.
        private Message mMessage;
        private Context mContext;

        public SaveBookmarkRunnable(Context ctx, Message msg) {
            mContext = ctx.getApplicationContext();
            mMessage = msg;
        }

        public void run() {
            // Unbundle bookmark data.
            Bundle bundle = mMessage.getData();
            String title = bundle.getString(BrowserContract.Bookmarks.TITLE);
            String url = bundle.getString(BrowserContract.Bookmarks.URL);
            String touchIconUrl = bundle.getString(TOUCH_ICON_URL);
            mInsertData = true;

            // Save to the bookmarks DB.
            try {
                final ContentResolver cr = getContentResolver();
                if (mFolderID == -1) {
//                    Bookmarks.addBookmark(AddBookmarkPage.this, false, url,
//                            title, null, mCurrentFolder);
                } else {
//                    Bookmarks.addBookmark(AddBookmarkPage.this, false, url,
//                            title, null, mFolderID);
                    mFolderID = 0;
                }
                if (touchIconUrl != null) {
                    new DownloadTouchIcon(mContext, cr, url).execute(mTouchIconUrl);
                }
                mMessage.arg1 = 1;
            } catch (IllegalStateException e) {
                mMessage.arg1 = 0;
            }
            mMessage.sendToTarget();
        }
    }

    /**
     * Runnable to save a folder, so it can be performed in its own thread.
     */
    private class SaveFolderRunnable implements Runnable {
        // FIXME: This should be an async task.
        private Message mMessage;
        private Context mContext;

        public SaveFolderRunnable(Context ctx, Message msg) {
            mContext = ctx.getApplicationContext();
            mMessage = msg;
        }

        public void run() {
            // Unbundle bookmark data.
            Bundle bundle = mMessage.getData();
            int type = bundle.getInt(BrowserContract.Bookmarks.IS_FOLDER);
            long parentID = bundle.getLong(BrowserContract.Bookmarks.PARENT);
            String title = bundle.getString(BrowserContract.Bookmarks.TITLE);
            mInsertData = true;
            // Save to the bookmarks DB.
            try {
                Bookmarks.addFolder(AddBookmarkPage.this, false,
                        title, parentID);
                mMessage.arg1 = 1;
            } catch (IllegalStateException e) {
                mMessage.arg1 = 0;
            }
            mMessage.sendToTarget();
        }
    }

    private static class UpdateBookmarkTask extends AsyncTask<ContentValues, Void, Void> {
        Context mContext;
        Long mId;

        public UpdateBookmarkTask(Context context, long id) {
            mContext = context.getApplicationContext();
            mId = id;
        }

        @Override
        protected Void doInBackground(ContentValues... params) {
            if (params.length != 1) {
                throw new IllegalArgumentException("No ContentValues provided!");
            }
            Uri uri = ContentUris.withAppendedId(BookmarkUtils.getBookmarksUri(mContext), mId);
            mContext.getContentResolver().update(
                    uri,
                    params[0], null, null);
            return null;
        }
    }

    private void createHandler() {
        if (mHandler == null) {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case SAVE_BOOKMARK:
                            if (1 == msg.arg1) {
                                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_saved,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_not_saved,
                                        Toast.LENGTH_LONG).show();
                            }
                            break;
                        case SAVE_FOLDER:
                            if (1 == msg.arg1) {
                                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_saved,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(AddBookmarkPage.this, R.string.bookmark_not_saved,
                                        Toast.LENGTH_LONG).show();
                            }
                            break;
                        case TOUCH_ICON_DOWNLOADED:
                            Bundle b = msg.getData();
                            sendBroadcast(BookmarkUtils.createAddToHomeIntent(
                                    AddBookmarkPage.this,
                                    b.getString(BrowserContract.Bookmarks.URL),
                                    b.getString(BrowserContract.Bookmarks.TITLE),
                                    (Bitmap) b.getParcelable(BrowserContract.Bookmarks.TOUCH_ICON),
                                    (Bitmap) b.getParcelable(BrowserContract.Bookmarks.FAVICON)));
                            break;
                        case BOOKMARK_DELETED:
                            finish();
                            break;
                    }
                }
            };
        }
    }

    /**
     * Parse the data entered in the dialog and post a message to update the
     * bookmarks database.
     */
    boolean save() {
        if (mUrlEdit == null || mTitleEdit == null) {
            return false;
        }
        String bookmarkUrl = mUrlEdit.getText().toString().trim();
        if (bookmarkUrl == null) {
            return false;
        }
        String title = mTitleEdit.getText().toString().trim();
        String unfilteredUrl = UrlUtils.fixUrl(bookmarkUrl).trim();
        boolean emptyTitle = title.length() == 0;
        boolean emptyUrl = unfilteredUrl.length() == 0;
        if (emptyTitle) {
            Toast.makeText(this, this.getString(R.string.bookmark_needs_title),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (emptyUrl && !mEditingFolder) {
            Toast.makeText(this, this.getString(R.string.bookmark_needs_url),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        String url = unfilteredUrl;
        if (!mEditingFolder) {
            try {
                // We allow bookmarks with a javascript: scheme, but these will
                // in most cases
                // fail URI parsing, so don't try it if that's the kind of
                // bookmark we have.

                if (!url.toLowerCase().startsWith("javascript:")) {
                    URI uriObj = new URI(url);
                    String scheme = uriObj.getScheme();
                    if (!Bookmarks.urlHasAcceptableScheme(url)) {
                        // If the scheme was non-null, let the user know that we
                        // can't save their bookmark. If it was null, we'll
                        // assume
                        // they meant http when we parse it in the WebAddress
                        // class.
                        if (scheme != null) {
                            // mAddress.setError(r.getText(R.string.bookmark_cannot_save_url));
                            return false;
                        }
                        WebAddress address;
                        try {
                            address = new WebAddress(unfilteredUrl);
                        } catch (ParseException e) {
                            throw new URISyntaxException("", "");
                        }
                        if (address.getHost().length() == 0) {
                            throw new URISyntaxException("", "");
                        }
                        url = address.toString();
                    }
                }
            } catch (URISyntaxException e) {
                Toast.makeText(this.getApplicationContext(), R.string.bookmark_url_not_valid, Toast.LENGTH_LONG).show();
                return false;
            }
        }

        createHandler();

        if (mSaveToHomeScreen) {
            mEditingExisting = false;
        }

        boolean urlUnmodified = url.equals(mOriginalUrl);

        if (mEditingExisting) {
            Long id = mMap.getLong(BrowserContract.Bookmarks._ID);

            ContentValues values = new ContentValues();
            values.put(BrowserContract.Bookmarks.TITLE, title);

            // values.put(BrowserContract.Bookmarks.PARENT, mCurrentFolder);
            values.put(BrowserContract.Bookmarks.PARENT, mFolderID);

            if (!mEditingFolder) {
                values.put(BrowserContract.Bookmarks.URL, url);
                if (!urlUnmodified) {
                    values.putNull(BrowserContract.Bookmarks.THUMBNAIL);
                }
            }
            if (values.size() > 0) {
                new UpdateBookmarkTask(getApplicationContext(), id).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,values);
            }
            setResult(RESULT_OK);
        } else {
            Bitmap thumbnail;
            Bitmap favicon;
            if (urlUnmodified) {
                thumbnail = (Bitmap) mMap.getParcelable(
                        BrowserContract.Bookmarks.THUMBNAIL);
                favicon = (Bitmap) mMap.getParcelable(
                        BrowserContract.Bookmarks.FAVICON);
            } else {
                thumbnail = null;
                favicon = null;
            }

            Bundle bundle = new Bundle();
            bundle.putString(BrowserContract.Bookmarks.TITLE, title);
            bundle.putString(BrowserContract.Bookmarks.URL, url);
            bundle.putParcelable(BrowserContract.Bookmarks.FAVICON, favicon);

            if (mSaveToHomeScreen) {
                if (mTouchIconUrl != null && urlUnmodified) {
                    Message msg = Message.obtain(mHandler,
                            TOUCH_ICON_DOWNLOADED);
                    msg.setData(bundle);
                    DownloadTouchIcon icon = new DownloadTouchIcon(this, msg,
                            mMap.getString(USER_AGENT));
                    icon.execute(mTouchIconUrl);
                } else {
                    sendBroadcast(BookmarkUtils.createAddToHomeIntent(this, url,
                            title, null /* touchIcon */, favicon));
                }
            } else {
                bundle.putParcelable(BrowserContract.Bookmarks.THUMBNAIL, thumbnail);
                bundle.putBoolean(REMOVE_THUMBNAIL, !urlUnmodified);
                bundle.putString(TOUCH_ICON_URL, mTouchIconUrl);
                // Post a message to write to the DB.
                Message msg = Message.obtain(mHandler, SAVE_BOOKMARK);
                msg.setData(bundle);
                // Start a new thread so as to not slow down the UI
                Thread t = new Thread(new SaveBookmarkRunnable(getApplicationContext(), msg));
                t.start();
            }
            setResult(RESULT_OK);
            //LogTag.logBookmarkAdded(url, "bookmarkview");
        }
        return true;
    }

    /*
     * Class used as a proxy for the InputMethodManager to get to mFolderNamer
     */
    public static class CustomListView extends ListView {
        private EditText mEditText;

        public void addEditText(EditText editText) {
            mEditText = editText;
        }

        public CustomListView(Context context) {
            super(context);
        }

        public CustomListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomListView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public boolean checkInputConnectionProxy(View view) {
            return view == mEditText;
        }
    }

    static class AccountsLoader extends CursorLoader {

        static final String[] PROJECTION = new String[] {
                Accounts.ACCOUNT_NAME,
                Accounts.ACCOUNT_TYPE,
                Accounts.ROOT_ID,
        };

        static final int COLUMN_INDEX_ACCOUNT_NAME = 0;
        static final int COLUMN_INDEX_ACCOUNT_TYPE = 1;
        static final int COLUMN_INDEX_ROOT_ID = 2;

        public AccountsLoader(Context context) {
            super(context, Accounts.CONTENT_URI, PROJECTION, null, null, null);
        }
    }

    public static class BookmarkAccount {

        private String mLabel;
        String accountName, accountType;
        public long rootFolderId;

        public BookmarkAccount(Context context, Cursor cursor) {
            accountName = cursor.getString(
                    AccountsLoader.COLUMN_INDEX_ACCOUNT_NAME);
            accountType = cursor.getString(
                    AccountsLoader.COLUMN_INDEX_ACCOUNT_TYPE);
            rootFolderId = cursor.getLong(
                    AccountsLoader.COLUMN_INDEX_ROOT_ID);
            mLabel = accountName;
            if (TextUtils.isEmpty(mLabel)) {
                mLabel = context.getString(R.string.local_bookmarks);
            }
        }

        @Override
        public String toString() {
            return mLabel;
        }
    }

    static class EditBookmarkInfo {
        long id = -1;
        long parentId = -1;
        String parentTitle;
        String title;
        String accountName;
        String accountType;

        long lastUsedId = -1;
        String lastUsedTitle;
        String lastUsedAccountName;
        String lastUsedAccountType;
    }

    static class EditBookmarkInfoLoader extends AsyncTaskLoader<EditBookmarkInfo> {

        private Context mContext;
        private Bundle mMap;

        public EditBookmarkInfoLoader(Context context, Bundle bundle) {
            super(context);
            mContext = context.getApplicationContext();
            mMap = bundle;
        }

        @Override
        public EditBookmarkInfo loadInBackground() {
            final ContentResolver cr = mContext.getContentResolver();
            EditBookmarkInfo info = new EditBookmarkInfo();
            Cursor c = null;

            try {
                // First, let's lookup the bookmark (check for dupes, get needed
                // info)
                if (mMap == null) {
                    return null;
                }
                String url = mMap.getString(BrowserContract.Bookmarks.URL);
                info.id = mMap.getLong(BrowserContract.Bookmarks._ID, -1);
                boolean checkForDupe = mMap.getBoolean(CHECK_FOR_DUPE);
                if (checkForDupe && info.id == -1 && !TextUtils.isEmpty(url)) {
                    c = cr.query(BrowserContract.Bookmarks.CONTENT_URI,
                            new String[] {
                                BrowserContract.Bookmarks._ID
                            },
                            BrowserContract.Bookmarks.URL + "=?",
                            new String[] {
                                url
                            }, null);
                    if (c.getCount() == 1 && c.moveToFirst()) {
                        info.id = c.getLong(0);
                    }
                    c.close();
                }
                if (info.id != -1) {
                    c = cr.query(ContentUris.withAppendedId(
                            BrowserContract.Bookmarks.CONTENT_URI, info.id),
                            new String[] {
                                    BrowserContract.Bookmarks.PARENT,
                                    BrowserContract.Bookmarks.ACCOUNT_NAME,
                                    BrowserContract.Bookmarks.ACCOUNT_TYPE,
                                    BrowserContract.Bookmarks.TITLE
                            },
                            null, null, null);
                    if (c.moveToFirst()) {
                        info.parentId = c.getLong(0);
                        info.accountName = c.getString(1);
                        info.accountType = c.getString(2);
                        info.title = c.getString(3);
                    }
                    c.close();
                    c = cr.query(ContentUris.withAppendedId(
                            BrowserContract.Bookmarks.CONTENT_URI, info.parentId),
                            new String[] {
                                BrowserContract.Bookmarks.TITLE,
                            },
                            null, null, null);
                    if (c.moveToFirst()) {
                        info.parentTitle = c.getString(0);
                    }
                    c.close();
                }

                // Figure out the last used folder/account
                c = cr.query(BrowserContract.Bookmarks.CONTENT_URI,
                        new String[] {
                            BrowserContract.Bookmarks.PARENT,
                        }, null, null,
                        BrowserContract.Bookmarks.DATE_MODIFIED + " DESC LIMIT 1");
                if (c.moveToFirst()) {
                    long parent = c.getLong(0);
                    c.close();
                    c = cr.query(BrowserContract.Bookmarks.CONTENT_URI,
                            new String[] {
                                    BrowserContract.Bookmarks.TITLE,
                                    BrowserContract.Bookmarks.ACCOUNT_NAME,
                                    BrowserContract.Bookmarks.ACCOUNT_TYPE
                            },
                            BrowserContract.Bookmarks._ID + "=?", new String[] {
                                Long.toString(parent)
                            }, null);
                    if (c.moveToFirst()) {
                        info.lastUsedId = parent;
                        info.lastUsedTitle = c.getString(0);
                        info.lastUsedAccountName = c.getString(1);
                        info.lastUsedAccountType = c.getString(2);
                    }
                    c.close();
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            return info;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestFullScreen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void openFolderPathPage() {
        isOnCreate = false;
        startFragment(FRAGMENT_FOLDER_PAGE);
    }

    @Override
    public void getBookmarkTitleEdit(EditText titleEdit) {
        mTitleEdit = titleEdit;
    }

    @Override
    public void getBookmarkUrlEdit(EditText urlEdit) {
        mUrlEdit = urlEdit;
    }

    @Override
    public void isCreateNewFolder(boolean isCreate) {
        mCrateOrEditFolder = isCreate;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int IntTest = savedInstanceState.getInt("IntTest");
        String StrTest = savedInstanceState.getString("StrTest");

    }

    @Override
    public void setFolderID(long folderID) {
        mFolderID = folderID;
    }

    @Override
    public void onBackPressed() {
        if (mCurrentFragment == FRAGMENT_FOLDER_PAGE) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            AddBookmarkFolder fregment = (AddBookmarkFolder) getFragmentManager()
                    .findFragmentByTag(FRAGMENT_FOLDER_TAG);
            ft.hide(fregment);
            isOnCreate = false;
            startFragment(FRAGMENT_EDIT_PAGE);
        } else {
            super.onBackPressed();
            if (!mEditingExisting || isNewFolder) {
                overridePendingTransition(R.anim.activity_slide_do_nothing,
                        R.anim.slide_down_out);
            }
        }
    }

    @Override
    public void setLastTitle(String title) {
        mLastBookmarkTitle = title;
    }

    @Override
    public String getLastTitle() {
        return mLastBookmarkTitle;
    }

    @Override
    public void setLastUrl(String url) {
        mLastBookmakrUrl = url;
    }

    @Override
    public String getLastUrl() {
        return mLastBookmakrUrl;
    }

    @Override
    public void setFolderName(String name) {
        mSelectFolderName = name;
    }

    @Override
    public String getSelectFoldeName() {
        return mSelectFolderName;
    }

    @Override
    public long getRootID() {
        final ContentResolver cr = this.getContentResolver();
        long id;
        String where = BrowserContract.Bookmarks.PARENT +
                " IS NULL AND " + BrowserContract.Bookmarks.IS_DELETED + " = 0 ";
        Cursor cursor = cr.query(BrowserContract.Bookmarks.CONTENT_URI,
                new String[] {
                    BrowserContract.Bookmarks._ID,
                }, where, null, null);
        if (cursor.moveToFirst()) {
            id = cursor.getLong(0);
        } else {
            id = BrowserProvider2.FIXED_ID_ROOT;
        }
        cursor.close();
        return id;
    }
}
