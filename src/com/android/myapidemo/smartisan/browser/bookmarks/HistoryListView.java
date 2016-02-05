
package com.android.myapidemo.smartisan.browser.bookmarks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.DateSortedExpandableListAdapter.GroupNameChangeListener;

public class HistoryListView extends HorizontalScrollExpandableListView implements OnScrollListener,GroupNameChangeListener {//MultiDeleteAnimation.MultiDeleteAnimationOperator
    // Gesture type
    private static final int GESTURE_NONE = -1;
    private static final int GESTURE_DOWN = 0;
    private static final int GESTURE_TAP = 1;
    private static final int GESTURE_SCROLL_HORIZONTAL = 2;
    private static final int GESTURE_SCROLL_VERTICAL = 3;
    private static final int GESTURE_LONGPRESS = 4;
    private static final int GESTURE_FLING_HORIZONTAL = 5;
    private static final int GESTURE_FLING_VERTICAL = 6;
    private int mGesture = GESTURE_NONE;

    private boolean mGestureScrollingChild;
    private boolean mGestureOnOpenedChild;
    private boolean mGestureIntercepted;
    private boolean mIgnoreMutiTouch;

    private LinearLayout mGroupLayout;
    public int mGroupIndex = -1;
    private OnScrollListener mScrollListener;
    private DateSortedExpandableListAdapter mAdapter;
    private TextView mGroupNameText;
    private GestureDetector mGestureDetector;

    final Rect r = new Rect();
      /**
     * the checkBox's id, which in list item view.
     */
    private int mCheckboxId;
    private SlideListener mListener;
    private boolean mIsChecked = false;
    /**
     * Store the checkBox's position
     */
    private int[] mTempLoc = new int[2];

    private final int MISS = -1;
    private final int SELECT = 1;
    private final int SPECIAL = -2;
    private int mState = MISS;

    private final int DEFAULT_PRE_POSITION = -2;
    /**
     * Store the previous list item position, avoiding execute same operation
     * many times.
     */
    private int mPrePosition = DEFAULT_PRE_POSITION;

    private int mStartX;
    private int mEndX;

    /**
     * used to determine whether the slide select enable or not.
     */
    private boolean mSlideEnable;

    private TouchMonitorListener mTouchMonitorListener;

    /**
     * @param context
     */
    public HistoryListView(Context context) {
        super(context);
        super.setOnScrollListener(this);
        init(context,null);

    }

    /**
     * @param context
     * @param attrs
     */
    public HistoryListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnScrollListener(this);
        init(context,attrs);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public HistoryListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnScrollListener(this);
        init(context,attrs);
    }

    private void init(Context context,AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
//        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderListView, 0, 0);
//        if (a != null) {
//            mSlideEnable = a.getBoolean(R.styleable.SliderListView_slider_enabled, false);
//            a.recycle();
//        }
    }

    public interface SlideListener {
        public void setChecked(int position, boolean isChecked);

        public boolean isChecked(int position);
    }
    
    public interface TouchMonitorListener {
        public void onTouchActionUp();
    }
    
    public void init() {
        setChoiceMode(CHOICE_MODE_SINGLE);
    }

    boolean mInterruptionGesture;
    View mChildUnderGesture;
    private ArrayList<Integer> mIgnoreSecondaryPointers = new ArrayList<Integer>();

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mSlideEnable) {
            boolean ret = super.onInterceptTouchEvent(ev);
            return ret;
        }

        int position = startCheckPosition(ev);
        int action = ev.getActionMasked();

        // if the list item has checkBox and the touch spot is located in it,
        // it will intercept {@link MotionEvent#ACTION_DOWN}.
        if (position > MISS && action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        // if the touch spot can't be located in checkBox, it will invoke
        // {@link super#onInterceptTouchEvent(ev)}.
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mSlideEnable) {
            return super.onTouchEvent(ev);
        }
        int position = startCheckPosition(ev);

        int x = (int) ev.getX();
        int y = (int) ev.getY();
        mCurrPoint.set(x, y);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mDownPoint.set(x, y);
                // if the down point located in header view or other place, but checkbox area.
                // we will do noting, just invoke the super method.
                if (position == SPECIAL || (position == MISS && mState == MISS)) {
                    break;
                }

                onDown(position);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                boolean flag = position == SPECIAL || (position == MISS && mState == MISS);
                if (mState != SELECT && !flag) {
                    // if the down point don't located in checkbox area, then we move to the
                    // checkbox area, we should set the init-state for selection.
                    onDown(position);
                    return true;
                }

                if (mState == SELECT && position == SPECIAL) {
                    return true;
                } else if (flag) {
                    break;
                }

                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                recovery();
                if (mTouchMonitorListener != null) {
                    mTouchMonitorListener.onTouchActionUp();
                }
                break;
        }

        return super.onTouchEvent(ev);
    }

    private int startCheckPosition(MotionEvent ev) {
        return viewIdHitPosition(ev, mCheckboxId);
    }

    private void onDown(int position) {
        // Avoid touch on Scrolling.
        if (mListener == null  || position <= MISS || mPrePosition == position) {
            return;
        }
        mState = SELECT;
        onMoving(position);
    }

    private void onMoving(int position) {
        if (mListener == null || position <= MISS || mPrePosition == position) {
            return;
        }
        mPrePosition = position;
    }

    private void recovery() {
        if (mState == MISS) {
            return;
        }
        mIsChecked = false;
        mState = MISS;
        mPrePosition = DEFAULT_PRE_POSITION;
        mStartX = 0;
        mEndX = 0;
    }
    /**
     * check the touch spot whether be located in checkBox or not.
     *
     * @param ev
     * @param id
     *            the resource id of checkBox.
     * @return if the touch spot is located in checkBox, it will return the
     *         occurred list item's position; Otherwise, it will return
     *         {@link #MISS}
     */
    private int viewIdHitPosition(MotionEvent ev, int id) {

        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        // includes headers/footers
        int touchPos = pointToPosition(x, y);

        final int numHeaders = getHeaderViewsCount();
        final int numFooters = getFooterViewsCount();
        final int count = getCount();

        // We're only interested if the touch was on an item that's not a header
        // or footer.
        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders
          && touchPos < (count - numFooters)) {
            final int rawX = (int) ev.getRawX();


            final int rawY = (int) ev.getRawY();
            final View item = getChildAt(touchPos - getFirstVisiblePosition());

            View checkBox = id == 0 ? null : (View) item.findViewById(id);
            if (checkBox != null && (checkBox.getVisibility() == View.VISIBLE)) {
                // if in select state, we only need consider the distance of X
                // direction.
                if (mState == SELECT && rawX > mStartX && rawX < mEndX) {
                    return touchPos;
                }

                checkBox.getLocationOnScreen(mTempLoc);

                if (rawX > mTempLoc[0] && rawY > mTempLoc[1]
                  && rawX < mTempLoc[0] + checkBox.getWidth()
                  && rawY < mTempLoc[1] + checkBox.getHeight()) {

                    mStartX = mTempLoc[0];
                    mEndX = mTempLoc[0] + checkBox.getWidth();

                    return touchPos;
                }
            } else if (checkBox == null && mState == SELECT && rawX > mStartX && rawX < mEndX) {
                //if the occurred item is headers/footers. it will not scroll and not return it's position.
                return SPECIAL;
            }
        }

        return MISS;
    }

    /**
     * the speed cardinal number, determine the speed's value.
     */
    private final float SPEED_CARDINAL = 0.01f;


    private Point mDownPoint = new Point();
    private Point mCurrPoint = new Point();

    /**
     * the start of the downward select-scroll region.
     */
    private float mDownScrollStartY;

    /**
     * the start of the upward select-scroll region.
     */
    private float mUpScrollStartY;

    private View getChildUnderMotion(MotionEvent ev) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (isChildUnderMotion(child, ev)) {
                return child;
            }
        }
        return null;
    }

    private boolean isChildUnderMotion(View child, MotionEvent ev) {
        int pointerIndex = ev.getActionIndex();
        int x = Math.round(ev.getX(pointerIndex));
        int y = Math.round(ev.getY(pointerIndex));
        child.getHitRect(r);
        return r.contains(x, y);
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mScrollListener = l;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (mAdapter == null) {
            mAdapter = (DateSortedExpandableListAdapter) this.getExpandableListAdapter();
        } else {
            mAdapter.setGroupNameChangeListener(this);
        }

        if (mScrollListener != null)
            mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        Resources res = view.getResources();
        int nptp = res.getDimensionPixelSize(R.dimen.history_group_name_height);
        int ptp = view.getFirstVisiblePosition();
        int ptp1 = view.pointToPosition(0, nptp);
        if (ptp != AdapterView.INVALID_POSITION) {
            HistoryListView qExlist = (HistoryListView) view;
            long pos = qExlist.getExpandableListPosition(ptp);
            long nextPos = qExlist.getExpandableListPosition(ptp1);
            int groupPos = HistoryListView.getPackedPositionGroup(pos);
            int childPos = HistoryListView.getPackedPositionChild(pos);
            Rect r = new Rect();
            if (this.getChildAt(0) != null) {
                this.getChildAt(0).getGlobalVisibleRect(r);
            }
            int groupCount = 0;
            if(mAdapter!= null){
                groupCount  = mAdapter.getGroupCount();
            }
            if (groupPos != mGroupIndex && groupCount != 0) {
                mGroupIndex = groupPos;
                final RelativeLayout fl = (RelativeLayout) getParent();
                String groupName = mAdapter.getGroupName(mGroupIndex);
                if (fl.getChildCount() == 3) {
                    mGroupLayout = (LinearLayout) inflate(getContext(),
                            R.layout.history_header,
                            null);
                    mGroupNameText = (TextView)
                            mGroupLayout.findViewById(R.id.group_name);
                    mGroupNameText.setText(groupName);
                    new Handler().post(new Runnable() {

                        @Override
                        public void run() {
                            fl.addView(mGroupLayout, fl.getChildCount(), new LayoutParams(
                                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                            fl.invalidate();
                            fl.requestLayout();
                        }
                    });
                } else {
                    if (mAdapter != null && mAdapter.getGroupCount() == (mGroupIndex + 1)) {
                        mGroupNameText.setText(R.string.tab_most_visited);
                    } else {
                        mGroupNameText.setText(groupName);
                    }
                    mGroupLayout.setVisibility(View.VISIBLE);
                }
            }
            if (mGroupLayout != null && mGroupLayout.getVisibility() == View.VISIBLE) {
                int nextGroupPos = HistoryListView.getPackedPositionGroup(nextPos);
                if (nextGroupPos != AdapterView.INVALID_POSITION && nextGroupPos != groupPos) {
                    Rect rect = new Rect();
                    if (getChildAt(0) != null && mGroupLayout.getParent() != null) {
                        getChildAt(0).getDrawingRect(rect);
                        ((ViewGroup) mGroupLayout.getParent()).offsetDescendantRectToMyCoords(
                                getChildAt(0), rect);
                        mGroupLayout.setY((float) (rect.bottom - mGroupLayout.getHeight()));
                    }
                } else {
                    mGroupLayout.setY(0);
                }
            }
        }

    }

    public void hideGroupLayout() {
        if (mGroupLayout != null && mGroupLayout.getVisibility() == View.VISIBLE) {
            mGroupLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mScrollListener != null)
            mScrollListener.onScrollStateChanged(view, scrollState);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if (mGroupLayout == null) {
            return;
        }
        if (scrollY < 0) {
            mGroupLayout.setVisibility(View.INVISIBLE);
        } else {
            mGroupLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void changeGroupName(String name) {
        if (mGroupNameText != null) {
            mGroupNameText.setText(name);
        }
    }

    public boolean hasOpenAndCloseIt() {
        if(completeScrollState()){
            restoreScrollState(true);
            return true;

        }else{
            return false;
        }
    }
}
