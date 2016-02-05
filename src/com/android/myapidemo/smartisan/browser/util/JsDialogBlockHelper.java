/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.myapidemo.smartisan.browser.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import com.android.myapidemo.R;

/**
 * Helper class to create JavaScript dialogs. It is used by
 * different WebView implementations.
 *
 * @hide Helper class for internal use
 */
public class JsDialogBlockHelper {

    private static final String TAG = "JsDialogBlockHelper";

    // Dialog types
    public static final int ALERT   = 1;
    public static final int CONFIRM = 2;
    public static final int PROMPT  = 3;
    public static final int UNLOAD  = 4;

    private final String mDefaultValue;
    private final JsResult mResult;
    private final String mMessage;
    private final int mType;
    private final String mUrl;
    private final HashMap<String, Object> mJsMap;
    private CheckBox mCheckBox;

    public JsDialogBlockHelper(JsResult result, int type, String defaultValue, String message,
            String url, HashMap<String, Object> jsMap) {
        mResult = result;
        mDefaultValue = defaultValue;
        mMessage = message;
        mType = type;
        mUrl = url;
        mJsMap = jsMap;
    }

    public void showDialog(Context context) {
        if (!canShowAlertDialog(context)) {
            Log.w(TAG, "Cannot create a dialog, the WebView context is not an Activity");
            mResult.cancel();
            return;
        }

        String title, displayMessage;
        int positiveTextId, negativeTextId;
        if (mType == UNLOAD) {
            title = context.getString(R.string.js_dialog_before_unload_title);
            displayMessage = context.getString(
                    R.string.js_dialog_before_unload, mMessage);
            positiveTextId = R.string.js_dialog_before_unload_positive_button;
            negativeTextId = R.string.js_dialog_before_unload_negative_button;
        } else {
            title = getJsDialogTitle(context);
            displayMessage = mMessage;
            positiveTextId = R.string.ok;
            negativeTextId = R.string.cancel;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setOnCancelListener(new CancelListener());
        View view = LayoutInflater.from(context).inflate(
                R.layout.js_block_prompt, null);
        if (mType != PROMPT) {
            builder.setPositiveButton(positiveTextId, new PositiveListener(null));
        } else {
            EditText edit = ((EditText) view.findViewById(R.id.value));
            edit.setText(mDefaultValue);
            edit.setVisibility(View.VISIBLE);
            builder.setPositiveButton(positiveTextId, new PositiveListener(edit));
        }
        ((TextView) view.findViewById(R.id.message)).setText(displayMessage);
        mCheckBox = (CheckBox) view.findViewById(R.id.block);
        builder.setView(view);
        if (mType != ALERT) {
            builder.setNegativeButton(negativeTextId, new CancelListener());
        }
        builder.show();
    }

    private class CancelListener implements DialogInterface.OnCancelListener,
            DialogInterface.OnClickListener {
        @Override
        public void onCancel(DialogInterface dialog) {
            mResult.cancel();
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mResult.cancel();
        }
    }

    private class PositiveListener implements DialogInterface.OnClickListener {
        private final EditText mEdit;

        public PositiveListener(EditText edit) {
            mEdit = edit;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mEdit == null) {
                mResult.confirm();
            } else {
                //((JsPromptResult)mResult).confirm(mEdit.getText().toString());
            }
            mJsMap.put(mUrl, mCheckBox.isChecked());
        }
    }

    private String getJsDialogTitle(Context context) {
        String title = mUrl;
        if (URLUtil.isDataUrl(mUrl)) {
            // For data: urls, we just display 'JavaScript' similar to Chrome.
            title = "JavaScript";
        } else {
            try {
                URL alertUrl = new URL(mUrl);
                // For example: "The page at 'http://www.mit.edu' says:"
                title = context.getString(R.string.js_dialog_title,
                        alertUrl.getProtocol() + "://" + alertUrl.getHost());
            } catch (MalformedURLException ex) {
                // do nothing. just use the url as the title
            }
        }
        return title;
    }

    private static boolean canShowAlertDialog(Context context) {
        // We can only display the alert dialog if mContext is
        // an Activity context.
        // FIXME: Should we display dialogs if mContext does
        // not have the window focus (e.g. if the user is viewing
        // another Activity when the alert should be displayed) ?
        // See bug 3166409
        return context instanceof Activity;
    }
}
