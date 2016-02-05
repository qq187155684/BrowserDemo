
package com.android.myapidemo.smartisan.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.navigation.NavigationInfo;

import java.util.ArrayList;

public class AddNavSearchAdapter extends BaseAdapter {
    private ArrayList<NavigationInfo> mInfos;
    private LayoutInflater mInflater;
    private Context mContext;

    public AddNavSearchAdapter(Context context, ArrayList<NavigationInfo> infos) {
        mInfos = infos;
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NavSearchHolder holder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.add_nav_item, parent, false);
            holder = new NavSearchHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (NavSearchHolder) convertView.getTag();
        }
        NavigationInfo info = (NavigationInfo) getItem(position);
        holder.build(info);
        return convertView;
    }

    private class NavSearchHolder {
        ImageView icon;
        TextView title;
        TextView description;
        Button addNav;

        NavSearchHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.icon);
            title = (TextView) view.findViewById(R.id.title);
            description = (TextView) view.findViewById(R.id.desc);
            addNav = (Button) view.findViewById(R.id.add_nav);
        }

        void build(final NavigationInfo info) {
            if (info.getBitmap() == null) {
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
