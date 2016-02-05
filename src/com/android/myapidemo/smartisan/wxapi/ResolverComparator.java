
package com.android.myapidemo.smartisan.wxapi;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashSet;

import com.android.myapidemo.smartisan.reflect.ReflectHelper;

public class ResolverComparator implements Comparator<ResolveInfo> {
    private HashSet<String> mSmartisanSocialPackages = new HashSet<String>();

    public ResolverComparator(Context context) {
        mPM = context.getPackageManager();
        mCollator.setStrength(Collator.PRIMARY);
        Object object = ReflectHelper.getStaticVariable("com.android.internal.R$array",
                "smartisan_app_social_notification");
        if (object instanceof Integer) {
            String[] socialList = context.getResources().getStringArray((Integer) object);
            for (int i = 0; i < socialList.length; ++i) {
                mSmartisanSocialPackages.add(socialList[i]);
            }
        }
    }

    public final int compare(ResolveInfo a, ResolveInfo b) {
        Object chosenPriorityA = ReflectHelper.getVariable(a, "chosenPriority");
        Object chosenPriorityB = ReflectHelper.getVariable(b, "chosenPriority");
        if (chosenPriorityA instanceof Integer) {
            if ((Integer) chosenPriorityA < (Integer) chosenPriorityB)
                return 1;
            if ((Integer) chosenPriorityA > (Integer) chosenPriorityB)
                return -1;

        }
        final int fa = a.activityInfo.applicationInfo.flags & FLAG_SYSTEM_APP;
        final int fb = b.activityInfo.applicationInfo.flags & FLAG_SYSTEM_APP;
        if (fa < fb)
            return 1;
        if (fa > fb)
            return -1;

        final boolean isa = mSmartisanSocialPackages.contains(a.activityInfo.packageName);
        final boolean isb = mSmartisanSocialPackages.contains(b.activityInfo.packageName);
        if (isa && !isb)
            return -1;
        if (!isa && isb)
            return 1;

        CharSequence sa = a.loadLabel(mPM);
        if (sa == null)
            sa = a.activityInfo.name;
        CharSequence sb = b.loadLabel(mPM);
        if (sb == null)
            sb = b.activityInfo.name;
        return mCollator.compare(sa.toString(), sb.toString());
    }

    private final static int FLAG_SYSTEM_APP = ApplicationInfo.FLAG_SYSTEM
            | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
    private final Collator mCollator = Collator.getInstance();
    private PackageManager mPM;
}
