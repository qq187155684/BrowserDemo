/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.myapidemo.smartisan.browser.bookmarks;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.BehindItemOption.OptionListener;
import com.android.myapidemo.smartisan.browser.bookmarks.BookmarksAdapter.FragmentEditListener;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class BookmarkItemCoverView extends RelativeLayout implements OptionListener
{
    private static final int TEXT_SIZE = 48;
    private static final int TEXT_TOP = 105;
    private static final int TEXT_START_POSITION = 286;
    private static final int TEXT_ADD_VALUE = 135;

    private int mCurrentColor;
    private static final TextPaint mPaint = new TextPaint();

    public int mMoveX;
    public int mX;
    private int mTextWidth1 = 0;
    private int mStartWidth1 = 0;
    private int mStartWidth2 = 0;
    private int mEndWidth1 = 0;
    private int mPosition = -1;

    FragmentEditListener mListener;
    private OptionListener mOptionListener;
    public CheckBox mCheckBox;
    public ImageView mIcon;
    public TextView mBookMarkName;
    public ImageView mArrow;
    public ColorStateList mTextColor;
    private Context mContext;
    private String mTextName;
    private String mUrl;
    private boolean mDoingAnimatorState;

    public View mItemView;

    public BookmarkItemCoverView(Context context) {
        super(context);
        mContext = context;
    }

    public BookmarkItemCoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public BookmarkItemCoverView(
            Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mItemView = (RelativeLayout) findViewById(R.id.bookmark_cover);
        mCheckBox = (CheckBox) findViewById(R.id.check_box);
        mIcon = (ImageView) findViewById(R.id.icon);
        mArrow = (ImageView) findViewById(R.id.arrow);
        mBookMarkName = (TextView) findViewById(R.id.label);
        mTextColor = (ColorStateList) mContext.getResources().getColorStateList(
                R.drawable.browser_item_title_text_colorlist);
    }

    public void setItemCallBackListener(FragmentEditListener listener) {
        mListener = listener;
    }

    public String getTitle() {
        if (mBookMarkName != null) {
            return mBookMarkName.getText().toString();
        }
        return null;
    }

    @Override
    protected void drawableStateChanged() {
        mCurrentColor = mTextColor.getColorForState(getDrawableState(), 0);
        super.drawableStateChanged();
    }

    public void setDoingAnimatorState(boolean state){
        mDoingAnimatorState = state;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawText(canvas);
    }

    private void drawText(Canvas canvas) {
        mBookMarkName.setVisibility(View.GONE);
        mPaint.setColor(mCurrentColor);
        mPaint.setTextSize(TEXT_SIZE);
        mPaint.setAntiAlias(true);
        mPaint.setTypeface(mBookMarkName.getTypeface());
        mTextName = mBookMarkName.getText().toString();
        if (TextUtils.isEmpty(mTextName)) {
            return;
        }
        if (mListener.getEditState()) {
            mStartWidth2 = (int) getContext().getResources()
                    .getDimensionPixelSize(R.dimen.bookmark_name_start_edit_width);
            if (mDoingAnimatorState) {
                canvasDrawText(canvas, mStartWidth2 - mMoveX, mMoveX, TEXT_TOP);
            } else {
                canvasDrawText(canvas, mStartWidth2 - TEXT_START_POSITION, TEXT_START_POSITION,
                        TEXT_TOP);
            }
        } else {
            mStartWidth1 = (int) getContext().getResources()
                    .getDimensionPixelSize(R.dimen.bookmark_name_start_over_width);
            mTextWidth1 = (int) getContext().getResources().getDimensionPixelSize(
                    R.dimen.bookmark_name_over_width);
            mEndWidth1 = (int) getContext().getResources()
                    .getDimensionPixelSize(R.dimen.bookmark_name_end_over_width);
            if (BookmarksPageFragment.mFirstDraw) {
                if (BookmarksPageFragment.mRemoveState) {
                    canvasDrawText(canvas, mEndWidth1, mMoveX, TEXT_TOP);
                } else {
                    canvasDrawText(canvas, mTextWidth1, TEXT_START_POSITION - mIcon.getWidth(),
                            TEXT_TOP);
                }
            } else {
                if (mDoingAnimatorState) {
                    canvasDrawText(canvas,mStartWidth1 + mX,mMoveX,TEXT_TOP);
                } else {
                    canvasDrawText(canvas,mStartWidth1 + TEXT_ADD_VALUE,TEXT_START_POSITION-TEXT_ADD_VALUE,TEXT_TOP);
                }
            }
        }
        canvas.save();
    }

    private void canvasDrawText(Canvas canvas, int width, float avail, float where) {
        canvas.drawText(TextUtils.ellipsize(mTextName, mPaint,
                width, TextUtils.TruncateAt.END).toString(), avail,
                where, mPaint);
    }

    public void setPosition(int position){
        mPosition = position;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    @Override
    public String getUrl() {
        return mUrl;
    }

    @Override
    public void editBookmark() {
        mListener.editBookmark(mPosition);
    }

    @Override
    public void exitAnimation() {
    }

}
