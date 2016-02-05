/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.myapidemo.R;
import com.android.myapidemo.smartisan.browser.platformsupport.WebAddress;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

/**
 * Handle download requests
 */
public class DownloadHandler {

    private static final boolean LOGD_ENABLED = Browser.LOGD_ENABLED;

    private static final String LOGTAG = "DLHandler";

    public static abstract class UserChooseCallBack {
        protected Tab mTab;
        public UserChooseCallBack(Tab tab){
            mTab = tab;
            mTab.setIsDownloadUrl(true);
        }
        public abstract void onCloseCurrentPage(Tab tab) ;
    }

    private static AlertDialog mDialog;
    /**
     * Notify the host application a download should be done, or that the data
     * should be streamed if a streaming viewer is available.
     * 
     * @param activity Activity requesting the download.
     * @param url The full url to the content that should be downloaded
     * @param userAgent User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype The mimetype of the content reported by the server
     * @param referer The referer associated with the downloaded url
     * @param privateBrowsing If the request is coming from a private browsing
     *            tab.
     */
    public static void onDownloadStart(final Activity activity, final String url,
            final String userAgent, final String contentDisposition, final String mimetype,
            final String referer, final boolean privateBrowsing , final UserChooseCallBack cb) {
        if (activity == null) {
            return;
        }
        if (mDialog != null)
            mDialog.cancel();
        mDialog = new AlertDialog.Builder(new ContextThemeWrapper(activity,
                android.R.style.Theme_DeviceDefault_Light_Dialog))
                .setTitle(activity.getResources().getText(R.string.whether_download_file))
                .setMessage(activity.getResources().getText(R.string.download_file_tip))
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cb.onCloseCurrentPage(cb.mTab);
                    }
                })
                .setPositiveButton(R.string.download, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onDownloadStartNoStream(activity, url, userAgent, contentDisposition,
                                mimetype, referer, privateBrowsing);
                        cb.onCloseCurrentPage(cb.mTab);
                    }
                })
                .setCancelable(true).create();
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mDialog.setCanceledOnTouchOutside(false);

        if (mimetype != null && (mimetype.startsWith("audio/") || "application/octet-stream".equals(mimetype))) {
            mDialog.show();
            return;
        }

        if (contentDisposition == null
                || !contentDisposition.regionMatches(
                        true, 0, "attachment", 0, 10)) {
            // query the package manager to see if there's a
            // registered handler
            // that matches.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.parse(url), mimetype);
            ResolveInfo info = activity.getPackageManager().resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null) {
                ComponentName myName = activity.getComponentName();
                // If we resolved to ourselves, we don't want to
                // attempt to
                // load the url only to try and download it
                // again.
                if (!myName.getPackageName().equals(
                        info.activityInfo.packageName)
                        || !myName.getClassName().equals(
                                info.activityInfo.name)) {
                    // someone (other than us) knows how to
                    // handle this mime
                    // type with this scheme, don't download.
                    try {
                        activity.startActivity(intent);
                        cb.onCloseCurrentPage(cb.mTab);
                        return;
                    } catch (ActivityNotFoundException ex) {
                        if (LOGD_ENABLED) {
                            Log.d(LOGTAG, "activity not found for " + mimetype
                                    + " over " + Uri.parse(url).getScheme(),
                                    ex);
                        }
                        // Best behavior is to fall back to a
                        // download in this
                        // case
                    }
                }
            }
        }

        mDialog.show();
    }

    // This is to work around the fact that java.net.URI throws Exceptions
    // instead of just encoding URL's properly
    // Helper method for onDownloadStartNoStream
    private static String encodePath(String path) {
        char[] chars = path.toCharArray();

        boolean needed = false;
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                needed = true;
                break;
            }
        }
        if (needed == false) {
            return path;
        }

        StringBuilder sb = new StringBuilder("");
        for (char c : chars) {
            if (c == '[' || c == ']' || c == '|') {
                sb.append('%');
                sb.append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Notify the host application a download should be done, even if there is a
     * streaming viewer available for thise type.
     * 
     * @param activity Activity requesting the download.
     * @param url The full url to the content that should be downloaded
     * @param userAgent User agent of the downloading application.
     * @param contentDisposition Content-disposition http header, if present.
     * @param mimetype The mimetype of the content reported by the server
     * @param referer The referer associated with the downloaded url
     * @param privateBrowsing If the request is coming from a private browsing
     *            tab.
     */
    /* package */static void onDownloadStartNoStream(Activity activity,
            String url, String userAgent, String contentDisposition,
            String mimetype, String referer, boolean privateBrowsing) {
        final String filename = guessFileName(url,contentDisposition, mimetype);
        // Check to see if we have an SDCard
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            int title;
            String msg;

            // Check to see if the SDCard is busy, same as the music app
            if (status.equals(Environment.MEDIA_SHARED)) {
                msg = activity.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = activity.getString(R.string.download_no_sdcard_dlg_msg, filename);
                title = R.string.download_no_sdcard_dlg_title;
            }

            new AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(msg)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        // java.net.URI is a lot stricter than KURL so we have to encode some
        // extra characters. Fix for b 2538060 and b 1634719
        WebAddress webAddress;
        try {
            webAddress = new WebAddress(url);
            webAddress.setPath(encodePath(webAddress.getPath()));
        } catch (Exception e) {
            // This only happens for very bad urls, we want to chatch the
            // exception here
            Log.e(LOGTAG, "Exception trying to parse url:" + url);
            return;
        }

        String addressString = webAddress.toString();
        Uri uri = Uri.parse(addressString);
        final DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(uri);
        } catch (IllegalArgumentException e) {
            Toast.makeText(activity, R.string.cannot_download, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(mimetype)) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if (TextUtils.isEmpty(extension)) {
                if (!TextUtils.isEmpty(filename) && filename.lastIndexOf('.') != 0) {
                    int dotIndex = filename.lastIndexOf('.') + 1;
                    extension = filename.substring(dotIndex);
                    mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                }
            } else {
                mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }

        } else {
            mimetype = remapGenericMimeType(mimetype, url, contentDisposition);
        }
        request.setMimeType(mimetype);
        // set downloaded file destination to /sdcard/Download.
        // or, should it be set to one of several Environment.DIRECTORY* dirs
        // depending on mimetype?
        try {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        } catch(IllegalStateException e) {}
        // let this downloaded file be scanned by MediaScanner - so that it can
        // show up in Gallery app, for example.
        request.allowScanningByMediaScanner();
        request.setDescription(webAddress.getHost());
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
//        String cookies = CookieManager.getInstance().getCookie(url, privateBrowsing);
//        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.addRequestHeader("Referer", referer);
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(filename);
        if (mimetype == null) {
            if (TextUtils.isEmpty(addressString)) {
                return;
            }
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            //new FetchUrlMimeType(activity, request, addressString, cookies,
                    //userAgent).start();

        } else {
            final DownloadManager manager = (DownloadManager) activity
                    .getSystemService(Context.DOWNLOAD_SERVICE);
            new Thread("Browser download") {
                public void run() {
                    manager.enqueue(request);
                }
            }.start();
        }
        Toast.makeText(activity, R.string.download_pending, Toast.LENGTH_SHORT)
                .show();
    }

    private static String remapGenericMimeType(String mimeType, String url,
            String contentDisposition) {
        if ("text/plain".equals(mimeType) ||
                "application/octet-stream".equals(mimeType)) {

            // for attachment, use the filename in the Content-Disposition
            // to guess the mimetype
            String filename = null;
            if (contentDisposition != null) {
                filename = parseContentDisposition(contentDisposition);
            }
            if (filename != null) {
                if (filename.startsWith("=?UTF-8?B?")
                        && filename.endsWith("?=")) {
                    /**
                     * when in this block , this means check the chrome's
                     * response header if using chrome user-agent , the server
                     * response the file name by using UTF-8 & B (Base64) way ,
                     * which means we must decode it and show the correct file
                     * name sometimes , if the file name is too long ,the name
                     * is separated into several parts in ContentDisposition,
                     * such as : attachment;filename=
                     * "=?UTF-8?B?5ZOG5ZWmQeaipiDnrKzkuozpg6hf56ysNjg06ZuGXzE4?= =?UTF-8?B?MzAwODUuMDBfODBjZjVlMTNi?= =?UTF-8?B?NDc1NDIzNjhjNWI2OTE2OTIzMDFmMzg1M2QwMjg5Ny5tcDQ=?="
                     * So , just use loop to figure out the correct file name
                     *
                     */
                    String savedFileName = new String(filename);
                    StringBuilder decodeFileNameTotal = new StringBuilder();
                    final int tagLen = "=?UTF-8?B?".length();
                    try {
                        int lastStartIndex = -1;
                        int lastEndIndex = -1;
                        do {
                            lastStartIndex = filename.indexOf("=?UTF-8?B?",
                                    lastStartIndex + 1);
                            if (lastStartIndex < 0) {
                                break;
                            }
                            int startPos = lastStartIndex + tagLen;
                            // check the end one
                            lastEndIndex = filename.indexOf("?=",
                                    lastEndIndex + 1);
                            if (lastEndIndex < 0) {
                                break;
                            }
                            int endPos = lastEndIndex;
                            if (startPos >= endPos)
                                break;
                            String base64Filename = filename.substring(
                                    startPos, endPos);
                            byte[] data = Base64.decode(base64Filename,
                                    Base64.DEFAULT);
                            decodeFileNameTotal
                                    .append(new String(data, "UTF-8"));
                        } while (true);
                    } catch (Exception ex) {
                        filename = savedFileName;
                    }
                    if (decodeFileNameTotal.length() > 0) {
                        filename = decodeFileNameTotal.toString();
                    }
                }
                url = URLEncoder.encode(filename);
            }
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            String newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (newMimeType != null) {
                mimeType = newMimeType;
            }
        } else if ("text/vnd.wap.wml".equals(mimeType)) {
            // As we don't support wml, render it as plain text
            mimeType = "text/plain";
        } else if ("content/unknown".equals(mimeType) && !TextUtils.isEmpty(url) && url.toLowerCase().endsWith(".apk")) {
            mimeType = "application/vnd.android.package-archive";
        } else {
            // It seems that xhtml+xml and vnd.wap.xhtml+xml mime
            // subtypes are used interchangeably. So treat them the same.
            if ("application/vnd.wap.xhtml+xml".equals(mimeType)) {
                mimeType = "application/xhtml+xml";
            }
        }
        return mimeType;
    }

    /** Regex used to parse content-disposition headers */
    private static final Pattern CONTENT_DISPOSITION_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /*
     * Parse the Content-Disposition HTTP Header. The format of the header is
     * defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html This
     * header provides a filename for content that is going to be downloaded to
     * the file system. We only support the attachment type. Note that RFC 2616
     * specifies the filename value must be double-quoted. Unfortunately some
     * servers do not quote the value so to maintain consistent behaviour with
     * other browsers, we allow unquoted values too.
     */
    private static String parseContentDisposition(String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(2);
            }
        } catch (IllegalStateException ex) {
            // This function is defined as returning null when it can't parse
            // the header
        }
        return null;
    }

    private static String guessFileName(String url, String contentDisposition, String mimeType) {
        try {
            if (!TextUtils.isEmpty(contentDisposition)) {
                contentDisposition = URLDecoder.decode(contentDisposition, "UTF-8");
                url = URLDecoder.decode(url, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
      //URLUtil.guessFileName is not work when download file from qq mail,just workaround it.
        if (contentDisposition != null) {
            String[] split = contentDisposition.split(";");
            if (split != null && split.length == 3) {
                String startStr = "filename=\"";
                for (String s : split) {
                    if(s.contains(startStr)){
                        return s.substring(startStr.length() + 1, s.length() - 1);
                    }
                }
            }
        }
        return URLUtil.guessFileName(url, contentDisposition, mimeType);
    }
}
