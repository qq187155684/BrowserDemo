
package com.android.myapidemo.smartisan.browse;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.platformsupport.WebAddress;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser.NavigationInfoFilter;
import com.android.myapidemo.smartisan.navigation.NavigationInfo;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;
import com.android.myapidemo.smartisan.view.ResizeRelativeLayout;
import com.android.myapidemo.smartisan.view.ResizeRelativeLayout.InputMethodListener;

public class EditNavActivity extends Activity implements OnClickListener {
    private NavAutoCompleteAdapter mAdapter;
    private AutoCompleteTextView mAutoCompleteTextView;
    private EditText mTitle;
    private ImageView mClearAddress, mClearTitle;
    private InputMethodManager mInputManager;
    private QuickBar mQuickBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_nav);
        initResizeLayout();
        initTitleBar();
        initAutoCompleteTextView();
    }

    private void initResizeLayout() {
        ResizeRelativeLayout layout = (ResizeRelativeLayout) findViewById(R.id.resizeLayout);
        mQuickBar = (QuickBar) findViewById(R.id.quick_input_bar);
        layout.setInputMethodListener(new InputMethodListener() {
            @Override
            public void inputMethodStateChange(boolean isShow) {
                mQuickBar.setVisibility(isShow && mAutoCompleteTextView.isFocused() && mIsPortrait ? View.VISIBLE : View.INVISIBLE);
            }
        });
        /*mQuickBar.setOnTextClickListener(new OnTextClickListener() {
            @Override
            public void onTextClick(String text) {
                int start = Math.max(mAutoCompleteTextView.getSelectionStart(), 0);
                int end = Math.max(mAutoCompleteTextView.getSelectionEnd(), 0);
                mAutoCompleteTextView.getText().replace(Math.min(start, end), Math.max(start, end), text,
                        0, text.length());
            }
        });*/
    }

    private void initTitleBar() {
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM);
        View actionBar = LayoutInflater.from(this).inflate(
                R.layout.add_navigation_header, null);
        actionBar.setVisibility(View.VISIBLE);
        getActionBar().setCustomView(
                actionBar,
                new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = (TextView) actionBar.findViewById(R.id.header_title);
        title.setText(R.string.add_nav_title_by_hand);
        TextView cancel = (TextView) actionBar.findViewById(R.id.header_left);
        cancel.setOnClickListener(this);
        cancel.setText(R.string.cancel);
        actionBar.findViewById(R.id.header_right).setVisibility(View.GONE);
        View view = actionBar.findViewById(R.id.header_right_2);
        view.setVisibility(View.VISIBLE);
        view.setOnClickListener(this);
        mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private void initAutoCompleteTextView() {
        mAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.address);
        mAdapter = new NavAutoCompleteAdapter(this);
        mAutoCompleteTextView.setAdapter(mAdapter);
        mAutoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NavigationInfo navigationInfo = mAdapter.getNavigationInfo(position);
                if(navigationInfo != null){
                    mTitle.setText(navigationInfo.getTitle());
                }
            }
        });
        mAutoCompleteTextView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mQuickBar.setVisibility(hasFocus && mIsPortrait ? View.VISIBLE : View.INVISIBLE);
            }
        });
        onConfigurationChanged(getResources().getConfiguration());
        mAutoCompleteTextView.setDropDownHorizontalOffset(-76);
        mTitle = (EditText) findViewById(R.id.title);
        mClearAddress = (ImageView) findViewById(R.id.clear_address_button);
        mClearTitle = (ImageView) findViewById(R.id.clear_title_button);
        mAutoCompleteTextView.addTextChangedListener(new ClearTextWatcher(mClearAddress));
        mTitle.addTextChangedListener(new ClearTextWatcher(mClearTitle));
        mClearAddress.setOnClickListener(this);
        mClearTitle.setOnClickListener(this);
        ReflectHelper.invokeMethod(mAutoCompleteTextView, "setCornerListView",
                new Class[]{ boolean.class }, new Object[] { true });
    }

    private class ClearTextWatcher implements TextWatcher {
        View clearBtn;
        public ClearTextWatcher(View clearBtn) {
            this.clearBtn = clearBtn;
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            clearBtn.setVisibility(s.toString().length() > 0 ? View.VISIBLE: View.GONE);
        }
    }
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void afterTextChanged(Editable s) {
            mClearAddress.setVisibility(mAutoCompleteTextView.toString().length() > 0 ? View.VISIBLE: View.GONE);
            mClearTitle.setVisibility(mTitle.toString().length() > 0 ? View.VISIBLE: View.GONE);
        }
    };

    boolean mIsPortrait = true;
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int intrinsicWidth = getResources().getDrawable(R.drawable.text_field).getIntrinsicWidth() - 33;
        mAutoCompleteTextView.setDropDownWidth(intrinsicWidth);
        requestFullScreen();
        mIsPortrait = (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
        if (!mIsPortrait)
            mQuickBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.header_left:
                finish();
                break;
            case R.id.header_right_2:
                mInputManager.hideSoftInputFromWindow(mAutoCompleteTextView.getWindowToken(), 0);
                String url = mAutoCompleteTextView.getText().toString().trim();
                String title = mTitle.getText().toString().trim();
                if (!Patterns.WEB_URL.matcher(url).matches()) {
                    Toast.makeText(this, R.string.url_not_true, Toast.LENGTH_SHORT).show();
                    return;
                }
                url = URLUtil.guessUrl(url);
                if (TextUtils.isEmpty(title)) {
                    title = makeTitleByUrl(url);
                }
                NavigationInfo info = new NavigationInfo();
                info.setTitle(title);
                info.setUrl(url);
                NavigationInfoParser.getInstance(this).addNavigationInfo(info);
                Intent intent = new Intent(this, BrowserActivity.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.activity_slide_do_nothing,R.anim.slide_down_out);
                break;
            case R.id.clear_address_button:
                mAutoCompleteTextView.setText("");
                break;
            case R.id.clear_title_button:
                mTitle.setText("");
                break;
            default:
                break;
        }
    }

    private String makeTitleByUrl(String url) {
        WebAddress address = null;
        try {
            address = new WebAddress(url);
        } catch (WebAddress.ParseException e) {
        }
        return address != null ? address.getHost() : url;

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_slide_do_nothing, R.anim.slide_down_out);
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
    protected void onResume() {
        super.onResume();
        requestFullScreen();
    }

    private class NavAutoCompleteAdapter extends BaseAdapter implements Filterable {
        private ArrayList<NavigationInfo> infos = new ArrayList<NavigationInfo>();
        private Filter filter;
        private LayoutInflater inflater;

        public NavAutoCompleteAdapter(Context context) {
            filter = new TipFilter();
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return infos.size();
        }

        @Override
        public Object getItem(int position) {
            return infos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.edit_nav_tip_item, null);
            }
            NavigationInfo info = infos.get(position);
            TextView title = (TextView) convertView.findViewById(R.id.text);
            title.setText(info.getUrl());
            return convertView;
        }

        @Override
        public Filter getFilter() {
            return filter;
        }

        public void setInfos(List<NavigationInfo> infos) {
            if (infos != null) {
                this.infos.clear();
                this.infos.addAll(infos);
            }
        }
        public NavigationInfo getNavigationInfo(int pos){
            if (infos != null) {
                return infos.get(pos);
            }
            return null;
        }

    }

    private class TipFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (TextUtils.isEmpty(constraint)) {
                results.count = 0;
                return results;
            }
            List<NavigationInfo> infos = NavigationInfoParser.getInstance(
                    getApplicationContext()).searchNavigationInfos(constraint.toString(),new NavigationInfoFilter() {
                        @Override
                        public boolean onFilter(NavigationInfo info) {
                            return info.isAdded();
                        }
                    });
            if (infos.size() > 3) {
                infos = infos.subList(0, 3);
            }
            results.count = infos.size();
            results.values = infos;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mAdapter.setInfos((List<NavigationInfo>) results.values);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object item) {
            if (item == null) {
                return "";
            }
            return ((NavigationInfo) item).getUrl();
        }
    }
}
