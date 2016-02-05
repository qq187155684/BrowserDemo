
package com.android.myapidemo.smartisan.widget;

import com.android.myapidemo.R;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;


public class MenuPopupWindow extends BasePopupWindow{
    private ListView mListView;
    private LinearLayout mLinearLayout;
    public static final int ITEM_REOPEN_CLOSED_TAB = 0;
    public static final int ITEM_FIND_IN_PAGE = 1;
    public static final int ITEM_SWITCH_INCOG = 2;
    public static final int ITEM_SWITCH_UA = 3;
    public static final int ITEM_COPY_AND_SHARE = 4;
    public static final int ITEM_SAVE_OFFLINE = 5;
    public static final int ITEM_SAVE_BOOKMARK = 5;
    public static final int ITEM_BOOKMARKS = 6;
    public static final int ITEM_HISTORY = 7;
    public static final int ITEM_SETTING = 8;

    public static int iTEM_SAVE_BOOKMARK = ITEM_SAVE_BOOKMARK;
    public static int iTEM_BOOKMARKS = ITEM_BOOKMARKS;
    public static int iTEM_HISTORY = ITEM_HISTORY;
    public static int iTEM_SETTING = ITEM_SETTING;

    private Context mContext;
    public MenuPopupWindow(Context context, View view) {
        super(context, view);
    }

    @Override
    protected void initLayout(Context context) {
        super.initLayout(context);
        mContext = context;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View popupView = layoutInflater.inflate(R.layout.popup_menu, null);
//        popupView.setOnClickListener(mPopviewClickListener);
        setContentView(popupView);
        mListView = (ListView) popupView.findViewById(R.id.context_menu_list_view);
        mLinearLayout = (LinearLayout)popupView.findViewById(R.id.menu_popup);
        mListView.setFocusableInTouchMode(true);
        mListView.setFocusable(true);
    }

    @Override
    public void setListViewParams(LinearLayout.LayoutParams params) {
        int orientation = mContext.getResources().getConfiguration().orientation;
        int width = getContentView().getResources().getInteger(R.integer.find_on_page_right_point);
        params.width = width;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.rightMargin = getContentView().getResources().getDimensionPixelSize(
                    R.dimen.menu_popup_margin_left);
        } else {
            params.rightMargin = 0;
        }
        mLinearLayout.setLayoutParams(params);
    }

    @Override
    public void setAdapter(BaseAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    OnClickListener mPopviewClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            dismiss();
        }
    };

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        mListView.setOnItemClickListener(listener);
    }

    public void setLandState(boolean isLand) {
        Resources res = mContext.getResources();
        if (isLand) {
            Drawable drawable = res.getDrawable(R.drawable.menu_item_top_land);
            mLinearLayout.setBackground(drawable);
        } else {
            Drawable drawable = res.getDrawable(R.drawable.menu_item_bottom);
            mLinearLayout.setBackground(drawable);
        }
    }
}
