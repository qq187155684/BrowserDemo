
package com.android.myapidemo.smartisan.navigation;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;
import com.android.myapidemo.smartisan.navigation.CategoryGroupView.OnNavigationInfoClickListener;

import java.util.ArrayList;

public class CategoryChildView extends View implements View.OnClickListener {
    private static final int COLUMN_COUNT_PORTRAIT = 3;
    private static final int COLUMN_COUNT_LANDSCAPE = 5;
    private int count = COLUMN_COUNT_PORTRAIT;
    public CategoryChildView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CategoryChildView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryChildView(Context context) {
        super(context);
        init();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public int getTotalHeight() {
        return totalHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < childItems.size(); i++) {
            childItems.get(i).draw(canvas);
        }
        drawLine(canvas);
    }

    private void drawLine(Canvas canvas) {
        for (int i = 0; i < row; i++) {
            int top = (int) (i * Childheight);
            canvas.drawRect(0, top, totalWidth, top + lineHeigh, linePaint);
        }
        for (int i = 1; i < count; i++) {
            int left = (int) (i * childWidth);
            canvas.drawRect(left, 0, left + lineHeigh, totalHeight, linePaint);
        }

    }

    int lineColor, textColor, textSelectColor;
    Paint textPaint = new Paint();
    Paint linePaint = new Paint();
    int textSize;
    int lineHeigh;
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        count = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ? COLUMN_COUNT_PORTRAIT
                : COLUMN_COUNT_LANDSCAPE;
        setAddNavigationInfo(addNavigationInfo);
        invalidate();
    }

    private void init() {
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        textSize = CommonUtil.sp2px(getContext(), 15);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);
        Childheight = CommonUtil.dip2px(getContext(), 47);
        textColor = getResources().getColor(R.color.browser_history_item_title_color);
        textSelectColor = getResources().getColor(R.color.nav_select_color);
        textPaint.setColor(textColor);
        lineColor = getResources().getColor(R.color.nav_list_line);
        lineHeigh = CommonUtil.dip2px(getContext(), 0.66);
        linePaint.setColor(lineColor);
    }

    private ArrayList<ChildItem> childItems = new ArrayList<CategoryChildView.ChildItem>();
    int row;
    AddNavigationInfo addNavigationInfo;
    public void setAddNavigationInfo(AddNavigationInfo info) {
        addNavigationInfo = info;
        childItems.clear();
        ArrayList<NavigationInfo> navigationInfos = info.getAllNavigationInfos();
        int size = navigationInfos.size();
        row = (size + count - 1) / count;
        for (int i = 0; i < row; i++) {
            for (int k = 0, j = i * count; j < navigationInfos.size() && k < count; j++, k++) {
                ChildItem item = new ChildItem();
                item.row = i;
                item.column = k;
                item.setNavigationInfo(navigationInfos.get(j));
                childItems.add(item);
            }
        }
        totalHeight = 0;
        int row = (childItems.size() + count - 1) / count;
        for (int i = 0; i < row; i++) {
            totalHeight += Childheight;
        }
        getLayoutParams().height = totalHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getLayoutParams().height > 0) {
            super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(getLayoutParams().height, MeasureSpec.EXACTLY));
        }
        int measuredHeight = getMeasuredHeight();
        totalWidth = getMeasuredWidth();
        childWidth = totalWidth / count;
    }
    int totalWidth;
    int totalHeight;

    @Override
    public void onClick(View v) {

    }

    ChildItem firstChildItem = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                firstChildItem = getSelectChildItem(event.getX(), event.getY());
                if (firstChildItem != null) {
                    firstChildItem.setPressed(true);
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (firstChildItem != null) {
                    boolean point2Me = firstChildItem.isPoint2Me((int) event.getX(),
                            (int) event.getY());
                    if (!point2Me) {
                        firstChildItem.setPressed(false);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (firstChildItem != null) {
                    firstChildItem.setPressed(false);
                    firstChildItem = null;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (firstChildItem != null) {
                    boolean point2Me = firstChildItem.isPoint2Me((int) event.getX(), (int)
                            event.getY());
                    firstChildItem.setPressed(false);
                    if (point2Me) {
                        if(mListener != null){
                            mListener.onNavigationInfoClick(firstChildItem.info);
                        }
                    }
                    firstChildItem = null;
                }
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    public ChildItem getSelectChildItem(float x, float y) {
        for (int i = 0; i < childItems.size(); i++) {
            ChildItem item = childItems.get(i);
            if (item.isPoint2Me((int) x, (int) y)) {
                return item;
            }
        }
        return null;
    }

    float childWidth;
    float Childheight;

    private class ChildItem {
        boolean isPressed;
        int row;
        int column;
        NavigationInfo info;
        Rect pressedRect;

        public boolean isPressed() {
            return isPressed;
        }

        public void setPressed(boolean isPressed) {
            if (this.isPressed != isPressed) {
                this.isPressed = isPressed;
                invalidate(pressedRect);
            }
        }

        public NavigationInfo getNavigationInfo() {
            return info;
        }

        void setNavigationInfo(NavigationInfo i) {
            info = i;
        }

        boolean isPoint2Me(int x, int y) {
            if (pressedRect == null)
                return false;
            return pressedRect.contains(x, y);
        }

        void draw(Canvas canvas) {
            String title = info.getTitle();
            float titleWidth = textPaint.measureText(title);
            float paddingLeft = (childWidth - titleWidth) / 2;
            float paddingTop = (Childheight - textSize) / 2;
            if (pressedRect == null && childWidth != 0) {
                int left = (int) (column * childWidth);
                int top = (int) (row * Childheight);
                int right = (int) (left + childWidth);
                int bottom = (int) (top + Childheight);
                pressedRect = new Rect(left, top, right, bottom);
            }
            if (isPressed) {
                textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                textPaint.setColor(textSelectColor);
                canvas.drawRect(pressedRect, textPaint);
                textPaint.setColor(textColor);
            }
            if(info.getColor() != -1){
                textPaint.setColor(info.getColor());
            }
            canvas.drawText(title, 0, title.length(), column * childWidth + paddingLeft, row * Childheight
                    + paddingTop + textSize,
                    textPaint);
            textPaint.setColor(textColor);
        }
    }

    OnNavigationInfoClickListener mListener;

    public void setOnNavigationInfoClickListener(OnNavigationInfoClickListener listener) {
        mListener = listener;
    }

    public interface NavigationInfoListener {
        void onNavigatioinInfoClick(NavigationInfo info);
    }
}
