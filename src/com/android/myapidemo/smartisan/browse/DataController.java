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


package com.android.myapidemo.smartisan.browse;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.os.Bundle;


import java.io.ByteArrayOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.BookmarkUtils;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract.History;
import com.android.myapidemo.smartisan.browser.provider.BrowserProvider2;
import com.android.myapidemo.smartisan.browser.provider.BrowserProvider2.Thumbnails;

public class DataController {
    private static final String LOGTAG = "DataController";
    // Message IDs
    private static final int HISTORY_UPDATE_VISITED = 100;
    private static final int HISTORY_UPDATE_TITLE = 101;
    private static final int QUERY_URL_IS_BOOKMARK = 200;
    private static final int TAB_LOAD_THUMBNAIL = 201;
    private static final int TAB_SAVE_THUMBNAIL = 202;
    private static final int TAB_DELETE_THUMBNAIL = 203;
//    private static final int TAB_SAVE_LAND_THUMBNAIL = 204;
//    private static final int TAB_LOAD_LAND_THUMBNAIL = 205;
//    private static final int TAB_DELETE_LAND_THUMBNAIL = 206;
    private static final int TAB_SAVE_STATE = 207;
    private static final int TAB_CLEAR_STATE = 208;
    private static DataController sInstance;

    private Context mContext;
    private DataControllerHandler mDataHandler;
    private Handler mCbHandler; // To respond on the UI thread

    /* package */ public static interface OnQueryUrlIsBookmark {
        void onQueryUrlIsBookmark(String url, boolean isBookmark);
    }
    private static class CallbackContainer {
        Object replyTo;
        Object[] args;
    }

    private static class DCMessage {
        int what;
        Object obj;
        Object replyTo;
        DCMessage(int w, Object o) {
            what = w;
            obj = o;
        }
    }

    /* package */ public static DataController getInstance(Context c) {
        if (sInstance == null) {
            sInstance = new DataController(c);
        }
        return sInstance;
    }

    private DataController(Context c) {
        mContext = c.getApplicationContext();
        mDataHandler = new DataControllerHandler();
        mDataHandler.start();
        mCbHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                CallbackContainer cc = (CallbackContainer) msg.obj;
                switch (msg.what) {
                    case QUERY_URL_IS_BOOKMARK: {
                        OnQueryUrlIsBookmark cb = (OnQueryUrlIsBookmark) cc.replyTo;
                        String url = (String) cc.args[0];
                        boolean isBookmark = (Boolean) cc.args[1];
                        cb.onQueryUrlIsBookmark(url, isBookmark);
                        break;
                    }
                }
            }
        };
    }

    public void updateVisitedHistory(String url) {
        mDataHandler.sendMessage(HISTORY_UPDATE_VISITED, url);
    }

    public void updateHistoryTitle(String url, String title) {
        mDataHandler.sendMessage(HISTORY_UPDATE_TITLE, new String[] { url, title });
    }

    public void queryBookmarkStatus(String url, OnQueryUrlIsBookmark replyTo) {
        if (url == null || url.trim().length() == 0) {
            // null or empty url is never a bookmark
            replyTo.onQueryUrlIsBookmark(url, false);
            return;
        }
        mDataHandler.sendMessage(QUERY_URL_IS_BOOKMARK, url.trim(), replyTo);
    }

    public void loadThumbnail(Tab tab) {
        mDataHandler.sendMessage(TAB_LOAD_THUMBNAIL, tab);
    }

    public void deleteThumbnail(Tab tab) {
        mDataHandler.sendMessage(TAB_DELETE_THUMBNAIL, tab.getId());
    }

    public void saveThumbnail(Tab tab) {
        mDataHandler.sendMessage(TAB_SAVE_THUMBNAIL, tab);
    }

    public void clearState(String url) {
        mDataHandler.sendMessage(TAB_CLEAR_STATE, url);
    }

    public void saveState(Bundle state) {
        mDataHandler.sendMessage(TAB_SAVE_STATE, state);
    }

    // The standard Handler and Message classes don't allow the queue manipulation
    // we want (such as peeking). So we use our own queue.
    class DataControllerHandler extends Thread {
        private BlockingQueue<DCMessage> mMessageQueue
                = new LinkedBlockingQueue<DCMessage>();

        public DataControllerHandler() {
            super("DataControllerHandler");

            mContentResolver = mContext.getContentResolver();
            mMaxTabSize = mContext.getResources().getInteger(R.integer.max_tabs);
            validateRevertPageCount();
        }

        @Override
        public void run() {
            setPriority(Thread.MIN_PRIORITY);
            while (true) {
                try {
                    handleMessage(mMessageQueue.take());
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }

        void sendMessage(int what, Object obj) {
            DCMessage m = new DCMessage(what, obj);
            mMessageQueue.add(m);
        }

        void sendMessage(int what, Object obj, Object replyTo) {
            DCMessage m = new DCMessage(what, obj);
            m.replyTo = replyTo;
            mMessageQueue.add(m);
        }

        private void handleMessage(DCMessage msg) {
            switch (msg.what) {
            case HISTORY_UPDATE_VISITED:
                doUpdateVisitedHistory((String) msg.obj);
                break;
            case HISTORY_UPDATE_TITLE:
                String[] args = (String[]) msg.obj;
                doUpdateHistoryTitle(args[0], args[1]);
                break;
            case QUERY_URL_IS_BOOKMARK:
                // TODO: Look for identical messages in the queue and remove them
                // TODO: Also, look for partial matches and merge them (such as
                //       multiple callbacks querying the same URL)
                doQueryBookmarkStatus((String) msg.obj, msg.replyTo);
                break;
            case TAB_LOAD_THUMBNAIL:
                doLoadThumbnail((Tab) msg.obj);
                break;
            case TAB_DELETE_THUMBNAIL:
                try {
                    mContentResolver.delete(ContentUris.withAppendedId(
                            Thumbnails.CONTENT_URI, (Long)msg.obj),
                            null, null);
                } catch (Throwable t) {}
                break;
            case TAB_SAVE_THUMBNAIL:
                doSaveThumbnail((Tab)msg.obj);
                break;
            case TAB_SAVE_STATE:
                doSaveState((Bundle)msg.obj);
                break;
            case TAB_CLEAR_STATE:
                doClearState(msg.obj);
                break;
            }
        }

        private byte[] getCaptureBlob(Tab tab, boolean vertical) {
            synchronized (tab) {
                Bitmap capture = null;
                if (vertical)
                    capture = tab.getScreenshot();
                else
                    capture = tab.getHScreenshot();
                if (capture == null) {
                    return null;
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                try {
                    capture.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                } catch (IllegalStateException e) {
                    return null;
                }
                return stream.toByteArray();
            }
        }

        private void doSaveThumbnail(Tab tab) {
            byte[] blob = getCaptureBlob(tab, true);
            byte[] blobLand = getCaptureBlob(tab, false);
            if (blob == null || blobLand == null) {
                return;
            }
            ContentValues values = new ContentValues();
            values.put(Thumbnails._ID, tab.getId());
            values.put(Thumbnails.THUMBNAIL, blob);
            values.put(Thumbnails.THUMBNAIL_LAND, blobLand);
            try {
                mContentResolver.insert(Thumbnails.CONTENT_URI, values);
            } catch(Exception e) {}
        }

        private void doLoadThumbnail(Tab tab) {
            Cursor c = null;
            try {
                Uri uri = ContentUris.withAppendedId(Thumbnails.CONTENT_URI, tab.getId());
                c = mContentResolver.query(uri, new String[] {Thumbnails.THUMBNAIL, Thumbnails.THUMBNAIL_LAND}, null, null, null);
                if (c.moveToFirst()) {
                    byte[] data = c.getBlob(0);
                    byte[] dataLand = c.getBlob(1);
                    if (data != null && data.length > 0) {
                        tab.updateCaptureFromBlob(data, true);
                    }
                    if (dataLand != null && dataLand.length > 0) {
                        tab.updateCaptureFromBlob(dataLand, false);
                    }
                }
            } catch (Exception e) {
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        private void doUpdateVisitedHistory(String url) {
            Cursor c = null;
            try {
                c = mContentResolver.query(History.CONTENT_URI, new String[] { History._ID, History.VISITS },
                        History.URL + "=?", new String[] { url }, null);
                if (c.moveToFirst()) {
                    ContentValues values = new ContentValues();
                    values.put(History.VISITS, c.getInt(1) + 1);
                    values.put(History.DATE_LAST_VISITED, System.currentTimeMillis());
                    mContentResolver.update(ContentUris.withAppendedId(History.CONTENT_URI, c.getLong(0)),
                            values, null, null);
                } else {
                    //Browser.truncateHistory(mContentResolver);
                    ContentValues values = new ContentValues();
                    values.put(History.URL, url);
                    values.put(History.VISITS, 1);
                    values.put(History.DATE_LAST_VISITED, System.currentTimeMillis());
                    values.put(History.TITLE, url);
                    values.put(History.DATE_CREATED, 0);
                    values.put(History.USER_ENTERED, 0);
                    mContentResolver.insert(History.CONTENT_URI, values);
                }
            } catch(Exception e) {
            } finally {
                if (c != null) c.close();
            }
        }

        private void doQueryBookmarkStatus(String url, Object replyTo) {
            // Check to see if the site is bookmarked
            Cursor cursor = null;
            boolean isBookmark = false;
            try {
                cursor = mContentResolver.query(
                        BookmarkUtils.getBookmarksUri(mContext),
                        new String[]{BrowserContract.Bookmarks.URL},
                        BrowserContract.Bookmarks.URL + " == ?",
                        new String[]{url},
                        null);
                isBookmark = cursor.moveToFirst();
            } catch (Exception e) {
                Log.e(LOGTAG, "Error checking for bookmark: " + e);
            } finally {
                if (cursor != null) cursor.close();
            }
            CallbackContainer cc = new CallbackContainer();
            cc.replyTo = replyTo;
            cc.args = new Object[] { url, isBookmark };
            mCbHandler.obtainMessage(QUERY_URL_IS_BOOKMARK, cc).sendToTarget();
        }

        private void doUpdateHistoryTitle(String url, String title) {
            ContentValues values = new ContentValues();
            values.put(History.TITLE, title);
            try {
                mContentResolver.update(History.CONTENT_URI, values, History.URL + "=?",
                        new String[] { url });
            } catch(Exception e) {}
        }

        private void doClearState(Object obj) {
            Cursor cursor = null;
            if (obj == null) {// delete all state
                try {
                    cursor = mContentResolver.query(uri, new String[] { History._ID },
                            null, null, null);
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        mContentResolver.delete(uri, null, null);
                    }
                } catch (Exception e) {
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            else {// delete state by url
                String url = (String)obj;
                try {
                    cursor = mContentResolver.query(uri, new String[] { History._ID },
                            History.URL + "=?", new String[] { url }, null);
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int id = cursor.getInt(cursor.getColumnIndexOrThrow(History._ID));
                        mContentResolver.delete(uri, "_id = ?", new String[]{id+""});
                    }
                } catch (Exception e) {
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

        private void doSaveState(Bundle state) {
            if (state == null) {
                return;
            }
            String url = state.getString(History.URL, null);
            if (url == null || "".equals(url)) {
                return;
            }
            ContentValues cValues = new ContentValues();
            cValues.put(History.URL, url);
            cValues.put(History.TITLE, state.getString(History.TITLE, ""));
            cValues.put(History.DATE_CREATED, state.getLong(History.DATE_CREATED));

            byte[] backfordList = state.getByteArray(BACKFORWARD_LIST);
            if (backfordList != null && backfordList.length < 2000000 && backfordList.length >= 0) // the blob must < 2MB. see http://stackoverflow.com/questions/21432556
                cValues.put(BrowserProvider2.COLUMN_BACKFORWARD_LIST, backfordList);

            try {
                if (queryClosedUrlCount() == mMaxTabSize) {
                    mContentResolver.delete(uri, "_id = ?", new String[]{getEarliestUrlId()+""});
                }
                mContentResolver.insert(uri, cValues);
            } catch(Exception e) {}
        }

        private int getEarliestUrlId() {
            return getUrlId("created asc");
        }

        private void validateRevertPageCount(){
            try {
                while (queryClosedUrlCount() > mMaxTabSize) {
                    int id = getEarliestUrlId();
                    if (id == -1) { // some error happens, can't delete
                        break;
                    }
                    mContentResolver.delete(uri, "_id = ?", new String[]{id+""});
                }
            } catch(Exception e) {}
        }
    }

    private ContentResolver mContentResolver;
    private int mMaxTabSize;
    private Uri uri = Uri.parse("content://" + BrowserContract.AUTHORITY + "/thumbnailcache");
    public int queryClosedUrlCount() {
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = mContentResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                count = cursor.getCount();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    private int getUrlId(String order) {
        int id = -1;
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, null, null, null, order);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }

    public static String BACKFORWARD_LIST = "WEBVIEW_CHROMIUM_STATE"; // only for 4.4.2 and up?
    public Bundle getLastestState(){
        Bundle state = null;
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(uri, null, null, null, "_id desc");
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                state = new Bundle();
                state.putString(History.URL, cursor.getString(cursor.getColumnIndexOrThrow(History.URL)));
                state.putString(History.TITLE, cursor.getString(cursor.getColumnIndexOrThrow(History.TITLE)));
                state.putByteArray(BACKFORWARD_LIST, cursor.getBlob(cursor.getColumnIndexOrThrow(BrowserProvider2.COLUMN_BACKFORWARD_LIST)));
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return state;
    }
}
