package com.android.myapidemo.smartisan.browser.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lipeng on 14-9-11.
 */
public class IconColor {
    /**
     * hue map
     * 341 360, 0 - 24 Red
     * 25 - 40 Orange
     * 41 - 68 Yellow
     * 69 - 90 Pale Green
     * 91 - 150 Green
     * 151 - 188 Dark Green
     * 189 - 240 Blue
     * 241 - 280 Purple
     * 281 - 340 Pink
     */

    private static boolean DEBUG_MODE = false;

    public static enum HUE {
        RED, ORANGE, YELLOW, PALE_GREEN, GREEN, DARK_GREEN, BLUE, PURPLE, PINK, WHITE, BLACK, GRAY, MIXED
    };

    public final static HUE[] HUE_ORDER = {
            HUE.RED,
            HUE.ORANGE,
            HUE.YELLOW,
            HUE.GREEN,
            HUE.DARK_GREEN,
            HUE.BLUE,
            HUE.PURPLE,
            HUE.PINK,
            HUE.WHITE,
            HUE.GRAY,
            HUE.BLACK,
            HUE.MIXED
    };

    public static class ColorInfo implements Comparable {
        public String imagePath;
        public int majorColor;
        public int num;
        public int total;
        public float[] hsl = null;
        public double colorRatio;
        public double sortValue;

        private ColorInfo() {}

        public ColorInfo(int color, int pixelNum, int totalPixel) {
            majorColor = color;
            hsl = new float[3];
            RGB2HSL(majorColor, hsl);
            num = pixelNum;
            total = totalPixel;
            colorRatio = (num * 1.0) / (total * 1.0f);

            if(majorColor == Color.WHITE
                    || majorColor == Color.BLACK
                    || majorColor == Color.GRAY) {
                sortValue = colorRatio;
            } else {
                //colorRatio
                double areaSize = 1;
                if (colorRatio < 0.2d) {
                    areaSize = colorRatio;
                }
                sortValue = hsl[1] / (1 - hsl[2]) * areaSize;
            }
        }

        public ColorInfo clone() {
            ColorInfo c = new ColorInfo();
            c.imagePath = imagePath;
            c.majorColor = majorColor;
            c.num = num;
            c.total = total;
            c.hsl = hsl;
            c.colorRatio = colorRatio;
            c.sortValue = sortValue;
            return c;
        }

        private static HashMap<String, String> fieldIndexMap = new HashMap<String, String>();
        static {
            fieldIndexMap.put("majorColor", "1");
            fieldIndexMap.put("num", "2");
            fieldIndexMap.put("total", "3");
        }

        public JSONArray toJson() {
            JSONArray obj = new JSONArray();
            obj.put(majorColor);
            obj.put(num);
            obj.put(total);
            obj.put(imagePath);
            return obj;
        }

        public HUE getHUE() {
            if(majorColor == Color.WHITE) {
                return HUE.WHITE;
            } else if(majorColor == Color.BLACK) {
                return HUE.BLACK;
            } else if(majorColor == Color.GRAY) {
                return HUE.GRAY;
            } else {
                float[] buf = new float[3];
                return getHue(majorColor, buf, true);
            }
        }

        public static ColorInfo toColorInfo(String info) {
            if(info == null) {
                return null;
            }
            try {
                String[] strs = info.split(",");
                if(strs.length == 3) {
                    int mc = Integer.parseInt(strs[0]);
                    int n = Integer.parseInt(strs[1]);
                    int t = Integer.parseInt(strs[2]);
                    return new ColorInfo(mc, n, t);
                }
            } catch (Exception e) {}
            return null;
        }

        @Override
        public int compareTo(Object another) {
            ColorInfo ci = (ColorInfo) another;
            if(majorColor == Color.BLACK
                    || majorColor == Color.WHITE
                    || majorColor == Color.GRAY) {
                if(ci.majorColor != majorColor) {
                    throw new IllegalArgumentException("color is different");
                }
            }
            double f = sortValue - ci.sortValue;
            if(f > 0) {
                return 1;
            } if(f < 0) {
                return -1;
            }
            return 0;
        }

        @Override
        public String toString() {
            return majorColor + "," + num + "," + total;
        }
    }

    public static ColorInfo getMajorColor(Bitmap image) {
        ColorInfo info = null;
        if(image == null) {
            return info;
        }
        int width = image.getWidth();
        int height = image.getHeight();

        int white      = 0;//HUE.WHITE
        int black      = 0;//HUE.BLACK
        int gray       = 0;//HUE.GRAY
        int red        = 0;//HUE.RED
        int orange     = 0;//HUE.ORANGE
        int yellow     = 0;//HUE.YELLOW
        int pale_green = 0;//HUE.PALE_GREEN
        int green      = 0;//HUE.GREEN
        int dark_green = 0;//HUE.DARK_GREEN
        int blue       = 0;//HUE.BLUE
        int purple     = 0;//HUE.PURPLE
        int pink       = 0;//HUE.PINK

        int total = 0;
        float[] hsl_buf = new float[3];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = image.getPixel(i, j);
                HUE hueArea = getHue(pixel, hsl_buf, false);
                if(hueArea == null) {
                    continue;
                }
                total = total + 1;
                switch (hueArea) {
                    case WHITE : {
                        white = white + 1;
                    }
                    break;
                    case BLACK : {
                        black = black + 1;
                    }
                    break;
                    case GRAY : {
                        gray = gray + 1;
                    }
                    break;
                    case RED : {
                        red = red + 1;
                    }
                    break;
                    case ORANGE : {
                        orange = orange + 1;
                    }
                    break;
                    case YELLOW : {
                        yellow = yellow + 1;
                    }
                    break;
                    case PALE_GREEN : {
                        green = green + 1;//pale_green = pale_green + 1;
                    }
                    break;
                    case GREEN : {
                        green = green + 1;
                    }
                    break;
                    case DARK_GREEN : {
                        dark_green = dark_green + 1;
                    }
                    break;
                    case BLUE : {
                        blue = blue + 1;
                    }
                    break;
                    case PURPLE : {
                        purple = purple + 1;
                    }
                    break;
                    case PINK : {
                        pink = pink + 1;
                    }
                    break;
                    default:
                        throw new IllegalArgumentException("getHueIndex err");
                }
            }
        }
        int[] countArr = {
                red, orange, yellow, pale_green, green, dark_green, blue, purple, pink, white, black, gray
        };
        Arrays.sort(countArr);
        int max = countArr[countArr.length - 1];
        HUE mainHue = null;
        if (max == white) {
            mainHue = HUE.WHITE;
        } else if (max == black) {
            mainHue = HUE.BLACK;
        } else if (max == gray) {
            mainHue = HUE.GRAY;
        } else if (max == red) {
            mainHue = HUE.RED;
        } else if (max == orange) {
            mainHue = HUE.ORANGE;
        } else if (max == yellow) {
            mainHue = HUE.YELLOW;
        } else if (max == pale_green) {
            mainHue = HUE.PALE_GREEN;
        } else if (max == green) {
            mainHue = HUE.GREEN;
        } else if (max == dark_green) {
            mainHue = HUE.DARK_GREEN;
        } else if (max == blue) {
            mainHue = HUE.BLUE;
        } else if (max == purple) {
            mainHue = HUE.PURPLE;
        } else if (max == pink) {
            mainHue = HUE.PINK;
        } else {
            throw new IllegalArgumentException("unknown mainHue");
        }

        if (mainHue == HUE.WHITE) {
            info = new ColorInfo(Color.WHITE, white, total);
        } else if (mainHue == HUE.BLACK) {
            info = new ColorInfo(Color.BLACK, black, total);
        }  else if(mainHue == HUE.GRAY) {
            info = new ColorInfo(Color.GRAY, gray, total);
        } else {
            int aCount = 0;
            int rCount = 0;
            int gCount = 0;
            int bCount = 0;
//            float hCount = 0;
//            float sCount = 0;
//            float vCount = 0;
            int count = 0;
//            if (LOG.ENABLE_DEBUG) LOG.e(LOG.A140, "getMajorColor area = " + mainHue.name());
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int pixel = image.getPixel(i, j);
                    int a = (pixel >> 24) & 0xff;
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >>  8) & 0xff;
                    int b = (pixel      ) & 0xff;
                    HUE h = getHue(pixel, hsl_buf, false);
                    if (h == mainHue) {
                        aCount = aCount + a;
                        rCount = rCount + r;
                        gCount = gCount + g;
                        bCount = bCount + b;
                        count = count + 1;
                    }
                }
            }
            int majorColor = Color.argb(aCount / count, rCount / count, gCount / count, bCount / count);
            info = new ColorInfo(majorColor, max, total);
        }
        return info;
    }

    public static HUE getHue(int color, float[] hsl, boolean ignoreSkin) {
        int r = (color >> 16) & 0xff;
        int g = (color >>  8) & 0xff;
        int b = (color      ) & 0xff;
        if((r + g + b) == 0) {
            return null;
        }
        RGB2HSL(color, hsl);
        if(!ignoreSkin && isSkintone(hsl[0], hsl[1], hsl[2])) {
            return null;
        }
        if(isWhite(hsl[0], hsl[1], hsl[2])) {
            return HUE.WHITE;
        } else if(isBlack(hsl[0], hsl[1], hsl[2])) {
            return HUE.BLACK;
        } else if(isGray(hsl[0], hsl[1], hsl[2])) {
            return HUE.GRAY;
        } else {
            float hue = hsl[0];
            HUE h = null;
            if (16 <= hue && hue < 40) {
                h = HUE.ORANGE;
            } else if (40 <= hue && hue < 68) {
                h = HUE.YELLOW;
            } else if (68 <= hue && hue < 90) {
                h = HUE.PALE_GREEN;
            } else if (90 <= hue && hue < 150) {
                h = HUE.GREEN;
            } else if (150 <= hue && hue < 188) {
                h = HUE.DARK_GREEN;
            } else if (188 <= hue && hue < 240) {
                h = HUE.BLUE;
            } else if (240 <= hue && hue < 295) {
                h = HUE.PURPLE;
            } else {
                //340 ~ 360 & 0 ~ 16
                //(295 <=hue< 340）或（340 <=hue< 360   &   saturation < 0.6f)   或   (0 <=hue< 5   &   saturation < 0.6f)
                if (295 <= hue && hue < 340) {
                    h = HUE.PINK;
                } else if (hsl[1] < 0.6f) {
                    if (340 <= hue && hue < 360) {
                        h = HUE.PINK;
                    } else if (0 <= hue && hue < 5) {
                        h = HUE.PINK;
                    }
                }
                if (h == null) {
                    h = HUE.RED;
                }
            }
            if (h == HUE.PALE_GREEN) {
                h = HUE.GREEN;
            }
            return h;
        }
    }

    private static boolean isWhite(float hue, float saturation, float lightness) {
        if (saturation <= 0.15f && lightness >= 0.75f) {
            return true;
        }
        return false;
    }

    private static boolean isBlack(float hue, float saturation, float lightness) {
        if(lightness < 0.08f) {
            return true;
        }
        if(lightness < 0.15f) {
            if(saturation < 0.7f) {
                return true;
            }
        }
        if (saturation < 0.35f && lightness < 0.3f) {
            return true;
        }
        return false;
    }

    private static boolean isGray(float hue, float saturation, float lightness) {
        if(saturation < 0.2f) {
            if(0.3f >= lightness && lightness < 0.75f) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSkintone(float hue, float saturation, float lightness) {
        if(lightness >= 0.85f) {
            if(20 < hue && hue < 68) {
                if(0.06f < saturation && saturation < 0.3f) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void RGB2HSL(int color, float[] hsl) {
        float r = ((color >> 16) & 0xff) / 255.0f;
        float g = ((color >>  8) & 0xff) / 255.0f;
        float b = ((color      ) & 0xff) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float h, s, l;
        if(max == min) {
            h = 0.0f;
        } else if(max == r && g >= b) {
            h = 60.0f * (g - b) / (max - min) + 0.0f;
        } else if(max == r && g < b) {
            h = 60.0f * (g - b) / (max - min) + 360.0f;
        } else if(max == g) {
            h = 60.0f * (b - r) / (max - min) + 120.0f;
        } else {// max == b
            h = 60.0f * (r - g) / (max - min) + 240;
        }
        l = 0.5f * (max + min);
        //calculate s in hsl
//        if(l == 0.0f || max == min) {
//            s = 0;
//        } else if(0 < l && l <= 0.5f) {
//            s = (max - min) / (max + min);
//        } else {// l > 0.5f
//            s = (max - min) / (2 - max - min);
//        }
        //s from hsv
        if(-0.00001f < max && max < 0.00001f) {
            s = 0;
        } else {
            s = (max - min) / max;
        }
        hsl[0] = h;
        hsl[1] = s;
        hsl[2] = l;
    }
}
