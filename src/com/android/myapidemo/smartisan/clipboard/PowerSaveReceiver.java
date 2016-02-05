package com.android.myapidemo.smartisan.clipboard;


import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PowerSaveReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = (String) ReflectHelper.getStaticVariable("android.os.PowerManager", "ACTION_POWER_SAVE_MODE_CHANGED");
        boolean isMonitorClipBoard = BrowserSettings.getInstance().isMonitorClipBoard();
        if (action != null && action.equals(intent.getAction())) {
            String mode = (String) ReflectHelper.getStaticVariable("android.os.PowerManager", "EXTRA_POWER_SAVE_MODE");
            if (mode != null) {
                boolean mExtra = intent.getBooleanExtra(mode, false);
                if (mExtra) {
                    context.stopService(new Intent(context, ClipboardMonitor.class));
                } else if (isMonitorClipBoard) {
                    context.startService(new Intent(context, ClipboardMonitor.class));
                }
            }
        }
    }

}
