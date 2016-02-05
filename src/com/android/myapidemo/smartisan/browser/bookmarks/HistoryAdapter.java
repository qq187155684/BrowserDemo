package com.android.myapidemo.smartisan.browser.bookmarks;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.HistoryPageFragment.HistoryQuery;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;


public class HistoryAdapter extends DateSortedExpandableListAdapter{

    public Cursor mMostVisited, mHistoryCursor;
    private boolean isChange = false;
    private Context mContext;
    Drawable mFaviconBackground;
    public HorizontalScrollExpandableListView mHSListView;
    public HistoryEditListener mListener;
    CombinedBookmarksCallbacks mCallback;

    public interface HistoryEditListener {
        public void disableClearBtn();
    }

    HistoryAdapter(Activity activity) {
        super(activity, HistoryQuery.INDEX_DATE_LAST_VISITED);
        mFaviconBackground = BookmarkUtils
                .createListFaviconBackground(activity);
        mContext = activity;
    }

    @Override
    public void changeCursor(Cursor cursor) {
        mHistoryCursor = cursor;
        isChange = false;
        if (mListener != null && mHistoryCursor.getCount() == 0) {
            mListener.disableClearBtn();
        }
        super.changeCursor(cursor);
    }

    void changeMostVisitedCursor(Cursor cursor) {
        if (mMostVisited == cursor) {
            return;
        }
        if (mMostVisited != null) {
            mMostVisited.unregisterDataSetObserver(mDataSetObserver);
            mMostVisited.close();
        }
        mMostVisited = cursor;
        if (mMostVisited != null) {
            mMostVisited.registerDataSetObserver(mDataSetObserver);
        }
        isChange = true;
        notifyDataSetChanged();
    }

    protected void setHistoryEditListener(HistoryEditListener l) {
        mListener = l;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        if (moveCursorToChildPosition(groupPosition, childPosition)) {
            Cursor cursor = getCursor(groupPosition);
            try {
                return cursor.getLong(HistoryQuery.INDEX_ID);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor.moveToLast()) {
                    return cursor.getLong(HistoryQuery.INDEX_ID);
                }
            }
        }
        return 0;
    }

    @Override
    public int getGroupCount() {
        return super.getGroupCount() + (!isMostVisitedEmpty() ? 1 : 0);
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (groupPosition >= super.getGroupCount()) {
            if (isMostVisitedEmpty()) {
                return 0;
            }
            return mMostVisited.getCount();
        }
        return super.getChildrenCount(groupPosition);
    }

    @Override
    public boolean isEmpty() {
        if (!super.isEmpty()) {
            return false;
        }
        return isMostVisitedEmpty();
    }

    private boolean isMostVisitedEmpty() {
        return mMostVisited == null || mMostVisited.isClosed()
                || mMostVisited.getCount() == 0;
    }

    Cursor getCursor(int groupPosition) {
        if (groupPosition >= super.getGroupCount()) {
            return mMostVisited;
        }
        return mHistoryCursor;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
            View convertView, ViewGroup parent) {
        if (groupPosition >= super.getGroupCount()) {
            if (mMostVisited == null || mMostVisited.isClosed()) {
                throw new IllegalStateException("Data is not valid");
            }
            TextView item;
            if (null == convertView || !(convertView instanceof TextView)) {
                LayoutInflater factory = LayoutInflater.from(getContext());
                convertView = factory
                        .inflate(R.layout.history_header, null);
                item = (TextView) convertView.findViewById(R.id.group_name);
            } else {
                item = (TextView) convertView.findViewById(R.id.group_name);
            }
            item.setText(R.string.tab_most_visited);
            return item;
        }
        return super.getGroupView(groupPosition, isExpanded, convertView,
                parent);
    }

    @Override
    boolean moveCursorToChildPosition(int groupPosition, int childPosition) {
        if (groupPosition >= super.getGroupCount()) {
            if (mMostVisited != null && !mMostVisited.isClosed()) {
                mMostVisited.moveToPosition(childPosition);
                return true;
            }
            return false;
        }
        return super
                .moveCursorToChildPosition(groupPosition, childPosition);
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
            boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        HistoryHolder view = new HistoryHolder();
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.history_list_item, null);
            view.mItem = (HistoryItemCoverView) (convertView.findViewById(R.id.item_history));
            view.mName = (TextView) (convertView.findViewById(R.id.title));
            view.mUrl = (TextView) (convertView.findViewById(R.id.url));
            view.mBgItem= convertView.findViewById(R.id.item_background);
            convertView.setTag(view);
        } else {
            view = (HistoryHolder) convertView.getTag();
            if (view.mNeedInflate) {
                LayoutParams lp = convertView.getLayoutParams();
                lp.height = LayoutParams.WRAP_CONTENT;
                convertView.setLayoutParams(lp);
                view.mNeedInflate = false;
                convertView.setTag(view);
            }
        }

        if (mCallback != null) {
            view.mItem.setActivityCallBack(mCallback);
        }

        if (!moveCursorToChildPosition(groupPosition, childPosition)) {
            return convertView;
        }

        Cursor cursor = getCursor(groupPosition);
        if (cursor.getPosition() >= cursor.getCount()) {
            cursor.moveToLast();
        }

        if (view != null && view.mItem != null) {
            view.mItem.setPadding(view.mItem.getPaddingLeft(), view.mItem.getPaddingTop(),
                    view.mItem.getPaddingRight(), view.mItem.getPaddingBottom());
            view.mItem.setTitle(cursor.getString(HistoryQuery.INDEX_TITE));
            view.mItem.setUrl(cursor.getString(HistoryQuery.INDEX_URL));
        }

        BehindItemOption option = new BehindItemOption(mContext);
        if (option != null && view != null) {
            option.setOptionListener(view.mItem);
        }
        option.setListView(mHSListView);
        option.setItemView(convertView);
        option.setTypeOption(1);
        if (view != null) {
            setBackgroundItemButtonsOnClickListener(view.mBgItem, option);
        }
        return convertView;
    }

    public class HistoryHolder {
        public TextView mName;
        public TextView mUrl;
        public HistoryItemCoverView mItem;
        public View mBgItem;
        public boolean mNeedInflate;
    }

    protected void setBackgroundItemButtonsOnClickListener(View vg, View.OnClickListener l) {
        if (null == vg)
            return;
        int ids[] = {
                R.id.cell_add_bookmark,
                R.id.cell_share,
                R.id.cell_copy_link,
                R.id.cell_delete,
        };
        for (int i = 0, count = ids.length; i < count; i++) {
            View v = vg.findViewById(ids[i]);
            if (v != null) {
                v.setOnClickListener(l);
            }
        }
    }

    protected void setCallback(CombinedBookmarksCallbacks callback) {
        mCallback = callback;
    }

}
