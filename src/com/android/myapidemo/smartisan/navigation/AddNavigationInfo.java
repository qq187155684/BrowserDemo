
package com.android.myapidemo.smartisan.navigation;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;

import java.util.ArrayList;

public class AddNavigationInfo {
    private ArrayList<NavigationInfo> navigationInfos = new ArrayList<NavigationInfo>();
    private String title;
    private String iconPath;
    private int id;
    private int color;
    private boolean isExpand;
    transient private Bitmap bitmap;
    private String desc;
    public void addNavgationInfo(NavigationInfo info) {
        navigationInfos.add(info);
    }

    public void clearNavgationInfos() {
        navigationInfos.clear();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public int getColor() {
        return color;
    }

    public void setColor(String color) {
        if (!TextUtils.isEmpty(color) && !NavigationInfo.NONE.equals(color)) {
            try {
                this.color = Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isExpand() {
        return isExpand;
    }

    public void setExpand(boolean isExpand) {
        this.isExpand = isExpand;
    }

    public NavigationInfo getNavigationInfo(int index) {
        return navigationInfos.get(index);
    }

    public ArrayList<NavigationInfo> getAllNavigationInfos() {
        return navigationInfos;
    }

    public int navigationInfoSize() {
        return navigationInfos.size();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

}
