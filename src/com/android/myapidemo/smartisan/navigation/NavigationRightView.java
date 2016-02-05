
package com.android.myapidemo.smartisan.navigation;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.URLUtil;
import android.widget.LinearLayout;

import com.android.myapidemo.smartisan.browse.BaseUi;
import com.android.myapidemo.smartisan.browse.IntentHandler.UrlData;
import com.android.myapidemo.smartisan.browse.Tab;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.navigation.CategoryGroupView.OnGroupClickListener;
import com.android.myapidemo.smartisan.navigation.CategoryGroupView.OnNavigationInfoClickListener;

import java.util.ArrayList;

public class NavigationRightView extends LinearLayout implements OnNavigationInfoClickListener,
        OnGroupClickListener {
    private BaseUi mBaseUi;
    private static final int EXPAND_DEFAULT = 0;
    private int expandIndex = EXPAND_DEFAULT;

    public NavigationRightView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public NavigationRightView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationRightView(Context context) {
        super(context);
    }

    public void setBaseUi(BaseUi baseUi) {
        mBaseUi = baseUi;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mBaseUi.hideSoftKeyboard();
            }
        }
        return super.onTouchEvent(event);
    }

    private void initView() {
        ArrayList<AddNavigationInfo> categoryInfos = NavigationInfoParser.getInstance(getContext())
                .parseCategoryInfos();
        for (int i = 0; i < categoryInfos.size(); i++) {
            CategoryGroupView group = new CategoryGroupView(getContext());
            //addView(group);
            AddNavigationInfo addInfo = categoryInfos.get(i);
            group.setAddNavigationInfo(addInfo);
            group.setExpand(expandIndex == i, false);
            group.setOnGroupClickListener(this);
            group.setOnNavigationInfoClickListener(this);
        }
    }

    @Override
    public void onNavigationInfoClick(NavigationInfo info) {
        if (mBaseUi != null) {
            Tab tab = mBaseUi.getUiController().getCurrentTab();
            UrlData urldata = new UrlData(URLUtil.guessUrl(info.getUrl()));
            mBaseUi.getUiController().reuseTab(tab, urldata);
            tab.setShowHomePage(false);
        }
    }

    @Override
    public void onGroupClick(CategoryGroupView view) {
        view.setExpand(!view.isExpand(), true);
    }
}
