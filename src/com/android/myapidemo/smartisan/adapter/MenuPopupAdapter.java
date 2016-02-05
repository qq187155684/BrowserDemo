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

package com.android.myapidemo.smartisan.adapter;

import java.util.ArrayList;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.widget.BasePopupWindow.ListItem;
import com.android.myapidemo.smartisan.widget.MenuPopupWindow;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MenuPopupAdapter extends BaseAdapter {

    public static final int TOP_ITEM = 0;
    public static final int RIGHT_BOTTOTM_ITEM = 1;
    public static final int MIDLE_BOTTOM_ITEM = 2;

    public int mType = 0;
    private boolean mIncogState;
    private boolean mBlankPageState;
    private boolean mHasDeleteThumbnail;
    private boolean mCanSaveBookmark;
    private LayoutInflater mInflater;
    private ArrayList<ListItem> mItems = new ArrayList<ListItem>();
    private Context mContext;

    public MenuPopupAdapter(Context context, ArrayList<ListItem> items) {
        super();
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItems = items;
        mContext = context;
    }

    private void bindView(View convertView, int position) {
        ListItem item = (ListItem) getItem(position);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.mIcon = (ImageView) convertView
                .findViewById(R.id.option_list_item_icon);
        viewHolder.mIcon.setImageDrawable(item.mImage);
        viewHolder.mTvTitle = (TextView) convertView
                .findViewById(R.id.option_list_item_text);
        viewHolder.mTvTitle.setText(item.mText);
        viewHolder.mRelativeLayout = (RelativeLayout) convertView
                .findViewById(R.id.poprelative);
        boolean isEnabled = isEnabled(position);
        int colorId = isEnabled ? R.color.popup_window_font : R.color.disabled_color;
        float alpha = isEnabled ? 1.0f : 0.3f;
        viewHolder.mTvTitle.setTextColor(mContext.getResources()
                .getColor(colorId));
        viewHolder.mIcon.setAlpha(alpha);
        viewHolder.mRelativeLayout.setGravity(Gravity.LEFT
                | Gravity.CENTER_VERTICAL);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.menu_list_item, parent,
                    false);
            ViewHolder viewHolder = new ViewHolder();
            convertView.setTag(viewHolder);
        }
        bindView(convertView, position);
        return convertView;
    }

    public int getCount() {
        return mItems.size();
    }

    public Object getItems() {
        return mItems;
    }

    public boolean isItemCheck(int position) {
        return mItems.get(position).mChecked;
    }

    public Object getItem(int position) {
        return mItems.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public void setSelectedItem(int position) {
        int cc = getCount();
        for (int i = 0; i < cc; i++) {
            mItems.get(i).mChecked = (i == position);
        }
        notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(int position) {
        if (mBlankPageState) {
            if ((position == MenuPopupWindow.ITEM_FIND_IN_PAGE)
                    || (position == MenuPopupWindow.ITEM_SWITCH_UA)
                    || (position == MenuPopupWindow.ITEM_COPY_AND_SHARE)
                    || (position == MenuPopupWindow.iTEM_SAVE_BOOKMARK)) {
                return false;
            }
        }
        if ((!mHasDeleteThumbnail || mIncogState) && position == MenuPopupWindow.ITEM_REOPEN_CLOSED_TAB) {
            return false;
        }
        if (!mCanSaveBookmark && position == MenuPopupWindow.iTEM_SAVE_BOOKMARK) {
            return false;
        }
        return true;
    }

    // whether current tab is null
    public void setBlankPage(boolean isBlankPage) {
        mBlankPageState = isBlankPage;
    }

    // whether current tab is incog state
    public void setIncogState(boolean isIncog) {
        mIncogState = isIncog;
    }

    public void setRemusePageState(boolean pageState) {
        mHasDeleteThumbnail = pageState;
    }

    public void setCanSaveBookmark(boolean canSave) {
        mCanSaveBookmark = canSave;
    }

    private static class ViewHolder {
        ImageView mIcon;
        TextView mTvTitle;
        RelativeLayout mRelativeLayout;
    }
}
