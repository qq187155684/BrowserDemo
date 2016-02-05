
package com.android.myapidemo.smartisan.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.AddNavigationActivity;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.navigation.AddNavigationInfo;
import com.android.myapidemo.smartisan.navigation.NavigationInfo;
import com.android.myapidemo.smartisan.navigation.AnimatedExpandableListView.AnimatedExpandableListAdapter;

import java.util.ArrayList;

public class AddNavAdapter extends AnimatedExpandableListAdapter {
    private ArrayList<AddNavigationInfo> mAddNavigationInfos;
    private Context mContext;
    private LayoutInflater mInflater;

    public AddNavAdapter(Context context) {
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
    public NavigationInfo getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).getNavigationInfo(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return getGroup(groupPosition).getId();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return getChild(groupPosition, childPosition).getId();
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
            convertView = mInflater.inflate(R.layout.add_nav_group, parent, false);
            holder = new GroupHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (GroupHolder) convertView.getTag();
        }
        boolean doAnimation = isDoAnimation(groupPosition);
        holder.build(navigationInfo, isExpanded, doAnimation);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
        final ChildHolder holder;
        final NavigationInfo info = getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.add_nav_item, parent, false);
            holder = new ChildHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ChildHolder) convertView.getTag();
        }
        holder.build(info);
        return convertView;
    }
    @Override
    public int getRealChildrenCount(int groupPosition) {
        return getGroup(groupPosition).navigationInfoSize();
    }

    private class GroupHolder {
        TextView groupTitle;
        ImageView groupArrow;
        public GroupHolder(View view) {
            groupTitle = (TextView) view.findViewById(R.id.group_title);
            groupArrow = (ImageView)view.findViewById(R.id.arrow);
        }

        public void build(AddNavigationInfo info, boolean isExpanded, boolean doAnimation) {
            groupTitle.setText(info.getTitle());
            if (!doAnimation) {
                groupArrow.setRotation(isExpanded ? AddNavigationActivity.ROTATION_DEGEE : 0);
            }
            Bitmap bitmap = NavigationInfoParser.getInstance(mContext).parseIcon(info.getIconPath(), info.getIconPath());
            BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
            groupTitle.setCompoundDrawablesWithIntrinsicBounds(bitmapDrawable, null, null, null);
        }
    }

    private class ChildHolder {
        ImageView icon;
        TextView title;
        TextView description;
        Button addNav;
        String tag;

        public ChildHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.icon);
            title = (TextView) view.findViewById(R.id.title);
            description = (TextView) view.findViewById(R.id.desc);
            addNav = (Button) view.findViewById(R.id.add_nav);
        }

        public void build(final NavigationInfo info) {
            if (info.getBitmap() == null) {
                tag = info.getUrl();
                NavigationInfoParser parser = NavigationInfoParser.getInstance(mContext);
                Bitmap bitmap = parser.parseIcon(info.getIconPath(), info.getUrl());
                info.setBitmap(bitmap);
                icon.setImageBitmap(info.getBitmap());
            }else{
                icon.setImageBitmap(info.getBitmap());
            }
            title.setText(info.getTitle());
            description.setText(info.getDescription());
            addNav.setText(info.isAdded() ? R.string.nav_added : R.string.nav_adding);
            Resources resources = mContext.getResources();
            addNav.setTextColor(info.isAdded() ? resources.getColor(R.color.expand_textview_color_disable):resources.getColor(R.color.expand_textview_color));
            addNav.setEnabled(!info.isAdded());
            addNav.setTag(info);
            addNav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NavigationInfo info = (NavigationInfo) v.getTag();
                    info.setAdded(true);
                    NavigationInfoParser.getInstance(mContext).addNavigationInfo(info);
                    notifyDataSetChanged();
                }
            });
        }
    }
}
