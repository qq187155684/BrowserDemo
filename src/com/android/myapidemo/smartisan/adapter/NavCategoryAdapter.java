
package com.android.myapidemo.smartisan.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.navigation.AddNavigationInfo;
import com.android.myapidemo.smartisan.navigation.AnimatedExpandableListView;
import com.android.myapidemo.smartisan.navigation.NavigationInfo;
import com.android.myapidemo.smartisan.navigation.AnimatedExpandableListView.AnimatedExpandableListAdapter;

import java.util.ArrayList;

public class NavCategoryAdapter extends AnimatedExpandableListAdapter implements
        OnClickListener {
    private static final int COLUMN_COUNT = 3;
    private ArrayList<AddNavigationInfo> mAddNavigationInfos;
    private Context mContext;
    private LayoutInflater mInflater;
    private OnClickListener mOnClickListener;

    public NavCategoryAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getGroupCount() {
        return mAddNavigationInfos.size();
    }

    public void setNavigationInfos(ArrayList<AddNavigationInfo> addNavigationInfos) {
        this.mAddNavigationInfos = addNavigationInfos;
    }

    public ArrayList<AddNavigationInfo> getNavigationInfos() {
        return mAddNavigationInfos;
    }

    @Override
    public AddNavigationInfo getGroup(int groupPosition) {
        return mAddNavigationInfos.get(groupPosition);
    }

    @Override
    public AddNavigationInfo getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return getGroup(groupPosition).getId();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return getGroup(groupPosition).getNavigationInfo(childPosition).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
        GroupHolder holder;
        AddNavigationInfo navigationInfo = getGroup(groupPosition);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.category_group, parent, false);
            holder = new GroupHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (GroupHolder) convertView.getTag();
        }
        holder.build(navigationInfo, isExpanded);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        ChildHolder holder;
        AddNavigationInfo addNavigationInfo = getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.category_item, parent, false);
            holder = new ChildHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ChildHolder) convertView.getTag();
        }
        holder.build(addNavigationInfo, childPosition);
        return convertView;
    }

    @Override
    public int getRealChildrenCount(int groupPosition) {
        return (getGroup(groupPosition).navigationInfoSize() + COLUMN_COUNT - 1) / COLUMN_COUNT;
    }

    private class GroupHolder {
        TextView groupTitle;
        ImageView groupArrow;
        ImageView groupImage;

        public GroupHolder(View view) {
            groupTitle = (TextView) view.findViewById(R.id.group_title);
            groupArrow = (ImageView) view.findViewById(R.id.arrow);
            groupImage = (ImageView) view.findViewById(R.id.image);
        }

        public void build(final AddNavigationInfo addNavInfo, boolean isExpanded) {
            groupTitle.setText(addNavInfo.getTitle());
            groupArrow.setImageResource(isExpanded ? R.drawable.nav_fold_selector
                    : R.drawable.nav_unfold_selector);
            if (addNavInfo.getBitmap() == null) {
                NavigationInfoParser parser = NavigationInfoParser.getInstance(mContext);
//                parser.parseIcon(addNavInfo.getIconPath(), null, new NavIconParseListener() {
//                    @Override
//                    public void onUpdateIcon(Bitmap bitmap, String url, boolean isFavIcon) {
//                        addNavInfo.setBitmap(bitmap);
//                        groupImage.setImageBitmap(addNavInfo.getBitmap());
//                    }
//                });
            }else{
                groupImage.setImageBitmap(addNavInfo.getBitmap());
            }
        }
    }

    private class ChildHolder {
        ArrayList<TextView> textViews;

        public ChildHolder(View view) {
            textViews = new ArrayList<TextView>();
            ViewGroup viewGroup = ((ViewGroup) view);
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof TextView) {
                    textViews.add((TextView) child);
                }
            }
        }

        public void build(AddNavigationInfo addNavInfo, int childPosition) {
            ArrayList<NavigationInfo> infos = addNavInfo.getAllNavigationInfos();
            int j = 0;
            for (int i = childPosition * COLUMN_COUNT; j < COLUMN_COUNT && i < infos.size(); i++, j++) {
                NavigationInfo info = infos.get(i);
                TextView textView = textViews.get(j);
                textView.setText(info.getTitle());
                textView.setOnClickListener(NavCategoryAdapter.this);
                textView.setTag(info);
            }
            for (; j < COLUMN_COUNT; j++) {
                TextView textView = textViews.get(j);
                textView.setText("");
                textView.setOnClickListener(null);
                textView.setClickable(false);
            }
        }
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(v);
        }
    }
}
