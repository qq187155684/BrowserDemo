package com.android.myapidemo.smartisan.widget;


import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

public abstract class BasePopupWindow extends PopupWindow{

    private View mView;

    public BasePopupWindow(Context context, View view) {
        mView = view;
        mView.setEnabled(false);
        initLayout(context);
    }

    protected void initLayout(Context context) {
        setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                mView.setEnabled(true);
            }
        });
    }

    public abstract void setListViewParams(LinearLayout.LayoutParams params);

    public abstract void setAdapter(BaseAdapter adapter);

    public abstract void setOnItemClickListener(OnItemClickListener listener);

    public static class ListItem {
        public final CharSequence mText;
        public final Drawable mImage;
        public boolean mChecked;

        public ListItem(String text, Drawable drawable) {
            mText = text;
            mImage = drawable;
        }
    }
}
