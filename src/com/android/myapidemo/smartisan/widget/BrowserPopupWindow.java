/**
 *
 */

package com.android.myapidemo.smartisan.widget;

/**
 * @author qijin
 *
 */

import com.android.myapidemo.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class BrowserPopupWindow extends BasePopupWindow{

    private ListView mListView;

    public BrowserPopupWindow(Context context, View view) {
        super(context, view);
    }

    @Override
    protected void initLayout(Context context) {
        super.initLayout(context);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View popupView = layoutInflater.inflate(R.layout.popupwindow_menu, null);
        popupView.setOnClickListener(mPopviewClickListener);
        setContentView(popupView);
        mListView = (ListView) popupView.findViewById(R.id.context_menu_list_view);
        mListView.setFocusableInTouchMode(true);
        mListView.setFocusable(true);
    }

    public void setListViewParams(LinearLayout.LayoutParams params) {
        int width = getContentView().getResources().getInteger(R.integer.find_on_page_right_point);
        params.width = width;
        mListView.setLayoutParams(params);
    }

    public void setAdapter(BaseAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListView.setOnItemClickListener(listener);
    }

    OnClickListener mPopviewClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            dismiss();
        }
    };
}
