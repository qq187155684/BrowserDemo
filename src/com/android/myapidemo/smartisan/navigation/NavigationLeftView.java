
package com.android.myapidemo.smartisan.navigation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.adapter.BaseDynamicGridAdapter;
import com.android.myapidemo.smartisan.animation.CubicInterpolator;
import com.android.myapidemo.smartisan.browse.AddNavigationActivity;
import com.android.myapidemo.smartisan.browse.BackgroundHandler;
import com.android.myapidemo.smartisan.browse.BaseUi;
import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.browse.DownloadFaviconIcon;
import com.android.myapidemo.smartisan.browse.DownloadFaviconTask;
import com.android.myapidemo.smartisan.browse.DownloadFaviconTask.DownloadFaviconIconCallBack;
import com.android.myapidemo.smartisan.browse.IntentHandler.UrlData;
import com.android.myapidemo.smartisan.browse.Tab;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser.AddNavigationInfoListener;
import com.android.myapidemo.smartisan.navigation.DynamicGridView.OnEditModeChangeListener;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class NavigationLeftView extends LinearLayout implements OnItemLongClickListener,
        OnItemClickListener {
    private static final int PORTRAIT_NUM_COLUMN = 3;
    private static final int LANDSCAPE_NUM_COLUMN = 5;
    private DynamicGridView mDynamicGridView;
    private DynmicGridViewAdapter mDynmicGridViewAdapter;
    private ArrayList<NavigationInfo> mInfos;
    private BaseUi mBaseUi;
    private int mNumColumn;
    private long UPDATE_DURATION = 24 * 60 * 60 * 1000;
    private long ANIMATION_DURATION = 200;
    private ViewGroup mParent;
    private Handler mHandler = new Handler();

    public NavigationLeftView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public NavigationLeftView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigationLeftView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initDynamicGridView();
    }

    private void initDynamicGridView() {
        mDynamicGridView = (DynamicGridView) findViewById(R.id.dynamic_grid);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mNumColumn = LANDSCAPE_NUM_COLUMN;
        } else {
            mNumColumn = PORTRAIT_NUM_COLUMN;
        }
        mDynamicGridView.setNumColumns(mNumColumn);
        mDynmicGridViewAdapter = new DynmicGridViewAdapter(getContext(), mNumColumn);
        mInfos = NavigationInfoParser.getInstance(getContext()).parseNavigationInfos();
        mDynamicGridView.setAdapter(mDynmicGridViewAdapter);
        mDynmicGridViewAdapter.set(mInfos);
        mDynmicGridViewAdapter.add(NavigationInfo.EMPTY_NAVIGATION_INFO);
        mDynamicGridView.setOnItemLongClickListener(this);
        mDynamicGridView.setOnItemClickListener(this);
        NavigationInfoParser.getInstance(getContext()).addNavigationInfoListener(mListener);
        if (!CommonUtil.isNetworkAvailable(getContext())) {
            return;
        }
        BackgroundHandler.execute(new Runnable() {
            @Override
            public void run() {
                long lastUpdateTime = BrowserSettings.getInstance().getLastUpdateTime();
                long currentTimeMillis = System.currentTimeMillis();
                long duration = currentTimeMillis - lastUpdateTime;
                if (duration > UPDATE_DURATION || duration < 0) {
                    String content = CommonUtil.downloadContent(getContext(),getContext().getString(R.string.icon_list_url));
                    if (content != null && CommonUtil.isJson(content)) {
                        CommonUtil.saveString(getContext(), content, NavigationInfoParser.ICON_LIST_NAME);
                        NavigationInfoParser.getInstance(getContext()).parseIconList(true);
                        BrowserSettings.getInstance().saveLastUpdateTime(currentTimeMillis);
                        updateNavigationIcons();
                    }
                }
            }
        });
    }

    private HashSet<String> set = new HashSet<String>();
    private void updateNavigationIcons() {
        for (int i = 0; i < mInfos.size(); i++) {
            final NavigationInfo navInfo = mInfos.get(i);
            updateNavigationIcon(navInfo);
        }
    }

    private void updateNavigationIcon(final NavigationInfo navInfo) {
        String md5 = navInfo.getBitmapMd5();
        final String url = navInfo.getUrl();
        final String rootDomain = CommonUtil.getRootDomain(url);
        String tmpMd5 = null;
        String tmpDownloadUrl = null;
        final String domainFix = fixDomainFix(url);
        if(set.contains(domainFix)){
            navInfo.setBitmap(null);
            mHandler.removeCallbacks(updateDynmicGridViewRunnable);
            mHandler.postDelayed(updateDynmicGridViewRunnable, 500);
            return;
        }
        set.add(domainFix);
        File iconFile = DownloadFaviconIcon.getIconFile(domainFix);
        Map<String, String> iconList = NavigationInfoParser.getInstance(getContext()).parseIconList();
        String key = iconList.get(domainFix);
        boolean hasNewIcon = false;
        if (key != null) {
            if (key.equalsIgnoreCase(md5) && iconFile.exists()) {//it's downloaded
                return;
            } else {
                String iconUrl = getContext().getString(R.string.icon_url, domainFix);
                String content = CommonUtil.downloadContent(getContext(), iconUrl);
                if (content != null) {
                    String[] value = parseIconJson(content);
                    if (value[0] != null) {//download icon from our server
                        tmpDownloadUrl = value[0];
                        tmpMd5 = value[1];
                        hasNewIcon = true;
                    }
                }
            }

        }
        if(!hasNewIcon && iconFile.exists()){//if no new icon downloadurl and has old icon, do nothing.
            return;
        }
        File file = DownloadFaviconIcon.getIconFile(rootDomain);
        if (!hasNewIcon && file.exists()) {// if has textIcon,use the it.
            return;
        }
        if (!hasNewIcon) {
            tmpDownloadUrl = DownloadFaviconTask.createFaviconUrl(rootDomain);
        }
        if (tmpDownloadUrl != null) {
            DownloadFaviconTask downloadTask = new DownloadFaviconTask(getContext(), tmpDownloadUrl,
                    mHandler, new DownloadFaviconIconCallBack() {
                        @Override
                        public void receiveBitmap(Bitmap bitmap, String downloadUrl, String md5) {
                            if (md5 == null && bitmap != null) {
                                bitmap = NavigationInfoParser.getInstance(getContext()).parseIcon(null, url, bitmap);
                            }
                            if(bitmap != null){
                                navInfo.setBitmapMd5(md5);
                                DownloadFaviconTask.saveBitmap(bitmap, md5 == null ? rootDomain : domainFix);
                                navInfo.setBitmap(bitmap);
                                mHandler.removeCallbacks(updateDynmicGridViewRunnable);
                                mHandler.postDelayed(updateDynmicGridViewRunnable, 500);
                                BackgroundHandler.execute(saveNavigationRunnable);
                            }
                        }
                    });
            downloadTask.setMd5(tmpMd5);
            BackgroundHandler.execute(downloadTask);
        }
    }
    private String fixDomainFix(String url) {
        final String domain = CommonUtil.getDomain(url);
        final String rootDomain = CommonUtil.getRootDomain(url);
        String domainFix = domain.replaceAll("\\.", "_");
        String rootDomainFix = rootDomain.replaceAll("\\.", "_");
        Map<String, String> iconList = NavigationInfoParser.getInstance(getContext()).parseIconList();
        if(!iconList.containsKey(domainFix)){
            domainFix = rootDomainFix;
        }
        return domainFix;
    }

    private Runnable updateDynmicGridViewRunnable = new Runnable() {
        @Override
        public void run() {
            mDynmicGridViewAdapter.notifyDataSetChanged();
        }
    };
    public void updateUi(){
        mDynmicGridViewAdapter.notifyDataSetChanged();
    }

    private String[] parseIconJson(String content) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(content);
            String url = jsonObject.optString("url");
            String md5 = jsonObject.optString("md5");
            return new String[] {url, md5};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String[] {null, null};
    }

    private AddNavigationInfoListener mListener = new AddNavigationInfoListener() {
        @Override
        public void onAdd(final NavigationInfo info) {
            int index = mDynmicGridViewAdapter.index(NavigationInfo.EMPTY_NAVIGATION_INFO);
            if (index == -1) {
                mDynmicGridViewAdapter.add(info);
            } else {
                mDynmicGridViewAdapter.add(index, info);
            }
            BackgroundHandler.execute(new Runnable() {
                @Override
                public void run() {
                    updateNavigationIcon(info);
                }
            });
        }
    };
    public boolean isInEditMode() {
        return mDynamicGridView != null && mDynamicGridView.isEditMode();
    }

    public void setOnEditModeListener(OnEditModeChangeListener editModeChangeListener) {
        mDynamicGridView.setOnEditModeChangeListener(editModeChangeListener);
    }

    public boolean isEditMode() {
        return mDynamicGridView.isEditMode();
    }

    public DynamicGridView getDynamicGridView() {
        return mDynamicGridView;
    }

    public void stopEditMode() {
        mDynamicGridView.stopEditMode();
        mDynmicGridViewAdapter.setStopEditModeAnim(true);
        postDelayed(new Runnable() {
            @Override
            public void run() {
                mDynmicGridViewAdapter.setStopEditModeAnim(false);
                mDynmicGridViewAdapter.notifyDataSetChanged();
            }
        }, 200);
        BackgroundHandler.execute(saveNavigationRunnable);
    }
    Runnable saveNavigationRunnable = new Runnable() {
        public void run() {
            if(mBaseUi.getActivity().isFinishing()){
                return;
            }
            List<NavigationInfo> items = mDynmicGridViewAdapter.getItems();
            ArrayList<NavigationInfo> arrayList = new ArrayList<NavigationInfo>(items);
            arrayList.remove(NavigationInfo.EMPTY_NAVIGATION_INFO);
            CommonUtil.saveObject(getContext(), arrayList, NavigationInfo.SERIALIZABLE_NAME);
            NavigationInfoParser.getInstance(getContext()).syncCache(arrayList);
        }
    };

    public void setBaseUi(BaseUi baseUi) {
        mBaseUi = baseUi;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mNumColumn = LANDSCAPE_NUM_COLUMN;
        } else {
            mNumColumn = PORTRAIT_NUM_COLUMN;
        }
        if (mDynmicGridViewAdapter != null) {
            mDynamicGridView.setNumColumns(mNumColumn);
            mDynmicGridViewAdapter.setColumnCount(mNumColumn);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mNumColumn == LANDSCAPE_NUM_COLUMN) {
            int action = ev.getAction();
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                mBaseUi.hideSoftKeyboard();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        if (!mDynmicGridViewAdapter.isAddNavButton(position) && !isEditMode()) {
            mBaseUi.getBarBase().hideIME();
            post(new Runnable() {
                @Override
                public void run() {
                    mDynmicGridViewAdapter.setStopEditModeAnim(true);
                    mDynamicGridView.startEditMode(position);
                    post(new Runnable() {
                        public void run() {
                            mDynmicGridViewAdapter.setStopEditModeAnim(false);
                        }
                    });
                }
            });
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mDynmicGridViewAdapter.isAddNavButton(position)) {
            Intent intent = new Intent(getContext(), AddNavigationActivity.class);
            Context context = getContext();
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity)context).overridePendingTransition(R.anim.pop_up_in, R.anim.activity_close_enter_in_call);
            }
            return;
        }
        NavigationInfo info = mDynmicGridViewAdapter.getItem(position);
        if (mBaseUi != null) {
            Tab tab = mBaseUi.getUiController().getCurrentTab();
            UrlData urldata = new UrlData(URLUtil.guessUrl(info.getUrl()));
            mBaseUi.getUiController().reuseTab(tab, urldata);
            tab.setShowHomePage(false);
        }
    }

    class DynmicGridViewAdapter extends BaseDynamicGridAdapter<NavigationInfo> implements OnClickListener {
        private boolean isStopEditModeAnim = false;
        protected DynmicGridViewAdapter(Context context, int columnCount) {
            super(context, columnCount);
        }

        @Override
        public boolean canReorder(int position) {
            return !isAddNavButton(position);
        }

        public boolean isAddNavButton(int position) {
            return getItem(position) == NavigationInfo.EMPTY_NAVIGATION_INFO;
        }

        public void setStopEditModeAnim(boolean isStopEditModeAnim) {
            this.isStopEditModeAnim = isStopEditModeAnim;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final CheeseViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.navigation_left_view_item, parent, false);
                holder = new CheeseViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (CheeseViewHolder) convertView.getTag();
            }
            if (isAddNavButton(position)) {
                holder.shadowMask.setVisibility(View.INVISIBLE);
                holder.image.setImageResource(R.drawable.add_nav_btn_selector);
                holder.image.setAlpha(isEditMode() ? 0.33f : 1f);
                holder.image.setEnabled(!isEditMode());
                holder.delete.setVisibility(View.INVISIBLE);
                holder.titleText.setText("");
            } else {
                final NavigationInfo info = getItem(position);
                if (info.getBitmap() == null) {
                    holder.tag = info.getUrl();
                    final NavigationInfoParser parser = NavigationInfoParser.getInstance(getContext());
                    Bitmap bitmap = parser.parseIcon(info.getIconPath(), info.getUrl());
                    info.setBitmap(bitmap);
                    holder.image.setImageBitmap(info.getBitmap());
                } else {
                    holder.image.setImageBitmap(info.getBitmap());
                }
                int visibility = holder.titleText.getVisibility();
                if (isEditMode()) {
                    holder.titleText.setVisibility(isEditMode() ? View.INVISIBLE : View.VISIBLE);
                }
                if(visibility == View.INVISIBLE && !isEditMode()){
                    holder.titleText.setVisibility(isEditMode() ? View.INVISIBLE : View.VISIBLE);
                }
                holder.image.setAlpha(1f);
                holder.image.setEnabled(true);
                holder.titleText.setText(info.getTitle());
                holder.shadowMask.setVisibility(View.VISIBLE);
                if(!isStopEditModeAnim){
                    holder.delete.setVisibility(isEditMode() ? View.VISIBLE : View.INVISIBLE);
                    if(isEditMode()){
                        holder.nav_delete.setAlpha(1f);
                        holder.nav_delete_mask.setAlpha(1f);
                    }
                }
                holder.delete.setTag(info);
                holder.delete.setOnClickListener(this);
            }
            return convertView;
        }

        private class CheeseViewHolder {
            private TextView titleText;
            private ImageView image;
            private ImageView shadowMask;
            private ViewGroup delete;
            private ImageView nav_delete;
            private ImageView nav_delete_mask;
            private String tag;

            private CheeseViewHolder(View view) {
                titleText = (TextView) view.findViewById(R.id.item_title);
                delete = (ViewGroup)view.findViewById(R.id.delete);
                image = (ImageView) view.findViewById(R.id.item_img);
                shadowMask = (ImageView) view.findViewById(R.id.img_shadow_mask);
                nav_delete = (ImageView) view.findViewById(R.id.nav_delete);
                nav_delete_mask = (ImageView) view.findViewById(R.id.nav_delete_mask);
            }

            @Override
            public String toString() {
                return titleText.getText().toString();
            }
        }

        @Override
        public void onClick(final View v) {
            if (!isEditMode()) {
                return;
            }
            v.setPressed(false);
            v.setClickable(false);
            //all the params copy from actionScript by zhangxuefeng@smartisan.com
            final NavigationInfo info = (NavigationInfo) v.getTag();
            final int originalPosition = mDynmicGridViewAdapter.index(info);
            int childPos = originalPosition - mDynamicGridView.getFirstVisiblePosition();
            View view = mDynamicGridView.getChildAt(childPos);
            View delete = view.findViewById(R.id.nav_delete);
            View deleteMask = view.findViewById(R.id.nav_delete_mask);
            ObjectAnimator rotationAnimation = ObjectAnimator.ofFloat(delete, View.ROTATION, 0, 270);
            rotationAnimation.setDuration(ANIMATION_DURATION);

            ArrayList<View> deletes = new ArrayList<View>();
            deletes.add(delete);
            deletes.add(deleteMask);
            deleteMask.setPivotX(v.getWidth() / 2 + 10);
            deleteMask.setPivotY(v.getHeight() / 2);
            Animator zoomAnimation = CommonUtil.createZoomAnimation(deletes, 1, 0.3f);
            zoomAnimation.setStartDelay(130);
            zoomAnimation.setDuration(110);
            zoomAnimation.setInterpolator(new AccelerateInterpolator(0.9f));

            Animator alphaDeletesAnimation = CommonUtil.createAlphaAnimation(deletes, 1, 0.01f);
            alphaDeletesAnimation.setDuration(ANIMATION_DURATION);
            alphaDeletesAnimation.setInterpolator(new AccelerateInterpolator(0.9f));
            View icon = view.findViewById(R.id.item_img);

            View shadow = view.findViewById(R.id.img_shadow_mask);
            ArrayList<View> icons = new ArrayList<View>();
            ArrayList<View> shadows = new ArrayList<View>();
            icons.add(icon);
            shadows.add(shadow);
            icon.setPivotX(icon.getWidth());
            icon.setPivotY(0);
            shadow.setPivotX(shadow.getWidth() - (shadow.getWidth() - icon.getWidth()) / 2);
            shadow.setPivotY(5);
            Animator alphaIconsAnimations = CommonUtil.createAlphaAnimation(icons, 1, 0);
            alphaIconsAnimations.setDuration(ANIMATION_DURATION);
            Animator alphaShadowsAnimations = CommonUtil.createAlphaAnimation(shadows, 1, 0);
            alphaShadowsAnimations.setDuration(ANIMATION_DURATION);

            Animator zoomIconsAnimations = CommonUtil.createZoomAnimation(icons, 1, 0.1f);
            zoomIconsAnimations.setInterpolator(CubicInterpolator.OUT);
            zoomIconsAnimations.setDuration(ANIMATION_DURATION);

            Animator zoomShadowsAnimations = CommonUtil.createZoomAnimation(shadows, 1, 0.1f);
            zoomShadowsAnimations.setInterpolator(CubicInterpolator.OUT);
            zoomShadowsAnimations.setDuration(ANIMATION_DURATION);
            ArrayList<Animator> animators = new ArrayList<Animator>();
            animators.add(rotationAnimation);
            animators.add(zoomAnimation);
            animators.add(alphaDeletesAnimation);
            animators.add(alphaIconsAnimations);
            animators.add(alphaShadowsAnimations);
            animators.add(zoomIconsAnimations);
            animators.add(zoomShadowsAnimations);
            final AnimatorSet set = new AnimatorSet();
            final AnimatorSet setReverse = new AnimatorSet();
            set.playTogether(animators);
            animators.remove(alphaIconsAnimations);
            setReverse.playTogether(animators);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mDynmicGridViewAdapter.remove(info);
                    int visiblePosition = mDynamicGridView.getLastVisiblePosition();
                    mDynamicGridView.animateSwitchCell(originalPosition, visiblePosition);
                    // sync memory cache
                    NavigationInfoParser.getInstance(getContext()).parseNavigationInfos().remove(info);
                    reverse(setReverse, 0);
                    v.setClickable(true);
                    if (mDynmicGridViewAdapter.getCount() == 1) {
                        postDelayed(new Runnable() {
                            public void run() {
                                stopEditMode();
                            }
                        }, 200);
                    }
                }
            });
            set.start();
        }
    }
    public void reverse(Animator animation, long time) {
        if (animation instanceof ValueAnimator) {
            animation.removeAllListeners();
            animation.setStartDelay(time);
            animation.setDuration(time);
            ((ValueAnimator) animation).reverse();
        } else if (animation instanceof AnimatorSet) {
            ArrayList<Animator> animations = ((AnimatorSet) animation).getChildAnimations();
            for (Animator animator : animations) {
                reverse(animator, time);
            }
        } else if (animation instanceof ObjectAnimator) {
            animation.removeAllListeners();
            animation.setStartDelay(time);
            animation.setDuration(time);
            ((ObjectAnimator) animation).reverse();
        }
    }

    public void onDestory() {
        NavigationInfoParser.getInstance(getContext()).removeNavigationInfoListener(mListener);
    }
}
