package com.android.myapidemo.smartisan.browser.bookmarks;

import java.lang.reflect.Field;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Scroller;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.bookmarks.BookmarksAdapter.BookmarkHolder;

public class HorizontalScrollListView extends ListView implements
        GestureDetector.OnGestureListener {

    private static String TAG = "HorizontalScrollListView";
    private static boolean DEBUG_SCROLL = true;

    private static int COMPLETE_SCROLL_DURATION = 200;
    private static int RESTORE_SCROLL_DURATION = 150;
    private static int DEL_ANIMATION_DURATION = 200;
    /**
     * Scroll mode enum.
     */
    public static final int SCROLL_MODE_RIGHT = 0;
    public static final int SCROLL_MODE_LEFT = 1;
    public static final int SCROLL_MODE_BOTH = 2;

    private int mScrollMode;

    /**
     * Scroll state enum.
     */
    public final static int IDLE = 0;
    public final static int SCROLLING_RIGHT = 1;
    public final static int SCROLLING_LEFT = 2;
    public final static int SCROLLED_RIGHT = 3;
    public final static int SCROLLED_LEFT = 4;

    private int mScrollState = IDLE;

    private static final int MISS = -1;

    private int mClickScrollHitPos = MISS;

    private boolean mAutoClose = true;
    private boolean mScrollEnabled = false;
    private boolean mRestoreEatBackKey = true;

    private boolean mIsAnimating = false;
    private Scroller mScroller;
    private ScrollRunner mScrollRunner;
    private GestureDetector mDetector;
    private MotionEvent mCancelEvent;
    private View mHintView;
    private View.OnClickListener mLeftBtnsClickListener, mRightBtnsClickListener;

    private int mTouchSlop;
//    private int[] mTempLoc = new int[2];

    private int mLeftBtnsWidth = -1;
    private int mRightBtnsWidth = -1;

    private int mMaxLeftBtnsWidth = -1;
    private int mMaxRightBtnsWidth = -1;

    private float mVelocityFactor = 1F;
    private float mOverflowVelocityFactor = 0.2F;

    private float mScrollFactor = 0.5F;

    private int mScrollHandleId;
    private int mLeftBtnsId;
    private int mRightBtnsId;

    private boolean mIntercept = false;
    private boolean mIsScrollRunning = false;

    private class ScrollRunner implements Runnable {

        @Override
        public void run() {
            boolean isContinue = mScroller.computeScrollOffset();
            if (getScrollHandleView() != null) {
                getScrollHandleView().scrollTo(mScroller.getCurrX(), 0);
            }
            if (isContinue) {
                setBtnsListenerEnabled(false);
                mIsScrollRunning = true;
                post(this);
            } else {
                mIsScrollRunning = false;
                if (getHorizontalScrollX() == 0) {
                    setScrollState(IDLE);
                    setBtnsListenerEnabled(false);
                    setBackgroundViewVisibility(View.INVISIBLE);
                    mHintView = null;
                } else {
                    if (getBtnsView() != null) {
                        setScrollState(getHorizontalScrollX() > 0 ? SCROLLED_LEFT : SCROLLED_RIGHT);
                        setBtnsListenerEnabled(true);
                    }
                }
            }
        }
    }

    public HorizontalScrollListView(Context context) {
        this(context, null);
    }

    public HorizontalScrollListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HorizontalScrollListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.HorizontalScrollListView, 0, 0);

            mScrollEnabled = a.getBoolean(R.styleable.HorizontalScrollListView_scroll_enabled,
                   mScrollEnabled);

            mRestoreEatBackKey = a.getBoolean(
                    R.styleable.HorizontalScrollListView_restore_eat_back_key, mRestoreEatBackKey);

            mLeftBtnsWidth = a.getDimensionPixelSize(
                    R.styleable.HorizontalScrollListView_left_btns_width, mLeftBtnsWidth);
            mRightBtnsWidth = a.getDimensionPixelSize(
                    R.styleable.HorizontalScrollListView_right_btns_width, mRightBtnsWidth);

            mMaxLeftBtnsWidth = a.getDimensionPixelSize(
                    R.styleable.HorizontalScrollListView_max_left_btns_width, mMaxLeftBtnsWidth);
            mMaxRightBtnsWidth = a.getDimensionPixelSize(
                    R.styleable.HorizontalScrollListView_max_right_btns_width, mMaxRightBtnsWidth);

            mVelocityFactor = a.getFloat(R.styleable.HorizontalScrollListView_velocity_factor,
                    mVelocityFactor);
            mOverflowVelocityFactor = a.getFloat(
                    R.styleable.HorizontalScrollListView_overflow_velocity_factor,
                   mOverflowVelocityFactor);
            mScrollFactor = a.getFloat(R.styleable.HorizontalScrollListView_scroll_factor,
                    mScrollFactor);

            mLeftBtnsId = a.getResourceId(R.styleable.HorizontalScrollListView_left_btns_id, 0);
            mRightBtnsId = a.getResourceId(R.styleable.HorizontalScrollListView_right_btns_id, 0);

            mScrollHandleId = a.getResourceId(
                    R.styleable.HorizontalScrollListView_scroll_handle_id, 0);
            mScrollMode = a.getInt(R.styleable.HorizontalScrollListView_scroll_mode,
                    SCROLL_MODE_RIGHT);

            a.recycle();
        }

        mScrollRunner = new ScrollRunner();
        mScroller = new Scroller(context);
        mDetector = new GestureDetector(context, this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mCancelEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0f, 0f, 0, 0f,
                0f, 0, 0);
    }

    public void setAutoClose(boolean autoClose) {
        mAutoClose = autoClose;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mScrollEnabled) {
                    if (restoreScrollState(true)) {
                        if (mRestoreEatBackKey) {
                            return true;
                        }
                    }
                }
                break;

            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            restoreScrollState(false);
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            Field ACTION_STATUS_BAR_CLICKED = Intent.class.getDeclaredField("ACTION_STATUS_BAR_CLICKED");
            IntentFilter clickFilter = new IntentFilter((String) ACTION_STATUS_BAR_CLICKED.get(this));
            getContext().registerReceiver(mReceiver, new IntentFilter(clickFilter));
        } catch(Exception e) {}
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mScrollEnabled) {
            if (isStateScrolled() && isMotionHintView(getBtnsView(), e)) {
                // Let btn's View.OnClickListener handle this MotionEvent.
                // See: mLeftBtnsClickListener & mRightBtnsClickListener.;
                mClickScrollHitPos = MISS;
            } else {
                mClickScrollHitPos = viewIdHitPosition(e, mScrollHandleId);
            }
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (!mScrollEnabled) {
            return false;
        }
        if (mClickScrollHitPos == MISS) {
            return false;
        }
        final int deltaX = (int) (e2.getX() - e1.getX());
        final int deltaY = (int) (e2.getY() - e1.getY());

        if (!isStateScrolling()) {
            if (mScrollMode == SCROLL_MODE_LEFT && deltaX > 0) {
                return false;
            }
            if (mScrollMode == SCROLL_MODE_RIGHT && deltaX < 0) {
                return false;
            }
            if (Math.abs(deltaX) < Math.abs(deltaY)) {
                return false;
            }
            if (Math.abs(deltaX) < mTouchSlop) {
                return false;
            }
            if (Math.abs(distanceX) < Math.abs(distanceY)) {
                return false;
            }
            if (Math.abs(distanceX) < mTouchSlop) {
                return false;
            }
        }
        if (DEBUG_SCROLL) {
            Log.d(TAG, "onScroll mode: " + mScrollMode + ", deltaX: " + deltaX + ", deltaY: " + deltaY + ", distanceX: "
                    + distanceX + ", distanceY: " + distanceY);
        }
        scrolling(mClickScrollHitPos - getHeaderViewsCount(), distanceX);
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (DEBUG_SCROLL){
            log("dispatchTouchEvent");
        }
        boolean ret = super.dispatchTouchEvent(ev);
        if (mAutoClose && isStateScrolled()) {
            onScrolledStateTouchEvent_V2(ev);
            onCancelPendingInputEvents();
        }
        return ret;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (DEBUG_SCROLL){
            log("onTouchEvent");
        }
        if (!mScrollEnabled) {
            return super.onTouchEvent(e);
        }
        if (mIsAnimating || mIsScrollRunning) {
            mIntercept = true;
        }
        mDetector.onTouchEvent(e);

        if (isStateScrolled()) {
            onScrolledStateTouchEvent(e);
            mIntercept = true;
        }

        boolean intercept = mIntercept;

        int action = e.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ensureScrollState(true);
                doActionUpOrCancel();
                break;
            default:
                if (intercept) {
                    super.onTouchEvent(mCancelEvent);
                }
                break;
        }

        if (intercept) {
            onCancelPendingInputEvents();
            doActionUpOrCancel();
            return true;
        }

        return super.onTouchEvent(e);
    }

    private void doActionUpOrCancel() {
        mIntercept = false;
        mClickScrollHitPos = MISS;
    }

    private void onScrolledStateTouchEvent(MotionEvent e) {
        if (isMotionHintView(getBtnsView(), e)) {
            // Let btn's View.OnClickListener handle this MotionEvent.
            // See: mLeftBtnsClickListener & mRightBtnsClickListener.
            return;
        }

        restoreScrollState(true);
    }

    private void onScrolledStateTouchEvent_V2(MotionEvent e) {
        if (!isMotionHintView(getBtnsView(), e)) {
            // Let btn's View.OnClickListener handle this MotionEvent.
            // See: mLeftBtnsClickListener & mRightBtnsClickListener.
            return;
        }

        boolean restoreState = false;
        boolean hint = true;
        int action = e.getAction() & MotionEvent.ACTION_MASK;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                restoreState = !hint;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (hint && !isStateScrolling()) {
                    restoreState = true;
                }
                break;
            default:
                break;
        }
        if (restoreState) {
            restoreScrollState(true);
        }
    }

    private void setBtnsListenerEnabled(boolean enabled) {
        View view = getBtnsView();
        View.OnClickListener listener = isStateLeft() ? mRightBtnsClickListener
                : mLeftBtnsClickListener;

        if (view == null || listener == null) {
            return;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                if (enabled) {
                    group.getChildAt(index).setOnClickListener(listener);
                } else {
                    group.getChildAt(index).setClickable(false);
                }
            }
        } else {
            if (enabled) {
                view.setOnClickListener(listener);
            } else {
                view.setClickable(false);
            }
        }
    }

    private int viewIdHitPosition(MotionEvent e, int id) {
        final int x = (int) e.getX();
        final int y = (int) e.getY();

        int touchPos = pointToPosition(x, y); // includes headers/footers

        final int numHeaders = getHeaderViewsCount();
        final int numFooters = getFooterViewsCount();
        final int count = getCount();

        // We're only interested if the touch was on an
        // item that's not a header or footer.
        if (touchPos != AdapterView.INVALID_POSITION && touchPos >= numHeaders
                && touchPos < (count - numFooters)) {
            final View item = getChildAt(touchPos - getFirstVisiblePosition());
            View view = id == 0 ? item : (View) item.findViewById(id);
            if (isMotionHintView(view, e)) {
                return touchPos;
            }
        }
        return -1;
    }

    private boolean isMotionHintView(final View view, MotionEvent e) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }

        int[] loc = new int[2];

        final int rawX = (int) e.getRawX();
        final int rawY = (int) e.getRawY();
        view.getLocationOnScreen(loc);

        if (rawX > loc[0] && rawY > loc[1] &&
                rawX < loc[0] + view.getWidth() &&
                rawY < loc[1] + view.getHeight()) {
            return true;
        }

        return false;
    }

    private void scrolling(int position, float deltaX) {
        position = position - getHeaderViewsCount() - getFirstVisiblePosition();
        View v = getChildAt(position);
        if (v == null) {
            return;
        }

        final BookmarkHolder holder = (BookmarkHolder) v.getTag();
        if (holder != null && holder.mIsFolder) {
            return;
        }

        if (mHintView != null && mHintView != v) {
            return;
        }
        if (mIntercept && mHintView == null) {
            return;
        }
        super.onTouchEvent(mCancelEvent);

        mHintView = v;
        final int oldScrollX = getHorizontalScrollX();
        if (oldScrollX == 0) {
            setScrollState(deltaX < 0 ? SCROLLING_RIGHT : SCROLLING_LEFT);
        } else {
            setScrollState(oldScrollX < 0 ? SCROLLING_RIGHT : SCROLLING_LEFT);
        }

        final int btnsWidth = getBtnsWidth();
        if (btnsWidth <= 0 || Math.abs(oldScrollX) < btnsWidth) {
            deltaX = deltaX * mVelocityFactor;
        } else {
            deltaX = deltaX * mOverflowVelocityFactor;
        }

        int newScrollX = (int) (deltaX + oldScrollX);

        if (newScrollX > 0 && mScrollMode == SCROLL_MODE_RIGHT) {
            newScrollX = 0;
        }
        if (newScrollX < 0 && mScrollMode == SCROLL_MODE_LEFT) {
            newScrollX = 0;
        }

        final int maxBtnsWidth = getMaxBtnsWidth();
        if (maxBtnsWidth > 0 && Math.abs(newScrollX) > maxBtnsWidth) {
            newScrollX = newScrollX > 0 ? maxBtnsWidth : -maxBtnsWidth;
        }

        if (DEBUG_SCROLL) {
            Log.d(TAG, "oldScrollX: " + oldScrollX);
            Log.d(TAG, "newScrollX: " + newScrollX);
        }

        getScrollHandleView().scrollTo(newScrollX, 0);

        View btnsView = getBtnsView();
        if (btnsView != null) {
            setBackgroundViewVisibility(View.VISIBLE);
        }

    }

    private void horizontalScrolling(int startX, int dx, int duration) {
        removeCallbacks(mScrollRunner);
        mScroller.startScroll(startX, 0, dx, 0, duration);
        post(mScrollRunner);
    }

    /**
     * If configure attrs left_btns_width or right_btns_width, return
     * configuration's value, otherwise return btns view's width.
     *
     * @return
     */
    private int getBtnsWidth() {
        int width = isStateLeft() ? mRightBtnsWidth : mLeftBtnsWidth;
        if (width <= 0) {
            View view = getBtnsView();
            if (view != null) {
                width = view.getWidth();
            }
        }

        return width > 0 ? width : -1;
    }

    private int getMaxBtnsWidth() {
        int maxWidth = isStateLeft() ? mMaxRightBtnsWidth : mMaxLeftBtnsWidth;

        return maxWidth > 0 ? maxWidth : -1;
    }

    public boolean completeScrollState() {
        if (mHintView == null) {
            return false;
        }

        final int btnsWidth = getBtnsWidth();
        if (btnsWidth <= 0) {
            return false;
        }

        final int startX = getHorizontalScrollX();
        if (startX == 0 && mScrollMode == SCROLL_MODE_BOTH) {
            return false;
        }

        final int distance = btnsWidth - Math.abs(startX);
        final int dx = startX < 0 ? -distance : distance;
        horizontalScrolling(startX, dx, COMPLETE_SCROLL_DURATION);
        return true;
    }

    private void setBackgroundViewVisibility(int visibility) {
        if (mHintView != null) {
            View btnsView = mHintView.findViewById(mLeftBtnsId);
            if (btnsView != null) {
                btnsView.setVisibility(visibility);
            }
        }
    }

    public boolean restoreScrollState(boolean withAnim) {
        if (getHorizontalScrollX() == 0) {
            setBackgroundViewVisibility(View.INVISIBLE);
            setScrollState(IDLE);
            mHintView = null;
            return false;
        }

        final int scrollX = getScrollHandleView().getScrollX();
        horizontalScrolling(scrollX, -scrollX, withAnim ? RESTORE_SCROLL_DURATION : 0);
        return true;
    }

    /**
     * Ensure scroll state(will be restore or scroll complete) when touch event
     * is completed.
     *
     * @return True if restore or complete scroll state.
     */
    public boolean ensureScrollState(boolean withAnim) {
        if (isStateScrolled()) {
            return true;
        }
        if (mHintView != null) {
            final View btnsView = getBtnsView();
            if (btnsView == null
                    || Math.abs(getHorizontalScrollX()) < getBtnsWidth() * mScrollFactor) {
                restoreScrollState(withAnim);
            } else {
                boolean completed = completeScrollState();
                if (!completed) {
                    restoreScrollState(withAnim);
                }
            }

            return true;
        }

        return false;
    }

    private void changeHintView(View view) {
        final int width = getBtnsWidth();
        if (width <= 0) {
            return;
        }

        removeCallbacks(mScrollRunner);
        final int scrollX = isStateLeft() ? width : -width;

        if (getScrollHandleView() != null) {
            setBtnsListenerEnabled(false);
            getScrollHandleView().scrollTo(0, 0);
        }

        mHintView = view;
        setBtnsListenerEnabled(true);
        getScrollHandleView().scrollTo(scrollX, 0);
    }

    public void changeHintView(int position) {
        position = position - getHeaderViewsCount() - getFirstVisiblePosition();
        View view = getChildAt(position);
        changeHintView(view);
    }

    /**
     * @return The item of ListView.
     */
    public View getHintView() {
        return mHintView;
    }

    public View getScrollHandleView() {
        if (mHintView != null) {
            return mScrollHandleId == 0 ? mHintView : mHintView.findViewById(mScrollHandleId);
        }

        return null;
    }

    /**
     * It can be ViewGroup. See attrs left_btns_id or right_btns_id.
     *
     * @return The view below of scroll view.
     */
    public View getBtnsView() {
        if (mHintView == null) {
            return null;
        }
        return mHintView.findViewById(isStateLeft() ? mRightBtnsId : mLeftBtnsId);
    }

    /**
     * Return value is negative when scroll right, is positive when scroll left.
     *
     * @return The left edge of the displayed part of scroll view, in pixels.
     */
    public int getHorizontalScrollX() {
        View v = getScrollHandleView();
        if (v != null) {
            return v.getScrollX();
        }

        return 0;
    }

    public void setScrollEnabled(boolean enabled) {
        mScrollEnabled = enabled;
    }

    public boolean isScrollEnabled() {
        return mScrollEnabled;
    }

    public void setScrollMode(int scrollMode) {
        mScrollMode = scrollMode;
    }

    public int getScrollMode() {
        return mScrollMode;
    }

    public void setScrollState(int state) {
        if (mScrollState == state) {
            return;
        }
        mScrollState = state;
    }

    public int getScrollState() {
        return mScrollState;
    }

    public boolean isStateLeft() {
        return mScrollState == SCROLLED_LEFT || mScrollState == SCROLLING_LEFT;
    }

    public boolean isStateRight() {
        return mScrollState == SCROLLED_RIGHT || mScrollState == SCROLLING_RIGHT;
    }

    public boolean isStateScrolled() {
        return mScrollState == SCROLLED_LEFT || mScrollState == SCROLLED_RIGHT;
    }

    public boolean isStateScrolling() {
        return mScrollState == SCROLLING_LEFT || mScrollState == SCROLLING_RIGHT;
    }

    public boolean isStateIdle() {
        return mScrollState == IDLE;
    }

    public void setBtnsOnClickListener(View.OnClickListener leftBtnsListener,
            View.OnClickListener rightBtnsListener) {
        mLeftBtnsClickListener = leftBtnsListener;
        mRightBtnsClickListener = rightBtnsListener;
    }

    public void playDeleteItemAnimation(final AnimatorListener listener) {
        final View hintView = mHintView;
        if (hintView == null) {
            if (listener != null) {
                listener.onAnimationStart(null);
                listener.onAnimationEnd(null);
            }
            return;
        }

        final View btnsView = getBtnsView();
        final View otherBtnsView = hintView
                .findViewById(isStateLeft() ? mLeftBtnsId : mRightBtnsId);

        final int otherBtnsVisibility = otherBtnsView != null ? otherBtnsView.getVisibility()
                : View.GONE;

        final int fromXDelta = /*getHorizontalScrollX();*/hintView.getScrollX();
        final int toXDelta = isStateLeft() ? hintView.getWidth() : -hintView.getWidth();
        ValueAnimator scrollAnimator = ValueAnimator.ofInt(fromXDelta, toXDelta);
        scrollAnimator.setInterpolator(new DecelerateInterpolator(1.5F));
        scrollAnimator.setDuration(DEL_ANIMATION_DURATION);
        scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (getScrollHandleView() != null) {
                    int currentVal = (Integer) animation.getAnimatedValue();
                    hintView.scrollTo(currentVal, 0);
                }
            }
        });
        scrollAnimator.addListener(new ValueAnimator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimating = true;
                if (listener != null) {
                    listener.onAnimationStart(animation);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                final int height = hintView.getHeight();
                ValueAnimator upAnimator = ValueAnimator.ofInt(height, 1);
                upAnimator.setDuration(DEL_ANIMATION_DURATION);
                upAnimator.setInterpolator(new DecelerateInterpolator(1.5F));
                upAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (hintView != null) {
                            int currentVal = (Integer) animation.getAnimatedValue();
                            hintView.setLayoutParams(new AbsListView.LayoutParams(
                                    AbsListView.LayoutParams.MATCH_PARENT, currentVal));
                        }
                    }
                });
                upAnimator.addListener(new AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        restoreScrollState(false);

                        if (btnsView != null) {
                            if (btnsView instanceof ViewGroup) {
                                ViewGroup btnsGroup = (ViewGroup) btnsView;
                                int btnCount = btnsGroup.getChildCount();
                                for (int index = 0; index < btnCount; index++) {
                                    View btn = btnsGroup.getChildAt(index);
                                    btn.clearAnimation();
                                }
                            } else {
                                btnsView.clearAnimation();
                            }
                        }
                        if (otherBtnsView != null) {
                            otherBtnsView.setVisibility(otherBtnsVisibility);
                        }

                        hintView.scrollTo(fromXDelta, 0);

                        if (listener != null) {
                            listener.onAnimationEnd(animation);
                        }
                        mIsAnimating = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mIsAnimating = false;
                    }
                });
                upAnimator.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });

        if (otherBtnsView != null) {
            otherBtnsView.setVisibility(INVISIBLE);
        }

        if (btnsView != null) {
            AlphaAnimation alphaAnim = new AlphaAnimation(1F, 0F);
            alphaAnim.setDuration(DEL_ANIMATION_DURATION);
            alphaAnim.setFillAfter(true);
            alphaAnim.setInterpolator(new DecelerateInterpolator(1.5F));
            if (btnsView instanceof ViewGroup) {
                ViewGroup btnsGroup = (ViewGroup) btnsView;
                int btnCount = btnsGroup.getChildCount();
                for (int index = 0; index < btnCount; index++) {
                    View btn = btnsGroup.getChildAt(index);
                    if (btn != null && btn.getVisibility() == View.VISIBLE) {
                        btn.startAnimation(alphaAnim);
                    }
                }
            } else {
                btnsView.startAnimation(alphaAnim);
            }
        }

        scrollAnimator.start();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus && isStateScrolled()) {
            restoreScrollState(true);
            onCancelPendingInputEvents();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) {
            restoreScrollState(true);
            onCancelPendingInputEvents();
        }
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        if (SCREEN_STATE_OFF == screenState) {
            restoreScrollState(false);
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) {
            restoreScrollState(true);
            onCancelPendingInputEvents();
        }
    }

    private static void log(String msg) {
        Log.d("HorizontalScrollListView", msg);
    }
}
