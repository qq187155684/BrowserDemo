
package com.android.myapidemo.smartisan.navigation;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;

import java.io.Serializable;

public class NavigationInfo implements Serializable{
    private static final long serialVersionUID = 31L;
    public static final String SERIALIZABLE_NAME = "navinfo_serializable_v1.1";
    public static NavigationInfo EMPTY_NAVIGATION_INFO = new NavigationInfo();
    public static String NONE = "-1";
    public static int FIELD_COUNT = 4;
    public static int NAV_TITLE = 0;
    public static int NAV_URL = 1;
    public static int NAV_TEXT_COLOR = 2;
    public static int NAV_ICON_PATH = 3;
    private int id;
    private String title;
    private String url;
    private String iconPath;
    private String description;
    private String bitmapMd5;
    transient private Bitmap bitmap;
    private int color = -1;
    private boolean added;
    private boolean hasFavicon;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
        if (!TextUtils.isEmpty(color) && !NONE.equals(color)) {
            try {
                this.color = Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean hasThisKeyword(String text) {
        return (title != null && title.contains(text)) ||
                (url != null && url.contains(text)) ||
                (description != null && description.contains(text));
    }

    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
    }

    public String getBitmapMd5() {
        return bitmapMd5;
    }

    public void setBitmapMd5(String bitmapMd5) {
        this.bitmapMd5 = bitmapMd5;
    }
}
