
package com.android.myapidemo.smartisan.browser.util;

import com.android.myapidemo.R;

import android.app.Dialog;
import android.content.Context;

public class DialogConfirm extends Dialog {

    public DialogConfirm(Context context) {
        super(context, R.style.calDialogTheme);
    }

    public void InitDialog(int resID) {
        setContentView(resID);
    }

}
