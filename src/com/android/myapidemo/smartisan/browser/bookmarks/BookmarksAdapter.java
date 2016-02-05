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
import java.util.List;

import com.android.myapidemo.R;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog.Calls;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


public class BookmarksAdapter extends CursorAdapter
{
    static final int NORMAL_STATE_PADDING_LEFT = 0;
    static final int EDIT_STATE_PADDING_LEFT = 9;
    static final int FOLDER = 1;
    private boolean mEditState;
    LayoutInflater mInflater;
    Context mContext;
    ArrayList<Long> mSelectedIds = new ArrayList<Long>();
    private int mCheckBoxWidth = 0;
    private int mIconWidth = 0;
    private int mArrowWidth = 0;
    private int mDefaultIconWidth = 135;
    private int mDefaultIconHeight = 120;
    private int mScaleWidth = 64;
    private int mScaleHeight = 64;
    private int mLeft = 30;
    private int mTop = 30;

    public HorizontalScrollListView mHSListView;

    public interface FragmentEditListener {
        public void onChangAction(int position);

        public void onChangeEditButton(boolean showRemoveBtn);

        public void itemClickEvent(int position, long id);

        public void editBookmark(int position);

        public boolean getEditState();

        public void setViewEnabled(boolean enable, float alpha);

        public CombinedBookmarksCallbacks getActivityCallback();
    }

    private FragmentEditListener mListener;

    /**
     * Create a new BrowserBookmarksAdapter.
     */
    public BookmarksAdapter(Context context) {
        // Make sure to tell the CursorAdapter to avoid the observer and
        // auto-requery
        // since the Loader will do that for us.
        super(context, null);
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mCheckBoxWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.bookmark_checkbox_width);
        mIconWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.bookmark_icon_width);
        mArrowWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.bookmark_arrow_width);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater
                .inflate(R.layout.bookmark_list_item, parent, false);
        BookmarkHolder viewHolder = new BookmarkHolder();
        viewHolder.mNeedInflate = false;
        viewHolder.mItem = ((BookmarkItemCoverView) view.findViewById(R.id.bookmark_cover));
        viewHolder.mItem.setItemCallBackListener(mListener);
        viewHolder.mBgItem = view.findViewById(R.id.item_background);
        viewHolder.mSelectIcon = (CheckBox) view.findViewById(R.id.check_box);
        viewHolder.mArrow = (ImageView) view.findViewById(R.id.arrow);
        viewHolder.mArrow.setOnClickListener(mEditBookMarkListener);
        view.setTag(viewHolder);
        return view;
    }

    private boolean isInSelectedIds(int value){
        boolean result = false;

        for (int i = 0; i < mSelectedIds.size(); i++){
            if (mSelectedIds.get(i) == value){
                result = true;
                break;
            }
        }

        return result;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        bindGridView(view, mContext, cursor);
        int itemID = cursor.getInt(BookmarksLoader.COLUMN_INDEX_ID);
        int position = cursor.getPosition();
        String url = cursor.getString(BookmarksLoader.COLUMN_INDEX_URL);
        BookmarkHolder holder = new BookmarkHolder();
        holder = (BookmarkHolder) view.getTag();
        holder.mID = itemID;
        holder.mPosition = position;
        holder.mItem.setPosition(position);
        holder.mItem.setUrl(url);
        holder.mArrow = (ImageView) view.findViewById(R.id.arrow);
        holder.mArrow.setTag(position);
        if (isInSelectedIds(itemID)){
            holder.mSelectIcon.setChecked(true);
        }else{
            holder.mSelectIcon.setChecked(false);
        }
        if (view == null || holder.mNeedInflate) {
            LayoutParams lp = view.getLayoutParams();
            lp.height = LayoutParams.WRAP_CONTENT;
            view.setLayoutParams(lp);
            holder.mNeedInflate = false;
            view.setTag(holder);
        }
        checkState(view);
        bindBackgroundViewClick(view);
    }

    void bindGridView(View view, Context context, Cursor cursor) {
        if (view == null || view.getTag() == null) {
            return;
        }
        // We need to set this to handle rotation and other configuration change
        // events. If the padding didn't change, this is a no op.
        BookmarkHolder viewHolder = (BookmarkHolder) view.getTag();
        viewHolder.mIcon = (ImageView) view.findViewById(R.id.icon);
        viewHolder.mTitle = (TextView) view.findViewById(R.id.label);
        String title = cursor.getString(BookmarksLoader.COLUMN_INDEX_TITLE);
        int type = cursor.getInt(BookmarksLoader.COLUMN_INDEX_IS_FOLDER);
        viewHolder.mTitle.setText(title);
        if (type == FOLDER) {
            viewHolder.mIsFolder = true;
            viewHolder.mIcon.setImageResource(R.drawable.folder_icon_selector);
        } else {
            byte[] data = cursor.getBlob(BookmarksLoader.COLUMN_INDEX_TOUCH_ICON);
            viewHolder.mIsFolder = false;
            if (data != null && data.length != 0) {
                Bitmap icon = BitmapFactory.decodeByteArray(data, 0,
                        data.length);
                int width = icon.getWidth();
                int height = icon.getHeight();
                float scaleWidth = ((float) mScaleWidth) / width;
                float scaleHeight = ((float) mScaleHeight) / height;
                Matrix matrix = new Matrix();
                matrix.postScale(scaleWidth, scaleHeight);
                icon = Bitmap.createBitmap(icon, 0, 0, width, height, matrix, true);
                Bitmap bitmap = Bitmap.createBitmap(mDefaultIconWidth,
                        mDefaultIconHeight, Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(icon, mLeft, mTop, null);
                viewHolder.mIcon.setImageBitmap(bitmap);
                if (icon != null && !icon.isRecycled()) {
                    icon.recycle();
                }
            } else {
                viewHolder.mIcon.setImageResource(R.drawable.website_icon_selector);
            }
        }
    }

    private void checkState(View view) {
        BookmarkHolder viewHolder = (BookmarkHolder) view.getTag();
        if (mEditState) {
            changeEditMode(viewHolder);
        } else {
            changeNormalMode(viewHolder);
        }
    }

    private void changeEditMode(BookmarkHolder viewHolder) {
        if (viewHolder.mSelectIcon.getTranslationX() == 0) {
            return;
        }
        viewHolder.mSelectIcon.setVisibility(View.VISIBLE);
        viewHolder.mSelectIcon.setTranslationX(0);
        viewHolder.mIcon.setTranslationX(0);
        viewHolder.mTitle.setTranslationX(0);
        viewHolder.mArrow.setTranslationX(0);
    }

    private void changeNormalMode(BookmarkHolder viewHolder) {
        if (viewHolder.mSelectIcon.getTranslationX() < 0) {
            return;
        }
        viewHolder.mSelectIcon.setVisibility(View.VISIBLE);
        viewHolder.mSelectIcon.setTranslationX(-mCheckBoxWidth);
        viewHolder.mIcon.setTranslationX(-mIconWidth);
        viewHolder.mTitle.setTranslationX(-mIconWidth);
        viewHolder.mArrow.setTranslationX(mArrowWidth);
    }

    public void setStartFragmentListener(FragmentEditListener listener) {
        mListener = listener;
    }

    public ArrayList<Long> getSelectItemList() {
        return mSelectedIds;
    }

    private OnClickListener mEditBookMarkListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();
            mListener.onChangAction(position);
        }
    };

    public void setEditCreateState(boolean state) {
        mEditState = state;
    }

    public boolean isChecked(int position) {
        long id = getItemId(position);
        return mSelectedIds.contains(id);
    }

    public boolean setChecked(int position, boolean isChecked) {
        long id = getItemId(position);
        if (id < 0) {
            return false;
        }

        Cursor cursor = getCursor();
        // the cursor's position, we start slide select. the id including in the
        // selected.
        int startPosition = cursor.getPosition();
        // the cursor's position, we end slide select. the id not including in
        // the selected.
        int endPosition = getEndPosition(position);
        // the startPosition should less than endPosition.
        if (startPosition > endPosition) {
            return false;
        }
        boolean flag = false;
        for (int i = startPosition; i < endPosition; i++) {
            if (cursor.moveToPosition(i)) {
                long oneId = cursor.getLong(BookmarksLoader.COLUMN_INDEX_ID);
                if (isChecked && !mSelectedIds.contains(oneId)) {
                    mSelectedIds.add(oneId);
                    flag = true;
                } else if (!isChecked && mSelectedIds.contains(oneId)) {
                    mSelectedIds.remove(oneId);
                    flag = true;
                }
            }
        }
        return flag;
    }

    /**
     * Obtain the cursor position, which is the end position we slide select.
     * And this position not including in selected.
     *
     * @param position
     * @return
     */
    private int getEndPosition(int position) {
        int nextPosition = position + 1;
        // if current position is the last list item, return the cursor's size
        if (nextPosition >= getCount()) {
            return getCursor().getCount();
        }
        long id = getItemId(position + 1);
        if (id < 0) {
            return 0;
        }
        return getCursor().getPosition();
    }

    private void bindBackgroundViewClick(View convertView) {
        BehindItemOption option = new BehindItemOption(mContext);
        BookmarkHolder viewHoler = (BookmarkHolder)convertView.getTag();
        if (option != null && viewHoler != null) {
            option.setOptionListener(viewHoler.mItem);
        }
        option.setListView(mHSListView);
        option.setItemView(convertView);
        option.setTypeOption(0);
        if (viewHoler != null) {
            setBackgroundItemButtonsOnClickListener(viewHoler.mBgItem, option);
        }
    }

    protected void setBackgroundItemButtonsOnClickListener(View vg, View.OnClickListener l) {
        if (null == vg)
            return;
        int ids[] = {
                R.id.cell_edit_bookmark,
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

    public class BookmarkHolder {
        public BookmarkItemCoverView mItem;
        public View mBgItem;
        public Integer mID;
        public Integer mPosition;
        public boolean mIsFolder;
        public boolean mNeedInflate;
        CheckBox mSelectIcon;
        ImageView mIcon;
        TextView mTitle;
        ImageView mArrow;
    }
}
