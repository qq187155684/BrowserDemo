
package com.android.myapidemo.smartisan.browse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.AddBookmarkPage.EditBookmarkInfo;
import com.android.myapidemo.smartisan.browser.bookmarks.BookmarksPageFragment;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract.Accounts;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.MergeCursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class AddBookmarkFolder extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private View mFolderPath;
    private FolderAdapter mAdapter;
    private FolderAdapter mFolderAdapter;

    static final String EXTRA_IS_FOLDER = "is_folder";

    // cursor column id
    public static final int ACCOUNT_NAME_COLUMN_ID = 0;
    public static final int ACCOUNT_TYPE_COLUMN_ID = 1;
    public static final int ACCOUNT_ROOT_COLUMN_ID = 2;

    public static final int FOLDER_ROOT_ID = 1;

    // Pading value
    private static final int FOLDER_FIRST_LEVEL = 0;
    private static final int FOLDER_SECOEND_LEVEL = 36;
    private static final int FOLDER_THIRD_LEVEL = 72;

    private static final long POST_DELAYED = 150L;

    private EditBookmark mListener;
    private Bundle mMap;
    private long mRootFolder;

    private String mTitle;
    private long mCurrentID = 0;
    // current bookmark or folde save the folder id.
    private long mCurrentFolderID = 0;

    // sort relate
    private int mSelfID = 0;
    private int mParentID = 0;
    private int mLevel = 0;
    private int maxChild = 0;
    private int mPosition = 0;

    private int mNodeCount = 0;
    private int mNodeNum = 0;

    private boolean mEditingFolder;
    private boolean mEditingExisting;
    private boolean mInsertData;
    private boolean mSortFinish = false;
    private boolean mChange = false;

    private ListView mListView;
    private TextView mSelectFolder;

    private int mRecordID;

    private Cursor mSortCursor;
    private Context mContext;

    // IDs for the CursorLoaders that are used.
    private final int LOADER_ID_ACCOUNTS = 0;
    private final int LOADER_ID_FOLDER_CONTENTS = 1;
    private final int LOADER_ID_EDIT_INFO = 2;

    private HashMap<Integer, Integer> mSelfMap = new HashMap<Integer, Integer>();
    private HashMap<Integer, Integer> mPositionMap = new HashMap<Integer, Integer>();
    private HashMap<Integer, Integer> mLevelMap = new HashMap<Integer, Integer>();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mMap = getArguments();
        mContext = getActivity().getApplicationContext();
        mAdapter = new FolderAdapter(getActivity());
        mFolderAdapter = new FolderAdapter(getActivity());
        mEditingFolder = mMap.getBoolean(EXTRA_IS_FOLDER, false);
        mCurrentFolderID = mMap.getLong(BrowserContract.Bookmarks.PARENT, 1);
        ContentResolver resolver = getActivity().getContentResolver();
        String projection[] = new String[] {
                BrowserContract.Bookmarks._ID,
                BrowserContract.Bookmarks.TITLE,
                BrowserContract.Bookmarks.IS_FOLDER,
                BrowserContract.Bookmarks.PARENT
        };

        String where = BrowserContract.Bookmarks._ID +
                " = " + 0;
        mSortCursor = resolver.query(BrowserContract.Bookmarks.CONTENT_URI,
                projection,
                where,
                null,
                null);
        getLoaderManager().restartLoader(LOADER_ID_ACCOUNTS, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mFolderPath = inflater.inflate(R.layout.bookmark_folder, container, false);
        // hide inputMethod
        mListView = (ListView) mFolderPath.findViewById(R.id.bookmark_floder);
        mListView.setAdapter(mFolderAdapter);
        return mFolderPath;
    }

    @Override
    public void onResume() {
        super.onResume();
        hideInputMethod();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof EditBookmark)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mListener = (EditBookmark) activity;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection;
        switch (id) {
            case LOADER_ID_ACCOUNTS:
                return new AccountsLoader(getActivity());
            case LOADER_ID_FOLDER_CONTENTS:
                projection = new String[] {
                        BrowserContract.Bookmarks._ID,
                        BrowserContract.Bookmarks.TITLE,
                        BrowserContract.Bookmarks.IS_FOLDER,
                        BrowserContract.Bookmarks.PARENT
                };

                String where0 = BrowserContract.Bookmarks.IS_FOLDER + " != 0";

                // root node parent id is null
                String where1 = " AND " + BrowserContract.Bookmarks.PARENT +
                        " is NULL";
                // normal node query
                String where2 = " AND " + BrowserContract.Bookmarks.PARENT +
                        " = " + mCurrentID;
                String where = null;

                if (mCurrentID == 0) {
                    where = where0 + where1;
                } else {
                    where = where0 + where2;
                }

                String whereArgs[] = null;
                if (mEditingFolder) {
                    where += " AND " + BrowserContract.Bookmarks._ID + " != ?";
                    whereArgs = new String[] {
                            Long.toString(mMap.getLong(
                                    BrowserContract.Bookmarks._ID))
                    };
                }

                long currentFolder = 0;
                return new CursorLoader(getActivity(),
                        getUriForFolder(currentFolder),
                        projection,
                        where,
                        whereArgs,
                        BrowserContract.Bookmarks._ID + " ASC");
            default:
                throw new AssertionError("Asking for nonexistant loader!");
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ID_ACCOUNTS:
                int id = LOADER_ID_FOLDER_CONTENTS;
                LoaderManager lm = getLoaderManager();
                mNodeCount = cursor.getCount();

                while (cursor.moveToNext()) {
                    String accountName = cursor.getString(ACCOUNT_NAME_COLUMN_ID);
                    String accountType = cursor.getString(ACCOUNT_TYPE_COLUMN_ID);
                    Bundle args = new Bundle();
                    args.putString(BookmarksPageFragment.ACCOUNT_NAME, accountName);
                    args.putString(BookmarksPageFragment.ACCOUNT_TYPE, accountType);
                    // load folder
                    mCurrentID = cursor.getInt(ACCOUNT_ROOT_COLUMN_ID);
                    if (mCurrentID > 0) {
                        mCurrentID = mCurrentID - 1;
                    }
                    lm.restartLoader(id, args, this);

                }
                getLoaderManager().destroyLoader(LOADER_ID_ACCOUNTS);
                //getLoaderManager().restartLoader(LOADER_ID_EDIT_INFO, null,
                        //mEditInfoLoaderCallbacks);

                break;
            case LOADER_ID_FOLDER_CONTENTS:
                if (!mInsertData) {
                    mAdapter.changeCursor(cursor);
                    sortAdapter(mAdapter);
                }
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_FOLDER_CONTENTS:
                mAdapter.changeCursor(null);
                break;
        }

    }

    private void setAdapter() {
        mFolderAdapter.changeCursor(mSortCursor);
        mFolderAdapter.notifyDataSetChanged();
    }

    private Uri getUriForFolder(long folderID) {
        return BrowserContract.Bookmarks.CONTENT_URI;
    }

    private LoaderCallbacks<EditBookmarkInfo> mEditInfoLoaderCallbacks =
            new LoaderCallbacks<EditBookmarkInfo>() {

                @Override
                public void onLoaderReset(Loader<EditBookmarkInfo> loader) {
                    // Don't care
                }

                @Override
                public void onLoadFinished(Loader<EditBookmarkInfo> loader,
                        EditBookmarkInfo info) {
                    if (info == null) {
                        return;
                    }
                    boolean setAccount = false;
                    if (info != null && info.id != -1) {
                        mEditingExisting = true;
                        // showRemoveButton();
                        // mFakeTitle.setText(R.string.edit_bookmark);

                        // mTitle.setText(info.title);
                        mTitle = info.title;

                        // mFolderAdapter.setOtherFolderDisplayText(info.parentTitle);
                        mMap.putLong(BrowserContract.Bookmarks._ID, info.id);
                        setAccount = true;
                        // setAccount(info.accountName, info.accountType);
                        // mCurrentFolder = info.parentId;
                        // onCurrentFolderFound();
                    }
                    // TODO: Detect if lastUsedId is a subfolder of info.id in
                    // the
                    // editing folder case. For now, just don't show the last
                    // used
                    // folder at all to prevent any chance of the user adding a
                    // parent
                    // folder to a child folder
                    if (info.lastUsedId != -1 && info.lastUsedId != info.id
                            && !mEditingFolder) {
                        if (setAccount && info.lastUsedId != mRootFolder
                                && TextUtils.equals(info.lastUsedAccountName, info.accountName)
                                && TextUtils.equals(info.lastUsedAccountType, info.accountType)) {
                            // mFolderAdapter.addRecentFolder(info.lastUsedId,
                            // info.lastUsedTitle);
                        } else if (!setAccount) {
                            setAccount = true;
                            /*
                             * setAccount(info.lastUsedAccountName,
                             * info.lastUsedAccountType); if (info.lastUsedId !=
                             * mRootFolder) {
                             * mFolderAdapter.addRecentFolder(info.lastUsedId,
                             * info.lastUsedTitle); }
                             */
                        }
                    }
                    /*
                     * if (!setAccount) { if (mAccountSpinner != null) {
                     * mAccountSpinner.setSelection(0); } }
                     */
                }

                @Override
                public Loader<EditBookmarkInfo> onCreateLoader(int id, Bundle args) {
                    return null;//new EditBookmarkInfoLoader(getActivity(), mMap);
                }
            };

    public void sortAdapter(FolderAdapter adapter) {
        // if current node has not child,find its brother node.
        if (adapter.getCount() == 0) {
            long fatherID = 0;
            if (!mSelfMap.isEmpty()) {
                fatherID = mSelfMap.get(mSelfID);
            }
            // record node position
            if (!mPositionMap.containsKey(mSelfID)) {
                mPositionMap.put(mSelfID, mPosition);
            }
            // by this position find next node.
            mPosition = mPositionMap.get(mSelfID) + 1;
            restartLoad(fatherID);
        } else {
            if (adapter.getCount() > 0) {
                // if adapter count is 0,means this point no child.
                maxChild = adapter.getCount();
            }
            // if parent node has itreator finish,return last node.
            if (mPosition >= maxChild) {
                // if (adapter.getItem(mPosition) != null) {
                Cursor cursor1 = (Cursor) adapter.getItem(mPosition);
                cursor1.moveToLast();
                mSelfID = (int) cursor1.getLong(cursor1
                        .getColumnIndexOrThrow(BrowserContract.Bookmarks._ID));
                // }

                long fatherID = mSelfMap.get(mSelfID);
                if (mSelfMap.containsKey((int) fatherID) && !mPositionMap.isEmpty()) {
                    long grandpaID = mSelfMap.get((int) fatherID);
                    if (mPositionMap.containsKey((int) fatherID)) {
                        mPosition = mPositionMap.get((int) fatherID) + 1;
                    }
                    // by node id 0 as final point.
                    if (grandpaID == 0) {
                        mNodeNum = mNodeNum + 1;
                        mLevel = 0;
                    }
                    if (grandpaID == 0 && (mNodeNum >= mNodeCount)) {
                        mPositionMap.clear();
                        mSortFinish = true;
                        setAdapter();
                        return;
                    }
                    restartLoad(grandpaID);
                } else {
                    mSortFinish = true;
                    setAdapter();
                    return;
                }
            }

            if (adapter.getItem(mPosition) != null) {
                Cursor cursor1 = (Cursor) adapter.getItem(mPosition);
                ContentResolver resolver = getActivity().getContentResolver();

                // cursor1.moveToLast();// first or last
                mSelfID = (int) cursor1.getLong(cursor1
                        .getColumnIndexOrThrow(BrowserContract.Bookmarks._ID));
                mParentID = (int) cursor1.getLong(cursor1
                        .getColumnIndexOrThrow(BrowserContract.Bookmarks.PARENT));

                // close cursor
                try {
                    if (cursor1 != null && !cursor1.isClosed())
                        cursor1.close();
                    cursor1 = null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                String projection[] = new String[] {
                        BrowserContract.Bookmarks._ID,
                        BrowserContract.Bookmarks.TITLE,
                        BrowserContract.Bookmarks.IS_FOLDER,
                        BrowserContract.Bookmarks.PARENT
                };

                String where = BrowserContract.Bookmarks._ID +
                        " = " + mSelfID;
                Cursor cursor2 = resolver.query(getUriForFolder(0),
                        projection,
                        where,
                        null,
                        BrowserContract.Bookmarks._ID + " ASC");

                cursor2.moveToFirst();

                // meger cursor
                if (!mSelfMap.containsKey(mSelfID)) {
                    mSortCursor = new MergeCursor(new Cursor[] {
                            mSortCursor, cursor2
                    });
                    mSelfMap.put(mSelfID, mParentID);
                }

                // set folder in which level.
                if (!mLevelMap.containsKey(mSelfID)) {
                    if (mLevelMap.containsKey(mParentID)) {
                        mLevel = mLevelMap.get(mParentID) + 1;
                        mLevelMap.put(mSelfID, mLevel);
                    }
                    mLevelMap.put(mSelfID, mLevel);
                }
                if (mPositionMap.containsKey(mSelfID)) {
                    // get self position
                    mPosition = mPositionMap.get(mSelfID);
                } else {
                    // record self position
                    mPositionMap.put(mSelfID, mPosition);
                    // if not contains in position map,that is next level,when
                    // in cycle time.
                    mPosition = 0;
                }
                restartLoad(mSelfID);
            }
        }
    }

    public void restartLoad(long id) {
        mInsertData = false;
        LoaderManager manager = getLoaderManager();
        CursorLoader loader = (CursorLoader) ((Loader<?>) manager.getLoader(
                LOADER_ID_FOLDER_CONTENTS));
        if (loader == null) {
            return;
        }
        mCurrentID = id;
        loader.reset();
        getLoaderManager().restartLoader(LOADER_ID_FOLDER_CONTENTS, null, this);

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

    public class FolderAdapter extends CursorAdapter {
        int mSelectPosition = 0;

        public FolderAdapter(Context context) {
            super(context, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView selectView = (ImageView) view.findViewById(R.id.select_icon);
            long fodlerID = cursor.getLong(cursor
                    .getColumnIndexOrThrow(BrowserContract.Bookmarks._ID));
            mSelectFolder = (TextView) view.findViewById(android.R.id.text1);
            mSelectFolder.setText(
                    cursor.getString(cursor.getColumnIndexOrThrow(
                            BrowserContract.Bookmarks.TITLE)));
            if(FOLDER_ROOT_ID == fodlerID){
                mSelectFolder.setText(R.string.tab_bookmarks);
            }
            if (mCurrentFolderID == fodlerID) {
                selectView.setVisibility(View.VISIBLE);
            } else {
                selectView.setVisibility(View.INVISIBLE);
            }
            if (mSortFinish) {
                ImageView folderIcon = (ImageView) view.findViewById(R.id.icon);
                int key = (int) fodlerID;
                int level = mLevelMap.get(key);
                switch (level) {
                    case 0:
                        folderIcon.setPadding(FOLDER_FIRST_LEVEL, 0, 0, 0);
                        break;
                    case 1:
                        folderIcon.setPadding(FOLDER_SECOEND_LEVEL, 0, 0, 0);
                        break;
                    case 2:
                        folderIcon.setPadding(FOLDER_THIRD_LEVEL, 0, 0, 0);
                        break;
                    default:
                        folderIcon.setPadding(FOLDER_THIRD_LEVEL, 0, 0, 0);
                        break;
                }
            }
            LinearLayout folderItem = (LinearLayout) view.findViewById(R.id.folder_list_item);
            folderItem.setTag(cursor.getPosition());
            folderItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Integer position = (Integer) view.getTag();
                    mSelectPosition = (int) position;
                    long folderID = getItemId(position);
                    mCurrentFolderID = folderID;
                    TextView foldeName = (TextView) view.findViewById(android.R.id.text1);
                    mListener.setFolderID(mCurrentFolderID);
                    mListener.setFolderName(foldeName.getText().toString());
                    mMap.putString(EditBookmarkOrFolderPage.CURRENT_FOLDER_NAME,foldeName.getText().toString());
                    ImageView selectView = (ImageView) view.findViewById(R.id.select_icon);
                    selectView.setVisibility(View.VISIBLE);
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.folder_list_item, null);
            return view;
        }
    }

    private void hideInputMethod() {
        final InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(mContext.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if ((getActivity() != null && getActivity().getCurrentFocus() != null)
                        && imm.isActive()) {
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus()
                            .getWindowToken(),
                            0);
                }
            }
        };
        handler.postDelayed(runnable, POST_DELAYED);
    }
}
