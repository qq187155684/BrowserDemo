/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.myapidemo.smartisan.browse;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.preferences.BandwidthPreferencesFragment;
import com.android.myapidemo.smartisan.preferences.DebugPreferencesFragment;

public class BrowserPreferencesPage extends PreferenceActivity implements
        OnClickListener {

    public static final String CURRENT_PAGE = "currentPage";
    public static final String SAVED_INSTANCE_ADAPTER = "save_adapter";

    private List<Header> mHeaders;

    private TextView mActionNewEvent;
    private TextView mBackBtn;
    private View mCancleView;
    private View mDoneActionView;
    private String mTitle;
    private static boolean isStartFragment;
    private HeaderAdapter mAdapter;
    private int mTitleMaxWidth;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // add Item layout
        if (mHeaders != null && mHeaders.size() > 0) {
            mAdapter = new HeaderAdapter(this, mHeaders);
            // mAdapter.notifyDataSetChanged();
            setListAdapter(mAdapter);
        }

        mTitleMaxWidth = getResources().getDimensionPixelSize(R.dimen.actionbar_title_max_width);
        // Show the custom ActionBar view and hide the normal Home icon and
        // title.
        View actionBar = LayoutInflater.from(this).inflate(
                R.layout.browser_settings_actionbar, null);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        getActionBar().setCustomView(actionBar,
                new ActionBar.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        mTitle = getResources().getString(R.string.menu_preferences);

        mActionNewEvent = (TextView) actionBar
                .findViewById(R.id.action_new_event_text);
        mActionNewEvent.setText(getTitle());
        mCancleView = actionBar.findViewById(R.id.action_cancel);
        mCancleView.setOnClickListener(this);
        mBackBtn = (TextView) actionBar.findViewById(R.id.action_back_textview);
        mBackBtn.setVisibility(View.VISIBLE);
        mDoneActionView = actionBar.findViewById(R.id.action_done);
        mDoneActionView.setOnClickListener(this);
        int titleWidth = (int) caculateTextWidth(mActionNewEvent);
        if (getTitle().equals(mTitle)) {
            mDoneActionView.setVisibility(View.VISIBLE);
            mCancleView.setVisibility(View.INVISIBLE);
        } else {
            mCancleView.setVisibility(View.VISIBLE);
            int orientation = getResources().getConfiguration().orientation;
            if (titleWidth >= mTitleMaxWidth && orientation != Configuration.ORIENTATION_LANDSCAPE) {
                mActionNewEvent.setGravity(Gravity.LEFT);
                mActionNewEvent.setGravity(Gravity.CENTER_VERTICAL);
                mDoneActionView.setVisibility(View.GONE);
            } else {
                mDoneActionView.setVisibility(View.INVISIBLE);
            }

        }
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);

        if (BrowserSettings.getInstance().isDebugEnabled()) {
            Header debug = new Header();
            debug.title = getText(R.string.pref_development_title);
            debug.fragment = DebugPreferencesFragment.class.getName();
            target.add(debug);
        }
        mHeaders = target;
        setContentView(R.layout.settings);
    }

    @Override
    public Header onGetInitialHeader() {
        String action = getIntent().getAction();
        if (Intent.ACTION_MANAGE_NETWORK_USAGE.equals(action)) {
            String fragName = BandwidthPreferencesFragment.class.getName();
            for (Header h : mHeaders) {
                if (fragName.equals(h.fragment)) {
                    return h;
                }
            }
        }
        return super.onGetInitialHeader();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    finish();
                }
                return true;
        }

        return false;
    }

    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args,
            int titleRes, int shortTitleRes) {
        isStartFragment = true;
        Intent intent = super.onBuildStartFragmentIntent(fragmentName, args,
                titleRes, shortTitleRes);
        String url = getIntent().getStringExtra(CURRENT_PAGE);
        intent.putExtra(CURRENT_PAGE, url);
        return intent;
    }

    private static final Set<String> sKnownFragments = new HashSet<String>(Arrays.asList(
            "com.android.browser.preferences.GeneralPreferencesFragment",
            "com.android.browser.preferences.PrivacySecurityPreferencesFragment",
            "com.android.browser.preferences.AccessibilityPreferencesFragment",
            "com.android.browser.preferences.AdvancedPreferencesFragment",
            "com.android.browser.preferences.BandwidthPreferencesFragment",
            "com.android.browser.preferences.LabPreferencesFragment"));

    // @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    // Add adapter
    private static class HeaderAdapter extends ArrayAdapter<Header> implements Parcelable {
        private static class HeaderViewHolder {
            ImageView icon;
            TextView title;
            TextView summary;
        }

        private LayoutInflater mInflater;
        private int count = 0;

        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);
            count = objects.size();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        final int titlePosition = 1;
        @Override
        public boolean isEnabled(int position) {
            if (position == titlePosition) return false;
            else return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.browser_setting_items,
                        parent, false);

                if (position == 0) {
                    view.setBackgroundResource(R.drawable.sub_item_back_single_selector);
                } else if (position == titlePosition) {
                    // no background, just a title. but need set the height and margin of it
                    ViewGroup.LayoutParams lp = view.getLayoutParams();
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                    ImageView icon = (ImageView) view.findViewById(R.id.imageview);
                    icon.setVisibility(View.INVISIBLE);
                    lp = icon.getLayoutParams();
                    lp.height = 0;// otherwise can't set less height on view

                    TextView title = (TextView) view.findViewById(R.id.title);
                    title.setTextSize(16);
                    title.setTextColor(0xff7e818b);// it will get wrong color if set the color in res/values/color
                    lp = view.getLayoutParams();
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    final ViewGroup.MarginLayoutParams lpt =(ViewGroup.MarginLayoutParams)title.getLayoutParams();
                    lpt.setMargins(0, 30, 0, 6);
                } else if (position == 2) {
                    //view.setBackgroundResource(R.drawable.sub_item_back_top_selector);
                    view.setBackgroundResource(R.drawable.sub_item_back_single_selector);
                } else if (position == (count - 1)) {
                    view.setBackgroundResource(R.drawable.sub_item_back_bottom_selector);
                } else {
                    view.setBackgroundResource(R.drawable.sub_item_back_middle_selector);
                }
                holder = new HeaderViewHolder();
                holder.icon = (ImageView) view.findViewById(R.id.imageview);
                holder.title = (TextView) view.findViewById(R.id.title);
                holder.summary = (TextView) view.findViewById(R.id.content);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }
            // All view fields must be updated every time, because the view may
            // be recycled
            Header header = getItem(position);
            // holder.icon.setoverridePendingTransitionImageResource(header.iconRes);
            holder.title.setText(header.getTitle(getContext().getResources()));
            CharSequence summary = header.getSummary(getContext().getResources());
            if (!TextUtils.isEmpty(summary)) {
                holder.summary.setVisibility(View.VISIBLE);
                holder.summary.setText(summary);
            } else {
                holder.summary.setVisibility(View.GONE);
            }

            return view;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel arg0, int arg1) {
        }

        public static final Parcelable.Creator<HeaderAdapter> CREATOR =
                new Parcelable.Creator<HeaderAdapter>() {

            @Override
            public HeaderAdapter createFromParcel(Parcel source) {
               return null;
            }

            @Override
            public HeaderAdapter[] newArray(int size) {
               return new HeaderAdapter[size];
            }
        };

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_INSTANCE_ADAPTER, mAdapter);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAdapter = savedInstanceState.getParcelable(SAVED_INSTANCE_ADAPTER);
        if (!isStartFragment) {
            invalidateHeaders();
            setListAdapter(mAdapter);
        }
    }

    public void onResume() {
        super.onResume();
        requestFullScreen();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getTitle().equals(mTitle)) {
            setContentView(R.layout.settings);
        }
        int orientation = getResources().getConfiguration().orientation;
        int titleWidth = (int) caculateTextWidth(mActionNewEvent);
        if (titleWidth >= mTitleMaxWidth) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mActionNewEvent.setGravity(Gravity.CENTER);
                mDoneActionView.setVisibility(View.INVISIBLE);
            } else {
                mActionNewEvent.setGravity(Gravity.LEFT);
                mActionNewEvent.setGravity(Gravity.CENTER_VERTICAL);
                mDoneActionView.setVisibility(View.GONE);
            }
        }
        requestFullScreen();
    }

    public void requestFullScreen() {
        Window win = getWindow();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onClick(View view) {
        if (mDoneActionView == view) {
            finish();
            overridePendingTransition(R.anim.activity_slide_do_nothing,
                    R.anim.slide_down_out);
        } else if (mCancleView == view) {
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!isStartFragment) {
            overridePendingTransition(R.anim.activity_slide_do_nothing,
                    R.anim.slide_down_out);
        } else {
            isStartFragment = false;
        }
    }

    private float caculateTextWidth(TextView tv) {
        Paint paint = new Paint();
        paint.setTextSize(tv.getTextSize());
        CharSequence text = tv.getText();
        if (text == null)
            return 0;
        return paint.measureText(tv.getText().toString());
    }

}
