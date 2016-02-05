/**
 * 
 */

package com.android.myapidemo.smartisan.browse;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.BookmarksPageFragment;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 * @author qijin
 */
public class EditBookmarkOrFolderPage extends Fragment implements OnClickListener {

    // IDs for the CursorLoaders that are used.
    private final int LOADER_ID_ACCOUNTS = 0;
    private final int LOADER_ID_FOLDER_CONTENTS = 1;
    private final int LOADER_ID_EDIT_INFO = 2;

    // get save current bookmark folder name.
    private final int LOADER_FOLDER_NAME = 0;

    public interface OnChangeFolderPage
    {
        public void getBookmarkTitleEdit(EditText titleEdit);

        public void getBookmarkUrlEdit(EditText urlEdit);

        public void getSaveFolderID(long id);

        public void isCreateNewFolder(boolean isCreate);

        public void openFolderPathPage();

        public void setLastTitle(String title);

        public String getLastTitle();

        public void setLastUrl(String url);

        public String getLastUrl();

    }

    private View mFolderPath;
    private EditBookmark mListener;

    static final String EXTRA_IS_FOLDER = "is_folder";
    static final String NAME_IS_BOOKMARKS = "Bookmarks";
    static final String CURRENT_FOLDER_NAME = "current_folder_name";

    public static final long DEFAULT_FOLDER_ID = -1;

    private Bundle mMap;

    private long mRootFolder;

    private boolean mEditingFolder;
    private boolean mNewFolder;

    private long mCurrentFolder;
    private long mParentFolder;
    private long mRootID;
    private View mEditPage;
    private View mDivider;

    private TextView mFolderNameText;
    private EditText mTitle;
    private EditText mAddress;
    private ImageView mClearBtn;

    private Context mContext;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mMap = getArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mEditPage = inflater.inflate(R.layout.browser_add_bookmark_editpage, container, false);

        String title = mMap.getString(BrowserContract.Bookmarks.TITLE);
        String folderName = mMap.getString(CURRENT_FOLDER_NAME);
        String url = mMap.getString(BrowserContract.Bookmarks.URL);
        String accountName = mMap.getString(BookmarksPageFragment.ACCOUNT_NAME);

        mParentFolder = mMap.getLong(BrowserContract.Bookmarks.PARENT, mRootID);
        mListener.setFolderID(mParentFolder);
        mEditingFolder = mMap.getBoolean(EXTRA_IS_FOLDER, false);
        mTitle = (EditText) mEditPage.findViewById(R.id.title);
        mTitle.addTextChangedListener(new TitleTextWatcher());
        setTitle(title, url);
        mListener.getBookmarkTitleEdit(mTitle);
        mAddress = (EditText) mEditPage.findViewById(R.id.address);
        setUrl(url);
        mListener.getBookmarkUrlEdit(mAddress);
        mCurrentFolder = mMap.getLong(BrowserContract.Bookmarks.PARENT, DEFAULT_FOLDER_ID);
        mFolderNameText = (TextView) mEditPage.findViewById(R.id.folder_name);
        setFolderName(folderName, accountName, url);
        mDivider = mEditPage.findViewById(R.id.divider);
        if (mEditingFolder) {
            mAddress.setVisibility(View.GONE);
            mDivider.setVisibility(View.GONE);
        }
        mClearBtn = (ImageView) mEditPage.findViewById(R.id.clear_button);
        mClearBtn.setOnClickListener(this);
        mFolderPath = (RelativeLayout) mEditPage.findViewById(R.id.folder_path);
        mFolderPath.setOnClickListener(this);
        return mEditPage;
    }

    public void setTitle(String title, String url) {
        if (mListener == null || mTitle == null) {
            return;
        }
        if (title == null && url == null) {
            mListener.isCreateNewFolder(true);
            if (mListener.getLastTitle() != null) {
                mTitle.setText(mListener.getLastTitle());
            } else {
                mTitle.setText(R.string.new_folder);
            }
        } else {
            if (mListener.getLastUrl() != null) {
                mTitle.setText(mListener.getLastTitle());
            } else {
                mTitle.setText(title);
            }
        }
        mTitle.selectAll();
    }

    public void setUrl(String url) {
        if (mListener == null && url == null) {
            return;
        }
        if (mListener.getLastUrl() != null) {
            mAddress.setText(mListener.getLastUrl());
        } else {
            mAddress.setText(url);
        }
    }

    public void setFolderName(String folderName, String accountName, String url) {
        if (folderName == null) {
            if (accountName == null || url == null) {
                mFolderNameText.setText(R.string.tab_bookmarks);
            } else {
                mFolderNameText.setText(accountName);
            }
        } else {
            if (mListener != null && mListener.getSelectFoldeName() != null) {
                mFolderNameText.setText(mListener.getSelectFoldeName());
            } else {
                mFolderNameText.setText(folderName);
            }
        }
    }

    private static final long POST_DELAYED = 100L;

    @Override
    public void onResume() {
        super.onResume();
        if (mTitle == null || mTitle.getText() == null) {
            return;
        }
        if (mTitle.getText().length() == 0) {
            mClearBtn.setVisibility(View.INVISIBLE);
        } else {
            mClearBtn.setVisibility(View.VISIBLE);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showSoftKeyboard();
            }
        }, POST_DELAYED);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof EditBookmark)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mListener = (EditBookmark) activity;
        mRootID = mListener.getRootID();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListener.setLastTitle(mTitle.getText().toString());
        mListener.setLastUrl(mAddress.getText().toString());
    }

    @Override
    public void onClick(View view) {
        if (mFolderPath == view) {
            if (isShowSoftKeyboard()) {
                hideSoftKeyboard();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mListener != null)
                            mListener.openFolderPathPage();
                    }
                }, 300);
            } else {
                mListener.openFolderPathPage();
            }
        } else if (mClearBtn == view) {
            mTitle.setText("");
            mClearBtn.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isShowSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
          .getSystemService(Context.INPUT_METHOD_SERVICE);
        return inputMethodManager.isActive();
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
          .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                mEditPage.getWindowToken(), 0);
    }

    private void showSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(mEditPage.findFocus(), 0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        hideSoftKeyboard();
    }

    private class TitleTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mClearBtn != null) {
                if (s.length() == 0) {
                    mClearBtn.setVisibility(View.INVISIBLE);
                } else {
                    mClearBtn.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}
