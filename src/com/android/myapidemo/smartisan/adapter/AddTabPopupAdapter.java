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

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AddTabPopupAdapter extends BaseAdapter {

    public static final int TOP_ITEM = 0;
    public static final int RIGHT_BOTTOTM_ITEM = 1;
    public static final int MIDLE_BOTTOM_ITEM = 2;

    public static final int POSITION_FIRST_ITEM = 1;
    public static final int POSITION_SECOEND_ITEM = 2;
    public static final int POSITION_THIRD_ITEM = 3;

    public int mType = 0;
    private boolean mBlankPageState;
    private boolean mIsIncognito;
    private LayoutInflater mInflater;
    private ArrayList<ListItem> mItems = new ArrayList<ListItem>();
    private Context mContext;

    public AddTabPopupAdapter(Context context, ArrayList<ListItem> items,
            int type) {
        super();
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItems = items;
        mType = type;
        mContext = context;
    }

    private void bindView(View convertView, int position) {
        ListItem item = (ListItem) getItem(position);
        if (position == 0) {
            if (mType == TOP_ITEM) {
                setBackgroundResource(convertView,
                        R.drawable.menu_item_top_selector);
            } else {
                setBackgroundResource(convertView,
                        R.drawable.menu_item_top2_selector);
            }
        } else if (position == getCount() - 1) {
            switch (mType) {
            case TOP_ITEM:
                setBackgroundResource(convertView,
                        R.drawable.menu_item_bottom1_selector);
                break;
            case RIGHT_BOTTOTM_ITEM:
                setBackgroundResource(convertView,
                        R.drawable.menu_item_bottom3_selector);
                break;
            case MIDLE_BOTTOM_ITEM:
                setBackgroundResource(convertView,
                        R.drawable.menu_item_bottom2_selector);
                break;
            default:
                break;
            }
        } else {
            setBackgroundResource(convertView,
                    R.drawable.menu_item_mid_selector);
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.mIcon = (ImageView) convertView
                .findViewById(R.id.option_list_item_icon);
        viewHolder.mIcon.setImageDrawable(item.mImage);
        viewHolder.mTvTitle = (TextView) convertView
                .findViewById(R.id.option_list_item_text);
        viewHolder.mTvTitle.setText(item.mText);
        viewHolder.mDwCheck = (ImageView) convertView
                .findViewById(R.id.option_list_item_check);
        viewHolder.mDwCheck
                .setBackgroundResource(R.drawable.ic_bookmark_list_selected);
        viewHolder.mDwCheck.setVisibility(item.mChecked ? View.VISIBLE
                : View.GONE);
        viewHolder.mRelativeLayout = (RelativeLayout) convertView
                .findViewById(R.id.poprelative);
        boolean isEnabled = isEnabled(position);
        int colorId = isEnabled ? R.color.popup_window_font : R.color.disabled_color;
        float alpha = isEnabled ? 1.0f : 0.3f;
        viewHolder.mTvTitle.setTextColor(mContext.getResources()
                .getColor(colorId));
        viewHolder.mIcon.setAlpha(alpha);
        if (mType == RIGHT_BOTTOTM_ITEM) {
            viewHolder.mRelativeLayout.setGravity(Gravity.LEFT
                    | Gravity.CENTER_VERTICAL);
        } else {
            viewHolder.mIcon.setVisibility(View.GONE);
            viewHolder.mDwCheck.setVisibility(View.GONE);
            viewHolder.mRelativeLayout.setGravity(Gravity.CENTER);
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.option_list_item, parent,
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

    private void setBackgroundResource(View view, int resID) {
        if (view == null || resID == -1 || resID == 0) {
            return;
        }
        view.setBackgroundResource(resID);
    }

    @Override
    public boolean isEnabled(int position) {
        if (mBlankPageState && !mIsIncognito && position == 0) {// if current is blank,disable
            return false;
        }
        if (mBlankPageState && mIsIncognito && position == 1) {// if current is blank and in Incognito mode,disable
            return false;
        }
        return true;

    }

    // whether current tab is null
    public void setBlankPage(boolean isBlankPage) {
        mBlankPageState = isBlankPage;
    }

    public void setPrivateBrowsingEnable(boolean isIncognito) {
        mIsIncognito = isIncognito;
    }

    private static class ViewHolder {
        ImageView mIcon;
        TextView mTvTitle;
        ImageView mDwCheck;
        RelativeLayout mRelativeLayout;
    }
}
