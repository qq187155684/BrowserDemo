
package com.android.myapidemo.smartisan.wxapi;

import com.android.myapidemo.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class AllShareActivity extends Activity {
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            ShareInfo info = (ShareInfo) msg.obj;
            Drawable drawable = getResources().getDrawable(R.drawable.weixin_share_icon);
            Bitmap bitmap = null;
            if(drawable instanceof BitmapDrawable){
                bitmap = ((BitmapDrawable)drawable).getBitmap();
            }
            ShareUtil.shareCircleOfFriends(info.title, info.url, bitmap, info.shareType);
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        int type = intent.getIntExtra(ShareUtil.SHARE_TYPE, -1);
        finish();
        switch (type) {
            case ShareUtil.WEIXIN:
            case ShareUtil.CICLE_OF_FRIEND:
                ShareInfo info = new ShareInfo(url, title, type);
                Message msg = Message.obtain(mHandler);
                msg.obj = info;
                mHandler.sendMessageDelayed(msg, 300);//if not delay,the dialog can't dismiss.
                break;
            case ShareUtil.COPY://do nothing

                break;
            default:
                break;
        }
    }

    class ShareInfo {
        String url;
        String title;
        int shareType;

        public ShareInfo(String url, String title, int type) {
            this.url = url;
            this.title = title;
            this.shareType = type;
        }
    }
}
