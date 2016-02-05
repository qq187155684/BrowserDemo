package com.android.myapidemo.smartisan.browse;

import com.android.myapidemo.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
/**
 * thumbnail items for thumbnail page
 * @author luzhd
 *
 */
public class ThumbnailItems extends RelativeLayout{
    private LayoutInflater inflater;
    public NavTabView mTabViewLeft;
    public NavTabView mTabViewRight;
    public NavTabView mTabViewDownLeft;
    public NavTabView mTabViewDownRight;
    public FrameLayout frameLayoutLeft;
    public FrameLayout frameLayoutRight;
    public FrameLayout frameLayoutDownLeft;
    public FrameLayout frameLayoutDownRight;
    public ImageView mImageViewLeft;
    public ImageView mImageViewRight;
    public ImageView mImageViewDownLeft;
    public ImageView mImageViewDownRight;
    private OnClickListener mClickListener;
    public ThumbnailItems(Context context) {
        super(context);
        init(context);
    }
    public ThumbnailItems(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public ThumbnailItems(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context context){
        inflater = LayoutInflater.from(context);
        final View itemView = inflater.inflate(R.layout.pager_item, this, true);
        mTabViewLeft = (NavTabView)itemView.findViewById(R.id.tab0);
        mTabViewRight = (NavTabView)itemView.findViewById(R.id.tab1);
        mTabViewDownLeft = (NavTabView)itemView.findViewById(R.id.tab2);
        mTabViewDownRight = (NavTabView)itemView.findViewById(R.id.tab3);
        frameLayoutLeft = (FrameLayout)itemView.findViewById(R.id.frame0);
        frameLayoutRight = (FrameLayout)itemView.findViewById(R.id.frame1);
        frameLayoutDownLeft = (FrameLayout)itemView.findViewById(R.id.frame2);
        frameLayoutDownRight = (FrameLayout)itemView.findViewById(R.id.frame3);
        mImageViewLeft = (ImageView)itemView.findViewById(R.id.image0);
        mImageViewRight = (ImageView)itemView.findViewById(R.id.image1);
        mImageViewDownLeft = (ImageView)itemView.findViewById(R.id.image2);
        mImageViewDownRight = (ImageView)itemView.findViewById(R.id.image3);
    }
    public boolean isImageViewLeft(View v){
        return v == mImageViewLeft;
    }
    public boolean isImageViewRight(View v){
        return v == mImageViewRight;
    }
    public boolean isImageViewDownLeft(View v){
        return v == mImageViewDownLeft;
    }
    public boolean isImageViewDownRight(View v){
        return v == mImageViewDownRight;
    }
    //set the webview values for four webview
    public void setDatas(Tab... tab){
        if (tab == null || mTc == null) {
            return;
        }
        if (tab[0] != null) {
            frameLayoutLeft.setVisibility(View.VISIBLE);
            mTabViewLeft.setTabControl(mTc);
            mTabViewLeft.setWebView(tab[0]);
            mTabViewLeft.refeshLayout();//need it while close the last one tab
        }else {
            frameLayoutLeft.setVisibility(View.GONE);
            return;
        }
        if (tab[1] != null) {
            frameLayoutRight.setVisibility(View.VISIBLE);
            mTabViewRight.setTabControl(mTc);
            mTabViewRight.setWebView(tab[1]);
        }else {
            frameLayoutRight.setVisibility(View.GONE);
        }
        if (tab[2] != null) {
            frameLayoutDownLeft.setVisibility(View.VISIBLE);
            mTabViewDownLeft.setTabControl(mTc);
            mTabViewDownLeft.setWebView(tab[2]);
        }else {
            frameLayoutDownLeft.setVisibility(View.GONE);
        }
        if (tab[3] != null) {
            frameLayoutDownRight.setVisibility(View.VISIBLE);
            mTabViewDownRight.setTabControl(mTc);
            mTabViewDownRight.setWebView(tab[3]);
        }else {
            frameLayoutDownRight.setVisibility(View.GONE);
        }
    }

    private TabControl mTc;
    public void setTabControl(TabControl tc){
        mTc = tc;
    }

    public void hideTabViewTitle(){
        mTabViewLeft.hideTabTitle();
        mTabViewRight.hideTabTitle();
        mTabViewDownLeft.hideTabTitle();
        mTabViewDownRight.hideTabTitle();
    }

    public void showTabViewTitle(){
        mTabViewLeft.showTabTitle();
        mTabViewRight.showTabTitle();
        mTabViewDownLeft.showTabTitle();
        mTabViewDownRight.showTabTitle();
    }

    public void changeBitmap() {
        mTabViewLeft.changeBitmap();
        mTabViewRight.changeBitmap();
        mTabViewDownLeft.changeBitmap();
        mTabViewDownRight.changeBitmap();
    }

    @Override
    public void addView(View child) {
        super.addView(child);
    }
    @Override
    public void removeView(View view) {
        super.removeView(view);
    }
    //onclick for four items
    @Override
    public void setOnClickListener(OnClickListener l) {
        mClickListener = l;
        mImageViewLeft.setOnClickListener(mClickListener);
        mImageViewRight.setOnClickListener(mClickListener);
        mImageViewDownLeft.setOnClickListener(mClickListener);
        mImageViewDownRight.setOnClickListener(mClickListener);
    }
}
