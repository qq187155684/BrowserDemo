package com.android.myapidemo.smartisan.readmode;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.myapidemo.smartisan.browse.Browser;
import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.browse.BrowserWebView;
import com.android.myapidemo.smartisan.browse.PhoneUi;
import com.android.myapidemo.smartisan.browse.Tab;
import com.android.myapidemo.smartisan.browse.WebViewController;
import com.android.myapidemo.smartisan.browser.util.StringUtils;

import org.apache.http.util.EncodingUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.util.Log;
/**
 * Created by jude on 14-11-19.
 */
public class ReadModeHelper {

    private Tab mTab;
    private Context mContext;
    private WebViewController mWebViewController;
    private PhoneUi mPhoneUi;
    private BrowserWebView mReadModeView;
    private String mJsCacheReadability = null;
    private boolean mIsBtnReadModeShowing = false;
    private String mUrl, mHtml;
    private static final boolean LOGD_ENABLED = Browser.LOGD_ENABLED;
    private static final String LOGTAG = "ReadView";

    public ReadModeHelper(Context context, Tab tab, WebViewController webViewController) {
        mContext = context;
        mTab = tab;
        mWebViewController = webViewController;
        mPhoneUi = (PhoneUi) webViewController.getUi();
        if (mReadModeView == null)
            createReadModeWindow();
        mJsCacheReadability = getFromAssets("readability.js");
    }

    public void reset() {
        setBtnReadModeShowing(false);
        if (mReadModeView != null && mReadModeView.getParent() != null) {
            mReadModeView.scrollTo(0, 0);
        }
    }

    public void showHtml(final String html, final String url) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(html))
            return;
        if (url.equals(mUrl) && html.equals(mHtml) && isBtnReadModeShowing()) {
            mPhoneUi.setBtnReadModeVisibility(mTab, mTab.getWebView().getUrl());
            return;
        }

        setBtnReadModeShowing(false);
        if (!url.equals(mUrl))
            mPhoneUi.setBtnReadModeVisibility(mTab, mTab.getWebView().getUrl());
        mUrl = url;
        mHtml = html;
        mWebViewController.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mReadModeView == null) {
                    createReadModeWindow();
                }
                mReadModeView.getSettings().setJavaScriptEnabled(false);
                mReadModeView
                  .loadDataWithBaseURL(url, "<!DOCTYPE HTML>" + html, "text/html", "UTF-8", null);
            }
        });
    }

    public void createReadModeWindow() {
        //mReadModeView = new BrowserWebView(mContext, null, 0, false, true);
        if (LOGD_ENABLED) {
            Log.d(LOGTAG, "createReadView " + mReadModeView);
        }
        mReadModeView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mReadModeView.getSettings().setSupportMultipleWindows(true);
//        mReadModeView.setWebViewClient(new ReadModeWebViewClient());
//        mReadModeView.setWebChromeClient(new ReadModeWebChromeClient());
        mReadModeView.onPause();
//        if (mTab.inForeground())
//            mWebViewController.setReadViewHelper(this);
    }

    public void destroy() {
        if (LOGD_ENABLED) {
            Log.d(LOGTAG, "destroy ReadView " + mReadModeView);
        }
        if (mReadModeView != null) {
            ViewGroup parent = (ViewGroup) mReadModeView.getParent();
            if (parent != null)
                parent.removeView(mReadModeView);
            mReadModeView.removeAllViews();
            mReadModeView.setWebViewClient(null);
            mReadModeView.setWebChromeClient(null);
            mReadModeView.destroy();
            mReadModeView = null;
        }
    }

    public BrowserWebView getReadView() {
        return mReadModeView;
    }

    public boolean isBtnReadModeShowing() {
        return mIsBtnReadModeShowing;
    }

    private void setBtnReadModeShowing(boolean isBtnReadModeShowing) {
        mIsBtnReadModeShowing = isBtnReadModeShowing;
    }

    private class ReadModeWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (StringUtils.inResourceWhiteList(url)) {
                return null;
            } else {
                ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
                return new WebResourceResponse("text/plain", "utf-8", EMPTY);
            }
        }

        @Override
        public void onPageStarted(WebView view, final String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (LOGD_ENABLED) {
                Log.d(LOGTAG, "readview onPageStarted " + url);
            }
        }

        @Override
        public void onPageFinished(final WebView view, String url) {
            super.onPageFinished(view, url);
            view.onPause();
            if (LOGD_ENABLED) {
                Log.d(LOGTAG, "readview onPageFinished " + url);
            }
            if (TextUtils.isEmpty(url)) {
                if (LOGD_ENABLED)
                    Log.d(LOGTAG, "readview onPageFinished with null url, just return");
                return;
            }

            if (mPhoneUi.isActivityPaused())
                return;

            view.getSettings().setJavaScriptEnabled(true);
            view.loadUrl("javascript:" + mJsCacheReadability);
        }
    }

    private class ReadModeWebChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            String consoleString = consoleMessage.message();
            if (TextUtils.isEmpty(consoleString) || mReadModeView == null)
                return true;
            if (consoleString.startsWith("ready read!") && consoleString.substring(11).equals(mUrl)) {
                if (LOGD_ENABLED) {
                    Log.d(LOGTAG, "ready read " + mUrl);
                }
                updateStyle();
                if (!mIsBtnReadModeShowing) {
                    setBtnReadModeShowing(true);
                    mPhoneUi.setBtnReadModeVisibility(mTab, mUrl);
                }
                mReadModeView.onPause();
            } else if ((consoleString.startsWith("cant read!") && consoleString.substring(10).equals(mUrl)) || consoleString.equals("Uncaught ReferenceError: readability is not defined")) {
                if (LOGD_ENABLED) {
                    Log.d(LOGTAG, "can't read " + mUrl);
                }
                setBtnReadModeShowing(false);
                mPhoneUi.setBtnReadModeVisibility(mTab, mUrl);
                mReadModeView.onPause();
            }
            return true;
        }

//        @Override
//        public void onOffsetsForFullscreenChanged(float topControlsOffsetYPix,
//                                                  float contentOffsetYPix,
//                                                  float overdrawBottomHeightPix) {
//            if (mWebViewController instanceof Controller) {
//                Controller controller = (Controller)mWebViewController;
//                controller.getUi().translateReadModeTitleBar(topControlsOffsetYPix);
//            }
//        }
    }

    public void updateStyle() {
        String script = "javascript:readability.changeStyle(" +
                BrowserSettings.getInstance().getReadFontSize() + "," +
                BrowserSettings.getInstance().getReadStyle() + ")";
        mReadModeView.getSettings().setJavaScriptEnabled(true);
        mReadModeView.evaluateJavascript(script, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                if (mReadModeView != null)
                    mReadModeView.getSettings().setJavaScriptEnabled(false);
            }
        });
    }

    /*
    * read String from assets files
    * */
    public String getFromAssets(String fileName) {
        String result = "";
        try {
            InputStream in = mWebViewController.getActivity().getAssets().open(fileName);
            int lenght = in.available();
            byte[] buffer = new byte[lenght];
            in.read(buffer);
            in.close();
            result = EncodingUtils.getString(buffer, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
