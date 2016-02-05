package com.android.myapidemo.smartisan.browse;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.view.PagerAdapter;
import com.android.myapidemo.smartisan.view.ViewPager;
import com.android.myapidemo.smartisan.view.ViewPager.OnPageChangeListener;

import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

/**
 * Viewpager builder for thumbnail Every page has four imageviews Left---Right |
 * | Dleft---Dright
 *
 * @author luzhd
 */
@SuppressLint("NewApi")
public class NavTabViewPagerBuilder extends LinearLayout {

    private LayoutInflater mInflater;
    private ViewPager mViewPager;
    private RadioGroup mDotGroupButton;
    private Context mContext;
    private ViewTabAdapter mViewTabAdapter;
    PhoneUi mUi;
    TabControl mTc;
    private static int ANIM_DURATION = 200;
    private static int LEFT_POSITION = 0;
    private static int RIGHT_POSITION = 1;
    private static int DLEFT_POSITION = 2;
    private static int DRIGHT_POSITION = 3;
    public boolean mIsShowing = true;
    int mCurrentPosition = 0;
    int mCurrentConfigPosition = -1;
    ViewPagerChangedListener mViewPagerChangedListener;
    List<ThumbnailItems> mList = new ArrayList<ThumbnailItems>(4);
    List<ThumbnailItems> mIncogList = new ArrayList<ThumbnailItems>(4);
    SparseArray<ThumbnailItems> mCacheThumbnailItems = new SparseArray<ThumbnailItems>();
    public NavTabViewPagerBuilder(Context context) {
        super(context);
        mContext = context;
        init(context);
    }

    public NavTabViewPagerBuilder(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context);
    }

    public NavTabViewPagerBuilder(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init(context);
    }

    public void init(Context context) {
        mInflater = LayoutInflater.from(context);
        View mainView = mInflater.inflate(R.layout.thumbnail_viewpager, this, true);
        mViewPager = (ViewPager) mainView.findViewById(R.id.viewPager);
        mDotGroupButton = (RadioGroup) mainView
                .findViewById(R.id.dotGroupButton);
    }

    public List<ThumbnailItems> getList() {
        if (mTc.getIncogMode()) {
            return mIncogList;
        } else {
            return mList;
        }
    }

    public void makeViewPager(final PhoneUi ui, TabControl tc) {
        mUi = ui;
        mTc = tc;
        getList().clear();
        mCurrentPosition = 0;
        mDotGroupButton.clearCheck();
        mDotGroupButton.removeAllViews();
        initThumbnailItems(tc);
        mViewTabAdapter = new ViewTabAdapter();
        mViewPager.setAdapter(mViewTabAdapter);
        mViewPager.setOffscreenPageLimit(getList().size());
        mViewPagerChangedListener = new ViewPagerChangedListener();
        mViewPager.setOnPageChangeListener(mViewPagerChangedListener);
        mViewPagerChangedListener.onPageSelected(tc.getCurrentPosition() / 4);
    }

    private void initThumbnailItems(TabControl tc) {
        int tabCount = tc.getTabCount();
        for (int i = 0, j = 0; j < tabCount; i++, j = j + 4) {
            ThumbnailItems thumbnailItems = mCacheThumbnailItems.get(i);
            if (thumbnailItems == null) {
                thumbnailItems = new ThumbnailItems(mContext);
            }
            thumbnailItems.setTabControl(tc);
            thumbnailItems.setDatas(tc.getTab(j + LEFT_POSITION),
                    tc.getTab(j + RIGHT_POSITION),
                    tc.getTab(j + DLEFT_POSITION),
                    tc.getTab(j + DRIGHT_POSITION));
            initDot(i, j);
            getList().add(thumbnailItems);
            mCacheThumbnailItems.put(i, thumbnailItems);
        }
    }

    private void initDot(int tabIndex, int pageIndex) {
        final RadioButton dotButton = new RadioButton(mContext);
        dotButton.setLayoutParams(new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT));
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            dotButton.setPadding(0, 0, 0, -20);
        } else {
            dotButton.setPadding(0, 0, 0, 0);
        }
        if (mTc.getIncogMode()) {
            dotButton.setButtonDrawable(R.drawable.dot_bg_incog);
        } else {
            dotButton.setButtonDrawable(R.drawable.dot_bg);
        }
        dotButton.setId(pageIndex + 1);
        dotButton.setTag(pageIndex + 1);
        final int position = tabIndex;
        dotButton
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        if (isChecked) {
                            mViewPager.setCurrentItem(position);
                        }
                    }
                });
        mDotGroupButton.addView(dotButton);
    }

    public class ViewPagerChangedListener implements
            ViewPager.OnPageChangeListener {

        @Override
        public void onPageSelected(int position) {
            RadioButton radioButton = (RadioButton) mDotGroupButton
                    .getChildAt(position);
            if (radioButton != null) {
                radioButton.setChecked(true);
                mCurrentPosition = position;
                mCurrentConfigPosition = position;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (mViewTabAdapter != null && state == ViewPager.SCROLL_STATE_IDLE) {
                mViewTabAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            // TODO Auto-generated method stub
        }
    }

    protected ThumbnailItems getcurrentTabVIew() {
        if (getList().size() - 1 >= mCurrentPosition) {
            return (ThumbnailItems) getList().get(mCurrentPosition);
        } else {
            return null;
        }
    }

    protected ThumbnailItems getTabView(int pos) {
        if (pos > getList().size() - 1) {
            return null;
        } else {
            return (ThumbnailItems) getList().get(pos);
        }
    }

    // set the current pager
    public void setCurrentView(int current) {
        if (mDotGroupButton != null
                && mDotGroupButton.getChildAt(current / 4) != null) {
            ((RadioButton) mDotGroupButton.getChildAt(current / 4))
                    .setChecked(true);
        }
        if (mViewPager != null) {
            mViewPager.setCurrentItem(current / 4, true);
        }
    }

    // set the current pager
    public void setCurrentItemView(int current) {
        if (mDotGroupButton != null
                && mDotGroupButton.getChildAt(current) != null) {
            ((RadioButton) mDotGroupButton.getChildAt(current))
                    .setChecked(true);
        }
        if (mViewPager != null) {
            mViewPager.setCurrentItem(current, true);
        }
    }

    class ViewTabAdapter extends PagerAdapter implements OnLayoutChangeListener {
        List<ThumbnailItems> mLocalList = getList();

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            // TODO Auto-generated method stub
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
        }

        @Override
        public Object instantiateItem(View container, int position) {
            ((ViewPager) container).addView(getList().get(position));
            if (position != getList().size() - 1) {
                setOnclickforThumbnail(mUi,
                        (ThumbnailItems) getList().get(position),
                        (ThumbnailItems) getList().get(position + 1), position);
            } else {
                setOnclickforThumbnail(mUi,
                        (ThumbnailItems) getList().get(position), null, position);
            }
            return getList().get(position);
        }

        @Override
        public int getCount() {
            if (!getList().equals(mLocalList)) {
                mLocalList = getList();
                notifyDataSetChanged();
            }
            return getList().size();
        }

        @Override
        public int getItemPosition(Object object) {
            // TODO Auto-generated method stub
            if (getList().contains(object)) {
                return POSITION_UNCHANGED;
            } else {
                return POSITION_NONE;
            }
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            // TODO Auto-generated method stub
        }

    }

    public void setOnclickforThumbnail(final PhoneUi pu,
            final ThumbnailItems thumbnailItems,
            final ThumbnailItems thumbnailItemsNext, final int j) {
        // set onclick listener for full screen button
        thumbnailItems.mTabViewLeft.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!mIsShowing) {
                    return;
                }
                mIsShowing = false;
                thumbnailItems.mImageViewLeft.setVisibility(View.GONE);
                pu.hideNavScreen(j * 4 + LEFT_POSITION, true);
            }
        });
        thumbnailItems.mTabViewRight.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!mIsShowing) {
                    return;
                }
                mIsShowing = false;
                thumbnailItems.mImageViewRight.setVisibility(View.GONE);
                pu.hideNavScreen(j * 4 + RIGHT_POSITION, true);
            }
        });
        thumbnailItems.mTabViewDownLeft
                .setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        if (!mIsShowing) {
                            return;
                        }
                        mIsShowing = false;
                        thumbnailItems.mImageViewDownLeft
                                .setVisibility(View.GONE);
                        pu.hideNavScreen(j * 4 + DLEFT_POSITION, true);
                    }
                });
        thumbnailItems.mTabViewDownRight
                .setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        if (!mIsShowing) {
                            return;
                        }
                        mIsShowing = false;
                        thumbnailItems.mImageViewDownRight
                                .setVisibility(View.GONE);
                        pu.hideNavScreen(j * 4 + DRIGHT_POSITION, true);
                    }
                });

        // set onclick listener for delete button
        thumbnailItems.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!mIsShowing) {
                    return;
                }
                mIsShowing = false;
                startDismissAnim(v, thumbnailItems, thumbnailItemsNext, pu, j);
            }
        });
    }

    public void startDismissAnim(View v, final ThumbnailItems thumbnailItems,
            final ThumbnailItems thumbnailItemsNext, PhoneUi pu, int position) {
        if (thumbnailItems.isImageViewLeft(v)) {
            setAnimValues(position * 4 + LEFT_POSITION, thumbnailItems,
                    thumbnailItemsNext, pu);
        } else if (thumbnailItems.isImageViewRight(v)) {
            setAnimValues(position * 4 + RIGHT_POSITION, thumbnailItems,
                    thumbnailItemsNext, pu);
        } else if (thumbnailItems.isImageViewDownLeft(v)) {
            setAnimValues(position * 4 + DLEFT_POSITION, thumbnailItems,
                    thumbnailItemsNext, pu);
        } else if (thumbnailItems.isImageViewDownRight(v)) {
            setAnimValues(position * 4 + DRIGHT_POSITION, thumbnailItems,
                    thumbnailItemsNext, pu);
        }
    }

    FrameLayout frameLayout = null;
    private boolean mHideState;
    public void setAnimValues(final int positon,
            final ThumbnailItems thumbnailItems,
            final ThumbnailItems thumbnailItemsNext, final PhoneUi pu) {
        final UiController uiController = pu.mUiController;
        ArrayList<Animator> animList = new ArrayList<Animator>();
        PropertyValuesHolder pvhX = null;
        PropertyValuesHolder pvhY = null;
        Animator oa = null;
        animList.clear();
        switch (positon % 4) {
        case 0:
            // left thumbnail items,dismiss anim for it
            frameLayout = thumbnailItems.frameLayoutLeft;
            thumbnailItems.frameLayoutRight.setLayerType(
                    View.LAYER_TYPE_HARDWARE, null);
            // fly to left anim
            pvhX = PropertyValuesHolder.ofFloat("x",
                    thumbnailItems.frameLayoutRight.getX(),
                    thumbnailItems.frameLayoutLeft.getX());
            pvhY = PropertyValuesHolder.ofFloat("y",
                    thumbnailItems.frameLayoutRight.getY(),
                    thumbnailItems.frameLayoutRight.getY());
            oa = ObjectAnimator.ofPropertyValuesHolder(
                    thumbnailItems.frameLayoutRight, pvhX, pvhY);
            animList.add(oa);
            // fly to right-up
            thumbnailItems.frameLayoutDownLeft.setLayerType(
                    View.LAYER_TYPE_HARDWARE, null);
            pvhX = PropertyValuesHolder.ofFloat("x",
                    thumbnailItems.frameLayoutDownLeft.getX(),
                    thumbnailItems.frameLayoutRight.getX());
            pvhY = PropertyValuesHolder.ofFloat("y",
                    thumbnailItems.frameLayoutDownLeft.getY(),
                    thumbnailItems.frameLayoutRight.getY());
            oa = ObjectAnimator.ofPropertyValuesHolder(
                    thumbnailItems.frameLayoutDownLeft, pvhX, pvhY);
            animList.add(oa);
            // fly to left
            thumbnailItems.frameLayoutDownRight.setLayerType(
                    View.LAYER_TYPE_HARDWARE, null);
            pvhX = PropertyValuesHolder.ofFloat("x",
                    thumbnailItems.frameLayoutDownRight.getX(),
                    thumbnailItems.frameLayoutDownLeft.getX());
            pvhY = PropertyValuesHolder.ofFloat("y",
                    thumbnailItems.frameLayoutDownRight.getY(),
                    thumbnailItems.frameLayoutDownLeft.getY());
            oa = ObjectAnimator.ofPropertyValuesHolder(
                    thumbnailItems.frameLayoutDownRight, pvhX, pvhY);
            animList.add(oa);
            break;
        case 1:
            // right thumbnailitems
            frameLayout = thumbnailItems.frameLayoutRight;
            thumbnailItems.frameLayoutDownLeft.setLayerType(
                    View.LAYER_TYPE_HARDWARE, null);
            pvhX = PropertyValuesHolder.ofFloat("x",
                    thumbnailItems.frameLayoutDownLeft.getX(),
                    thumbnailItems.frameLayoutRight.getX());
            pvhY = PropertyValuesHolder.ofFloat("y",
                    thumbnailItems.frameLayoutDownLeft.getY(),
                    thumbnailItems.frameLayoutRight.getY());
            oa = ObjectAnimator.ofPropertyValuesHolder(
                    thumbnailItems.frameLayoutDownLeft, pvhX, pvhY);
            animList.add(oa);
            thumbnailItems.frameLayoutDownRight.setLayerType(
                    View.LAYER_TYPE_HARDWARE, null);
            pvhX = PropertyValuesHolder.ofFloat("x",
                    thumbnailItems.frameLayoutDownRight.getX(),
                    thumbnailItems.frameLayoutDownLeft.getX());
            pvhY = PropertyValuesHolder.ofFloat("y",
                    thumbnailItems.frameLayoutDownRight.getY(),
                    thumbnailItems.frameLayoutDownLeft.getY());
            oa = ObjectAnimator.ofPropertyValuesHolder(
                    thumbnailItems.frameLayoutDownRight, pvhX, pvhY);
            animList.add(oa);
            break;
        case 2:
            // down left thumbnail
            frameLayout = thumbnailItems.frameLayoutDownLeft;
            thumbnailItems.frameLayoutDownRight.setLayerType(
                    View.LAYER_TYPE_HARDWARE, null);
            pvhX = PropertyValuesHolder.ofFloat("x",
                    thumbnailItems.frameLayoutDownRight.getX(),
                    thumbnailItems.frameLayoutDownLeft.getX());
            pvhY = PropertyValuesHolder.ofFloat("y",
                    thumbnailItems.frameLayoutDownRight.getY(),
                    thumbnailItems.frameLayoutDownLeft.getY());
            oa = ObjectAnimator.ofPropertyValuesHolder(
                    thumbnailItems.frameLayoutDownRight, pvhX, pvhY);
            animList.add(oa);
            break;
        case 3:
            // down right thunbnial
            frameLayout = thumbnailItems.frameLayoutDownRight;
            break;
        default:
            break;
        }
        AnimatorSet moveAnim = null;
        // anim for dissmiss items
        frameLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofFloat("alpha",
                1.0f, 0f);
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat("scaleX",
                frameLayout.getScaleX(), frameLayout.getScaleX() / 5);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat("scaleY",
                frameLayout.getScaleY(), frameLayout.getScaleY() / 5);
        ObjectAnimator disAnim = ObjectAnimator.ofPropertyValuesHolder(frameLayout,
                pvhAlpha, pvhScaleX, pvhScaleY);
        disAnim.setDuration(ANIM_DURATION);
        ObjectAnimator oaNext = null;
        if (thumbnailItemsNext != null) {
            View tmpView = makeTmpView(thumbnailItemsNext);
            thumbnailItems.addView(tmpView, tmpView.getLayoutParams());
            int screenSize = (int) mContext.getResources().getDimension(
                    R.dimen.thumbnail_delete_value);
            PropertyValuesHolder pvhX0 = PropertyValuesHolder.ofFloat("x",
                    screenSize, thumbnailItems.frameLayoutDownRight.getX());
            PropertyValuesHolder pvhY0 = PropertyValuesHolder.ofFloat("y",
                    thumbnailItems.frameLayoutDownRight.getY(),
                    thumbnailItems.frameLayoutDownRight.getY());
            oaNext = ObjectAnimator.ofPropertyValuesHolder(tmpView,
                    pvhX0, pvhY0);
        }
        moveAnim = new AnimatorSet();
        if (oaNext != null) {
            animList.add(oaNext);
        }
        AnimatorSet animSet = new AnimatorSet();
        if (animList.size() > 0) {
            moveAnim.playTogether(animList);
            moveAnim.setStartDelay(50);
            moveAnim.setDuration(ANIM_DURATION);
            animSet.playTogether(disAnim,moveAnim);
        } else {
            animSet.play(disAnim);
        }
        animSet.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                TabControl tc = uiController.getTabControl();
                if (tc != null && (tc.getTabCount() == 0)) {
                    mHideState = true;
                } else {
                    mHideState = false;
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                if (frameLayout != null && frameLayout.getParent() != null) {
                    frameLayout.setScaleX(1f);
                    frameLayout.setScaleY(1f);
                }
                Tab tab = uiController.getTabControl().getTab(positon);
                if (tab != null) {
                    uiController.closeTab(tab);
                    if (positon % 4 == 0
                            && getList().size() > 1
                            && positon == uiController.getTabControl()
                                    .getTabCount()
                            && mCurrentPosition + 1 == getList().size()) {
                        deleteLastTabAnim(positon, pu, uiController,animation);
                    } else {
                        postBuildPager(pu, uiController.getTabControl(), positon / 4, animation);
                        removeTmpViewIfNeed(thumbnailItemsNext);
                    }
                } else {
                    postBuildPager(pu, uiController.getTabControl(), positon / 4, animation);
                    removeTmpViewIfNeed(thumbnailItemsNext);
                }
                mIsShowing = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // TODO Auto-generated method stub
            }
        });
        animSet.setDuration(300);
        animSet.start();
    }

    private void removeTmpViewIfNeed(final ThumbnailItems thumbnailItemsNext) {
        post(new Runnable() {// we must call it after animator reverse,so we post a Runnable to message loop
            @Override
            public void run() {
                if (thumbnailItemsNext != null) {
                    final Object obj = thumbnailItemsNext.getTag();
                    if (obj != null) {
                        ViewParent parent = ((View) obj).getParent();
                        ((ViewGroup) parent).removeView((View) obj);
                    }
                }
            }
        });
    }

    /**make a view for animation,after the animation,we will call removeTmpViewIfNeed method to remove it*/
    private View makeTmpView(ThumbnailItems thumbnailItemsNext) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) thumbnailItemsNext.frameLayoutLeft.getLayoutParams();
        FrameLayout frameLayout = new FrameLayout(mContext);
        RelativeLayout.LayoutParams parentParams = new RelativeLayout.LayoutParams(layoutParams);
        parentParams.leftMargin = -thumbnailItemsNext.frameLayoutLeft.getWidth();
        frameLayout.setLayoutParams(parentParams);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
                (FrameLayout.LayoutParams) thumbnailItemsNext.mTabViewLeft.getLayoutParams());
        NavTabView navTabView = new NavTabView(mContext);
        navTabView.setLayoutParams(navParams);
        Tab tab = thumbnailItemsNext.mTabViewLeft.getTab();
        if (tab != null) {
            navTabView.showTabTitle();
            navTabView.setTabControl(mTc);
            navTabView.setWebView(tab);
        }

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                (FrameLayout.LayoutParams) thumbnailItemsNext.mImageViewLeft.getLayoutParams());

        ImageView close = new ImageView(mContext);
        close.setBackgroundResource(R.drawable.close_tab_selector);
        close.setLayoutParams(closeParams);
        frameLayout.addView(navTabView, navParams);
        frameLayout.addView(close, closeParams);
        frameLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        frameLayout.setPadding(53, 36, 0, 0);
        if (mTc.getIncogMode()) {
            frameLayout.setBackgroundResource(R.drawable.window_shadow_private);
        } else {
            frameLayout.setBackgroundResource(R.drawable.window_shadow);
        }
        // frameLayout.setPadding(-((RelativeLayout.LayoutParams)thumbnailItemsNext.frameLayoutDownRight.getLayoutParams()).leftMargin,
        // -(((FrameLayout.LayoutParams)thumbnailItemsNext.mImageViewDownRight.getLayoutParams()).bottomMargin
        // - ((RelativeLayout.LayoutParams)thumbnailItemsNext.frameLayoutDownRight.getLayoutParams()).topMargin), 0, 0);
        thumbnailItemsNext.setTag(frameLayout);
        return frameLayout;
    }

    protected void reverse(Animator animation,long time) {
        if(animation instanceof ObjectAnimator){
            animation.removeAllListeners();
            animation.setDuration(time);
            ((ObjectAnimator)animation).reverse();
        }else if(animation instanceof AnimatorSet){
            ArrayList<Animator> animations = ((AnimatorSet)animation).getChildAnimations();
            for(Animator animator: animations){
                reverse(animator, time);
            }
        }
    }
    void postBuildPager(final PhoneUi pUi,final TabControl tc,final int positon,final Animator reverseAnimator){
        postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                makeViewPager(pUi, tc);
                mViewPager.setCurrentItem(positon);
                int tabSize = tc.getList().size();
                /**
                 * open a new tab after all tabs are closed
                 */
                if (tabSize == 0) {
                    mIsShowing = false;
                    pUi.openNewTab();
                }
                /**
                 * Hide the nav screen when only remaining one tab
                 */
                else if (tabSize == 1) {
                    mIsShowing = false;
                    pUi.hideNavScreen(0, true);
                }
                /**
                 * Hide the nav screen when screen rotation
                 */
                else if (mConfigChange) {
                    mIsShowing = false;
                    pUi.hideNavScreen(tabSize - 1, true);
                }
                /**
                 * if the last tab close (it mains tabSize == 0)
                 * the reverseAnimator not need to reverse quickly because of it will auto openNewTab
                 */
                if (reverseAnimator != null) {
                    reverse(reverseAnimator, tabSize == 0 ? reverseAnimator.getDuration() : 0);
                }
            }
        },0);
    }
    public void deleteLastTabAnim(final int position, final PhoneUi pu,
            final UiController ui,final Animator animator) {
        ThumbnailItems preItems = getList().get(position / 4 - 1);
        if (preItems != null) {
            final ObjectAnimator oTran = ObjectAnimator.ofFloat(preItems,
                    "translationX", 0, preItems.getWidth() + 8);
            oTran.setDuration(200);
            oTran.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(final Animator animation) {
                    // TODO Auto-generated method stub
                    super.onAnimationEnd(animation);
                    postBuildPager(pu, ui.getTabControl(), position / 4 - 1, animator);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            reverse(animation, 0);
                        }
                    });
                }

            });
            oTran.start();
        }
    }

    public void finishViewPager() {
        this.removeAllViews();
    }

    private static boolean mConfigChange = false;
    private static final long POST_DELAYED = 500L;
    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            mConfigChange = false;
        }
    };

    @Override
    protected void onConfigurationChanged(Configuration newconfig) {
        mConfigChange = true;
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, POST_DELAYED);
    }

    public void addThumbnailIfNeed(){
        if (!mTc.canCreateNewTab()) {
            return;
        }
        int tabCount = mTc.getTabCount();
        int needSize = tabCount / 4 + 1;// this is our need size
        synchronized (mCacheThumbnailItems) {
            while (needSize > mCacheThumbnailItems.size()) {
                ThumbnailItems thumbnailItems = new ThumbnailItems(mContext);
                mCacheThumbnailItems.put(mCacheThumbnailItems.size(), thumbnailItems);
            }
        }
    }

    public void removeThumbnailIfNeed(){
        int tabCount = mTc.getTabCount();
        int needSize = tabCount / 4 + 1;// this is our need size
        synchronized (mCacheThumbnailItems) {
            while (needSize < mCacheThumbnailItems.size()) {
                mCacheThumbnailItems.remove(mCacheThumbnailItems.size() - 1);
            }
        }
    }
}
