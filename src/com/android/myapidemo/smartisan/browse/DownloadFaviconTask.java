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
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.params.HttpConnectionParams;

import com.android.myapidemo.smartisan.browser.platformsupport.WebAddress;
import com.android.myapidemo.smartisan.browser.util.IconColor;
import com.android.myapidemo.smartisan.browser.util.IconColor.ColorInfo;
import com.android.myapidemo.smartisan.browser.util.NavigationInfoParser;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class DownloadFaviconTask implements Runnable {
    public static final String ICON = "icon";
    public static final String LISTENER = "listener";
    public static final String ICON_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/smartisan/browser/icons";
    private final Context mContext;
    private DownloadFaviconIconCallBack mCalback;
    private Handler mHandler;
    private String mDownloadUrl;
    private Bitmap icon;
    public DownloadFaviconTask(Context ctx, String url, Handler handler, DownloadFaviconIconCallBack callBack) {
        mContext = ctx.getApplicationContext();
        mCalback = callBack;
        mHandler = handler;
        mDownloadUrl = url;
    }

    public static Bitmap makeTextIcon(Context ctx, Bitmap icon, String url){
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

    public static void saveBitmap(Bitmap icon, String key) {
        if (icon == null) {
            return;
        }
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, os);
        byte[] bytes = os.toByteArray();
        File file = new File(ICON_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        File iconFile = new File(file, key);
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
    }

    public static Bitmap loadBitmap(String key) {
        File iconFile = getIconFile(key);
        if (iconFile.exists()) {
            return BitmapFactory.decodeFile(iconFile.getAbsolutePath());
        } else {
            return null;
        }
    }
    public static Bitmap loadFavIcon(String domain) {
        return loadBitmap(domain);
    }

    public static File getIconFile(String domain) {
        File iconFile = new File(ICON_PATH, domain);
        return iconFile;
    }

    @Override
    public void run() {
        AndroidHttpClient client = null;
        HttpGet request = null;
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
            if (mHandler != null && mCalback != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCalback.receiveBitmap(icon, mDownloadUrl, mMd5);
                    }
                });
            }
        }
    }
    public interface DownloadFaviconIconCallBack {
        void receiveBitmap(Bitmap bitmap, String url, String md5);
    }

    public static String createFaviconUrl(String domain) {
        String url = domain + "/favicon.ico";
        try {
            return new WebAddress(url).toString();
        } catch (ParseException e) {
        }
        return null;
    }
    private String mMd5;

    public void setMd5(String md5) {
        mMd5 = md5;
    }
}
