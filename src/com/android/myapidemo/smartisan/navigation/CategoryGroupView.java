
package com.android.myapidemo.smartisan.navigation;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.view.RoundedRectLinearLayout;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CategoryGroupView extends RoundedRectLinearLayout implements View.OnClickListener {

    public CategoryGroupView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CategoryGroupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryGroupView(Context context) {
        super(context);
        init();
    }

    private ImageView mArrow;

    private void init() {
        setRadius(32);
        setOrientation(LinearLayout.VERTICAL);
        LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.bottomMargin = CommonUtil.dip2px(getContext(), 3);
        setLayoutParams(params);
        inflate(getContext(), R.layout.category_group, this);
        setBackgroundResource(R.drawable.expand_group_normal);
        mArrow = (ImageView) findViewById(R.id.arrow);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    CategoryChildView mCategoryChildView;

    public void setAddNavigationInfo(AddNavigationInfo addInfo) {
        initGroupView(addInfo);
        mCategoryChildView = new CategoryChildView(getContext());
        mCategoryChildView.setAddNavigationInfo(addInfo);
        addView(mCategoryChildView);
    }

    boolean isDoingAnim;

    public void setExpand(final boolean expand, boolean anim) {
        if (isDoingAnim) {
            return;
        }
        if (!anim) {
            mCategoryChildView.setVisibility(expand ? View.VISIBLE : View.GONE);
            mArrow.setRotation(expand ? -180 : 0);
            return;
        }
        float start = expand ? 0 : -180;
        float end = expand ? -180 : 0;
        ObjectAnimator rotationAnimation = ObjectAnimator.ofFloat(mArrow, View.ROTATION, start, end);
        rotationAnimation.setDuration(250);
        rotationAnimation.setInterpolator(new DecelerateInterpolator(1.0f));
        rotationAnimation.start();
        int startHeight = expand ? 1 : mCategoryChildView.getTotalHeight();
        int endHeight = expand ? mCategoryChildView.getTotalHeight() : 1;
        ExpandAnimation animation = new ExpandAnimation(mCategoryChildView, startHeight, endHeight);
        animation.setDuration(250);
        animation.setInterpolator(new DecelerateInterpolator(1.0f));
        animation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                if (expand) {
                    mCategoryChildView.setVisibility(View.VISIBLE);
                }
                isDoingAnim = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!expand) {
                    mCategoryChildView.setVisibility(View.GONE);
                }
                isDoingAnim = false;
            }
        });
        mCategoryChildView.startAnimation(animation);
    }

    public boolean isExpand() {
        return mCategoryChildView.getVisibility() == View.VISIBLE;
    }

    private void initGroupView(final AddNavigationInfo addInfo) {
        TextView groupTitle = (TextView) findViewById(R.id.group_title);
        final ImageView groupImage = (ImageView) findViewById(R.id.image);
        TextView groupDesc = (TextView)findViewById(R.id.group_desc);
        groupDesc.setText(addInfo.getDesc());
        groupTitle.setText(addInfo.getTitle());
        if (addInfo.getBitmap() == null) {
            Bitmap bitmap = NavigationInfoParser.getInstance(getContext()).parseIcon(addInfo.getIconPath(), addInfo.getIconPath());
            addInfo.setBitmap(bitmap);
            groupImage.setImageBitmap(addInfo.getBitmap());
        } else {
            groupImage.setImageBitmap(addInfo.getBitmap());
        }
        View view = findViewById(R.id.group);
        view.setTag(this);
        view.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.group:
                if (onGroupClickListener != null) {
                    onGroupClickListener.onGroupClick(this);
                }
                break;
            default:
                if (onNavigationInfoClickListener != null && v instanceof TextView) {
                    NavigationInfo info = (NavigationInfo) v.getTag();
                    onNavigationInfoClickListener.onNavigationInfoClick(info);
                }
                break;
        }
    }

    private OnGroupClickListener onGroupClickListener;
    private OnNavigationInfoClickListener onNavigationInfoClickListener;

    public void setOnGroupClickListener(OnGroupClickListener l) {
        onGroupClickListener = l;
    }

    public void setOnNavigationInfoClickListener(OnNavigationInfoClickListener listener) {
        mCategoryChildView.setOnNavigationInfoClickListener(listener);
    }

    public interface OnNavigationInfoClickListener {
        public void onNavigationInfoClick(NavigationInfo info);
    }

    public interface OnGroupClickListener {
        public void onGroupClick(CategoryGroupView categoryGroupView);
    }

    private static class ExpandAnimation extends Animation {
        private int baseHeight;
        private int delta;
        private CategoryChildView view;

        private ExpandAnimation(CategoryChildView v, int startHeight, int endHeight) {
            baseHeight = startHeight;
            delta = endHeight - startHeight;
            view = v;
            view.getLayoutParams().height = startHeight;
            view.requestLayout();
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            if (interpolatedTime < 1.0f) {
                int val = baseHeight + (int) (delta * interpolatedTime);
                view.getLayoutParams().height = val;
                view.requestLayout();
            } else {
                int val = baseHeight + delta;
                view.getLayoutParams().height = val;
                view.requestLayout();
            }
        }
    }
}
