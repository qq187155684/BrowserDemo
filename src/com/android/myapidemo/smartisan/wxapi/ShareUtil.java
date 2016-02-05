
package com.android.myapidemo.smartisan.wxapi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class ShareUtil {
    public static final String SHARE_TYPE = "SHARE_TYPE";
    public static final int WEIXIN = SendMessageToWX.Req.WXSceneSession;
    public static final int CICLE_OF_FRIEND = SendMessageToWX.Req.WXSceneTimeline;
    public static final int COPY = 2;
    public static final String PKG_NAME = "com.tencent.mm";
    private static final String APP_ID = "wx72263b5abaaa00e6";
    private static final String WEBPAGE_TYPE = "webpage";
    private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;
    private static IWXAPI api;

    public static void init(Context context) {
        api = WXAPIFactory.createWXAPI(context, APP_ID, true);
        api.registerApp(APP_ID);
    }

    public static boolean shareCircleOfFriends(String title, String url,Bitmap bitmap,int shareType) {
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = url;
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = title;
        msg.description = title;
        msg.setThumbImage(bitmap);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction(WEBPAGE_TYPE);
        req.message = msg;
        req.scene = shareType;
        return api.sendReq(req);
    }

    private static String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    public static void handleIntent(Intent intent, IWXAPIEventHandler handler) {
        if (api != null) {
            api.handleIntent(intent, handler);
        }
    }

    public static boolean isCircleOfFriendsSupport() {
        if (api != null) {
            int wxSdkVersion = api.getWXAppSupportAPI();
            return wxSdkVersion >= TIMELINE_SUPPORTED_VERSION;
        }
        return false;
    }
    public static boolean isWeChatShareSupport() {
        if (api != null) {
           return api.isWXAppSupportAPI();
        }
        return false;
    }
}
