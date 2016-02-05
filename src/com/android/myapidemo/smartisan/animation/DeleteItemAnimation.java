
package com.android.myapidemo.smartisan.animation;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;

/**
 * help bookmark or history item slide and delete smoothly.
 */
public class DeleteItemAnimation {
    private final static int DURA_ANIM_ALPHA_BEHIND = 120;
    private final static int DURA_ANIM_HORI_COVER = 200;
    private final static int HORI_MOVE_COVER = 369;

    /**
     * @param v the view will do animation
     * @param measureHeight view's actually height.
     */
    public static void slideAnimBehindView(final View view) {
        AlphaAnimation anim = new AlphaAnimation(1, 0);
        anim.setInterpolator(CubicInterpolator.OUT);
        anim.setDuration(DURA_ANIM_ALPHA_BEHIND);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    public static void slideAnimCoverView(final View coverView,
            final AnimationListener al) {
        TranslateAnimation anim = new TranslateAnimation(0, HORI_MOVE_COVER, 0, 0);
        anim.setInterpolator(CubicInterpolator.OUT);
        anim.setDuration(DURA_ANIM_HORI_COVER);
        anim.setFillAfter(true);
        anim.setAnimationListener(al);
        coverView.startAnimation(anim);
    }
}
