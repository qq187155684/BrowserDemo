package com.android.myapidemo.smartisan.view;

import com.android.myapidemo.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


public class RoundedRectListView extends ListView {
    private boolean isSelectLastVisiablePos;
    private int mPressed = android.R.attr.state_pressed;
    private Rect mBounds = new Rect();
    public RoundedRectListView(Context context) {
        super(context);
    }

    public RoundedRectListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundedRectListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean onTouchEvent = super.onTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int orientation = getResources().getConfiguration().orientation;
                int firstOrLastVisiablePos = (orientation == Configuration.ORIENTATION_LANDSCAPE ? getFirstVisiblePosition()
                        : getLastVisiblePosition());
                int curTouchPos = getChildAtPosition(ev) + getFirstVisiblePosition();
                if (firstOrLastVisiablePos == curTouchPos) {
                    isSelectLastVisiablePos = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                isSelectLastVisiablePos = false;
                break;
            default:
                break;
        }
        return onTouchEvent;
    }

    public int getChildAtPosition(MotionEvent ev) {
        final int count = getChildCount();
        final int touchY = (int) ev.getY();
        int childIdx = 0;
        View selectedChild;
        for (; childIdx < count; childIdx++) {
            selectedChild = getChildAt(childIdx);
            if (selectedChild.getVisibility() == GONE) {
                continue;
            }
            if (touchY >= selectedChild.getTop()
                    && touchY <= selectedChild.getBottom()) {
                return childIdx;
            }
        }
        return -1;
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        super.invalidateDrawable(drawable);
        boolean isPressed = false;
        int[] state = drawable.getCurrent().getState();
        for (int s : state) {
            if (s == mPressed) {
                isPressed = true;
            }
        }
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ((ViewGroup) (((ViewGroup) getParent()).getParent()))
            .setBackgroundResource((isPressed && isSelectLastVisiablePos) ? R.drawable.menu_item_top_press_land
                    : R.drawable.menu_item_top_land);
        } else {
            ((ViewGroup) (((ViewGroup) getParent()).getParent()))
            .setBackgroundResource((isPressed && isSelectLastVisiablePos) ? R.drawable.menu_item_bottom_press
                    : R.drawable.menu_item_bottom);
        }
    }
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //copy from listview, workaround bug 53036
        mBounds.left = getPaddingLeft();
        mBounds.right = getRight() - getLeft() - getPaddingRight();
        Drawable divider = getDivider();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            int nextIndex = i + 1;
            if (i != childCount - 1 && (!getAdapter().isEnabled(i) || !getAdapter().isEnabled(nextIndex))) {
                View child = getChildAt(i);
                mBounds.top = child.getBottom();
                mBounds.bottom = child.getBottom() + getDividerHeight();
                divider.setBounds(mBounds);
                divider.draw(canvas);
            }
        }
    }
}
