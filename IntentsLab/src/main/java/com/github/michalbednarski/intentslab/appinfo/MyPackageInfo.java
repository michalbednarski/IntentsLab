package com.github.michalbednarski.intentslab.appinfo;

import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.Collection;

/**
 * Information about application/package
 */
public interface MyPackageInfo {
    String getPackageName();

    CharSequence loadLabel(PackageManager pm);

    Collection<MyComponentInfo> getActivities();
    MyComponentInfo getActivityByName(String name);

    Collection<MyComponentInfo> getReceivers();
    MyComponentInfo getReceiverByName(String name);

    Collection<MyComponentInfo> getServices();
    MyComponentInfo getServiceByName(String name);

    Collection<MyComponentInfo> getProviders();
    MyComponentInfo getProviderByName(String name);

    boolean isApplicationEnabled();

    boolean isSystemApplication();

    /**
     * Get meta data for 'application'
     */
    Bundle getMetaData();
}
