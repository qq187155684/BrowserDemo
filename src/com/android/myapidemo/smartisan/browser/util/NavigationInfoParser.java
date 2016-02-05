
package com.android.myapidemo.smartisan.browser.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Patterns;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browse.BackgroundHandler;
import com.android.myapidemo.smartisan.browse.DownloadFaviconIcon;
import com.android.myapidemo.smartisan.browse.DownloadFaviconTask;
import com.android.myapidemo.smartisan.navigation.AddNavigationInfo;
import com.android.myapidemo.smartisan.navigation.NavigationInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NavigationInfoParser {
    private static final String QUERY_TOKENIZER_REGEX = "\\s+";
    private static final String ADD_NAV_FILE_NAME = "add_nav_config.json";
    public static final String ICON_LIST_NAME = "icon_list.json";
    private static final String SPLIT_STRING = "@";
    private static NavigationInfoParser mNavigationInfoParser;
    private ArrayList<AddNavigationInfoListener> listeners = new ArrayList<AddNavigationInfoListener>();
    private Context mContext;
    Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = (Bundle)msg.obj;
            NavIconParseListener listener = (NavIconParseListener) bundle.getSerializable(DownloadFaviconIcon.LISTENER);
            Bitmap bitmap = bundle.getParcelable(DownloadFaviconIcon.ICON);
            String tag = (String) bundle.get(DownloadFaviconIcon.TAG);
            listener.onUpdateIcon(bitmap, tag, true);
        }
    };

    public NavigationInfoParser(Context context) {
        mContext = context;
    }

    public static NavigationInfoParser getInstance(Context context) {
        if (mNavigationInfoParser != null) {
            return mNavigationInfoParser;
        }
        synchronized (NavigationInfoParser.class) {
            if (mNavigationInfoParser == null) {
                mNavigationInfoParser = new NavigationInfoParser(context.getApplicationContext());
            }
        }
        return mNavigationInfoParser;
    }

    private ArrayList<NavigationInfo> mNavigationInfos;

    public ArrayList<NavigationInfo> parseNavigationInfos() {
        return parseNavigationInfos(false);
    }

    public void syncCache(ArrayList<NavigationInfo> arrayList) {
        mNavigationInfos = arrayList;
    }
    public ArrayList<NavigationInfo> parseNavigationInfos(boolean refresh) {
        if (mNavigationInfos != null && !refresh) {
            return mNavigationInfos;
        }
        mNavigationInfos = CommonUtil.readObject(mContext, ArrayList.class,
                NavigationInfo.SERIALIZABLE_NAME);
        if (mNavigationInfos != null) {
          //FIXBUG 10718 from bugzilla, OTA not work because the navigationInfo will save in disk, so fix it on this way.
            for (NavigationInfo info : mNavigationInfos) {
                if(info.getUrl() != null && info.getUrl().startsWith("http://www.touch.qunar.com")){
                    info.setUrl("http://touch.qunar.com");
                    BackgroundHandler.execute(new SaveNavigationRunnable(mNavigationInfos));
                    break;
                }
            }
            for (NavigationInfo info : mNavigationInfos) {
                if(info.getUrl() != null && info.getUrl().equals("http://www.baidu.com")){
                    info.setUrl("http://m.baidu.com/?from=1013377a");
                    BackgroundHandler.execute(new SaveNavigationRunnable(mNavigationInfos));
                    break;
                }
            }
            return mNavigationInfos;
        }
        mNavigationInfos = loadDefaultInfos();
        return mNavigationInfos;
    }

    private ArrayList<NavigationInfo> loadDefaultInfos() {
        ArrayList<NavigationInfo> navInfos = new ArrayList<NavigationInfo>();
        String[] array = mContext.getResources().getStringArray(R.array.nav_left_infos);
        for (int i = 0; i < array.length; i++) {
            String[] infos = array[i].split(SPLIT_STRING);
            if (infos.length == NavigationInfo.FIELD_COUNT) {
                NavigationInfo info = new NavigationInfo();
                info.setUrl(infos[NavigationInfo.NAV_URL]);
                info.setTitle(infos[NavigationInfo.NAV_TITLE]);
                info.setIconPath((infos[NavigationInfo.NAV_ICON_PATH]));
                info.setColor(infos[NavigationInfo.NAV_TEXT_COLOR]);
                navInfos.add(info);
            }
        }
        return navInfos;
    }

    public ArrayList<AddNavigationInfo> parseAddNavigationInfos() {
        InputStream input;
        try {
            input = mContext.getAssets().open(ADD_NAV_FILE_NAME);
            String json = CommonUtil.readTextFromStream(input, true);
            ArrayList<AddNavigationInfo> addNavigationInfos = parseAddNavigationInfos(json);
            ArrayList<NavigationInfo> infos = parseNavigationInfos();
            // check the info is added.
            for (int i = 0; i < addNavigationInfos.size(); i++) {
                AddNavigationInfo addNavInfo = addNavigationInfos.get(i);
                for (int j = 0; j < addNavInfo.navigationInfoSize(); j++) {
                    NavigationInfo navigationInfo = addNavInfo.getNavigationInfo(j);
                    if (isAdded(infos, navigationInfo)) {
                        navigationInfo.setAdded(true);
                    }
                }
            }
            return addNavigationInfos;
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return new ArrayList<AddNavigationInfo>();
    }

    private boolean isAdded(ArrayList<NavigationInfo> infos, NavigationInfo navigationInfo) {
        for (int i = 0; i < infos.size(); i++) {
            if (navigationInfo.getUrl().equals(infos.get(i).getUrl())) {
                return true;
            }
        }
        return false;
    }

    private HashMap<Integer, NavigationInfo> websites;

    private ArrayList<AddNavigationInfo> parseAddNavigationInfos(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (websites == null) {
                websites = parseNavigationInfos(jsonObject);
            }
            ArrayList<AddNavigationInfo> addNavigationInfos = parseAddNavigationInfos(jsonObject,
                    websites);
            return addNavigationInfos;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new ArrayList<AddNavigationInfo>();
    }

    private ArrayList<AddNavigationInfo> parseCategoryInfos(JSONObject jsonObject,
            HashMap<Integer, NavigationInfo> maps)
            throws JSONException {
        ArrayList<AddNavigationInfo> addNavigationInfos = new ArrayList<AddNavigationInfo>();
        JSONArray array = jsonObject.getJSONArray(Constants.NAV_CATEGORY_LIST);
        for (int i = 0; i < array.length(); i++) {
            Object object = array.get(i);
            if (object instanceof JSONObject) {
                JSONObject obj = ((JSONObject) object);
                AddNavigationInfo addNavInfo = new AddNavigationInfo();
                addNavInfo.setId(obj.optInt(Constants.NAV_ID, -1));
                addNavInfo.setTitle(obj.optString(Constants.NAV_TITLE));
                addNavInfo.setExpand(obj.optInt(Constants.NAV_IS_EXPAND) == 1);
                addNavInfo.setColor(obj.optString(Constants.NAV_COLOR));
                addNavInfo.setIconPath(obj.optString(Constants.NAV_ICON_PATH));
                addNavInfo.setDesc(obj.optString(Constants.NAV_DESC));
                addNavInfos2AddNavigationInfo(obj, addNavInfo, maps);
                addNavigationInfos.add(addNavInfo);
            }
        }
        return addNavigationInfos;
    }
    public ArrayList<AddNavigationInfo> parseCategoryInfos(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (websites == null) {
                websites = parseNavigationInfos(jsonObject);
            }
            ArrayList<AddNavigationInfo> addNavigationInfos = parseCategoryInfos(jsonObject,
                    websites);
            return addNavigationInfos;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new ArrayList<AddNavigationInfo>();
    }

    public ArrayList<AddNavigationInfo> parseCategoryInfos() {
        String json;
        try {
            InputStream inputStream = mContext.getAssets().open(ADD_NAV_FILE_NAME);
            json = CommonUtil.readTextFromStream(inputStream, true);
            return parseCategoryInfos(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<AddNavigationInfo>();
    }

    private ArrayList<AddNavigationInfo> parseAddNavigationInfos(JSONObject jsonObject,
            HashMap<Integer, NavigationInfo> maps)
            throws JSONException {
        ArrayList<AddNavigationInfo> addNavigationInfos = new ArrayList<AddNavigationInfo>();
        JSONArray array = jsonObject.getJSONArray(Constants.NAV_CATEGORY);
        for (int i = 0; i < array.length(); i++) {
            Object object = array.get(i);
            if (object instanceof JSONObject) {
                JSONObject obj = ((JSONObject) object);
                AddNavigationInfo addNavInfo = new AddNavigationInfo();
                addNavInfo.setId(obj.optInt(Constants.NAV_ID, -1));
                addNavInfo.setTitle(obj.optString(Constants.NAV_TITLE));
                addNavInfo.setExpand(obj.optInt(Constants.NAV_IS_EXPAND) == 1);
                addNavInfo.setColor(obj.optString(Constants.NAV_COLOR));
                addNavInfo.setIconPath(obj.optString(Constants.NAV_ICON_PATH));
                addNavInfo.setDesc(obj.optString(Constants.NAV_DESC));
                addNavInfos2AddNavigationInfo(obj, addNavInfo, maps);
                addNavigationInfos.add(addNavInfo);
            }
        }
        return addNavigationInfos;
    }

    private void addNavInfos2AddNavigationInfo(JSONObject obj, AddNavigationInfo addNavigationInfo,
            HashMap<Integer, NavigationInfo> allNavInfos) throws JSONException {
        JSONArray jsonArray = obj.getJSONArray(Constants.NAV_SITE_LIST);
        for (int i = 0; i < jsonArray.length(); i++) {
            NavigationInfo navigationInfo = allNavInfos.get(jsonArray.optInt(i));
            if (navigationInfo != null) {
                navigationInfo.setAdded(false);//we cache allNavInfos, and must reset this field.
                addNavigationInfo.addNavgationInfo(navigationInfo);
            }
        }
    }

    private HashMap<Integer, NavigationInfo> parseNavigationInfos(JSONObject jsonObject)
            throws JSONException {
        JSONArray array = jsonObject.getJSONArray(Constants.NAV_SITES);
        HashMap<Integer, NavigationInfo> navigationInfos = new HashMap<Integer, NavigationInfo>();
        for (int i = 0; i < array.length(); i++) {
            Object object = array.get(i);
            if (object instanceof JSONObject) {
                JSONObject obj = ((JSONObject) object);
                NavigationInfo info = new NavigationInfo();
                info.setId(obj.optInt(Constants.NAV_ID, -1));
                info.setUrl(obj.optString(Constants.NAV_URL));
                info.setTitle(obj.optString(Constants.NAV_TITLE));
                info.setIconPath(obj.optString(Constants.NAV_ICON_PATH));
                info.setColor(obj.optString(Constants.NAV_COLOR));
                navigationInfos.put(info.getId(), info);
            }
        }
        return navigationInfos;
    }

    public Bitmap parseIcon(String path, String url, NavIconParseListener listener) {
        if (TextUtils.isEmpty(path)) {
            path = "";
        }
        if (path.startsWith("assets/")) {
            try {
                InputStream input = mContext.getAssets().open(path.substring("assets/".length()));
                listener.onUpdateIcon(BitmapFactory.decodeStream(input), url, false);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        if (new File(path).exists()) {
            listener.onUpdateIcon(BitmapFactory.decodeFile(path), url, false);
        } else if (Patterns.WEB_URL.matcher(path).matches()) {
            //if path is url
        } else {

            // if it's a url
            if (url == null) {
                return null;
            }
            String domain = CommonUtil.getRootDomain(url);
            domain = UrlUtils.fixUrl(domain);
//            faviconUrl = faviconUrl + "/favicon.ico";
            Map<String, String> list = NavigationInfoParser.getInstance(mContext).parseIconList();
            Bitmap bitmap = DownloadFaviconIcon.loadBitmap(mContext, domain);
            if (bitmap != null) {
                listener.onUpdateIcon(bitmap, url, true);
            } else {
                listener.onUpdateIcon(createDefaultBitmap(url), url, true);
                Message message = handler.obtainMessage();
                DownloadFaviconIcon downloadFaviconIcon = new DownloadFaviconIcon(mContext, domain, url,
                        message,listener);
                BackgroundHandler.execute(downloadFaviconIcon);
            }
        }
        return null;
    }

    public Bitmap parseIcon(String path, String url, Bitmap favicon) {
        if (TextUtils.isEmpty(path)) {
            path = "";
        }
        if (new File(path).exists()) {
            return BitmapFactory.decodeFile(path);
        } else if (Patterns.WEB_URL.matcher(path).matches()) {
            //if path is url
            return null;
        } else {
            // if it's a url
            if (url == null) {
                return null;
            }
            String domainFix = fixDomainFix(url);
            String rootDomain = CommonUtil.getRootDomain(url);
            Bitmap bitmap = DownloadFaviconTask.loadBitmap(domainFix);
            if(bitmap == null){
                if (path.startsWith("assets/")) {
                    try {
                        InputStream input = mContext.getAssets().open(path.substring("assets/".length()));
                        return BitmapFactory.decodeStream(input);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }else{
                    try {
                        InputStream is = mContext.getResources().getAssets().open("icons/" + domainFix + ".png");
                        bitmap = BitmapFactory.decodeStream(is);
                    } catch (IOException e) {
                        bitmap = DownloadFaviconTask.loadFavIcon(rootDomain);
                        if(bitmap == null){
                            bitmap = DownloadFaviconTask.makeTextIcon(mContext, favicon, url);
                        }
                    }
                }
            }
            return bitmap;
        }
    }


    public Bitmap parseIcon(String path, String url) {
        return parseIcon(path, url, (Bitmap)null);
    }

    private String fixDomainFix(String url) {
        final String domain = CommonUtil.getDomain(url);
        final String rootDomain = CommonUtil.getRootDomain(url);
        String domainFix = domain.replaceAll("\\.", "_");
        String rootDomainFix = domainFix;
        if (rootDomain != null) {
            rootDomainFix = rootDomain.replaceAll("\\.", "_");
            Map<String, String> iconList = NavigationInfoParser.getInstance(mContext).parseIconList();
            if (!iconList.containsKey(domainFix)) {
                domainFix = rootDomainFix;
            }
        }
        return domainFix;
    }

    public interface NavIconParseListener extends Serializable {
        public void onUpdateIcon(Bitmap bitmap,String url, boolean isFavIcon);
    }

    public ArrayList<NavigationInfo> searchNavigationInfos(
            ArrayList<AddNavigationInfo> addNavInfos,
            String queryTexts, NavigationInfoFilter filter) {
        ArrayList<NavigationInfo> queryResult = new ArrayList<NavigationInfo>();
        String[] tokens = queryTexts.toLowerCase().trim().split(QUERY_TOKENIZER_REGEX, 0);
        for (int i = 0; i < addNavInfos.size(); i++) {
            AddNavigationInfo addNavInfo = addNavInfos.get(i);
            for (int j = 0; j < addNavInfo.navigationInfoSize(); j++) {
                NavigationInfo info = addNavInfo.getNavigationInfo(j);
                if (filter != null && filter.onFilter(info)) {
                    continue;
                }
                int matchTitleCount = 0;
                int matchUrlCount = 0;
                int matchDescriptionCount = 0;
                for (String text : tokens) {
                    if (info.getTitle() != null && info.getTitle().contains(text)) {
                        matchTitleCount++;
                    }
                    if (info.getUrl() != null && info.getUrl().contains(text)) {
                        matchUrlCount++;
                    }
                    if (info.getDescription() != null && info.getDescription().contains(text)) {
                        matchDescriptionCount++;
                    }
                }
                if (matchTitleCount == tokens.length || matchUrlCount == tokens.length
                        || matchDescriptionCount == tokens.length) {
                    queryResult.add(info);
                }
            }
        }
        return queryResult;
    }

    public ArrayList<NavigationInfo> searchNavigationInfos(String queryTexts,
            NavigationInfoFilter filter) {
        ArrayList<AddNavigationInfo> addNavInfos = parseAddNavigationInfos();
        return searchNavigationInfos(addNavInfos, queryTexts, filter);
    }

    public ArrayList<NavigationInfo> searchNavigationInfos(String queryTexts) {
        ArrayList<AddNavigationInfo> addNavInfos = parseAddNavigationInfos();
        return searchNavigationInfos(addNavInfos, queryTexts, null);
    }

    public void addNavigationInfo(NavigationInfo info) {
        ArrayList<NavigationInfo> infos = parseNavigationInfos();
        infos.add(info);
        BackgroundHandler.execute(new SaveNavigationRunnable(infos));
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAdd(info);
        }
    }

    private class SaveNavigationRunnable implements Runnable {
        private ArrayList<NavigationInfo> infos;

        public SaveNavigationRunnable(ArrayList<NavigationInfo> infos) {
            this.infos = infos;
        }

        @Override
        public void run() {
            CommonUtil.saveObject(mContext, infos, NavigationInfo.SERIALIZABLE_NAME);
        }
    }
    public Bitmap createDefaultBitmap(String url) {
        return createDefaultBitmap(url, Color.WHITE);
    }
    public Bitmap createDefaultBitmap(String url, int color) {//this code will refact
        Resources resources = mContext.getResources();
//        if (mBitmap == null) {
//            mBitmap = ((BitmapDrawable) resources.getDrawable(R.drawable.jd)).getBitmap();
//        }
//        int bitmapWidth = resources.getDimensionPixelSize(R.dimen.nav_icon_width);
//        int bitmapHeight = resources.getDimensionPixelSize(R.dimen.nav_icon_height);
//        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);
//        Bitmap destBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);
//        String[] texts = generateNeedDrawText(url);
//        drawText(mContext, bitmap, texts, color);
//        Canvas canvas = new Canvas(destBitmap);
//        Paint paint = new Paint();
//        canvas.drawBitmap(bitmap, 0, 0, paint);
//        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
//        canvas.drawBitmap(mBitmap, 0, 0, paint);
//        bitmap.recycle();
        return null;//destBitmap;
    }
    private Bitmap mBitmap;
    private void drawText(Context context, Bitmap bitmap, String[] texts, int color) {
//        int textColor;
//        float gray = 0.21f * Color.red(color) + 0.72f * Color.green(color) + 0.07f * Color.blue(color);
//        if(gray >= 256 * 0.9){
//            textColor = context.getResources().getColor(R.color.expand_child_textview_color);
//        }else{
//            textColor = Color.WHITE;
//        }
//        Resources resources = mContext.getResources();
//        int minFrameWidth = resources.getDimensionPixelSize(R.dimen.nav_text_min_frame_width);
//        int maxFrameWidth = resources.getDimensionPixelSize(R.dimen.nav_text_max_frame_width);
//        int minFrameHeight = resources.getDimensionPixelSize(R.dimen.nav_text_min_frame_height);
//        TextPaint paint = new TextPaint();
//        paint.setAntiAlias(true);
//        paint.setFakeBoldText(true);
//        int minTextSize = CommonUtil.sp2px(context, 11);
//        int maxTextSize = CommonUtil.sp2px(context, 22);
//        int textSize = minTextSize;
//        paint.setTextSize(textSize);
//        Rect bounds = new Rect();
//        Canvas canvas = new Canvas(bitmap);
//        paint.setColor(textColor);
//        paint.getTextBounds(texts[1], 0, texts[1].length(), bounds);
//        int textWidth = bounds.width();
////        int textHeight = bounds.height();// tieba.baidu.com --> baidu's text height
//        int prefixHeight = 0;
//        int prefixWidth = 0;
//        paint.setTextSize(minTextSize);
//        FontMetricsInt fontMetrics = paint.getFontMetricsInt();
//        int textHeight = fontMetrics.bottom - fontMetrics.ascent;
//        if(texts[2] != null){
//            paint.getTextBounds(texts[2], 0, texts[2].length(), bounds);
//            prefixHeight = fontMetrics.bottom - fontMetrics.ascent;// tieba.baidu.com --> tieba's text height
//            prefixWidth = bounds.width();
//        }
//        paint.setTextSize(minTextSize);
//        paint.getTextBounds(texts[0], 0, texts[0].length(), bounds);
//        fontMetrics = paint.getFontMetricsInt();
//        int suffixHeight = fontMetrics.bottom - fontMetrics.ascent;// tieba.baidu.com --> com's text height
////        suffixHeight = bounds.height();
//        int suffixWidth = bounds.width();
//        int maxWidth = getMaxValue(textWidth, prefixWidth, suffixWidth, minFrameWidth);
//        texts[0] = truncateIfNeed(texts[0], maxFrameWidth,suffixWidth, paint);
//        texts[1] = truncateIfNeed(texts[1], maxFrameWidth,textWidth, paint);
//        texts[2] = truncateIfNeed(texts[2], maxFrameWidth,prefixWidth, paint);
//        paint.getTextBounds(texts[1], 0, texts[1].length(), bounds);
//        textWidth = bounds.width();
//        while (textWidth < maxFrameWidth && textSize < maxTextSize) {
//            textSize += CommonUtil.sp2px(mContext, 1);
//            paint.setTextSize(textSize);
//            paint.getTextBounds(texts[1], 0, texts[1].length(), bounds);
//            textWidth = bounds.width();
//            fontMetrics = paint.getFontMetricsInt();
//            textHeight = fontMetrics.bottom - fontMetrics.ascent;
//        }
//        if(textWidth > maxFrameWidth && textSize < maxTextSize){
//            textSize -= CommonUtil.sp2px(mContext, 1);
//            paint.setTextSize(textSize);
//            paint.getTextBounds(texts[1], 0, texts[1].length(), bounds);
//            textWidth = bounds.width();
//            fontMetrics = paint.getFontMetricsInt();
//            textHeight = fontMetrics.bottom - fontMetrics.ascent;
//        }
//        maxWidth = getMaxValue(textWidth, prefixWidth, suffixWidth, minFrameWidth);
//        int totalHeight = prefixHeight + textHeight + suffixHeight;
//        totalHeight = Math.max(minFrameHeight, totalHeight);
//        canvas.drawColor(color);
//        if (texts[2] == null) {
//            int y = (bitmap.getHeight() - minFrameHeight) / 2 + minFrameHeight;
//            int x = bitmap.getWidth() - (bitmap.getWidth() - maxWidth) / 2;
//            paint.setTextSize(minTextSize);
//            paint.getTextBounds(texts[0], 0, texts[0].length(), bounds);
//            fontMetrics = paint.getFontMetricsInt();
//            canvas.translate(0, y - fontMetrics.bottom);
//            canvas.drawText(texts[0], x - suffixWidth, 0, paint);
//            paint.setTextSize(textSize);
//            canvas.translate(0, -suffixHeight);
//            canvas.drawText(texts[1], x - textWidth, 0, paint);
//        }else{
//            int y = (bitmap.getHeight() - totalHeight) / 2 + totalHeight;
//            int x = bitmap.getWidth() - (bitmap.getWidth() - maxWidth) / 2;
//            // draw suffix
//            paint.setTextSize(minTextSize);
//            paint.getTextBounds(texts[0], 0, texts[0].length(), bounds);
//            canvas.translate(0, y- paint.getFontMetrics().bottom);
//            canvas.drawText(texts[0], x - suffixWidth, 0, paint);
//            //draw text
//            canvas.translate(0, -suffixHeight - (paint.getFontMetrics().ascent - paint.getFontMetrics().top));
//            paint.setTextSize(textSize);
//            canvas.drawText(texts[1], x - textWidth, 0, paint);
//            if (texts[2] != null) {// prefix
//                paint.getTextBounds(texts[0], 0, texts[0].length(), bounds);
//                canvas.translate(0, -textHeight + bounds.bottom);
//                paint.setTextSize(minTextSize);
//                canvas.drawText(texts[2], x - prefixWidth, 0, paint);
//            }
//        }
    }

    private String truncateIfNeed(String text, int maxFrameWidth, int textWidth, TextPaint paint) {
        Rect bounds = new Rect();
        String result = text;
        if (text != null && textWidth > maxFrameWidth) {
            result = TextUtils.ellipsize(text, paint, maxFrameWidth, TruncateAt.MIDDLE).toString();
            paint.getTextBounds(result, 0, result.length(), bounds);
            textWidth = bounds.width();
        }
        return result;
    }

    private int getMaxValue(int a, int b, int c, int d) {
        int x = Math.max(a, b);
        int y = Math.max(c, d);
        return Math.max(x, y);
    }

    private final static String SPECIAL_SUFFIX = "com.cn";
    private HashMap<String, String> iconList;

    public Map<String, String> parseIconList() {
        return parseIconList(false);
    }

    public Map<String, String> parseIconList(boolean reflesh) {
        InputStream input = null;
        try {
            if (iconList != null && !reflesh) {
                return iconList;
            }
            String content = CommonUtil.readString(mContext, ICON_LIST_NAME);
            if(!CommonUtil.isJson(content)){
                input = mContext.getAssets().open(ICON_LIST_NAME);
                content = CommonUtil.readTextFromStream(input, true);
            }
            JSONObject object = new JSONObject(content);
            Field field = JSONObject.class.getDeclaredField("nameValuePairs");
            field.setAccessible(true);
            Map<String, String> map = (Map<String, String>) field.get(object);
            iconList = new HashMap<String, String>(map);
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(input != null){
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
        return iconList;
    }

    private String[] generateNeedDrawText(String url) {
        String texts[] = new String[3];
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        String suffix = null;
        int indexOf = -1;
        String domain = CommonUtil.getDomain(url);
        if (domain.endsWith(SPECIAL_SUFFIX)) {
            suffix = SPECIAL_SUFFIX;
            indexOf = domain.lastIndexOf(SPECIAL_SUFFIX) - 1;
        } else {
            indexOf = domain.lastIndexOf(".");
            suffix = domain.substring(indexOf, domain.length());
        }
        texts[0] = suffix;
        String domainWithoutSuffix = domain.substring(0, indexOf);
        int lastIndexOf = domainWithoutSuffix.lastIndexOf(".");
        if (lastIndexOf == -1) {
            texts[1] = domainWithoutSuffix;
        } else {
            texts[1] = domainWithoutSuffix.substring(lastIndexOf + 1);
            texts[2] = domainWithoutSuffix.substring(0, lastIndexOf + 1);
        }
        return texts;
    }

    public void addNavigationInfoListener(AddNavigationInfoListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeNavigationInfoListener(AddNavigationInfoListener listener) {
        listeners.remove(listener);
    }

    public interface AddNavigationInfoListener {
        public void onAdd(NavigationInfo info);
    }

    public interface NavigationInfoFilter {
        public boolean onFilter(NavigationInfo info);
    }
}
