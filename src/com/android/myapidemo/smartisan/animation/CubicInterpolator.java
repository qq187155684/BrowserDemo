
package com.android.myapidemo.smartisan.animation;

import android.view.animation.Interpolator;

public class CubicInterpolator implements Interpolator {

    public static CubicInterpolator IN = new CubicInterpolator() {
        @Override
        public final float getInterpolation(float t) {
            return t * t;
        }

        @Override
        public String toString() {
            return "CubicInterpolator.IN";
        }
    };

    public static final CubicInterpolator OUT = new CubicInterpolator() {
        @Override
        public final float getInterpolation(float t) {
            return -t * (t - 2);
        }

        @Override
        public String toString() {
            return "CubicInterpolator.OUT";
        }
    };

    public static final CubicInterpolator INOUT = new CubicInterpolator() {
        @Override
        public final float getInterpolation(float t) {
            if ((t *= 2) < 1)
                return 0.5f * t * t;
            return -0.5f * ((--t) * (t - 2) - 1);
        }

        @Override
        public String toString() {
            return "CubicInterpolator.INOUT";
        }
    };

    @Override
    public float getInterpolation(float input) {
        // TODO Auto-generated method stub
        // return default IN
        return INOUT.getInterpolation(input);
    }

}
