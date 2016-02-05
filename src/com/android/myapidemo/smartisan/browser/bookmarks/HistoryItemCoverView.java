
package com.android.myapidemo.smartisan.browser.bookmarks;


import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.BehindItemOption.OptionListener;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HistoryItemCoverView extends LinearLayout implements OptionListener {

    private TextView mTextView;
    private TextView mUrlText;
    CombinedBookmarksCallbacks mCallback;
    private OptionListener mOptionListener;
    private Context mContext;
    private View mCoverView;

    public HistoryItemCoverView(Context context) {
        super(context);
        init(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public HistoryItemCoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     * @return
     */
    public HistoryItemCoverView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context context) {
        mContext = context;
        LayoutInflater factory = LayoutInflater.from(mContext);
        factory.inflate(R.layout.history_item, this);
        mTextView = (TextView) this.findViewById(R.id.title);
        mUrlText = (TextView) this.findViewById(R.id.url);
    }

    public void setOptionListener(OptionListener l) {
        mOptionListener = l;
    }

    public void setActivityCallBack(CombinedBookmarksCallbacks callback) {
        mCallback = callback;
    }

    public void setTitle(String text) {
        if (mTextView != null) {
            mTextView.setText(text);
        }
    }

    @Override
    public String getTitle() {
        return mTextView.getText().toString();
    }

    public void setUrl(String text) {
        if (mUrlText != null) {
            mUrlText.setText(text);
        }
    }

    @Override
    public String getUrl() {
        if (mUrlText != null) {
            return mUrlText.getText().toString();
        }
        return null;
    }

    @Override
    public void editBookmark() {
    }

    @Override
    public void exitAnimation() {
        if (mCallback != null) {
            mCallback.exitAnim();
        }
    }

}
