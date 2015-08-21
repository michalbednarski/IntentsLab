package com.github.michalbednarski.intentslab.appinfo;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * See {@link MyComponentInfo}
 */
class MyComponentInfoImpl implements MyComponentInfo {

    int mType;
    ComponentInfo mComponentInfo;


    public IntentFilter[] mIntentFilters;


    @Override
    public String getName() {
        return mComponentInfo.name;
    }

    @Override
    public boolean isExported() {
        return mComponentInfo.exported;
    }

    /**
     * Get permission protecting this component
     * for providers this is reading permission
     */
    public String getPermission() {
        if (mComponentInfo instanceof ActivityInfo) {
            ActivityInfo activityInfo = (ActivityInfo) mComponentInfo;
            return activityInfo.permission;
        } else if (mComponentInfo instanceof ServiceInfo) {
            ServiceInfo serviceInfo = (ServiceInfo) mComponentInfo;
            return serviceInfo.permission;
        } else if (mComponentInfo instanceof ProviderInfo) {
            ProviderInfo providerInfo = (ProviderInfo) mComponentInfo;
            return providerInfo.readPermission;
        }
        throw new AssertionError();
    }

    /**
     * Get write permission for provider
     */
    public String getWritePermission() {
        return ((ProviderInfo) mComponentInfo).writePermission;
    }

    @Override
    public CharSequence loadLabel(PackageManager packageManager) {
        return mComponentInfo.loadLabel(packageManager);
    }

    @Override
    public Drawable loadIcon(PackageManager packageManager) {
        return mComponentInfo.loadIcon(packageManager);
    }

    @Override
    @SuppressLint("InlinedApi")
    public boolean isEnabled(PackageManager packageManager) {
        int state = packageManager.getComponentEnabledSetting(new ComponentName(mComponentInfo.packageName, mComponentInfo.name));
        return !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && !mComponentInfo.enabled));
    }

    @Override
    public IntentFilter[] getIntentFilters() {
        return mIntentFilters;
    }

    @Override
    public Bundle getMetaData() {
        return mComponentInfo.metaData;
    }

    @Override
    public ProviderInfo getProviderInfo() {
        return (ProviderInfo) mComponentInfo;
    }

    MyComponentInfoImpl(int type, ComponentInfo componentInfo) {
        mType = type;
        mComponentInfo = componentInfo;
    }



}
