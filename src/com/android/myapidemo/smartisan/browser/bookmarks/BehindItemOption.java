
package com.android.myapidemo.smartisan.browser.bookmarks;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.BookmarksAdapter.BookmarkHolder;
import com.android.myapidemo.smartisan.browser.bookmarks.HistoryAdapter.HistoryHolder;
import com.android.myapidemo.smartisan.browser.platformsupport.Browser;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;

public class BehindItemOption implements OnClickListener {

    public static final int BOOKMARK_OPTION = 0;
    public static final int HISTORY_OPTION = 1;

    // mark browser or history event
    private int mCurrentType = -1;

    private OptionListener mOptionListener;
    private Context mContext;
    private boolean mShareClicked;

    interface OptionListener {

        public String getUrl();

        public String getTitle();

        public void editBookmark();

        public void exitAnimation();
    }

    public BehindItemOption(Context context) {
        mContext = context;
    }

    public void setTypeOption(int type) {
        mCurrentType = type;

    }

    public void setOptionListener(OptionListener l) {
        mOptionListener = l;
    }

    private View mItemView;

    public void setItemView(View view) {
        mItemView = view;
    }

    private void shareItem() {
        if (mContext == null || mOptionListener == null || !mShareClicked) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, mOptionListener.getUrl());
        send.putExtra(Intent.EXTRA_SUBJECT, mOptionListener.getTitle());
        try {
            Intent i = Intent.createChooser(send,
                    mContext.getText(R.string.choosertitle_sharevia));
            ((Activity) mContext).startActivity(i);
        } catch (android.content.ActivityNotFoundException ex) {
            // if no app handles it, do nothing
        }
        mShareClicked = false;
    }

    private View mListView;

    public void setListView(View view) {
        mListView = view;
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (mOptionListener == null) {
            return;
        }
        if (id == R.id.cell_add_bookmark) {
            Browser.saveBookmark(mContext, mOptionListener.getTitle(),
                    mOptionListener.getUrl());
            mOptionListener.exitAnimation();
        } else if (id == R.id.cell_edit_bookmark) {
            mOptionListener.editBookmark();
        } else if (id == R.id.cell_share) {
            mShareClicked = true;
            shareItem();
        } else if (id == R.id.cell_copy_link) {
            Toast.makeText(mContext,
                    mContext.getString(R.string.have_copied),
                    Toast.LENGTH_LONG).show();
            ClipboardManager cm = (ClipboardManager) mContext
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setText(mOptionListener.getUrl());
        } else if (id == R.id.cell_delete) {
            if (mCurrentType == BOOKMARK_OPTION) {
                HorizontalScrollListView listView = (HorizontalScrollListView) mListView;
                View convertView = mItemView.findViewById(R.id.bookmark_list_item);
                final BookmarkHolder viewholder = (BookmarkHolder) convertView.getTag();
                listView.playDeleteItemAnimation(new AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        viewholder.mNeedInflate = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        Uri uri = ContentUris.withAppendedId(
                                BrowserContract.Bookmarks.CONTENT_URI, viewholder.mID);
                        mContext.getContentResolver().delete(uri, null, null);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            } else {
                HorizontalScrollExpandableListView listView = (HorizontalScrollExpandableListView) mListView;
                listView.playDeleteItemAnimation(new AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        View convertView = mItemView.findViewById(R.id.history_list_item);
                        final HistoryHolder holder = (HistoryHolder) convertView.getTag();
                        holder.mNeedInflate = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        Browser.deleteFromHistory(mContext.getContentResolver(),
                                mOptionListener.getUrl());
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
            }
        }
    }
}
