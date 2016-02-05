package com.android.myapidemo.smartisan.browse;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CustomScreen extends FrameLayout{
    private BaseUi mUi;

    public CustomScreen(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomScreen(Context context) {
        super(context);
    }

    public void setUi(BaseUi ui) {
        mUi = ui;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //this not work if in fullscreen,so we not excute in landscape mode
        if(Configuration.ORIENTATION_LANDSCAPE == getResources().getConfiguration().orientation){
            return;
        }
        if (mUi == null) {
            return;
        }

        int height = getMeasuredHeight();
        int rootViewHeight = CustomScreen.this.getRootView().getHeight();
        if (height > 0 && height < rootViewHeight * 2 / 3) {
            mUi.updateQuickBar(true);
        } else {
            mUi.updateQuickBar(false);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
