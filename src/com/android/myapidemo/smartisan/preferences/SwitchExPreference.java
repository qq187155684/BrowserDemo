/**
 * 
 */

package com.android.myapidemo.smartisan.preferences;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;

import android.R.integer;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author qijin
 */
public class SwitchExPreference extends CheckBoxPreference implements
        OnCheckedChangeListener {

    private CharSequence mTitle;
    //private SwitchEx mSwitchButton;

    private Context mContext;
    private boolean mChecked;

    public static final int BACKGROUND_SINGLE = 0;
    public static final int BACKGROUND_TOP = 1;
    public static final int BACKGROUND_MIDDLE = 2;
    public static final int BACKGROUND_BOTTOM = 3;

    private int mBackGroundType;
    private static final int MAX_LENGTH = 9;
    private SwitchPreferenceChangeListener mChangeListener;

    public static interface SwitchPreferenceChangeListener {
        public void onCheckStateChange(String key);
    }

    /**
     * @param context
     */
    public SwitchExPreference(Context context) {
        super(context);
        // initView();
        mContext = context;

    }

    public SwitchExPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // initView();
        mContext = context;
    }

    public void initView() {
        // setLayoutResource(R.layout.setting_widget_switch);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        super.setSummary(mTitle);
    }

    @Override
    public CharSequence getTitle() {
        if (mTitle != null) {
            return mTitle;
        }
        return super.getTitle();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final LayoutInflater layoutInflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View layout = layoutInflater.inflate(R.layout.browser_preference_switch,
                parent, false);

//        mSwitchButton = (SwitchEx) layout.findViewById(R.id.checkbox);
//
//        mSwitchButton.setChecked(mChecked);
//
//        mSwitchButton.setOnCheckedChangeListener(this);

        initBackground(layout);
        return layout;
    }

    public void setBackgroundType(int type) {
        mBackGroundType = type;
    }

    public void setCheckState(boolean checked) {
        mChecked = checked;
    }

    public void setCheckedChangeListener(SwitchPreferenceChangeListener listener) {
        mChangeListener = listener;
    }

    public boolean getCheckState() {
        return mChecked;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (getTitle() != null && getTitle().length() > MAX_LENGTH) {
            TextView summaryView = (TextView) view.findViewById(android.R.id.title);
            int px = CommonUtil.dip2px(getContext(), 23);
            ReflectHelper.invokeMethod(summaryView, "setMaxTextSize", new Class[]{float.class}, new Object[]{px});
        }
    }

    public void initBackground(View view) {

        if (view == null) {
            return;
        }
        switch (mBackGroundType) {
            case BACKGROUND_SINGLE:
                view.setBackgroundResource(R.drawable.sub_item_back_single_selector);
                break;
            case BACKGROUND_TOP:
                view.setBackgroundResource(R.drawable.sub_item_back_top_selector);
                break;
            case BACKGROUND_MIDDLE:
                view.setBackgroundResource(R.drawable.sub_item_back_middle_selector);
                break;
            case BACKGROUND_BOTTOM:
                view.setBackgroundResource(R.drawable.sub_item_back_bottom_selector);
                break;

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton checkbox, boolean checked) {

//        if (mSwitchButton == null || mChangeListener == null) {
//            return;
//        }
//
//        mSwitchButton.setChecked(checked);
        mChecked = checked;
        mChangeListener.onCheckStateChange(getKey());
    }
}
