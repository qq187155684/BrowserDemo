package com.android.myapidemo.smartisan.adapter;

import java.util.ArrayList;

import com.android.myapidemo.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SearchPopupWindowAdapter extends BaseAdapter {

    public static final int TOP_ITEM = 0;
    public static final int RIGHT_BOTTOTM_ITEM = 1;
    public static final int MIDLE_BOTTOM_ITEM = 2;

    public static final int POSITION_FIRST_ITEM = 1;
    public static final int POSITION_SECOEND_ITEM = 2;
    public static final int POSITION_THIRD_ITEM = 3;

    public int mType = 0;
    private LayoutInflater mInflater;
    private ArrayList<ListItem> mItems = new ArrayList<ListItem>();

    public static class ListItem {
        public final CharSequence mText;
        public final Drawable mImage;
        public boolean mChecked;

        public ListItem(String text, Drawable drawable) {
            mText = text;
            mImage = drawable;
        }
    }

    public SearchPopupWindowAdapter(Context context, ArrayList<ListItem> items, int type) {
        super();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItems = items;
        mType = type;
    }

    private void bindView(View convertView, int position) {
        ListItem item = (ListItem) getItem(position);
        if (position == 0) {
            if (mType == TOP_ITEM) {
                setBackgroundResource(convertView, R.drawable.menu_item_top_selector);
            } else {
                setBackgroundResource(convertView, R.drawable.menu_item_top2_selector);
            }
        } else if (position == getCount() - 1) {
            switch (mType) {
                case TOP_ITEM:
                    setBackgroundResource(convertView, R.drawable.menu_item_bottom1_selector);
                    break;
                case RIGHT_BOTTOTM_ITEM:
                    setBackgroundResource(convertView, R.drawable.menu_item_bottom3_selector);
                    break;
                case MIDLE_BOTTOM_ITEM:
                    setBackgroundResource(convertView, R.drawable.menu_item_bottom2_selector);
                    break;
                default:
                    break;
            }
        } else {
            setBackgroundResource(convertView, R.drawable.menu_item_mid_selector);
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.mIcon = (ImageView) convertView.findViewById(R.id.option_list_item_icon);
        viewHolder.mIcon.setImageDrawable(item.mImage);
        viewHolder.mTvTitle = (TextView) convertView.findViewById(R.id.option_list_item_text);
        viewHolder.mTvTitle.setText(item.mText);
        viewHolder.mDwCheck = (ImageView) convertView.findViewById(R.id.option_list_item_check);
        viewHolder.mDwCheck.setBackgroundResource(R.drawable.ic_bookmark_list_selected);
        viewHolder.mDwCheck.setVisibility(item.mChecked ? View.VISIBLE : View.GONE);
        viewHolder.mRelativeLayout = (RelativeLayout) convertView.findViewById(R.id.poprelative);
        if (mType == MIDLE_BOTTOM_ITEM) {
            viewHolder.mIcon.setVisibility(View.GONE);
            viewHolder.mDwCheck.setVisibility(View.GONE);
            viewHolder.mRelativeLayout.setGravity(Gravity.CENTER);
        } else {
            viewHolder.mRelativeLayout.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }
     }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.option_list_item, parent, false);
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

    private static class ViewHolder {
        ImageView mIcon;
        TextView mTvTitle;
        ImageView mDwCheck;
        RelativeLayout mRelativeLayout;
    }
}
