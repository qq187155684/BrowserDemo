package com.android.myapidemo.smartisan.browse;

import com.android.myapidemo.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class QuickBar extends RelativeLayout {
    private OnTextClickListener mListener;
    public QuickBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initQuickBar(context);
    }

    public QuickBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initQuickBar(context);
    }

    public QuickBar(Context context) {
        super(context);
        initQuickBar(context);
    }

    private void initQuickBar(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.quick_input_bar, this);
        OnClickListener quickClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    String textToInsert = ((TextView )v).getText().toString();
                    mListener.onTextClick(textToInsert);
                }
            }
        };
        findViewById(R.id.quick_https).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_www).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_dot).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_slash).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_com).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_cn).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_net).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_org).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_me).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_m).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_bbs).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_blog).setOnClickListener(quickClickListener);
        findViewById(R.id.quick_wap).setOnClickListener(quickClickListener);
    }

    public void setOnTextClickListener(OnTextClickListener listener) {
        mListener = listener;
    }

    public interface OnTextClickListener {
        public void onTextClick(String text);
    }
}
