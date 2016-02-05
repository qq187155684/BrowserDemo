/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.myapidemo.smartisan.browse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ParseException;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.platformsupport.WebAddress;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;
import com.android.myapidemo.smartisan.browser.util.IconColor;
import com.android.myapidemo.smartisan.browser.util.IconColor.ColorInfo;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser.NavIconParseListener;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class DownloadFaviconIcon implements Runnable {
    public static final String ICON = "icon";
    public static final String TAG = "tag";
    public static final String MD5 = "md5";
    public static final String LISTENER = "listener";
    public static final String ICON_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/smartisan/browser/icons";
    private final String mDomain;
    private final Context mContext;
    private NavIconParseListener mListener;
    private String mTag;
    private Message mMessage;
    private static HashMap<String, String> map = new HashMap<String, String>();
    private String mDownloadUrl;
    /**
     * Use this ctor to store the touch icon in the bookmarks database for the
     * originalUrl so we take account of redirects. Used when the user bookmarks
     * a page from outside the bookmarks activity.
     */
    public DownloadFaviconIcon(Context ctx, String domain,String tag, Message message, NavIconParseListener listener) {
        mContext = ctx.getApplicationContext();
        mDomain = domain;
        mListener = listener;
        mTag = tag;
        mMessage = message;
    }

    private Bitmap storeIcon(Bitmap icon, boolean isMakeTextIcon) {
        return saveBitmap(mContext, icon, mDomain, isMakeTextIcon);
    }

    private String createFaviconUrl() {
        String url = mDomain + "/favicon.ico";
        try {
            return new WebAddress(url).toString();
        } catch (ParseException e) {
        }
        return null;
    }

    private static Bitmap makeTextIcon(Context ctx, Bitmap icon, String url){
        ColorInfo colorInfo = IconColor.getMajorColor(icon);
        Bitmap defaultBitmap = null;
        NavigationInfoParser parser = NavigationInfoParser.getInstance(ctx);
        if (colorInfo != null) {
            defaultBitmap = parser.createDefaultBitmap(url,
                    colorInfo.majorColor);
        } else {
            defaultBitmap = parser.createDefaultBitmap(url);
        }
        return defaultBitmap;
    }

    public static Bitmap saveBitmap(Context ctx, Bitmap icon, String url, boolean isMakeTextIcon) {
        Bitmap saveIcon = null;
        if (isMakeTextIcon) {
            saveIcon = makeTextIcon(ctx, icon, url);
//            icon.recycle();
        } else {
            saveIcon = icon;
        }
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        saveIcon.compress(Bitmap.CompressFormat.PNG, 100, os);
        byte[] bytes = os.toByteArray();
        File file = new File(ICON_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        File iconFile = new File(file, Base64.encodeToString(url.getBytes(), Base64.DEFAULT));
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(iconFile);
            fos.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return saveIcon;
    }

    public static Bitmap loadBitmap(Context ctx, String domain) {
        File iconFile = getIconFile(domain);
        if (iconFile.exists()) {
            return BitmapFactory.decodeFile(iconFile.getAbsolutePath());
        } else {
            return null;
        }
    }

    public static File getIconFile(String domain) {
        File iconFile = new File(ICON_PATH, domain);
        return iconFile;
    }

    @Override
    public void run() {
        String url = mDomain;
        boolean containsKey = map.containsKey(url);
        if (containsKey) {
            return;
        }
        boolean isMakeTextIcon = true;
        String md5 = null;
        Map<String, String> parseIconList = NavigationInfoParser.getInstance(mContext).parseIconList();
        String domainFix = mDomain.replaceAll("\\.", "_");
        if (parseIconList.containsKey(domainFix)) {
            mDownloadUrl = mContext.getString(R.string.icon_url, domainFix);
            String content = CommonUtil.downloadContent(mContext, url);
            String[] result = parseDownloadUrl(content);
            mDownloadUrl = result[0];
            md5 = result[1];
            //if mDownloadUrl is null, it means server make sth. wrong, we handle it.
            if (TextUtils.isEmpty(mDownloadUrl) && !hasOldIcon(mDomain)) {
                mDownloadUrl = createFaviconUrl();//if we haven't old icon, get the favicon ourself.
            }else{
                isMakeTextIcon = false;
            }
        } else {
            mDownloadUrl = createFaviconUrl();
        }
        map.put(url, url);
        AndroidHttpClient client = null;
        HttpGet request = null;
        Bitmap icon = null;
        try {
            client = AndroidHttpClient.newInstance(null);
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10 * 1000);
            // HttpHost httpHost = Proxy.getPreferredHttpHost(mContext, url);
            Object[] params = {
                    mContext, mDownloadUrl
            };
            Class[] type = new Class[] {
                    android.content.Context.class, String.class
            };
            HttpHost httpHost = (HttpHost) ReflectHelper.invokeMethod(
                    "android.net.Proxy", "getPreferredHttpHost",
                    type, params);
            if (httpHost != null) {
                ConnRouteParams.setDefaultProxy(client.getParams(), httpHost);
            }
            request = new HttpGet(mDownloadUrl);
            // Follow redirects
            HttpClientParams.setRedirecting(client.getParams(), true);
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream content = entity.getContent();
                    if (content != null) {
                        icon = BitmapFactory.decodeStream(
                                content, null, null);
                    }
                }
            }
        } catch (Exception ex) {
            if (request != null) {
                request.abort();
            }
        } finally {
            if (client != null) {
                client.close();
            }
            icon = storeIcon(icon, isMakeTextIcon);
            if (mMessage != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(ICON, icon);
                bundle.putString(MD5, md5);
                bundle.putString(TAG, mTag);
                bundle.putSerializable(LISTENER, mListener);
                mMessage.obj = bundle;
                mMessage.sendToTarget();
            }
            map.remove(url);
        }
    }

    private boolean hasOldIcon(String domain) {
        return getIconFile(domain).exists();
    }

    private String[] parseDownloadUrl(String content) {
        if (!TextUtils.isEmpty(content)) {
            try {
                JSONObject jsonObject = new JSONObject(content);
                String url = jsonObject.optString("url");
                String md5 = jsonObject.optString("md5");
                return new String[]{url, md5};
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return new String[]{null, null};
    }
}
