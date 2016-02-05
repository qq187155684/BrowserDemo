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

import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


//import org.codeaurora.swe.GeolocationPermissions;

import android.webkit.WebViewClient;

import com.android.myapidemo.R;
import com.android.myapidemo.UI;
import com.android.myapidemo.smartisan.browse.TabControl.OnThumbnailUpdatedListener;
import com.android.myapidemo.smartisan.browser.platformsupport.BrowserContract;
import com.android.myapidemo.smartisan.browser.util.CommonUtil;
import com.android.myapidemo.smartisan.browser.util.JsDialogBlockHelper;
import com.android.myapidemo.smartisan.browser.util.StringUtils;
import com.android.myapidemo.smartisan.browser.util.UrlUtils;
import com.android.myapidemo.smartisan.readmode.ReadModeHelper;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

/**
 * Class for maintaining Tabs with a main WebView and a subwindow.
 */
public class Tab {

    // Log Tag
    private static final String LOGTAG = "Tab";
    private static final boolean LOGD_ENABLED = Browser.LOGD_ENABLED;
    // Special case the logtag for messages for the Console to make it easier to
    // filter them and match the logtag used for these messages in older
    // versions
    // of the browser.
    private static final String CONSOLE_LOGTAG = "browser";
    private static final int MSG_CAPTURE = 42;
    private static final int MSG_CAPTURE_H = 43;
    private static final int CAPTURE_DELAY = 100;
    public static final int INITIAL_PROGRESS = 5;
    public static final int PROGRESS_MAX = 100;

    private static Bitmap sDefaultFavicon;
    protected boolean hasCrashed = false;

    private static Paint sAlphaPaint = new Paint();
    static {
        sAlphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        sAlphaPaint.setColor(Color.TRANSPARENT);
    }

    public enum SecurityState {
        // The page's main resource does not use SSL. Note that we use this
        // state irrespective of the SSL authentication state of sub-resources.
        SECURITY_STATE_NOT_SECURE,
        // The page's main resource uses SSL and the certificate is good. The
        // same is true of all sub-resources.
        SECURITY_STATE_SECURE,
        // The page's main resource uses SSL and the certificate is good, but
        // some sub-resources either do not use SSL or have problems with their
        // certificates.
        SECURITY_STATE_MIXED,
        // The page's main resource uses SSL but there is a problem with its
        // certificate.
        SECURITY_STATE_BAD_CERTIFICATE,
    }

    Context mContext;
    protected WebViewController mWebViewController;
    private ReadModeHelper mReadModeHelper;
    // The tab ID
    private long mId = -1;

    // The Geolocation permissions prompt
    private GeolocationPermissionsPrompt mGeolocationPermissionsPrompt;
    // Main WebView wrapper
    private View mContainer;
    // Main WebView
    private WebView mMainView;
    // Subwindow container
    private View mSubViewContainer;
    // Subwindow WebView
    private WebView mSubView;

    // Saved bundle for when we are running low on memory. It contains the
    // information needed to restore the WebView if the user goes back to the
    // tab.
    private Bundle mSavedState;
    // Parent Tab. This is the Tab that created this Tab, or null if the Tab was
    // created by the UI
    private Tab mParent;
    // Tab that constructed by this Tab. This is used when this Tab is
    // destroyed, it clears all mParentTab values in the children.
    private Vector<Tab> mChildren;
    // If true, the tab is in the foreground of the current activity.
    private boolean mInForeground;
    // If true, the tab is in page loading state (after onPageStarted,
    // before onPageFinsihed)
    private boolean mInPageLoad;
    // The last reported progress of the current page
    private int mPageLoadProgress = INITIAL_PROGRESS;
    // Application identifier used to find tabs that another application wants
    // to reuse.
    private String mAppId;
    // flag to indicate if tab should be closed on back
    private boolean mCloseOnBack;
    // Keep the original url around to avoid killing the old WebView if the url
    // has not changed.
    // Error console for the tab
    private ErrorConsoleView mErrorConsole;
    // The listener that gets invoked when a download is started from the
    // mMainView
    private final BrowserDownloadListener mDownloadListener;
    // Listener used to know when we move forward or back in the history list.
    //private final WebBackForwardListClient mWebBackForwardListClient;
    // State of the auto-login request.
    private DeviceAccountLogin mDeviceAccountLogin;

    // AsyncTask for downloading touch icons
    DownloadTouchIcon mTouchIconLoader;

    private BrowserSettings mSettings;
    private int mCaptureWidth;
    private int mCaptureHeight;
    private int mCaptureWidthHroizontal;
    private int mCaptureHeightHroizontal;
    private Handler mHandler;
    private Timestamp mTimestamp;
    private boolean mUpdateThumbnail;
    private boolean mIsShowHomePage = false;
    private boolean mReuseState;
    private AlertDialog mAlterDialog;
    private AlertDialog mGeolocationDialog;
    private boolean mFullScreen = false;
    // determine if webview is destroyed to MemoryMonitor
    private boolean mWebViewDestroyedByMemoryMonitor;

    int mPosition = -1;
    int mIncogPosition = -1;
    private boolean mIsDownloadUrl;

    static class CaptureInfo {
        public Bitmap bitmapCapture ;
        public Bitmap bitmapCaptureH ;
    }
    private CaptureInfo mCaptureCached;
    /**
     * See {@link #clearBackStackWhenItemAdded(String)}.
     */
    private Pattern mClearHistoryUrlPattern;

    private static synchronized Bitmap getDefaultFavicon(Context context) {
        if (sDefaultFavicon == null) {
            /*
             * sDefaultFavicon = BitmapFactory.decodeResource(
             * context.getResources(), R.drawable.app_web_browser_sm);
             */
        }
        return sDefaultFavicon;
    }

    // All the state needed for a page
    protected static class PageState {
        String mUrl;
        String mOriginalUrl;
        String mTitle;
        SecurityState mSecurityState;
        // This is non-null only when mSecurityState is
        // SECURITY_STATE_BAD_CERTIFICATE.
        SslError mSslCertificateError;
        Bitmap mFavicon;
        boolean mIsBookmarkedSite;
        boolean mIncognito;

        PageState(Context c, boolean incognito) {
            mIncognito = incognito;
            mOriginalUrl = mUrl = "";
            mTitle = c.getString(R.string.blank_page);
            mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
        }

        PageState(Context c, boolean incognito, String url, Bitmap favicon) {
            mIncognito = incognito;
            mOriginalUrl = mUrl = url;
            if (URLUtil.isHttpsUrl(url)) {
                mSecurityState = SecurityState.SECURITY_STATE_SECURE;
            } else {
                mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
            }
            mFavicon = favicon;
        }

    }

    // The current/loading page's state
    protected PageState mCurrentState;

    // Used for saving and restoring each Tab
    static final String ID = "ID";
    static final String CURRURL = "currentUrl";
    static final String CURRTITLE = "currentTitle";
    static final String PARENTTAB = "parentTab";
    static final String APPID = "appid";
    static final String INCOGNITO = "privateBrowsingEnabled";
    static final String USERAGENT = "useragent";
    static final String CLOSEFLAG = "closeOnBack";
    static final String SHOW_HOMEPAGE = "showHomePage";

    // Container class for the next error dialog that needs to be displayed
    private class ErrorDialog {
        public final int mTitle;
        public final String mDescription;
        public final int mError;

        ErrorDialog(int title, String desc, int error) {
            mTitle = title;
            mDescription = desc;
            mError = error;
        }
    }

    private void processNextError() {
        if (mQueuedErrors == null) {
            return;
        }
        // The first one is currently displayed so just remove it.
        mQueuedErrors.removeFirst();
        if (mQueuedErrors.size() == 0) {
            mQueuedErrors = null;
            return;
        }
        showError(mQueuedErrors.getFirst());
    }

    private DialogInterface.OnDismissListener mDialogListener =
            new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface d) {
                    processNextError();
                }
            };
    private LinkedList<ErrorDialog> mQueuedErrors;

    private void queueError(int err, String desc) {
        if (mQueuedErrors == null) {
            mQueuedErrors = new LinkedList<ErrorDialog>();
        }
        for (ErrorDialog d : mQueuedErrors) {
            if (d.mError == err) {
                // Already saw a similar error, ignore the new one.
                return;
            }
        }
        ErrorDialog errDialog = new ErrorDialog(
                err == WebViewClient.ERROR_FILE_NOT_FOUND ?
                        R.string.browserFrameFileErrorLabel :
                        R.string.browserFrameNetworkErrorLabel,
                desc, err);
        mQueuedErrors.addLast(errDialog);

        // Show the dialog now if the queue was empty and it is in foreground
        if (mQueuedErrors.size() == 1 && mInForeground) {
            showError(errDialog);
        }
    }

    private void showError(ErrorDialog errDialog) {
        if (mInForeground) {
            AlertDialog d = new AlertDialog.Builder(mContext)
                    .setTitle(errDialog.mTitle)
                    .setMessage(errDialog.mDescription)
                    .setPositiveButton(R.string.ok, null)
                    .create();
            d.setOnDismissListener(mDialogListener);
            d.show();
        }
    }

    private static final String mLocationString = "location:";
    private static final String mCodeString = "code:";
    static final int POST_DELAY = 300;
    Runnable mReadingRunnable = new Runnable() {
        public void run() {
            if (getWebView() != null && getWebView().getParent() != null && !mWebViewController.getUi().isReadModeWindowShowing() && !((Controller)mWebViewController).isActivityPaused()) {
                mReadModeHelper.reset();
                if (mPageLoadProgress >= PROGRESS_MAX && !StringUtils.inBlackList(getWebView().getUrl())) {
                    getWebView().loadUrl(
                            "javascript: if ((window.location.protocol + '//' + window.location.host + '/' != window.location)"
                                    + " && (window.location.protocol + '//' + window.location.host + '/#' != window.location))"
                                    + " console.log('" + mLocationString
                                    + "' + window.location.href +'\\n' + '" + mCodeString
                                    + "' + document.getElementsByTagName('html')[0].innerHTML);");
                }
            }
        }
    };
    // -------------------------------------------------------------------------
    // WebViewClient implementation for the main WebView
    // -------------------------------------------------------------------------

    private final WebViewClient mWebViewClient = new WebViewClient() {
        private Message mDontResend;
        private Message mResend;
        private PhoneUi ui;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {

            if (LOGD_ENABLED) {
                Log.d(LOGTAG, "onPageStarted url: " + url + ", webview: " + view);
            }

            if (url != null && url.startsWith("http://www.hugeurl.com")) {// for cmcc test
                view.loadUrl("http://www.baidu.com");
                return;
            }

            ui = (PhoneUi) mWebViewController.getUi();
            mHandler.removeCallbacks(mReadingRunnable);
            mReadModeHelper.reset();
            mInPageLoad = true;
            mUpdateThumbnail = true;
            mPageLoadProgress = INITIAL_PROGRESS;
            mCurrentState = new PageState(mContext,
                    view.isPrivateBrowsingEnabled(), url, favicon);

            // If we start a touch icon load and then load a new page, we don't
            // want to cancel the current touch icon loader. But, we do want to
            // create a new one when the touch icon url is known.
            if (mTouchIconLoader != null) {
                mTouchIconLoader.mTab = null;
                mTouchIconLoader = null;
            }

            // reset the error console
            if (mErrorConsole != null) {
                mErrorConsole.clearErrorMessages();
                if (mWebViewController.shouldShowErrorConsole()) {
                    mErrorConsole.showConsole(ErrorConsoleView.SHOW_NONE);
                }
            }

            // Cancel the auto-login process.
            if (mDeviceAccountLogin != null) {
                mDeviceAccountLogin.cancel();
                mDeviceAccountLogin = null;
                mWebViewController.hideAutoLogin(Tab.this);
            }

            // finally update the UI in the activity if it is in the foreground
            mWebViewController.onPageStarted(Tab.this, view, favicon);
            updateBookmarkedStatus();
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            if (LOGD_ENABLED) {
                Log.d(LOGTAG, "onPageFinished url: " + url + ", webview: " + view);
            }
            syncCurrentState(view, url);
            mWebViewController.onPageFinished(Tab.this);

            // update bookmark status after mCurrentState.mUrl has been set.
            updateBookmarkedStatus();

            // capture the webview opened in background,
            // then remove it from content view.
            // seems only active tab are kept in content view
            //if (ui != null) // as it can't get capture of background tab with swe engine, comment it. can open again if resolve stability issue of swe, or use other engine
            //    captureInBackGround();

            if (view.getParent() != null) {
                mHandler.removeCallbacks(mReadingRunnable);
                if (getWebView().getSettings().getLoadsImagesAutomatically() && !mSettings.hasDesktopUseragent(getWebView()) && !((Controller)mWebViewController).isActivityPaused())
                    mHandler.postDelayed(mReadingRunnable, StringUtils.getDelayTime(url));
            } else {// pause media in background tab to reserve resource
                view.onPause();
            }
        }

        /*private void captureInBackGround() {
            if (!mInForeground) {
                if (ui.isNewTabAnimating() || ui.isCloseTabAnimating() || ui.isShowNavAnimating()) {// wait recursively to capture if it is animating
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            captureInBackGround();
                        }
                    }, POST_DELAY);
                }
                else {
                    ui.attachTab(Tab.this, false);// must attach, otherwise can't capture it
                    //getWebView().onPause();
                    //getWebView().onResume(); // this can fix capture blank in background, but will cause random crash
                    mHandler.postDelayed(new Runnable() {// need wait a short time to capture after attach, otherwise only capture white screen
                        @Override
                        public void run() {
                            getWebViewCapture(new ValueCallback<Bitmap>() {
                                @Override
                                public void onReceiveValue(Bitmap webCapture) {
                                    capture(webCapture);
                                    if (Tab.this != ui.getActiveTab())
                                        ui.detachTab(Tab.this);
                                    mWebViewController.getTabControl().freeMemory(); // it will crash for no resource if open too many background tabs
                                }
                            });
                        }
                    }, 1000);
                }
            }
        }*/

        // return true if want to hijack the url to let another app to handle it
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mInForeground) {
                return mWebViewController.shouldOverrideUrlLoading(Tab.this,
                        view, url);
            } else {
                return false;
            }
        }

        /**
         * Updates the security state. This method is called when we discover
         * another resource to be loaded for this page (for example,
         * javascript). While we update the security state, we do not update the
         * lock icon until we are done loading, as it is slightly more secure
         * this way.
         */
        @Override
        public void onLoadResource(WebView view, String url) {
            if (url != null && url.length() > 0) {
                // It is only if the page claims to be secure that we may have
                // to update the security state:
                if (mCurrentState.mSecurityState == SecurityState.SECURITY_STATE_SECURE) {
                    // If NOT a 'safe' url, change the state to mixed content!
                    if (!(URLUtil.isHttpsUrl(url) || URLUtil.isDataUrl(url)
                    || URLUtil.isAboutUrl(url))) {
                        mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_MIXED;
                    }
                }
            }
        }

        /**
         * Show a dialog informing the user of the network error reported by
         * WebCore if it is in the foreground.
         */
        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            if (errorCode != WebViewClient.ERROR_HOST_LOOKUP &&
                    errorCode != WebViewClient.ERROR_TIMEOUT &&
                    errorCode != WebViewClient.ERROR_CONNECT &&
                    errorCode != WebViewClient.ERROR_BAD_URL &&
                    errorCode != WebViewClient.ERROR_UNSUPPORTED_SCHEME &&
                    errorCode != WebViewClient.ERROR_FILE) {
                queueError(errorCode, description);

                // Don't log URLs when in private browsing mode
                if (!isPrivateBrowsingEnabled()) {
                    Log.e(LOGTAG, "onReceivedError " + errorCode + " " + failingUrl
                            + " " + description);
                }
            }
        }

        /**
         * Check with the user if it is ok to resend POST data as the page they
         * are trying to navigate to is the result of a POST.
         */
        @Override
        public void onFormResubmission(WebView view, final Message dontResend,
                final Message resend) {
            if (!mInForeground) {
                dontResend.sendToTarget();
                return;
            }
            if (mDontResend != null) {
                Log.w(LOGTAG, "onFormResubmission should not be called again "
                        + "while dialog is still up");
                dontResend.sendToTarget();
                return;
            }
            mDontResend = dontResend;
            mResend = resend;
            new AlertDialog.Builder(mContext).setTitle(
                    R.string.browserFrameFormResubmitLabel).setMessage(
                    R.string.browserFrameFormResubmitMessage)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (mResend != null) {
                                        mResend.sendToTarget();
                                        mResend = null;
                                        mDontResend = null;
                                    }
                                }
                            }).setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (mDontResend != null) {
                                        mDontResend.sendToTarget();
                                        mResend = null;
                                        mDontResend = null;
                                    }
                                }
                            }).setOnCancelListener(new OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            if (mDontResend != null) {
                                mDontResend.sendToTarget();
                                mResend = null;
                                mDontResend = null;
                            }
                        }
                    }).show();
        }

        /**
         * Insert the url into the visited history database.
         * 
         * @param url The url to be inserted.
         * @param isReload True if this url is being reloaded. FIXME: Not sure
         *            what to do when reloading the page.
         */
        @Override
        public void doUpdateVisitedHistory(WebView view, String url,
                boolean isReload) {
            mWebViewController.doUpdateVisitedHistory(Tab.this, isReload);
        }

        /**
         * Displays SSL error(s) dialog to the user.
         */
        @Override
        public void onReceivedSslError(final WebView view,
                final SslErrorHandler handler, final SslError error) {
            if (!mInForeground) {
                handler.cancel();
                setSecurityState(SecurityState.SECURITY_STATE_NOT_SECURE);
                return;
            }
            if (mAlterDialog != null && mAlterDialog.isShowing()) {
                return;
            }
            SslCertificate cert = error.getCertificate();
            // Log.e("Tab onReceivedSslError", "SslError " + error.toString());
            if (cert != null) {
                // FIXME: ignore SSL_UNTRUSTED error issued by GoAgent
                if ((error.getPrimaryError() == SslError.SSL_UNTRUSTED)
                        && cert.getIssuedBy()
                                .getDName()
                                .equalsIgnoreCase(
                                        "L=Cernet,C=CN,ST=Internet,CN=GoAgent CA,O=GoAgent,OU=GoAgent Root")) {
                    handler.proceed();
                    handleProceededAfterSslError(error);
                    return;
                }
            }
            if (mSettings.showSecurityWarnings()) {
                mAlterDialog = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.security_warning)
                        .setMessage(R.string.ssl_warnings_header)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(R.string.ssl_continue,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        handler.proceed();
                                        handleProceededAfterSslError(error);
                                    }
                                })
                        .setNeutralButton(R.string.view_certificate,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        mWebViewController.showSslCertificateOnError(
                                                view, handler, error);
                                    }
                                })
                        .setNegativeButton(R.string.ssl_go_back,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        dialog.cancel();
                                    }
                                })
                        .setOnCancelListener(
                                new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        handler.cancel();
                                        setSecurityState(SecurityState.SECURITY_STATE_NOT_SECURE);
                                        mWebViewController.onUserCanceledSsl(Tab.this);
                                    }
                                })
                        .show();
            } else {
                handler.proceed();
            }
        }

        //temp closed
//        @Override
//        public void onRendererCrash(WebView view, boolean crashedWhileOomProtected) {
//            hasCrashed = true;
//            if (LOGD_ENABLED)
//                Log.e(LOGTAG, "Tab Crashed"+ ", webview: " + view + ", crashedWhileOomProtected: " + crashedWhileOomProtected);
//            if (view.getParent() != null
//                && mReadModeHelper.getReadView() == null
//                && !crashedWhileOomProtected
//                && !((Controller)mWebViewController).isActivityPaused()){
//                    if (LOGD_ENABLED)
//                        Log.e(LOGTAG, "In OnRendererCrash and do view.reload()");
//                    view.reload();
//                }
//        }

        /**
         * Handles an HTTP authentication request.
         *
         * @param handler The authentication handler
         * @param host The host
         * @param realm The realm
         */
        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                final HttpAuthHandler handler, final String host,
                final String realm) {
            mWebViewController.onReceivedHttpAuthRequest(Tab.this, view, handler, host, realm);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                String url) {
            WebResourceResponse res = null;//HomeProvider.shouldInterceptRequest(
                    //mContext, url);
            return res;
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            if (!mInForeground && Tab.this != mWebViewController.getTabControl().getCurrentTab()) {
                return false;
            }
            return mWebViewController.shouldOverrideKeyEvent(event);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            if (!mInForeground) {
                return;
            }
            if (!mWebViewController.onUnhandledKeyEvent(event)) {
                super.onUnhandledKeyEvent(view, event);
            }
        }

        @Override
        public void onReceivedLoginRequest(WebView view, String realm,
                String account, String args) {
            new DeviceAccountLogin(mWebViewController.getActivity(), view, Tab.this,
                    mWebViewController)
                    .handleLogin(realm, account, args);
        }

    };

    private void syncCurrentState(WebView view, String url) {
        // Sync state (in case of stop/timeout)
        if (view == null) {
            return;
        }
        if (!URLUtil.isAboutUrl(view.getUrl()) && mCurrentState != null) {
            if ((view.getUrl() != null) && !view.getUrl().matches(UrlInputView.MATCHES)) {
                mCurrentState.mUrl = view.getUrl();
            } else {
                mCurrentState.mUrl = getUrl();
            }
        }
        if (mCurrentState.mUrl == null) {
            mCurrentState.mUrl = "";
        }
        mCurrentState.mOriginalUrl = view.getOriginalUrl();
        mCurrentState.mTitle = view.getTitle();
        if (mCurrentState.mTitle == null)
            mCurrentState.mTitle = mContext.getString(R.string.blank_page);
        mCurrentState.mFavicon = view.getFavicon();
        if (!URLUtil.isHttpsUrl(mCurrentState.mUrl)) {
            // In case we stop when loading an HTTPS page from an HTTP page
            // but before a provisional load occurred
            mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
            mCurrentState.mSslCertificateError = null;
        }
        mCurrentState.mIncognito = view.isPrivateBrowsingEnabled();
    }

    public void restoreBackForwardList(Bundle state) {
        if (mMainView == null)
            return;

        setShowHomePage(false);
        byte[] latestState = state.getByteArray(DataController.BACKFORWARD_LIST);
        if (latestState == null) // it will be null if user update from old database
            mMainView.loadUrl(state.getString("url", ""));
        else
            mMainView.restoreState(state);
    }

    Bundle saveBackForwardList() {
        if (isPrivateBrowsingEnabled()) return null; // not record private tab

        String url = getUrl();
        if (url == null || "".equals(url)) return null;
        WebView wv = getWebView();
        Bundle bundle = new Bundle();
        bundle.putLong(BrowserContract.History.DATE_CREATED, System.currentTimeMillis());
        bundle.putString(BrowserContract.History.URL, url);
        if (wv == null) {// if kill browser and launch browser again, wv may be null
            bundle.putString(BrowserContract.History.TITLE, mCurrentState.mTitle);
        }
        else {
            bundle.putString(BrowserContract.History.TITLE, getTitle());
            if (wv.getProgress() == 100) // otherwise it will restore a blank page next time?
                wv.saveState(bundle);
        }
        return bundle;
    }

    // Called by DeviceAccountLogin when the Tab needs to have the auto-login UI
    // displayed.
    void setDeviceAccountLogin(DeviceAccountLogin login) {
        mDeviceAccountLogin = login;
    }

    // Returns non-null if the title bar should display the auto-login UI.
    DeviceAccountLogin getDeviceAccountLogin() {
        return mDeviceAccountLogin;
    }

    public boolean isTabFullScreen() {
        return mFullScreen;
    }

    protected void setTabFullscreen(boolean fullScreen) {
        Controller controller = (Controller)mWebViewController;
        controller.getUi().showFullscreen(fullScreen);
        mFullScreen = fullScreen;
    }

    public boolean exitFullscreen() {
        if (mFullScreen) {
            Controller controller = (Controller)mWebViewController;
            controller.getUi().showFullscreen(false);
            if (getWebView() != null)
                //getWebView().exitFullscreen();
            mFullScreen = false;
            return true;
        }
        return false;
    }




    // -------------------------------------------------------------------------
    // WebChromeClient implementation for the main WebView
    // -------------------------------------------------------------------------
    public float mTopControlsOffsetYPix = 0;
    private final WebChromeClient mWebChromeClient = new WebChromeClient() {

        HashMap<String, Object> mJsMap = new HashMap<String, Object>();
        @Override
        public boolean onJsAlert(WebView view, String url, String message,
                JsResult result) {
            return handleJsPopup(url, message, result, JsDialogBlockHelper.ALERT);
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message,
                JsResult result) {
            return handleJsPopup(url, message, result, JsDialogBlockHelper.CONFIRM);
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message,
                String defaultValue, JsPromptResult result) {
            return handleJsPopup(url, message, result, JsDialogBlockHelper.PROMPT);
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message,
                JsResult result) {
            return handleJsPopup(url, message, result, JsDialogBlockHelper.UNLOAD);
        }

        private boolean handleJsPopup(String url, String message, JsResult result, int type) {
            if (mJsMap.containsKey(url)) {
                boolean block = (Boolean) mJsMap.get(url);
                if (!block) {
                    new JsDialogBlockHelper(result, type, null, message, url, mJsMap).showDialog(mContext);
                }
                else
                    result.confirm();
                return true;
            } else {
                mJsMap.put(url, false);
                return false;
            }
        }

        // Helper method to create a new tab or sub window.
        private void createWindow(final boolean dialog, final Message msg) {
            if (mIsRequestFocus) {
                mIsRequestFocus = false;
                return;
            }

            final WebView.WebViewTransport transport =
                    (WebView.WebViewTransport) msg.obj;
            // modify by smartisanos 2014-02-27
            // it is not support on sfo code tree if dialog is true
            // if (dialog) {
            // createSubWindow();
            // mWebViewController.attachSubWindow(Tab.this);
            // transport.setWebView(mSubView);
            // } else {
            final PhoneUi ui = (PhoneUi) mWebViewController.getUi();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!ui.isNewTabAnimating()) {
                        Bundle bundle = new Bundle();
                        bundle.putString("url", "");
                        final Tab newTab = ((BaseUi) ui).NewTab(BottomBarPhone.NEW_TAB_INNER_TAG, bundle);
                        if (newTab != null) {
                            newTab.setShowHomePage(false);
                            ((BaseUi) ui).newAnim(newTab, msg);
                        }
                    }
                }
            }, 0);
        }

//        @Override
//        public void toggleFullscreenModeForTab(boolean enterFullscreen) {
//            if (mWebViewController instanceof Controller) {
//                setTabFullscreen(enterFullscreen);
//            }
//        }
//
//        @Override
//        public void onOffsetsForFullscreenChanged(float topControlsOffsetYPix,
//                                                  float contentOffsetYPix,
//                                                  float overdrawBottomHeightPix) {
//            if (mWebViewController instanceof Controller) {
//                Controller controller = (Controller)mWebViewController;
//                controller.getUi().translateTitleBar(topControlsOffsetYPix);
//                mTopControlsOffsetYPix = topControlsOffsetYPix;
//            }
//        }
//
//        @Override
//        public boolean isTabFullScreen() {
//          return mFullScreen;
//        }

        HashMap<String, Object> mPopupMap = new HashMap<String, Object>();
        private CheckBox mCheckBox;
        private AlertDialog mBlockPopWinDlg;
        @Override
        public boolean onCreateWindow(final WebView view, final boolean dialog,
                final boolean userGesture, final Message resultMsg) {
            // only allow new window or sub window for the foreground case
            if (!mInForeground) {
                return false;
            }
            // Short-circuit if we can't create any more tabs or sub windows.
            if (dialog && mSubView != null) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.too_many_subwindows_dialog_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(R.string.too_many_subwindows_dialog_message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return false;
            } else if (!mWebViewController.getTabControl().canCreateNewTab()) {
                mWebViewController.getUi().closeTheLeastUsedTab();
            }

            // Short-circuit if this was a user gesture.
            if (userGesture || StringUtils.inOneKeyLoginList(view.getUrl())) {
                if (StringUtils.inOneKeyLoginList(view.getUrl())) {
                    WebView wv = new WebView(mContext); // for bug 38921, 42525. the web page will close this wv immediately
                    ((WebView.WebViewTransport)resultMsg.obj).setWebView(wv);
                    resultMsg.sendToTarget();
                } else {
                    createWindow(dialog, resultMsg);
                }
                return true;
            }

            if (mBlockPopWinDlg != null && mBlockPopWinDlg.isShowing())
                return false;

            final String url = view.getUrl();
            if (mPopupMap.containsKey(url)) {
                boolean blocked = (Boolean) mPopupMap.get(url);
                if (blocked)
                    return false;
            }
            else
                mPopupMap.put(url, false);

            // Allow the popup and create the appropriate window.
            final AlertDialog.OnClickListener allowListener =
                    new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface d,
                                int which) {
                            if (mPopupMap.containsKey(url) && mCheckBox != null)
                                mPopupMap.put(url, mCheckBox.isChecked());
                            createWindow(dialog, resultMsg);
                        }
                    };

            // Block the popup by returning a null WebView.
            final AlertDialog.OnClickListener blockListener =
                    new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface d, int which) {
                            if (mPopupMap.containsKey(url) && mCheckBox != null)
                                mPopupMap.put(url, mCheckBox.isChecked());
                            resultMsg.sendToTarget();
                        }
                    };

            // Build a confirmation dialog to display to the user.
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setIconAttribute(android.R.attr.alertDialogIcon);
            builder.setPositiveButton(R.string.allow, allowListener);
            builder.setNegativeButton(R.string.block, blockListener);
            builder.setCancelable(false);
            if (mPopupMap.containsKey(url)) {
                View dialogView = LayoutInflater.from(mContext).inflate(
                        R.layout.js_block_prompt, null);
                mCheckBox = (CheckBox) dialogView.findViewById(R.id.block);
                ((TextView) dialogView.findViewById(R.id.message)).setText(R.string.popup_window_attempt);
                builder.setView(dialogView);
            } else
                builder.setMessage(R.string.popup_window_attempt);
            // Show the confirmation dialog.
            mBlockPopWinDlg = builder.show();

            return true;
        }

        boolean mIsRequestFocus = false;
        @Override
        public void onRequestFocus(WebView view) {
            if (!mInForeground) {
                mIsRequestFocus = true;
                mWebViewController.switchToTab(Tab.this, true);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (mParent != null) {
                // JavaScript can only close popup window.
                if (mInForeground) {
                    mWebViewController.switchToTab(mParent);
                }
            }
            mWebViewController.closeTab(Tab.this);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            mPageLoadProgress = (newProgress < INITIAL_PROGRESS) ? INITIAL_PROGRESS : newProgress;
            if (newProgress >= PROGRESS_MAX) {
                mInPageLoad = false;
            }
            mWebViewController.onProgressChanged(Tab.this);
            if (mUpdateThumbnail && newProgress == PROGRESS_MAX) {
                mUpdateThumbnail = false;
            }

            if (newProgress >= 78 && StringUtils.getDelayTime(view.getUrl()) < 0 && !isBtnReadModeShowing()) {
                view.loadUrl(
                        "javascript: console.log('" + mLocationString
                                + "' + window.location.href +'\\n' + '" + mCodeString
                                + "' + document.getElementsByTagName('html')[0].innerHTML);");
            }
        }

        @Override
        public void onReceivedTitle(WebView view, final String title) {
            mCurrentState.mTitle = title;
            mWebViewController.onReceivedTitle(Tab.this, title);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            mCurrentState.mFavicon = icon;
            mWebViewController.onFavicon(Tab.this, view, icon);
        }

        @Override
        public void onReceivedTouchIconUrl(WebView view, String url,
                boolean precomposed) {
            final ContentResolver cr = mContext.getContentResolver();
            // Let precomposed icons take precedence over non-composed
            // icons.
            if (precomposed && mTouchIconLoader != null) {
                mTouchIconLoader.cancel(false);
                mTouchIconLoader = null;
            }
            // Have only one async task at a time.
            if (mTouchIconLoader == null) {
                mTouchIconLoader = new DownloadTouchIcon(Tab.this,
                        mContext, cr, view);
                mTouchIconLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,url);
            }
        }

        @Override
        public void onShowCustomView(View view,
                CustomViewCallback callback) {
            Activity activity = mWebViewController.getActivity();
            if (activity != null) {
                onShowCustomView(view, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, callback);
            }
        }

        @Override
        public void onShowCustomView(View view, int requestedOrientation,
                CustomViewCallback callback) {
            if (mInForeground)
                mWebViewController.showCustomView(Tab.this, view,
                        requestedOrientation, callback);
        }

        @Override
        public void onHideCustomView() {
            if (mInForeground)
                mWebViewController.hideCustomView();
        }

        /**
         * The origin has exceeded its database quota.
         * 
         * @param url the URL that exceeded the quota
         * @param databaseIdentifier the identifier of the database on which the
         *            transaction that caused the quota overflow was run
         * @param currentQuota the current quota for the origin.
         * @param estimatedSize the estimated size of the database.
         * @param totalUsedQuota is the sum of all origins' quota.
         * @param quotaUpdater The callback to run when a decision to allow or
         *            deny quota has been made. Don't forget to call this!
         */
        @Override
        public void onExceededDatabaseQuota(String url,
                String databaseIdentifier, long currentQuota, long estimatedSize,
                long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
            mSettings.getWebStorageSizeManager()
                    .onExceededDatabaseQuota(url, databaseIdentifier,
                            currentQuota, estimatedSize, totalUsedQuota,
                            quotaUpdater);
        }

        /**
         * The Application Cache has exceeded its max size.
         * 
         * @param spaceNeeded is the amount of disk space that would be needed
         *            in order for the last appcache operation to succeed.
         * @param totalUsedQuota is the sum of all origins' quota.
         * @param quotaUpdater A callback to inform the WebCore thread that a
         *            new app cache size is available. This callback must always
         *            be executed at some point to ensure that the sleeping
         *            WebCore thread is woken up.
         */
        @Override
        public void onReachedMaxAppCacheSize(long spaceNeeded,
                long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
            mSettings.getWebStorageSizeManager()
                    .onReachedMaxAppCacheSize(spaceNeeded, totalUsedQuota,
                            quotaUpdater);
        }

        private String mOrigin;
        private GeolocationPermissions.Callback mCallback;

        /**
         * Instructs the browser to show a prompt to ask the user to set the
         * Geolocation permission state for the specified origin.
         * 
         * @param origin The origin for which Geolocation permissions are
         *            requested.
         * @param callback The callback to call once the user has set the
         *            Geolocation permission state.
         */
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                GeolocationPermissions.Callback callback) {
            mCallback = callback;
            mOrigin = origin;
            mGeolocationDialog = new AlertDialog.Builder(mContext)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(
                            String.format(
                                    mContext.getResources().getString(
                                            R.string.geolocation_permissions_prompt_message),
                                    getMessage(mOrigin)))
                    .setPositiveButton(R.string.geolocation_permissions_prompt_share,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Toast toast = Toast.makeText(
                                            mContext,
                                            R.string.geolocation_permissions_prompt_toast_allowed,
                                            Toast.LENGTH_LONG);
                                    //toast.setGravity(Gravity.BOTTOM, 0, 0);
                                    toast.show();
                                    mCallback.invoke(mOrigin, true, true);
                                }
                            })
                    .setNegativeButton(R.string.geolocation_permissions_prompt_dont_share,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Toast toast = Toast
                                            .makeText(
                                                    mContext,
                                                    R.string.geolocation_permissions_prompt_toast_disallowed,
                                                    Toast.LENGTH_LONG);
                                    //toast.setGravity(Gravity.BOTTOM, 0, 0);
                                    toast.show();
                                    mCallback.invoke(mOrigin, false, true);
                                }
                            })
                    .show();
        }

        public String getMessage(String origin) {
            Uri uri = Uri.parse(origin);
            return "http".equals(uri.getScheme()) ? origin.substring(7) : origin;
        }

        /**
         * Instructs the browser to hide the Geolocation permissions prompt.
         */
        @Override
        public void onGeolocationPermissionsHidePrompt() {
            if (mInForeground && mGeolocationPermissionsPrompt != null) {
                mGeolocationPermissionsPrompt.hide();
            }
        }

        /*
         * Adds a JavaScript error message to the system log and if the JS
         * console is enabled in the about:debug options, to that console also.
         * @param consoleMessage the message object.
         */
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage.message() != null &&
              consoleMessage.message().startsWith(mLocationString)) {
                if (consoleMessage.message().length() > mLocationString.length()) {
                    int locationEnd = consoleMessage.message().indexOf('\n', mLocationString.length());
                    if (locationEnd > 0) {
                        String location = consoleMessage.message().substring(mLocationString.length(), locationEnd);
                        if (consoleMessage.message().length() > (locationEnd + mCodeString.length()+1)) {
                            String html = consoleMessage.message().substring(locationEnd + mCodeString.length()+1);
                            // not show read mode for mobile page, unless it in white list
                            if (StringUtils.inWhiteList(location) || !StringUtils.inMobileList(html) && !((Controller)mWebViewController).isActivityPaused()) {
                                html = StringUtils.prepareHtml(html);
                                mReadModeHelper.showHtml(html, location);
                            }
                        }
                    }
                }
                return true;
            }

            return Tab.this.onConsoleMessage(consoleMessage);
        }

        /**
         * Ask the browser for an icon to represent a <video> element. This icon
         * will be used if the Web page did not specify a poster attribute.
         * 
         * @return Bitmap The icon or null if no such icon is available.
         */
        @Override
        public Bitmap getDefaultVideoPoster() {
            if (mInForeground) {
                return mWebViewController.getDefaultVideoPoster();
            }
            return null;
        }

        /**
         * Ask the host application for a custom progress view to show while a
         * <video> is loading.
         * 
         * @return View The progress view.
         */
        @Override
        public View getVideoLoadingProgressView() {
            if (mInForeground) {
                return mWebViewController.getVideoLoadingProgressView();
            }
            return null;
        }

//        @Override
//        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
//            if (mInForeground) {
//                mWebViewController.openFileChooser(uploadMsg, acceptType, capture);
//            } else {
//                uploadMsg.onReceiveValue(null);
//            }
//        }

//        @Override
//        public void showFileChooser(ValueCallback<String[]> uploadFilePaths, String acceptTypes,
//                boolean capture) {
//            if (mInForeground) {
//                mWebViewController.showFileChooser(uploadFilePaths, acceptTypes, capture);
//            } else {
//                uploadFilePaths.onReceiveValue(null);
//            }
//        }

        /**
         * Deliver a list of already-visited URLs
         */
        @Override
        public void getVisitedHistory(final ValueCallback<String[]> callback) {
            mWebViewController.getVisitedHistory(callback);
        }

    };

    // -------------------------------------------------------------------------
    // WebViewClient implementation for the sub window
    // -------------------------------------------------------------------------

    // Subclass of WebViewClient used in subwindows to notify the main
    // WebViewClient of certain WebView activities.
    private static class SubWindowClient extends WebViewClient {
        // The main WebViewClient.
        private final WebViewClient mClient;
        private final WebViewController mController;

        SubWindowClient(WebViewClient client, WebViewController controller) {
            mClient = client;
            mController = controller;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // Unlike the others, do not call mClient's version, which would
            // change the progress bar. However, we do want to remove the
            // find or select dialog.
            mController.endActionMode();
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url,
                boolean isReload) {
            mClient.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return mClient.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                SslError error) {
            mClient.onReceivedSslError(view, handler, error);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view,
                HttpAuthHandler handler, String host, String realm) {
            mClient.onReceivedHttpAuthRequest(view, handler, host, realm);
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend,
                Message resend) {
            mClient.onFormResubmission(view, dontResend, resend);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                String description, String failingUrl) {
            mClient.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view,
                android.view.KeyEvent event) {
            return mClient.shouldOverrideKeyEvent(view, event);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view,
                android.view.KeyEvent event) {
            mClient.onUnhandledKeyEvent(view, event);
        }
    }

    // -------------------------------------------------------------------------
    // WebChromeClient implementation for the sub window
    // -------------------------------------------------------------------------

    private class SubWindowChromeClient extends WebChromeClient {
        // The main WebChromeClient.
        private final WebChromeClient mClient;

        SubWindowChromeClient(WebChromeClient client) {
            mClient = client;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            mClient.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean dialog,
                boolean userGesture, android.os.Message resultMsg) {
            return mClient.onCreateWindow(view, dialog, userGesture, resultMsg);
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (window != mSubView) {
                Log.e(LOGTAG, "Can't close the window");
            }
            mWebViewController.dismissSubWindow(Tab.this);
        }
    }

    // -------------------------------------------------------------------------

    // Construct a new tab
    Tab(WebViewController wvcontroller, WebView w) {
        this(wvcontroller, w, null);
    }

    Tab(WebViewController wvcontroller, Bundle state) {
        this(wvcontroller, null, state);
    }

    Tab(WebViewController wvcontroller, WebView w, Bundle state) {
        mWebViewController = wvcontroller;
        mContext = mWebViewController.getContext();
        mSettings = BrowserSettings.getInstance();
        mCurrentState = new PageState(mContext, w != null
         ? w.isPrivateBrowsingEnabled() : false);
        setTimeStamp();
        mInPageLoad = false;
        mInForeground = false;
        mWebViewDestroyedByMemoryMonitor = false;

        mDownloadListener = new BrowserDownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                    String contentDisposition, String mimetype, String referer,
                    long contentLength) {
                mWebViewController.onDownloadStart(Tab.this, url, userAgent, contentDisposition,
                        mimetype, referer, contentLength);
            }
        };

        /*mWebBackForwardListClient = new WebBackForwardListClient() {
            @Override
            public void onNewHistoryItem(WebHistoryItem item) {
                if (mClearHistoryUrlPattern != null) {
                    boolean match =
                            mClearHistoryUrlPattern.matcher(item.getOriginalUrl()).matches();
                    if (LOGD_ENABLED) {
                        Log.d(LOGTAG, "onNewHistoryItem: match=" + match + "\n\t"
                                + item.getUrl() + "\n\t"
                                + mClearHistoryUrlPattern);
                    }
                    if (match) {
                        if (mMainView != null) {
                            mMainView.clearHistory();
                        }
                    }
                    mClearHistoryUrlPattern = null;
                }
            }
        };*/

        mCaptureWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.tab_thumbnail_width);
        mCaptureHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.tab_thumbnail_height);
        mCaptureWidthHroizontal = mContext.getResources().getDimensionPixelSize(
                R.dimen.tab_thumbnail_width_h);
        mCaptureHeightHroizontal = mContext.getResources().getDimensionPixelSize(
                R.dimen.tab_thumbnail_height_h);
        updateShouldCaptureThumbnails();
        restoreState(state);
        if (getId() == -1) {
            mId = TabControl.getNextId();
        }
        setWebView(w);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                switch (m.what) {
                    case MSG_CAPTURE:
                        //capture();
                        //captureH();
                        break;
                    case MSG_CAPTURE_H:
                        //captureH();
                        break;
                }
            }
        };
    }

    public boolean shouldUpdateThumbnail() {
        return mUpdateThumbnail;
    }

    /**
     * This is used to get a new ID when the tab has been preloaded, before it
     * is displayed and added to TabControl. Preloaded tabs can be created
     * before restoreInstanceState, leading to overlapping IDs between the
     * preloaded and restored tabs.
     */
    public void refreshIdAfterPreload() {
        mId = TabControl.getNextId();
    }

    private void createBitmap() {
        mCaptureCached.bitmapCapture = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight,
                Bitmap.Config.ARGB_8888);
        mCaptureCached.bitmapCapture.eraseColor(Color.WHITE);
    }
    private void createBitmapH() {
        mCaptureCached.bitmapCaptureH = Bitmap.createBitmap(mCaptureWidthHroizontal,
                mCaptureHeightHroizontal,
                Bitmap.Config.ARGB_8888);
        mCaptureCached.bitmapCaptureH.eraseColor(Color.WHITE);
    }
    public void updateShouldCaptureThumbnails() {
        if (mWebViewController.shouldCaptureThumbnails()) {
            synchronized (Tab.this) {
                if (mCaptureCached == null) {
                    mCaptureCached = new CaptureInfo();
                    createBitmap();
                    createBitmapH();
                    if (mInForeground) {
                        postCapture();
                        // postHCapture();
                    }
                }
            }
        } else {
            synchronized (Tab.this) {
                mCaptureCached.bitmapCapture = null;
                mCaptureCached.bitmapCaptureH = null;
                mCaptureCached = null;
                deleteThumbnail();
                // deleteLandThumbnail();
            }
        }
    }

    public void setController(WebViewController ctl) {
        mWebViewController = ctl;
        updateShouldCaptureThumbnails();
    }

    public long getId() {
        return mId;
    }

    void setWebView(WebView w) {
        setWebView(w, true);
    }

    public boolean isNativeActive(){
        if (mMainView == null)
            return false;
        return true;
    }

    public void setTimeStamp(){
        Date d = new Date();
        mTimestamp = (new Timestamp(d.getTime()));
    }

    public Timestamp getTimestamp() {
        return mTimestamp;
    }

    /**
     * Sets the WebView for this tab, correctly removing the old WebView from
     * the container view.
     */
    void setWebView(WebView w, boolean restore) {
        if (mMainView == w) {
            return;
        }
        // If the WebView is changing, the page will be reloaded, so any ongoing
        // Geolocation permission requests are void.
        if (mGeolocationPermissionsPrompt != null) {
            mGeolocationPermissionsPrompt.hide();
        }

        mWebViewController.onSetWebView(this, w);

        if(mReadModeHelper==null)
            //mReadModeHelper = new ReadModeHelper(mContext, this, mWebViewController);

        if (mMainView != null) {
            if (w != null) {
                syncCurrentState(w, null);
            } /*else {
                mCurrentState = new PageState(mContext, mMainView.isPrivateBrowsingEnabled());
                if (mWebViewDestroyedByMemoryMonitor) {
                    * If tab was destroyed as a result of the MemoryMonitor
                    * then we need to restore the state properties
                    * from the old WebView (mMainView)
                    syncCurrentState(mMainView, null);
                    mWebViewDestroyedByMemoryMonitor = false;
                }
            }*/
        }
        // set the new one
        mMainView = w;
        // attach the WebViewClient, WebChromeClient and DownloadListener
        if (mMainView != null) {
            mMainView.setWebViewClient(mWebViewClient);
            mMainView.setWebChromeClient(mWebChromeClient);
            // Attach DownloadManager so that downloads can start in an active
            // or a non-active window. This can happen when going to a site that
            // does a redirect after a period of time. The user could have
            // switched to another tab while waiting for the download to start.
            mMainView.setDownloadListener(mDownloadListener);
            if (restore && (mSavedState != null)) {
                restoreUserAgent();
                WebBackForwardList restoredState = mMainView.restoreState(mSavedState);
                if (restoredState == null || restoredState.getSize() == 0) {
                    if (mCurrentState.mOriginalUrl == null){
                        mCurrentState.mOriginalUrl = getUrl();
                    }
                    Log.w(LOGTAG, "Failed to restore WebView state!");
                    loadUrl(mCurrentState.mOriginalUrl, null);
                }
                mSavedState = null;
            }
        }
    }

    public void destroyThroughMemoryMonitor() {
        mWebViewDestroyedByMemoryMonitor = true;
        destroy();
    }

    /**
     * Destroy the tab's main WebView and subWindow if any
     */
    void destroy() {
        if (mMainView != null) {
            mMainView.removeAllViews();
            mMainView.setWebViewClient(null);
            mMainView.setWebChromeClient(null);
            mMainView.setDownloadListener(null);
            dismissSubWindow();
            // save the WebView to call destroy() after detach it from the tab
            WebView webView = mMainView;
            setWebView(null);
            webView.destroy();
        }
        if (mErrorConsole != null) {
            mErrorConsole.setWebView(null);
            mErrorConsole = null;
        }
        if (mReadModeHelper != null) {
            mReadModeHelper.destroy();
            mReadModeHelper = null;
        }
    }

    public void recycle() {
        if (mCaptureCached != null) {
            if (mCaptureCached.bitmapCapture != null)
                mCaptureCached.bitmapCapture.recycle();
            if (mCaptureCached.bitmapCaptureH != null)
                mCaptureCached.bitmapCaptureH.recycle();
            if(mWebCaptue != null)
                mWebCaptue.recycle();
        }
    }

    /**
     * Remove the tab from the parent
     */
    void removeFromTree() {
        // detach the children
        if (mChildren != null) {
            for (Tab t : mChildren) {
                t.setParent(null);
            }
        }
        // remove itself from the parent list
        if (mParent != null && mParent.mChildren != null) {
            mParent.mChildren.remove(this);
        }
        deleteThumbnail();
//        deleteLandThumbnail();
    }

    /**
     * Create a new subwindow unless a subwindow already exists.
     * 
     * @return True if a new subwindow was created. False if one already exists.
     */
    boolean createSubWindow() {
        if (mSubView == null) {
            mWebViewController.createSubWindow(this);
            mSubView.setWebViewClient(new SubWindowClient(mWebViewClient,
              mWebViewController));
            mSubView.setWebChromeClient(new SubWindowChromeClient(
              mWebChromeClient));
            // Set a different DownloadListener for the mSubView, since it will
            // just need to dismiss the mSubView, rather than close the Tab
            mSubView.setDownloadListener(new BrowserDownloadListener() {
                public void onDownloadStart(String url, String userAgent,
                        String contentDisposition, String mimetype, String referer,
                        long contentLength) {
                    mWebViewController.onDownloadStart(Tab.this, url, userAgent,
                            contentDisposition, mimetype, referer, contentLength);
                    if (mSubView.copyBackForwardList().getSize() == 0) {
                        // This subwindow was opened for the sole purpose of
                        // downloading a file. Remove it.
                        mWebViewController.dismissSubWindow(Tab.this);
                    }
                }
            });
            mSubView.setOnCreateContextMenuListener(mWebViewController.getActivity());
            return true;
        }
        return false;
    }

    public boolean isBtnReadModeShowing() {
        return mReadModeHelper == null ? false : mReadModeHelper.isBtnReadModeShowing();
    }


    private boolean onConsoleMessage(ConsoleMessage consoleMessage){
        if (mInForeground) {
            // call getErrorConsole(true) so it will create one if needed
            ErrorConsoleView errorConsole = getErrorConsole(true);
            errorConsole.addErrorMessage(consoleMessage);
            if (mWebViewController.shouldShowErrorConsole()
                    && errorConsole.getShowState() !=
                    ErrorConsoleView.SHOW_MAXIMIZED) {
                errorConsole.showConsole(ErrorConsoleView.SHOW_MINIMIZED);
            }
        }

        // Don't log console messages in private browsing mode
        if (isPrivateBrowsingEnabled())
            return true;

        String message = "Console: " + consoleMessage.message() + " "
                + consoleMessage.sourceId() + ":"
                + consoleMessage.lineNumber();

        switch (consoleMessage.messageLevel()) {
            case TIP:
                if (LOGD_ENABLED) {
                    Log.v(CONSOLE_LOGTAG, message);
                }
                break;
            case LOG:
                Log.i(CONSOLE_LOGTAG, message);
                break;
            case WARNING:
                Log.w(CONSOLE_LOGTAG, message);
                break;
            case ERROR:
                Log.e(CONSOLE_LOGTAG, message);
                break;
            case DEBUG:
                if (LOGD_ENABLED) {
                    Log.d(CONSOLE_LOGTAG, message);
                }
                break;
        }

        return true;
    }

     /**
     * Dismiss the subWindow for the tab.
     */
    void dismissSubWindow() {
        if (mSubView != null) {
            mWebViewController.endActionMode();
            mSubView.destroy();
            mSubView = null;
            mSubViewContainer = null;
        }
    }

    /**
     * Set the parent tab of this tab.
     */
    void setParent(Tab parent) {
        if (parent == this) {
            // throw new IllegalStateException("Cannot set parent to self!");
            return;
        }
        mParent = parent;
        // This tab may have been freed due to low memory. If that is the case,
        // the parent tab id is already saved. If we are changing that id
        // (most likely due to removing the parent tab) we must update the
        // parent tab id in the saved Bundle.
        if (mSavedState != null) {
            if (parent == null) {
                mSavedState.remove(PARENTTAB);
            } else {
                mSavedState.putLong(PARENTTAB, parent.getId());
            }
        }

        // Sync the WebView useragent with the parent
//        if (parent != null && mSettings.hasDesktopUseragent(parent.getWebView())
//                    != mSettings.hasDesktopUseragent(getWebView())) {
//            mSettings.toggleDesktopUseragent(getWebView());
//        }

        if (parent != null && parent.getId() == getId()) {
            // throw new IllegalStateException("Parent has same ID as child!");
        }
    }

    /**
     * If this Tab was created through another Tab, then this method returns
     * that Tab.
     * 
     * @return the Tab parent or null
     */
    public Tab getParent() {
        return mParent;
    }

    /**
     * When a Tab is created through the content of another Tab, then we
     * associate the Tabs.
     * 
     * @param child the Tab that was created from this Tab
     */
    void addChildTab(Tab child) {
        if (mChildren == null) {
            mChildren = new Vector<Tab>();
        }
        mChildren.add(child);
        child.setParent(this);
    }

    Vector<Tab> getChildren() {
        return mChildren;
    }

    void resume() {
        if (mMainView != null) {
//            if (mMainView.hasCrashed()) {
//                // Reload if render process has crashed. This is done here so that
//                // setFocus call sends wasShown message to correct render process.
//                if (LOGD_ENABLED)
//                    Log.e(LOGTAG, "reload Crashed Tab"+ ", webview: " + mMainView);
//                mMainView.reload();
//            }
            setupHwAcceleration(mMainView);
            mMainView.onResume();
            if (mSubView != null) {
                mSubView.onResume();
            }
        }
    }

    private void setupHwAcceleration(View web) {
        if (web == null)
            return;
        BrowserSettings settings = BrowserSettings.getInstance();
        if (settings.isHardwareAccelerated()) {
            web.setLayerType(View.LAYER_TYPE_NONE, null);
        } else {
            web.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    void pauseVideo() {
//        if (mMainView != null) {
//            mMainView.pauseVideo();
//            if (mSubView != null) {
//                mSubView.pauseVideo();
//            }
//        }
    }

    void pause() {
//        if (mMainView != null) {
//            mMainView.onPause();
//            if (mSubView != null) {
//                mSubView.onPause();
//            }
//        }
    }

    void putInForeground() {
        if (mInForeground) {
            return;
        }
        resume();
        mInForeground = true;
        Activity activity = mWebViewController.getActivity();
        if (mMainView != null) {
            mMainView.setOnCreateContextMenuListener(activity);
        }
        if (mSubView != null) {
            mSubView.setOnCreateContextMenuListener(activity);
        }
        // Show the pending error dialog if the queue is not empty
        if (mQueuedErrors != null && mQueuedErrors.size() > 0) {
            showError(mQueuedErrors.getFirst());
        }
        mWebViewController.bookmarkedStatusHasChanged(this);
    }

    void putInBackground() {
        if (!mInForeground) {
            return;
        }
        mInForeground = false;
        pause();
        if (mMainView != null) {
            mMainView.setOnCreateContextMenuListener(null);
        }
        if (mSubView != null) {
            mSubView.setOnCreateContextMenuListener(null);
        }
        //capture();
        //captureH();
    }

    public boolean inForeground() {
        return mInForeground;
    }

    /**
     * Return the top window of this tab; either the subwindow if it is not null
     * or the main window.
     * 
     * @return The top window of this tab.
     */
    WebView getTopWindow() {
        if (mSubView != null)
            return mSubView;
        else
            return mMainView;
    }

    /**
     * Return the main window of this tab. Note: if a tab is freed in the
     * background, this can return null. It is only guaranteed to be non-null
     * for the current tab.
     * 
     * @return The main WebView of this tab.
     */
    public WebView getWebView() {
        return mMainView;
    }

    void setViewContainer(View container) {
        mContainer = container;
    }

    View getViewContainer() {
        return mContainer;
    }

    /**
     * Return whether private browsing is enabled for the main window of this
     * tab.
     * 
     * @return True if private browsing is enabled.
     */
    boolean isPrivateBrowsingEnabled() {
        return mCurrentState.mIncognito;
    }

    /**
     * Return the subwindow of this tab or null if there is no subwindow.
     * 
     * @return The subwindow of this tab or null.
     */
    public WebView getSubWebView() {
        return mSubView;
    }

    void setSubWebView(WebView subView) {
        mSubView = subView;
    }

    View getSubViewContainer() {
        return mSubViewContainer;
    }

    void setSubViewContainer(View subViewContainer) {
        mSubViewContainer = subViewContainer;
    }

    public ReadModeHelper getReadModeHelper(){
        return mReadModeHelper;
    }

    /**
     * @return The geolocation permissions prompt for this tab.
     */
    GeolocationPermissionsPrompt getGeolocationPermissionsPrompt() {
        if (mGeolocationPermissionsPrompt == null) {
            ViewStub stub = (ViewStub) mContainer
                    .findViewById(R.id.geolocation_permissions_prompt);
            mGeolocationPermissionsPrompt = (GeolocationPermissionsPrompt) stub
                    .inflate();
        }
        return mGeolocationPermissionsPrompt;
    }

    /**
     * @return The application id string
     */
    String getAppId() {
        return mAppId;
    }

    /**
     * Set the application id string
     * 
     * @param id
     */
    void setAppId(String id) {
        mAppId = id;
    }

    boolean closeOnBack() {
        return mCloseOnBack;
    }

    void setCloseOnBack(boolean close) {
        mCloseOnBack = close;
    }

    String getUrl() {
        return UrlUtils.filteredUrl(mCurrentState.mUrl);
    }

    String getOriginalUrl() {
        // must be strict here, otherwise may cause security issue
        if (mMainView != null && mMainView.getOriginalUrl() != null)
            return mMainView.getOriginalUrl();
        else
            return "";
    }

    /**
     * Get the title of this tab.
     */
    String getTitle() {
        if (mCurrentState.mTitle == null && mInPageLoad) {
            return mContext.getString(R.string.title_bar_loading);
        }
        if(isShowHomePage()){
            return mContext.getString(R.string.go_home);
        }
        return mCurrentState.mTitle;
    }

    /**
     * Get the favicon of this tab.
     */
    Bitmap getFavicon() {
        if (mCurrentState.mFavicon != null) {
            return mCurrentState.mFavicon;
        }
        return getDefaultFavicon(mContext);
    }

    public boolean isBookmarkedSite() {
        return mCurrentState.mIsBookmarkedSite;
    }

    public boolean isReuseTab() {
        return mReuseState;
    }

    /**
     * Return the tab's error console. Creates the console if createIfNEcessary
     * is true and we haven't already created the console.
     * 
     * @param createIfNecessary Flag to indicate if the console should be
     *            created if it has not been already.
     * @return The tab's error console, or null if one has not been created and
     *         createIfNecessary is false.
     */
    ErrorConsoleView getErrorConsole(boolean createIfNecessary) {
        if (createIfNecessary && mErrorConsole == null) {
            mErrorConsole = new ErrorConsoleView(mContext);
            mErrorConsole.setWebView(mMainView);
        }
        return mErrorConsole;
    }

    /**
     * Sets the security state, clears the SSL certificate error and informs the
     * controller.
     */
    private void setSecurityState(SecurityState securityState) {
        mCurrentState.mSecurityState = securityState;
        mCurrentState.mSslCertificateError = null;
        mWebViewController.onUpdatedSecurityState(this);
    }

    /**
     * @return The tab's security state.
     */
    SecurityState getSecurityState() {
        return mCurrentState.mSecurityState;
    }

    /**
     * Gets the SSL certificate error, if any, for the page's main resource.
     * This is only non-null when the security state is
     * SECURITY_STATE_BAD_CERTIFICATE.
     */
    SslError getSslCertificateError() {
        return mCurrentState.mSslCertificateError;
    }

    int getLoadProgress() {
        if (mInPageLoad || !CommonUtil.isNetworkAvailable(mContext)) {
            if (!isShowHomePage()) {
                return mPageLoadProgress;
            }
        }
        return PROGRESS_MAX;
    }

    /**
     * @return TRUE if onPageStarted is called while onPageFinished is not
     *         called yet.
     */
    boolean inPageLoad() {
        return mInPageLoad;
    }

    /**
     * @return The Bundle with the tab's state if it can be saved, otherwise
     *         null
     */
    public Bundle saveState() {
        // If the WebView is null it means we ran low on memory and we already
        // stored the saved state in mSavedState.
        if (mMainView == null) {
            return mSavedState;
        }

        if (TextUtils.isEmpty(mCurrentState.mUrl)) {
            return null;
        }

        mSavedState = new Bundle();
        WebBackForwardList savedList = mMainView.saveState(mSavedState);
        if (savedList == null || savedList.getSize() == 0) {
            Log.w(LOGTAG, "Failed to save back/forward list for "
                    + mCurrentState.mUrl);
        }

        mSavedState.putLong(ID, mId);
        mSavedState.putString(CURRURL, mCurrentState.mUrl);
        mSavedState.putString(CURRTITLE, mCurrentState.mTitle);
        mSavedState.putBoolean(INCOGNITO, mMainView.isPrivateBrowsingEnabled());
        if (mAppId != null) {
            mSavedState.putString(APPID, mAppId);
        }
        mSavedState.putBoolean(CLOSEFLAG, mCloseOnBack);
        // Remember the parent tab so the relationship can be restored.
        if (mParent != null) {
            mSavedState.putLong(PARENTTAB, mParent.mId);
        }
//        mSavedState.putBoolean(USERAGENT,
//                mSettings.hasDesktopUseragent(getWebView()));
        mSavedState.putBoolean(SHOW_HOMEPAGE, mIsShowHomePage);
        return mSavedState;
    }

    /*
     * Restore the state of the tab.
     */
    private void restoreState(Bundle b) {
        mSavedState = b;
        if (mSavedState == null) {
            return;
        }
        // Restore the internal state even if the WebView fails to restore.
        // This will maintain the app id, original url and close-on-exit values.
        mId = b.getLong(ID);
        mAppId = b.getString(APPID);
        mCloseOnBack = b.getBoolean(CLOSEFLAG);
        restoreUserAgent();
        String url = b.getString(CURRURL);
        String title = b.getString(CURRTITLE);
        boolean incognito = b.getBoolean(INCOGNITO);
        mCurrentState = new PageState(mContext, incognito, url, null);
        mCurrentState.mTitle = title;
        synchronized (Tab.this) {
            if (mCaptureCached != null && mCaptureCached.bitmapCapture != null
                    && mCaptureCached.bitmapCaptureH != null) {
                DataController.getInstance(mContext).loadThumbnail(this);
            }
        }
        mIsShowHomePage = mSavedState.getBoolean(SHOW_HOMEPAGE, mIsShowHomePage);
    }

    private void restoreUserAgent() {
        if (mMainView == null || mSavedState == null) {
            return;
        }
        if (mSavedState.getBoolean(USERAGENT)
                != mSettings.hasDesktopUseragent(mMainView)) {
            mSettings.toggleDesktopUseragent(mMainView);
        }
    }

    public void updateBookmarkedStatus() {
        DataController.getInstance(mContext).queryBookmarkStatus(mCurrentState.mUrl, mIsBookmarkCallback);
    }

    private DataController.OnQueryUrlIsBookmark mIsBookmarkCallback = new DataController.OnQueryUrlIsBookmark() {
        @Override
        public void onQueryUrlIsBookmark(String url, boolean isBookmark) {
            if (mCurrentState.mUrl.equals(url)) {
                mCurrentState.mIsBookmarkedSite = isBookmark;
                mWebViewController.bookmarkedStatusHasChanged(Tab.this);
            }
        }
    };

    public Bitmap getScreenshot() {
        synchronized (Tab.this) {
            if (mCaptureCached == null)
                mCaptureCached = new CaptureInfo();
            if (mCaptureCached.bitmapCapture == null)
                createBitmap();
            return mCaptureCached.bitmapCapture;
        }
    }

    // screen shot for land
    public Bitmap getHScreenshot() {
        synchronized (Tab.this) {
            if (mCaptureCached == null)
                mCaptureCached = new CaptureInfo();
            if (mCaptureCached.bitmapCaptureH == null)
                createBitmapH();
            return mCaptureCached.bitmapCaptureH;
        }
    }

    // screen shot for land
    public Bitmap getBScreenshot() {
        synchronized (Tab.this) {
            if (mCaptureCached == null)
                mCaptureCached = new CaptureInfo();
            return mWebCaptue;
        }
    }

    public boolean isSnapshot() {
        return false;
    }

    /**
     * Must be called on the UI thread
     */
    public ContentValues createSnapshotValues() {
        return null;
    }

    /**
     * Probably want to call this on a background thread
     */
    public boolean saveViewState(ContentValues values) {
        return false;
    }

    public byte[] compressBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public void loadUrl(String url, Map<String, String> headers) {
        if (mMainView != null) {
            mPageLoadProgress = 0;
            mInPageLoad = true;
            String originalUrl = getUrl();
            mCurrentState = new PageState(mContext, isPrivateBrowsingEnabled(), url, null);
            mWebViewController.onPageStarted(this, mMainView, null);
            if (!URLUtil.isValidUrl(url)) {
                mCurrentState = new PageState(mContext, isPrivateBrowsingEnabled(), originalUrl, null);
                mWebViewController.getUi().onTabDataChanged(this);
            } else {
                mMainView.loadUrl(url);
            }
        }
    }

    public void disableUrlOverridingForLoad() {
        //mDisableOverrideUrlLoading = true;
    }

    protected void capture(Bitmap bitmap) {
        if (mMainView == null || mCaptureCached == null)
            return;
        if (!mIsShowHomePage
                && (mMainView.getContentHeight() <= 0 || mMainView.getContentHeight() <= 0)) {
            return;
        }

        int orientation = mContext.getResources().getConfiguration().orientation;
        drawTabInBitmap(orientation,
                orientation == Configuration.ORIENTATION_PORTRAIT ? mCaptureCached.bitmapCapture
                        : mCaptureCached.bitmapCaptureH,
                bitmap);
        mHandler.removeMessages(MSG_CAPTURE);
        // draw other otherOrientation screenshot delay
        int otherOrientation = Configuration.ORIENTATION_PORTRAIT;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            otherOrientation = Configuration.ORIENTATION_LANDSCAPE;
        }
        drawTabInBitmap(orientation,
                otherOrientation == Configuration.ORIENTATION_PORTRAIT ? mCaptureCached.bitmapCapture
                        : mCaptureCached.bitmapCaptureH,
                bitmap);
        persistThumbnail();

        TabControl tc = mWebViewController.getTabControl();
        if (tc != null) {
            OnThumbnailUpdatedListener updateListener = tc
                    .getOnThumbnailUpdatedListener();
            if (updateListener != null) {
                updateListener.onThumbnailUpdated(this);
            }
        }
    }

    private Bitmap mWebCaptue;
    //capture full screen web content bitmap
    public void setBackBitmap(Bitmap bmp) {
        mWebCaptue = Bitmap.createBitmap(mCaptureWidthHroizontal,
                mCaptureHeightHroizontal,
                Bitmap.Config.ARGB_8888);
        int orientation = mContext.getResources().getConfiguration().orientation;
        drawBackBitmap(orientation,mWebCaptue,bmp);
    }

    private void drawTabInBitmap(int orientation, Bitmap bitmap, Bitmap content){
        if ((mMainView == null || bitmap == null || bitmap.isRecycled() || content == null || content.isRecycled()) && !isShowHomePage())
            return;

        int width;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            width = mCaptureWidth;
        } else {
            width = mCaptureWidthHroizontal;
        }
        BaseUi ui = (BaseUi)mWebViewController.getUi();
        View captureView = isShowHomePage() ? ui.getHomePage() : mMainView;
        final int scrollX = captureView.getScrollX();
        final int scrollY = captureView.getScrollY();
        Canvas c = new Canvas(bitmap);
        c.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        final int left = scrollX;
        int top = 0;
        top = scrollY;// + ui.getTitleBar().getHeight() - ui.getCurrentMarginTop();
        int state = c.save();
        c.translate(-left, -top);
        float scale = width / (float) captureView.getWidth();
        c.scale(scale, scale, left, top);
        if (isShowHomePage()) {
            captureView.draw(c);
        } else {
            c.drawBitmap(content, 0, 0, null);
        }
        c.restoreToCount(state);
        c.setBitmap(null);
    }

    private void drawBackBitmap(int orientation, Bitmap bitmap, Bitmap content){
        if ((mMainView == null || bitmap == null || bitmap.isRecycled() || content == null || content.isRecycled()) && !isShowHomePage())
            return;
        int width;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            width = mCaptureWidth;
        } else {
            width = mCaptureWidthHroizontal;
        }
        BaseUi ui = (BaseUi)mWebViewController.getUi();
        View captureView = isShowHomePage() ? ui.getHomePage() : mMainView;
        final int scrollX = captureView.getScrollX();
        final int scrollY = captureView.getScrollY();
        Canvas c = new Canvas(bitmap);
        c.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        final int left = scrollX;
        int top = 0;
        top = scrollY;// + ui.getTitleBar().getHeight() - ui.getCurrentMarginTop();
        int state = c.save();
        c.translate(-left, -top);
        float scale = width / (float) captureView.getWidth();
        c.scale(scale, 0.353f, left, top);
        if (isShowHomePage()) {
            captureView.draw(c);
        } else {
            c.drawBitmap(content, 0, 0, null);
        }
        c.restoreToCount(state);
        c.setBitmap(null);
    }

    public void getWebViewCapture(ValueCallback<Bitmap> callback) {
//        if (mMainView != null)
//            mMainView.getContentBitmapAsync(1.0f, new Rect(), callback);
//        else
//            callback.onReceiveValue(null);
    }

    /*@Override
    public void onNewPicture(WebView view, Picture picture) {
        postCapture();
    }*/

    private void postCapture() {
        if (!mHandler.hasMessages(MSG_CAPTURE)) {
            mHandler.sendEmptyMessageDelayed(MSG_CAPTURE, CAPTURE_DELAY);
        }
    }

    public boolean canGoBack() {
        return (mMainView != null && mMainView.canGoBack())/* ||
                ((!isShowHomePage() && mParent == null) && !mWebViewController.getUi().isCmccFeature())*/;
    }

    public boolean canGoForward() {
        if(mMainView == null){
            return false;
        }
        if(mMainView.canGoForward()){
            return true;
        }
        if (!mWebViewController.getUi().isOpenHomePageFeature()) {
            return false;
        }
        //if (isShowHomePage() && !TextUtils.isEmpty(mMainView.getUrl())) {
        //    return true;
        //}
        return false;
    }

    public void goBack() {
        if (mMainView != null && mMainView.canGoBack()) {
            mMainView.goBack();
            return;
        }
        UI ui = mWebViewController.getUi();
        if(!ui.isOpenHomePageFeature()){
            return;
        }
        ui.showHomePage();
        CrashRecoveryHandler.getInstance().backupState();
    }

    public void goForward() {
        BaseUi baseUi = (BaseUi)mWebViewController.getUi();
        if (mMainView != null && mMainView.canGoForward() && !baseUi.isShowHomePage()) {
            mMainView.goForward();
            return;
        }
        baseUi.hideHomePage();
    }

    /**
     * Causes the tab back/forward stack to be cleared once, if the given URL is
     * the next URL to be added to the stack. This is used to ensure that
     * preloaded URLs that are not subsequently seen by the user do not appear
     * in the back stack.
     */
    public void clearBackStackWhenItemAdded(Pattern urlPattern) {
        mClearHistoryUrlPattern = urlPattern;
    }

    protected void persistThumbnail() {
        DataController.getInstance(mContext).saveThumbnail(this);
    }

//    protected void persistLandThumbnail() {
//        DataController.getInstance(mContext).saveLandThumbnail(this);
//    }

    protected void deleteThumbnail() {
        DataController.getInstance(mContext).deleteThumbnail(this);
    }

//    protected void deleteLandThumbnail() {
//        DataController.getInstance(mContext).deleteLandThumbnail(this);
//    }

    void updateCaptureFromBlob(byte[] blob, boolean vertical) {
        if (blob.length > 2000000 || blob.length <= 0) // avoid out of memory. and if is impossible for blob larger than 2M
            return;

        synchronized (Tab.this) {
            if (mCaptureCached != null) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                //opt.inMutable = true;
                if (vertical) {
                    mCaptureCached.bitmapCapture = BitmapFactory.decodeByteArray(blob, 0, blob.length, opt);
                } else {
                    mCaptureCached.bitmapCaptureH = BitmapFactory.decodeByteArray(blob, 0, blob.length, opt);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(100);
        builder.append(mId);
        builder.append(") has parent: ");
        if (getParent() != null) {
            builder.append("true[");
            builder.append(getParent().getId());
            builder.append("]");
        } else {
            builder.append("false");
        }
        builder.append(", incog: ");
        builder.append(isPrivateBrowsingEnabled());
            builder.append(", title: ");
            builder.append(getTitle());
            builder.append(", url: ");
            builder.append(getUrl());
            builder.append(", reuse: ");
            builder.append(mReuseState);
            builder.append(", mIsShowHomePage: ");
            builder.append(mIsShowHomePage);
        return builder.toString();
    }

    private void handleProceededAfterSslError(SslError error) {
        if (error.getUrl().equals(mCurrentState.mUrl)) {
            // The security state should currently be SECURITY_STATE_SECURE.
            setSecurityState(SecurityState.SECURITY_STATE_BAD_CERTIFICATE);
            mCurrentState.mSslCertificateError = error;
        } else if (getSecurityState() == SecurityState.SECURITY_STATE_SECURE) {
            // The page's main resource is secure and this error is for a
            // sub-resource.
            setSecurityState(SecurityState.SECURITY_STATE_MIXED);
        }
    }

    public void closeGeolocationDialog() {
        if (mGeolocationDialog != null && mGeolocationDialog.isShowing()) {
            mGeolocationDialog.dismiss();
        }
    }

    public void setPosition(int position) {
        if (isPrivateBrowsingEnabled()) {
            mPosition = position;
        } else {
            mIncogPosition = position;
        }
    }

    public int getPosition(boolean isIncog) {
        if (isIncog) {
            if (mPosition < 0) {
                return 0;
            }
            return mPosition;
        } else {
            if (mIncogPosition < 0) {
                return 0;
            }
            return mIncogPosition;
        }
    }

    public boolean isShowHomePage() {
        return mIsShowHomePage && mWebViewController.getUi().isOpenHomePageFeature();
    }

    public void setShowHomePage(boolean isShowHomePage) {
        mIsShowHomePage = isShowHomePage;
        if(!mWebViewController.getUi().isOpenHomePageFeature()){
            return;
        }
        if(mIsShowHomePage){
            putInBackground();
        }else{
            putInForeground();
        }
    }

    private boolean mCanChangeTitle = true;

    public void setCanChangeTitle(boolean canChangeTitle) {
        mCanChangeTitle = canChangeTitle;
    }

    public boolean canChangeTitle() {
        return mCanChangeTitle;
    }

    public boolean isDownoadUrl() {
        return mIsDownloadUrl;
    }

    public void setIsDownloadUrl(boolean isDownloadUrl) {
        mIsDownloadUrl = isDownloadUrl;
    }

    public boolean isMediaPlaying() {
        if (mMainView == null) {
            return false;
        }
        return false;//mMainView.isMediaPlaying();
    }
}
