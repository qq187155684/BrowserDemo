
package com.android.myapidemo.smartisan.clipboard;


import com.android.myapidemo.smartisan.browse.BrowserSettings;
import com.android.myapidemo.smartisan.reflect.ReflectHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * When booting is completed, it starts {@link ClipboardMonitor} service to monitor the states of
 * clipboard.
 */
public class BootupMonitor extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intents) {
        PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        Object isPowerSaveMode = ReflectHelper.invokeMethod(mPowerManager, "isPowerSaveMode", null, null);
        if (BrowserSettings.getInstance().isMonitorClipBoard()) {
            if (isPowerSaveMode != null && !((Boolean)isPowerSaveMode) || isPowerSaveMode == null)
                context.startService(new Intent(context, ClipboardMonitor.class));
        }
    }
}
